# Norwegian 4x4 — Setup Guide (Galaxy Watch 7)

This guide takes you from zero to running the app on your watch and uploading workouts to Strava. No prior Android experience needed. Expect about 45–60 minutes the first time (mostly downloads); after that, installing updates takes seconds.

## What the app does

- **Workout structure:** 10 min warm-up → 4 × (4 min hard + 3 min recovery) → 5 min cool-down. Total ~44 minutes.
- **Live display:** interval countdown, current heart rate in color, SPEED UP / IN ZONE / SLOW DOWN guidance, current pace (min/km) and distance.
- **Zones:** the hard intervals target 85–95% of your max HR (which you enter on the start screen); recoveries target 60–75%.
- **Haptics:** triple buzz = hard interval starting, double buzz = recovery starting, long buzz = workout done.
- **Strava:** every workout is saved as a `.tcx` file with GPS track, heart rate, distance, and one lap per phase. After a one-time setup (step 6), an **Upload to Strava** button on the watch sends it straight to Strava.

---

## Step 1 — Install Android Studio

1. Download Android Studio from https://developer.android.com/studio and install it (default options are fine).
2. On first launch, the setup wizard downloads the Android SDK. Accept the licenses and let it finish.

## Step 2 — Open the project

1. Unzip `Norwegian4x4.zip` somewhere permanent (e.g. Documents).
2. In Android Studio: **File → Open** and select the `Norwegian4x4` folder (the one containing `settings.gradle.kts`).
3. If asked to "Trust the project", say yes.
4. Wait for **Gradle sync** to finish (progress bar at the bottom). The first sync downloads Gradle and all libraries — 5–15 minutes depending on your connection. Later syncs are fast.

## Step 3 — Enable developer mode on the watch

On your Galaxy Watch 7:

1. **Settings → About watch → Software information**, then tap **Software version** 5 times quickly. You'll see "Developer mode turned on".
2. Go back to **Settings → Developer options** (now visible at the bottom).
3. Turn on **ADB debugging** and **Wireless debugging** (agree to the prompts). The watch and your computer must be on the **same Wi-Fi network**.

## Step 4 — Connect the watch to Android Studio

1. On the watch: **Developer options → Wireless debugging → Pair new device**. It shows a pairing code plus an IP address and port (e.g. `192.168.1.42:40001`).
2. In Android Studio, open the **Terminal** tab (bottom of the window) and run, using the values from your watch:

   ```
   adb pair 192.168.1.42:40001
   ```

   Enter the 6-digit pairing code when asked.
3. Back on the **Wireless debugging** main screen the watch shows a second IP:port (a *different* port than the pairing one). Connect to that one:

   ```
   adb connect 192.168.1.42:37000
   ```

   You should see `connected to ...`. The watch now appears in the device dropdown at the top of Android Studio.

   *Tip: if `adb` isn't found, in Android Studio go to Tools → SDK Manager, note the SDK path, and use `<sdk-path>/platform-tools/adb` — or just restart the Terminal tab.*

## Step 5 — Install and run

1. Select your watch in the device dropdown at the top of Android Studio.
2. Press the green **Run ▶** button. The app builds and launches on the watch (first build takes a few minutes).
3. On the watch, when you press **Start workout** the first time, grant the permission prompts (body sensors, location, notifications). All are required — HR guidance needs the sensor, pace/distance need GPS.

## Step 6 — Direct Strava upload (one-time setup, ~10 minutes)

After this setup, an **Upload to Strava** button appears on the watch after every workout — no computer needed anymore. (The TCX file is still saved on the watch as a backup either way.)

**A. Create your Strava API app**

1. Log in at https://www.strava.com/settings/api and create an app:
   - Application name: anything (e.g. `Watch4x4`) — note it may not contain the word "Strava"
   - Category: anything, Website: `http://localhost`
   - **Authorization Callback Domain: `localhost`** (this one matters)
2. After saving you'll see your **Client ID** (a number) and **Client Secret** (long string). Keep this page open.

**B. Authorize it once**

