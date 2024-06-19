// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.io

import android.media.AudioManager
import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_VIBRATE
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Use the [audioManager] interface to play a sound effect when the ringtone mode is set to
 * [RINGER_MODE_NORMAL], or use the [haptics] interface to provide haptic feedback when
 * the ringtone mode is set to [RINGER_MODE_VIBRATE].
 */
fun clockFeedback(audioManager: AudioManager, haptics: HapticFeedback) {
    when (audioManager.ringerMode) {
        RINGER_MODE_NORMAL -> with(audioManager) {
            val streamType = AudioManager.STREAM_MUSIC
            val volumeIndex = getStreamVolume(streamType).toFloat()
            val maxVolumeIndex = getStreamMaxVolume(streamType).toFloat()
            val volume = volumeIndex / maxVolumeIndex
            playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, volume)
        }

        RINGER_MODE_VIBRATE -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
