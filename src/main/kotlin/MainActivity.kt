// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz

import android.graphics.Color.BLACK
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.leodesouza.blitz.ui.ClockScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(scrim = TRANSPARENT, darkScrim = BLACK),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            ClockScreen(
                onClockStart = { window.addFlags(FLAG_KEEP_SCREEN_ON) },
                onClockStop = { window.clearFlags(FLAG_KEEP_SCREEN_ON) },
            )
        }
    }
}
