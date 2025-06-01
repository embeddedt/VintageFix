#!/bin/bash

set -e

VERSION=$(cat gradle.properties | grep mod_version | sed 's/mod_version=//')
NEXTVERSION=$1

if [ -z "$NEXTVERSION" ]; then
# from https://stackoverflow.com/a/61921674
NEXTVERSION=$(echo ${VERSION} | awk -F. -v OFS=. '{$NF += 1 ; print}')
fi

read -p "Will release $NEXTVERSION, agree? " -n 1 -r
echo
if [ "x$REPLY" == "xy" ]; then
sed -i "s/mod_version=${VERSION}/mod_version=${NEXTVERSION}/" gradle.properties
git commit -m "$NEXTVERSION" gradle.properties
git tag -a $NEXTVERSION -m "$NEXTVERSION"
git push
git push origin tag $NEXTVERSION
./gradlew -Pbuild.release=true publishMods
else
echo "Abort."
fi
