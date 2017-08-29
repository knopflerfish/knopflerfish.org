README mvnrepo tools
======================================================================

This directory contain various tools and utilities for creating
and managing KF mvn repos


mvn test builds
----------------------------------------------------------------------
The directory test-builds contains mvn test builds. It can be used
to test the maven2 repo included in a release, or test the release
repository on knopflerfish.org


ant build.xml
----------------------------------------------------------------------

*The ant build.xml file was used to build the mvnrepo before the move
 to github. It is no longer used.*

Build file for creating the global Maven2 repository from a set o
Knopflerfish releases.

Usage: 

env JAVA_HOME=/usr ant all install

New KF releases must be added to the all-target in the build.xml file.
For more info on targets type:

env JAVA_HOME=/usr ant -p
