======================================================================
	Knopflerfish Release  @VERSION@
======================================================================

Contents:
----------------------------------------
 README.txt       - this file.
 release_notes.*  - the release notes for the current release.
 changelog.html   - the subversion change log for this release.
 NOTICE.txt       - notice file on included third party software.
 LICENSE.txt      - the Knopflerfish license.

 ant              - Ant related code and build include files.
 docs             - online documentation (html),
 docs/index.html  - Starting point for reading the documentation.
 osgi             - all OSGi related code.
 osgi/bundles     - bundles included in distribution.
 osgi/bundles_opt - some extra bundles.
 osgi/framework   - core OSGi framework.
 osgi/jars        - Precompiled bundles.
 osgi/test_jars   - Precompiled JUnit test suite bundles.


Basic: How to start
----------------------------------------
 Prerequisites

   - JRE 1.4 or later, available from Oracle.

1. Step into the osgi dir
2. Start the OSGi framework by:
   > java -jar framework.jar

3. This starts the framework + a large set of bundles, including
   the desktop


Building:
----------------------------------------
 Prerequisites

   - JDK 1.6 (or 1.5), available from Oracle.
     See http://java.com/en/download/faq/java_6.xml for details.
     Note that JDK 1.7 or later can NOT be used to build Knopflerfish,
     but Knopflerfish will run on them.
   - Ant 1.9.1 or later, available from ant.apache.org.
   - openssl, to create and manipulate certificates when using
     security and the Conditional Permission Admin (CPA) service. Test
     suites for CPA can not be built and executed without openssl.
   - ProGuard 4.10 or later, tested with 5.2. This is only need if you want
     to build the compact version of the framework. You need ProGuard 5
     or later if you want to build with Java8.

1. Step into the osgi dir
2. Start the build by:
   > ant all
