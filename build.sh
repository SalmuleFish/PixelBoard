#!/usr/bin/env bash
# ==============================================================================
# PixelBoard - Build & Patch Automation Script (Security Hardened Fork)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD_PATCH=false
INPUT_APK="input/gboard.apk"
for arg in "$@"; do
    case "$arg" in
        -r|--rebuild|--compile|-c)
            REBUILD_PATCH=true
            ;;
        -h|--help)
            echo "Usage: ./build.sh [options] [input_apk]"
            echo "Options:"
            echo "  -r, --rebuild    Recompile patch bundle from pixelboard-patches/ using Gradle"
            echo "  -h, --help       Show this help message"
            echo ""
            echo "By default, ./build.sh runs 100% offline using the pre-bundled patches/PixelBoard.mpp."
            exit 0
            ;;
        *)
            if [[ -f "$arg" ]]; then
                INPUT_APK="$arg"
            fi
            ;;
    esac
done

PATCH_VERSION=$(grep -E "^version\s*=" pixelboard-patches/gradle.properties | cut -d'=' -f2 | tr -d ' ')
MPP_PATH="patches/PixelBoard.mpp"

if [[ "$REBUILD_PATCH" == true ]] || [[ ! -f "$MPP_PATH" ]]; then
    echo "=== 🎹 Compiling PixelBoard Patch Bundle from Source ==="
    cd pixelboard-patches
    ./gradlew clean :patches:buildAndroid
    cd "$SCRIPT_DIR"

    COMPILED_MPP="pixelboard-patches/patches/build/libs/patches-${PATCH_VERSION}.mpp"
    if [[ ! -f "$COMPILED_MPP" ]]; then
        COMPILED_MPP=$(ls pixelboard-patches/patches/build/libs/patches-*.mpp 2>/dev/null | grep -v 'javadoc' | grep -v 'sources' | head -n 1)
    fi
    if [[ -z "${COMPILED_MPP:-}" ]] || [[ ! -f "$COMPILED_MPP" ]]; then
        echo "❌ Error: Patch bundle failed to build at pixelboard-patches/patches/build/libs/"
        exit 1
    fi
    mkdir -p patches
    cp "$COMPILED_MPP" "$MPP_PATH"
    if [[ -n "${PATCH_VERSION:-}" ]]; then
        cp "$COMPILED_MPP" "patches/patches-${PATCH_VERSION}.mpp"
    fi
    echo "✅ Patch bundle compiled & synced to: $MPP_PATH"
else
    echo "📦 Using local patch bundle: $MPP_PATH (100% offline)"
fi

