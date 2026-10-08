package com.hermes.taskmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TodoistRed = Color(0xFFDB4C3F)
val TodoistRedDark = Color(0xFFC53B27)
val P1Red = Color(0xFFDE4C4A)
val P2Orange = Color(0xFFF49C18)
val P3Blue = Color(0xFF4073D6)
val P4Grey = Color(0xFF808080)

val PriorityP1 = P1Red
val PriorityP2 = P2Orange
val PriorityP3 = P3Blue
val PriorityP4 = P4Grey

private val LightColorScheme = lightColorScheme(
    primary = TodoistRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF775651),
    background = Color(0xFFFCFCFC),
    surface = Color.White,
    onSurface = Color(0xFF1F1F1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = TodoistRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF930006),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFE7BDB6),
    background = Color(0xFF18181B),
    surface = Color(0xFF27272A),
    onSurface = Color(0xFFF4F4F5)
)

@Composable
fun HermesTasksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
