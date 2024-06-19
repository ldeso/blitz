// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.components

import androidx.compose.runtime.Composable

/**
 * Run an [effect] when the value returned by [enabledProvider] is true, without causing
 * recompositions of the parent composable when this value changes.
 */
@Composable
fun ScopedEffectHandler(enabledProvider: () -> Boolean, effect: () -> Unit) {
    val enabled = enabledProvider()

    if (enabled) {
        effect()
    }
}
