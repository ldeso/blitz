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

    /** Enable the edge-to-edge display, start the activity and compose a chess clock. */
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

/** Return the given [number] rounded to the nearest [step] from zero. */
fun round(number: Long, step: Long): Long {
    return (number + (step / 2L)) / step * step
}

/**
 * Basic element that displays [timeMillis] in the form "MM:SS.D" or "H:MM:SS.D", in a given [style]
 * and accepting a given [modifier] to apply to the layout node.
 */
@Composable
fun BasicTime(
    timeMillis: Long, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default
) {
    val roundedTime = round(number = timeMillis, step = 100L)
    val hours = (roundedTime / 3_600_000L).toString()
    val minutes = (roundedTime % 3_600_000L / 60_000L).toString().padStart(2, '0')
    val seconds = (roundedTime % 60_000L / 1_000L).toString().padStart(2, '0')
    val monospaceStyle = style.merge(fontFamily = Monospace)
    CompositionLocalProvider(LocalLayoutDirection provides Ltr) {
        Row(modifier) {
            if (hours == "0") {
                val decimal = (roundedTime % 1_000L).toString().take(1)
                BasicText(text = minutes, style = monospaceStyle)
                BasicText(text = ":", style = style)
                BasicText(text = seconds, style = monospaceStyle)
                BasicText(text = ".", style = style)
                BasicText(text = decimal, style = monospaceStyle)
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
    isBlackRightHanded: MutableState<Boolean> = remember { mutableStateOf(true) },
    onClick: () -> Unit = {},
    onDragStart: (Offset) -> Unit = { _: Offset -> },
    onHorDrag: (PointerInputChange, Float) -> Unit = { _: PointerInputChange, _: Float -> },
    onVertDrag: (PointerInputChange, Float) -> Unit = { _: PointerInputChange, _: Float -> },
    onKeyEvent: (KeyEvent) -> Boolean = { true },
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
            style = TextStyle(color = blackColor, fontSize = fontSize),
        )
        BasicTime(
            whiteTime,
            modifier = Modifier
                .background(Color.White)
                .rotate(rotation)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(color = whiteColor, fontSize = fontSize),
        )
    }
}

/**
 * Two-player time counter initially starting from [durationMinutes] and adding [incrementSeconds]
 * before each turn with a given [delayMillis] before each recomposition, and where back events
 * pause or reset the time.
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

    // General state that survives activity or process recreation
    var duration by rememberSaveable { mutableLongStateOf(initialDuration) }
    var increment by rememberSaveable { mutableLongStateOf(initialIncrement) }
    var whiteTime by rememberSaveable { mutableLongStateOf(duration + increment) }
    var blackTime by rememberSaveable { mutableLongStateOf(duration + increment) }
    var targetElapsedRealtime by rememberSaveable { mutableLongStateOf(0L) }
    var isWhiteTurn by rememberSaveable { mutableStateOf(true) }
    var isReset by rememberSaveable { mutableStateOf(true) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var isFinished by rememberSaveable { mutableStateOf(false) }

    // Saved state to update from when changing the configuration
    var savedDurationMinutes by remember { mutableFloatStateOf(0F) }
    var savedIncrementSeconds by remember { mutableFloatStateOf(0F) }
    var savedTimeMinutes by remember { mutableFloatStateOf(0F) }
    var savedTimeSeconds by remember { mutableFloatStateOf(0F) }

    /** Save duration, time increment and current time before changing the configuration. */
    fun saveState() {
        val currentTime = (if (isWhiteTurn) whiteTime else blackTime)
        savedDurationMinutes = duration / 60_000F
        savedIncrementSeconds = increment / 1_000F
        savedTimeMinutes = (currentTime / 60_000L).toFloat()
        savedTimeSeconds = round(currentTime % 60_000L, 1_000L) / 1_000F
    }

    /** Update current player's time to [newTime]. */
    fun updateTime(newTime: Long) {
        if (isWhiteTurn) {
            whiteTime = newTime
        } else {
            blackTime = newTime
        }
    }

    /** Reset time to starting position. */
    fun resetTime() {
        val newTime = duration + increment
        whiteTime = newTime
        blackTime = newTime
    }

    /** Update time or duration by [minutes] from state that optionally [isNewSavedState]. */
    fun updateMinutes(minutes: Float, isNewSavedState: Boolean = true) {
        if (!isRunning) {
            if (isNewSavedState) saveState()
            if (isReset) {
                val minMinutes = if (increment < 1_000L) 1F else 0F
                savedDurationMinutes = (savedDurationMinutes + minutes).coerceIn(minMinutes, 180F)
                duration = savedDurationMinutes.roundToLong() * 60_000L
                resetTime()
            } else if (!isFinished) {
                val newSeconds = savedTimeSeconds.roundToLong() * 1_000L
                val minMinutes = -newSeconds / 60_000L + if (newSeconds % 60_000L == 0L) 1F else 0F
                val maxMinutes = 599F - newSeconds / 60_000L
                savedTimeMinutes = (savedTimeMinutes + minutes).coerceIn(minMinutes, maxMinutes)
                val newMinutes = savedTimeMinutes.roundToLong() * 60_000L
                updateTime(newMinutes + newSeconds)
            }
        }
    }

    /** Update time or increment by [seconds] from state that optionally [isNewSavedState]. */
    fun updateSeconds(seconds: Float, isNewSavedState: Boolean = true) {
        if (!isRunning) {
            if (isNewSavedState) saveState()
            if (isReset) {
                val minSeconds = if (duration < 60_000L) 1F else 0F
                savedIncrementSeconds = (savedIncrementSeconds + seconds).coerceIn(minSeconds, 30F)
                increment = savedIncrementSeconds.roundToLong() * 1_000L
                resetTime()
            } else if (!isFinished) {
                val newMinutes = savedTimeMinutes.roundToLong() * 60_000L
                val minSeconds = 1F - newMinutes / 1_000L
                val maxSeconds = 35_999F - newMinutes / 1_000L
                savedTimeSeconds = (savedTimeSeconds + seconds).coerceIn(minSeconds, maxSeconds)
                val newSeconds = savedTimeSeconds.roundToLong() * 1_000L
                updateTime(newMinutes + newSeconds)
            }
        }
    }

    /** On-click event callback to resume or trigger next turn. */
    val onClick = {
        if (!isFinished) {
            if (isRunning) {
                val remainingTime = targetElapsedRealtime - elapsedRealtime()
                val newTime = if (remainingTime > 0L) remainingTime + increment else 0L
                updateTime(newTime)
                isWhiteTurn = !isWhiteTurn
            } else {
                isReset = false
                isRunning = true
                window.addFlags(FLAG_KEEP_SCREEN_ON)
            }
            targetElapsedRealtime = elapsedRealtime() + if (isWhiteTurn) whiteTime else blackTime
        }
    }

    /** Event callback to save the current state before dragging to change the configuration. */
    val onDragStart = { _: Offset -> saveState() }

    /** Horizontal dragging event callback to change the current configuration. */
    val onHorDrag = { _: PointerInputChange, dragAmount: Float ->
        if (isLandscape) {
            val sign = if (isRtl) -1F else 1F
            updateMinutes(sign * dragSensitivity * dragAmount, isNewSavedState = false)
        } else {
            val sign = if (isBlackRightHanded.value) -1F else 1F
            updateSeconds(sign * dragSensitivity * dragAmount, isNewSavedState = false)
        }
    }

    /** Vertical dragging event callback to change the current configuration. */
    val onVertDrag = { _: PointerInputChange, dragAmount: Float ->
        if (isLandscape) {
            val sign = -1F
            updateSeconds(sign * dragSensitivity * dragAmount, isNewSavedState = false)
        } else {
            val sign = if (isBlackRightHanded.value xor isRtl) -1F else 1F
            updateMinutes(sign * dragSensitivity * dragAmount, isNewSavedState = false)
        }
    }

    /** Key press event callback to change the current configuration. */
    val onKeyEvent = onKeyEvent@{ it: KeyEvent ->
        if (it.type == KeyDown) {
            when (it.key) {
                DirectionUp -> updateSeconds(1F)
                DirectionDown -> updateSeconds(-1F)
                DirectionRight -> updateMinutes(if (isRtl) -1F else 1F)
                DirectionLeft -> updateMinutes(if (isRtl) 1F else -1F)
                else -> return@onKeyEvent false
            }
        } else {
            return@onKeyEvent false
        }
        return@onKeyEvent true
    }

    content.invoke(whiteTime, blackTime, onClick, onDragStart, onHorDrag, onVertDrag, onKeyEvent)

    BackHandler(isRunning) {
        isRunning = false
        window.clearFlags(FLAG_KEEP_SCREEN_ON)
    }

    BackHandler(!isRunning && !isReset) {
        resetTime()
        isWhiteTurn = true
        isReset = true
        isFinished = false
    }

    BackHandler(isReset && (duration != initialDuration || increment != initialIncrement)) {
        duration = initialDuration
        increment = initialIncrement
        resetTime()
    }

    LaunchedEffect(isRunning, whiteTime, blackTime) {
        if (isRunning) {
            if (whiteTime > 0L && blackTime > 0L) {
                val remainingTime = targetElapsedRealtime - elapsedRealtime()
                val newTime = if (remainingTime > 0L) remainingTime else 0L
                delay(newTime % delayMillis)
                updateTime(newTime)
            } else {
                isFinished = true
                isRunning = false
            }
        }
    }
}
