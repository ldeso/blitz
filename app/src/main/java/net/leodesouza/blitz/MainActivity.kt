package net.leodesouza.blitz

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Build
import android.os.Bundle
import android.os.SystemClock.elapsedRealtime
import android.view.OrientationEventListener
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_90
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getDisplayOrDefault
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
    private val isLeaningRight = mutableStateOf(true)

    /** Event listener updating [isLeaningRight] based on the orientation of the device. */
    private val orientationEventListener by lazy {
        val activity = this
        object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (getDisplayOrDefault(activity).rotation) {
                    ROTATION_0 -> 0
                    ROTATION_90 -> 90
                    ROTATION_180 -> 180
                    else -> 270
                }
                when ((orientation + rotation) % 360) {
                    in 10 until 170 -> isLeaningRight.value = true
                    in 190 until 350 -> isLeaningRight.value = false
                }
            }
        }
    }

    /** Enable the [orientationEventListener] after [onCreate] or [onRestart]. */
    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    /** Disable the [orientationEventListener] when the activity is no longer visible. */
    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    /** Enable the edge-to edge display, start the activity and compose a chess clock. */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Black.toArgb(),
            ),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContent {
            val clockModel = rememberChessClockModel(
                durationMinutes = 5,
                incrementSeconds = 3,
                tickPeriodMillis = 100,
                onStart = { window.addFlags(FLAG_KEEP_SCREEN_ON) },
                onPause = { window.clearFlags(FLAG_KEEP_SCREEN_ON) },
            )
            ChessClockController(clockModel, { isLeaningRight.value }) { whiteTime, blackTime ->
                ChessClockView(whiteTime, blackTime, isLeaningRight.value)
            }
        }
    }
}

/**
 * Chess clock model that notifies composition when one of its public properties changes.
 *
 * @param[defaultDuration] Default initial time for each player in milliseconds.
 * @param[defaultIncrement] Default time increment in milliseconds.
 * @param[tickPeriod] Period between each tick in milliseconds.
 * @param[onStart] Callback called when the clock starts ticking.
 * @param[onPause] Callback called when the clock stops ticking.
 * @param[durationState] Mutable state holding the initial time for each player in milliseconds.
 * @param[incrementState] Mutable state holding the time increment in milliseconds.
 * @param[whiteTimeState] Mutable state holding the time for the first player in milliseconds.
 * @param[blackTimeState] Mutable state holding the time for the second player in milliseconds.
 * @param[targetRealtimeState] Mutable state holding the time since boot where the clock should end.
 * @param[isWhiteTurnState] Mutable state holding whether it is the turn of the first player.
 * @param[isStartedState] Mutable state holding whether the clock has started ticking.
 * @param[isTickingState] Mutable state holding whether the clock is currently ticking.
 */
