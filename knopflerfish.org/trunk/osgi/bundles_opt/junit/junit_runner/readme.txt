
JUnit test runner bundle
========================

This bundle is intended to run other installed junit test 
bundles in the framework. As each bundle is run, the XML 
test results are written to a file in a specified directory.

When all tests are run, the junit_runner bundle can be 
instructed to quit the framework.

The resources/junit_style.xsl XSL style sheet can be used to 
format the XML result files to HTML.


The junit_runner bundle is configured via system properties:

org.knopflerfish.junit_runner.tests

 Space-separated list of test IDs. These are the IDs that
 are used as service.pid, when registering JUnit tests into
 the framework.

 A framework filter string can be used instead of a list of service 
 IDs. To do this, prefix the test list with "filter", for example

   filter:(objectclass=junit.framework.TestSuite)

 To run all tests registered as TestSuites

 For each ID, the test is retrecived from the framework using
 the JUnitService (exported from the junit bundle), and the test
 is run. The output is written to

  <outdir>/<ID>.xml

 Note that test IDs not present in the framework will still generate
 test results, but marked as failed.

 Default is 

  "filter:(objectclass=junit.framework.TestSuite)"

 (all installed tests are run)

org.knopflerfish.junit_runner.outdir

 Output directory for XML result files.
 Default is "junit_grunt"

org.knopflerfish.junit_runner.quit

 If set to "true", quit framework when all tests are run.
 Default is "false"


Note:
  The resource directory contains som XSLT style
  sheets which may be useful for formatting XML test results to
  HTML. These files are automatically copied to the output directory.

