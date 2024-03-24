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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Effect to handle whether the device is leaning left or right. Call the callback
 * [onLeaningChanged] when the device orientation changes, whose argument is a Boolean representing
 * whether the device is currently leaning towards its right side.
 */
@Composable
fun IsLeaningRightListener(onLeaningChanged: (isLeaningRight: Boolean) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = object : DefaultLifecycleObserver {
            private val orientationEventListener by lazy {
                object : OrientationEventListener(context) {
                    override fun onOrientationChanged(orientation: Int) {
                        if (orientation == ORIENTATION_UNKNOWN) return
                        val rotation = when (ContextCompat.getDisplayOrDefault(context).rotation) {
                            Surface.ROTATION_0 -> 0
                            Surface.ROTATION_90 -> 90
                            Surface.ROTATION_180 -> 180
                            else -> 270
                        }
                        when ((orientation + rotation) % 360) {
                            in 10 until 170 -> onLeaningChanged(true)
                            in 190 until 350 -> onLeaningChanged(false)
                        }
                    }
                }
            }

            override fun onStart(owner: LifecycleOwner) = orientationEventListener.enable()

            override fun onStop(owner: LifecycleOwner) = orientationEventListener.disable()
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
}
