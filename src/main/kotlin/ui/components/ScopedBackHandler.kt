// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Handle presses of the system back button by executing [onBack] when the value returned by
 * [enabledProvider] is true, without causing recompositions of the parent composable when this
 * value changes.
 */
@Composable
fun ScopedBackHandler(enabledProvider: () -> Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabledProvider(), onBack = onBack)
}
