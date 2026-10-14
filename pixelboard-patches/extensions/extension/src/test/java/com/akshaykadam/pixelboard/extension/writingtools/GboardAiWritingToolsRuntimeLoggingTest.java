package com.akshaykadam.pixelboard.extension.writingtools;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.akshaykadam.pixelboard.extension.settings.GboardRoundedKeysSettings;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public final class GboardAiWritingToolsRuntimeLoggingTest {
    private static final String TAG = "GboardPatches";

    @Before
    @After
    public void resetRuntimeState() throws Exception {
        while (GboardAiWritingToolsRuntime.currentSettingsControllerScopeDepth() > 0) {
            GboardAiWritingToolsRuntime.exitSettingsControllerScope();
        }
        staticField("applicationContext").set(null, null);
        GboardAiWritingToolsOfficialPreferences.clearForTest();
        for (Field field : GboardAiWritingToolsRuntime.class.getDeclaredFields()) {
            if (field.getType() == AtomicInteger.class) {
                field.setAccessible(true);
                ((AtomicInteger) field.get(null)).set(0);
            }
        }
        ShadowLog.clear();
    }

    @Test
    public void persistentFailureLoggingIsBoundedPerSite() throws Exception {
        staticField("applicationContext").set(null, new ThrowingSharedPreferencesContext());
        GboardAiWritingToolsRuntime.enterSettingsControllerScope();

        for (int index = 0; index < 85; index++) {
            Assert.assertSame(Boolean.FALSE,
                    GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                            new Object(), Boolean.FALSE));
            Assert.assertFalse(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(
                    GboardAiWritingToolsOfficialPreferences.PROOFREAD_PREF_KEY));
            Assert.assertFalse(GboardAiWritingToolsRuntime.applySignalResult(
                    new Object(), false));
        }

        Assert.assertEquals(80L, warningCount("failed to apply flag override"));
        Assert.assertEquals(80L, warningCount("failed to evaluate settings removal"));
        Assert.assertEquals(80L, warningCount("failed to evaluate signal"));
    }

    @Test
    public void successfulFlagLogsDoNotConsumeTheFailureBudget() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = context.getSharedPreferences(
                GboardAiWritingToolsSettings.PREF_FILE, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        GboardAiWritingToolsSettings.ensureDefaults(preferences);
        staticField("applicationContext").set(null, context);

        for (int index = 0; index < 80; index++) {
            Assert.assertSame(Boolean.TRUE,
                    GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                            new FlagReceiver(), Boolean.FALSE));
        }
        ShadowLog.clear();
        staticField("applicationContext").set(null, new ThrowingSharedPreferencesContext());

        Assert.assertSame(Boolean.FALSE,
                GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                        new Object(), Boolean.FALSE));
        Assert.assertEquals(1L, warningCount("failed to apply flag override"));
    }

    @Test
    public void successfulSignalLogsDoNotConsumeTheFailureBudget() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = context.getSharedPreferences(
                GboardAiWritingToolsSettings.PREF_FILE, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        GboardAiWritingToolsSettings.ensureDefaults(preferences);
        staticField("applicationContext").set(null, context);
        Object forcedSignal = Class.forName("gvu").getField("a").get(null);

        for (int index = 0; index < 80; index++) {
            Assert.assertTrue(GboardAiWritingToolsRuntime.applySignalResult(
                    forcedSignal, false));
        }
        ShadowLog.clear();
        staticField("applicationContext").set(null, new ThrowingSharedPreferencesContext());

        Assert.assertFalse(GboardAiWritingToolsRuntime.applySignalResult(
                new Object(), false));
        Assert.assertEquals(1L, warningCount("failed to evaluate signal"));
    }

    @Test
    public void failureSitesUseTheBoundedLogger() throws Exception {
        String source = new String(Files.readAllBytes(Path.of(
                "src/main/java/com/akshaykadam/pixelboard/extension/writingtools/"
                        + "GboardAiWritingToolsRuntime.java")), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        Assert.assertTrue(source.contains(
                "logLimited(FLAG_FAILURE_LOG_COUNT,\n"
                        + "                    \"failed to apply flag override\", throwable);"));
        Assert.assertTrue(source.contains(
                "logLimited(SETTINGS_REMOVAL_LOG_COUNT,\n"
                        + "                    \"failed to evaluate settings removal\", throwable);"));
        Assert.assertTrue(source.contains(
                "logLimited(SIGNAL_FAILURE_LOG_COUNT,\n"
                        + "                    \"failed to evaluate signal\", throwable);"));
        Assert.assertFalse(source.contains(
                "Log.w(TAG, LOG_PREFIX + \"failed to apply flag override\", throwable)"));
        Assert.assertFalse(source.contains(
                "Log.w(TAG, LOG_PREFIX + \"failed to evaluate settings removal\", throwable)"));
        Assert.assertFalse(source.contains(
                "safeLog(\"failed to evaluate signal\", throwable)"));
    }

    @Test
    public void deviceIntelligenceFlagEnforcesTrueAndLogsDiagnostics() throws Exception {
        DeviceIntelligenceFlagReceiver receiver = new DeviceIntelligenceFlagReceiver();

        // 1. Overriding Boolean.FALSE -> returns Boolean.TRUE and logs to GboardPatches tag
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=enable_device_intelligence, original=Boolean(false), enforced=Boolean(true)"));

        // 2. Already Boolean.TRUE -> returns Boolean.TRUE without redundant logging
        Object unchanged = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.TRUE);
        Assert.assertSame(Boolean.TRUE, unchanged);
        Assert.assertEquals(1L, warningCount(
                "flag=enable_device_intelligence, original=Boolean(false), enforced=Boolean(true)"));

        // 3. Non-boolean values are preserved
        Object wrongType = "disabled";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, wrongType));

        // 4. Logging is bounded (at most 80 warnings)
        for (int index = 0; index < 85; index++) {
            GboardAiWritingToolsRuntime.applyOverriddenFlagValue(receiver, Boolean.FALSE);
        }
        Assert.assertEquals(80L, warningCount(
                "flag=enable_device_intelligence, original=Boolean(false), enforced=Boolean(true)"));
    }

    @Test
    public void roundedKeysFlagsEnforceTrueWhenEnabledAndLogDiagnostics() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        staticField("applicationContext").set(null, context);
        SharedPreferences preferences = GboardRoundedKeysSettings.preferences(context);
        GboardRoundedKeysSettings.writeEnabled(preferences, false);

        RoundedKeysFlagReceiver receiver = new RoundedKeysFlagReceiver();

        // 1. When disabled, original value is preserved
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE));
        Assert.assertEquals(0L, warningCount(
                "flag=belka_rounded_keyboard, original=Boolean(false), enforced=Boolean(true)"));

        // 2. When enabled, Boolean.FALSE is overridden to Boolean.TRUE with diagnostics log
        GboardRoundedKeysSettings.writeEnabled(preferences, true);
        ShadowLog.clear();

        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=belka_rounded_keyboard, original=Boolean(false), enforced=Boolean(true)"));

        // 3. When already Boolean.TRUE, returns Boolean.TRUE without diagnostic warning
        ShadowLog.clear();
        Object unchanged = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.TRUE);
        Assert.assertSame(Boolean.TRUE, unchanged);
        Assert.assertEquals(0L, warningCount(
                "flag=belka_rounded_keyboard, original=Boolean(false), enforced=Boolean(true)"));

        // 4. Non-boolean values are preserved
        Object wrongType = "invalid";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, wrongType));
    }

    @Test
    public void emojiScaleFlagEnforcesTrue() throws Exception {
        Object receiver = new EmojiScaleFlagReceiver();

        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);

        Object alreadyTrue = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.TRUE);
        Assert.assertSame(Boolean.TRUE, alreadyTrue);
    }

    private static long warningCount(String message) {
        String expected = "[gboard-writing-tools-18.0.3] " + message;
        return ShadowLog.getLogsForTag(TAG).stream()
                .filter(item -> item.type == android.util.Log.WARN)
                .filter(item -> expected.equals(item.msg))
                .count();
    }

    private static Field staticField(String name) throws Exception {
        Field field = GboardAiWritingToolsRuntime.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class FlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS;
    }

    private static final class DeviceIntelligenceFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_DEVICE_INTELLIGENCE;
    }

    private static final class EmojiScaleFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_EMOJI_SCALE_SUPPORTED;
    }

    private static final class RoundedKeysFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardRoundedKeysSettings.FLAG_BELKA_ROUNDED_KEYBOARD;
    }

    private static final class ThrowingSharedPreferencesContext extends ContextWrapper {
        private ThrowingSharedPreferencesContext() {
            super(null);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            throw new IllegalStateException("settings unavailable");
        }
    }
}
