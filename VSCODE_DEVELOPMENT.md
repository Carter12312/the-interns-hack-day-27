# VS Code workstation setup

Hub Companion can be developed and run without opening Android Studio. The
repository includes Gradle wrappers, VS Code tasks, FastAPI scripts, and
Android emulator helpers. The workstation still needs the platform toolchains:

- JDK 17 or newer
- Python 3.11 or newer
- Android SDK command-line tools
- Android SDK Platform 35, platform-tools, emulator, and an Android system image
- VS Code

The Android SDK is not committed because it is several gigabytes and differs
between macOS, Linux, CPU architectures, and installed emulator images.

## Windows first-time setup

Install these tools before opening the repository:

1. VS Code.
2. JDK 17 or newer, with `java.exe` available on `PATH`.
3. Python 3.11 or newer, with `python.exe` available on `PATH`.
4. Android command-line tools in `%LOCALAPPDATA%\Android\Sdk`.

The expected Android SDK structure is:

```text
%LOCALAPPDATA%\Android\Sdk\
├── cmdline-tools\latest\bin\sdkmanager.bat
├── emulator\emulator.exe
└── platform-tools\adb.exe
```

From PowerShell, install the Android packages:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" `
  "platform-tools" "emulator" "platforms;android-35" `
  "build-tools;35.0.0" `
  "system-images;android-35;google_apis_playstore;x86_64"
```

Create the Windows emulator:

```powershell
& "$sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd `
  --name Nexus_5_API_34 `
  --package "system-images;android-35;google_apis_playstore;x86_64" `
  --device "Nexus 5"
```

On a Windows ARM workstation, use an available ARM64 system image instead.

Clone the repository, open its root in VS Code, and run this once:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev\setup.ps1
```

## macOS or Linux first-time setup

Clone the repository and open its root folder in VS Code. Install the suggested
extensions when prompted, then run:

```bash
./scripts/dev/setup.sh
```

This creates `.venv`, installs Python dependencies, verifies Java and Android
tools, and writes ignored `local.properties` files for Gradle.

If the workstation does not have Android command-line tools, download them from
the Android developer site. Use `sdkmanager` to install the required packages:

```bash
sdkmanager "platform-tools" "emulator" "platforms;android-35" \
  "build-tools;35.0.0" \
  "system-images;android-35;google_apis_playstore;arm64-v8a"
```

On an Intel workstation, replace `arm64-v8a` with `x86_64`. Create an emulator:

```bash
avdmanager create avd \
  --name Nexus_5_API_34 \
  --package "system-images;android-35;google_apis_playstore;arm64-v8a" \
  --device "Nexus 5"
```

## Daily workflow

Use **Terminal → Run Task** in VS Code, in this order:

1. `Hub: Start emulator`
2. `Hub: Start FastAPI`
3. `Hub: Build, install, and launch Android`
4. `Hub: Android logs` when debugging

On Windows, the equivalent PowerShell commands are:

```powershell
.\scripts\dev\emulator.ps1
.\scripts\dev\api.ps1
.\scripts\dev\app.ps1
.\scripts\dev\logs.ps1
```

On macOS or Linux, use:

```bash
./scripts/dev/emulator.sh
./scripts/dev/api.sh
./scripts/dev/app.sh
./scripts/dev/logs.sh
```

To use a differently named virtual device on Windows:

```powershell
.\scripts\dev\emulator.ps1 -AvdName My_AVD_Name
```

On macOS or Linux:

```bash
./scripts/dev/emulator.sh My_AVD_Name
```

The Android emulator connects to the local FastAPI server through
`http://10.0.2.2:8000`. Corporate HQ parking responses are precomputed, so
opening a lot does not perform live model inference.
