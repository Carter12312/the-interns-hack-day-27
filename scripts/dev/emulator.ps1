param(
    [string]$AvdName = $(if ($env:ANDROID_AVD_NAME) { $env:ANDROID_AVD_NAME } else { "Nexus_5_API_34" })
)

. "$PSScriptRoot\_common.ps1"
Assert-File $Emulator "Android Emulator is missing from $AndroidSdk."

$AvailableAvds = @(& $Emulator -list-avds)
if ($AvailableAvds -notcontains $AvdName) {
    Write-Host "Available AVDs:"
    $AvailableAvds | ForEach-Object { Write-Host "  $_" }
    throw "AVD '$AvdName' does not exist. Pass another name with -AvdName."
}

Write-Host "Starting Android emulator: $AvdName"
& $Emulator "@$AvdName"
