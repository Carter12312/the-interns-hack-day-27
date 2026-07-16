#!/usr/bin/env bash

source "$(dirname "$0")/_common.sh"

echo "Preparing Hub Companion development environment"
require_command java "Install JDK 17 or newer and ensure java is on PATH."
require_command python3 "Install Python 3.11 or newer."
require_file "$ADB" "Android platform-tools are missing from $ANDROID_SDK_ROOT."
require_file "$EMULATOR" "Android Emulator is missing from $ANDROID_SDK_ROOT."

JAVA_VERSION="$(java -version 2>&1 | head -n 1)"
echo "Java: $JAVA_VERSION"
echo "Android SDK: $ANDROID_SDK_ROOT"

printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > "$PROJECT_ROOT/local.properties"
printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > "$PROJECT_ROOT/mobile-app/local.properties"

if [[ ! -x "$PROJECT_ROOT/.venv/bin/python" ]]; then
  python3 -m venv "$PROJECT_ROOT/.venv"
fi
"$PROJECT_ROOT/.venv/bin/python" -m pip install -r "$PROJECT_ROOT/requirements.txt"

echo
echo "Available Android virtual devices:"
"$EMULATOR" -list-avds || true
echo
echo "Setup complete. Run: scripts/dev/emulator.sh"
