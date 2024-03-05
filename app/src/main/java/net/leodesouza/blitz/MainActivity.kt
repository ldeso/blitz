package net.leodesouza.blitz

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Build
import android.os.Bundle
import android.os.SystemClock.elapsedRealtime
import android.view.OrientationEventListener
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key.Companion.DirectionDown
import androidx.compose.ui.input.key.Key.Companion.DirectionLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.key.Key.Companion.DirectionUp
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily.Companion.Monospace
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

/**
 * A minimalist Fischer chess clock for Android.
 *
 * Default to 5+3 Fischer timing (5 minutes + 3 seconds per move). Total time and increment can be
 * set by horizontal and vertical dragging. The back action pauses or resets the clock.
 */
class MainActivity : ComponentActivity() {
    /** Mutable state keeping track of the orientation. */
    private val isBlackRightHanded = mutableStateOf(true)

    /** Event listener updating [isBlackRightHanded] based on the orientation of the device. */
    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                when (orientation) {
                    in 10 until 135 -> isBlackRightHanded.value = true
                    in 135 until 170 -> isBlackRightHanded.value = false
                    in 190 until 225 -> isBlackRightHanded.value = true
                    in 225 until 350 -> isBlackRightHanded.value = false
                }
            }
        }
    }

    /** Enable the edge-to-edge display, startCounter the activity and compose a chess clock. */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Black.toArgb()
            ),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContent {
            Counter(
                durationMinutes = 5L,
                incrementSeconds = 3L,
                delayMillis = 100L,
                isBlackRightHanded = isBlackRightHanded,
                window = window,
            ) { whiteTime, blackTime, onClick, onDragStart, onHorDrag, onVertDrag, onKeyEvent ->
                ChessClock(
                    whiteTime,
                    blackTime,
                    isBlackRightHanded,
                    onClick,
                    onDragStart,
                    onHorDrag,
                    onVertDrag,
                    onKeyEvent
                )
            }
        }
    }

    /** Enable the orientation event listener after [onCreate] or [onRestart]. */
    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    /** Disable the orientation event listener when the activity is no longer visible. */
    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }
}

/**
 * Basic element that displays [timeMillis] in the form "MM:SS.D" or "H:MM:SS.D", in a given [style]
 * and accepting a given [modifier] to apply to the layout node.
 */
@Composable
fun BasicTime(
    timeMillis: Long, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default
) {
    val tenthsOfSeconds = (timeMillis + 50L) / 100L
    val hours = (tenthsOfSeconds / 36_000L).toString()
    val minutes = (tenthsOfSeconds % 36_000L / 600L).toString().padStart(2, '0')
    val seconds = (tenthsOfSeconds % 600L / 10L).toString().padStart(2, '0')
    val monospaceStyle = style.merge(fontFamily = Monospace)
    CompositionLocalProvider(LocalLayoutDirection provides Ltr) {
        Row(modifier) {
            if (hours == "0") {
                BasicText(text = minutes, style = monospaceStyle)
                BasicText(text = ":", style = style)
                BasicText(text = seconds, style = monospaceStyle)
                BasicText(text = ".", style = style)
                BasicText(text = (tenthsOfSeconds % 10L).toString(), style = monospaceStyle)
            } else {
                BasicText(text = hours, style = monospaceStyle)
                BasicText(text = ":", style = style)
                BasicText(text = minutes, style = monospaceStyle)
                BasicText(text = ":", style = style)
                BasicText(text = seconds, style = monospaceStyle)
            }
        }
    }
}

/**
 * Chess clock displaying [whiteTime] and [blackTime] in an orientation that depends on whether
 * [isBlackRightHanded], and calling the [onClick] callback on click events, the [onDragStart]
 * followed by [onHorDrag] or [onVertDrag] callbacks on drag events, and the [onKeyEvent] callback
 * on key events.
 */
@Preview
@Composable
fun ChessClock(
    whiteTime: Long = 303_000L,
    blackTime: Long = 303_000L,
    isBlackRightHanded: MutableState<Boolean> = mutableStateOf(true),
    onClick: () -> Unit = {},
    onDragStart: (Offset) -> Unit = { _: Offset -> },
    onHorDrag: (PointerInputChange, Float) -> Unit = { _: PointerInputChange, _: Float -> },
    onVertDrag: (PointerInputChange, Float) -> Unit = { _: PointerInputChange, _: Float -> },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val rotation = if (isLandscape) {
        0F
    } else if (isBlackRightHanded.value) {
        -90F
    } else {
        90F
    }
    val blackColor = if (blackTime > 0L) Color.White else Color.Red
    val whiteColor = if (whiteTime > 0L) Color.Black else Color.Red
    val textHeight = LocalConfiguration.current.screenHeightDp.dp / if (isLandscape) 3 else 8
    val fontSize = with(LocalDensity.current) { textHeight.toSp() }
    val fontWeight = Bold
    Column(modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = onDragStart, onHorizontalDrag = onHorDrag,
            )
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = onDragStart, onVerticalDrag = onVertDrag,
            )
        }
        .onKeyEvent(onKeyEvent = onKeyEvent)) {
        BasicTime(
            blackTime,
            modifier = Modifier
                .background(Color.Black)
                .rotate(rotation)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(color = blackColor, fontSize = fontSize, fontWeight = fontWeight),
        )
        BasicTime(
            whiteTime,
            modifier = Modifier
                .background(Color.White)
                .rotate(rotation)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(color = whiteColor, fontSize = fontSize, fontWeight = fontWeight),
        )
    }
}

