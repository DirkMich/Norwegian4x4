package com.example.norwegian4x4

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.wear.compose.material.Text

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

    /** Wear OS side/stem buttons — toggle pause/resume during a workout instead of the default action. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val state = ExerciseService.state.value
        val isSideButton = keyCode == KeyEvent.KEYCODE_STEM_1 ||
            keyCode == KeyEvent.KEYCODE_STEM_2 ||
            keyCode == KeyEvent.KEYCODE_STEM_3 ||
            keyCode == KeyEvent.KEYCODE_BACK
        if (state.running && isSideButton) {
            sendAction(if (state.paused) ExerciseService.ACTION_RESUME else ExerciseService.ACTION_PAUSE)
            return true
        }
        return super.onKeyDown(keyCode, event)
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

/**
 * Horizontal inset that keeps content clear of the bezel. Round screens clip a
 * plain rectangular Column near the top/bottom corners, so they need noticeably
 * more side padding than square ones to keep edge content from being cut off.
 */
@Composable
private fun edgePadding(): androidx.compose.ui.unit.Dp =
    if (LocalConfiguration.current.isScreenRound) 18.dp else 8.dp

/** Small uppercase label — quiet by design so the big numbers next to it carry the eye. */
@Composable
private fun Label(text: String, color: Color = MistGray) {
    Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = color)
}

/** A rounded, filled "expressive container" — the M3 card grouping for related metrics. */
@Composable
private fun Container(
    modifier: Modifier = Modifier,
    tint: Color = SlateSurface,
    content: ColumnScopeContent,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(ContainerRadius))
            .background(tint)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

private typealias ColumnScopeContent = @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit

/** A tinted capsule badge for state/phase indicators — no borders, just fill + contrast. */
@Composable
private fun Pill(text: String, tint: Color, onTint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(tint)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = onTint)
    }
}

