#!/usr/bin/env bash
PROJECT_DIR="${1:-/home/natesh/Downloads/Rhody}"
STUDIO_BIN="/home/natesh/android-studio/bin/studio.sh"

if [ -f "$STUDIO_BIN" ]; then
    nohup "$STUDIO_BIN" "$PROJECT_DIR" >/dev/null 2>&1 &
    echo "Android Studio launched with project: $PROJECT_DIR"
else
    echo "Android Studio not found at $STUDIO_BIN"
    exit 1
fi
