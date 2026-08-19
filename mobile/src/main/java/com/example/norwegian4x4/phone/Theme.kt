package com.example.norwegian4x4.phone

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "Glacial Precision" palette for the High-Contrast Utilitarian design language —
 * shared visual language with the watch app, adapted to Material3 tokens.
 */
val Ink = Color(0xFF05090C)
val Panel = Color(0xFF10171F)
val Hairline = Color(0xFF2A3742)
val IceWhite = Color(0xFFEDF4F7)
val SteelGray = Color(0xFF7C8FA0)
val GlacialCyan = Color(0xFF29E1EA)
val FrostBlue = Color(0xFF5AA8E0)
val SignalAmber = Color(0xFFFFB020)
val AlertRed = Color(0xFFFF4B4B)

private val GlacialScheme = darkColorScheme(
    primary = GlacialCyan,
    onPrimary = Ink,
    secondary = FrostBlue,
    onSecondary = Ink,
    tertiary = SignalAmber,
    onTertiary = Ink,
    background = Ink,
    onBackground = IceWhite,
    surface = Panel,
    onSurface = IceWhite,
    surfaceVariant = Panel,
    onSurfaceVariant = SteelGray,
    outline = Hairline,
    error = AlertRed,
    onError = Ink,
)

private val GlacialShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

@Composable
fun Norwegian4x4Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlacialScheme,
        shapes = GlacialShapes,
        content = content,
    )
}