@Stable
class ChessClockModel(
    private val defaultDuration: Long,
    private val defaultIncrement: Long,
    private val tickPeriod: Long,
    private val onStart: () -> Unit,
    private val onPause: () -> Unit,
    durationState: MutableLongState,
    incrementState: MutableLongState,
    whiteTimeState: MutableLongState,
    blackTimeState: MutableLongState,
    targetRealtimeState: MutableLongState,
    isWhiteTurnState: MutableState<Boolean>,
    isStartedState: MutableState<Boolean>,
    isTickingState: MutableState<Boolean>,
) {
    /** Initial time for each player in milliseconds. */
    private var duration: Long by durationState

    /** Time increment for each player in milliseconds. */
    private var increment: Long by incrementState

    /** Time for the first player in milliseconds. */
    var whiteTime: Long by whiteTimeState
        private set

    /** Time for the second player in milliseconds. */
    var blackTime: Long by blackTimeState
        private set

    /** Time since boot where the clock should end. */
    private var targetRealtime: Long by targetRealtimeState

    /** Whether it is the turn of the first player. */
    private var isWhiteTurn: Boolean by isWhiteTurnState

    /** Whether the clock has started ticking. */
    var isStarted: Boolean by isStartedState
        private set

    /** Whether the clock is currently ticking. */
    var isTicking: Boolean by isTickingState
        private set

    /** Whether the clock has finished ticking. */
    private val isFinished: Boolean
        get() = whiteTime <= 0L || blackTime <= 0L

    /** Whether the clock is currently paused. */
    val isPaused: Boolean
        get() = !isTicking && !isFinished

    /** Whether the duration and time increment are set to their default value. */
    val isDefaultConfig: Boolean
        get() = duration == defaultDuration && increment == defaultIncrement

    /** Time of the current player. */
    private var currentTime: Long
        get() = if (isWhiteTurn) whiteTime else blackTime
        set(newTime) {
            if (isWhiteTurn) {
                whiteTime = newTime
            } else {
                blackTime = newTime
            }
        }

    /** Set the clock as having started ticking. */
    fun start() {
        targetRealtime = elapsedRealtime() + currentTime
        isStarted = true
        isTicking = true
        onStart.invoke()
    }

    /** Set the clock on pause. */
    fun pause() {
        currentTime = targetRealtime - elapsedRealtime()
        isTicking = false
        onPause.invoke()
    }

    /** If the clock is ticking, wait until next tick and update the time of the current player. */
    suspend fun tick() {
        if (isTicking) {
            if (isFinished) {
                pause()
            } else {
                val remainingTime = targetRealtime - elapsedRealtime()
                val delayMillis = remainingTime % tickPeriod
                delay(delayMillis)
                currentTime = remainingTime - delayMillis
            }
        }
    }

    /** If the clock is ticking, switch between ticking for the first and for the second player. */
    fun nextTurn() {
        if (isTicking) {
            val remainingTime = targetRealtime - elapsedRealtime()
            currentTime = if (remainingTime > 0L) (remainingTime + increment) else 0L
            isWhiteTurn = !isWhiteTurn
            targetRealtime = elapsedRealtime() + currentTime
        }
    }

    /** Set the time for each player according to the current duration and time increment. */
    private fun applyConfig() {
        val newTime = duration + increment
        whiteTime = newTime
        blackTime = newTime
    }

    /** Reset the time for each player to their initial value. */
    fun resetTime() {
        isWhiteTurn = true
        isStarted = false
        applyConfig()
    }

    /** Restore the duration and time increment to their default value. */
    fun restoreDefaultConfig() {
        duration = defaultDuration
        increment = defaultIncrement
        applyConfig()
    }

    /** Saved minutes or duration that can be used as a reference to add minutes to. */
    private var savedMinutes: Float = 0F

    /** Saved seconds or increment that can be used as a reference to add seconds to. */
    private var savedSeconds: Float = 0F

    /** Save the current time or duration/increment as a reference to add seconds or minutes to. */
    fun saveTime() {
        if (isStarted) {
            savedMinutes = (currentTime / 60_000L).toFloat()
            savedSeconds = (currentTime % 60_000L / 1_000L).toFloat()
        } else {
            savedMinutes = duration / 60_000F
            savedSeconds = increment / 1_000F
        }
    }

    /** Add [minutes] to the current or saved time depending on whether [isAddedToSavedTime]. */
    fun addMinutes(minutes: Float, isAddedToSavedTime: Boolean = false) {
        if (!isAddedToSavedTime) saveTime()
        if (isStarted) {
            val newSeconds = savedSeconds.roundToLong()
            val minMinutes = -newSeconds / 60L + if (newSeconds % 60L == 0L) 1F else 0F
            val maxMinutes = 599F - newSeconds / 60L
            savedMinutes = (savedMinutes + minutes).coerceIn(minMinutes, maxMinutes)
            val newMinutes = savedMinutes.roundToLong()
            currentTime = newMinutes * 60_000L + newSeconds * 1_000L
        } else {
            val minMinutes = if (increment < 1_000L) 1F else 0F
            savedMinutes = (savedMinutes + minutes).coerceIn(minMinutes, 180F)
            duration = savedMinutes.roundToLong() * 60_000L
            applyConfig()
        }
    }

    /** Add [seconds] to the current or saved time depending on whether [isAddedToSavedTime]. */
    fun addSeconds(seconds: Float, isAddedToSavedTime: Boolean = false) {
        if (!isAddedToSavedTime) saveTime()
        if (isStarted) {
            val newMinutes = savedMinutes.roundToLong()
            val minSeconds = 1F - newMinutes * 60L
            val maxSeconds = 35_999F - newMinutes * 60L
            savedSeconds = (savedSeconds + seconds).coerceIn(minSeconds, maxSeconds)
            val newSeconds = savedSeconds.roundToLong()
            currentTime = newMinutes * 60_000L + newSeconds * 1_000L
        } else {
            val minSeconds = if (duration < 60_000L) 1F else 0F
            savedSeconds = (savedSeconds + seconds).coerceIn(minSeconds, 30F)
            increment = savedSeconds.roundToLong() * 1_000L
            applyConfig()
        }
    }
}

