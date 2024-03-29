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

import androidx.compose.runtime.Composable

/** Which side the device is currently leaning towards. */
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
