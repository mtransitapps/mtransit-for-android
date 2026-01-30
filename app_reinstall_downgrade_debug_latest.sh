#!/bin/bash
SCRIPT_DIR="$(dirname "$0")"
source "${SCRIPT_DIR}"/commons/commons.sh;
source "${SCRIPT_DIR}"/commons/adb_setup.sh;
checkResult $?;
source "${SCRIPT_DIR}"/app_setup.sh "debug";
checkResult $?;

APK_DIR="${SCRIPT_DIR}/app-android/build/outputs/apk/debug";
APK_FILE=$(find "$APK_DIR" -name "*.apk" 2>/dev/null | head -n 1);

if [ -z "$APK_FILE" ] || [ ! -f "$APK_FILE" ]; then
    echo "APK file not found in $APK_DIR. Building...";
    "${SCRIPT_DIR}"/gradlew assembleDebug;
    checkResult $?;
    APK_FILE=$(find "$APK_DIR" -name "*.apk" 2>/dev/null | head -n 1);
    if [ -z "$APK_FILE" ] || [ ! -f "$APK_FILE" ]; then
        echo "Failed to build APK file!";
        exit 1;
    fi
fi

echo "Installing APK: $APK_FILE";
$ADB install -r -d "$APK_FILE";
checkResult $?;
echo "APK installed successfully!";
