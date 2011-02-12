Build file for creating the global Maven2 repository from a set of
Knopflerfish releases.

Usage: 

env JAVA_HOME=/usr ant all install

New KF releases must be added to the all-target in the build.xml file.
For more info on targets type:

env JAVA_HOME=/usr ant -p
