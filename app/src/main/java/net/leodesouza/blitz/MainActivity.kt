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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
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
    /** Mutable state keeping track of the whether the device is leaning right. */
    private val isLeaningRight = mutableStateOf(true)

    /** Event listener updating [isLeaningRight] based on the orientation of the device. */
    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (getDisplayOrDefault(this@MainActivity).rotation) {
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
 * Model for a two-player chess clock with Fischer time increment.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriodMillis] Period between each tick in milliseconds.
 * @param[onStart] Callback called when the clock starts ticking.
 * @param[onPause] Callback called when the clock stops ticking.
 */
@Stable
class ChessClockModel(
    durationMinutes: Long,
    incrementSeconds: Long,
    private val tickPeriodMillis: Long,
    private val onStart: () -> Unit = {},
    private val onPause: () -> Unit = {},
) {
    constructor(
        durationMinutes: Long,
        incrementSeconds: Long,
        tickPeriodMillis: Long,
        onStart: () -> Unit = {},
        onPause: () -> Unit = {},
        currentDuration: Long,
        currentIncrement: Long,
        whiteTime: Long,
        blackTime: Long,
        targetRealtime: Long,
        isWhiteTurn: Boolean,
        isStarted: Boolean,
        isTicking: Boolean,
    ) : this(durationMinutes, incrementSeconds, tickPeriodMillis, onStart, onPause) {
        this.currentDuration = currentDuration
        this.currentIncrement = currentIncrement
        this.whiteTime = whiteTime
        this.blackTime = blackTime
        this.targetRealtime = targetRealtime
        this.isWhiteTurn = isWhiteTurn
        this.isStarted = isStarted
        this.isTicking = isTicking
    }

    /** Default initial time for each player in milliseconds. */
    private val defaultDuration = durationMinutes * 60_000L

    /** Default time increment in milliseconds. */
    private val defaultIncrement = incrementSeconds * 1_000L

    /** Current initial time for each player in milliseconds. */
    var currentDuration: Long by mutableLongStateOf(defaultDuration)
        private set

    /** Current time increment in milliseconds. */
    var currentIncrement: Long by mutableLongStateOf(defaultIncrement)
        private set

    /** Time for the first player in milliseconds. */
    var whiteTime: Long by mutableLongStateOf(defaultDuration + defaultIncrement)
        private set

    /** Time for the second player in milliseconds. */
    var blackTime: Long by mutableLongStateOf(whiteTime)
        private set

    /** Time that should be returned by [elapsedRealtime] when the current time reaches zero. */
    var targetRealtime: Long by mutableLongStateOf(0L)
        private set

    /** Whether it is the turn of the first or the second player. */
    var isWhiteTurn: Boolean by mutableStateOf(true)
        private set

    /** Whether the clock has started ticking. */
    var isStarted: Boolean by mutableStateOf(false)
        private set

    /** Whether the clock is currently ticking. */
    var isTicking: Boolean by mutableStateOf(false)
        private set

    /** Whether the clock has finished ticking. */
    private val isFinished: Boolean
        get() = whiteTime <= 0L || blackTime <= 0L

    /** Whether the clock is currently on pause. */
    val isPaused: Boolean
        get() = !isTicking && !isFinished

    /** Whether the current duration and time increment are set to their default configuration. */
    val isDefaultConfig: Boolean
        get() = currentDuration == defaultDuration && currentIncrement == defaultIncrement

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

    /** If the clock is ticking and has not finished, wait until next tick and update the time. */
    suspend fun tick() {
        if (isTicking) {
            if (isFinished) {
                pause()
            } else {
                val remainingTime = targetRealtime - elapsedRealtime()
                val delayMillis = remainingTime % tickPeriodMillis
                delay(delayMillis)
                currentTime = remainingTime - delayMillis
            }
        }
    }

    /** If the clock is ticking, switch between ticking for the first and for the second player. */
    fun nextPlayer() {
        if (isTicking) {
            val remainingTime = targetRealtime - elapsedRealtime()
            currentTime = if (remainingTime > 0L) (remainingTime + currentIncrement) else 0L
            isWhiteTurn = !isWhiteTurn
            targetRealtime = elapsedRealtime() + currentTime
        }
    }

    /** Set the time for each player according to the current duration and time increment. */
    private fun applyConfig() {
        val newTime = currentDuration + currentIncrement
        whiteTime = newTime
        blackTime = newTime
    }

    /** Reset the clock to its initial state with the current duration and time increment. */
    fun resetTime() {
        isWhiteTurn = true
        isStarted = false
        applyConfig()
    }

    /** Restore the the default duration and time increment. */
    fun resetConfig() {
        currentDuration = defaultDuration
        currentIncrement = defaultIncrement
        applyConfig()
    }

    /** Duration in minutes that can be used as a reference to add minutes to with [addMinutes]. */
    private var savedDurationMinutes: Float = 0F
        set(minutes) {
            field = minutes.coerceIn(
                minimumValue = if (currentIncrement < 1_000L) 1F else 0F,
                maximumValue = 180F,
            )
        }

    /** Increment in seconds that can be used as a reference to add seconds to with [addSeconds]. */
    private var savedIncrementSeconds: Float = 0F
        set(seconds) {
            field = seconds.coerceIn(
                minimumValue = if (currentDuration < 60_000L) 1F else 0F,
                maximumValue = 30F,
            )
        }

    /** Remaining minutes that can be used as a reference to add minutes to with [addMinutes]. */
    private var savedMinutes: Float = 0F
        set(minutes) {
            val seconds = savedSeconds.roundToLong()
            field = minutes.coerceIn(
                minimumValue = (-seconds / 60L).toFloat() + if (seconds % 60L == 0L) 1F else 0F,
                maximumValue = (599L - seconds / 60L).toFloat(),
            )
        }

    /** Remaining seconds that can be used as a reference to add seconds to with [addSeconds]. */
    private var savedSeconds: Float = 0F
        set(seconds) {
            val minutes = savedMinutes.roundToLong()
            field = seconds.coerceIn(
                minimumValue = (1L - minutes * 60L).toFloat(),
                maximumValue = (35_999L - minutes * 60L).toFloat(),
            )
        }

    /** Save current time or duration/increment as a reference for [addMinutes] and [addSeconds]. */
    fun saveTime() {
        if (isStarted) {
            savedMinutes = (currentTime / 60_000L).toFloat()
            savedSeconds = (currentTime % 60_000L / 1_000L).toFloat()
        } else {
            savedDurationMinutes = (currentDuration / 60_000L).toFloat()
            savedIncrementSeconds = (currentIncrement / 1_000L).toFloat()
        }
    }

    /** Add [minutes] to the current or saved minutes depending on whether [isAddedToSavedTime]. */
    fun addMinutes(minutes: Float, isAddedToSavedTime: Boolean = false) {
        if (!isAddedToSavedTime) saveTime()
        if (isStarted) {
            savedMinutes += minutes
            currentTime = savedMinutes.roundToLong() * 60_000L + savedSeconds.roundToLong() * 1_000L
        } else {
            savedDurationMinutes += minutes
            currentDuration = savedDurationMinutes.roundToLong() * 60_000L
            applyConfig()
        }
    }

    /** Add [seconds] to the current or saved seconds depending on whether [isAddedToSavedTime]. */
    fun addSeconds(seconds: Float, isAddedToSavedTime: Boolean = false) {
        if (!isAddedToSavedTime) saveTime()
        if (isStarted) {
            savedSeconds += seconds
            currentTime = savedMinutes.roundToLong() * 60_000L + savedSeconds.roundToLong() * 1_000L
        } else {
            savedIncrementSeconds += seconds
            currentIncrement = savedIncrementSeconds.roundToLong() * 1_000L
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
    return rememberSaveable(
        saver = mapSaver(save = {
            mapOf(
                "currentDuration" to it.currentDuration,
                "currentIncrement" to it.currentIncrement,
                "whiteTime" to it.whiteTime,
                "blackTime" to it.blackTime,
                "targetRealtime" to it.targetRealtime,
                "isWhiteTurn" to it.isWhiteTurn,
                "isStarted" to it.isStarted,
                "isTicking" to it.isTicking,
            )
        }, restore = {
            ChessClockModel(
                durationMinutes = durationMinutes,
                incrementSeconds = incrementSeconds,
                tickPeriodMillis = tickPeriodMillis,
                onStart = onStart,
                onPause = onPause,
                currentDuration = it["currentDuration"] as Long,
                currentIncrement = it["currentIncrement"] as Long,
                whiteTime = it["whiteTime"] as Long,
                blackTime = it["blackTime"] as Long,
                targetRealtime = it["targetRealtime"] as Long,
                isWhiteTurn = it["isWhiteTurn"] as Boolean,
                isStarted = it["isStarted"] as Boolean,
                isTicking = it["isTicking"] as Boolean,
            )
        })
    ) {
        ChessClockModel(durationMinutes, incrementSeconds, tickPeriodMillis, onStart, onPause)
    }
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

    BackHandler(!clock.isStarted && !clock.isDefaultConfig) { clock.resetConfig() }

    Box(modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { if (clock.isPaused) clock.start() else clock.nextPlayer() },
        )
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { if (clock.isPaused) clock.saveTime() },
                onDragEnd = { if (!clock.isPaused) clock.nextPlayer() },
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
                onDragEnd = { if (!clock.isPaused) clock.nextPlayer() },
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
