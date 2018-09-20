#!/bin/bash
source ../commons/commons.sh
echo ">> Building...";

DIRECTORY=$(basename ${PWD});
CUSTOM_SETTINGS_GRADLE_FILE="../settings.gradle.all";

IS_CI=false;
if [[ ! -z "${CI}" ]]; then
	IS_CI=true;
fi
echo "$DIRECTORY/build.sh > IS_CI:'${IS_CI}'";

GRADLE_ARGS="";
if [ ${IS_CI} = true ]; then
	GRADLE_ARGS=" --console=plain";
fi

SETTINGS_FILE_ARGS="";
if [ -f ${CUSTOM_SETTINGS_GRADLE_FILE} ]; then
	SETTINGS_FILE_ARGS=" -c $CUSTOM_SETTINGS_GRADLE_FILE"; #--settings-file
fi

./keys_setup.sh;
RESULT=$?;
checkResult ${RESULT};

../gradlew ${SETTINGS_FILE_ARGS} clean ${GRADLE_ARGS};
RESULT=$?;
checkResult ${RESULT};

if [ ${IS_CI} = true ]; then
	if [[ -z "${MT_SONAR_LOGIN}" ]]; then
		echo "MT_SONAR_LOGIN environment variable is NOT defined!";
		exit -1;
	fi

	../gradlew ${SETTINGS_FILE_ARGS} lint test ${GRADLE_ARGS};
	RESULT=$?;
	checkResult ${RESULT};

	../gradlew ${SETTINGS_FILE_ARGS} :${DIRECTORY}:sonarqube -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=mtransitapps-github -Dsonar.login=${MT_SONAR_LOGIN} ${GRADLE_ARGS}
	RESULT=$?;
	checkResult ${RESULT};

	../gradlew ${SETTINGS_FILE_ARGS} build assemble ${GRADLE_ARGS};
	RESULT=$?;
	checkResult ${RESULT};
fi

../gradlew ${SETTINGS_FILE_ARGS} :${DIRECTORY}:assembleRelease :${DIRECTORY}:copyReleaseApkToOutputDirs ${GRADLE_ARGS};
RESULT=$?;
checkResult ${RESULT};

./keys_cleanup.sh;
RESULT=$?;
checkResult ${RESULT};

echo ">> Building... DONE";
exit ${RESULT};
