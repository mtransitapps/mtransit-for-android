#!/bin/bash
declare -a FILES=(
	"google-services.json"
	"key-store-release.keystore"
	"keys.properties"
	"res/values/keys.xml"
);
echo "Files:";
printf '* "%s"\n' "${FILES[@]}";
