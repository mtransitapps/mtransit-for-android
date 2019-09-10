#!/bin/bash
declare -a FILES=(
	"google-services.json"
	"app-signing-release-keystore.keystore"
	"app-signing-release-keys.properties"
	"app-signing-release-keystore-encrypted.keystore"
	"google-play-upload-certificate.pem"
	"google-play-upload-keystore.keystore"
	"google-play-upload-keys.properties"
	"res/values/keys.xml"
);
echo "Files:";
printf '* "%s"\n' "${FILES[@]}";
