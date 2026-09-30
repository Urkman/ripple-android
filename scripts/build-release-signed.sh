#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
RIPPLE_USER_HOME="${HOME:?HOME is not set}"
KEYSTORE_FILE="${RIPPLE_RELEASE_STORE_FILE:-${RIPPLE_USER_HOME}/.config/ripple/ripple-upload.jks}"
KEYCHAIN_ACCOUNT="${RIPPLE_UPLOAD_KEYCHAIN_ACCOUNT:-ripple-upload}"
KEYCHAIN_SERVICE="${RIPPLE_UPLOAD_KEYCHAIN_SERVICE:-de.stefansturm.ripple.upload-key}"

if [[ -z "${JAVA_HOME:-}" && -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
fi

if [[ -z "${ANDROID_HOME:-}" && -d "${RIPPLE_USER_HOME}/Library/Android/sdk" ]]; then
    export ANDROID_HOME="${RIPPLE_USER_HOME}/Library/Android/sdk"
fi

if [[ ! -x /usr/bin/security ]]; then
    echo "macOS security command not found; release signing cannot retrieve the key password." >&2
    exit 1
fi

if [[ ! -f "$KEYSTORE_FILE" ]]; then
    echo "Upload keystore not found: $KEYSTORE_FILE" >&2
    exit 1
fi

RIPPLE_RELEASE_STORE_PASSWORD="$(
    /usr/bin/security find-generic-password \
        -a "$KEYCHAIN_ACCOUNT" \
        -s "$KEYCHAIN_SERVICE" \
        -w
)"

if [[ -z "$RIPPLE_RELEASE_STORE_PASSWORD" ]]; then
    echo "Upload-key password was not found in the macOS Keychain." >&2
    exit 1
fi

export RIPPLE_RELEASE_STORE_FILE="$KEYSTORE_FILE"
export RIPPLE_RELEASE_STORE_PASSWORD
export RIPPLE_RELEASE_KEY_ALIAS="${RIPPLE_RELEASE_KEY_ALIAS:-ripple-upload}"
export RIPPLE_RELEASE_KEY_PASSWORD="$RIPPLE_RELEASE_STORE_PASSWORD"

cd "$PROJECT_ROOT"
exec ./gradlew :app:bundleRelease "$@"
