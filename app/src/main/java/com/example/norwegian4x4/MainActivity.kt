package com.example.norwegian4x4

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import java.io.File

private val ColorInZone = Color(0xFF66BB6A)
private val ColorSpeedUp = Color(0xFF42A5F5)
private val ColorSlowDown = Color(0xFFEF5350)
private val ColorWork = Color(0xFFFFA726)
private val ColorEasy = Color(0xFF80CBC4)
private val ColorPaused = Color(0xFFFFCA28)

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted[Manifest.permission.BODY_SENSORS] == true &&
                granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                startWorkout()
            }
        }

    private fun sendAction(action: String) {
        startService(Intent(this, ExerciseService::class.java).apply { this.action = action })
    }

    private fun startWorkout() {
        startForegroundService(
            Intent(this, ExerciseService::class.java).apply {
                action = ExerciseService.ACTION_START
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val state by ExerciseService.state.collectAsStateWithLifecycle()
            var showSettings by remember { mutableStateOf(false) }

            val view = LocalView.current
            LaunchedEffect(state.running) {
                view.keepScreenOn = state.running && Prefs.getScreenOn(context)
            }

            MaterialTheme {
                when {
                    state.running -> WorkoutScreen(
                        state = state,
                        onPause = { sendAction(ExerciseService.ACTION_PAUSE) },
                        onResume = { sendAction(ExerciseService.ACTION_RESUME) },
                        onEnd = { sendAction(ExerciseService.ACTION_END) },
                    )
                    state.ended -> SummaryScreen(state) { ExerciseService.reset() }
                    showSettings -> SettingsScreen { showSettings = false }
                    else -> HomeScreen(
                        error = state.error,
                        onStart = { permissionLauncher.launch(requiredPermissions) },
                        onSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------- home

@Composable
private fun HomeScreen(error: String?, onStart: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val maxHr = Prefs.getMaxHr(context)
    val intervals = Prefs.getIntervals(context)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Norwegian 4x4", style = MaterialTheme.typography.title3)
        Spacer(Modifier.height(4.dp))
        Text(
            "$intervals intervals \u2022 ~${workoutMinutes(intervals)} min",
            fontSize = 11.sp,
            color = Color.Gray,
        )
        Text(
            "Work zone ${(maxHr * 0.85).toInt()}\u2013${(maxHr * 0.95).toInt()} bpm",
            fontSize = 11.sp,
            color = ColorWork,
        )
        Spacer(Modifier.height(10.dp))
        Chip(
            onClick = onStart,
            label = { Text("Start workout") },
            colors = ChipDefaults.primaryChipColors(),
        )
        Spacer(Modifier.height(6.dp))
        Chip(
            onClick = onSettings,
            label = { Text("Settings", fontSize = 12.sp) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.height(32.dp),
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, fontSize = 10.sp, color = ColorSlowDown, textAlign = TextAlign.Center)
        }
    }
}

// ----------------------------------------------------------------- settings

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var maxHr by remember { mutableIntStateOf(Prefs.getMaxHr(context)) }
    var intervals by remember { mutableIntStateOf(Prefs.getIntervals(context)) }
    var screenOn by remember { mutableStateOf(Prefs.getScreenOn(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Settings", style = MaterialTheme.typography.title3)
        Spacer(Modifier.height(10.dp))

        Text("Max heart rate", fontSize = 12.sp, color = Color.Gray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { maxHr = (maxHr - 1).coerceAtLeast(120); Prefs.setMaxHr(context, maxHr) },
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) { Text("-", fontSize = 16.sp) }
            Text(
                "$maxHr",
                modifier = Modifier.padding(horizontal = 14.dp),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = { maxHr = (maxHr + 1).coerceAtMost(220); Prefs.setMaxHr(context, maxHr) },
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) { Text("+", fontSize = 16.sp) }
        }
        Text(
            "Work zone ${(maxHr * 0.85).toInt()}\u2013${(maxHr * 0.95).toInt()} bpm",
            fontSize = 10.sp,
            color = ColorWork,
        )
        Spacer(Modifier.height(12.dp))

        Text("Hard intervals", fontSize = 12.sp, color = Color.Gray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { intervals = (intervals - 1).coerceAtLeast(1); Prefs.setIntervals(context, intervals) },
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) { Text("-", fontSize = 16.sp) }
            Text(
                "$intervals",
                modifier = Modifier.padding(horizontal = 14.dp),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = { intervals = (intervals + 1).coerceAtMost(8); Prefs.setIntervals(context, intervals) },
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) { Text("+", fontSize = 16.sp) }
        }
        Text(
            "~${workoutMinutes(intervals)} min total",
            fontSize = 10.sp,
            color = Color.Gray,
        )
        Spacer(Modifier.height(12.dp))

        Chip(
            onClick = {
                screenOn = !screenOn
                Prefs.setScreenOn(context, screenOn)
            },
            label = { Text("Screen always on: ${if (screenOn) "ON" else "OFF"}", fontSize = 12.sp) },
            colors = ChipDefaults.secondaryChipColors(),
        )
        Text(
            if (screenOn) "Display stays lit all workout (more battery)"
            else "Display dims as usual; raise wrist to check",
            fontSize = 9.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        Chip(
            onClick = onBack,
            label = { Text("Done") },
            colors = ChipDefaults.primaryChipColors(),
        )
    }
}

// ------------------------------------------------------------------ workout

@Composable
private fun WorkoutScreen(
    state: WorkoutState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    val phaseColor = when (state.phaseType) {
        PhaseType.WORK -> ColorWork
        PhaseType.RECOVERY, PhaseType.WARMUP, PhaseType.COOLDOWN -> ColorEasy
    }
    val guidanceColor = when {
        state.paused -> ColorPaused
        state.guidance == Guidance.SPEED_UP -> ColorSpeedUp
        state.guidance == Guidance.SLOW_DOWN -> ColorSlowDown
        state.guidance == Guidance.IN_ZONE -> ColorInZone
        else -> Color.Gray
    }
    val guidanceText = when {
        state.paused -> "PAUSED"
        state.guidance == Guidance.SPEED_UP -> "SPEED UP \u25B2"
        state.guidance == Guidance.SLOW_DOWN -> "SLOW DOWN \u25BC"
        state.guidance == Guidance.IN_ZONE ->
            if (state.phaseType == PhaseType.WARMUP || state.phaseType == PhaseType.COOLDOWN)
                "EASY PACE" else "IN ZONE \u2713"
        else -> "reading HR\u2026"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.phaseLabel, fontSize = 13.sp, color = phaseColor, fontWeight = FontWeight.Bold)
        Text(
            formatClock(state.secondsLeft),
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.paused) ColorPaused else Color.White,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (state.hr > 0) "${state.hr}" else "--",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = guidanceColor,
            )
            Text(" bpm  (${state.targetLow}\u2013${state.targetHigh})", fontSize = 11.sp, color = Color.Gray)
        }
        Text(guidanceText, fontSize = 14.sp, color = guidanceColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row {
            Text("${formatPace(state.speedMps)} /km", fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            Text(String.format(java.util.Locale.US, "%.2f km", state.distanceM / 1000.0), fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Chip(
                onClick = if (state.paused) onResume else onPause,
                label = { Text(if (state.paused) "Resume" else "Pause", fontSize = 11.sp) },
                colors = if (state.paused) ChipDefaults.primaryChipColors()
                else ChipDefaults.secondaryChipColors(),
                modifier = Modifier.height(30.dp),
            )
            Spacer(Modifier.width(6.dp))
            Chip(
                onClick = onEnd,
                label = { Text("End", fontSize = 11.sp) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.height(30.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ summary

@Composable
private fun SummaryScreen(state: WorkoutState, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var uploaded by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Workout done!", style = MaterialTheme.typography.title3, color = ColorInZone)
        Spacer(Modifier.height(6.dp))
        Text("Time  ${formatClock(state.elapsedSec)}", fontSize = 13.sp)
        Text(
            String.format(java.util.Locale.US, "Distance  %.2f km", state.distanceM / 1000.0),
            fontSize = 13.sp,
        )
        val avgPace = if (state.distanceM > 50 && state.elapsedSec > 0) {
            formatPace(state.distanceM / state.elapsedSec)
        } else "--:--"
        Text("Avg pace  $avgPace /km", fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        if (state.savedFile != null) {
            when {
                uploaded -> {
                    Text("Uploaded to Strava \u2713", fontSize = 12.sp, color = ColorInZone)
                }
                StravaSecrets.isConfigured -> {
                    Chip(
                        onClick = {
                            if (uploading) return@Chip
                            uploading = true
                            uploadMessage = null
                            scope.launch {
                                val file = File(context.getExternalFilesDir(null), state.savedFile)
                                val intervals = Prefs.getIntervals(context)
                                val result = StravaUploader.upload(
                                    context = context,
                                    file = file,
                                    name = "Norwegian 4x4",
                                    description = "$intervals x 4 min intervals \u2022 recorded on Galaxy Watch",
                                )
                                uploading = false
                                result
                                    .onSuccess {
                                        uploaded = true
                                        uploadMessage = "Processing on Strava\u2026"
                                    }
                                    .onFailure {
                                        uploadMessage = it.message ?: "Upload failed"
                                    }
                            }
                        },
                        label = {
                            Text(
                                if (uploading) "Uploading\u2026" else "Upload to Strava",
                                fontSize = 12.sp,
                            )
                        },
                        colors = ChipDefaults.primaryChipColors(),
                    )
                }
                else -> {
                    Text(
                        "Saved: ${state.savedFile}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Direct Strava upload not set up \u2014 see guide. File can be pulled with adb.",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            uploadMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    fontSize = 10.sp,
                    color = if (uploaded) Color.Gray else ColorSlowDown,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (state.error != null) {
            Text(state.error, fontSize = 10.sp, color = ColorSlowDown, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(8.dp))
        Chip(
            onClick = onDone,
            label = { Text("Done") },
            colors = ChipDefaults.primaryChipColors(),
        )
    }
}
