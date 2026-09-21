package com.redplay.iptv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.redplay.iptv.data.RedPlayViewModel
import com.redplay.iptv.data.Screen
import com.redplay.iptv.player.VlcPlayerController
import com.redplay.iptv.ui.RedPlayApp
import com.redplay.iptv.ui.RedPlayTheme

class MainActivity : ComponentActivity() {
    private lateinit var playerController: VlcPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playerController = VlcPlayerController(this)
        setContent {
            val vm: RedPlayViewModel = viewModel()
            val state by vm.state.collectAsState()
            RedPlayTheme {
                BackHandler(state.fullscreenPlayer) { vm.exitFullscreen() }
                BackHandler(state.screen == Screen.Library && !state.fullscreenPlayer) { vm.closeLibrary() }
                RedPlayApp(vm, playerController)
            }
        }
    }

    override fun onDestroy() {
        playerController.release()
        super.onDestroy()
    }
}