1. Paste this into your browser, replacing `YOUR_ID` with your Client ID:

   ```
   https://www.strava.com/oauth/authorize?client_id=YOUR_ID&response_type=code&redirect_uri=http://localhost&approval_prompt=force&scope=activity:write,read
   ```

2. Click **Authorize**. Your browser will land on a "can't reach localhost" error page — that's expected. Look at the **address bar**: it contains `code=xxxxxxxx`. Copy that code (everything between `code=` and `&`).
3. In the Android Studio Terminal, run (all one line, fill in your three values):

   ```
   curl.exe -X POST https://www.strava.com/api/v3/oauth/token -d client_id=YOUR_ID -d client_secret=YOUR_SECRET -d code=THE_CODE -d grant_type=authorization_code
   ```

   (On Windows use `curl.exe`, not `curl`.) The JSON response contains `"refresh_token": "..."` — copy that value. The code from step 2 is single-use; if you get an error, redo step 1–2 for a fresh code.

**C. Put the values in the app**

1. Open `app/src/main/java/com/example/norwegian4x4/StravaSecrets.kt` and paste your Client ID, Client Secret, and refresh token into the three constants.
2. Press **Run ▶** to reinstall on the watch. Done — after each workout, tap **Upload to Strava** on the summary screen.

Tokens rotate automatically from here on; you never repeat this setup. Keep `StravaSecrets.kt` private (don't post it anywhere) — it grants write access to your Strava account.

## Step 6b — Manual fallback: TCX via adb

If an upload fails (no network) or you skipped the Strava setup, every workout is still on the watch:

1. `adb connect <ip:port>` if not connected (the port can change after a reboot).
2. In the Android Studio Terminal:

   ```
   adb pull /sdcard/Android/data/com.example.norwegian4x4/files/ ./workouts
   ```

3. Upload the `.tcx` file at **https://www.strava.com/upload/select**. Strava will show the run with your route, pace, heart rate, and each interval as its own lap.

## Using the app

1. Open **Settings** on the start screen to set **your max HR** (a rough estimate is 220 minus your age, but a measured value gives much better zone targets), the **number of hard intervals** (1–8; the start screen shows the resulting total time), and whether the **screen stays always on** during workouts (ON is easier to glance at; OFF saves battery — raise your wrist to check).
2. Tap **Start workout**, wait outside for a few seconds so GPS can lock, and go.
3. During hard intervals the HR number and guidance text turn **blue (speed up)**, **green (in zone)**, or **red (slow down)**. During warm-up/cool-down it only warns you if you're going too hard.
4. **Pause** freezes the interval timer and the recording; **Resume** continues where you left off. Tap **End** to stop early — the file is still saved.

## Troubleshooting

- **Gradle sync fails:** check your internet connection, then File → Sync Project with Gradle Files. If it mentions the JDK, go to Settings → Build Tools → Gradle and set the Gradle JDK to the embedded JDK (17).
- **HR shows "--" for a long time:** tighten the strap one notch and give it 15–30 seconds; the optical sensor needs skin contact.
- **Pace/distance stay at zero:** GPS has no fix yet. Start outdoors with a clear sky view; the first fix after install can take a minute or two.
- **Watch disappeared from the device list:** run `adb connect <ip:port>` again (check the current port under Wireless debugging).
- **"Upload to Strava" fails:** the watch needs internet. Connected to your phone via Bluetooth it normally routes through the phone; if that fails, connect the watch to Wi-Fi (Settings → Connections → Wi-Fi) and retry. An auth error (HTTP 400/401) means a value in `StravaSecrets.kt` is wrong — redo the authorization steps for a fresh refresh token.
- **Build errors in the code:** copy the exact error message back into our chat and I'll fix it.

## Customizing

All workout structure lives in one place: `app/src/main/java/com/example/norwegian4x4/Workout.kt` → `buildNorwegian4x4()`. Change durations or zone percentages there, then press Run again to reinstall. Max HR, interval count, and always-on screen are in the app's Settings menu.

## Later upgrades (just ask)

- Audio cues through Bluetooth earbuds, ambient (always-on-display) rendering, workout history on the watch, auto-upload without pressing the button.
