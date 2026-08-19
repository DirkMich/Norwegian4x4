package com.example.norwegian4x4

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Sends a finished workout to the phone companion app over the Wearable Data
 * Layer (Bluetooth). Delivery is handled by Play services: if the phone isn't
 * reachable right now, the data is queued and synced as soon as it reconnects.
 */
object PhoneSync {

    const val PATH_WORKOUT = "/workout"
    const val PATH_SETTINGS = "/settings"

    fun sendWorkout(
        context: Context,
        file: File,
        startTimeMs: Long,
        elapsedSec: Int,
        distanceM: Double,
        maxHrSetting: Int,
        samples: List<TrackSample>,
        phases: List<Phase>,
    ) {
        runCatching {
            val allHr = samples.filter { it.hr > 0 }.map { it.hr }
            val workAvgHr = JSONArray()
            phases.forEachIndexed { index, phase ->
                if (phase.type == PhaseType.WORK) {
                    val hrs = samples.filter { it.phaseIndex == index && it.hr > 0 }.map { it.hr }
                    workAvgHr.put(if (hrs.isEmpty()) 0 else hrs.average().toInt())
                }
            }
            val summary = JSONObject().apply {
                put("time", startTimeMs)
                put("durationSec", elapsedSec)
                put("distanceM", distanceM)
                put("avgHr", if (allHr.isEmpty()) 0 else allHr.average().toInt())
                put("maxHrSetting", maxHrSetting)
                put("workAvgHr", workAvgHr)
                put("filename", file.name)
            }

            val request = PutDataMapRequest.create("$PATH_WORKOUT/$startTimeMs").apply {
                dataMap.putAsset("tcx", Asset.createFromBytes(file.readBytes()))
                dataMap.putString("filename", file.name)
                dataMap.putString("summary", summary.toString())
                dataMap.putLong("sentAt", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request)
        }.onFailure {
            Log.w("PhoneSync", "Could not queue workout for phone", it)
        }
    }
}
