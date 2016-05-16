#!/bin/bash

RELEASE_VERSION="@VERSION@"
DOWNLOAD_SDK_PATH="@DISTRIB_NAME@/@SDK_NAME@.jar"

BINTRAY_URL="https://bintray.com/api/v1"

RELEASE_NOTES_PATH="packages/knopflerfish/kf_r6-snapshot/KnopflerfishBuild/versions/${RELEASE_VERSION}/release_notes"

RELEASE_NOTES=$(<release_notes.in.md)
ESCAPED_NOTES=${RELEASE_NOTES//\"/\\\"}

if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]   
then
    curl -vvf -umkwv:$BINTRAY_PASSWORD -H "Content-Type: application/json" \
        -X POST "${BINTRAY_URL}/${RELEASE_NOTES_PATH}" \
        --data "{ \"bintray\": { \"syntax\": \"markdown\", \"content\": \"${ESCAPED_NOTES}\" },}"
    echo "Created release notes"
else
    echo "Ignoring snapshot version"
fi

# This gives 500 internal error on travis, hmm
#curl -vvf -umkwv:$BINTRAY_PASSWORD -H "Content-Type: application/json" \
#     -X POST "${BINTRAY_URL}/file_metadata/knopflerfish/kf-r6-snapshot/${DOWNLOAD_SDK_PATH}" \
#     --data "{  \"list_in_downloads\":\"true\" }"
#echo "Created downloads list"
