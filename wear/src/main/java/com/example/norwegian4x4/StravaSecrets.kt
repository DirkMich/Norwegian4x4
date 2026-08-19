package com.example.norwegian4x4

/**
 * ONE-TIME STRAVA SETUP — fill in these three values, then reinstall the app.
 * Full walkthrough in SETUP_GUIDE.md, section "Direct Strava upload".
 *
 * Short version:
 *  1. Create a free API app at https://www.strava.com/settings/api
 *     (Authorization Callback Domain: localhost). Copy Client ID + Client Secret.
 *  2. Authorize it once in your browser and exchange the code for a refresh
 *     token with one curl command (exact commands in the guide).
 *  3. Paste all three values below.
 *
 * Keep this file private — the secret gives write access to YOUR Strava account.
 * The refresh token below is only used the first time; after that the app
 * stores and rotates tokens by itself.
 */
object StravaSecrets {
    const val CLIENT_ID = ""
    const val CLIENT_SECRET = ""
    const val INITIAL_REFRESH_TOKEN = ""

    val isConfigured: Boolean
        get() = CLIENT_ID.isNotBlank() && CLIENT_SECRET.isNotBlank() && INITIAL_REFRESH_TOKEN.isNotBlank()
}
