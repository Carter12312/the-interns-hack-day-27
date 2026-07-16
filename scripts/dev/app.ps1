. "$PSScriptRoot\_common.ps1"

Assert-File $Adb "ADB is missing from $AndroidSdk."
& $Adb get-state *> $null
if ($LASTEXITCODE -ne 0) {
    throw "No Android device is online. Start one with .\scripts\dev\emulator.ps1."
}

Set-Location $ProjectRoot
& .\gradlew.bat :mobile-app:composeApp:installDebug
if ($LASTEXITCODE -ne 0) { throw "Android build or installation failed." }
& $Adb shell am force-stop com.statefarm.hubcompanion
& $Adb shell am start -n com.statefarm.hubcompanion/.MainActivity
