#!/bin/bash
# ORIGINAL FILE: https://github.com/mtransitapps/commons/tree/master/shared-overwrite
#NO DEPENDENCY <= EXECUTED BEFORE GIT SUBMODULE
setGitProjectName() { # copy from commons.sh
	GIT_URL=$(git config --get remote.origin.url);
	GIT_PROJECT_NAME=$(basename -- "${GIT_URL}");
	GIT_PROJECT_NAME="${GIT_PROJECT_NAME%.*}" # remove ".git" extension
	if [[ -z "${GIT_PROJECT_NAME}" ]]; then
		echo "GIT_PROJECT_NAME not found!";
		exit 1;
	fi
	PROJECT_NAME="${GIT_PROJECT_NAME%-gradle}";
	if [[ -z "${PROJECT_NAME}" ]]; then
		echo "PROJECT_NAME not found!";
		exit 1;
	fi
}

echo "================================================================================";
echo "> INIT SUBMODULES...";
echo "--------------------------------------------------------------------------------";
BEFORE_DATE=$(date +%D-%X);
BEFORE_DATE_SEC=$(date +%s);

echo "> GitHub Actions: $GITHUB_ACTIONS.";

IS_SHALLOW=$(git rev-parse --is-shallow-repository);
if [[ "$IS_SHALLOW" == "true" && "$GITHUB_ACTIONS" != "true" ]]; then
	echo "> Fetching un-shallow GIT repo...";
	git fetch -v --unshallow;
	RESULT=$?;
	if [[ ${RESULT} -ne 0 ]]; then
		echo "> Error while fetching un-shallow GIT repository!";
		exit ${RESULT};
	fi
	echo "> Fetching un-shallow GIT repo... DONE";
else
	echo "> Not a shallow GIT repo.";
fi

setGitProjectName;

CURRENT_PATH=$(pwd);

FIRST_INIT=true
if [[ -f "$CURRENT_PATH/.gitmodules" ]]; then
	FIRST_INIT=false
fi

# SHARED SUBMODULES
declare -a SUBMODULES=(
	"commons"
	"commons-java"
	"commons-android"
);

#PARSER
if [[ $PROJECT_NAME == "mtransit-for-android" ]]; then
	echo "> Main android app: '$PROJECT_NAME' > parser required";
	SUBMODULES+=('parser');
elif [[ $PROJECT_NAME == *"-bike"* ]]; then
	echo "> Bike android app: '$PROJECT_NAME' > parser NOT required";
else
	echo "> Bus/Train/... android app: '$PROJECT_NAME' > parser required";
	SUBMODULES+=('parser');
	mkdir -p agency-parser/archive; # needed for shared-opt-dir #InitRepo
	if [[ "$FIRST_INIT" == true ]]; then
		git lfs track "agency-parser/archive/*"
		RESULT=$?
		if [[ ${RESULT} -ne 0 ]]; then
			echo "> Error while configuring Git LFS tracking for agency-parser archive!"
			exit ${RESULT}
		fi
	fi
fi

echo "> Submodules:";
printf '> - "%s"\n' "${SUBMODULES[@]}";

for SUBMODULE in "${SUBMODULES[@]}" ; do
    echo "--------------------------------------------------------------------------------";
    # Check if the submodule is already registered in .gitmodules
    if ! git config --file "$CURRENT_PATH/.gitmodules" --get "submodule.${SUBMODULE}.path" >/dev/null 2>&1; then
		if [[ -d "$CURRENT_PATH/$SUBMODULE" ]]; then
			echo "> Cannot override '$CURRENT_PATH/$SUBMODULE'!";
			exit 1;
		fi
		echo "> Adding submodule '$SUBMODULE'...";
		git submodule add "https://github.com/mtransitapps/${SUBMODULE}.git" "$SUBMODULE"; # GitHub secret PAT
		RESULT=$?;
		if [[ ${RESULT} -ne 0 ]]; then
			echo "> Error while cloning '$SUBMODULE' submodule!";
			exit ${RESULT};
		fi
		echo "> Adding submodule '$SUBMODULE'... DONE ✓";
	fi
	git submodule update --init --recursive "${SUBMODULE}";
	RESULT=$?;
	if [[ ${RESULT} -ne 0 ]]; then
		echo "> Error while updating GIT submodule '$SUBMODULE'!";
		exit ${RESULT};
	fi
	echo "'$SUBMODULE' updated successfully."
done

echo "--------------------------------------------------------------------------------";
AFTER_DATE=$(date +%D-%X);
AFTER_DATE_SEC=$(date +%s);
DURATION_SEC=$((AFTER_DATE_SEC - BEFORE_DATE_SEC));
echo "> $DURATION_SEC secs FROM $BEFORE_DATE TO $AFTER_DATE";
echo "> INIT SUBMODULES... DONE ✓";
echo "================================================================================";
