Design Notes on OSGi R7 / Knopflerfish 7
======================================================================

Build system, JDK versions and bundle versions
--------------------------------------------------
* KF 7 will require JDK8 or higher. 
* Use of java.* packages in bundles will be made visible as
  Import-Package statments in the manifest instead of using Execution
  Environment.
* The bundle major version will be changed to 7 for all bundles
* Build and test will be made with JDK9 and JDK10 as well.

General Changes
--------------------------------------------------
* Require-Capability osgi.ee has been removed


Reminder
--------------------------------------------------
* Change copyright year on all files you modify
* Alwasy update release notes when updating a bundle