/** A live, glanceable HR waveform — scale and motion instead of a bare number. */
@Composable
private fun HrWaveform(history: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val values = history.filter { it > 0 }
        if (values.size < 2) return@Canvas
        val min = values.min().toFloat() - 3f
        val max = values.max().toFloat() + 3f
        val range = (max - min).coerceAtLeast(1f)
        fun y(v: Int) = size.height * (1f - (v - min) / range)
        fun x(i: Int) = size.width * i / (values.size - 1f)

        for (i in 0 until values.size - 1) {
            drawLine(
                color = color,
                start = Offset(x(i), y(values[i])),
                end = Offset(x(i + 1), y(values[i + 1])),
                strokeWidth = 4f,
            )
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = edgePadding(), vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("NORWEGIAN 4X4", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = FrostWhite)
        Spacer(Modifier.height(8.dp))
        Container {
            Text("$intervals", fontSize = 34.sp, fontWeight = FontWeight.Black, color = IceBlue)
            Label("intervals • ~${workoutMinutes(intervals)} min")
            Spacer(Modifier.height(2.dp))
            Text(
                "Zone ${(maxHr * 0.85).toInt()}–${(maxHr * 0.95).toInt()} bpm",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SignalAmber,
            )
        }
        Spacer(Modifier.height(12.dp))
        Chip(
            onClick = onStart,
            label = { Text("START", fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 15.sp) },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
        Spacer(Modifier.height(6.dp))
        Chip(
            onClick = onSettings,
            label = { Text("Settings", fontSize = 12.sp) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
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
            .padding(horizontal = edgePadding(), vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SETTINGS", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = FrostWhite)
        Spacer(Modifier.height(10.dp))

        Container(modifier = Modifier.fillMaxWidth()) {
            Label("Max heart rate")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { maxHr = (maxHr - 1).coerceAtLeast(120); Prefs.setMaxHr(context, maxHr) },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) { Text("-", fontSize = 16.sp) }
                Text(
                    "$maxHr",
                    modifier = Modifier.padding(horizontal = 14.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = FrostWhite,
                )
                Button(
                    onClick = { maxHr = (maxHr + 1).coerceAtMost(220); Prefs.setMaxHr(context, maxHr) },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) { Text("+", fontSize = 16.sp) }
            }
            Text(
                "Zone ${(maxHr * 0.85).toInt()}–${(maxHr * 0.95).toInt()} bpm",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = SignalAmber,
            )
        }
        Spacer(Modifier.height(10.dp))

        Container(modifier = Modifier.fillMaxWidth()) {
            Label("Hard intervals")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { intervals = (intervals - 1).coerceAtLeast(1); Prefs.setIntervals(context, intervals) },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) { Text("-", fontSize = 16.sp) }
                Text(
                    "$intervals",
                    modifier = Modifier.padding(horizontal = 14.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = FrostWhite,
                )
                Button(
                    onClick = { intervals = (intervals + 1).coerceAtMost(8); Prefs.setIntervals(context, intervals) },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) { Text("+", fontSize = 16.sp) }
            }
            Text("~${workoutMinutes(intervals)} min total", fontSize = 10.sp, color = MistGray)
        }
        Spacer(Modifier.height(10.dp))

        Chip(
            onClick = {
                screenOn = !screenOn
                Prefs.setScreenOn(context, screenOn)
            },
            label = { Text("Screen always on: ${if (screenOn) "ON" else "OFF"}", fontSize = 12.sp) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        )
        Text(
            if (screenOn) "Display stays lit all workout (more battery)"
            else "Display dims as usual; raise wrist to check",
            fontSize = 9.sp,
            color = MistGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))

        Chip(
            onClick = onBack,
            label = { Text("DONE", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp) },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
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
        state.guidance == Guidance.SPEED_UP -> IceBlue
        state.guidance == Guidance.SLOW_DOWN -> AlertRed
        state.guidance == Guidance.IN_ZONE -> IceBlue
        else -> MistGray
    }
    val guidanceText = when {
        state.paused -> "PAUSED"
        state.guidance == Guidance.SPEED_UP -> "SPEED UP ▲"
        state.guidance == Guidance.SLOW_DOWN -> "SLOW DOWN ▼"
        state.guidance == Guidance.IN_ZONE ->
            if (state.phaseType == PhaseType.WARMUP || state.phaseType == PhaseType.COOLDOWN)
                "EASY PACE" else "IN ZONE ✓"
        else -> "READING HR…"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = edgePadding(), vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Pill(state.phaseLabel.uppercase(), tint = phaseColor.copy(alpha = 0.22f), onTint = phaseColor)
        Spacer(Modifier.height(4.dp))
        Text(
            formatClock(state.secondsLeft),
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = if (state.paused) PausedAmber else FrostWhite,
        )
        HrWaveform(state.hrHistory, guidanceColor, Modifier.fillMaxWidth().height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (state.hr > 0) "${state.hr}" else "--",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = guidanceColor,
            )
            Text(" bpm  (${state.targetLow}–${state.targetHigh})", fontSize = 10.sp, color = MistGray)
        }
        Spacer(Modifier.height(2.dp))
        Pill(guidanceText, tint = guidanceColor.copy(alpha = 0.22f), onTint = guidanceColor)
        Spacer(Modifier.height(6.dp))
        Row {
            Text("${formatPace(state.speedMps)} /km", fontSize = 12.sp, color = FrostWhite)
            Spacer(Modifier.width(10.dp))
            Text(
                String.format(java.util.Locale.US, "%.2f km", state.distanceM / 1000.0),
                fontSize = 12.sp,
                color = FrostWhite,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(
                onClick = if (state.paused) onResume else onPause,
                label = { Text(if (state.paused) "Resume" else "Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = if (state.paused) ChipDefaults.primaryChipColors()
                else ChipDefaults.secondaryChipColors(),
                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
            )
            Chip(
                onClick = onEnd,
                label = { Text("End", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ summary

@Composable
private fun SummaryScreen(state: WorkoutState, onDone: () -> Unit) {
    val avgPace = if (state.distanceM > 50 && state.elapsedSec > 0) {
        formatPace(state.distanceM / state.elapsedSec)
    } else "--:--"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = edgePadding(), vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("WORKOUT COMPLETE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = IceBlue)
        Text(formatClock(state.elapsedSec), fontSize = 36.sp, fontWeight = FontWeight.Black, color = FrostWhite)
        Spacer(Modifier.height(8.dp))

        val summary = state.summary
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Container(modifier = Modifier.weight(1f)) {
                Text(
                    String.format(java.util.Locale.US, "%.2f", state.distanceM / 1000.0),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = FrostWhite,
                )
                Label("km")
            }
            Container(modifier = Modifier.weight(1f)) {
                Text(avgPace, fontSize = 20.sp, fontWeight = FontWeight.Black, color = FrostWhite)
                Label("min/km")
            }
        }
        if (summary != null) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Container(modifier = Modifier.weight(1f)) {
                    Text("${summary.avgHr}/${summary.maxHr}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SignalAmber)
                    Label("avg/max bpm")
                }
                Container(modifier = Modifier.weight(1f)) {
                    Text(
                        "${summary.workTimeInZonePct}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (summary.workTimeInZonePct >= 60) IceBlue else AlertRed,
                    )
                    Label("in zone")
                }
            }
        }

        if (summary != null && summary.intervals.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("INTERVALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = FrostWhite)
            Spacer(Modifier.height(6.dp))
            summary.intervals.forEach { interval ->
                IntervalCard(interval)
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        if (state.savedFile != null) {
            Pill("SENT TO PHONE ✓", tint = IceBlue.copy(alpha = 0.22f), onTint = IceBlue)
            Spacer(Modifier.height(6.dp))
            Text(
                "Open the phone app to upload it to Strava",
                fontSize = 9.sp,
                color = MistGray,
                textAlign = TextAlign.Center,
            )
        } else if (state.error != null) {
            Text(state.error, fontSize = 10.sp, color = AlertRed, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(10.dp))
        Chip(
            onClick = onDone,
            label = { Text("DONE", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp) },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        )
    }
}

/** One expressive card in the interval breakdown: effort stats + post-interval recovery. */
@Composable
private fun IntervalCard(interval: IntervalSummary) {
    val zoneColor = if (interval.timeInZonePct >= 60) IceBlue else AlertRed
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ContainerRadius))
            .background(SlateSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                letterSpacing = 0.8.sp,
                color = SignalAmber,
            )
            Text(
                "${interval.avgHr}/${interval.maxHr} bpm",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = FrostWhite,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${formatPaceSec(interval.paceSecPerKm)}/km", fontSize = 11.sp, color = MistGray)
            Text("${interval.timeInZonePct}% in zone", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = zoneColor)
        }
        if (interval.recoveryAvgHr > 0) {
            Text(
                "Recovery avg ${interval.recoveryAvgHr} • low ${interval.recoveryMinHr} bpm",
                fontSize = 10.sp,
                color = PhaseEasy,
            )
        }
    }
}
