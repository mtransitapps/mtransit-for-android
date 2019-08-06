#!/bin/bash
declare -a FILES=(
	"google-services.json"
	"app-signing-release-keystore.keystore"
	"app-signing-release-keys.properties"
	"google-play-upload-keystore.keystore"
	"google-play-upload-keys.properties"
	"res/values/keys.xml"
);
echo "Files:";
printf '* "%s"\n' "${FILES[@]}";
