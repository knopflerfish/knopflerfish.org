Bundledexify is a tool for converting normal bundle files to
a bundle with the class files replaced by Android dex files.

To re-compile the bundledexify jar you need to set the
ANDROID_HOME property when invoking ant.

% ant -DANDROID_HOME=/.../Android/sdk

The tool is normally invoked by the ant build system. But
it can be invoked from the command line with:

% java -cp bundledexify.jar:$ANDROID_HOME/build-tools/22.0.1/lib/dx.jar org.knopflerfish.tools.bundledexify.Main
