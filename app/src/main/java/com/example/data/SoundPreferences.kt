package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nordicPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nordic_ludo_settings"
)

class SoundPreferences(private val context: Context) {

    private val volumeKey = intPreferencesKey("sound_volume_percent")

    val volumePercent: Flow<Int> = context.nordicPrefsDataStore.data.map { prefs ->
        (prefs[volumeKey] ?: DEFAULT_VOLUME).coerceIn(0, 100)
    }

    suspend fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        context.nordicPrefsDataStore.edit { prefs ->
            prefs[volumeKey] = clamped
        }
    }

    companion object {
        const val DEFAULT_VOLUME = 75
    }
}
