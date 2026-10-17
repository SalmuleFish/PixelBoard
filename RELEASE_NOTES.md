# PixelBoard Stable Release (v18.4.1-V6 / Patch v1.1.0)

## Summary
PixelBoard v18.4.1-V6 (Patch Bundle v1.1.0) delivers high-resolution Material You adaptive app icons across all screen densities, Rambler voice typing compliance feedback access point suppression, immediate access point population (preventing blank shortcuts on first launch), automated clipboard suggestions and screenshot auto-pasting, pre-consented Writing Tools and Jarvis integration, and backend host alignment for Writing Helper and Imagen on top of Google's latest **Gboard v18.4.1 Release** (`18.4.1.985164140-release-arm64-v8a`).

---

## What's New in This Release (v18.4.1-V6 / Patch v1.1.0)

### 1. Updated High-Resolution App Icon & Monochrome Vector Theming
- **Adaptive Launcher Icons**: Replaced raster assets with official high-resolution Material You adaptive app icons and mipmap drawables across all densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
- **Material You Dynamic Theming**: Added monochrome vector drawable (`drawable_0x7f080516.xml`) ensuring proper theme-tinted icons on Android 13+ Pixel and third-party launchers.

### 2. Gemini Rambler Voice Dictation & Access Point Refinements
- **Compliance Access Point Suppression**: Hooked `AgenticDictationFeedbackAccessPointProvider` / `fcx.a(Context)` to suppress redundant Rambler compliance feedback buttons in the toolbar.
- **Standalone Package Out-of-the-Box Setup**: Retains automatic seeding of `enable_jetson = true` for zero-configuration Rambler dictation on unrooted devices.

### 3. Immediate Access Points Population & Clipboard Suggestion Chips
- **Instant Shortcuts**: Enforced `stop_deferring_access_points_tasks = true` to immediately populate keyboard toolbar access points, preventing blank or missing shortcuts on cold boot.
- **Auto-Paste & Screenshot Chips**: Automatically initializes `enable_auto_paste_chips = true`, `enable_screenshot_in_clipboard = true`, `clipboard_screenshot_enabled_at_least_once = true`, and `enable_entity_extraction = true` across SharedPreferences.

### 4. Writing Tools, Jarvis Consent & Backend Server Alignment
- **Pre-Configured Writing Tools Consent**: Automatically pre-seeds `jarvis_consent_history`, `pref_key_writing_tools_opt_in`, and screen context permissions so Writing Tools are immediately operational.
- **Backend Server Targets**: Configured `writing_helper_server_target` and `gboard_imagen_api_host` targeting production/staging endpoints (`staging-gboard-imagen-pa.sandbox.googleapis.com`) with voice input model configuration.

### 5. Version 1.1.0 Synchronization
- **System-Wide Alignment**: Synchronized patch bundle version `1.1.0` across Gradle build properties, settings UI, preferences metadata, and bundle manifests.

---

## Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-18.4.1-V6.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V6/PixelBoard-18.4.1-V6.apk) | **Signed APK** *(Recommended)* | ~127 MB | **Ready to install.** Signed stable APK (v18.4.1 release base, versionCode 176027079, side-by-side coexistence). |
| [**patches-1.1.0.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V6/patches-1.1.0.mpp) | **Morphe Patch Bundle** | ~1.5 MB | Standalone patch file (v1.1.0) for [Morphe Manager](#option-3-patch-via-morphe-manager) to patch your own base APK. |
| [**Stock Gboard v18.4.1**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-release/) | **Base APK** | ~67 MB | Official unmodified base APK from APKMirror *(only required if patching manually with Morphe)*. |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-18.4.1-V6.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V6/PixelBoard-18.4.1-V6.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation (supports updating in-place over earlier builds).
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-18.4.1-V6.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard [**v18.4.1.985164140-release-arm64-v8a**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-release/) (or `v18.3.1`) and apply patches.

---

## Compatibility
- Target Package: `com.google.android.inputmethod.latin`
- Target Versions:
  - `18.4.1.985164140-release-arm64-v8a` (Primary Stable / Recommended)
  - `18.3.1.977415014-release-arm64-v8a`
- Architecture: `arm64-v8a`
- Minimum Android Version: Android 10 (API 29)+
- Standalone Coexistence: Supported (package `com.akshaykadam.pixelboard` installs alongside factory Gboard)
