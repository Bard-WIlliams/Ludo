package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.domain.PlayerColor
import com.example.domain.TeamId
import com.example.ui.theme.BirchLinen
import com.example.ui.theme.DarkBirch
import com.example.ui.theme.NightCharcoal
import com.example.ui.theme.NordicBlue
import com.example.ui.theme.NordicBlueDarkTint
import com.example.ui.theme.NordicBlueTint
import com.example.ui.theme.NordicGreen
import com.example.ui.theme.NordicGreenDarkTint
import com.example.ui.theme.NordicGreenTint
import com.example.ui.theme.NordicRed
import com.example.ui.theme.NordicRedDarkTint
import com.example.ui.theme.NordicRedTint
import com.example.ui.theme.NordicYellow
import com.example.ui.theme.NordicYellowDarkTint
import com.example.ui.theme.NordicYellowTint
import com.example.ui.theme.SoftOat
import com.example.ui.theme.TeamNorthAccent
import com.example.ui.theme.TeamSouthAccent
import com.example.ui.theme.WarmSand

fun PlayerColor.accentColor(): Color = when (this) {
    PlayerColor.RED -> NordicRed
    PlayerColor.GREEN -> NordicGreen
    PlayerColor.YELLOW -> NordicYellow
    PlayerColor.BLUE -> NordicBlue
}

@Composable
fun PlayerColor.surfaceTintColor(darkTheme: Boolean = isSystemInDarkTheme()): Color = when (this) {
    PlayerColor.RED -> if (darkTheme) NordicRedDarkTint else NordicRedTint
    PlayerColor.GREEN -> if (darkTheme) NordicGreenDarkTint else NordicGreenTint
    PlayerColor.YELLOW -> if (darkTheme) NordicYellowDarkTint else NordicYellowTint
    PlayerColor.BLUE -> if (darkTheme) NordicBlueDarkTint else NordicBlueTint
}

fun TeamId.accentColor(): Color = when (this) {
    TeamId.NORTH -> TeamNorthAccent
    TeamId.SOUTH -> TeamSouthAccent
}

@Composable
fun Modifier.nordicAmbientBackground(darkTheme: Boolean = isSystemInDarkTheme()): Modifier {
    return this.drawBehind {
        if (darkTheme) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(NightCharcoal, DarkBirch, NightCharcoal)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NordicBlue.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                    radius = size.maxDimension * 0.55f
                )
            )
        } else {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(BirchLinen, SoftOat.copy(alpha = 0.72f), WarmSand.copy(alpha = 0.45f))
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NordicYellow.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.1f),
                    radius = size.maxDimension * 0.5f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NordicGreen.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.85f),
                    radius = size.maxDimension * 0.55f
                )
            )
        }
    }
}
