package com.computerfolder.app

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(width = 1100.dp, height = 860.dp)
    val viewModel = MainViewModel()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Computer Folder Service",
        state = windowState,
    ) {
        MaterialTheme {
            MainScreen(viewModel)
        }
    }
}
