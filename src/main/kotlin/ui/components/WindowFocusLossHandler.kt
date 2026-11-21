// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Call [onWindowFocusLoss] whenever the window loses focus, such as when a system dialog or the
 * notification shade covers it, without causing recompositions when the focus changes.
 */
@Composable
fun WindowFocusLossHandler(onWindowFocusLoss: () -> Unit) {
    val currentOnWindowFocusLoss by rememberUpdatedState(onWindowFocusLoss)
    val windowInfo = LocalWindowInfo.current

    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { isWindowFocused ->
            if (!isWindowFocused) currentOnWindowFocusLoss()
        }
    }
}
