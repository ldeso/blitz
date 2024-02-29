package net.leodesouza.blitz

import android.os.Bundle
import android.os.SystemClock.elapsedRealtime
import android.view.OrientationEventListener
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
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
                if (orientation in 30 until 150) {
                    isBlackRightHanded.value = true
                } else if (orientation in 210 until 330) {
                    isBlackRightHanded.value = false
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
        setContent {
            Counter(
                durationMinutes = 5L,
                incrementSeconds = 3L,
                delayMillis = 100L,
                isBlackRightHanded = isBlackRightHanded,
                window = window,
            ) { whiteTime, blackTime, onClick, onDragStart, onDrag ->
                ChessClock(whiteTime, blackTime, isBlackRightHanded, onClick, onDragStart, onDrag)
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
    val hours = roundedTime / 3_600_000L
    val minutes = (roundedTime % 3_600_000L / 60_000L).toString().padStart(2, '0')
    val seconds = (roundedTime % 60_000L / 1_000L).toString().padStart(2, '0')
    val decimal = (roundedTime % 1_000L).toString().take(1)
    val text = if (hours > 0L) "$hours:$minutes:$seconds" else "$minutes:$seconds.$decimal"
    BasicText(text = text, modifier = modifier, style = style)
}

/**
 * Chess clock displaying [whiteTime] and [blackTime] in an orientation that depends on whether
 * [isBlackRightHanded], and calling the [onClick] callback on click events and the [onDragStart]
 *  followed by [onDrag] callbacks on drag events.
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
) {
    val blackColor = if (blackTime > 0L) Color.White else Color.Red
    val whiteColor = if (whiteTime > 0L) Color.Black else Color.Red
    val fontSize = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toSp() / 8
    }
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
        }) {
        BasicTime(
            blackTime,
            modifier = Modifier
                .background(Color.Black)
                .rotate(if (isBlackRightHanded.value) -90F else 90F)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(color = blackColor, fontSize = fontSize),
        )
        BasicTime(
            whiteTime,
            modifier = Modifier
                .background(Color.White)
                .rotate(if (isBlackRightHanded.value) -90F else 90F)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(color = whiteColor, fontSize = fontSize),
        )
    }
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
 * @param[content] Composable taking `whiteTime` and `blackTime` in milliseconds as arguments, as
 *     well as callbacks to be triggered by click and drag events. The `onClick` event callback
 *     triggers next turn, while the `onDragStart` and `onDrag` event callbacks allow changing the
 *     initial duration and time increment to different values.
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
    val isRTL = LocalLayoutDirection.current == LayoutDirection.Rtl

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
                    if (isHorizontalDrag) {
                        val dragFactor = if (isBlackRightHanded.value) -20L else 20L
                        val minIncrement = if (duration == 0L) 1_000L else 0L
                        val maxIncrement = 30_000L
                        increment = round(
                            number = savedIncrement + dragFactor * dragOffset.x.roundToLong(),
                            step = 1_000L,
                        ).coerceIn(minIncrement, maxIncrement)
                    } else {
                        val dragFactor = if (isBlackRightHanded.value xor isRTL) -1000L else 1000L
                        val minDuration = if (increment == 0L) 60_000L else 0L
                        val maxDuration = 10_800_000L
                        duration = round(
                            number = savedDuration + dragFactor * dragOffset.y.roundToLong(),
                            step = 60_000L,
                        ).coerceIn(minDuration, maxDuration)
                    }
                    whiteTime = duration + increment
                    blackTime = duration + increment
                } else if (!isFinished && isHorizontalDrag) {
                    val dragFactor = if (isBlackRightHanded.value) -20L else 20L
                    val minTime = 100L
                    val maxTime = 35_999_900L
                    val newTime = round(
                        number = savedTime + dragFactor * dragOffset.x.roundToLong(),
                        step = 100L,
                    ).coerceIn(minTime, maxTime)
                    if (isWhiteTurn) {
                        whiteTime = newTime
                    } else {
                        blackTime = newTime
                    }
                }
            }
        }
    }

    content.invoke(whiteTime, blackTime, onClick, onDragStart, onDrag)

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
