#!/bin/bash
#
# Script for uploading release notes for a release to bintray
#

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

RELEASE_VERSION="@VERSION@"
DOWNLOAD_SDK_PATH="@DISTRIB_NAME@/@SDK_NAME@.jar"

BINTRAY_URL="https://bintray.com/api/v1"

RELEASE_NOTES_PATH="packages/knopflerfish/kf_r6-snapshot/KnopflerfishBuild/versions/${RELEASE_VERSION}/release_notes"

RELEASE_NOTES_FILE="${DIR}/@DISTRIB_NAME@/release_notes.md

if [ ! -f $RELEASE_NOTES_FILE ]; then
    echo "Release note file does not exist, exiting: ${RELEASE_NOTES_FILE}"
    exit 1
fi

RELEASE_NOTES=$(<$RELEASE_NOTES_FILE)
ESCAPED_NOTES=${RELEASE_NOTES//\"/\\\"}

echo "Uploading Release Notes to Travis CI"

if [ "${PROJECT_VERSION}" != "*-SNAPSHOT" ]; then
    curl -vvf -umkwv:$BINTRAY_PASSWORD -H "Content-Type: application/json" \
         -X POST "${BINTRAY_URL}/${RELEASE_NOTES_PATH}" \
         --data "{ \"bintray\": { \"syntax\": \"markdown\", \"content\": \"${ESCAPED_NOTES}\" },}"
    echo "Release Notes uploaded"
else
    echo "Ignoring snapshot version. No Release Notes uploaded"
fi

# This gives 500 internal error on travis, hmm
#curl -vvf -umkwv:$BINTRAY_PASSWORD -H "Content-Type: application/json" \
#     -X POST "${BINTRAY_URL}/file_metadata/knopflerfish/kf-r6-snapshot/${DOWNLOAD_SDK_PATH}" \
#     --data "{  \"list_in_downloads\":\"true\" }"
#echo "Created downloads list"
