Design Notes on OSGi R7 / Knopflerfish 7
======================================================================

Build system, JDK versions and bundle versions
--------------------------------------------------
* KF 7 will require JDK8 or higher. 
* Use of java.* packages in bundles will be made visible as
  Import-Package statments in the manifest instead of using Execution
  Environment.
* The bundle major version will be changed to 7 for all bundles
* Build and test will be made with JDK11 as well.
* Compiling / running with JDK9 and JDK10 should also work, but will not be tested and supported.


General Changes
--------------------------------------------------
* Require-Capability osgi.ee has been removed


Status
--------------------------------------------------
* All OSGi API-classes are now updated to final OSGi R7 versions.
* Stubs have been created for all new interfaces / classes where needed so that everything compiles.
* There are still several pieces missing for full R7 compliance


Reminder to ourselves
--------------------------------------------------
* Change copyright year on all files you modify
* Always update release notes when updating a bundle

