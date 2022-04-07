Release Notes Knopflerfish $(VERSION) (OSGi R6)
======================================================================

Maintenance release of Knopflerfish 6 available from
$(BASE_URL)/$(VERSION). Released $(RELEASE_DATE).

Knopflerfish 6 is an implementation of the "OSGi Service Platform
Release 6". It contains all services specified in the "Core
Specification" and most of the non Enterprise Edition related
services specified in the "Compendium Specification".

The Release Notes include all new features & changes for
Knopflerfish $(VERSION) compared to the release of Knopflerfish
$(VERSION_PREV)

Knopflerfish Framework - OSGi Core Specification
----------------------------------------------------------------------

### Framework 8.0.12

* Internally use StringBuilder instead of StringBuffer.

### Framework 8.0.11

* Return null from WovenClass.bundleWiring() for uninstalled bundles.

### Framework 8.0.10

* Fixed issue #49. Use systemUrlStreamHandlerFactory if it was
  already initialized from Main.


OSGi Compendium Specification
----------------------------------------------------------------------

### Configuration Management Service (cm) 5.2.0

* Internally use StringBuilder instead of StringBuffer.

### Command Service (command) 0.3

* Internal code cleanup.

### Component (SCR) 6.1.0

* Internally use StringBuilder instead of StringBuffer.

### Component (SCR) 6.0.6

* Fixed issue #53, Fix ConcurrentModificationException in SCR service
  listener.

### Component (SCR) 6.0.5

* Fixed issue #46, Handle updated service ranking on active services.

### Component (SCR) 6.0.4

* Fixed issue #43, Removed faulty circular component errors when
  using target filters.

### Connection-Factories 3.1.0

* Internally use StringBuilder instead of StringBuffer.

### Console 4.1.0

* Internal code cleanup and refactoring.

### Console2Command 2.1.0

* Internal code cleanup and refactoring.

### TCP-Console 5.1.0

* Internal code cleanup and refactoring.

### Device-Manager 4.1.0

* Internally use StringBuilder instead of StringBuffer.

### FW-Commands 4.1.0

* Internally use StringBuilder instead of StringBuffer.

### HTTP-Server 5.3.0

* Internally use StringBuilder instead of StringBuffer.

### httpconsole 4.1.0

* Internal code cleanup and refactoring.

### JSDK 2.6.0.kf3-2

* Internally use StringBuilder instead of StringBuffer.

### kXML 2.4.0.kf4-001

* Internal code cleanup.

### Log Service 6.1.0

* Internal code cleanup and refactoring.

### LogCommands 5.1.0

* Internal code cleanup and refactoring.

### Repository-Manager 1.4.0

* Internally use StringBuilder instead of StringBuffer.

### repository xml 1.2.0

* Internally use StringBuilder instead of StringBuffer.

### ScrCommands 4.1.0

* Internal code cleanup.

### Telnet-Console 4.1.0

* Javadoc fixes. Internal code cleanup and refactoring.

### TTY-Console 4.1.0

* Internal code cleanup and refactoring.

### UserAdmin 4.2.0

* Internally use StringBuilder instead of StringBuffer.

### util 4.2.0

* Internal code cleanup.


Knopflerfish Services
----------------------------------------------------------------------

### CM-Desktop 5.1.0

* Internally use StringBuilder instead of StringBuffer.

### Commons-Logging 2.1.0.kf4-001

* Internally use StringBuilder instead of StringBuffer.

### Crimson is no longer redistributed

* The Knopflerfish redistribution of crimson has been removed.

### Datastorage 0.1.0

* A basic datastorage service. New service

### Desktop 6.1.0

* Code cleanup and refactoring.

### FW-Tray 4.1.0

* Internally use StringBuilder instead of StringBuffer.

### gson-2.7.0

* The standard gson bundle. User by the datastorage bundle for storing
  json data.

### httpclient_connector 3.2.0.kf5-001

* Code cleanup and refactoring.

### Jini-Driver 0.2.0

* Internally use StringBuilder instead of StringBuffer.

### JUnit 3.9.0.kf4-003

* Internally use StringBuilder instead of StringBuffer.

### junit_runner 4.3.0

* Internally use StringBuilder instead of StringBuffer.

### junit_runner 4.2.0

* The generated test index is styled as Knopflerfish distribution
  html pages.
* Test index show the different test runs to make it more clear which
  tests are executed when. 

### KF Resource Analyzer Extensions 1.1.0

* Internally use StringBuilder instead of StringBuffer.

### KF-AWT-Desktop 4.1.0

* Internal code cleanup.

### KF-XML-Metatype 5.2.0

* Internally use StringBuilder instead of StringBuffer.

### Repository Desktop 1.2.0

* Internally use StringBuilder instead of StringBuffer.


Test bundles
----------------------------------------------------------------------

### component_test 1.7.0

* Internally use StringBuilder instead of StringBuffer.

### framework_test 1.1.0

* Internal code cleanup.

### permissionadmin_test 1.1.0

* Internal code cleanup.

### registeryperformance_test 1.1.0

* Internally use StringBuilder instead of StringBuffer.

### timing_tester 2.1.0

* Internally use StringBuilder instead of StringBuffer.


Misc, start scripts, build system etc 
----------------------------------------------------------------------

### Build with Java 11 / JDK11

* Updated the build system to support building with JDK11 with the following
  remarks / limitations:
    - Compact version of framework is not built since the proguard compactor
      does not support Java > 8
      - Updated asm to version 7.0. Older versions does not support
        Java 10 or 11
      - Crimson does not support Java 8 and beyond and has been
        removed.
* Building with JDK9 and JDK10 should work, but is not supported since
  they are  both declared as no longer supported. 
  
### asm 7.0

* asm has been updated to version 7.0, with support for Java 11

### Knopflerfish installer - jarunpacker

* Updated to support building with > JDK8

### Maven snapshot bundles / artifacts

* In a Knopflerfish snapshot release the release specific maven2 repo
  will be a snapshot repository, i.e. include snapshot versions of
  all bundles only.

### bundles_ext and jars_ext

* New bundle directory where 3rd party bundles are included in a KF
  distribution as-is, i.e. no recompilation. Such bundle jars are
  placed in osgi/jars_ext. The gosg.jars property has been updated to
  also search this directory.

### Documentation

* Corrected broken links, causing 404 Not Found error

