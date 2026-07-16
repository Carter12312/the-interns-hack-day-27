#!/usr/bin/env bash

source "$(dirname "$0")/_common.sh"

require_file "$ADB" "ADB is missing from $ANDROID_SDK_ROOT."
echo "Streaming Hub Companion logs. Press Ctrl+C to stop."
exec "$ADB" logcat -v color HubWeather:D AndroidRuntime:E '*:S'
