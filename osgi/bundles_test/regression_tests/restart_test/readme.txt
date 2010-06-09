Tests for restarting the framework and verify the same bundles
start.

Should be used in two steps:

1. Launch with the test-restart1.xargs file to setup the test
   wait for FW shutdown

   Resulting test suite will be RestartSetupTestSuite

2. Launch again, but with test-restart2.xargs to verify the same state

   Resulting test suite will be RestartTestSuite
