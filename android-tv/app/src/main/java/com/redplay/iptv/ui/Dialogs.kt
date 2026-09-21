package com.redplay.iptv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.redplay.iptv.data.*

fun LibraryDialog(state: RedPlayUiState, vm: RedPlayViewModel) {
    Dialog(onDismissRequest = vm::closeLibrary, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f), color = RedPlayBackground, shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, RedPlayBorder)) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(state.library?.title ?: "Library", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("On-demand originals from the Western Channel", color = RedPlayMuted, fontSize = 11.sp)
                    }
                    FocusButton("Back to Live TV", vm::closeLibrary)
                }
                Spacer(Modifier.height(12.dp))
                if (state.libraryLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = RedPlayRed)
                if (state.libraryError.isNotBlank()) Text(state.libraryError, color = RedPlayRed, modifier = Modifier.padding(vertical = 8.dp))
                val movies = state.library?.movies.orEmpty()
                if (movies.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(210.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        gridItems(movies, key = { it.title }) { movie ->
                            FocusSurface(onClick = { vm.playLibraryMovie(movie) }, modifier = Modifier.height(130.dp).fillMaxWidth()) {
                                Column(Modifier.fillMaxSize().padding(12.dp)) {
                                    Text(movie.displayTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (movie.year.isNotBlank()) Text(movie.year, color = RedPlayRed, fontSize = 11.sp)
                                    Text(movie.description, color = RedPlayMuted, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp).weight(1f))
                                    Text("OK to play", color = RedPlayText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideDialog(state: RedPlayUiState, vm: RedPlayViewModel) {
    Dialog(onDismissRequest = vm::toggleGuide, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.78f).fillMaxHeight(0.86f), color = RedPlayBackground, shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, RedPlayBorder)) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TV Guide", color = RedPlayRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(state.selectedChannel?.name ?: "Channel", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    FocusButton("Close", vm::toggleGuide)
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lazyItems(vm.programmes().filter { it.stopMillis >= System.currentTimeMillis() }.take(100)) { p ->
                        FocusSurface(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("${formatClock(p.startMillis)}\n${formatClock(p.stopMillis)}", color = RedPlayMuted, fontSize = 11.sp, modifier = Modifier.width(72.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.title, fontWeight = FontWeight.Bold)
                                    if (p.description.isNotBlank()) Text(p.description, color = RedPlayMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderDialog(vm: RedPlayViewModel) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var epg by remember { mutableStateOf("") }
    Dialog(onDismissRequest = vm::closeProviderDialog) {
        Surface(Modifier.widthIn(min = 520.dp, max = 720.dp), color = RedPlayPanel, shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, RedPlayBorder)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add IPTV provider", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Remote M3U/M3U8 provider", color = RedPlayMuted, fontSize = 11.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("M3U / M3U8 playlist URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(epg, { epg = it }, label = { Text("EPG URL (optional - auto detected from M3U)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FocusButton("Cancel", vm::closeProviderDialog)
                    Spacer(Modifier.width(8.dp))
                    FocusButton("Save provider", { vm.addProvider(name, url, epg) }, enabled = name.isNotBlank() && url.isNotBlank())
                }
            }
        }
    }
}

@Composable
