# Implementation Plan - Adding App Logos

The goal is to add launcher icons (logos) to both the `mobile` and `wear` applications in the Norwegian4x4 project. Currently, the manifests do not specify an icon, and the resource folders are missing the standard `mipmap` structures.

## User Review Required

> [!IMPORTANT]
> Since I am an AI, I cannot interact with the Android Studio **Image Asset Studio** GUI. This tool is the recommended "best way" because it automatically generates all required screen densities (mdpi, hdpi, etc.) and handles Adaptive Icons for you.

I recommend using the following workflow:

1.  **Prepare your logo**: Ensure you have a high-resolution PNG or (preferably) a Vector Asset (SVG/PSD).
2.  **Use Image Asset Studio**:
    *   Right-click the `res` folder in each module (`mobile` and `wear`).
    *   Select **New > Image Asset**.
    *   Configure your icon (Adaptive for Mobile, Legacy/Circular for Wear).
3.  **Update Manifests**: I can automate this part once the assets are generated, or I can provide the template XMLs if you have the vector data.

## Proposed Changes

### [mobile] (Mobile App)
#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Dirk/StudioProjects/Norwegian4x4/mobile/src/main/AndroidManifest.xml)
- Add `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` to the `<application>` tag.

### [wear] (Watch App)
#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Dirk/StudioProjects/Norwegian4x4/wear/src/main/AndroidManifest.xml)
- Add `android:icon="@mipmap/ic_launcher"` to the `<application>` tag.

## Verification Plan

### Manual Verification
- Deploy both apps to an emulator or device.
- Verify the icon appears correctly in the launcher/app drawer.
- On Wear OS, verify the icon looks good in the circular app list.
