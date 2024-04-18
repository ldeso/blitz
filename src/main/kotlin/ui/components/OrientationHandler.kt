// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect

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

    val orientationEventListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) {
                return
            } else {
                currentOnOrientationChanged((orientation + rotation) % 360)
            }
        }
    }

    LifecycleStartEffect(lifecycleOwner) {
        orientationEventListener.enable()

        onStopOrDispose {
            orientationEventListener.disable()
        }
    }
}
