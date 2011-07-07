Bundle for testing memory consumption and running time over a number
of tests. The bundle itself is not part of the regression test, and is
a stand-alone bundle.

The result is either written to STDOUT or to a file set using the 
system property "org.knopflerfish.bundle.endurance_test.output", e.g.
"-Dorg.knopflerfish.bundle.endurance_test.output=test.log".

If the property "org.knopflerfish.bundle.endurance_test.halt_after_test"
is set to "true" the framework will be terminated after the tests are
through.


