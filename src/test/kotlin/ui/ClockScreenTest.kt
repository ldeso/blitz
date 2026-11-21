// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
class ClockScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val timeSource: TestTimeSource = TestTimeSource()
    private val clockViewModel: ClockViewModel = ClockViewModel(
        durationMinutes = 5,
        incrementSeconds = 3,
        tickPeriodMillis = 100,
        timeSource = timeSource,
    )

    @BeforeTest
    fun setUp() {
        composeTestRule.setContent {
            ClockScreen(clockViewModel = clockViewModel)
        }
    }

    private fun tap() {
        composeTestRule.onRoot().performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()
        timeSource += 10.seconds
    }

    private fun hover() {
        composeTestRule.onRoot().performMouseInput {
            moveTo(center)
            moveTo(centerRight)
            scroll(delta = 1F)
        }
        composeTestRule.waitForIdle()
    }

    private fun backSwipe(isCancelled: Boolean = false) {
        val dispatcher = composeTestRule.activity.onBackPressedDispatcher
        composeTestRule.onRoot().performTouchInput {
            down(centerLeft)
            cancel()
        }
        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(backEvent(progress = 0F))
            dispatcher.dispatchOnBackProgressed(backEvent(progress = 0.5F))
            if (isCancelled) dispatcher.dispatchOnBackCancelled() else dispatcher.onBackPressed()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        composeTestRule.waitForIdle()
    }

    private fun backEvent(progress: Float): BackEventCompat = BackEventCompat(
        touchX = 0F, touchY = 0F, progress = progress, swipeEdge = BackEventCompat.EDGE_LEFT,
    )

    @Test
    fun `tap, clockState is TICKING`() {
        tap()

        assertEquals(ClockState.TICKING, clockViewModel.clockState.value)
    }

    @Test
    fun `tap-tap, playerState is BLACK`() {
        tap()
        tap()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-hover, playerState is WHITE`() {
        tap()
        hover()

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-hover, blackTime does not change`() {
        tap()
        hover()

        assertEquals(5.minutes + 3.seconds, clockViewModel.blackTime.value)
    }

    @Test
    fun `tap-tap-backSwipe, clockState is PAUSED`() {
        tap()
        tap()
        backSwipe()

        assertEquals(ClockState.PAUSED, clockViewModel.clockState.value)
    }

    @Test
    fun `tap-tap-backSwipe, playerState is BLACK`() {
        tap()
        tap()
        backSwipe()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-tap-cancelledBackSwipe, clockState is TICKING`() {
        tap()
        tap()
        backSwipe(isCancelled = true)

        assertEquals(ClockState.TICKING, clockViewModel.clockState.value)
    }

    @Test
    fun `tap-tap-cancelledBackSwipe, playerState is WHITE`() {
        tap()
        tap()
        backSwipe(isCancelled = true)

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }
}
