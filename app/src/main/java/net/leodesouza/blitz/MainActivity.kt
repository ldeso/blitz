package net.leodesouza.blitz

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.os.Bundle
import android.os.SystemClock.elapsedRealtime
import android.text.format.DateUtils.HOUR_IN_MILLIS
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.text.format.DateUtils.formatElapsedTime
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * A minimalist Fischer chess clock for Android.
 *
 * Defaults to 5+3 Fischer timing (5 minutes + 3 seconds per move). Total time and increment can be
 * set by horizontal and vertical dragging. The back action pauses or resets the clock.
 */
class MainActivity : ComponentActivity() {
    /**
     * Enforce portrait orientation, enable the edge-to-edge display and set the content to a
     * fullscreen chess clock.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Black.toArgb()
            ),
        )
        super.onCreate(savedInstanceState)
        requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
        setContent {
            Counter(
                durationMinutes = 5L, incrementSeconds = 3L, delayMillis = 100L, window = window,
            ) { whiteTime, blackTime, onClick, onDragStart, onDrag ->
                ChessClock(whiteTime, blackTime, onClick, onDragStart, onDrag)
            }
        }
    }
}

/** Return the given [number] rounded to the nearest [step] from zero. */
fun round(number: Long, step: Long): Long {
    return (number + (step / 2L)) / step * step
}

/**
 * Display [timeMillis] in the form "MM:SS.D" or "H:MM:SS.D" in a given [color] and with a given
 * [modifier].
 */
@Composable
fun BasicTime(timeMillis: Long, color: Color, modifier: Modifier = Modifier) {
    val roundedTime = round(number = timeMillis, step = 100L)
    val integerPart = formatElapsedTime(roundedTime / SECOND_IN_MILLIS)
    val decimalPart = "${roundedTime % SECOND_IN_MILLIS}".take(1)
    BasicText(
        text = if (roundedTime < HOUR_IN_MILLIS) "$integerPart.$decimalPart" else integerPart,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = with(LocalDensity.current) {
            LocalConfiguration.current.screenHeightDp.dp.toSp() / 8
        }),
    )
}

/**
 * Chess clock displaying [whiteTime] and [blackTime], and calling the [onClick] callback on click
 * events and the [onDragStart] followed by [onDrag] callbacks on drag events.
 */
@Preview
@Composable
fun ChessClock(
    whiteTime: Long = 5L * MINUTE_IN_MILLIS + 3L * SECOND_IN_MILLIS,
    blackTime: Long = 5L * MINUTE_IN_MILLIS + 3L * SECOND_IN_MILLIS,
    onClick: () -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDrag: (PointerInputChange, Offset) -> Unit = { _: PointerInputChange, _: Offset -> },
) {
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
            color = if (blackTime > 0L) Color.White else Color.Red,
            modifier = Modifier
                .background(Color.Black)
                .rotate(-90F)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
        )
        BasicTime(
            whiteTime,
            color = if (whiteTime > 0L) Color.Black else Color.Red,
            modifier = Modifier
                .background(Color.White)
                .rotate(-90F)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
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
    window: Window,
    content: @Composable (
        whiteTime: Long,
        blackTime: Long,
        onClick: () -> Unit,
        onDragStart: (Offset) -> Unit,
        onDrag: (PointerInputChange, Offset) -> Unit,
    ) -> Unit
) {
    val initialDuration = durationMinutes * MINUTE_IN_MILLIS
    val initialIncrement = incrementSeconds * SECOND_IN_MILLIS
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
                        val minIncrement = SECOND_IN_MILLIS
                        val maxIncrement = 30L * SECOND_IN_MILLIS
                        val newIncrement = round(
                            number = savedIncrement - 20L * dragOffset.x.roundToLong(),
                            step = SECOND_IN_MILLIS
                        )
                        increment = if (newIncrement > maxIncrement) {
                            maxIncrement
                        } else if (newIncrement > 0L) {
                            newIncrement
                        } else if (duration == 0L) {
                            minIncrement
                        } else {
                            0L
                        }
                    } else {
                        val minDuration = MINUTE_IN_MILLIS
                        val maxDuration = 3L * HOUR_IN_MILLIS
                        val newDuration = round(
                            number = savedDuration - 1000L * dragOffset.y.roundToLong(),
                            step = MINUTE_IN_MILLIS
                        )
                        duration = if (newDuration > maxDuration) {
                            maxDuration
                        } else if (newDuration > 0L) {
                            newDuration
                        } else if (increment == 0L) {
                            minDuration
                        } else {
                            0L
                        }
                    }
                    whiteTime = duration + increment
                    blackTime = duration + increment
                } else if (!isFinished && isHorizontalDrag) {
                    val newTime = round(
                        number = savedTime - 20L * dragOffset.x.roundToLong(),
                        step = SECOND_IN_MILLIS
                    )
                    if (newTime > 0L) {
                        if (isWhiteTurn) {
                            whiteTime = newTime
                        } else {
                            blackTime = newTime
                        }
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
                isRunning = false
                isFinished = true
            }
        }
    }
}
