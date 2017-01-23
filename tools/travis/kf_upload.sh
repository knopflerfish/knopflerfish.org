#!/bin/bash
#
# Script for uploading Travis release builds to www.knopflerfish.org
#
# Takes 2 arguments <Release version> and <Git branch>
#
# Note! No whitespace in filenames!
#
PRIVATE_KEY=tools/travis/id_rsa.travis
RELEASE_DIR=out/distrib_$1
KF_SERVER=isora.oderland.com
KF_USER=makewav1
KF_RELEASES_DIR=public_html-knopflerfish.org/releases/

if [ ! -d "$RELEASE_DIR" ] ; then
    echo "Release not found at: $RELEASE_DIR"
    exit 1
fi

if [ -d .ssh ] ; then
    echo -e "Host $KF_SERVER\n\tStrictHostKeyChecking no\n" >> .ssh/config
fi

echo "Uploading KF release $1 to www.knopflerfish.org"
scp -rpqBi $PRIVATE_KEY $RELEASE_DIR $KF_USER@$KF_SERVER:$KF_RELEASES_DIR/$1

if [ "$2" == "master" ] ; then
    echo "Master build, update release soft-link"
    MAJOR=`echo $1 | cut -d. -f1`
    LINK="current-kf_$MAJOR"
    ssh -ni $PRIVATE_KEY -l $KF_USER $KF_SERVER "cd $KF_RELEASES_DIR && rm $LINK && ln -s $1 $LINK"
fi
