package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.nordicAmbientBackground
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.NordicBlue
import com.example.ui.theme.NordicGreen
import com.example.ui.theme.NordicRed
import com.example.ui.theme.NordicYellow

@Composable
fun HomeScreen(
    volumePercent: Int,
    onAiPlayClick: () -> Unit,
    onLocalPlayClick: () -> Unit,
    onLanPlayClick: () -> Unit,
    onOpenSoundSheet: () -> Unit,
    onOpenRules: () -> Unit
) {
    Scaffold(
        modifier = Modifier.nordicAmbientBackground(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FourDotEmblem()
                        Column {
                            Text(
                                text = "Nordic Ludo",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Scandinavian Tabletop Edition",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onOpenRules,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_rules_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = "Game rules"
                            )
                        }
                        Surface(
                            onClick = onOpenSoundSheet,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_sound_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (volumePercent == 0) {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    },
                                    contentDescription = "Sound settings",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "$volumePercent%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // Editorial Scandinavian Tabletop Hero Banner
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(198.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nordic_hero_1795508970490),
                            contentDescription = "Minimalist Scandinavian Ludo board tabletop",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(26.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ChalkWhite.copy(alpha = 0.90f)
                        ) {
                            Text(
                                text = "Solo AI · Pass & Play · 2v2 Teams · LAN",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1F2421),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Mode Cards
                ModeActionCard(
                    title = "Play vs AI Bots",
                    subtitle = "Challenge Casual, Tactical, or Master bots solo or in 2v2 co-op",
                    icon = Icons.Rounded.SmartToy,
                    accentColor = NordicRed,
                    testTag = "home_ai_play_card",
                    onClick = onAiPlayClick
                )

                ModeActionCard(
                    title = "Local Pass & Play",
                    subtitle = "2 to 4 players on one device · Free-for-All or 2v2 Teams",
                    icon = Icons.Rounded.Groups,
                    accentColor = NordicGreen,
                    testTag = "home_local_play_card",
                    onClick = onLocalPlayClick
                )

                ModeActionCard(
                    title = "LAN Multiplayer",
                    subtitle = "Auto-discover nearby Wi-Fi/hotspot rooms or connect via IP",
                    icon = Icons.Rounded.Wifi,
                    accentColor = NordicBlue,
                    testTag = "home_lan_play_card",
                    onClick = onLanPlayClick
                )

                // Feature Summary
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "House Rules",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "• Classic Roll & Capture: Roll 6 to enter; bonus roll on 6, capture, or finish.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• 2v2 Adjacent-Corner Teams: Red + Green vs Blue + Yellow with Shared Safe Zones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FourDotEmblem() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = NordicRed) {}
                Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = NordicGreen) {}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = NordicBlue) {}
                Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = NordicYellow) {}
            }
        }
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
