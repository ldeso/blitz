// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewRootImpl
import org.robolectric.util.ReflectionHelpers
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

    private fun overlappingTaps(delayMillis: Long, isFirstFingerLifted: Boolean = true) {
        composeTestRule.onRoot().performTouchInput {
            down(pointerId = 0, position = center)
            advanceEventTime(delayMillis)
            down(pointerId = 1, position = centerRight)
            up(pointerId = 1)
            if (isFirstFingerLifted) up(pointerId = 0)
        }
        composeTestRule.waitForIdle()
        timeSource += 10.seconds
    }

    private fun heldPress() {
        composeTestRule.onRoot().performTouchInput { down(center) }
        composeTestRule.waitForIdle()
        timeSource += 10.seconds
    }

    private fun takeOver() {
        composeTestRule.onRoot().performTouchInput { cancel() }
        composeTestRule.waitForIdle()
    }

    private fun cancelledPress() {
        composeTestRule.onRoot().performTouchInput { down(center) }
        takeOver()
    }

    private fun accessibilityClick() {
        composeTestRule.onNode(hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        timeSource += 10.seconds
    }

    private fun focusLoss() {
        val decorView = composeTestRule.activity.window.decorView
        val viewRoot = ReflectionHelpers.callInstanceMethod<Any>(decorView, "getViewRootImpl")
        val shadowViewRoot = Shadow.extract<ShadowViewRootImpl>(viewRoot)
        for (hasFocus in listOf(false, true)) {
            composeTestRule.runOnUiThread { shadowViewRoot.callWindowFocusChanged(hasFocus) }
            composeTestRule.waitForIdle()
        }
    }

    private fun back(dispatch: OnBackPressedDispatcher.() -> Unit) {
        val dispatcher = composeTestRule.activity.onBackPressedDispatcher
        composeTestRule.runOnUiThread { dispatcher.dispatch() }
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        composeTestRule.waitForIdle()
    }

    private fun backButton() = back { onBackPressed() }

    private fun backButtonLongPress() {
        startBack(swipeEdge = BackEventCompat.EDGE_NONE)
        backButton()
    }

    private fun hover() {
        composeTestRule.onRoot().performMouseInput {
            moveTo(center)
            moveTo(centerRight)
            scroll(delta = 1F)
        }
        composeTestRule.waitForIdle()
    }

    private fun backSwipe(
        isCancelled: Boolean = false,
        isTakenOverFirst: Boolean = true,
        hasProgress: Boolean = true,
    ) {
        composeTestRule.onRoot().performTouchInput { down(centerLeft) }
        if (isTakenOverFirst) takeOver()
        startBack(hasProgress = hasProgress)
        if (!isTakenOverFirst) takeOver()
        endBack(isCancelled)
    }

    private fun legacyBackSwipe() {
        composeTestRule.onRoot().performTouchInput { down(centerLeft) }
        takeOver()
        backButton()
    }

    private fun touchpadBackSwipe(isCancelled: Boolean = false) {
        startBack()
        endBack(isCancelled)
    }

    private fun startBack(
        swipeEdge: Int = BackEventCompat.EDGE_LEFT,
        hasProgress: Boolean = true,
    ) = back {
        dispatchOnBackStarted(backEvent(progress = 0F, swipeEdge = swipeEdge))
        if (hasProgress) dispatchOnBackProgressed(backEvent(progress = 0.5F, swipeEdge = swipeEdge))
    }

    private fun endBack(isCancelled: Boolean) = back {
        if (isCancelled) dispatchOnBackCancelled() else onBackPressed()
    }

    private fun backEvent(progress: Float, swipeEdge: Int): BackEventCompat = BackEventCompat(
        touchX = 0F, touchY = 0F, progress = progress, swipeEdge = swipeEdge,
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
    fun `tap-overlappingTaps, playerState is WHITE`() {
        tap()
        overlappingTaps(delayMillis = 200L)

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-overlappingTaps within 100 ms, playerState is BLACK`() {
        tap()
        overlappingTaps(delayMillis = 50L)

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
    fun `tap-backButton, clockState is PAUSED`() {
        tap()
        backButton()

        assertEquals(ClockState.PAUSED, clockViewModel.clockState.value)
    }

    @Test
    fun `tap-backButton, playerState is WHITE`() {
        tap()
        backButton()

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-tap-backButton, playerState is BLACK`() {
        tap()
        tap()
        backButton()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-accessibilityClick-backButton, playerState is BLACK`() {
        tap()
        accessibilityClick()
        backButton()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-accessibilityClick-touchpadBackSwipe, playerState is BLACK`() {
        tap()
        accessibilityClick()
        touchpadBackSwipe()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-cancelledPress-focusLoss-backButton, playerState is BLACK`() {
        tap()
        cancelledPress()
        focusLoss()
        backButton()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-heldPress-backButton, playerState is BLACK`() {
        tap()
        heldPress()
        backButton()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-heldPress-backButtonLongPress, clockState is PAUSED`() {
        tap()
        heldPress()
        backButtonLongPress()

        assertEquals(ClockState.PAUSED, clockViewModel.clockState.value)
    }

    @Test
    fun `tap-heldPress-backButtonLongPress, playerState is BLACK`() {
        tap()
        heldPress()
        backButtonLongPress()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-heldPress-focusLoss-takeOver-backButton, playerState is BLACK`() {
        tap()
        heldPress()
        focusLoss()
        takeOver()
        backButton()

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-overlappingTaps with first finger held-backButton, playerState is WHITE`() {
        tap()
        overlappingTaps(delayMillis = 200L, isFirstFingerLifted = false)
        backButton()

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
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
    fun `tap-tap-backSwipe taken over once started, playerState is BLACK`() {
        tap()
        tap()
        backSwipe(isTakenOverFirst = false)

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-tap-cancelledBackSwipe without progress-backButton, playerState is WHITE`() {
        tap()
        tap()
        backSwipe(isCancelled = true, hasProgress = false)
        backButton()

        assertEquals(PlayerState.WHITE, clockViewModel.playerState.value)
    }

    @Test
    fun `tap-tap-legacyBackSwipe, clockState is PAUSED`() {
        tap()
        tap()
        legacyBackSwipe()

        assertEquals(ClockState.PAUSED, clockViewModel.clockState.value)
    }

    @Test
    fun `tap-tap-legacyBackSwipe, playerState is BLACK`() {
        tap()
        tap()
        legacyBackSwipe()

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

    @Test
    fun `tap-tap-cancelledTouchpadBackSwipe, playerState is BLACK`() {
        tap()
        tap()
        touchpadBackSwipe(isCancelled = true)

        assertEquals(PlayerState.BLACK, clockViewModel.playerState.value)
    }
}
