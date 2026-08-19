package com.example.norwegian4x4

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Shapes

/**
 * M3 Expressive meets Nordic performance: deep obsidian base, slate-tinted
 * expressive card containers, and one unmistakable ice-blue accent. Scale and
 * contrast carry the hierarchy — no hairlines, no decorative borders.
 */
val Obsidian = Color(0xFF0B0F17)
val SlateSurface = Color(0xFF1E2630)
val SlateSurfaceHigh = Color(0xFF283241)
val FrostWhite = Color(0xFFF4F8FB)
val MistGray = Color(0xFF8FA0B3)

/** The one vibrant accent: primary action, "in zone", success. */
val IceBlue = Color(0xFF00D2FF)

/** Muted label color for warm-up / recovery / cool-down phase tags. */
val PhaseEasy = Color(0xFF6E93AC)

/** Work-phase label and high-effort accent. */
val SignalAmber = Color(0xFFFFB020)

/** Slow-down guidance and errors. */
val AlertRed = Color(0xFFFF4B4B)

/** Paused state. */
val PausedAmber = Color(0xFFFFD24D)

/** Large rounded "expressive container" radius for card groupings (24-28dp). */
val ContainerRadius = 26.dp

private val ExpressiveShapes = Shapes(
    // Capsule/pill buttons — fully rounded regardless of height.
    small = RoundedCornerShape(percent = 50),
    medium = RoundedCornerShape(percent = 50),
    large = RoundedCornerShape(ContainerRadius),
)

private val ExpressiveColors = Colors(
    primary = IceBlue,
    primaryVariant = IceBlue,
    secondary = SlateSurfaceHigh,
    secondaryVariant = MistGray,
    background = Obsidian,
    surface = SlateSurface,
    error = AlertRed,
    onPrimary = Obsidian,
    onSecondary = FrostWhite,
    onBackground = FrostWhite,
    onSurface = FrostWhite,
    onError = Obsidian,
)

@Composable
fun Norwegian4x4Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = ExpressiveColors,
        shapes = ExpressiveShapes,
        content = content,
    )
}
