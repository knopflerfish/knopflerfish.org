This is a simple performance test for the KF framework.

It consists of three parts:

 1) A combined bundle and framework wrapper

 2) A shell shripp for running multiple runs
    of the wrapper.

 3) A .xargs startup file for the test runs



To run a test, compile the bundle

 > ant

then run the tests with bash

 > bash ./timingtests.sh


timingtests.sh will delete and create a new log file

 log.csv   (comma-separated text file)

The number of runs can be set editing timingtests.sh



