package com.example.norwegian4x4.phone

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stores one summary entry per received workout in a JSON file, plus the TCX
 * files themselves under filesDir/workouts/. Emits on [changes] whenever a new
 * workout arrives so open UI can refresh.
 */
object History {

    data class Entry(
        val time: Long,
        val durationSec: Int,
        val distanceM: Double,
        val avgHr: Int,
        val maxHrSetting: Int,
        val workAvgHr: List<Int>,
        val filename: String,
    )

    /** Bumped whenever the history changes; collect to refresh UI. */
    val changes = MutableStateFlow(0L)

    private fun historyFile(context: Context) = File(context.filesDir, "history.json")

    fun workoutsDir(context: Context): File =
        File(context.filesDir, "workouts").apply { mkdirs() }

    @Synchronized
    fun add(context: Context, summaryJson: String) {
        val arr = readArray(context)
        val incoming = JSONObject(summaryJson)
        // Skip duplicates (Data Layer can redeliver).
        val newTime = incoming.optLong("time")
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).optLong("time") == newTime) return
        }
        arr.put(incoming)
        historyFile(context).writeText(arr.toString())
        changes.value = System.currentTimeMillis()
    }

    @Synchronized
    fun load(context: Context): List<Entry> {
        val arr = readArray(context)
        val list = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val work = mutableListOf<Int>()
            o.optJSONArray("workAvgHr")?.let { wa ->
                for (j in 0 until wa.length()) work.add(wa.optInt(j))
            }
            list.add(
                Entry(
                    time = o.optLong("time"),
                    durationSec = o.optInt("durationSec"),
                    distanceM = o.optDouble("distanceM", 0.0),
                    avgHr = o.optInt("avgHr"),
                    maxHrSetting = o.optInt("maxHrSetting", 190),
                    workAvgHr = work,
                    filename = o.optString("filename"),
                )
            )
        }
        return list.sortedByDescending { it.time }
    }

    private fun readArray(context: Context): JSONArray {
        val f = historyFile(context)
        if (!f.exists()) return JSONArray()
        return runCatching { JSONArray(f.readText()) }.getOrElse { JSONArray() }
    }
}
