#!/bin/bash
VERSION_PROPERTIES="version.properties";
if [ -f $VERSION_PROPERTIES ]; then
	read -r LINE < $VERSION_PROPERTIES;
	VERSION=$(echo $LINE | cut -d'=' -f 2);
	echo "Current version: $VERSION";
	NEW_VERSION=$(expr $VERSION + 1);
	echo "New version: $NEW_VERSION";
	sed -i -- "s/=$VERSION/=$NEW_VERSION/g" $VERSION_PROPERTIES;
	sed -i -- "s/r$VERSION/r$NEW_VERSION/g" $VERSION_PROPERTIES;
else
	echo "File $VERSION_PROPERTIES doesn't exist!";
	exit;
fi
