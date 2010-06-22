#!/bin/sh

BUILD_HOME=`dirname $0`
BUILD_HOME=`cd ${BUILD_HOME};/bin/pwd`
cd ${BUILD_HOME}

TAGS="trunk"

STATUS_MAIL=devel@makewave.com

#
# Clean, update and build for the given TAG.
#
# Usage: build <TAG>
#
build() {
    TAG=$1
    TAGs=`echo ${TAG} | tr "/" "_"`

    # Name of the build log file is build_<TAG>_<DayOfWeek>.log
    LOG_FILE_NAME=build_${TAGs}_`date +"%u"`.log

    # Write all output to ${LOG_FILE_NAME}
    exec >${LOG_FILE_NAME} 2>&1 

    echo "= `date +"%C%y-%m-%d %H:%M:%S"` Clean ================================="

    # Remove old build result and update to the current level.
    make -f Makefile_${TAGs} TAG=${TAG} update

    # Update the Makefile
    cp -pf ${TAGs}/tools/nightlybuild/Makefile Makefile_${TAGs}

    echo "= `date +"%C%y-%m-%d %H:%M:%S"` Build ================================="
    # Start build (separate command to ensure that everything can be updated).
    if make -f Makefile_${TAGs} TAG=${TAG} all ; then
	ETMP=/tmp/btmp$$
	TFILE=`echo ${TAGs}/out/*/junit_grunt/index.xml`
	if [ ! -r "${TFILE}" ] ; then
	    TFILE=${TAGs}/osgi/junit_grunt/index.xml
	fi
        tail ${LOG_FILE_NAME} | grep Total > $ETMP
	if [ -r "${TFILE}" ] ; then
            awk -F\" '/runCount/ { cnt += $2 } END { print "Test cases:", cnt}'  ${TFILE} >> $ETMP
            FAILED=`awk -F\" '/failureCount/ { cnt += $2 } /errorCount/ { cnt += $2 } END { if (cnt > 0) print ", but found ", cnt, " FAILED test case(s)"}'  ${TFILE}`
	else
            FAILED=", but no test results were found"
        fi
        cat $ETMP | mail -s "KF ${TAG} build successful${FAILED}" ${STATUS_MAIL}
	rm -f $ETMP
    else
        tail ${LOG_FILE_NAME} | mail -s "KF ${TAG} build FAILED" ${STATUS_MAIL}
    fi
}

# Build for all tags listed in TAGS
for TAG in ${TAGS}; do
    build ${TAG}
done
