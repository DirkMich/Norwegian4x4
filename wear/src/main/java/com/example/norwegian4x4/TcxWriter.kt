package com.example.norwegian4x4

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Writes a Strava-compatible TCX file.
 * Each workout phase (warm-up, interval, recovery, cool-down) becomes its own <Lap>,
 * so on Strava you can see the split for every 4-minute interval.
 *
 * Files are written to the app's external files dir:
 *   /sdcard/Android/data/com.example.norwegian4x4/files/
 */
object TcxWriter {

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val nameFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    private fun num(v: Double): String = String.format(Locale.US, "%.1f", v)
    private fun coord(v: Double): String = String.format(Locale.US, "%.7f", v)

    fun write(context: Context, samples: List<TrackSample>, startMs: Long): File {
        val sb = StringBuilder(samples.size * 160 + 1024)
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("""<TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2">""")
        sb.append("<Activities><Activity Sport=\"Running\">")
        sb.append("<Id>").append(isoFmt.format(Date(startMs))).append("</Id>")

        val byPhase = samples.groupBy { it.phaseIndex }.toSortedMap()
        var prevDist = 0.0
        for ((_, pts) in byPhase) {
            if (pts.isEmpty()) continue
            val lapStart = pts.first().timeMs
            val lapEnd = pts.last().timeMs
            val lapDist = (pts.last().distM - prevDist).coerceAtLeast(0.0)
            prevDist = pts.last().distM

            sb.append("<Lap StartTime=\"").append(isoFmt.format(Date(lapStart))).append("\">")
            sb.append("<TotalTimeSeconds>").append(num((lapEnd - lapStart) / 1000.0)).append("</TotalTimeSeconds>")
            sb.append("<DistanceMeters>").append(num(lapDist)).append("</DistanceMeters>")
            sb.append("<Calories>0</Calories>")
            sb.append("<Intensity>Active</Intensity>")
            sb.append("<TriggerMethod>Manual</TriggerMethod>")
            sb.append("<Track>")
            for (p in pts) {
                sb.append("<Trackpoint>")
                sb.append("<Time>").append(isoFmt.format(Date(p.timeMs))).append("</Time>")
                if (p.lat != null && p.lon != null) {
                    sb.append("<Position>")
                    sb.append("<LatitudeDegrees>").append(coord(p.lat)).append("</LatitudeDegrees>")
                    sb.append("<LongitudeDegrees>").append(coord(p.lon)).append("</LongitudeDegrees>")
                    sb.append("</Position>")
                }
                if (p.alt != null) {
                    sb.append("<AltitudeMeters>").append(num(p.alt)).append("</AltitudeMeters>")
                }
                sb.append("<DistanceMeters>").append(num(p.distM)).append("</DistanceMeters>")
                if (p.hr > 0) {
                    sb.append("<HeartRateBpm><Value>").append(p.hr).append("</Value></HeartRateBpm>")
                }
                sb.append("</Trackpoint>")
            }
            sb.append("</Track></Lap>")
        }

        sb.append("</Activity></Activities></TrainingCenterDatabase>")

        val file = File(context.getExternalFilesDir(null), "n4x4_${nameFmt.format(Date(startMs))}.tcx")
        file.writeText(sb.toString())
        return file
    }
}
