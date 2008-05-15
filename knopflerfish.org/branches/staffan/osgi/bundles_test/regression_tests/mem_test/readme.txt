Bundle for testing long-term memory usage. 

This is not a JUnit test, rather a standalone bundle which will
log memory usage at regular intervals, then stop (using System.exit())
after total time.
 
Configuration is done by system properties:

org.knopflerfish.bundle.test.memtest.logfile
 
 File name to save data in. The file will be a comma-separated
 text file on the format

   time, totalmem, freemem, usedmem

 ...where time is in milliseconds, memory in bytes

 Default: memtest.log

org.knopflerfish.bundle.test.memtest.interval=10

 Seconds between measurents

 Default: 10


org.knopflerfish.bundle.test.memtest.totaltime

 Total run time in minutes.

 Default: 1