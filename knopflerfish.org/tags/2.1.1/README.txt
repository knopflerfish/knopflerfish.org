*******************************************
** Knopflerfish Release @VERSION@
**
** For more information, please visit 
**   http://www.knopflerfish.org
**
*******************************************


--- Building ---

 Prerequisites

   - JDK 1.3 or later, available from java.sun.com 
   - Ant 1.6 or later, available from ant.apache.org 
   - BCEL, available from jakarta.apache.org/bcel 
     install locally in ant/lib or in $ANTHOME/lib

 > ant                   # builds framework and all bundle jar files

--- Running   ---

After building, the framework can be run using

 > cd osgi
 > java -jar framework.jar
               

--- Directories ---

 osgi             - all OSGi related code
 osgi/framework   - core OSGi framework
 osgi/bundles     - bundles included in distribution
 osgi/bundles_opt - some extra bundles

 ant              - Ant related code and build include files

 htdocs           - online documentation (html)

