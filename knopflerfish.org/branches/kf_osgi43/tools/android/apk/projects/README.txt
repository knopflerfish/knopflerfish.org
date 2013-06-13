To build a project, install ADT and use its 'android' command to create
the file local.properties:

$ cd <project>
$ android update project -p .

KfServiceLib is an Android Library project that contains an Android
service that starts the Knopflerfish framework and install/starts the
bundles from the assets/jars folder of the calling Android application.
The Lib project should be a dependency for App projects.

KfBasicApp is an Android Application project that builds a small set of
bundles to its .apk assets/jars folder. It contains classes for
starting/stopping the service from KfServiceLib when the user pushes
buttons in a simple Android view. This project can be used as a template
for creating new Knopflerfish Android instances (defining bundles to
install/start and including customized Android views).

To build a debug .apk that includes the KfBasicApp application and the
KfServiceLib library, enter the KfBasicApp application directory and
do "ant debug".

To change the set of bundles that will be included, add/delete/modify
file lists in the "custom_rules.xml" Ant file.

When compiling Knopflerfish bundles for the Android Application
projects, APIs are loaded from the standard Knopflerfish 'jars'
directory. Thus it is necessary to do a top level build of
knopflerfish.org before building Android Application projects.

