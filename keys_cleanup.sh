#!/bin/bash
source ../commons/commons.sh
echo ">> Cleaning keys...";

source keys_files.sh;

if [[ ${#FILES[@]} -lt 1 ]]; then
	echo "FILES environment variable is NOT defined (need at least 1 empty \"\")!";
	exit -1;
fi

for FILE in "${FILES[@]}" ; do
	if [[ -z "${FILE}" ]]; then
		echo "Ignoring empty '$FILE'.";
		continue;
	fi

	FILE_ENC="enc/${FILE}";

	git checkout -- ${FILE};
	RESULT=$?;
	if [ ${RESULT} -ne 0 ]; then
		echo "Resetting decrypted file '$FILE' using 'git checkout' did NOT work!";
		rm ${FILE}; # deleting file
		exit ${RESULT};
	fi

	git diff --name-status --exit-code ${FILE};
	RESULT=$?;
	if [ ${RESULT} -ne 0 ]; then
		echo "File '$FILE' NOT the same as clear file!";
		exit ${RESULT};
	fi

	echo "File '$FILE' cleaned successfully."
done

echo ">> Cleaning keys... DONE";

