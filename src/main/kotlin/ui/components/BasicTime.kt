// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlin.time.Duration

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
    autoSize: TextAutoSize? = null,
    timeOverColor: Color = style.color,
) {
    val time = timeProvider()

    BasicText(
        text = time.toComponents { hours, minutes, seconds, nanoseconds ->
            val paddedMinutes = minutes.toString().padStart(length = 2, padChar = '0')
            val paddedSeconds = seconds.toString().padStart(length = 2, padChar = '0')
            if (hours < 1) {
                val decimal = nanoseconds.toString().padStart(length = 9, padChar = '0').first()
                "$paddedMinutes:$paddedSeconds.$decimal"
            } else {
                "$hours:$paddedMinutes:$paddedSeconds"
            }
        },
        modifier = modifier,
        style = style.merge(color = if (time.isPositive()) style.color else timeOverColor),
        softWrap = false,
        autoSize = autoSize,
    )
}
