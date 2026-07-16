$ErrorActionPreference = "Stop"

$Script:ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$Script:AndroidSdk = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} elseif ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

$env:ANDROID_SDK_ROOT = $Script:AndroidSdk
$env:ANDROID_HOME = $Script:AndroidSdk
$env:Path = "$Script:AndroidSdk\platform-tools;$Script:AndroidSdk\emulator;$env:Path"

$Script:Adb = Join-Path $Script:AndroidSdk "platform-tools\adb.exe"
$Script:Emulator = Join-Path $Script:AndroidSdk "emulator\emulator.exe"
$Script:VenvPython = Join-Path $Script:ProjectRoot ".venv\Scripts\python.exe"

function Assert-File([string]$Path, [string]$Message) {
    if (-not (Test-Path -Path $Path -PathType Leaf)) {
        throw $Message
    }
}

function Assert-Command([string]$Command, [string]$Message) {
    if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) {
        throw $Message
    }
}
