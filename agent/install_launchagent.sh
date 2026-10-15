#!/bin/bash
# RemoteForge macOS LaunchAgent Installer
# Enables automatic startup of the PC agent on login

PLIST_NAME="com.remoteforge.agent.plist"
SOURCE_PLIST="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/${PLIST_NAME}"
TARGET_DIR="${HOME}/Library/LaunchAgents"
TARGET_PLIST="${TARGET_DIR}/${PLIST_NAME}"

echo "[RemoteForge] Installing LaunchAgent for automatic PC agent persistence..."
mkdir -p "${TARGET_DIR}"

# Unload existing if loaded
launchctl unload "${TARGET_PLIST}" 2>/dev/null

# Copy plist
cp "${SOURCE_PLIST}" "${TARGET_PLIST}"
chmod 644 "${TARGET_PLIST}"

# Load LaunchAgent
launchctl load "${TARGET_PLIST}"
echo "[RemoteForge] LaunchAgent installed and loaded successfully at ${TARGET_PLIST}"
echo "[RemoteForge] PC Agent will now automatically start on every macOS login."
