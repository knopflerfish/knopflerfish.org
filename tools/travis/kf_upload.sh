#!/bin/bash
#
# Script for uploading Travis release builds to www.knopflerfish.org
#
# Takes 1 argument <Release version>
#
# Note! No whitespace in filenames!
#
PRIVATE_KEY=tools/travis/id_rsa.travis
RELEASE_DIR=out/distrib_$1
BUILD_TMP_DIR=out/tmp
KF_SERVER=isora.oderland.com
KF_USER=makewav1
KF_RELEASES_DIR=public_html-knopflerfish.org/releases
SSH_OPT="StrictHostKeyChecking=no"

MVN_REMOTE_DIR=/home/makewav1/public_html-resources.knopflerfish.org/repo/maven2-test/
MVN_LOCAL_REMOTE_DIR="$BUILD_TMP_DIR/remote_maven2"

run_gradle() {
    (cd $BUILD_TMP_DIR; gradle publish)
}

if [ ! -d "$RELEASE_DIR" ] ; then
    echo "Release not found at: $RELEASE_DIR"
    exit 1
fi

# Merge into the main maven repo, fetch, update and push back up
mkdir -p $MVN_LOCAL_REMOTE_DIR

rsync -r -a -v -e "ssh -o $SSH_OPT -i $PRIVATE_KEY" "${KF_USER}@${KF_SERVER}:${MVN_REMOTE_DIR}" $MVN_LOCAL_REMOTE_DIR

run_gradle

rsync -r -a -v -i -e "ssh -o $SSH_OPT -i $PRIVATE_KEY" $MVN_LOCAL_REMOTE_DIR/ "${KF_USER}@${KF_SERVER}:${MVN_REMOTE_DIR}"

Echo "Uploading KF release $1 to www.knopflerfish.org"
tar czpf - -C $RELEASE_DIR . | ssh -o "$SSH_OPT" -i $PRIVATE_KEY  -l $KF_USER $KF_SERVER "mkdir $KF_RELEASES_DIR/$1 && tar xzpf - -C $KF_RELEASES_DIR/$1"

if [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
    echo "Official release build, update release soft-link"
    MAJOR=`echo $1 | cut -d. -f1`
    LINK="current-kf_$MAJOR"
    ssh -n -o "$SSH_OPT" -i $PRIVATE_KEY -l $KF_USER $KF_SERVER "cd $KF_RELEASES_DIR && rm $LINK && ln -s $1 $LINK"
fi
