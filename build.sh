#!/bin/bash
source ../commons/commons.sh
echo ">> Building...";
IS_CI=false;
if [[ ! -z "${CI}" ]]; then
	IS_CI=true;
fi
echo "app-android/build.sh: IS_CI '${IS_CI}'";
GRADLE_ARGS="";
if [ $IS_CI = true ]; then
	GRADLE_ARGS=" --console=plain";
fi
DIRECTORY=$(basename ${PWD});
CUSTOM_SETTINGS_GRADLE_FILE="../settings.gradle.all";
./keys_setup.sh;
RESULT=$?;
checkResult $RESULT;
if [ -f $CUSTOM_SETTINGS_GRADLE_FILE ]; then
	../gradlew -c $CUSTOM_SETTINGS_GRADLE_FILE :$DIRECTORY:clean :$DIRECTORY:assembleRelease :$DIRECTORY:copyReleaseApkToOutputDirs $GRADLE_ARGS;
	RESULT=$?;
	checkResult $RESULT;
else
	../gradlew :$DIRECTORY:clean :$DIRECTORY:assembleRelease :$DIRECTORY:copyReleaseApkToOutputDirs $GRADLE_ARGS;
	RESULT=$?;
	checkResult $RESULT;
fi
./keys_cleanup.sh;
RESULT=$?;
checkResult $RESULT;
echo ">> Building... DONE";
exit $RESULT;
