package com.redplay.iptv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusButton(text: String, onClick: () -> Unit, compact: Boolean = false, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FocusSurface(onClick = onClick, modifier = modifier, enabled = enabled) {
        Box(Modifier.height(if (compact) 38.dp else 46.dp).padding(horizontal = if (compact) 10.dp else 16.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) RedPlayText else RedPlayMuted)
        }
    }
}

@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, label = "focusScale")
    val border by animateColorAsState(if (focused || selected) RedPlayRed else RedPlayBorder, label = "focusBorder")
    val bg by animateColorAsState(if (selected) Color(0xFF2A171B) else if (focused) Color(0xFF151D28) else RedPlayPanel2, label = "focusBg")
    Box(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(if (focused || selected) 2.dp else 1.dp, border, RoundedCornerShape(7.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interaction)
    ) { content() }
}

fun formatClock(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
