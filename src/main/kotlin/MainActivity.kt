// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz

import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import net.leodesouza.blitz.ui.ClockScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.Transparent.toArgb(),
                darkScrim = Color.Black.toArgb(),
            ),
        )

        super.onCreate(savedInstanceState)

        setContent {
            ClockScreen(
                onClockStart = { window.addFlags(FLAG_KEEP_SCREEN_ON) },
                onClockStop = { window.clearFlags(FLAG_KEEP_SCREEN_ON) },
            )
        }
    }
}
