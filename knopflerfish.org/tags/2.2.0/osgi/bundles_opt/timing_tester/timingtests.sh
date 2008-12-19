#!/usr/bin/bash -e

ANT=${ANT:-ant}
LOGFILE=log.csv
TOPDIR=../..

function runtest() {
    VERSION=$1
    FWJARFILE=$2
    STORAGE=$3

    "${ANT}" -Dlog.file.name=${LOGFILE} \
        -Dfw.storage=${STORAGE} \
        -Dfw.version=${VERSION} \
        -Dfw.jar=${FWJARFILE} \
        run
}


function runAll() {

  for n in 1 2 3 4 5 6 7 8 9 10 1 2 3 4 5 6 7 8 9 10; do
   runtest "2.0" "${TOPDIR}/framework.jar" "file"
   runtest "2.0" "${TOPDIR}/framework.jar" "memory"
  done
}

echo >$LOGFILE \
    "date, id, runid, version, storage, module, message, time, mem, exception"
## echo >>$LOGFILE  "int, int, string, string, message, int, int, string"

runAll
