// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Which edge the back gesture starts from. */
enum class SwipeEdge { LEFT, RIGHT }

/**
 * Effect for handling system back gestures that animates their progress until 100% upon completion.
 *
 * @param[enabledProvider] Lambda for whether the back handler should be enabled.
 * @param[onBackStart] Callback called when the back gesture starts.
 * @param[onCompletion] Callback called when the back gesture is complete.
 * @param[onCancellation] Callback called when the back gesture is cancelled.
 * @param[updateSwipeEdge] Callback called to update which edge the back gesture starts from.
 * @param[updateProgress] Callback called to update how far along the back gesture is.
 */
@Composable
fun AnimatedBackHandler(
    enabledProvider: () -> Boolean = { true },
    onBackStart: suspend () -> Unit = {},
    onCompletion: suspend () -> Unit = {},
    onCancellation: suspend () -> Unit = {},
    updateSwipeEdge: (swipeEdge: SwipeEdge) -> Unit = {},
    updateProgress: (progress: Float) -> Unit = {},
) {
    PredictiveBackHandler(enabled = enabledProvider()) { backEvent ->
        onBackStart()

        try {
            var progress = 0F

            backEvent.collect {
                progress = it.progress
                when (it.swipeEdge) {
                    BackEventCompat.EDGE_LEFT -> updateSwipeEdge(SwipeEdge.LEFT)
                    BackEventCompat.EDGE_RIGHT -> updateSwipeEdge(SwipeEdge.RIGHT)
                }
                updateProgress(progress)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                animate(initialValue = progress, targetValue = 1F) { value, _ ->
                    updateProgress(value)
                }
                delay(100L)
            }

            onCompletion()

        } catch (e: CancellationException) {
            onCancellation()

        } finally {
            updateProgress(0F)
        }
    }
}
