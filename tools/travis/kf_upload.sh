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

KF_REPO_DIR=public_html-resources.knopflerfish.org/repo
KF_MVN_RELEASE_DIR=$KF_REPO_DIR/maven2-release/$1
KF_MVN_REPO_DIR=$KF_REPO_DIR/maven2/release

MVN_LOCAL_REMOTE_DIR="$BUILD_TMP_DIR/remote_maven2"

run_gradle() {
    (cd $BUILD_TMP_DIR; $GRADLE_HOME/bin/gradle updateMavenRepo)
}

if [ ! -d "$RELEASE_DIR" ] ; then
    echo "Release not found at: $RELEASE_DIR"
    exit 1
fi
echo "Uploading KF release $1 to www.knopflerfish.org"
tar czpf - -C $RELEASE_DIR . | ssh -o "$SSH_OPT" -i $PRIVATE_KEY  -l $KF_USER $KF_SERVER "mkdir $KF_RELEASES_DIR/$1 && tar xzpf - -C $KF_RELEASES_DIR/$1"

if [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
    echo "Official release build - preparing maven repo"
    
    ssh -n -T -o "$SSH_OPT" -i $PRIVATE_KEY -l $KF_USER $KF_SERVER <<ENDSSH 
if [ -d $KF_MVN_RELEASE_DIR ] ; then
echo "Maven release dir already exist: $KF_MVN_RELEASE_DIR"
exit 1
fi
echo "Maven repo dir set up: $KF_MVN_RELEASE_DIR"
cp -pR $KF_MVN_REPO_DIR $KF_MVN_RELEASE_DIR
ENDSSH

    if [ $? -ne 0 ]; then
	echo "Failed to prepare maven repo"
	exit 1
    fi

    # Merge into the main maven repo, fetch, update and push back up
#    mkdir -p $MVN_LOCAL_REMOTE_DIR

#    rsync -r -a -v -e "ssh -o $SSH_OPT -i $PRIVATE_KEY" "${KF_USER}@${KF_SERVER}:${MVN_REMOTE_DIR}" $MVN_LOCAL_REMOTE_DIR

    echo "  building and uploading new maven2 release repo"
    if [ ! -d "$MVN_LOCAL_REMOTE_DIR" ] ; then
	echo "Local Maven release dir not found at: $MVN_LOCAL_REMOTE_DIR"
	exit 1
    fi

    run_gradle && \
    rsync -r -a -v -i -e "ssh -o $SSH_OPT -i $PRIVATE_KEY" $MVN_LOCAL_REMOTE_DIR/ "${KF_USER}@${KF_SERVER}:${KF_MVN_RELEASE_DIR}"

    if [ $? -ne 0 ]; then
	echo "Failed to upload maven repo"
	exit 1
    fi

    echo " updating release soft-links"
    MAJOR=`echo $1 | cut -d. -f1`
    LINK="current-kf_$MAJOR"
    ssh -n -T -o "$SSH_OPT" -i $PRIVATE_KEY -l $KF_USER $KF_SERVER <<ENDSSH 
cd $KF_RELEASES_DIR && rm $LINK && ln -s $1 $LINK && \
cd ~/$KF_REPO_DIR/maven2 && rm release && ln -s ../maven2-release/$1 release && \
cd ~/$KF_REPO_DIR && tar -zcv -C maven2-release/$1 -f maven2-archives/release/maven2-release-$1.tar.gz org
ENDSSH

    if [ $? -ne 0 ]; then
	echo "Failed to upload maven repo"
	exit 1
    fi

    echo "Release uploaded, all soft links set to point to release: $1"

fi
