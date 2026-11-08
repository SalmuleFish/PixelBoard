package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class GboardSpeechServicesDetector {
    public static final String PACKAGE_NAME = "com.google.android.tts";

    // Official Google signing certificate SHA-256 digests for Speech Services by Google
    private static final Set<String> GOOGLE_CERT_SHA256_DIGESTS = new HashSet<String>(Arrays.asList(
            "3d7a1223019aa39d9ea0e3436ab7c0896bfb4fb679f4de5fe7c23f326c8f994a",
            "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
            "1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00",
            "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053"
    ));

    private GboardSpeechServicesDetector() {
    }

    public static Status detect(Context context) {
        if (context == null) {
            return Status.UNKNOWN;
        }
        try {
            Context applicationContext = context.getApplicationContext();
            Context lookupContext = applicationContext != null ? applicationContext : context;
            PackageManager packageManager = lookupContext.getPackageManager();
            if (packageManager == null) {
                return Status.UNKNOWN;
            }
            ApplicationInfo appInfo = packageManager.getApplicationInfo(PACKAGE_NAME, 0);
            if (appInfo == null) {
                return Status.ABSENT;
            }
            boolean isSystemApp = (appInfo.flags
                    & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (isSystemApp || isGoogleSignedPackage(packageManager, PACKAGE_NAME)) {
                return Status.PRESENT;
            }
            return Status.ABSENT;
        } catch (PackageManager.NameNotFoundException exception) {
            return Status.ABSENT;
        } catch (Throwable throwable) {
            return Status.UNKNOWN;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isGoogleSignedPackage(PackageManager packageManager, String packageName) {
        try {
            PackageInfo pkgInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES);
            if (pkgInfo == null || pkgInfo.signatures == null || pkgInfo.signatures.length == 0) {
                return false;
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Signature sig : pkgInfo.signatures) {
                if (sig == null) continue;
                byte[] digest = md.digest(sig.toByteArray());
                StringBuilder sb = new StringBuilder(digest.length * 2);
                for (byte b : digest) {
                    sb.append(String.format(Locale.US, "%02x", b & 0xff));
                }
                if (GOOGLE_CERT_SHA256_DIGESTS.contains(sb.toString())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public enum Status {
        PRESENT,
        ABSENT,
        UNKNOWN
    }
}
