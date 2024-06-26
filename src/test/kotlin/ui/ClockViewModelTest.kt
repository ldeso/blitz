// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
import kotlin.math.roundToInt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class ClockViewModelTest {
    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val duration: Duration = 5.minutes
    private val increment: Duration = 3.seconds
    private val tickPeriod: Duration = 100.milliseconds
    private val initialTime: Duration = duration + increment
    private val delayTime: Duration = (tickPeriod + duration) / 2
    private val addMinutes: Float = 1.2F
    private val addSeconds: Float = -1.8F
    private val clockViewModel: ClockViewModel = ClockViewModel(
        durationMinutes = duration.inWholeMinutes.toInt(),
        incrementSeconds = increment.inWholeSeconds.toInt(),
        tickPeriodMillis = tickPeriod.inWholeMilliseconds.toInt(),
        timeSource = scheduler.timeSource,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization, clockState is FULL_RESET`() {
        assertEquals(ClockState.FULL_RESET, clockViewModel.clockState.value)
    }

    @Test
    fun `initialization, playerState is WHITE`() {
        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }

    @Test
    fun `initialization, whiteTime and blackTime are sum of duration and increment`() {
        assertEquals(initialTime, clockViewModel.whiteTime.value)
        assertEquals(initialTime, clockViewModel.blackTime.value)
    }

    @Test
    fun `save-restore, clock state is FULL_RESET if default config`() = runTest {
        clockViewModel.save()
        clockViewModel.restore()

        assertEquals(ClockState.FULL_RESET, clockViewModel.clockState.value)
    }

    @Test
    fun `save-restore, clock state is SOFT_RESET if custom config`() = runTest {
        clockViewModel.save()
        clockViewModel.restore(addMinutes = addMinutes, addSeconds = addSeconds)

        assertEquals(ClockState.SOFT_RESET, clockViewModel.clockState.value)
    }

    @Test
    fun `save-restore, whiteTime and blackTime are set`() = runTest {
        clockViewModel.save()
        clockViewModel.restore(addMinutes = addMinutes, addSeconds = addSeconds)

        val expectedTime = initialTime.toComponents { minutes, seconds, nanoseconds ->
            val newMinutes = minutes.toFloat() + addMinutes
            val newSeconds = seconds.toFloat() + nanoseconds.toFloat() / 1_000_000_000F + addSeconds
            newMinutes.roundToInt().minutes + newSeconds.roundToInt().seconds
        }
        assertEquals(expectedTime, clockViewModel.whiteTime.value)
        assertEquals(expectedTime, clockViewModel.blackTime.value)
    }

    @Test
    fun `save-restore-reset, clockState is FULL_RESET`() = runTest {
        clockViewModel.save()
        clockViewModel.restore(addMinutes = addMinutes, addSeconds = addSeconds)
        clockViewModel.reset()

        assertEquals(ClockState.FULL_RESET, clockViewModel.clockState.value)
    }

    @Test
    fun `save-restore-start-advance-reset, clockState is SOFT_RESET`() = runTest {
        clockViewModel.save()
        clockViewModel.restore(addMinutes = addMinutes, addSeconds = addSeconds)
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.reset()

        assertEquals(ClockState.SOFT_RESET, clockViewModel.clockState.value)
    }

    @Test
    fun `save-restore-start-advance-reset-reset, clockState is FULL_RESET`() = runTest {
        clockViewModel.save()
        clockViewModel.restore(addMinutes = addMinutes, addSeconds = addSeconds)
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.reset()
        clockViewModel.reset()

        assertEquals(ClockState.FULL_RESET, clockViewModel.clockState.value)
    }

    @Test
    fun `start, clockState is TICKING`() = runTest {
        clockViewModel.start()

        assertEquals(ClockState.TICKING, clockViewModel.clockState.value)
    }

    @Test
    fun `start-advance, blackTime does not change`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)

        assertEquals(initialTime, clockViewModel.blackTime.value)
    }

    @Test
    fun `start-advance, whiteTime is set to the next multiple of tickPeriod`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)

        assertEquals(
            tickPeriod * (((initialTime - delayTime) / tickPeriod).toInt() + 1),
            clockViewModel.whiteTime.value,
        )
    }

    @Test
    fun `start-advance-pause, clockState is PAUSED`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.pause()

        assertEquals(ClockState.PAUSED, clockViewModel.clockState.value)
    }

    @Test
    fun `start-advance-pause, whiteTime is set to exact current value`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.pause()

        assertEquals(initialTime - delayTime, clockViewModel.whiteTime.value)
    }

    @Test
    fun `start-advance-pause-advance, whiteTime does not change after pause`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.pause()
        val expectedTime = clockViewModel.whiteTime.value
        advanceTimeBy(delayTime)

        assertEquals(expectedTime, clockViewModel.whiteTime.value)
    }

    @Test
    fun `start-advance-pause-save-restore, blackTime does not change`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.pause()
        clockViewModel.save()
        clockViewModel.restore(addMinutes = addMinutes, addSeconds = addSeconds)

        assertEquals(initialTime, clockViewModel.blackTime.value)
    }

    @Test
    fun `start-advance-pause-save-restore, whiteTime is set and not rounded`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.pause()
        clockViewModel.save()
        clockViewModel.restore(
            addMinutes = addMinutes, addSeconds = addSeconds, isDecimalRestored = true,
        )

        val newMinutes: Float
        val newSeconds: Float
        (initialTime - delayTime).toComponents { minutes, seconds, nanoseconds ->
            newMinutes = minutes.toFloat() + addMinutes
            newSeconds = seconds.toFloat() + nanoseconds.toFloat() / 1_000_000_000F + addSeconds
        }
        assertEquals(
            newMinutes.roundToInt().minutes + (newSeconds * 1_000F).roundToInt().milliseconds,
            clockViewModel.whiteTime.value,
        )
    }

    @Test
    fun `start-advance-pause-save-restore, whiteTime is set and rounded`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.pause()
        clockViewModel.save()
        clockViewModel.restore(addMinutes = addMinutes, addSeconds = addSeconds)

        val expectedTime = (initialTime - delayTime).toComponents { minutes, seconds, nanoseconds ->
            val newMinutes = minutes.toFloat() + addMinutes
            val newSeconds = seconds.toFloat() + nanoseconds.toFloat() / 1_000_000_000F + addSeconds
            newMinutes.roundToInt().minutes + newSeconds.roundToInt().seconds
        }
        assertEquals(expectedTime, clockViewModel.whiteTime.value)
    }

    @Test
    fun `start-advance-play, playerState is BLACK`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.play()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `start-advance-play, whiteTime is set to exact current value plus increment`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.play()

        assertEquals(initialTime - delayTime + increment, clockViewModel.whiteTime.value)
    }

    @Test
    fun `start-advance-play-advance, whiteTime does not change after play`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.play()
        val expectedWhiteTime = clockViewModel.whiteTime.value
        advanceTimeBy(delayTime)

        assertEquals(expectedWhiteTime, clockViewModel.whiteTime.value)
    }

    @Test
    fun `start-advance-play-advance-play, playerState is WHITE`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.play()
        advanceTimeBy(delayTime)
        clockViewModel.play()

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }

    @Test
    fun `start-advance-play-advance-reset, clockState is FULL_RESET`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.play()
        advanceTimeBy(delayTime)
        clockViewModel.reset()

        assertEquals(ClockState.FULL_RESET, clockViewModel.clockState.value)
    }

    @Test
    fun `start-advance-play-advance-reset, playerState is WHITE`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.play()
        advanceTimeBy(delayTime)
        clockViewModel.reset()

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }

    @Test
    fun `start-advance-play-advance-reset, whiteTime and blackTime are reset`() = runTest {
        clockViewModel.start()
        advanceTimeBy(delayTime)
        clockViewModel.play()
        advanceTimeBy(delayTime)
        clockViewModel.reset()

        assertEquals(initialTime, clockViewModel.whiteTime.value)
        assertEquals(initialTime, clockViewModel.blackTime.value)
    }

    @Test
    fun `start-idle, clockState is FINISHED`() = runTest {
        clockViewModel.start()
        advanceUntilIdle()

        assertEquals(ClockState.FINISHED, clockViewModel.clockState.value)
    }

    @Test
    fun `start-idle, whiteTime is zero`() = runTest {
        clockViewModel.start()
        advanceUntilIdle()

        assertEquals(Duration.ZERO, clockViewModel.whiteTime.value)
    }
}
