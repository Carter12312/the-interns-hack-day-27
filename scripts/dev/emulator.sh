#!/usr/bin/env bash

source "$(dirname "$0")/_common.sh"

require_file "$EMULATOR" "Android Emulator is missing from $ANDROID_SDK_ROOT."
AVD_NAME="${1:-${ANDROID_AVD_NAME:-Nexus_5_API_34}}"

if ! "$EMULATOR" -list-avds | grep -Fxq "$AVD_NAME"; then
  echo "Available AVDs:"
  "$EMULATOR" -list-avds
  fail "AVD '$AVD_NAME' does not exist. Pass another name as the first argument."
fi

echo "Starting Android emulator: $AVD_NAME"
exec "$EMULATOR" "@$AVD_NAME"
