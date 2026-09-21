package com.redplay.iptv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RedPlayBackground = Color(0xFF070B11)
val RedPlayPanel = Color(0xFF0A0F17)
val RedPlayPanel2 = Color(0xFF0D141D)
val RedPlayBorder = Color(0xFF171F2A)
val RedPlayRed = Color(0xFFFF3B48)
val RedPlayText = Color(0xFFF4F6F8)
val RedPlayMuted = Color(0xFF8B98AA)

private val scheme = darkColorScheme(
    primary = RedPlayRed,
    onPrimary = Color.White,
    background = RedPlayBackground,
    surface = RedPlayPanel,
    surfaceVariant = RedPlayPanel2,
    onBackground = RedPlayText,
    onSurface = RedPlayText,
    outline = RedPlayBorder,
)

@Composable
fun RedPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
