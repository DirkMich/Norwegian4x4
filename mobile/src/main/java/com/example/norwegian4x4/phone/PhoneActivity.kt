package com.example.norwegian4x4.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ZoneGreen = Color(0xFF43A047)
private val ZoneOrange = Color(0xFFFB8C00)
private val ChartBlue = Color(0xFF1E88E5)

class PhoneActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme {
                PhoneApp()
            }
        }
    }
}

// ---------------------------------------------------------------------- app

@Composable
private fun PhoneApp() {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Workouts") },
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.ShowChart, null) },
                    label = { Text("Progress") },
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text("Settings") },
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when (tab) {
                0 -> WorkoutsTab()
                1 -> ProgressTab()
                else -> SettingsTab()
            }
        }
    }
}

// ----------------------------------------------------------------- workouts

@Composable
private fun WorkoutsTab() {
    val context = LocalContext.current
    val changeTick by History.changes.collectAsState()
    val entries = remember(changeTick) { History.load(context) }
    val dateFmt = remember { SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault()) }

    if (entries.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No workouts yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Finish a workout on your watch and it will appear here automatically " +
                    "(phone and watch connected via Bluetooth).",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        items(entries) { e ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(dateFmt.format(Date(e.time)), style = MaterialTheme.typography.titleSmall)
                        Text(
                            String.format(
                                Locale.getDefault(),
                                "%.2f km \u2022 %s \u2022 avg %d bpm",
                                e.distanceM / 1000.0, clock(e.durationSec), e.avgHr,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (e.workAvgHr.isNotEmpty()) {
                            Text(
                                "Intervals: " + e.workAvgHr.joinToString(" \u2022 ") { "$it" } + " bpm",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                    }
                    IconButton(onClick = { shareTcx(context, e.filename) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share TCX")
                    }
                }
            }
        }
        item {
            Text(
                "Tap share, or find the files in Download/Norwegian4x4. " +
                    "Upload at strava.com/upload in your browser.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}

private fun shareTcx(context: Context, filename: String) {
    val file = File(History.workoutsDir(context), filename)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context, "com.example.norwegian4x4.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.garmin.tcx+xml"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share workout"))
}

// ----------------------------------------------------------------- progress

@Composable
private fun ProgressTab() {
    val context = LocalContext.current
    val changeTick by History.changes.collectAsState()
    val entries = remember(changeTick) { History.load(context).sortedBy { it.time } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        val totalKm = entries.sumOf { it.distanceM } / 1000.0
        val totalMin = entries.sumOf { it.durationSec } / 60
        Text("Progress", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "${entries.size} workouts \u2022 " +
                String.format(Locale.getDefault(), "%.1f km", totalKm) +
                " \u2022 $totalMin min total",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
        Spacer(Modifier.height(20.dp))

        if (entries.size < 2) {
            Text(
                "Charts appear after two or more workouts.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
            return@Column
        }

        // Chart 1: average heart rate across hard intervals, with target zone band.
        Text("Avg interval heart rate", style = MaterialTheme.typography.titleMedium)
        Text(
            "The green band is your 85\u201395% work zone. In-zone but with faster " +
                "pace over time = fitness improving.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
        Spacer(Modifier.height(8.dp))
        val hrValues = entries.map { e ->
            val work = e.workAvgHr.filter { it > 0 }
            if (work.isEmpty()) e.avgHr.toFloat() else work.average().toFloat()
        }
        val maxHr = entries.last().maxHrSetting.toFloat()
        LineChart(
            values = hrValues,
            band = (maxHr * 0.85f)..(maxHr * 0.95f),
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
        Spacer(Modifier.height(24.dp))

        // Chart 2: distance per workout.
        Text("Distance per workout", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        BarChart(
            values = entries.map { (it.distanceM / 1000.0).toFloat() },
            unit = "km",
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            "Oldest workout on the left, newest on the right.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
    }
}

@Composable
private fun LineChart(
    values: List<Float>,
    band: ClosedFloatingPointRange<Float>?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val allValues = values + listOfNotNull(band?.start, band?.endInclusive)
        val min = (allValues.min() - 5f)
        val max = (allValues.max() + 5f)
        fun y(v: Float) = size.height * (1f - (v - min) / (max - min))
        fun x(i: Int) = if (values.size == 1) size.width / 2f
        else size.width * i / (values.size - 1f)

        band?.let {
            drawRect(
                color = ZoneGreen.copy(alpha = 0.15f),
                topLeft = Offset(0f, y(it.endInclusive)),
                size = Size(size.width, y(it.start) - y(it.endInclusive)),
            )
        }
        for (i in 0 until values.size - 1) {
            drawLine(
                color = ChartBlue,
                start = Offset(x(i), y(values[i])),
                end = Offset(x(i + 1), y(values[i + 1])),
                strokeWidth = 5f,
            )
        }
        values.forEachIndexed { i, v ->
            drawCircle(ChartBlue, radius = 9f, center = Offset(x(i), y(v)))
        }
    }
}

@Composable
private fun BarChart(values: List<Float>, unit: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val max = (values.max() * 1.15f).coerceAtLeast(0.1f)
        val slot = size.width / values.size
        val barWidth = slot * 0.6f
        values.forEachIndexed { i, v ->
            val h = size.height * (v / max)
            drawRoundRect(
                color = ZoneOrange,
                topLeft = Offset(slot * i + (slot - barWidth) / 2f, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
    }
}

// ----------------------------------------------------------------- settings

private const val PREFS = "phone_prefs"

@Composable
private fun SettingsTab() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var maxHr by remember { mutableIntStateOf(prefs.getInt("maxHr", 190)) }
    var intervals by remember { mutableIntStateOf(prefs.getInt("intervals", 4)) }
    var screenOn by remember { mutableStateOf(prefs.getBoolean("screenOn", true)) }

    fun save() {
        prefs.edit().putInt("maxHr", maxHr).putInt("intervals", intervals)
            .putBoolean("screenOn", screenOn).apply()
        pushSettingsToWatch(context, maxHr, intervals, screenOn)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Changes sync to the watch automatically over Bluetooth.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
        Spacer(Modifier.height(20.dp))

        StepperRow(
            title = "Max heart rate",
            subtitle = "Work zone ${(maxHr * 0.85).toInt()}\u2013${(maxHr * 0.95).toInt()} bpm",
            value = maxHr,
            onChange = { maxHr = it.coerceIn(120, 220); save() },
        )
        Spacer(Modifier.height(16.dp))
        StepperRow(
            title = "Hard intervals",
            subtitle = "~${10 + intervals * 4 + (intervals - 1) * 3 + 5} min total workout",
            value = intervals,
            onChange = { intervals = it.coerceIn(1, 8); save() },
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Screen always on during workout", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (screenOn) "Easier to glance, more battery" else "Raise wrist to check",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            Switch(checked = screenOn, onCheckedChange = { screenOn = it; save() })
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Note: settings can also be changed on the watch itself; whichever " +
                "was changed last wins.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
    }
}

@Composable
private fun StepperRow(title: String, subtitle: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        OutlinedButton(onClick = { onChange(value - 1) }) { Text("\u2212") }
        Text(
            "$value",
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.titleLarge,
            fontSize = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        OutlinedButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

private fun pushSettingsToWatch(context: Context, maxHr: Int, intervals: Int, screenOn: Boolean) {
    runCatching {
        val request = PutDataMapRequest.create("/settings").apply {
            dataMap.putInt("maxHr", maxHr)
            dataMap.putInt("intervals", intervals)
            dataMap.putBoolean("screenOn", screenOn)
            dataMap.putLong("ts", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
    }
}

private fun clock(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
