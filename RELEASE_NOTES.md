# PixelBoard Stable Release (v18.4.1-V3 / Patch v1.0.7)

## Summary
PixelBoard v18.4.1-V3 (Patch Bundle v1.0.7) brings key quality-of-life enhancements, typing engine fixes, and customization features directly to Google's latest **Gboard v18.4.1 Release** (`18.4.1.985164140-release-arm64-v8a`). This update introduces **Instant Word Learning & 2-Letter Prefix Suggestions**, **Contextual Emoji Suggestions**, **Stable Autocorrect & Fluid Glide Typing**, **PixelBoard Spell Checker & Settings Branding**, **Inline Autofill / Password Manager Support**, **Restored Emoji Size Slider**, **Vibrant Candidate Strip Emojis**, **Samsung & Vivo Clipboard History Sync**, **Material 3 Expressive Rounded Keys**, and a streamlined Advanced Settings experience with zero required restarts.

---

## What's New & Fixed in This Release

### 1. Improved Autocorrect, Fast Word Learning, Emoji Suggestions & Glide Typing ([#33](https://github.com/Akshayykadam/PixelBoard/issues/33))
- **Instant Word Learning & 2-Letter Prefix Suggestions**: Lowered `intended_oov_min_count` threshold from 4 to 1 and enabled Delight5 language model user history n-gram frequency, continuation scoring, and timestamp tracking (`enable_pc_user_history_ngram_count`, `populate_ngram_count_with_context`, `enable_dynamic_lm_unigram_continuation_scoring`, `enable_multiword_predictions_from_user_history`). Frequently typed words are learned immediately and suggested after typing just the first two letters.
- **Contextual Emoji Suggestions**: Activated the Delight5 concept prediction engine and emoji shortcut candidates (`enable_concept_prediction_engine`, `enable_rule_based_concept_prediction`, `enable_emoji_suggestion_from_top_candidate`, `enable_predictions_from_emoji_shortcuts`, `config_expression`). In addition, `res/xml/setting_expression.xml` defaults `pref_key_enable_emoji_suggestion` to `true`, displaying relevant emojis directly in the suggestion strip when typing associated words.
- **Stable Autocorrect & Consistent Glide Typing**: Enabled streaming touch gesture tagging (`update_streaming_touch_data_set_is_gesture`) so continuous glide paths are accurately recognized as gestures without stroke stutter. Activated the Delight5 two-stage beam decoder (`enable_two_stage_decoder`), spatial model features, and auto-correction trigger predictors (`enable_space_transposition_post_correction`, `enable_pk_spatial_model_features`, `enable_pk_auto_correction_trigger_predictor`), providing accurate, consistent word completions and auto-corrections.

### 2. PixelBoard Spell Checker & Settings Rebranding
- **PixelBoard Spell Checker**: Rebranded the text services spell checker component to **PixelBoard Spell Checker** with full multi-locale support across all 100+ language resource directories.
- **Valid Resource ID Resolution**: Preserved compiled XML resource references (`@string/string_0x7f140d61`) in `res/xml/spell_checker.xml` to ensure Android's OS `SpellCheckerInfo` framework correctly retrieves the display label in the system "Choose spellchecker" selector.
- **PixelBoard Settings Branding**: Updated main settings header, activity label, and preferences screens (`res/xml/settings.xml`, `res/xml/settings_legacy.xml`) from *Gboard settings* to **PixelBoard Settings**.

### 3. Inline Autofill & Password Manager Support ([#24](https://github.com/Akshayykadam/PixelBoard/pull/24))
- **Fix Password Manager Chips**: Resolved an issue where password manager suggestions (Bitwarden, 1Password, KeePass, Proton Pass, etc.) failed to show inline above the keyboard on the renamed package.
- **DeviceIntelligence Module Activation**: The renamed package does not receive server-side Phenotype configuration, leaving Android's `DeviceIntelligence` module disabled. Overriding `enable_device_intelligence=true` at runtime allows `onCreateInlineSuggestionsRequest` to load and render inline autofill credentials seamlessly (credits: @alincornel).

### 4. Restored Emoji Size Slider Preference ([#26](https://github.com/Akshayykadam/PixelBoard/issues/26))
- **Unlocked Native Slider**: Overrides the `emoji_scale_supported` Phenotype flag to restore the missing interactive **Emoji size** slider and live row preview under **PixelBoard Settings > Emojis, stickers & GIFs**.
- **Adjustable Key Scale**: Freely adjust the size and number of emojis displayed in a row directly within native Gboard preferences.

### 5. Vibrant Suggestion Strip Emojis ([#26](https://github.com/Akshayykadam/PixelBoard/issues/26))
- **100% Paint Vibrancy**: Fixed faded, washed-out emojis appearing in the candidate suggestion strip while typing.
- **Candidate View Hook**: Hooked `AutoCenterScaleTextView.onDraw` to ensure candidate text paint renders with full 100% opacity (`alpha = 255`) when emojis are present, eliminating secondary candidate dimming and matching the vibrant look of the emoji drawer.

### 6. Clipboard History Sync for Samsung One UI & Vivo Devices ([#17](https://github.com/Akshayykadam/PixelBoard/issues/17))
- **Empty Clipboard Fix**: Fixed the issue where copied text appeared in the immediate suggestion chip but failed to save into the clipboard drawer history on Samsung One UI and Vivo (OriginOS / FuntouchOS) devices.
- **Foreground Clip Sync**: Hooked `onStartInputView` to guarantee `ClipboardManager` listener initialization and sync copied text into clipboard drawer history when typing, overcoming OEM background clipboard suppression on Samsung and Vivo devices.

### 7. Material 3 Expressive Rounded Keys (Belka UI) ([#14](https://github.com/Akshayykadam/PixelBoard/issues/14))
- **Pill-Shaped Keycaps**: Added a toggle under **★ Advanced settings** to activate Google's expressive Material 3 Belka rounded keyboard styling.
- **Dynamic Flag Management**: Automatically forces `belka_rounded_keyboard`, `belka_ui`, `belka_background`, `more_pill_keys`, and `enable_rounded_keys` flags at runtime without restarting.

### 8. Clean Advanced Settings & Hot-Reload
- **Streamlined Preferences Screen**: Cleaned up Advanced Settings to focus on rock-solid core toggles (**Enable AI Writing Tools**, **Support All Keyboards**, **Inference Backend Type**, **Enable Advanced Voice Typing**, and **Rounded Keys**); the experimental V2 toggle has been hidden.
- **Zero-Restart Architecture**: All changes take effect immediately on the fly without restarting Gboard, force-closing the app, or rebooting your device.

---

## Release Assets

| File | Size | Description |
| :--- | :--- | :--- |
| [**PixelBoard-18.4.1-V3.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V3/PixelBoard-18.4.1-V3.apk) | ~127 MB | Ready-to-install signed Stable APK (v18.4.1 release base, versionCode 176027079) |
| [**PixelBoard.mpp**](https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp) | ~1.5 MB | Standalone patch bundle (v1.0.7) for Morphe Manager |
| [**patches-1.0.7.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V3/patches-1.0.7.mpp) | ~1.5 MB | Versioned standalone patch bundle (v1.0.7) |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-18.4.1-V3.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V3/PixelBoard-18.4.1-V3.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation (supports updating in-place over earlier v18.4.1 builds).
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-18.4.1-V3.apk
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
  - `18.4.1.985164140-beta-arm64-v8a`
  - `18.3.1.977415014-release-arm64-v8a`
- Architecture: `arm64-v8a`
- Minimum Android Version: Android 10 (API 29)+
