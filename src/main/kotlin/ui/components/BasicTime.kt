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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * Basic element that displays the time returned by a [timeProvider] in the format "H:MM:SS" or
 * "MM:SS.D" depending on whether the time is higher than one hour, in a given [style] and accepting
 * a given [modifier] to apply to its layout node. Change the text color to [timeOverColor] when the
 * time is not greater than zero if specified.
 */
@Composable
fun BasicTime(
    timeProvider: () -> Duration,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    timeOverColor: Color = style.color,
) {
    val time = 100.milliseconds * floor(timeProvider() / 100.milliseconds)
    val punctuationStyle = if (time.isPositive()) style else style.merge(color = timeOverColor)
    val digitStyle = punctuationStyle.merge(fontFamily = FontFamily.Monospace)
    val hoursText: String
    val minutesText: String
    val secondsText: String
    val decimalText: String

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        hoursText = hours.toString()
        minutesText = minutes.toString().padStart(length = 2, padChar = '0')
        secondsText = seconds.toString().padStart(length = 2, padChar = '0')
        decimalText = (ceil(nanoseconds / 100_000_000.0).toInt()).toString()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier) {
            if (time >= 1.hours) {
                BasicText(text = hoursText, style = digitStyle)
                BasicText(text = ":", style = punctuationStyle)
            }
            BasicText(text = minutesText, style = digitStyle)
            BasicText(text = ":", style = punctuationStyle)
            BasicText(text = secondsText, style = digitStyle)
            if (time < 1.hours) {
                BasicText(text = ".", style = punctuationStyle)
                BasicText(text = decimalText, style = digitStyle)
            }
        }
    }
}
