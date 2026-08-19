package com.example.norwegian4x4.phone

/**
 * ONE-TIME STRAVA SETUP — fill in these two values, then reinstall the phone app.
 * Full walkthrough in SETUP_GUIDE.md, section "Strava".
 *
 * Short version:
 *  1. Create a free API app at https://www.strava.com/settings/api
 *     (Authorization Callback Domain: leave blank / any value — this app uses
 *     a custom-scheme redirect, not a web domain).
 *  2. Copy the Client ID and Client Secret below.
 *
 * Keep this file private — the secret gives write access to whoever's Strava
 * account gets connected. There's no token to paste: the phone app handles
 * the full connect flow itself the first time you tap "Connect Strava".
 */
object StravaSecrets {
    const val CLIENT_ID = ""
    const val CLIENT_SECRET = ""

    /** Must exactly match the intent-filter data in AndroidManifest.xml. */
    const val REDIRECT_URI = "norwegian4x4://strava-callback"

    val isConfigured: Boolean
        get() = CLIENT_ID.isNotBlank() && CLIENT_SECRET.isNotBlank()
}
