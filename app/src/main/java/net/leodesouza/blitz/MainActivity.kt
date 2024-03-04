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
import androidx.compose.foundation.gestures.detectDragGestures
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
import kotlin.math.absoluteValue
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
            ) { whiteTime, blackTime, onClick, onDragStart, onDrag, onKeyEvent ->
                ChessClock(
                    whiteTime,
                    blackTime,
                    isBlackRightHanded,
                    onClick,
                    onDragStart,
                    onDrag,
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
 * followed by [onDrag] callbacks on drag events, and the [onKeyEvent] on key events.
 */
@Preview
@Composable
fun ChessClock(
    whiteTime: Long = 303_000L,
    blackTime: Long = 303_000L,
    isBlackRightHanded: MutableState<Boolean> = remember { mutableStateOf(true) },
    onClick: () -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDrag: (PointerInputChange, Offset) -> Unit = { _: PointerInputChange, _: Offset -> },
    onKeyEvent: (KeyEvent) -> Boolean = { true },
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val rotation = if (isLandscape) {
        0F
    } else {
        if (isBlackRightHanded.value) -90F else 90F
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
            detectDragGestures(
                onDragStart = onDragStart, onDrag = onDrag
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
 * Return the horizontal part of an [offset] increasing from left to right and taking into account
 * whether the device orientation [isLandscape].
 */
fun horizontalOffset(offset: Offset, isLandscape: Boolean): Float {
    return if (isLandscape) -offset.y else -offset.x
}

/**
 * Return the vertical part of an [offset] increasing from the bottom to the top and taking into
 * account whether the device orientation [isLandscape].
 */
fun verticalOffset(offset: Offset, isLandscape: Boolean): Float {
    return if (isLandscape) offset.x else -offset.y
}

/**
 * Two-player time counter initially starting from [durationMinutes] and adding [incrementSeconds]
 * before each turn with a delay of [delayMillis] before each recomposition, and where back events
 * pause or reset the time.
 *
 * @param[durationMinutes] Initial duration in minutes.
 * @param[incrementSeconds] Time increment added before each turn in seconds.
 * @param[delayMillis] Time between recompositions in milliseconds.
 * @param[isBlackRightHanded] Whether to flip the horizontal dragging direction.
 * @param[window] Window for which to keep the screen on when time is running.
 * @param[content] Composable taking `whiteTime` and `blackTime` in milliseconds as arguments, as
 *     well as callbacks to be triggered by click and drag events. The `onClick` event callback
 *     triggers next turn, while the `onDragStart`, `onDrag`, and `onKeyEvent` event callbacks allow
 *     changing the initial duration and time increment to different values.
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
        onDrag: (PointerInputChange, Offset) -> Unit,
        onKeyEvent: (KeyEvent) -> Boolean,
    ) -> Unit
) {
    val initialDuration = durationMinutes * 60_000L
    val initialIncrement = incrementSeconds * 1_000L
    var duration by rememberSaveable { mutableLongStateOf(initialDuration) }
    var increment by rememberSaveable { mutableLongStateOf(initialIncrement) }
    var whiteTime by rememberSaveable { mutableLongStateOf(duration + increment) }
    var blackTime by rememberSaveable { mutableLongStateOf(duration + increment) }
    var targetElapsedRealtime by rememberSaveable { mutableLongStateOf(0L) }
    var isWhiteTurn by rememberSaveable { mutableStateOf(true) }
    var isReset by rememberSaveable { mutableStateOf(true) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var isFinished by rememberSaveable { mutableStateOf(false) }

    val onClick = {
        if (!isFinished) {
            if (isRunning) {
                val remainingTime = targetElapsedRealtime - elapsedRealtime()
                val newTime = if (remainingTime > 0L) remainingTime + increment else 0L
                if (isWhiteTurn) {
                    whiteTime = newTime
                    isWhiteTurn = false
                } else {
                    blackTime = newTime
                    isWhiteTurn = true
                }
            } else {
                isReset = false
                isRunning = true
                window.addFlags(FLAG_KEEP_SCREEN_ON)
            }
            targetElapsedRealtime = elapsedRealtime() + if (isWhiteTurn) whiteTime else blackTime
        }
    }

    var isDragStart by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var savedDuration by remember { mutableLongStateOf(0L) }
    var savedIncrement by remember { mutableLongStateOf(0L) }
    var savedTime by remember { mutableLongStateOf(0L) }

    val onDragStart = { it: Offset ->
        isDragStart = true
        dragPosition = it
        savedDuration = duration
        savedIncrement = increment
        savedTime = if (isWhiteTurn) whiteTime else blackTime
    }

    var isHorizontalDrag by remember { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val isRTL = LocalLayoutDirection.current == Rtl

    val onDrag = { change: PointerInputChange, _: Offset ->
        if (!isRunning) {
            val dragOffset = change.position - dragPosition
            if (isDragStart) {
                if (dragOffset.getDistanceSquared() > 40_000F) {
                    isHorizontalDrag = dragOffset.x.absoluteValue > dragOffset.y.absoluteValue
                    isDragStart = false
                }
            } else {
                if (isReset) {
                    if (isHorizontalDrag xor isLandscape) {
                        val sign = if (isLandscape || isBlackRightHanded.value) 1L else -1L
                        val magnitude = 20L
                        val offset = horizontalOffset(dragOffset, isLandscape).roundToLong()
                        increment = round(
                            number = savedIncrement + sign * magnitude * offset,
                            step = 1_000L,
                        ).coerceIn(if (duration == 0L) 1_000L..30_000L else 0L..30_000L)
                    } else {
                        val sign =
                            if ((isLandscape || isBlackRightHanded.value) xor isRTL) 1L else -1L
                        val magnitude = 1_000L
                        val offset = verticalOffset(dragOffset, isLandscape).roundToLong()
                        duration = round(
                            number = savedDuration + sign * magnitude * offset,
                            step = 60_000L,
                        ).coerceIn(if (increment == 0L) 60_000L..10_800_000L else 0L..10_800_000L)
                    }
                    whiteTime = duration + increment
                    blackTime = duration + increment
                } else if (!isFinished && (isHorizontalDrag xor isLandscape)) {
                    val sign = if (isLandscape || isBlackRightHanded.value) 1L else -1L
                    val magnitude = 20L
                    val offset = horizontalOffset(dragOffset, isLandscape).roundToLong()
                    val newTime = round(
                        number = savedTime + sign * magnitude * offset,
                        step = 100L,
                    ).coerceIn(100L..35_999_900L)
                    if (isWhiteTurn) {
                        whiteTime = newTime
                    } else {
                        blackTime = newTime
                    }
                }
            }
        }
    }

    val onKeyEvent = onKeyEvent@{ it: KeyEvent ->
        if (it.type == KeyDown) {
            if (isReset) {
                val sign = if (isRTL) -1L else 1L
                val incrementRange = if (duration == 0L) 1_000L..30_000L else 0L..30_000L
                val durationRange = if (increment == 0L) 60_000L..10_800_000L else 0L..10_800_000L
                when (it.key) {
                    DirectionUp -> increment = (increment + 1_000).coerceIn(incrementRange)
                    DirectionDown -> increment = (increment - 1_000).coerceIn(incrementRange)
                    DirectionRight -> duration = (duration + sign * 60_000).coerceIn(durationRange)
                    DirectionLeft -> duration = (duration - sign * 60_000).coerceIn(durationRange)
                    else -> return@onKeyEvent false
                }
                whiteTime = duration + increment
                blackTime = duration + increment
            } else {
                val timeUpdate = when (it.key) {
                    DirectionUp -> 1_000L
                    DirectionDown -> -1_000L
                    else -> return@onKeyEvent false
                }
                val oldTime = if (isWhiteTurn) whiteTime else blackTime
                val newTime = round(
                    number = oldTime + timeUpdate, step = 1000L
                ).coerceIn(1000L..35_999_000L)
                if (isWhiteTurn) {
                    whiteTime = newTime
                } else {
                    blackTime = newTime
                }
            }
            true
        } else {
            false
        }
    }

    content.invoke(whiteTime, blackTime, onClick, onDragStart, onDrag, onKeyEvent)

    BackHandler(isRunning) {
        isRunning = false
        window.clearFlags(FLAG_KEEP_SCREEN_ON)
    }

    BackHandler(!isRunning && !isReset) {
        whiteTime = duration + increment
        blackTime = duration + increment
        isWhiteTurn = true
        isReset = true
        isFinished = false
    }

    BackHandler(isReset && (duration != initialDuration || increment != initialIncrement)) {
        duration = initialDuration
        increment = initialIncrement
        whiteTime = duration + increment
        blackTime = duration + increment
    }

    LaunchedEffect(isRunning, whiteTime, blackTime) {
        if (isRunning) {
            if (whiteTime > 0L && blackTime > 0L) {
                val remainingTime = targetElapsedRealtime - elapsedRealtime()
                val newTime = if (remainingTime > 0L) remainingTime else 0L
                delay(newTime % delayMillis)
                if (isWhiteTurn) {
                    whiteTime = newTime
                } else {
                    blackTime = newTime
                }
            } else {
                isFinished = true
                isRunning = false
            }
        }
    }
}
