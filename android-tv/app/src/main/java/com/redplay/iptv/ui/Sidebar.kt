package com.redplay.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redplay.iptv.data.*

fun ProviderChannelRail(state: RedPlayUiState, vm: RedPlayViewModel, modifier: Modifier) {
    Column(modifier.background(RedPlayPanel).border(1.dp, RedPlayBorder).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Playlists / Providers", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            FocusButton("+ Add", vm::openProviderDialog, compact = true)
        }
        Spacer(Modifier.height(8.dp))
        state.providers.forEach { provider ->
            FocusSurface(
                onClick = { vm.loadProvider(provider) },
                selected = state.currentProvider?.id == provider.id,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (provider.builtIn) RedPlayRed else RedPlayPanel2), contentAlignment = Alignment.Center) {
                        Text(if (provider.name.contains("western", true)) "W" else if (provider.name.contains("star", true)) "ST" else "P", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(provider.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(provider.url, color = RedPlayMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FocusButton("Refresh", vm::refresh, compact = true, modifier = Modifier.weight(1f))
            FocusButton("Delete", vm::deleteCurrentProvider, compact = true, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Text("Channels", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        GroupSelector(state, vm)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FocusButton(if (state.favoritesOnly) "★ Favorites" else "☆ Favorites", vm::toggleFavoritesOnly, compact = true, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = RedPlayBorder)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 5.dp)) {
            items(vm.filteredChannels(), key = { channelKey(it) }) { channel ->
                val selected = state.selectedChannel?.url == channel.url
                FocusSurface(
                    onClick = { vm.selectChannel(channel) },
                    selected = selected,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(RedPlayPanel2), contentAlignment = Alignment.Center) {
                            Text(channel.name.take(2).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                            Text(channel.group, color = RedPlayMuted, fontSize = 10.sp, maxLines = 1)
                        }
                        if (state.favorites.contains(channelKey(channel))) Text("♥", color = RedPlayRed, fontSize = 12.sp)
                    }
                }
            }
        }
        Text(state.status, color = RedPlayMuted, fontSize = 10.sp, maxLines = 2)
    }
}

@Composable
fun GroupSelector(state: RedPlayUiState, vm: RedPlayViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FocusSurface(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.group, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 1)
                Text("⌄", color = RedPlayMuted)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            vm.groups().forEach { group -> DropdownMenuItem(text = { Text(group) }, onClick = { vm.setGroup(group); expanded = false }) }
        }
    }
}

@Composable
