#!/usr/bin/env bash

source "$(dirname "$0")/_common.sh"

require_file "$ADB" "ADB is missing from $ANDROID_SDK_ROOT."
if ! "$ADB" get-state >/dev/null 2>&1; then
  fail "No Android device is online. Start one with scripts/dev/emulator.sh."
fi

cd "$PROJECT_ROOT"
./gradlew :mobile-app:composeApp:installDebug
"$ADB" shell am force-stop com.statefarm.hubcompanion
"$ADB" shell am start -n com.statefarm.hubcompanion/.MainActivity
