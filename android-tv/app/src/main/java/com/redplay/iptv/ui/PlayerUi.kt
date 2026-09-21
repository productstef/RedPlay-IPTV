package com.redplay.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redplay.iptv.data.*
import com.redplay.iptv.player.VlcPlayerController
import com.redplay.iptv.player.VlcVideoSurface

fun PlayerPanel(
    state: RedPlayUiState,
    playerState: PlayerSnapshot,
    player: VlcPlayerController,
    vm: RedPlayViewModel,
    modifier: Modifier,
) {
    Column(modifier.fillMaxWidth().background(Color.Black).border(1.dp, RedPlayBorder)) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            VlcVideoSurface(player, state.playRequest, Modifier.fillMaxSize())
            Text(state.selectedChannel?.name ?: state.playRequest?.title.orEmpty(), Modifier.align(Alignment.TopStart).padding(14.dp), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            if (playerState.loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = RedPlayRed)
            if (playerState.error.isNotBlank()) {
                Surface(Modifier.align(Alignment.Center), color = RedPlayPanel2, shape = RoundedCornerShape(18.dp)) {
                    Text(playerState.error, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 12.sp)
                }
            }
        }
        PlayerControls(state, playerState, player, vm)
    }
}

@Composable
fun PlayerControls(state: RedPlayUiState, snapshot: PlayerSnapshot, player: VlcPlayerController, vm: RedPlayViewModel) {
    Column(Modifier.fillMaxWidth().background(RedPlayPanel).padding(horizontal = 10.dp, vertical = 8.dp)) {
        if (!state.playRequest?.isLive.orDefault(true) && snapshot.lengthMillis > 0) {
            Slider(
                value = snapshot.timeMillis.toFloat().coerceAtMost(snapshot.lengthMillis.toFloat()),
                onValueChange = { player.seekTo(it / snapshot.lengthMillis.toFloat()) },
                valueRange = 0f..snapshot.lengthMillis.toFloat(),
                colors = SliderDefaults.colors(thumbColor = RedPlayRed, activeTrackColor = RedPlayRed),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (!state.playRequest?.isLive.orDefault(true)) FocusButton("−10s", { player.skipBy(-10_000) }, compact = true)
            FocusButton(if (snapshot.playing) "Ⅱ" else "▶", player::playPause, compact = true)
            if (!state.playRequest?.isLive.orDefault(true)) FocusButton("+10s", { player.skipBy(10_000) }, compact = true)
            FocusButton(if (snapshot.muted) "Unmute" else "Mute", player::toggleMute, compact = true)
            FocusButton("Vol −", { player.setVolume(snapshot.volume - 5) }, compact = true)
            Text("${snapshot.volume}%", color = RedPlayMuted, fontSize = 11.sp)
            FocusButton("Vol +", { player.setVolume(snapshot.volume + 5) }, compact = true)
            Spacer(Modifier.weight(1f))
            FocusButton(if (state.selectedChannel?.let { state.favorites.contains(channelKey(it)) } == true) "♥ Favorite" else "♡ Favorite", vm::toggleFavorite, compact = true)
            FocusButton("Library", vm::openLibrary, compact = true)
            FocusButton("TV Guide", vm::toggleGuide, compact = true)
            FocusButton("⛶", vm::toggleFullscreen, compact = true)
        }
    }
}

fun Boolean?.orDefault(value: Boolean) = this ?: value

@Composable
fun ProgrammeCards(state: RedPlayUiState, vm: RedPlayViewModel, modifier: Modifier) {
    val (now, next) = vm.nowNext()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProgrammeCard("NOW PLAYING", now, true, Modifier.weight(1f))
        ProgrammeCard("UP NEXT", next, false, Modifier.weight(1f))
    }
}

@Composable
fun ProgrammeCard(label: String, p: Programme?, active: Boolean, modifier: Modifier) {
    Column(modifier.fillMaxHeight().background(RedPlayPanel2, RoundedCornerShape(8.dp)).border(1.dp, RedPlayBorder, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Text(label, color = if (active) RedPlayRed else RedPlayText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(p?.title ?: "No programme information", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (p != null) Text("${formatClock(p.startMillis)} – ${formatClock(p.stopMillis)}${p.category.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()}", color = RedPlayMuted, fontSize = 10.sp)
        p?.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = RedPlayMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
fun RightRail(state: RedPlayUiState, snapshot: PlayerSnapshot, player: VlcPlayerController, vm: RedPlayViewModel, modifier: Modifier) {
    Column(modifier.background(RedPlayPanel).border(1.dp, RedPlayBorder).padding(12.dp)) {
        Text("TV Guide", color = RedPlayRed, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val (now, next) = vm.nowNext()
        Text("Now on ${state.selectedChannel?.name ?: "channel"}", fontSize = 11.sp, color = RedPlayMuted)
        Text(now?.title ?: "No EPG data", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
        if (next != null) Text("Next: ${next.title} • ${formatClock(next.startMillis)}", color = RedPlayMuted, fontSize = 10.sp, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        TrackSelector("Audio Tracks", snapshot.audioTracts, snapshot.selectedAudio, player::selectAudio)
        Spacer(Modifier.height(8.dp))
        TrackSelector("Subtitles", snapshot.subtitleTracks, snapshot.selectedSubtitle, player::selectSubtitle)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Subtitle size", fontSize = 11.sp, modifier = Modifier.weight(1f))
            FocusButton("−", { val size = state.subtitleSize - 2; vm.setSubtitleSize(size); player.setSubtitleSize(size) }, compact = true)
            Text(" ${state.subtitleSize} ", color = RedPlayMuted, fontSize = 10.sp)
            FocusButton("+", { val size = state.subtitleSize + 2; vm.setSubtitleSize(size); player.setSubtitleSize(size) }, compact = true)
        }
        Spacer(Modifier.weight(1f))
        Text("Stream Information", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(if (state.playRequest?.isLive == true) "Live stream" else "Library / VOD", color = RedPlayMuted, fontSize = 10.sp)
        Text(state.playRequest?.url.orEmpty(), color = RedPlayMuted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(state.status, color = Color(0xFF42D392), fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun TrackSelector(label: String, tracks: List<TrackOption>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        FocusSurface(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(tracks.firstOrNull { it.id == selected }?.title ?: if (label == "Subtitles") "Off" else "Default", Modifier.padding(9.dp), fontSize = 11.sp, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tracks.forEach { track -> DropdownMenuItem(text = { Text(track.title) }, onClick = { onSelect(track.id); expanded = false }) }
        }
    }
}

@Composable
fun FullscreenPlayer(state: RedPlayUiState, player: VlcPlayerController, vm: RedPlayViewModel) {
    val snapshot by player.snapshot.collectAsState()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VlcVideoSurface(player, state.playRequest, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xCC070B11)).padding(12.dp)) {
            Text(state.playRequest?.title.orEmpty(), fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusButton(if (snapshot.playing) "Ⅱ" else "▶", player::playPause, compact = true)
                if (state.playRequest?.isLive == false) {
                    FocusButton("−10s", { player.skipBy(-10_000) }, compact = true)
                    FocusButton("+10s", { player.skipBy(10_000) }, compact = true)
                }
                Spacer(Modifier.weight(1f))
                FocusButton("Exit fullscreen", vm::exitFullscreen, compact = true)
            }
        }
    }
}

@Composable
