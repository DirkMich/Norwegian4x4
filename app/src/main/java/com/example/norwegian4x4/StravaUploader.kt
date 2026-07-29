package com.example.norwegian4x4

import android.content.Context
import kotlinx.coroutines.Dispatchers
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
 * Uploads a TCX file straight to Strava from the watch.
 *
 * Auth model: the compiled-in refresh token (StravaSecrets) is used once to get
 * an access token; Strava may rotate the refresh token, and the newest one is
 * always stored in Prefs, so authorization keeps working indefinitely.
 */
object StravaUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun upload(
        context: Context,
        file: File,
        name: String,
        description: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!StravaSecrets.isConfigured) {
                throw IOException("Strava is not set up (see SETUP_GUIDE.md)")
            }
            if (!file.exists()) throw IOException("Workout file not found")

            val accessToken = refreshAccessToken(context)

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", file.name,
                    file.asRequestBody("application/xml".toMediaType())
                )
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
                if (!resp.isSuccessful) {
                    throw IOException("Upload failed (HTTP ${resp.code})")
                }
                val json = JSONObject(text)
                val error = json.optString("error")
                if (error.isNotBlank() && error != "null") {
                    throw IOException("Strava: $error")
                }
            }
        }
    }

    private fun refreshAccessToken(context: Context): String {
        val form = FormBody.Builder()
            .add("client_id", StravaSecrets.CLIENT_ID)
            .add("client_secret", StravaSecrets.CLIENT_SECRET)
            .add("grant_type", "refresh_token")
            .add("refresh_token", Prefs.getStravaRefreshToken(context))
            .build()

        val request = Request.Builder()
            .url("https://www.strava.com/api/v3/oauth/token")
            .post(form)
            .build()

        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("Strava auth failed (HTTP ${resp.code}) — re-check StravaSecrets.kt")
            }
            val json = JSONObject(text)
            // Strava can rotate the refresh token; always keep the newest one.
            json.optString("refresh_token").takeIf { it.isNotBlank() }?.let {
                Prefs.setStravaRefreshToken(context, it)
            }
            return json.getString("access_token")
        }
    }
}
