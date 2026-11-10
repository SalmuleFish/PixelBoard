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
import struct
import subprocess
import zipfile
import tempfile

def remove_all_injected_header_preferences(data):
    data = bytearray(data)
    while True:
        tag, header_size, size, str_count, style_count, flags, str_offset, style_offset = struct.unpack_from('<HHIIIIII', data, 8)
        offsets = struct.unpack_from(f'<{str_count}I', data, 8 + 28)
        is_utf8 = (flags & (1 << 8)) != 0
        str_data = data[8 + str_offset:]
        strings = []
        for off in offsets:
            if is_utf8:
                u16len = str_data[off]
                idx = off + (1 if u16len < 0x80 else 2)
                u8len = str_data[idx]
                idx += (1 if u8len < 0x80 else 2)
                s = str_data[idx:idx+u8len].decode('utf-8', errors='replace')
            else:
                u16len = struct.unpack_from('<H', str_data, off)[0]
                s = str_data[off+2:off+2+u16len*2].decode('utf-16le', errors='replace')
            strings.append(s)

        pos = 8 + size
        tag, header_size, size = struct.unpack_from('<HHI', data, pos)
        if tag == 0x0180: pos += size

        start_tag_pos = None
        target_start = None
        target_end = None
        in_target = False

        while pos < len(data):
            chunk_pos = pos
            tag, header_size, size = struct.unpack_from('<HHI', data, pos)
            if tag == 0x0102: # START_TAG
                line_num, comment, ns, name, attr_start, attr_size, attr_count, id_idx, class_idx, style_idx = struct.unpack_from('<IIIIHHHHHH', data, pos + 8)
                tag_name = strings[name] if name < len(strings) else ''
                if 'HeaderPreference' in tag_name:
                    start_tag_pos = chunk_pos
                    in_target = False
                for a in range(attr_count):
                    raw = data[pos + 8 + attr_start + a * 20 : pos + 8 + attr_start + (a + 1) * 20]
                    ans, aname, araw, asize, ares, atype, adata = struct.unpack('<IIIHBBI', raw)
                    data_str = strings[adata] if (adata < len(strings)) else ''
                    raw_str = strings[araw] if (araw < len(strings)) else ''
                    name_s = strings[aname] if aname < len(strings) else ''
                    val_check = [data_str, raw_str, name_s]
                    if any('pixelboard_updates_entry' in v or 'action.UPDATES' in v for v in val_check):
                        in_target = True
                        target_start = start_tag_pos
                        break
            elif tag == 0x0103: # END_TAG
                line_num, comment, ns, name = struct.unpack_from('<IIII', data, pos + 8)
                tag_name = strings[name] if name < len(strings) else ''
                if 'HeaderPreference' in tag_name and in_target:
                    target_end = chunk_pos + size
                    break
            pos += size

        if target_start is not None and target_end is not None:
            data = data[:target_start] + data[target_end:]
            struct.pack_into('<I', data, 4, len(data))
        else:
            break
    return bytes(data)

def sanitize_axml_preferences(apk_path):
    print("🧹 Sanitizing preference headers (removing update menu entries)...")
    temp_apk = apk_path + ".res_clean.apk"
    modified_count = 0
    with zipfile.ZipFile(apk_path, 'r') as zin, zipfile.ZipFile(temp_apk, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            content = zin.read(item.filename)
            if item.filename.startswith('res/xml/') and (b'pixelboard_updates_entry' in content or b'action.UPDATES' in content):
                sanitized = remove_all_injected_header_preferences(content)
                if len(sanitized) != len(content):
                    content = sanitized
                    modified_count += 1
            zout.writestr(item, content)
    if modified_count > 0:
        print(f"✅ Removed update preference menu entries from {modified_count} XML file(s)")
        shutil.move(temp_apk, apk_path)
    else:
        if os.path.exists(temp_apk):
            os.remove(temp_apk)

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

    # Sanitize XML preference headers before aligning and signing
    sanitize_axml_preferences(target_apk)

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
