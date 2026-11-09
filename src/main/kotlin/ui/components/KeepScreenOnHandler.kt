// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn

/**
 * Keep the screen on while the value returned by [enabledProvider] is true, without causing
 * recompositions of the parent composable when this value changes.
 */
@Composable
fun KeepScreenOnHandler(enabledProvider: () -> Boolean) {
    if (enabledProvider()) {
        Spacer(Modifier.keepScreenOn())
    }
}
