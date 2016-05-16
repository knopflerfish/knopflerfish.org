#!/bin/bash
mkdir ~/.bintray/
BINTRAY_FILE=$HOME/.bintray/.credentials
ARTIFACTORY_FILE=$HOME/.bintray/.artifactory
cat <<EOF >$BINTRAY_FILE
realm = Bintray API Realm
host = api.bintray.com
user = mkwv
password = $BINTRAY_PASSWORD
EOF
