# PixelBoard Stable Release (v18.4.1-V4 / Patch v1.0.8)

## Summary
PixelBoard v18.4.1-V4 (Patch Bundle v1.0.8) brings major feature unlocks, system text integration, and keyboard productivity tools directly to Google's latest **Gboard v18.4.1 Release** (`18.4.1.985164140-release-arm64-v8a`). This update introduces **Modern Spell Checker Suggestion Chips & Grammar Checking**, **Scan Text / Camera OCR**, **Screenshot & Clipboard Image Floating Banner**, **Emoji Palette V2 & Dedicated Frequent Tabs**, **Super Insert / Rich In-Line Completion**, **Multilingual UI (20+ Locales)**, **Bloatware-Free Settings**, and **Instant Learning & Material 3 Rounded Keys** with zero required restarts.

---

## What's New & Unlocked in This Release

### 1. Modern Spell Checker Suggestion Chips & Grammar Checking
- **Android 12+ Inline Suggestion Chips (`GboardSpellCheckerChipsPatch`)**: Bytecode-level transformation hooking Gboard's spell checker utility class (`rcq.m` and `rcq.o`), forcing modern inline spellcheck suggestion chips and add-to-dictionary UI on all Android 12+ devices.
- **Always-On Grammar & Suggestion Strip**: Automatically initializes `spellchecker_enable_chips_ui=true`, `enable_mark_misspelled_words=true`, `pref_key_enable_grammar_checker=true`, and `show_suggestion_strip=true` across standard SharedPreferences and Device Protected Storage.
- **Spell Checker Phenotype Flags**: Enforces `spellchecker_enable_chips_ui`, `spellchecker_enable_chips_ui_web_view`, `enable_spell_checker_extension`, `writing_helper_chip_in_spellchecker`, `enable_spell_checker_highlight_on_web_input`, and `enable_spell_checker_training_cache`.
- **System Rebranding**: Full system spell checker service rebrand to **PixelBoard Spell Checker** across all system locales.

### 2. Scan Text / Camera OCR Toolbar Integration
- **Direct Optical Character Recognition**: Unlocks Gboard's hidden **Scan text** access point in the keyboard toolbar, allowing users to capture text directly through the device camera into any text field.
- **All Languages Supported**: Automatically overrides `enabled_ocr_language_tags` to `*` to enable OCR for all languages without device or region restrictions.
- **Camera Permission Added**: Enforces `android.permission.CAMERA` in `AndroidManifest.xml` to prevent security exceptions when opening the camera OCR viewfinder.

### 3. Screenshot & Image Floating Suggestion Banner
- **Floating Screenshot Chips**: Unlocks `enable_clipboard_image_suggestion`, `enable_screenshot_notification_flow`, `enable_screenshot_chip`, `enable_clipboard_recent_screenshot`, and `clipboard_screenshot_enabled_at_least_once`.
- **Media Permissions**: Enforces `android.permission.READ_MEDIA_IMAGES` and `android.permission.READ_EXTERNAL_STORAGE` in `AndroidManifest.xml`.
- **Seamless Pasting**: When the user toggles on **"Save recent screenshots in clipboard"** under **PixelBoard Settings > Clipboard**, recently captured screenshots appear instantly as a floating suggestion chip above the keyboard.

### 4. Emoji Palette V2 & Frequent Emoji Tabs
- **Refined Emoji Drawer Layout**: Unlocks `emoji_palette_v2`, `enable_frequent_emoji_recent_tab`, and `enable_emoji_frequent_recent_switch_option`.
- **Dynamic Density & Grid**: Enhances emoji grid responsiveness and provides a dedicated tab switch between recent emojis and frequent emojis.

### 5. Super Insert / Quick Insert & Smart Compose
- **Rich Multi-View Results**: Unlocks `super_insert`, `super_insert_vk`, `super_insert_enable_dynamic_multi_view`, `super_insert_contacts`, `super_insert_clipboard_query_results`, `super_insert_expression_query_results`, `super_insert_local_image_results`, `super_insert_browsing_history_results`, and `super_insert_below_cursor`.
- **Ghost Autocompletion**: Forces `enable_smart_compose`, `enable_inline_suggestions_on_all_fields`, `enable_inline_completion`, `enable_delight5_smart_compose`, and `smart_compose_status=1L` for in-line completion across text fields.

### 6. Instant Word Learning, 2-Letter Suggestions & Vibrancy
- **Instant Learning (OOV Threshold = 1)**: Typings are learned immediately and suggested after just the first 2 letters (`intended_oov_min_count = 1L`, `enable_pc_user_history_ngram_count`, `populate_ngram_count_with_context`, `enable_dynamic_lm_unigram_continuation_scoring`).
- **Contextual Emoji Suggestions**: Enabled the Delight5 concept prediction engine and emoji shortcut candidates (`enable_concept_prediction_engine`, `enable_rule_based_concept_prediction`, `enable_emoji_suggestion_from_top_candidate`).
- **Vibrant Suggestion Strip Emojis**: 100% alpha opacity hooked on candidate views (#26).
- **Material 3 Belka Rounded Keys**: Expressive pill-shaped keycaps toggle in Advanced Settings (#14).
- **Samsung & Vivo Clipboard Sync**: Foreground clipboard synchronization fix (#17).

### 7. Multilingual UI (20+ Locales) & Clean Settings Bloatware Removal
- **Native Localization**: Fully translated Advanced Settings across English, Spanish, French, German, Italian, Portuguese, Russian, Hindi, Japanese, Korean, Chinese, Arabic, Indonesian, Turkish, Vietnamese, Polish, Dutch, Thai, and Ukrainian (`@string/pixelboard_patches_settings_title`).
- **Cleaned Settings Tree**: Removed unwanted promotional bloatware ("Rate Gboard" / "Help & feedback") for a clean, distraction-free Material You preferences experience.

---

## Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-18.4.1-V4.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V4/PixelBoard-18.4.1-V4.apk) | **Signed APK** *(Recommended)* | ~127 MB | **Ready to install.** Signed stable APK (v18.4.1 release base, versionCode 176027079, side-by-side coexistence). |
| [**patches-1.0.8.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V4/patches-1.0.8.mpp) | **Morphe Patch Bundle** | ~1.5 MB | Standalone patch file (v1.0.8) for [Morphe Manager](#option-3-patch-via-morphe-manager) to patch your own base APK. |
| [**Stock Gboard v18.4.1**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-release/) | **Base APK** | ~67 MB | Official unmodified base APK from APKMirror *(only required if patching manually with Morphe)*. |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-18.4.1-V4.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V4/PixelBoard-18.4.1-V4.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation (supports updating in-place over earlier builds).
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-18.4.1-V4.apk
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
