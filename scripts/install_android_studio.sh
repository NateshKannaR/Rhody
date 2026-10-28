#!/usr/bin/env bash
set -e

URL="https://edgedl.me.gvt1.com/android/studio/ide-zips/2026.2.1.8/android-studio-rabbit1-linux.tar.gz"
TARGET_TAR="/home/natesh/Downloads/android-studio.tar.gz"
EXTRACT_DIR="/home/natesh"
STUDIO_DIR="/home/natesh/android-studio"

echo "=== Step 1: Downloading Android Studio ==="
wget -c "$URL" -O "$TARGET_TAR"

echo "=== Step 2: Extracting to $EXTRACT_DIR ==="
tar -xzf "$TARGET_TAR" -C "$EXTRACT_DIR"

echo "=== Step 3: Setting permissions and symlinks ==="
chmod +x "$STUDIO_DIR/bin/studio.sh"
mkdir -p /home/natesh/.local/bin
ln -sf "$STUDIO_DIR/bin/studio.sh" /home/natesh/.local/bin/studio
ln -sf "$STUDIO_DIR/bin/studio.sh" /home/natesh/.local/bin/android-studio

echo "=== Step 4: Creating Desktop Entry ==="
mkdir -p /home/natesh/.local/share/applications
cat << 'EOF' > /home/natesh/.local/share/applications/android-studio.desktop
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Icon=/home/natesh/android-studio/bin/studio.png
Exec=/home/natesh/android-studio/bin/studio.sh %f
Comment=The official Android IDE
Categories=Development;IDE;
Terminal=false
StartupWMClass=jetbrains-studio
EOF
chmod +x /home/natesh/.local/share/applications/android-studio.desktop

echo "=== Step 5: Cleaning up tar archive ==="
rm -f "$TARGET_TAR"

echo "=== Android Studio Installation Complete! ==="
