#!/bin/sh

BUILD_HOME=`dirname $0`
BUILD_HOME=`cd ${BUILD_HOME};/bin/pwd`
cd ${BUILD_HOME}

TAGS=trunk

#
# Clean, update and build for the given TAG.
#
# Usage: build <TAG>
#
build() {
    TAG=$1
    TAGs=`echo ${TAG} | tr "/" "_"`

    # Append all output to the file build.log
    exec >>build_${TAGs}.log  2>&1 

    echo "= `date +"%C%y-%m-%d %H:%M:%S"` Clean ================================="

    # Remove old build result and update to the current level.
    gmake TAG=${TAG} update

    # Update the Makefile
    cp -pf ${TAGs}/tools/nightlybuild/Makefile Makefile

    echo "= `date +"%C%y-%m-%d %H:%M:%S"` Build ================================="
    # Start build (separate command to ensure that everything can be updated).
    gmake TAG=${TAG} all
}

# Build for all tags listed in TAGS
for TAG in ${TAGS}; do
    build ${TAG}
done
