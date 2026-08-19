package com.example.norwegian4x4.phone

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive meets Nordic performance — shared visual language with the
 * watch app. Deep obsidian base, slate-tinted expressive containers, one
 * vibrant ice-blue accent, and generous rounded corners over hard borders.
 */
val Obsidian = Color(0xFF0B0F17)
val SlateSurface = Color(0xFF1E2630)
val SlateSurfaceHigh = Color(0xFF283241)
val FrostWhite = Color(0xFFF4F8FB)
val MistGray = Color(0xFF8FA0B3)
val IceBlue = Color(0xFF00D2FF)
val SignalAmber = Color(0xFFFFB020)
val AlertRed = Color(0xFFFF4B4B)

private val GlacialScheme = darkColorScheme(
    primary = IceBlue,
    onPrimary = Obsidian,
    secondary = SlateSurfaceHigh,
    onSecondary = FrostWhite,
    tertiary = SignalAmber,
    onTertiary = Obsidian,
    background = Obsidian,
    onBackground = FrostWhite,
    surface = SlateSurface,
    onSurface = FrostWhite,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = MistGray,
    outline = SlateSurfaceHigh,
    error = AlertRed,
    onError = Obsidian,
)

/** Expressive M3 container radius (24-28dp). */
private val ContainerRadius = 26.dp

private val GlacialShapes = Shapes(
    extraSmall = RoundedCornerShape(percent = 50),
    small = RoundedCornerShape(percent = 50),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(ContainerRadius),
    extraLarge = RoundedCornerShape(ContainerRadius),
)

@Composable
fun Norwegian4x4Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlacialScheme,
        shapes = GlacialShapes,
        content = content,
    )
}
