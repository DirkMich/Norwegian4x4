package com.example.norwegian4x4

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Shapes

/**
 * "Glacial Precision" palette for the High-Contrast Utilitarian design language.
 * Cold, instrument-panel tones with exactly two warm accents reserved for
 * effort (amber) and danger (red) so they stay unmistakable against the rest.
 */
val Ink = Color(0xFF05090C)
val Panel = Color(0xFF10171F)
val Hairline = Color(0xFF2A3742)
val IceWhite = Color(0xFFEDF4F7)
val SteelGray = Color(0xFF7C8FA0)

/** Primary action / "in zone" / success. */
val GlacialCyan = Color(0xFF29E1EA)

/** Info accent — "speed up" guidance, active/interactive secondary elements. */
val FrostBlue = Color(0xFF5AA8E0)

/** Muted label color for warm-up / recovery / cool-down phase tags. */
val PhaseEasy = Color(0xFF6E93AC)

/** Work-phase label and high-effort accent — the one deliberate warm tone. */
val SignalAmber = Color(0xFFFFB020)

/** Slow-down guidance and errors. */
val AlertRed = Color(0xFFFF4B4B)

/** Paused state. */
val PausedAmber = Color(0xFFFFD24D)

private val GlacialShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
)

private val GlacialColors = Colors(
    primary = GlacialCyan,
    primaryVariant = FrostBlue,
    secondary = SteelGray,
    secondaryVariant = Hairline,
    background = Ink,
    surface = Panel,
    error = AlertRed,
    onPrimary = Ink,
    onSecondary = IceWhite,
    onBackground = IceWhite,
    onSurface = IceWhite,
    onError = Ink,
)

@Composable
fun Norwegian4x4Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = GlacialColors,
        shapes = GlacialShapes,
        content = content,
    )
}
