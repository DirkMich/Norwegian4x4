package com.example.norwegian4x4

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontFamily
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

            Norwegian4x4Theme {
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

// ------------------------------------------------------------- shared bits

/** Small uppercase, letter-spaced section label — the instrument-panel voice of the UI. */
@Composable
private fun SectionLabel(text: String, color: Color = SteelGray) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = color,
    )
}

/** A hard 1dp rule instead of soft spacing — utilitarian, not decorative. */
@Composable
private fun HairlineDivider(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().height(1.dp).background(Hairline))
}

/** A bordered, sharp-cornered data readout: label on top, big mono value below. */
@Composable
private fun StatTile(label: String, value: String, valueColor: Color = IceWhite, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(BorderStroke(1.dp, Hairline))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionLabel(label)
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
        )
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
        Text(
            "NORWEGIAN 4X4",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = IceWhite,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$intervals intervals • ~${workoutMinutes(intervals)} min",
            fontSize = 11.sp,
            color = SteelGray,
        )
        Text(
            "Work zone ${(maxHr * 0.85).toInt()}–${(maxHr * 0.95).toInt()} bpm",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = SignalAmber,
        )
        Spacer(Modifier.height(10.dp))
        Chip(
            onClick = onStart,
            label = { Text("START WORKOUT", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
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
            Text(error, fontSize = 10.sp, color = AlertRed, textAlign = TextAlign.Center)
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
        SectionLabel("Settings", color = IceWhite)
        Spacer(Modifier.height(10.dp))

        SectionLabel("Max heart rate")
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
                fontFamily = FontFamily.Monospace,
            )
            Button(
                onClick = { maxHr = (maxHr + 1).coerceAtMost(220); Prefs.setMaxHr(context, maxHr) },
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) { Text("+", fontSize = 16.sp) }
        }
        Text(
            "Work zone ${(maxHr * 0.85).toInt()}–${(maxHr * 0.95).toInt()} bpm",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = SignalAmber,
        )
        Spacer(Modifier.height(12.dp))

        SectionLabel("Hard intervals")
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
                fontFamily = FontFamily.Monospace,
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
            color = SteelGray,
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
            color = SteelGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        Chip(
            onClick = onBack,
            label = { Text("Done", fontWeight = FontWeight.Bold) },
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
        PhaseType.WORK -> SignalAmber
        PhaseType.RECOVERY, PhaseType.WARMUP, PhaseType.COOLDOWN -> PhaseEasy
    }
    val guidanceColor = when {
        state.paused -> PausedAmber
        state.guidance == Guidance.SPEED_UP -> FrostBlue
        state.guidance == Guidance.SLOW_DOWN -> AlertRed
        state.guidance == Guidance.IN_ZONE -> GlacialCyan
        else -> SteelGray
    }
    val guidanceText = when {
        state.paused -> "PAUSED"
        state.guidance == Guidance.SPEED_UP -> "SPEED UP ▲"
        state.guidance == Guidance.SLOW_DOWN -> "SLOW DOWN ▼"
        state.guidance == Guidance.IN_ZONE ->
            if (state.phaseType == PhaseType.WARMUP || state.phaseType == PhaseType.COOLDOWN)
                "EASY PACE" else "IN ZONE ✓"
        else -> "reading HR…"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            state.phaseLabel.uppercase(),
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            color = phaseColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            formatClock(state.secondsLeft),
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (state.paused) PausedAmber else IceWhite,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (state.hr > 0) "${state.hr}" else "--",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = guidanceColor,
            )
            Text(
                " bpm  (${state.targetLow}–${state.targetHigh})",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = SteelGray,
            )
        }
        Text(guidanceText, fontSize = 14.sp, letterSpacing = 0.5.sp, color = guidanceColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row {
            Text("${formatPace(state.speedMps)} /km", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(10.dp))
            Text(
                String.format(java.util.Locale.US, "%.2f km", state.distanceM / 1000.0),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
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

    val avgPace = if (state.distanceM > 50 && state.elapsedSec > 0) {
        formatPace(state.distanceM / state.elapsedSec)
    } else "--:--"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionLabel("Workout complete", color = GlacialCyan)
        Text(
            formatClock(state.elapsedSec),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = IceWhite,
        )
        Spacer(Modifier.height(8.dp))

        val summary = state.summary
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StatTile(
                "Distance",
                String.format(java.util.Locale.US, "%.2f km", state.distanceM / 1000.0),
                modifier = Modifier.weight(1f),
            )
            StatTile("Avg pace", "$avgPace/km", modifier = Modifier.weight(1f))
        }
        if (summary != null) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatTile(
                    "Avg / Max HR",
                    "${summary.avgHr}/${summary.maxHr}",
                    valueColor = SignalAmber,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    "Time in zone",
                    "${summary.workTimeInZonePct}%",
                    valueColor = if (summary.workTimeInZonePct >= 60) GlacialCyan else AlertRed,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (summary != null && summary.intervals.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HairlineDivider()
            Spacer(Modifier.height(8.dp))
            SectionLabel("Intervals", color = IceWhite)
            Spacer(Modifier.height(6.dp))
            summary.intervals.forEach { interval ->
                IntervalRow(interval)
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(4.dp))
        HairlineDivider()
        Spacer(Modifier.height(8.dp))

        if (state.savedFile != null) {
            when {
                uploaded -> {
                    Text("Uploaded to Strava ✓", fontSize = 12.sp, color = GlacialCyan)
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
                                    description = "$intervals x 4 min intervals • recorded on Galaxy Watch",
                                )
                                uploading = false
                                result
                                    .onSuccess {
                                        uploaded = true
                                        uploadMessage = "Processing on Strava…"
                                    }
                                    .onFailure {
                                        uploadMessage = it.message ?: "Upload failed"
                                    }
                            }
                        },
                        label = {
                            Text(
                                if (uploading) "Uploading…" else "Upload to Strava",
                                fontSize = 12.sp,
                            )
                        },
                        colors = ChipDefaults.primaryChipColors(),
                    )
                }
                else -> {
                    Text(
                        "Sent to phone ✓",
                        fontSize = 12.sp,
                        color = GlacialCyan,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Check the notification on your phone to share it to Strava",
                        fontSize = 9.sp,
                        color = SteelGray,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            uploadMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    fontSize = 10.sp,
                    color = if (uploaded) SteelGray else AlertRed,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (state.error != null) {
            Text(state.error, fontSize = 10.sp, color = AlertRed, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(8.dp))
        Chip(
            onClick = onDone,
            label = { Text("Done", fontWeight = FontWeight.Bold) },
            colors = ChipDefaults.primaryChipColors(),
        )
    }
}

/** One bordered row in the interval breakdown: effort stats on top, recovery on the second line. */
@Composable
private fun IntervalRow(interval: IntervalSummary) {
    val zoneColor = if (interval.timeInZonePct >= 60) GlacialCyan else AlertRed
    Column(
        Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Hairline))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "INTERVAL ${interval.index}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = SignalAmber,
            )
            Text(
                "${interval.avgHr}/${interval.maxHr} bpm",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = IceWhite,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${formatPaceSec(interval.paceSecPerKm)}/km",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = SteelGray,
            )
            Text(
                "${interval.timeInZonePct}% in zone",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = zoneColor,
            )
        }
        if (interval.recoveryAvgHr > 0) {
            Text(
                "Recovery avg ${interval.recoveryAvgHr} • low ${interval.recoveryMinHr} bpm",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = PhaseEasy,
            )
        }
    }
}
