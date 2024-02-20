package net.leodesouza.blitz

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock.elapsedRealtime
import android.text.format.DateUtils.formatElapsedTime
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
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
    var endTime by remember { mutableLongStateOf(elapsedRealtime() + duration) }
    var timesPressed by remember { mutableIntStateOf(0) }
    var isPressed by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    val onClick = {
        timesPressed++
        isPressed = true
    }
    content.invoke(whiteTime, blackTime, onClick)
    BackHandler(isRunning) {
        timesPressed--
        isRunning = false
    }
    LaunchedEffect(whiteTime, blackTime, isPressed) {
        val isBlackTurn = (timesPressed % 2 == 0)
        if (isPressed) {
            if (isRunning) {
                if (isBlackTurn) {
                    whiteTime += increment
                } else {
                    blackTime += increment
                }
            } else {
                isRunning = true
            }
            endTime = elapsedRealtime() + if (isBlackTurn) blackTime else whiteTime
            isPressed = false
        }
        if (isRunning) {
            val remainingTime = if (elapsedRealtime() < endTime) {
                delay((endTime - elapsedRealtime()) % 100L)
                endTime - elapsedRealtime()
            } else {
                0L
            }
            if (isBlackTurn) blackTime = remainingTime
            else whiteTime = remainingTime
        }
    }
}

@Composable
fun ChessClock(whiteTime: Long, blackTime: Long, onClick: () -> Unit) {
    Surface(onClick) {
        Row {
            Time(
                whiteTime,
                color = if (whiteTime > 0L)  Color.Black else Color.Red,
                modifier = Modifier
                    .background(Color.White)
                    .weight(1F)
                    .fillMaxSize()
                    .wrapContentSize()
            )
            Time(
                blackTime,
                color = if (blackTime > 0L) Color.White else Color.Red,
                modifier = Modifier
                    .background(Color.Black)
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
            (LocalConfiguration.current.screenWidthDp / 8).dp.toSp()
        },
    )
}

@Preview
@Composable
fun ChessClockPreview() {
    CountDown(13_000L, 3_000L) { whiteTime, blackTime, onClick ->
        ChessClock(whiteTime, blackTime, onClick)
    }
}