/**
 * Remember a [ChessClockModel] that survives recomposition and activity or process recreation.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriodMillis] Period between each tick in milliseconds.
 * @param[onStart] Callback called when the clock starts ticking.
 * @param[onPause] Callback called when the clock stops ticking.
 */
@Composable
fun rememberChessClockModel(
    durationMinutes: Long,
    incrementSeconds: Long,
    tickPeriodMillis: Long,
    onStart: () -> Unit = {},
    onPause: () -> Unit = {},
): ChessClockModel {
    val duration = durationMinutes * 60_000L
    val increment = incrementSeconds * 1_000L
    return ChessClockModel(
        defaultDuration = duration,
        defaultIncrement = increment,
        tickPeriod = tickPeriodMillis,
        onStart = onStart,
        onPause = onPause,
        durationState = rememberSaveable { mutableLongStateOf(duration) },
        incrementState = rememberSaveable { mutableLongStateOf(increment) },
        whiteTimeState = rememberSaveable { mutableLongStateOf(duration + increment) },
        blackTimeState = rememberSaveable { mutableLongStateOf(duration + increment) },
        targetRealtimeState = rememberSaveable { mutableLongStateOf(0L) },
        isWhiteTurnState = rememberSaveable { mutableStateOf(true) },
        isStartedState = rememberSaveable { mutableStateOf(false) },
        isTickingState = rememberSaveable { mutableStateOf(false) },
    )
}

/**
 * Screen-level composable that controls a chess [clock] model through clicking, dragging, key
 * presses and back events, and where in portrait mode the dragging direction is reversed depending
 * on whether the device [isLeaningRight].
 */
