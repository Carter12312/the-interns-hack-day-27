. "$PSScriptRoot\_common.ps1"

Assert-File $Adb "ADB is missing from $AndroidSdk."
Write-Host "Streaming Hub Companion logs. Press Ctrl+C to stop."
& $Adb logcat -v color "HubWeather:D" "AndroidRuntime:E" "*:S"