/**
 * Two-player time counter initially starting from [durationMinutes] and adding [incrementSeconds]
 * before each turn with a given [delayMillis] before each recomposition, and where back events
 * pauseCounter or reset the time.
 *
 * @param[durationMinutes] Initial duration in minutes.
 * @param[incrementSeconds] Time increment added before each turn in seconds.
 * @param[delayMillis] Time between recompositions in milliseconds.
 * @param[isBlackRightHanded] Whether to flip the orientation when using portrait mode.
 * @param[window] Window for which to keep the screen on when time is running.
 * @param[content] Composable taking `whiteTime` and `blackTime` in milliseconds as arguments, as
 *     well as callbacks to be triggered by click, drag and key events. The `onClick` event callback
 *     resumes or triggers next turn, while the `onDragStart`, `onHorDrag`, `onVertDrag`, and
 *     `onKeyEvent` callbacks allow changing the duration and time increment to different values.
 */
@Composable
fun Counter(
    durationMinutes: Long,
    incrementSeconds: Long,
    delayMillis: Long,
    isBlackRightHanded: MutableState<Boolean>,
    window: Window,
    content: @Composable (
        whiteTime: Long,
        blackTime: Long,
        onClick: () -> Unit,
        onDragStart: (Offset) -> Unit,
        onHorDrag: (PointerInputChange, Float) -> Unit,
        onVertDrag: (PointerInputChange, Float) -> Unit,
        onKeyEvent: (KeyEvent) -> Boolean,
    ) -> Unit
) {
    val initialDuration = durationMinutes * 60_000L
    val initialIncrement = incrementSeconds * 1_000L
    val dragSensitivity = 0.01F
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val isRtl = LocalLayoutDirection.current == Rtl

    var duration by rememberSaveable { mutableLongStateOf(initialDuration) }
    var increment by rememberSaveable { mutableLongStateOf(initialIncrement) }
    var whiteTime by rememberSaveable { mutableLongStateOf(duration + increment) }
    var blackTime by rememberSaveable { mutableLongStateOf(duration + increment) }
    var targetElapsedRealtime by rememberSaveable { mutableLongStateOf(0L) }
    var isWhiteTurn by rememberSaveable { mutableStateOf(true) }
    var isStarted by rememberSaveable { mutableStateOf(false) }
    var isFinished by rememberSaveable { mutableStateOf(false) }
    var isCounting by rememberSaveable { mutableStateOf(false) }
    var savedMinutes by remember { mutableFloatStateOf(0F) }
    var savedSeconds by remember { mutableFloatStateOf(0F) }

    fun getPlayerTime(): Long {
        return if (isWhiteTurn) whiteTime else blackTime
    }

    fun setPlayerTime(timeMillis: Long) {
        if (isWhiteTurn) {
            whiteTime = timeMillis
        } else {
            blackTime = timeMillis
        }
    }

    fun applyConfig() {
        val newTime = duration + increment
        whiteTime = newTime
        blackTime = newTime
    }

    fun resetConfig() {
        duration = initialDuration
        increment = initialIncrement
        applyConfig()
    }

    fun startCounter() {
        targetElapsedRealtime = elapsedRealtime() + getPlayerTime()
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        isStarted = true
        isCounting = true
    }

    fun pauseCounter() {
        window.clearFlags(FLAG_KEEP_SCREEN_ON)
        isCounting = false
    }

    fun resetCounter() {
        isWhiteTurn = true
        isStarted = false
        isFinished = false
        applyConfig()
    }

    fun nextTurn() {
        val remainingTime = targetElapsedRealtime - elapsedRealtime()
        val newTime = if (remainingTime > 0L) (remainingTime + increment) else 0L
        setPlayerTime(newTime)
        isWhiteTurn = !isWhiteTurn
        targetElapsedRealtime = elapsedRealtime() + getPlayerTime()
    }

    fun saveMinutesAndSeconds() {
        if (isStarted) {
            savedMinutes = (getPlayerTime() / 60_000L).toFloat()
            savedSeconds = getPlayerTime() % 60_000L / 1_000F
        } else {
            savedMinutes = duration / 60_000F
            savedSeconds = increment / 1_000F
        }
    }

    fun addMinutes(minutes: Float, addToSavedMinutes: Boolean = false) {
        if (!addToSavedMinutes) saveMinutesAndSeconds()
        if (isStarted) {
            val newSeconds = savedSeconds.roundToLong()
            val minMinutes = -newSeconds / 60L + if (newSeconds % 60L == 0L) 1F else 0F
            val maxMinutes = 599F - newSeconds / 60L
            savedMinutes = (savedMinutes + minutes).coerceIn(minMinutes, maxMinutes)
            val newMinutes = savedMinutes.roundToLong()
            setPlayerTime(newMinutes * 60_000L + newSeconds * 1_000L)
        } else {
            val minMinutes = if (increment < 1_000L) 1F else 0F
            savedMinutes = (savedMinutes + minutes).coerceIn(minMinutes, 180F)
            duration = savedMinutes.roundToLong() * 60_000L
            applyConfig()
        }
    }

    fun addSeconds(seconds: Float, addToSavedSeconds: Boolean = false) {
        if (!addToSavedSeconds) saveMinutesAndSeconds()
        if (isStarted) {
            val newMinutes = savedMinutes.roundToLong()
            val minSeconds = 1F - newMinutes * 60L
            val maxSeconds = 35_999F - newMinutes * 60L
            savedSeconds = (savedSeconds + seconds).coerceIn(minSeconds, maxSeconds)
            val newSeconds = savedSeconds.roundToLong()
            setPlayerTime(newMinutes * 60_000L + newSeconds * 1_000L)
        } else {
            val minSeconds = if (duration < 60_000L) 1F else 0F
            savedSeconds = (savedSeconds + seconds).coerceIn(minSeconds, 30F)
            increment = savedSeconds.roundToLong() * 1_000L
            applyConfig()
        }
    }

    val onClick = {
        if (isCounting) {
            nextTurn()
        } else if (!isFinished) {
            startCounter()
        }
    }

    val onDragStart = { _: Offset ->
        if (isCounting) {
            nextTurn()
        } else if (!isFinished) {
            saveMinutesAndSeconds()
        }
    }

    val onHorDrag = { _: PointerInputChange, dragAmount: Float ->
        if (!isCounting && !isFinished) {
            if (isLandscape) {
                val sign = if (isRtl) -1F else 1F
                addMinutes(sign * dragSensitivity * dragAmount, addToSavedMinutes = true)
            } else {
                val sign = if (isBlackRightHanded.value) -1F else 1F
                addSeconds(sign * dragSensitivity * dragAmount, addToSavedSeconds = true)
            }
        }
    }

    val onVertDrag = { _: PointerInputChange, dragAmount: Float ->
        if (!isCounting && !isFinished) {
            if (isLandscape) {
                val sign = -1F
                addSeconds(sign * dragSensitivity * dragAmount, addToSavedSeconds = true)
            } else {
                val sign = if (isBlackRightHanded.value xor isRtl) -1F else 1F
                addMinutes(sign * dragSensitivity * dragAmount, addToSavedMinutes = true)
            }
        }
    }

    val onKeyEvent = onKeyEvent@{ it: KeyEvent ->
        var isConsumed = false
        if (it.type == KeyDown) {
            isConsumed = true
            if (!isCounting && !isFinished) {
                when (it.key) {
                    DirectionUp -> addSeconds(1F)
                    DirectionDown -> addSeconds(-1F)
                    DirectionRight -> addMinutes(if (isRtl) -1F else 1F)
                    DirectionLeft -> addMinutes(if (isRtl) 1F else -1F)
                    else -> isConsumed = false
                }
            }
        }
        isConsumed
    }

    BackHandler(isCounting) {
        pauseCounter()
    }

    BackHandler(isStarted && !isCounting) {
        resetCounter()
    }

    BackHandler(!isStarted && (duration != initialDuration || increment != initialIncrement)) {
        resetConfig()
    }

    LaunchedEffect(isCounting, whiteTime, blackTime) {
        if (isCounting) {
            if (whiteTime > 0L && blackTime > 0L) {
                val remainingTime = targetElapsedRealtime - elapsedRealtime()
                val correctedDelay = remainingTime % delayMillis
                delay(correctedDelay)
                setPlayerTime(remainingTime - correctedDelay)
            } else {
                isFinished = true
                pauseCounter()
            }
        }
    }

    content.invoke(whiteTime, blackTime, onClick, onDragStart, onHorDrag, onVertDrag, onKeyEvent)
}
