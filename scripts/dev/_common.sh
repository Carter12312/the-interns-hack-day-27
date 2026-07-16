#!/usr/bin/env bash

set -euo pipefail

DEV_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$DEV_SCRIPT_DIR/../.." && pwd)"

if [[ -z "${ANDROID_SDK_ROOT:-}" ]]; then
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    ANDROID_SDK_ROOT="$ANDROID_HOME"
  elif [[ "$(uname -s)" == "Darwin" ]]; then
    ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
  else
    ANDROID_SDK_ROOT="$HOME/Android/Sdk"
  fi
fi

export ANDROID_SDK_ROOT
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"

ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
EMULATOR="$ANDROID_SDK_ROOT/emulator/emulator"

fail() {
  echo "Error: $*" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || fail "$2"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$2"
}
