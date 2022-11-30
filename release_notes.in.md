Release Notes Knopflerfish $(VERSION) (OSGi R7)
======================================================================

Maintenance release of Knopflerfish 7 available from
$(BASE_URL)/$(VERSION). Released $(RELEASE_DATE).

Knopflerfish 7 is an implementation of the "OSGi Service Platform
Release 7". It contains all services specified in the "Core
Specification" and most of the non Enterprise Edition related
services specified in the "Compendium Specification".

The Release Notes include all new features & changes for
Knopflerfish $(VERSION) compared to the release of Knopflerfish
$(VERSION_PREV)

Knopflerfish Framework - OSGi Core Specification
----------------------------------------------------------------------

### Framework 9.0

* Updated API classes to match OSGi R7.
* Javadoc fixes.

### Framework 8.0.12

* Internally use StringBuilder instead of StringBuffer.
* Return empty ServiceReferenceDTO[] instead of null from Bundle.adapt() for
  stopped bundles.

### Framework 8.0.11

* Return null from WovenClass.bundleWiring() for uninstalled bundles.

### Framework 8.0.10

* Fixed issue #49. Use systemUrlStreamHandlerFactory if it was
  already initialized from Main.


OSGi Compendium Specification
----------------------------------------------------------------------

### Application Admin 7.0.0

* Updated API classes to match OSGi R7.

### Converter 7.0.0

* Added converter utility services. New in R7

### CM 7.0.0

* Updated API classes to match OSGi R7.
* Internal code cleanup.
* Javadoc fixes.

### Component Annotations 7.0.0

* Updated API classes to match OSGi R7.

### Coordinator 7.0.0

* Updated API classes to match OSGi R7.

### Deployment Admin 7.0.0

* Updated API classes to match OSGi R7.

### Event Admin Service 7.0.0

* Added org.osgi.service.event.annotations and org.osgi.service.event.propertytypes packages.
* Added EVENT_ADMIN_IMPLEMENTATION capability.
* Added osgi.service capability
* Updated API classes to match OSGi R7.
* Javadoc fixes.

### Foreign Applications 7.0.0

* Updated API classes to match OSGi R7.

### IO Connector Service 7.0.0

* Updated API classes to match OSGi R7.

### KF Metatype 7.0.0

* Updated to match OSGi R7 API minor version 1.4.
* Javadoc fixes.

### Log 7.0.0

* Updated API classes to match OSGi R7.
* Added dependency to new Push Stream bundle

### Metatype API 7.0.0

* Updated API classes to match OSGi R7.

### Metatype Annotations API 7.0.0

* Updated API classes to match OSGi R7.

### Monitor 7.0.0

* Updated API classes to match OSGi R7.

### Namespace 7.0.0

* Added osgi.unresolvable Namespace.
* Updated API classes to match OSGi R7.

### Preferences Service 7.0.0

* Updated API classes to match OSGi R7.

### Promise 7.0.0

* Updated API classes to match OSGi R7
* Javadoc fixes.

### Provisioning 7.0.0

* Updated API classes to match OSGi R7

### Push Stream 7.0.0

* New library bundle providing the `org.osgi.util.pushstream` package.
* Javadoc fixes.

### Remote Service Admin 7.0.0

* Updated API classes to match OSGi R7

### Repository 7.0.0

* Updated API classes to match OSGi R7
* Javadoc fixes.

### Repository-Commands 7.0.0

* Fixed bug in repository list command, not possible to pass repository parameter.
* Internal code cleanup.

### Wire Admin Service 7.0.0

* Updated API classes to match OSGi R7

### XML 7.0.0

* Updated API classes to match OSGi R7

### Configuration Management Service (cm) 5.2.0

* Internally use StringBuilder instead of StringBuffer.
* Internal code cleanup.

### Command Service (command) 0.3

* Internal code cleanup.

### Component (SCR) 7.0.0

* Internal code cleanup.
* Javadoc fixes.

### Component (SCR) 6.1.0

* Internally use StringBuilder instead of StringBuffer.

### Component (SCR) 6.0.7

* Improved fixed for issue #53, ConcurrentModificationException in SCR service
  listener.

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
* Internal code cleanup.

### Console 4.1.0

* Internal code cleanup and refactoring.

### Console2Command 2.1.0

* Internal code cleanup and refactoring.

### TCP-Console 5.1.0

* Internal code cleanup and refactoring.

### Device-Manager 4.1.0

* Internally use StringBuilder instead of StringBuffer.

### FW-Commands 7.0.0

* Internal code cleanup.

### FW-Commands 4.1.0

* Internally use StringBuilder instead of StringBuffer.
* Javadoc fixes.

### HTTP-Server 7.0.0

* Javadoc fixes.

### HTTP-Server 5.4.0

* Fixed issue #58, SocketListener limits transactions, not threads.

### HTTP-Server 5.3.0

* Internally use StringBuilder instead of StringBuffer.

### httpconsole 4.1.0

* Internal code cleanup and refactoring.

### JSDK 2.5.0.kf3-2

* Internally use StringBuilder instead of StringBuffer.
* Javadoc fixes.

### kXML 2.4.0.kf4-001

* Internal code cleanup.

### Log Service 6.1.0

* Internal code cleanup and refactoring.

### LogCommands 7.0.0

* Internal code cleanup

### LogCommands 5.1.0

* Internal code cleanup and refactoring.

### Repository-Manager 1.4.0

* Internally use StringBuilder instead of StringBuffer.
* Javadoc fixes.

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
* Internal code cleanup.

### util 4.2.0

* Internal code cleanup.


Knopflerfish Services
----------------------------------------------------------------------

### basicdriverlocator 7.0.0

* Internal code cleanup.

### CM-Desktop 7.0.0

* Internal code cleanup.

### CM-Desktop 5.1.0

* Internally use StringBuilder instead of StringBuffer.

### Commons-Logging 2.1.0.kf4-001

* Internally use StringBuilder instead of StringBuffer.
* Javadoc fixes.
* Fixed issue with Log instance reuse.

### comm-win32 3.1.0
* Internal code cleanup.

### Crimson is no longer redistributed

* The Knopflerfish redistribution of crimson has been removed.

### Datastorage 0.1.0

* A basic datastorage service. New service

### Desktop 7.0.0

* Updated to match OSGi R7 Log
* Internal code cleanup.
* Fixed issue with input dialog icons.
* Javadoc fixes.

### Desktop 6.1.0

* Code cleanup and refactoring.

### FW-Tray 4.1.0

* Internally use StringBuilder instead of StringBuffer.
* Internal code cleanup.

### gson-2.7.0

* The standard gson bundle. User by the datastorage bundle for storing
  json data.

### httpclient_connector 3.2.0.kf5-001

* Code cleanup and refactoring.
* Javadoc fixes.

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

### KF Resource Analyzer Extensions 7.0.0

* Internal code cleanup.

### KF Resource Analyzer Extensions 1.1.0

* Internally use StringBuilder instead of StringBuffer.

### KF-AWT-Desktop 4.1.0

* Internal code cleanup.

### KF-XML-Metatype 5.2.0

* Internally use StringBuilder instead of StringBuffer.
* Internal code cleanup.

### Repository Desktop 1.2.0

* Internally use StringBuilder instead of StringBuffer.

### Repository Desktop 7.0.0

* Internal code cleanup.


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
  
### asm 6.2

* asm has been updated to version 6.2, with support for Java 9
  
### asm 7.0

* asm has been updated to version 7.0, with support for Java 11

### Knopflerfish installer - jarunpacker

* Updated to support building with > JDK8

### xargs

* Added Push Stream bundle to default and tests xargs files

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

