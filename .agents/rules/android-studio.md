# Android Studio & Antigravity Workflow Guide

This project is configured for seamless co-development between Google Antigravity and Android Studio.

## Environment Paths
- **Android Studio Installation**: `/home/natesh/android-studio`
- **CLI Commands**: `studio` or `android-studio` (available via `~/.local/bin/`)
- **Android SDK**: `/home/natesh/android-sdk` (configured in `local.properties`)
- **JDK 17**: `/home/natesh/.tools/jdk-17`

## Available Antigravity Tasks
In Antigravity, press `Ctrl+Shift+P` -> `Tasks: Run Task` (or configure a keybinding):
1. **Android Studio: Open Rhody in Studio**: Launches Android Studio with `/home/natesh/Downloads/Rhody` directly opened.
2. **Android Studio: Build Debug APK**: Compiles the project using JDK 17 and Gradle.
3. **Android Studio: Build, Install & Launch APK**: Compiles the APK and deploys to a connected physical device or emulator via ADB.
4. **Android Studio: Clean Project**: Cleans build artifacts and caches.
5. **Android Studio: Stream Logcat (Rhody)**: Streams real-time Android logcat filtered for Rhody logs.

## Helper Scripts
- `scripts/launch_studio.sh`: Launches Android Studio in the background.
- `scripts/build_and_install.sh`: Builds and deploys debug APK.
