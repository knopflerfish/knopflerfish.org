JUnit on OSGI
=============

This directory contains support for JUnit testing on a running framework. 

 junit    - bundle exporting the junit.framework.* classes
            as well as allowing remote connection to test suites
            in a running framework via http

 examples - some small examples using the junit remote bundle

Requirements:


 - http server (used by the server part)
 - KF LogRef log wrapper from log_api (used by the server part)
 - XML parser (used by the client part)

Background
-----------

JUnit allows developers to write unit tests by writing classes 
implementing junit.framework.Test, e.g TestCase, TestSuite etc.

As long as these test do not depend on a running framework, they
are easy to test using normal tools, but when they depend on the
framework, the standard test running tools become insufficient.

Thus, the junit bundle allows remote access to test cases running
on a framework, from any normal test tool running on a developer 
machine.

How it works
------------
 
 1. Bundle developers write tests for a bundle B just as any other 
    JUnit tests. The tests simply extends TestCase or TestSuite.

 2. The bundle developer register these tests as-is into the OSGi
    framework, with a service.pid property giving the name
    of the test. This should normally be the same name as the
    test. Typically all tests from a bundle is grouped into
    a TestSuite.

    Example: register a test suite into the framework.

      TestSuite suite = new TestSuite("example1");

      suite.addTest(...);
      Hashtable props = new Hashtable();
      props.put("service.pid", suite.getName());
    
      bc.registerService(Test.class.getName(), suite, props);


 3. The junit bundle, when started, registers a servlet in the
    OSGi web server. This servlet then allows remote running of the
    registered test cases.

    The servlet is available at

      http://<host>:<port>/junit?id=<testid>

    where <testid> == value of the service.pid property exported
    in 2) 

    The only interface that registered tests need to implement
    is junit.framework.Test

    The junit bundle thus accesses the tests via BundleContext.getService()
    and needs ServicePermission, if FW security is active.

 Note how *only* step 2) is extra work compared to writing standard
 JUnit tests.



 4. When actual testing is desired, a single client test class is used
    on the development machine (which doesn't need to run the framework!)

    The client class name is:

     org.knopflerfish.service.junit.client.JUnitClient

    The target host and test id is passed to the client as a 
    system property "suite.url"

    This client test class extends TestSuite and act as a proxy to the 
    actual test, and can thus be passed to any test runner, as

     junit.swingui.TestRunner or
     junit.textui.TestRunner

   as well as Ant's "junit" task.

   Example: Using the Swing TestRunner

    > java "-Dsuite.url=http://localhost:8080/junit?id=example1" \
       junit.swingui.TestRunner \
       org.knopflerfish.service.junit.client.JUnitClient

    This will run the test with id "example1" on localhost:8080

    Note that the class path must be set fo find both junit.jar and
    the junit_all-1.0.0.jar bundle.

Note:

  The servlet is also capable of exporting the test results as plain
  HTML. In this case, the client proxy isn't needed. Just point your
  browser at

   http://<host>:<port>/junit

  and you'll get a list of available tests. From this list you can
  select suites and individual tests to run. The result will be
  presented as HTML.
 

JUnit support in bundlebuild_include.xml
----------------------------------------

As a convenience, the ant/bundlebuild_include.xml script
contains support for using the JUnit client.


Example: run the swing Test runner from Ant

   > ant -Dtest.id=example1 junit_ext

   
Example: run Ant's junit task

   > ant -Dtest.id=example1 junit_ant


Tip: Bundles can be installed using the telnet console. The telnet
console is installed by the default init.xargs. In this case a bundle
can be installed and started by

   > ant install start
     

The following Ant properties are set as default in bundlebuild_include.xml:

  http.host              localhost
  http.port              8080
  junit.runner.class     junit.swingui.TestRunner
  junit.formatter        plain
  junit.outfile          junit


Known issues
------------

 * TestSuites are "flattened" by the client proxy, so the
   tree structure in the Swing runner might look a bit different
   compared from tests run locally.

 * Proxied tests cannot (yet) be individually re-run. The entire client 
   proxy must be re-run

