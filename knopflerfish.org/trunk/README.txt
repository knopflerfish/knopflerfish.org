======================================================================
	Knopflerfish Release  @VERSION@
======================================================================

Contents:
----------------------------------------
 README.txt       - this file.
 release_notes.*  - the release notes for the current release.
 changelog.html   - the subversion change log for this release.

 ant              - Ant related code and build include files.
 docs             - online documentation (html),
 docs/index.html  - Starting point for reading the documentation.
 osgi             - all OSGi related code.
 osgi/bundles     - bundles included in distribution.
 osgi/bundles_opt - some extra bundles.
 osgi/framework   - core OSGi framework.


Basic: How to start
----------------------------------------
 Prerequisites

   - JRE 1.3 (1.4 if you want security) or later, available from
     java.sun.com 

1. Step into the osgi dir
2. Start the OSGi framework by:
   > java -jar framework.jar

3. This starts the framework + a large set of bundles, including
   the desktop


Building:
----------------------------------------
 Prerequisites

   - JDK 1.3(1.4 if you want security) or later, available from java.sun.com
   - Ant 1.7 or later, available from ant.apache.org
   - BCEL, available from jakarta.apache.org/bcel
     BCEL is automtically downloaded to ant/lib during the build
     process, you may also choose to install it locally in
     $ANTHOME/lib to chare it between many build trees.
   - openssl, for Certificate Authority (CA) Management when creating
     a keystore for tests running with security enabled. For availability
     see http://www.openssl.org/related/binaries.html.

1. Step into the knopflerfish.org directory.
2. Start the build by:
   > ant all
