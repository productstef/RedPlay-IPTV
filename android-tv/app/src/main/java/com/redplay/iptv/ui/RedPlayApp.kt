package com.redplay.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redplay.iptv.data.*
import com.redplay.iptv.player.VlcPlayerController

fun RedPlayApp(vm: RedPlayViewModel, player: VlcPlayerController) {
    val state by vm.state.collectAsState()
    val playerState by player.snapshot.collectAsState()
    LaunchedEffect(state.subtitleSize) { player.setSubtitleSize(state.subtitleSize) }

    if (state.fullscreenPlayer) {
        FullscreenPlayer(state, player, vm)
        return
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(RedPlayBackground)
    ) {
        val wide = maxWidth >= 1100.dp
        Column(Modifier.fillMaxSize()) {
            TopBar(state, vm)
            Row(Modifier.fillMaxSize()) {
                ProviderChannelRail(state, vm, Modifier.width(if (wide) 300.dp else 260.dp).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight().padding(10.dp)) {
                    PlayerPanel(state, playerState, player, vm, Modifier.weight(1f))
                    Spacer(Modifier.height(10.dp))
                    ProgrammeCards(state, vm, Modifier.heightIn(min = 110.dp, max = 160.dp))
                    if (!wide) {
                        Spacer(Modifier.height(8.dp))
                        RightRail(state, playerState, player, vm, Modifier.height(190.dp))
                    }
                }
                if (wide) RightRail(state, playerState, player, vm, Modifier.width(305.dp).fillMaxHeight())
            }
        }
    }

    if (state.screen == Screen.Library) LibraryDialog(state, vm)
    if (state.guideOpen) GuideDialog(state, vm)
    if (state.providerDialogOpen) ProviderDialog(vm)
}

@Composable
fun TopBar(state: RedPlayUiState, vm: RedPlayViewModel) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).background(RedPlayPanel).border(1.dp, RedPlayBorder).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(RedPlayRed), contentAlignment = Alignment.Center) {
            Text("▶", color = Color.White, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text("Red", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("Play", color = RedPlayRed, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(" IPTV", color = RedPlayMuted, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        OutlinedTextField(
            value = state.search,
            onValueChange = vm::setSearch,
            placeholder = { Text("Search channels or programmes…", color = RedPlayMuted) },
            singleLine = true,
            modifier = Modifier.widthIn(min = 280.dp, max = 520.dp).height(52.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RedPlayRed,
                unfocusedBorderColor = RedPlayBorder,
                focusedContainerColor = RedPlayPanel2,
                unfocusedContainerColor = RedPlayPanel2,
            ),
        )
        Spacer(Modifier.width(10.dp))
        FocusButton("↻", onClick = vm::refresh, compact = true)
    }
}

@Composable
