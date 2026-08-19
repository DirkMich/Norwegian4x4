# Walkthrough - Added App Logos

The launcher icons for both the mobile and watch applications have been successfully configured in the project manifests.

## Changes Made

### [mobile] (Mobile App)
- Updated [AndroidManifest.xml](file:///C:/Users/Dirk/StudioProjects/Norwegian4x4/mobile/src/main/AndroidManifest.xml) to reference the new icons:
    - `android:icon="@mipmap/ic_launcher"`
    - `android:roundIcon="@mipmap/ic_launcher_round"`
- Added standard application attributes: `android:allowBackup="true"` and `android:supportsRtl="true"`.

### [wear] (Watch App)
- Updated [AndroidManifest.xml](file:///C:/Users/Dirk/StudioProjects/Norwegian4x4/wear/src/main/AndroidManifest.xml) to reference the new icons:
    - `android:icon="@mipmap/ic_launcher"`
    - `android:roundIcon="@mipmap/ic_launcher_round"`
- Added standard application attributes: `android:allowBackup="true"` and `android:supportsRtl="true"`.

## Verification Results

### Automated Tests
- No automated tests were run as this is a resource/manifest change.

### Manual Verification Required
- [ ] Build and run the **mobile** app. Check that the icon appears correctly on the home screen.
- [ ] Build and run the **wear** app. Check that the icon appears correctly in the watch app list.
