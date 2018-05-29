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

### Framework 8.0.6

* Fixed issue #40. There is a small risk for a dead lock if you
  dynamically import a package and at the same time resolve a bundle
  that access the same package.

OSGi Compendium Specification
----------------------------------------------------------------------

### KF Metatype 7.0.0

* Updated to match OSGi R7 API minor version 1.4.

### Metatype API 7.0.0

* Updated API classes to match OSGi R7.

### Metatype Annotations API 7.0.0

* Updated API classes to match OSGi R7.

### Promise 7.0.0

* Updated API classes to match OSGi R7

### Push Stream 7.0.0

* New library bundle providing the `org.osgi.util.pushstream` package.

### Log 7.0.0

* Updated API classes to match OSGi R7.
* Added dependency to new Push Stream bundle

### Namespace 7.0.0

* Added osgi.unresolvable Namespace.
* Updated API classes to match OSGi R7.

### Component Annotations 7.0.0

* Updated API classes to match OSGi R7.

### Event Admin Service 7.0.0

* Added org.osgi.service.event.annotations and org.osgi.service.event.propertytypes packages.
* Added EVENT_ADMIN_IMPLEMENTATION capability.
* Updated API classes to match OSGi R7.

### Preferences Service 7.0.0

* Updated API classes to match OSGi R7.

### IO Connector Service 7.0.0

* Updated API classes to match OSGi R7.

Knopflerfish Services
----------------------------------------------------------------------

### Crimson is no longer redistributed

* The Knopflerfish redistribution of crimson has been removed.

### Desktop 7.0.0

* Updated to match OSGi R7 Log

Misc, start scripts, build system etc 
----------------------------------------------------------------------

### Build with Java 9 / JDK9

* Updated the build system to support building with JDK9 with the following
  remarks / limitations:
    - Compact version of framework is not built since the proguard compactor
      does not support Java 9
      - Updated asm to version 6.0. Older versions does not support Java 9.
      - Crimson does not support Java 9 and has been removed.
  
### asm 6.0

* asm has been updated to version 6.0, with support for Java 9

### Knopflerfish installer - jarunpacker

* Updated to support building with JDK9

### xargs

* Added Push Stream bundle to default and tests xargs files

### Maven snapshot bundles / artifacts

* In a Knopflerfish snapshot release the release specific maven2 repo
  will be a snapshot repository, i.e. include snapshot versions of
  all bundles only.

### Documentation

* Corrected broken links, causing 404 Not Found error
