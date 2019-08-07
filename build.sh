#!/bin/bash
source ../commons/commons.sh
echo ">> Building...";

DIRECTORY=$(basename ${PWD});
CUSTOM_SETTINGS_GRADLE_FILE="../settings.gradle.all";

GIT_URL=$(git config --get remote.origin.url); # remote get-url origin
echo ">> Git URL: '$GIT_URL'.";
GIT_PROJECT_NAME=$(basename -- ${GIT_URL});
GIT_PROJECT_NAME="${GIT_PROJECT_NAME%.*}"
echo ">> Git project name: '$GIT_PROJECT_NAME'.";
if [[ -z "${GIT_PROJECT_NAME}" ]]; then
	echo "GIT_PROJECT_NAME not found!";
	exit 1;
fi

IS_CI=false;
if [[ ! -z "${CI}" ]]; then
	IS_CI=true;
fi
echo "$DIRECTORY/build.sh > IS_CI:'${IS_CI}'";

GRADLE_ARGS="";
if [[ ${IS_CI} = true ]]; then
	GRADLE_ARGS=" --console=plain";
fi

SETTINGS_FILE_ARGS="";
if [[ -f ${CUSTOM_SETTINGS_GRADLE_FILE} ]]; then
	SETTINGS_FILE_ARGS=" -c $CUSTOM_SETTINGS_GRADLE_FILE"; #--settings-file
fi

echo ">> Setup-ing keys...";
chmod +x keys_setup.sh;
checkResult $?;
./keys_setup.sh;
RESULT=$?;
checkResult ${RESULT};
echo ">> Setup-ing keys... DONE";

echo ">> Gradle cleaning...";
../gradlew ${SETTINGS_FILE_ARGS} clean ${GRADLE_ARGS};
RESULT=$?;
checkResult ${RESULT};
echo ">> Gradle cleaning... DONE";

if [[ ${IS_CI} = true ]]; then
    echo ">> Running test...";
	../gradlew ${SETTINGS_FILE_ARGS} :commons-android:testDebugUnitTest :app-android:testDebugUnitTest ${GRADLE_ARGS};
	RESULT=$?;
	checkResult ${RESULT};
    echo ">> Running test... DONE";

    echo ">> Running lint...";
	../gradlew ${SETTINGS_FILE_ARGS} :app-android:lintDebug ${GRADLE_ARGS};
	RESULT=$?;
	checkResult ${RESULT};
    echo ">> Running lint... DONE";

	declare -a SONAR_PROJECTS=(
	    "mtransit-for-android"
	    "commons-android"
	);
	if contains ${GIT_PROJECT_NAME} ${SONAR_PROJECTS[@]}; then
		if [[ -z "${MT_SONAR_LOGIN}" ]]; then
			echo "MT_SONAR_LOGIN environment variable is NOT defined!";
			exit 1;
		fi
		echo ">> Running sonar...";
		../gradlew ${SETTINGS_FILE_ARGS} :${DIRECTORY}:sonarqube \
			-Dsonar.organization=mtransitapps-github -Dsonar.projectName=${GIT_PROJECT_NAME} \
			-Dsonar.host.url=https://sonarcloud.io -Dsonar.login=${MT_SONAR_LOGIN} ${GRADLE_ARGS}
		RESULT=$?;
		checkResult ${RESULT};
		echo ">> Running sonar... DONE";
	else
		echo ">> Skipping sonar for '$GIT_PROJECT_NAME'.";
	fi

    echo ">> Running build, assemble & bundle...";
	../gradlew ${SETTINGS_FILE_ARGS} buildDebug assembleDebug bundleDebug ${GRADLE_ARGS};
	RESULT=$?;
	checkResult ${RESULT};
	echo ">> Running build, assemble & bundle... DONE";
fi

echo ">> Running assemble, bundle release & copy-to-output dir...";
../gradlew ${SETTINGS_FILE_ARGS} :${DIRECTORY}:assembleRelease :${DIRECTORY}:bundleRelease :${DIRECTORY}:copyReleaseApkToOutputDirs ${GRADLE_ARGS};
RESULT=$?;
checkResult ${RESULT};
echo ">> Running assemble, bundle release & copy-to-output dir... DONE";

echo ">> Cleaning keys...";
chmod +x keys_cleanup.sh;
checkResult $?;
./keys_cleanup.sh;
RESULT=$?;
checkResult ${RESULT};
echo ">> Cleaning keys... DONE";

echo ">> Building... DONE";
exit ${RESULT};
