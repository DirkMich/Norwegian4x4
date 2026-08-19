package com.example.norwegian4x4

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExerciseService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.example.norwegian4x4.START"
        const val ACTION_PAUSE = "com.example.norwegian4x4.PAUSE"
        const val ACTION_RESUME = "com.example.norwegian4x4.RESUME"
        const val ACTION_END = "com.example.norwegian4x4.END"

        private const val CHANNEL_ID = "workout"
        private const val NOTIF_ID = 1

        /** Single source of truth the UI observes. */
        val state = MutableStateFlow(WorkoutState())

        /** Called from the summary screen to go back to the start screen. */
        fun reset() {
            state.value = WorkoutState()
        }
    }

    private var maxHr = 190
    private var phases: List<Phase> = emptyList()
    private var phaseIndex = 0
    private var phaseElapsed = 0
    private var elapsed = 0
    private var startTimeMs = 0L
    private var tickerJob: Job? = null
    private var startedWorkout = false
    private var paused = false
    private var ending = false

    // Latest sensor values, merged from Health Services updates.
    @Volatile private var lastHr = 0
    @Volatile private var lastSpeed = 0.0
    @Volatile private var lastLat: Double? = null
    @Volatile private var lastLon: Double? = null
    @Volatile private var lastAlt: Double? = null
    @Volatile private var totalDistance = 0.0

    private val samples = mutableListOf<TrackSample>()

    private val exerciseClient by lazy { HealthServices.getClient(this).exerciseClient }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> if (!startedWorkout) {
                startedWorkout = true
                maxHr = Prefs.getMaxHr(this)
                goForeground()
                beginWorkout()
            }
            ACTION_PAUSE -> pauseWorkout()
            ACTION_RESUME -> resumeWorkout()
            ACTION_END -> lifecycleScope.launch { finishWorkout() }
        }
        return START_NOT_STICKY
    }

    // ------------------------------------------------------------------ setup

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Workout", NotificationManager.IMPORTANCE_LOW)
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Norwegian 4x4")
            .setContentText("Workout in progress")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun beginWorkout() {
        phases = buildNorwegian4x4(Prefs.getIntervals(this))
        phaseIndex = 0
        phaseElapsed = 0
        elapsed = 0
        paused = false
        samples.clear()
        startTimeMs = System.currentTimeMillis()

        exerciseClient.setUpdateCallback(updateCallback)

        lifecycleScope.launch {
            try {
                val config = ExerciseConfig.builder(ExerciseType.RUNNING)
                    .setDataTypes(
                        setOf(
                            DataType.HEART_RATE_BPM,
                            DataType.SPEED,
                            DataType.DISTANCE,
                            DataType.LOCATION,
                        )
                    )
                    .setIsGpsEnabled(true)
                    .setIsAutoPauseAndResumeEnabled(false)
                    .build()
                exerciseClient.startExerciseAsync(config).await()
                vibrate(longArrayOf(0, 200))
                startTicker()
                publish()
            } catch (t: Throwable) {
                state.value = WorkoutState(error = t.message ?: "Could not start exercise")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    // ----------------------------------------------------- health services in

    private val updateCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val metrics = update.latestMetrics
            metrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let {
                lastHr = it.value.toInt()
            }
            metrics.getData(DataType.SPEED).lastOrNull()?.let {
                lastSpeed = it.value
            }
            metrics.getData(DataType.DISTANCE).forEach {
                totalDistance += it.value
            }
            metrics.getData(DataType.LOCATION).lastOrNull()?.let {
                lastLat = it.value.latitude
                lastLon = it.value.longitude
                lastAlt = it.value.altitude
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
        override fun onRegistered() {}
        override fun onRegistrationFailed(throwable: Throwable) {
            state.value = state.value.copy(error = "Sensor registration failed")
        }
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
    }

    // ------------------------------------------------------------ timer logic

    private fun startTicker() {
        tickerJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                elapsed++
                phaseElapsed++

                // Record one sample per second for the TCX file.
                samples.add(
                    TrackSample(
                        timeMs = System.currentTimeMillis(),
                        lat = lastLat,
                        lon = lastLon,
                        alt = lastAlt,
                        hr = lastHr,
                        distM = totalDistance,
                        phaseIndex = phaseIndex,
                    )
                )

                val phase = phases[phaseIndex]
                if (phaseElapsed >= phase.durationSec) {
                    phaseIndex++
                    phaseElapsed = 0
                    if (phaseIndex >= phases.size) {
                        finishWorkout()
                        return@launch
                    }
                    // Buzz pattern depends on what's coming next.
                    when (phases[phaseIndex].type) {
                        PhaseType.WORK -> vibrate(longArrayOf(0, 500, 150, 500, 150, 500))
                        PhaseType.RECOVERY -> vibrate(longArrayOf(0, 300, 150, 300))
                        else -> vibrate(longArrayOf(0, 400))
                    }
                }
                publish()
            }
        }
    }

    // ---------------------------------------------------------- pause/resume

    private fun pauseWorkout() {
        if (!startedWorkout || paused || ending) return
        paused = true
        tickerJob?.cancel()
        lifecycleScope.launch {
            runCatching { exerciseClient.pauseExerciseAsync().await() }
            vibrate(longArrayOf(0, 150, 100, 150))
            publish()
        }
    }

    private fun resumeWorkout() {
        if (!startedWorkout || !paused || ending) return
        paused = false
        lifecycleScope.launch {
            runCatching { exerciseClient.resumeExerciseAsync().await() }
            vibrate(longArrayOf(0, 200))
            startTicker()
            publish()
        }
    }

    private fun publish() {
        val phase = phases[phaseIndex]
        val low = (maxHr * phase.lowFrac).toInt()
        val high = (maxHr * phase.highFrac).toInt()
        val easyPhase = phase.type == PhaseType.WARMUP || phase.type == PhaseType.COOLDOWN
        val guidance = when {
            lastHr <= 0 -> Guidance.NONE
            lastHr > high -> Guidance.SLOW_DOWN
            // Don't nag "speed up" while warming up or cooling down.
            easyPhase -> Guidance.IN_ZONE
            lastHr < low -> Guidance.SPEED_UP
            else -> Guidance.IN_ZONE
        }
        state.value = WorkoutState(
            running = true,
            paused = paused,
            phaseLabel = phase.label,
            phaseType = phase.type,
            secondsLeft = phase.durationSec - phaseElapsed,
            hr = lastHr,
            targetLow = low,
            targetHigh = high,
            guidance = guidance,
            speedMps = lastSpeed,
            distanceM = totalDistance,
            elapsedSec = elapsed,
        )
    }

    // --------------------------------------------------------------- shutdown

    private suspend fun finishWorkout() {
        if (ending) return
        ending = true
        tickerJob?.cancel()
        runCatching { exerciseClient.endExerciseAsync().await() }
        val file = runCatching { TcxWriter.write(this, samples, startTimeMs) }.getOrNull()
        if (file != null) {
            PhoneSync.sendWorkout(
                context = this,
                file = file,
                startTimeMs = startTimeMs,
                elapsedSec = elapsed,
                distanceM = totalDistance,
                maxHrSetting = maxHr,
                samples = samples,
                phases = phases,
            )
        }
        vibrate(longArrayOf(0, 700))
        state.value = WorkoutState(
            running = false,
            ended = true,
            hr = lastHr,
            distanceM = totalDistance,
            elapsedSec = elapsed,
            savedFile = file?.name,
            error = if (file == null && samples.isNotEmpty()) "Could not save TCX file" else null,
            summary = if (samples.isNotEmpty()) buildWorkoutSummary(samples, phases, maxHr) else null,
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
