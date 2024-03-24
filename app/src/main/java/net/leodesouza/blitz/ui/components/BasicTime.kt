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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection

/**
 * Basic element that displays the time returned by a [timeProvider] in the format "MM:SS.D" or
 * "H:MM:SS.D" depending on whether there is more than one hour, in a given [style] and accepting a
 * given [modifier] to apply to its layout node. Change the text color to [timeOverColor] when the
 * time is over if specified.
 */
@Composable
fun BasicTime(
    timeProvider: () -> Long,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    timeOverColor: Color = style.color,
) {
    val timeMillis = timeProvider()
    val timeTenthsOfSeconds = (timeMillis + 99L) / 100L  // round up to the nearest tenth of second
    val hours = timeTenthsOfSeconds / 36_000L
    val minutes = timeTenthsOfSeconds % 36_000L / 600L
    val seconds = timeTenthsOfSeconds % 600L / 10L
    val tenthsOfSeconds = timeTenthsOfSeconds % 10L
    val defaultStyle = if (timeMillis > 0L) style else style.merge(color = timeOverColor)
    val monospaceStyle = defaultStyle.merge(fontFamily = FontFamily.Monospace)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier) {
            if (hours != 0L) {
                BasicText(text = "$hours", style = monospaceStyle)
                BasicText(text = ":", style = defaultStyle)
            }
            BasicText(text = "$minutes".padStart(length = 2, padChar = '0'), style = monospaceStyle)
            BasicText(text = ":", style = defaultStyle)
            BasicText(text = "$seconds".padStart(length = 2, padChar = '0'), style = monospaceStyle)
            if (hours == 0L) {
                BasicText(text = ".", style = defaultStyle)
                BasicText(text = "$tenthsOfSeconds", style = monospaceStyle)
            }
        }
    }
}