# Check for Java 21 to run patcher.jar
JAVA_BIN=""
for candidate in \
    "${JAVA_HOME:-}/bin/java" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" \
    "/opt/homebrew/opt/openjdk@21/bin/java" \
    /opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home/bin/java \
    "/opt/homebrew/opt/openjdk/bin/java" \
    "$(/usr/libexec/java_home -v 21 2>/dev/null)/bin/java" \
    "$(which java 2>/dev/null)"; do
    if [[ -x "$candidate" ]]; then
        candidate_ver=$("$candidate" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [[ "$candidate_ver" -ge 21 ]]; then
            JAVA_BIN="$candidate"
            break
        fi
    fi
done

if [[ -z "$JAVA_BIN" ]]; then
    echo "⚠️ Note: patcher.jar requires Java 21+."
    echo "   Please install Java 21 (e.g. 'brew install openjdk@21') or specify JAVA_HOME."
    exit 1
fi
echo "☕ Using Java 21+: $JAVA_BIN"

if [[ ! -f "$INPUT_APK" ]]; then
    CANDIDATE=$(ls input/*.apk input/*.apkm 2>/dev/null | head -n 1)
    if [[ -n "$CANDIDATE" ]]; then
        INPUT_APK="$CANDIDATE"
    else
        echo "⚠️ Note: Place stock Gboard APK at $INPUT_APK to generate output APK."
        exit 0
    fi
fi

echo "=== 🔨 Applying Patches to $INPUT_APK ==="
mkdir -p output
mkdir -p tools/patcher-data

KEYSTORE_PATH="tools/patcher-data/private-local.jks"
KEYSTORE_PASS_FILE="tools/patcher-data/private-local.pass"
KEYSTORE_ALIAS="pixelboard_local"

if [[ ! -f "$KEYSTORE_PATH" ]] && [[ -f "tools/patcher-data/pixelboard.keystore" ]]; then
    KEYSTORE_PATH="tools/patcher-data/pixelboard.keystore"
    KEYSTORE_ALIAS="pixelboard"
fi

if [[ ! -f "$KEYSTORE_PATH" ]]; then
    echo "🔑 Generating secure local private key (RSA-4096)..."
    RANDOM_PASS=$(head -c 32 /dev/urandom 2>/dev/null | xxd -p -c 32 2>/dev/null || openssl rand -hex 16 2>/dev/null || tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 24)
    printf "%s" "$RANDOM_PASS" > "$KEYSTORE_PASS_FILE"
    chmod 600 "$KEYSTORE_PASS_FILE" 2>/dev/null || true

    keytool -genkeypair \
        -keystore "$KEYSTORE_PATH" \
        -storetype PKCS12 \
        -alias "$KEYSTORE_ALIAS" \
        -keyalg RSA \
        -keysize 4096 \
        -validity 10000 \
        -storepass "$RANDOM_PASS" \
        -keypass "$RANDOM_PASS" \
        -dname "CN=PixelBoard Local Private Key, OU=Security Hardened, O=Private, L=Local, ST=Local, C=US" \
        >/dev/null 2>&1
    echo "✅ Generated private key: $KEYSTORE_PATH"
fi

if [[ -f "$KEYSTORE_PASS_FILE" ]]; then
    KEYSTORE_PASS=$(cat "$KEYSTORE_PASS_FILE")
else
    KEYSTORE_PASS="${KEYSTORE_PASSWORD:-pixelboard}"
fi

"$JAVA_BIN" -jar tools/patcher.jar patch \
    --patches="$MPP_PATH" \
    --options-file=options.json \
    --striplibs=arm64-v8a \
    --signer="PixelBoard" \
    --keystore="$KEYSTORE_PATH" \
    --keystore-password="$KEYSTORE_PASS" \
    --keystore-entry-alias="$KEYSTORE_ALIAS" \
    --keystore-entry-password="$KEYSTORE_PASS" \
    --temporary-files-path="tools/patcher-data/tmp" \
    -o=output/PixelBoard.apk \
    -r=output/patching-result.json \
    "$INPUT_APK"

python3 tools/sanitize_apk_resources.py output/PixelBoard.apk "$INPUT_APK" "$KEYSTORE_PATH" "$KEYSTORE_PASS" "$KEYSTORE_ALIAS"

if [[ -f "output/patching-result.json" ]]; then
    GBOARD_VER=$(grep -o '"packageVersion":"[^"]*"' output/patching-result.json | cut -d'"' -f4 | cut -d'-' -f1)
    GBOARD_SHORT_VER=$(echo "$GBOARD_VER" | cut -d'.' -f1-3)
    if [[ -n "$GBOARD_VER" ]]; then
        cp output/PixelBoard.apk "output/PixelBoard-${GBOARD_VER}.apk"
        echo "📦 Versioned APK saved to: output/PixelBoard-${GBOARD_VER}.apk"
    fi
    if [[ -n "$GBOARD_SHORT_VER" ]] && [[ "$GBOARD_SHORT_VER" != "$GBOARD_VER" ]]; then
        cp output/PixelBoard.apk "output/PixelBoard-${GBOARD_SHORT_VER}.apk"
        echo "📦 Short-versioned APK saved to: output/PixelBoard-${GBOARD_SHORT_VER}.apk"
    fi
fi
if [[ -n "${PATCH_VERSION:-}" ]]; then
    cp output/PixelBoard.apk "output/PixelBoard-${PATCH_VERSION}.apk"
fi

echo "🎉 Successfully built output/PixelBoard.apk!"
