package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PrumoGreen,
    onPrimary = Color.White,
    primaryContainer = PrumoGreenSoft,
    onPrimaryContainer = PrumoGreenDark,
    secondary = PrumoGreenDark,
    onSecondary = Color.White,
    secondaryContainer = PrumoSoftSurface,
    onSecondaryContainer = PrumoInk,
    tertiary = PrumoAlertYellow,
    onTertiary = PrumoAlertYellowInk,
    background = PrumoBg,
    onBackground = PrumoInk,
    surface = PrumoCard,
    onSurface = PrumoInk,
    surfaceVariant = PrumoSoftSurface,
    onSurfaceVariant = PrumoMuted,
    surfaceContainerLow = PrumoSoftSurface,
    surfaceContainer = PrumoCard,
    surfaceContainerHigh = PrumoSurfaceHigh,
    outline = PrumoLine,
    outlineVariant = PrumoLine,
    error = PrumoDanger,
    onError = Color.White,
    errorContainer = PrumoDangerBg,
    onErrorContainer = PrumoDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = PrumoDarkGreenPrimary,
    onPrimary = PrumoDarkBg,
    primaryContainer = PrumoDarkGreenContainer,
    onPrimaryContainer = Color(0xFFB5F4D4),
    secondary = PrumoLime,
    onSecondary = PrumoInk,
    secondaryContainer = PrumoDarkSurfaceHigh,
    onSecondaryContainer = PrumoDarkOnSurface,
    tertiary = PrumoAlertYellow,
    onTertiary = PrumoAlertYellowInk,
    background = PrumoDarkBg,
    onBackground = PrumoDarkOnSurface,
    surface = PrumoDarkSurface,
    onSurface = PrumoDarkOnSurface,
    surfaceVariant = PrumoDarkSurfaceHigh,
    onSurfaceVariant = PrumoDarkMuted,
    surfaceContainerLow = PrumoDarkSurface,
    surfaceContainer = PrumoDarkSurface,
    surfaceContainerHigh = PrumoDarkSurfaceHigh,
    outline = PrumoDarkLine,
    outlineVariant = PrumoDarkLine,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5F0A06),
    errorContainer = PrumoDarkDangerBg,
    onErrorContainer = Color(0xFFFFDAD6)
)

val PrumoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PrumoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PrumoTypography,
        shapes = PrumoShapes,
        content = content
    )
}
