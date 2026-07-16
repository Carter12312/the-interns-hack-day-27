. "$PSScriptRoot\_common.ps1"

Write-Host "Preparing Hub Companion development environment"
Assert-Command "java" "Install JDK 17 or newer and add java.exe to PATH."
Assert-Command "python" "Install Python 3.11 or newer and add python.exe to PATH."
Assert-File $Adb "Android platform-tools are missing from $AndroidSdk."
Assert-File $Emulator "Android Emulator is missing from $AndroidSdk."

Write-Host "Java: $(& java -version 2>&1 | Select-Object -First 1)"
Write-Host "Android SDK: $AndroidSdk"

$GradleSdkPath = $AndroidSdk.Replace('\', '/')
"sdk.dir=$GradleSdkPath" | Set-Content -Encoding ASCII (Join-Path $ProjectRoot "local.properties")
"sdk.dir=$GradleSdkPath" | Set-Content -Encoding ASCII (Join-Path $ProjectRoot "mobile-app\local.properties")

if (-not (Test-Path $VenvPython)) {
    & python -m venv (Join-Path $ProjectRoot ".venv")
}
& $VenvPython -m pip install -r (Join-Path $ProjectRoot "requirements.txt")

Write-Host ""
Write-Host "Available Android virtual devices:"
& $Emulator -list-avds
Write-Host ""
Write-Host "Setup complete. Run: .\scripts\dev\emulator.ps1"
