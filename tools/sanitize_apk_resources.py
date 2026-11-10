#!/usr/bin/env python3
"""
PixelBoard APK Alignment and Signing Tool
Aligns and signs the patched APK using zipalign and apksigner with the local private keystore.
Leaves DEX bytecode and compiled resources intact to preserve runtime stability and ART class verification.
"""

import sys
import os
import glob
import shutil
import subprocess

def find_sdk_tool(tool_name):
    env_dirs = [
        os.environ.get('ANDROID_HOME'),
        os.environ.get('ANDROID_SDK_ROOT'),
        os.path.join(os.environ.get('LOCALAPPDATA', ''), 'Android', 'Sdk') if os.environ.get('LOCALAPPDATA') else None
    ]
    extensions = ['', '.exe', '.bat'] if sys.platform.startswith('win') else ['']
    for env_var in env_dirs:
        if env_var and os.path.isdir(env_var):
            for ext in extensions:
                candidates = sorted(glob.glob(os.path.join(env_var, 'build-tools', '*', tool_name + ext)), reverse=True)
                if candidates:
                    return candidates[0]

    default_path = os.path.expanduser('~/Library/Android/sdk/build-tools')
    for ext in extensions:
        candidates = sorted(glob.glob(os.path.join(default_path, '*', tool_name + ext)), reverse=True)
        if candidates:
            return candidates[0]

    for ext in extensions:
        which_path = shutil.which(tool_name + ext)
        if which_path:
            return which_path

    return None

def sanitize_apk(target_apk, input_apk, keystore_path, keystore_pass=None, keystore_alias=None):
    if not os.path.isfile(target_apk):
        print(f"❌ Error: Target APK does not exist: {target_apk}")
        sys.exit(1)

    if not keystore_pass:
        pass_file = os.path.splitext(keystore_path)[0] + ".pass"
        if os.path.isfile(pass_file):
            with open(pass_file, "r") as pf:
                keystore_pass = pf.read().strip()
        else:
            keystore_pass = os.environ.get("KEYSTORE_PASSWORD", "pixelboard")

    if not keystore_alias:
        keystore_alias = "pixelboard_local" if "private-local" in keystore_path else "pixelboard"

    zipalign = find_sdk_tool('zipalign')
    apksigner = find_sdk_tool('apksigner')

    if not zipalign or not apksigner:
        print(f"⚠️ Warning: zipalign ({zipalign}) or apksigner ({apksigner}) not found.")
        print("   APK left unsigned.")
        return

    aligned_apk = target_apk + ".aligned.apk"

    print("📏 Aligning APK with zipalign (4-byte boundary)...")
    align_cmd = [zipalign, '-f', '-p', '4', target_apk, aligned_apk]
    res = subprocess.run(align_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"❌ zipalign failed: {res.stderr}")
        sys.exit(1)

    print(f"🔑 Signing APK with local private key ({keystore_alias})...")
    sign_cmd = [
        apksigner, 'sign',
        '--ks', keystore_path,
        '--ks-pass', f'pass:{keystore_pass}',
        '--key-pass', f'pass:{keystore_pass}',
        '--ks-key-alias', keystore_alias,
        aligned_apk
    ]
    res = subprocess.run(sign_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"❌ apksigner failed: {res.stderr}")
        if os.path.exists(aligned_apk):
            os.remove(aligned_apk)
        sys.exit(1)

    print("🔍 Verifying signed APK...")
    res = subprocess.run([apksigner, 'verify', '--verbose', aligned_apk], capture_output=True, text=True)
    if res.returncode == 0:
        print("✅ APK successfully signed and verified with local private key!")
        shutil.move(aligned_apk, target_apk)
    else:
        print(f"❌ Signature verification failed: {res.stderr}")
        if os.path.exists(aligned_apk):
            os.remove(aligned_apk)
        sys.exit(1)

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print("Usage: python3 sanitize_apk_resources.py <target_apk> <input_apk> <keystore_path> [keystore_pass] [keystore_alias]")
        sys.exit(1)
    ks_pass = sys.argv[4] if len(sys.argv) > 4 else None
    ks_alias = sys.argv[5] if len(sys.argv) > 5 else None
    sanitize_apk(sys.argv[1], sys.argv[2], sys.argv[3], ks_pass, ks_alias)