@Composable
fun ChessClockController(
    clock: ChessClockModel,
    isLeaningRight: () -> Boolean,
    dragSensitivity: Float = 0.01F,
    content: @Composable (whiteTime: Long, blackTime: Long) -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    LaunchedEffect(clock.isTicking, clock.whiteTime, clock.blackTime) { clock.tick() }

    BackHandler(clock.isTicking) { clock.pause() }

    BackHandler(!clock.isTicking && clock.isStarted) { clock.resetTime() }

    BackHandler(!clock.isStarted && !clock.isDefaultConfig) { clock.restoreDefaultConfig() }

    Box(modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { if (clock.isPaused) clock.start() else clock.nextTurn() },
        )
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { if (clock.isPaused) clock.saveTime() },
                onDragEnd = { if (!clock.isPaused) clock.nextTurn() },
                onHorizontalDrag = { _: PointerInputChange, dragAmount: Float ->
                    if (clock.isPaused) {
                        if (isLandscape) {
                            val sign = if (isRtl) -1F else 1F
                            val minutes = sign * dragSensitivity * dragAmount
                            clock.addMinutes(minutes, isAddedToSavedTime = true)
                        } else {
                            val sign = if (isLeaningRight.invoke()) -1F else 1F
                            val seconds = sign * dragSensitivity * dragAmount
                            clock.addSeconds(seconds, isAddedToSavedTime = true)
                        }
                    }
                },
            )
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { if (clock.isPaused) clock.saveTime() },
                onDragEnd = { if (!clock.isPaused) clock.nextTurn() },
                onVerticalDrag = { _: PointerInputChange, dragAmount: Float ->
                    if (clock.isPaused) {
                        if (isLandscape) {
                            val sign = -1F
                            val seconds = sign * dragSensitivity * dragAmount
                            clock.addSeconds(seconds, isAddedToSavedTime = true)
                        } else {
                            val sign = if (isLeaningRight.invoke() xor isRtl) -1F else 1F
                            val minutes = sign * dragSensitivity * dragAmount
                            clock.addMinutes(minutes, isAddedToSavedTime = true)
                        }
                    }
                },
            )
        }
        .onKeyEvent(onKeyEvent = {
            var isConsumed = false
            if (it.type == KeyEventType.KeyDown) {
                isConsumed = true
                if (clock.isPaused) {
                    when (it.key) {
                        Key.DirectionUp -> clock.addSeconds(1F)
                        Key.DirectionDown -> clock.addSeconds(-1F)
                        Key.DirectionRight -> clock.addMinutes(if (isRtl) -1F else 1F)
                        Key.DirectionLeft -> clock.addMinutes(if (isRtl) 1F else -1F)
                        else -> isConsumed = false
                    }
                }
            }
            isConsumed
        })
    ) {
        content.invoke(clock.whiteTime, clock.blackTime)
    }
}

/**
 * Basic element that displays a [timeMillis] in the form "MM:SS.D" or "H:MM:SS.D", in a given
 * [style] and accepting a given [modifier] to apply to its layout node.
 */
@Composable
fun BasicTime(
    timeMillis: Long, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default
) {
    val timeTenthsOfSeconds = (timeMillis + 99L) / 100L  // round up to the nearest tenth of second
    val hours = timeTenthsOfSeconds / 36_000L
    val minutes = timeTenthsOfSeconds % 36_000L / 600L
    val seconds = timeTenthsOfSeconds % 600L / 10L
    val tenthsOfSeconds = timeTenthsOfSeconds % 10L
    val monospace = style.merge(fontFamily = FontFamily.Monospace)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier) {
            if (hours != 0L) {
                BasicText("$hours", style = monospace)
                BasicText(":", style = style)
            }
            BasicText("$minutes".padStart(2, '0'), style = monospace)
            BasicText(":", style = style)
            BasicText("$seconds".padStart(2, '0'), style = monospace)
            if (hours == 0L) {
                BasicText(".", style = style)
                BasicText("$tenthsOfSeconds", style = monospace)
            }
        }
    }
}

/**
 * Minimal chess clock displaying remaining [whiteTime] and [blackTime] with a ninety degree
 * rotation in portrait mode in an orientation that depends on whether the device [isLeaningRight].
 */
@Preview
@Composable
fun ChessClockView(
    whiteTime: Long = 303_000L, blackTime: Long = 303_000L, isLeaningRight: Boolean = true
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val rotation = if (isLandscape) {
        0F
    } else {
        if (isLeaningRight) -90F else 90F
    }
    val textHeight = LocalConfiguration.current.screenHeightDp.dp / if (isLandscape) 3 else 8
    val fontSize = with(LocalDensity.current) { textHeight.toSp() }

    Column {
        BasicTime(
            blackTime,
            modifier = Modifier
                .background(Color.Black)
                .rotate(rotation)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(
                color = if (blackTime > 0L) Color.White else Color.Red,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            ),
        )
        BasicTime(
            whiteTime,
            modifier = Modifier
                .background(Color.White)
                .rotate(rotation)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(
                color = if (whiteTime > 0L) Color.Black else Color.Red,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            ),
        )
    }
}
