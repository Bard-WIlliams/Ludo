package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

data class DiscoveredLanRoom(
    val serviceName: String,
    val hostAddress: String,
    val port: Int
)

class LanDiscoveryManager(context: Context) {

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredLanRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredLanRoom>> = _discoveredRooms.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    fun registerService(roomName: String, port: Int) {
        unregisterService()
        val manager = nsdManager ?: return
        acquireMulticastLock()

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = roomName.take(28).ifBlank { "NordicLudo" }
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListener = listener
        try {
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Throwable) {
        }
    }

    fun unregisterService() {
        val manager = nsdManager ?: return
        registrationListener?.let { listener ->
            try {
                manager.unregisterService(listener)
            } catch (_: Throwable) {
            }
        }
        registrationListener = null
    }

    fun startDiscovery() {
        stopDiscovery()
        val manager = nsdManager ?: return
        acquireMulticastLock()
        _discoveredRooms.value = emptyList()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isDiscovering.value = true
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE &&
                    serviceInfo.serviceType != "$SERVICE_TYPE."
                ) return
                resolveService(manager, serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _discoveredRooms.value = _discoveredRooms.value.filterNot {
                    it.serviceName == serviceInfo.serviceName
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isDiscovering.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
            }
        }
        discoveryListener = listener
        try {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Throwable) {
            _isDiscovering.value = false
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveService(manager: NsdManager, serviceInfo: NsdServiceInfo) {
        try {
            manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit

                override fun onServiceResolved(info: NsdServiceInfo) {
                    val hostIp = info.host?.hostAddress ?: return
                    val port = info.port
                    if (port <= 0) return
                    val room = DiscoveredLanRoom(
                        serviceName = info.serviceName,
                        hostAddress = hostIp,
                        port = port
                    )
                    val current = _discoveredRooms.value
                        .filterNot { it.hostAddress == room.hostAddress && it.port == room.port }
                    _discoveredRooms.value = current + room
                }
            })
        } catch (_: Throwable) {
        }
    }

    fun stopDiscovery() {
        val manager = nsdManager ?: return
        discoveryListener?.let { listener ->
            try {
                manager.stopServiceDiscovery(listener)
            } catch (_: Throwable) {
            }
        }
        discoveryListener = null
        _isDiscovering.value = false
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("nordic_ludo_nsd")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
            }
        } catch (_: Throwable) {
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Throwable) {
        }
    }

    companion object {
        const val SERVICE_TYPE = "_nordicludo._tcp"

        fun getLocalIpv4Address(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
                val candidates = mutableListOf<String>()
                for (intf in interfaces) {
                    if (!intf.isUp || intf.isLoopback) continue
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: continue
                            if (intf.name.startsWith("wlan") || intf.name.startsWith("ap")) {
                                return ip
                            }
                            candidates += ip
                        }
                    }
                }
                return candidates.firstOrNull() ?: "127.0.0.1"
            } catch (_: Throwable) {
                return "127.0.0.1"
            }
        }
    }
}
