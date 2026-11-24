package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CharcoalInk,
    onPrimary = ChalkWhite,
    primaryContainer = SoftOat,
    onPrimaryContainer = CharcoalInk,
    secondary = NordicBlue,
    onSecondary = ChalkWhite,
    secondaryContainer = NordicBlueTint,
    onSecondaryContainer = CharcoalInk,
    tertiary = NordicGreen,
    onTertiary = ChalkWhite,
    tertiaryContainer = NordicGreenTint,
    onTertiaryContainer = CharcoalInk,
    background = BirchLinen,
    onBackground = CharcoalInk,
    surface = ChalkWhite,
    onSurface = CharcoalInk,
    surfaceVariant = SoftOat,
    onSurfaceVariant = MutedSlate,
    outline = StoneHairline,
    outlineVariant = WarmSand,
    error = NordicRed,
    onError = ChalkWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = WarmIvory,
    onPrimary = NightCharcoal,
    primaryContainer = DarkOat,
    onPrimaryContainer = WarmIvory,
    secondary = NordicBlue,
    onSecondary = ChalkWhite,
    secondaryContainer = NordicBlueDarkTint,
    onSecondaryContainer = WarmIvory,
    tertiary = NordicGreen,
    onTertiary = ChalkWhite,
    tertiaryContainer = NordicGreenDarkTint,
    onTertiaryContainer = WarmIvory,
    background = NightCharcoal,
    onBackground = WarmIvory,
    surface = DarkBirch,
    onSurface = WarmIvory,
    surfaceVariant = DarkOat,
    onSurfaceVariant = MutedAsh,
    outline = DarkStoneHairline,
    outlineVariant = DarkOat,
    error = NordicRed,
    onError = ChalkWhite
)

val NordicShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NordicLudoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NordicTypography,
        shapes = NordicShapes,
        content = content
    )
}
