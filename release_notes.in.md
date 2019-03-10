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

### Framework 8.0.9

* Fixed issue #51. Fixed dead-lock in weaving hooks handling.

### Framework 8.0.8

* Fixed issue #47. reference:file: bundles can not be loaded if
  org.knopflerfish.osgi.registerserviceurlhandler=false

### Framework 8.0.7

* Fixed issue #44. Avoid NoSuchElementException in bundle classloader
  when doing getResource.

### Framework 8.0.6

* Fixed issue #40. There is a small risk for a dead lock if you
  dynamically import a package and at the same time resolve a bundle
  that access the same package.


OSGi Compendium Specification
----------------------------------------------------------------------

### Component (SCR) 6.0.4

* Fixed issue #43, Removed faulty circular component errors when
  using target filters.


Knopflerfish Services
----------------------------------------------------------------------

### Crimson is no longer redistributed

* The Knopflerfish redistribution of crimson has been removed.

### Datastorage 0.1.0

* A basic datastorage service. New service

### gson-2.7.0

* The standard gson bundle. User by the datastorage bundle for storing
  json data.

### junit_runner 4.2.0

* The generated test index is styled as Knopflerfish distribution
  html pages.
* Test index show the different test runs to make it more clear which
  tests are executed when. 

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
