// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Where a back event comes from: a [SWIPE] from the edge of the screen, or a [BUTTON]. */
enum class BackSource { SWIPE, BUTTON }

/** Which edge the back gesture starts from. */
enum class SwipeEdge { LEFT, RIGHT }

/**
 * Effect for handling system back events that animates their progress until 100% upon completion.
 *
 * @param[enabledProvider] Lambda for whether the back handler should be enabled.
 * @param[onBackStart] Callback called when a back event starts, with where it comes from.
 * @param[onCompletion] Callback called when the back event is complete.
 * @param[onCancellation] Callback called when the back gesture is cancelled.
 * @param[updateSwipeEdge] Callback called to update which edge the back gesture starts from.
 * @param[updateProgress] Callback called to update how far along the back gesture is.
 */
@Composable
fun AnimatedBackHandler(
    enabledProvider: () -> Boolean = { true },
    onBackStart: suspend (backSource: BackSource) -> Unit = {},
    onCompletion: suspend () -> Unit = {},
    onCancellation: suspend () -> Unit = {},
    updateSwipeEdge: (swipeEdge: SwipeEdge) -> Unit = {},
    updateProgress: (progress: Float) -> Unit = {},
) {
    PredictiveBackHandler(enabled = enabledProvider()) { backEvent ->
        var isStarted = false  // its source is only known once it reports progress

        try {
            var progress = 0F

            backEvent.collect {
                if (!isStarted) {
                    isStarted = true
                    val isSwipe = it.swipeEdge != BackEventCompat.EDGE_NONE
                    onBackStart(if (isSwipe) BackSource.SWIPE else BackSource.BUTTON)
                }
                progress = it.progress
                when (it.swipeEdge) {
                    BackEventCompat.EDGE_LEFT -> updateSwipeEdge(SwipeEdge.LEFT)
                    BackEventCompat.EDGE_RIGHT -> updateSwipeEdge(SwipeEdge.RIGHT)
                }
                updateProgress(progress)
            }

            if (!isStarted) {
                isStarted = true
                onBackStart(BackSource.BUTTON)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                animate(initialValue = progress, targetValue = 1F) { value, _ ->
                    updateProgress(value)
                }
                delay(100L)
            }

            onCompletion()

        } catch (e: CancellationException) {
            if (!isStarted) onBackStart(BackSource.BUTTON)
            onCancellation()
            throw e

        } finally {
            updateProgress(0F)
        }
    }
}
