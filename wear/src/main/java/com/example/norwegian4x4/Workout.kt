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
)

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
    return "%d:%02d".format(secPerKm / 60, secPerKm % 60)
}
