package com.example.norwegian4x4.phone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.io.File

/**
 * Runs automatically (even if the app is closed) whenever the watch syncs a
 * finished workout. Saves the TCX to app storage AND to the phone's Downloads
 * folder (Download/Norwegian4x4/), records the summary in History, and posts a
 * notification that opens the app for one-tap sharing.
 */
class WorkoutListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith("/workout")) continue

            runCatching {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val filename = dataMap.getString("filename") ?: "workout.tcx"
                val summary = dataMap.getString("summary") ?: "{}"
                val asset = dataMap.getAsset("tcx") ?: return@runCatching

                // Read the TCX bytes from the asset.
                val fdResult = Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
                val bytes = fdResult.inputStream.use { it.readBytes() }

                // 1. Save into app storage (used by the Share button).
                File(History.workoutsDir(this), filename).writeBytes(bytes)

                // 2. Also save into Downloads so it's easy to find in any file picker.
                saveToDownloads(filename, bytes)

                // 3. Record the summary for the history list and charts.
                History.add(this, summary)

                // 4. Tell the user.
                postNotification(filename)
            }.onFailure {
                Log.w("WorkoutListener", "Failed to receive workout", it)
            }
        }
    }

    private fun saveToDownloads(filename: String, bytes: ByteArray) {
        if (Build.VERSION.SDK_INT < 29) return
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.garmin.tcx+xml")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/Norwegian4x4")
            }
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
        }
    }

    private fun postNotification(filename: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                "workouts", "New workouts", NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, PhoneActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = Notification.Builder(this, "workouts")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Workout received")
            .setContentText("Tap to view and share $filename to Strava")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        nm.notify(filename.hashCode(), notif)
    }
}
