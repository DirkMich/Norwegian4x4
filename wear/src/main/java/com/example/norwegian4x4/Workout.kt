package com.example.norwegian4x4

/**
 * The workout model for the Norwegian 4x4 protocol.
 *
 * Structure (with N = number of hard intervals, set in Settings):
 *   10 min warm-up (easy)
 *   N x [ 4 min hard @ 85-95% of max HR + 3 min active recovery @ 60-75% ]
 *   5 min cool-down
 */

enum class PhaseType { WARMUP, WORK, RECOVERY, COOLDOWN }

data class Phase(
    val label: String,
    val type: PhaseType,
    val durationSec: Int,
    /** Lower bound of the target zone, as a fraction of max HR. */
    val lowFrac: Double,
    /** Upper bound of the target zone, as a fraction of max HR. */
    val highFrac: Double,
)

fun buildNorwegian4x4(intervals: Int = 4): List<Phase> {
    val n = intervals.coerceIn(1, 8)
    val phases = mutableListOf<Phase>()
    phases += Phase("Warm-up", PhaseType.WARMUP, 10 * 60, 0.60, 0.75)
    for (i in 1..n) {
        phases += Phase("Interval $i/$n", PhaseType.WORK, 4 * 60, 0.85, 0.95)
        if (i < n) {
            phases += Phase("Recovery $i/${n - 1}", PhaseType.RECOVERY, 3 * 60, 0.60, 0.75)
        }
    }
    phases += Phase("Cool-down", PhaseType.COOLDOWN, 5 * 60, 0.50, 0.70)
    return phases
}

/** Total planned workout length in minutes for N hard intervals. */
fun workoutMinutes(intervals: Int): Int {
    val n = intervals.coerceIn(1, 8)
    return 10 + n * 4 + (n - 1) * 3 + 5
}

enum class Guidance { NONE, SPEED_UP, IN_ZONE, SLOW_DOWN }

/** Everything the UI needs to draw one frame of the workout. */
data class WorkoutState(
    val running: Boolean = false,
    val paused: Boolean = false,
    val ended: Boolean = false,
    val phaseLabel: String = "",
    val phaseType: PhaseType = PhaseType.WARMUP,
    val secondsLeft: Int = 0,
    val hr: Int = 0,
    val targetLow: Int = 0,
    val targetHigh: Int = 0,
    val guidance: Guidance = Guidance.NONE,
    val speedMps: Double = 0.0,
    val distanceM: Double = 0.0,
    val elapsedSec: Int = 0,
    val savedFile: String? = null,
    val error: String? = null,
    val summary: WorkoutSummary? = null,
    /** Most recent HR samples (oldest first), for the live waveform readout. */
    val hrHistory: List<Int> = emptyList(),
)

/** Per-hard-interval breakdown shown on the end-of-workout summary. */
data class IntervalSummary(
    /** 1-based, matches the "Interval N/total" phase label. */
    val index: Int,
    val avgHr: Int,
    val maxHr: Int,
    /** % of this interval spent inside its 85-95% target zone. */
    val timeInZonePct: Int,
    /** 0 if not enough distance was covered to compute a pace. */
    val paceSecPerKm: Int,
    val distanceM: Double,
    /** Avg/min HR during the recovery that follows; 0 for the last interval (no recovery after it). */
    val recoveryAvgHr: Int,
    val recoveryMinHr: Int,
)

/** Aggregate + per-interval stats computed from the recorded samples once a workout ends. */
data class WorkoutSummary(
    val avgHr: Int,
    val maxHr: Int,
    /** % of all hard-interval time spent inside the target zone. */
    val workTimeInZonePct: Int,
    val intervals: List<IntervalSummary>,
)

/** Builds the detailed summary from the per-second samples recorded during the workout. */
fun buildWorkoutSummary(
    samples: List<TrackSample>,
    phases: List<Phase>,
    maxHrSetting: Int,
): WorkoutSummary {
    val allHr = samples.mapNotNull { it.hr.takeIf { hr -> hr > 0 } }

    var workInZone = 0
    var workTotal = 0
    val intervals = mutableListOf<IntervalSummary>()
    var intervalNumber = 0

    phases.forEachIndexed { phaseIndex, phase ->
        if (phase.type != PhaseType.WORK) return@forEachIndexed
        intervalNumber++

        val phaseSamples = samples.filter { it.phaseIndex == phaseIndex }
        val hrs = phaseSamples.mapNotNull { it.hr.takeIf { hr -> hr > 0 } }
        val low = (maxHrSetting * phase.lowFrac).toInt()
        val high = (maxHrSetting * phase.highFrac).toInt()
        val inZone = hrs.count { it in low..high }
        workInZone += inZone
        workTotal += hrs.size

        val distanceM = if (phaseSamples.isEmpty()) 0.0
        else phaseSamples.last().distM - phaseSamples.first().distM
        val paceSecPerKm = if (distanceM > 20 && phaseSamples.isNotEmpty())
            (phaseSamples.size / (distanceM / 1000.0)).toInt() else 0

        val recoveryIndex = phaseIndex + 1
        val recoveryHrs = if (recoveryIndex < phases.size && phases[recoveryIndex].type == PhaseType.RECOVERY) {
            samples.filter { it.phaseIndex == recoveryIndex }.mapNotNull { it.hr.takeIf { hr -> hr > 0 } }
        } else emptyList()

        intervals += IntervalSummary(
            index = intervalNumber,
            avgHr = if (hrs.isEmpty()) 0 else hrs.average().toInt(),
            maxHr = hrs.maxOrNull() ?: 0,
            timeInZonePct = if (hrs.isEmpty()) 0 else inZone * 100 / hrs.size,
            paceSecPerKm = paceSecPerKm,
            distanceM = distanceM,
            recoveryAvgHr = if (recoveryHrs.isEmpty()) 0 else recoveryHrs.average().toInt(),
            recoveryMinHr = recoveryHrs.minOrNull() ?: 0,
        )
    }

    return WorkoutSummary(
        avgHr = if (allHr.isEmpty()) 0 else allHr.average().toInt(),
        maxHr = allHr.maxOrNull() ?: 0,
        workTimeInZonePct = if (workTotal == 0) 0 else workInZone * 100 / workTotal,
        intervals = intervals,
    )
}

/** One recorded data point (1 per second) used to build the TCX file. */
data class TrackSample(
    val timeMs: Long,
    val lat: Double?,
    val lon: Double?,
    val alt: Double?,
    val hr: Int,
    val distM: Double,
    val phaseIndex: Int,
)

fun formatClock(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

/** Converts speed in m/s to a "min:sec /km" pace string. */
fun formatPace(speedMps: Double): String {
    if (speedMps < 0.4) return "--:--"
    val secPerKm = (1000.0 / speedMps).toInt()
    return formatPaceSec(secPerKm)
}

/** Formats a "seconds per km" value (0 = unknown) as "min:sec". */
fun formatPaceSec(secPerKm: Int): String {
    if (secPerKm <= 0) return "--:--"
    return "%d:%02d".format(secPerKm / 60, secPerKm % 60)
}
