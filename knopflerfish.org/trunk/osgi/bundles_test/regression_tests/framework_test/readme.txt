This test suite tests core framework functionality as
bundle install, start, stop, uninstall, resources etc.

Exported test suite IDs
=======================

FrameworkTestSuite      - tests core framework
PackageAdminTestSuite   - tests package admin service
NativeCodeTestSuite     - skeleton code only yet

Configuration
=============

Configuration is done using system properties.

org.knopflerfish.framework_tests.eventdelay

 Millisecond delay to wait for async events to arrive.
 Default is 500
