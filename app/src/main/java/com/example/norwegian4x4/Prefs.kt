package com.example.norwegian4x4

import android.content.Context

object Prefs {
    private const val FILE = "n4x4_prefs"
    private const val KEY_MAX_HR = "maxHr"
    private const val KEY_INTERVALS = "intervals"
    private const val KEY_SCREEN_ON = "screenOn"
    private const val KEY_STRAVA_REFRESH = "stravaRefreshToken"

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getMaxHr(c: Context): Int = prefs(c).getInt(KEY_MAX_HR, 190)
    fun setMaxHr(c: Context, v: Int) = prefs(c).edit().putInt(KEY_MAX_HR, v.coerceIn(120, 220)).apply()

    fun getIntervals(c: Context): Int = prefs(c).getInt(KEY_INTERVALS, 4)
    fun setIntervals(c: Context, v: Int) = prefs(c).edit().putInt(KEY_INTERVALS, v.coerceIn(1, 8)).apply()

    /** Keep the screen fully on during a workout (uses more battery). */
    fun getScreenOn(c: Context): Boolean = prefs(c).getBoolean(KEY_SCREEN_ON, true)
    fun setScreenOn(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_SCREEN_ON, v).apply()

    /** Latest Strava refresh token; falls back to the compiled-in one from StravaSecrets. */
    fun getStravaRefreshToken(c: Context): String =
        prefs(c).getString(KEY_STRAVA_REFRESH, null) ?: StravaSecrets.INITIAL_REFRESH_TOKEN
    fun setStravaRefreshToken(c: Context, v: String) =
        prefs(c).edit().putString(KEY_STRAVA_REFRESH, v).apply()
}
