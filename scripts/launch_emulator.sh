#!/usr/bin/env bash
export ANDROID_HOME="/home/natesh/Android/Sdk"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

AVD_NAME="${1:-Medium_Phone_API_37.0}"
echo "Launching Android Emulator ($AVD_NAME)..."
nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD_NAME" > /tmp/emulator.log 2>&1 &
echo "Emulator process started in background. Logs: /tmp/emulator.log"
