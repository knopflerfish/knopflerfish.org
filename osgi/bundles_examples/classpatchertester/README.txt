This is a simple command group bundle for testing the classpatcher bundle.
It registers a command group called patchtest.

The classes have been put in the com.makewave.bundle.classpatchertester package
because the classpatcher ignores classes within any org.knopflerfish.* or
org.osgi.* package. This filter is configured in the classpatcher patches.props
file.
