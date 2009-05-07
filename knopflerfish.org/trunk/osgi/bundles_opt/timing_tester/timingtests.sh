#!/bin/bash -e

ANT=${ANT:-ant}
LOGFILE=log.csv
TOPDIR=../..

function runtest() {
    RUNID=$1
    FWJARFILE=$2
    STORAGE=$3

    "${ANT}" -Dlog.file.name=${LOGFILE} \
        -Dfw.storage=${STORAGE} \
        -Dfw.runid=${RUNID} \
        -Dfw.jar=${FWJARFILE} \
        run
}


function runAll() {

  for runid in 1 2; do
   runtest $runid "${TOPDIR}/framework.jar" "file"
   runtest $runid "${TOPDIR}/framework.jar" "memory"
  done
}

echo >$LOGFILE \
    "date, id, runid, storage, module, message, time, mem, exception"
## echo >>$LOGFILE  "int, int, string, string, message, int, int, string"

runAll
