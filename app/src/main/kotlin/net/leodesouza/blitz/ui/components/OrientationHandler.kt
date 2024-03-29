/*
 * Copyright 2024 Léo de Souza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.leodesouza.blitz.ui.components

import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Observe the orientation of the device in a lifecycle-aware manner and call [onOrientationChanged]
 * when it changes, with the new orientation in degrees as an argument. Take into account the
 * rotation of the screen from its "natural" orientation.
 */
@Composable
fun OrientationHandler(onOrientationChanged: (orientation: Int) -> Unit) {
    val currentOnOrientationChanged by rememberUpdatedState(onOrientationChanged)
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val display = ContextCompat.getDisplayOrDefault(context)

    val rotation = when (display.rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        else -> 270
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            private val orientationEventListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == ORIENTATION_UNKNOWN) return
                    currentOnOrientationChanged((orientation + rotation) % 360)
                }
            }

            override fun onStart(owner: LifecycleOwner) = orientationEventListener.enable()

            override fun onStop(owner: LifecycleOwner) = orientationEventListener.disable()
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
