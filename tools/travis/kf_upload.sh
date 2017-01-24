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
KF_SERVER=isora.oderland.com
KF_USER=makewav1
KF_RELEASES_DIR=public_html-knopflerfish.org/releases/
SSH_OPT="StrictHostKeyChecking no"

if [ ! -d "$RELEASE_DIR" ] ; then
    echo "Release not found at: $RELEASE_DIR"
    exit 1
fi

echo "Uploading KF release $1 to www.knopflerfish.org"
scp -rpqB -o "$SSH_OPT" -i $PRIVATE_KEY $RELEASE_DIR $KF_USER@$KF_SERVER:$KF_RELEASES_DIR/$1

if [[ "$1" =~ [0-9]+\.[0-9]+\.[0-9]+ ]] ; then
    echo "Official release build, update release soft-link"
    MAJOR=`echo $1 | cut -d. -f1`
    LINK="current-kf_$MAJOR"
    ssh -n -o "$SSH_OPT" -i $PRIVATE_KEY -l $KF_USER $KF_SERVER "cd $KF_RELEASES_DIR && rm $LINK && ln -s $1 $LINK"
fi
