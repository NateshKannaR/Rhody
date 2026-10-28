#!/usr/bin/env bash
set -e
PROJECT_DIR="/home/natesh/Downloads/Rhody"
cd "$PROJECT_DIR"

export JAVA_HOME="/home/natesh/.tools/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/home/natesh/android-sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

echo "=== Building Rhody Debug APK ==="
./gradlew assembleDebug

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "=== Build Successful: $APK_PATH ==="
    if command -v adb >/dev/null 2>&1 && adb devices | grep -q 'device$'; then
        echo "=== Installing to connected Android device ==="
        adb install -r "$APK_PATH"
        echo "=== Launching Rhody MainActivity ==="
        adb shell am start -n com.nateshr.rhody/.MainActivity
        echo "=== App is running on device ==="
    else
        echo "Note: No connected ADB device detected. APK built successfully at $APK_PATH."
    fi
else
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi
