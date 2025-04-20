// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import androidx.compose.runtime.Composable

/** Whether the device is currently leaning towards its [LEFT] side or its [RIGHT] side. */
enum class LeaningSide { LEFT, RIGHT }

/**
 * Call [onLeaningSideChanged] when the side that the device is currently leaning towards, which is
 * calculated from the orientation in degrees returned by [orientationProvider], becomes different
 * to the [LeaningSide] that is returned by [leaningSideProvider].
 */
@Composable
fun LeaningSideHandler(
    orientationProvider: () -> Int,
    leaningSideProvider: () -> LeaningSide,
    onLeaningSideChanged: () -> Unit,
) {
    val orientation = orientationProvider()
    val leaningSide = leaningSideProvider()

    when (leaningSide) {
        LeaningSide.LEFT -> if (orientation in 10 until 170) onLeaningSideChanged()
        LeaningSide.RIGHT -> if (orientation in 190 until 350) onLeaningSideChanged()
    }
}
