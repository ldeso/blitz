package net.leodesouza.blitz

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.SystemClock.elapsedRealtime
import android.text.format.DateUtils.formatElapsedTime
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Black.toArgb()
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            CountDown(303_000L, 3_000L) { whiteTime, blackTime, onClick ->
                ChessClock(whiteTime, blackTime, onClick)
            }
        }
    }
}

@Composable
fun CountDown(
    duration: Long,
    increment: Long,
    content: @Composable (whiteTime: Long, blackTime: Long, onClick: () -> Unit) -> Unit
) {
    var whiteTime by remember { mutableLongStateOf(duration) }
    var blackTime by remember { mutableLongStateOf(duration) }
    var endTime by remember { mutableLongStateOf(0L) }
    var isWhiteTurn by remember { mutableStateOf(true) }
    var isRunning by remember { mutableStateOf(false) }
    content.invoke(whiteTime, blackTime) {
        if (whiteTime > 0L && blackTime > 0) {
            if (isRunning) {
                if (isWhiteTurn) {
                    whiteTime += increment
                } else {
                    blackTime += increment
                }
                isWhiteTurn = !isWhiteTurn
            }
            endTime = elapsedRealtime() + if (isWhiteTurn) whiteTime else blackTime
            isRunning = true
        }
    }
    BackHandler(isRunning) {
        isRunning = false
    }
    BackHandler(!(isRunning && whiteTime > 0L && blackTime > 0L)) {
        whiteTime = duration
        blackTime = duration
    }
    LaunchedEffect(whiteTime, blackTime, isRunning) {
        if (isRunning && elapsedRealtime() < endTime) {
            delay((endTime - elapsedRealtime()) % 100L)
            if (isWhiteTurn) {
                whiteTime = endTime - elapsedRealtime()
            } else {
                blackTime = endTime - elapsedRealtime()
            }
        }
    }
}

@Composable
fun ChessClock(whiteTime: Long, blackTime: Long, onClick: () -> Unit) {
    Surface(onClick) {
        Column {
            Time(
                blackTime,
                color = if (blackTime > 0L) Color.White else Color.Red,
                modifier = Modifier
                    .background(Color.Black)
                    .rotate(-90F)
                    .weight(1F)
                    .fillMaxSize()
                    .wrapContentSize()
            )
            Time(
                whiteTime,
                color = if (whiteTime > 0L) Color.Black else Color.Red,
                modifier = Modifier
                    .background(Color.White)
                    .rotate(-90F)
                    .weight(1F)
                    .fillMaxSize()
                    .wrapContentSize()
            )
        }
    }
}

@Composable
fun Time(timeMillis: Long, color: Color, modifier: Modifier = Modifier) {
    val roundedTime = (timeMillis + 50L) / 100L
    val integerPart = formatElapsedTime(roundedTime / 10L)
    val decimalPart = roundedTime % 10L
    Text(
        text = "$integerPart.$decimalPart",
        modifier = modifier,
        color = color,
        fontSize = with(LocalDensity.current) {
            (LocalConfiguration.current.screenHeightDp / 8).dp.toSp()
        },
    )
}

@Preview
@Composable
fun ChessClockPreview() {
    CountDown(10_000L, 1_000L) { whiteTime, blackTime, onClick ->
        ChessClock(whiteTime, blackTime, onClick)
    }
}
