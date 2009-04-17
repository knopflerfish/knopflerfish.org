#!/usr/bin/bash -e

JAVA=/c/j2sdk1.4.2_03/bin/java
LOGFILE=log.csv
TOPDIR=../..

function runtest() {
    prefix=$1
    jarfile=$2
    storage=$3

     $JAVA \
	"-Dtest.log.prefix=$prefix" \
        -Dtest.log.file=$LOGFILE \
	-Dorg.knopflerfish.framework.bundlestorage=$storage \
	-Dfwdir.delete=true \
	-cp $TOPDIR/jars/tester/tester-1.0.0.jar\;$TOPDIR/$jarfile \
	org.knopflerfish.test.framework.TestFW \
	-xargs test.xargs
  
}


function runAll() {

  for n in 1 2 3 4 5 6 7 8 9 10 1 2 3 4 5 6 7 8 9 10; do
   runtest "1.3_file, 1.3,  file"    "framework.jar" "file"
#   runtest "1.3_mem, 1.0, memory"    "framework.jar" "memory"
   runtest "1.0_file, 1.0, file"     "framework-1.0.2.jar" "file"
#   runtest "1.0_mem, 1.0, memory"    "framework-1.0.2.jar" "memory"
  done
}

echo >$LOGFILE \
    "date, id, runid, version, storage, module, message, time, mem, exception"
## echo >>$LOGFILE  "int, int, string, string, message, int, int, string"

runAll
