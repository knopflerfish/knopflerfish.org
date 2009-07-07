*******************************************
** Knopflerfish Release @VERSION@
**
** For more information, please visit
**   http://www.knopflerfish.org
**
*******************************************


--- Directories ---

 ant              - Ant related code and build include files.

 docs             - online documentation (html),
 docs/index.html  - Starting point for reading the documentation.

 osgi             - all OSGi related code.
 osgi/bundles     - bundles included in distribution.
 osgi/bundles_opt - some extra bundles.
 osgi/framework   - core OSGi framework.

 release_notes.*  - the release notes for the current release.
 changelog.html   - the subversion change log for this release.
 README.txt       - this file.


--- Running   ---

The framework can be run using

 > cd osgi
 > java -jar framework.jar



--- Building ---

 Prerequisites

   - JDK 1.3(1.4 if you want security) or later, available from java.sun.com
   - Ant 1.7 or later, available from ant.apache.org
   - BCEL, available from jakarta.apache.org/bcel
     BCEL is automtically downloaded to ant/lib during the build
     process, you may also choose to install it locally in
     $ANTHOME/lib to chare it between many build trees.

 > ant                   # builds framework and all bundle jar files
