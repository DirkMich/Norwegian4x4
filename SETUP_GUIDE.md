# Norwegian 4x4 — Setup Guide (Galaxy Watch 7 + phone companion)

This project contains **two apps**: the watch app (module `wear`) and a phone companion app (module `mobile`). The watch runs the workout; the phone receives every workout automatically over Bluetooth, keeps your history and progress charts, controls the settings, and makes sharing to Strava a 20-second job.

## What the apps do

**Watch (wear):**
- Workout structure: 10 min warm-up → N × (4 min hard + 3 min recovery) → 5 min cool-down. N is configurable (1–8).
- Live display: interval countdown, heart rate in color, SPEED UP / IN ZONE / SLOW DOWN guidance, pace (min/km), distance. Pause/Resume/End buttons.
- Hard intervals target 85–95% of your max HR; recoveries 60–75%. Haptics on every phase change.
- Saves each workout as a Strava-compatible `.tcx` (GPS route, HR, distance, one lap per phase) and sends it to the phone automatically.

**Phone (mobile):**
- Gets a notification when a workout arrives (works even with the app closed; the file is also copied to `Download/Norwegian4x4/`).
- **Workouts tab:** your history with per-interval average HR, a cloud-upload button that sends the workout straight to Strava (once connected), and a Share button for each TCX.
- **Progress tab:** charts of average interval heart rate (with your target zone drawn as a band) and distance per workout.
- **Settings tab:** max HR, interval count, always-on-screen (synced to the watch automatically), and the Strava connect/disconnect control.

The watch never talks to Strava directly — it only hands the finished workout to the phone over Bluetooth. All Strava auth and uploading happens on the phone.

---

## Step 1 — Install Android Studio

1. Download Android Studio from https://developer.android.com/studio and install it (default options are fine).
2. On first launch, the setup wizard downloads the Android SDK. Accept the licenses and let it finish.
3. If Gradle later complains about the JVM/JDK version: File → Settings → Build Tools → Gradle → set **Gradle JDK** to the embedded **jbr** (JetBrains Runtime).

## Step 2 — Open the project

1. Unzip `Norwegian4x4.zip` somewhere permanent and open the `Norwegian4x4` folder (the one containing `settings.gradle.kts`) via **File → Open**.
2. Let Gradle sync finish (first time: 5–15 min). Afterwards the run-configuration dropdown at the top shows **wear** and **mobile** — that's how you choose which app to build.

## Step 3 — Enable developer mode

**On the watch:** Settings → About watch → Software information → tap **Software version** 5 times → back → **Developer options** → enable **ADB debugging** and **Wireless debugging** (watch on the same Wi-Fi as your computer).

**On your phone:** Settings → About phone → Software information → tap **Build number** 7 times → back → **Developer options** → enable **USB debugging**.

## Step 4 — Install the watch app

1. Watch: Developer options → Wireless debugging → **Pair new device** (shows a code + IP:port).
2. Android Studio Terminal: `adb pair <ip:port>` and enter the code, then `adb connect <ip:port>` using the IP:port from the Wireless debugging main screen (a different port).
3. Select the **wear** configuration + your watch in the device dropdown, press **Run ▶**.
4. First start: grant the sensor/location/notification permissions on the watch.

## Step 5 — Install the phone app

1. Connect your phone with a **USB cable**. Accept the "Allow USB debugging?" prompt on the phone.
2. Select the **mobile** configuration + your phone in the device dropdown, press **Run ▶**.
3. Allow notifications when the app asks — that's how you're told a workout arrived.

Both apps are now permanently installed; you only reconnect to install updates. Important: install both from the same computer (they must carry the same debug signature to talk to each other).

## Step 6 — The workflow after a run

1. Finish the workout on the watch (or tap End). The watch shows "Sent to phone ✓".
2. Your phone gets a **"Workout received"** notification within moments (watch and phone connected via Bluetooth as usual). If the phone wasn't reachable, the workout is queued and delivered automatically when they reconnect.
3. Open the notification → **Workouts tab**. From here you have two ways to get it onto Strava:
   - Tap the **cloud icon** on the workout to upload it directly (see "Connecting Strava" below to set this up first).
   - Or tap **Share** and send it wherever is handy — or skip the share entirely, the file is already in **Download/Norwegian4x4/** on your phone; go to **strava.com/upload** in a browser and pick it.

### Connecting Strava

Direct upload uses Strava's API, which has required a **paid Strava API subscription since June 2026** — the same restriction as before, just now enforced on the phone side instead of the watch. If you have that subscription:

1. Create a free API app at https://www.strava.com/settings/api (the Authorization Callback Domain field can be left as anything — this app uses an in-app custom-scheme redirect, not a website).
2. Copy the **Client ID** and **Client Secret** into `mobile/src/main/java/com/example/norwegian4x4/phone/StravaSecrets.kt`, then reinstall the phone app.
3. In the phone app, **Settings → Connect Strava**. You'll be sent to Strava's consent screen in your browser and dropped straight back into the app once you approve.

If you'd rather not pay for API access, just use the Share button — that path is a normal Strava user feature and stays completely free.

## Using the watch app

1. Set max HR and interval count in the phone app's Settings (or on the watch — last change wins). If you don't know your max HR, 220 minus your age is a rough start.
2. Tap **Start workout**, wait a few seconds outdoors for GPS, and go.
3. Ice blue = speed up or in zone (the guidance text tells you which), red = slow down. During warm-up/cool-down it only warns when you're going too hard.
4. **Pause** freezes timer and recording; **End** stops early (the file is still saved and sent).

## Troubleshooting

- **Gradle sync fails:** check internet, then File → Sync Project with Gradle Files. JDK complaints → Step 1.3.
- **HR shows "--":** tighten the strap one notch and give it 15–30 seconds.
- **Pace/distance stay at zero:** GPS has no fix yet; start outdoors, first fix can take a minute.
- **Workout doesn't reach the phone:** check the watch is connected to the phone in the Galaxy Wearable app (Bluetooth). The transfer retries automatically on reconnect. Make sure both apps were installed from the same computer, and that the phone app has been opened at least once.
- **Settings don't sync to the watch:** same Bluetooth check; then change the setting again.
- **Watch/phone disappeared from Android Studio:** watch → `adb connect <ip:port>` again (port changes after reboot); phone → replug USB.
- **Build errors:** copy the exact error message back into our chat and I'll fix it.

## Customizing

Workout structure (durations, zone percentages): `wear/src/main/java/com/example/norwegian4x4/Workout.kt` → `buildNorwegian4x4()`. Everything else is in the Settings tab.

## Later upgrades (just ask)

- Audio cues through earbuds, more charts (pace trends, time-in-zone), workout notes, auto-opening the Strava upload page, an app icon.
