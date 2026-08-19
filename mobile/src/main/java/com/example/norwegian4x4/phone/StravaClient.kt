package com.example.norwegian4x4.phone

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Strava connect + upload, run entirely from the phone. The watch's only job
 * is to hand a finished workout's TCX file to the phone over Bluetooth; this
 * is the one place in the app that talks to Strava's API.
 */
object StravaClient {

    private const val PREFS = "strava_prefs"
    private const val KEY_ACCESS_TOKEN = "accessToken"
    private const val KEY_REFRESH_TOKEN = "refreshToken"
    private const val KEY_EXPIRES_AT = "expiresAt"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Bumped whenever connect/disconnect happens, so UI can react without polling prefs. */
    val connectionChanges = MutableStateFlow(0L)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isConnected(context: Context): Boolean =
        prefs(context).getString(KEY_REFRESH_TOKEN, null) != null

    fun disconnect(context: Context) {
        prefs(context).edit().clear().apply()
        connectionChanges.value = System.currentTimeMillis()
    }

    /** Opens Strava's consent screen in the browser; the result arrives via the redirect URI. */
    fun authorizeIntent(): Intent {
        val url = "https://www.strava.com/oauth/mobile/authorize" +
            "?client_id=${StravaSecrets.CLIENT_ID}" +
            "&redirect_uri=${Uri.encode(StravaSecrets.REDIRECT_URI)}" +
            "&response_type=code" +
            "&approval_prompt=auto" +
            "&scope=activity:write,read"
        return Intent(Intent.ACTION_VIEW, url.toUri())
    }

    /** Feed this the launching intent's data (onCreate/onNewIntent); true if it was Strava's redirect. */
    suspend fun handleRedirect(context: Context, uri: Uri?): Boolean {
        if (uri == null || uri.scheme != "norwegian4x4" || uri.host != "strava-callback") return false
        val code = uri.getQueryParameter("code") ?: return false
        exchangeCodeForTokens(context, code)
        return true
    }

    private suspend fun exchangeCodeForTokens(context: Context, code: String) = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("client_id", StravaSecrets.CLIENT_ID)
            .add("client_secret", StravaSecrets.CLIENT_SECRET)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .build()
        val request = Request.Builder().url("https://www.strava.com/api/v3/oauth/token").post(form).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("Strava connect failed (HTTP ${resp.code})")
            storeTokens(context, JSONObject(text))
        }
    }

    private fun storeTokens(context: Context, json: JSONObject) {
        prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, json.getString("access_token"))
            .putString(KEY_REFRESH_TOKEN, json.getString("refresh_token"))
            .putLong(KEY_EXPIRES_AT, json.getLong("expires_at"))
            .apply()
        connectionChanges.value = System.currentTimeMillis()
    }

    /** Blocking; only ever called from an IO-dispatcher coroutine. */
    private fun freshAccessToken(context: Context): String {
        val p = prefs(context)
        val cached = p.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = p.getLong(KEY_EXPIRES_AT, 0)
        if (cached != null && System.currentTimeMillis() / 1000 < expiresAt - 60) return cached

        val refreshToken = p.getString(KEY_REFRESH_TOKEN, null)
            ?: throw IOException("Strava is not connected — connect it in Settings")
        val form = FormBody.Builder()
            .add("client_id", StravaSecrets.CLIENT_ID)
            .add("client_secret", StravaSecrets.CLIENT_SECRET)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()
        val request = Request.Builder().url("https://www.strava.com/api/v3/oauth/token").post(form).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("Strava session expired — reconnect in Settings")
            val json = JSONObject(text)
            storeTokens(context, json)
            return json.getString("access_token")
        }
    }

    suspend fun upload(context: Context, file: File, name: String, description: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!StravaSecrets.isConfigured) throw IOException("Strava is not set up (see SETUP_GUIDE.md)")
                if (!file.exists()) throw IOException("Workout file not found")

                val accessToken = freshAccessToken(context)
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, file.asRequestBody("application/xml".toMediaType()))
                    .addFormDataPart("data_type", "tcx")
                    .addFormDataPart("name", name)
                    .addFormDataPart("description", description)
                    .build()
                val request = Request.Builder()
                    .url("https://www.strava.com/api/v3/uploads")
                    .header("Authorization", "Bearer $accessToken")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) throw IOException("Upload failed (HTTP ${resp.code})")
                    val json = JSONObject(text)
                    val error = json.optString("error")
                    if (error.isNotBlank() && error != "null") throw IOException("Strava: $error")
                }
            }
        }
}
