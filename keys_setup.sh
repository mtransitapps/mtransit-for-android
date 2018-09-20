#!/bin/bash
source ../commons/commons.sh
echo ">> Setup-ing keys...";

source keys_files.sh;

if [[ ${#FILES[@]} -lt 1 ]]; then
	echo "FILES environment variable is NOT defined (need at least 1 empty \"\")!";
	exit -1;
fi

if [[ -z "${MT_ENCRYPT_KEY}" ]]; then
	echo "MT_ENCRYPT_KEY environment variable is NOT defined!";
	exit -1;
fi

for FILE in "${FILES[@]}" ; do
	if [[ -z "${FILE}" ]]; then
		echo "Ignoring empty '$FILE'.";
		continue;
	fi

	FILE_ENC="enc/${FILE}";

	if [ ! -f ${FILE} ]; then
		echo "File '$FILE' does NOT exist!";
		exit -1;
	fi

	if [ ! -f ${FILE_ENC} ]; then
		echo "File '$FILE_ENC' does NOT exist!";
		exit -1;
	fi
done

for FILE in "${FILES[@]}" ; do
	if [[ -z "${FILE}" ]]; then
		echo "Ignoring empty '$FILE'.";
		continue;
	fi

	FILE_ENC="enc/${FILE}";

	# Encrypt file: openssl aes-256-cbc -md sha256 -salt -in file.clear -out enc/file.clear -k $MT_ENCRYPT_KEY
	openssl aes-256-cbc -md sha256 -d -in ${FILE_ENC} -out ${FILE} -k ${MT_ENCRYPT_KEY};
	RESULT=$?;
	if [ ${RESULT} -ne 0 ]; then
		echo "Error while decrypting '$FILE_ENC'!";
		exit ${RESULT};
	fi

	git diff --name-status --exit-code ${FILE};
	RESULT=$?;
	if [ ${RESULT} -eq 0 ]; then
		echo "Decrypted file '$FILE' NOT different than clear file!";
		exit ${RESULT};
	fi

	echo "File '$FILE' decrypted successfully."
done

echo ">> Setup-ing keys... DONE";

