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

### Framework 8.0.7

* Fixed issue #44. Avoid NoSuchElementException in bundle classloader
  when doing getResource.

### Framework 8.0.6

* Fixed issue #40. There is a small risk for a dead lock if you
  dynamically import a package and at the same time resolve a bundle
  that access the same package.


OSGi Compendium Specification
----------------------------------------------------------------------



Knopflerfish Services
----------------------------------------------------------------------



Misc, start scripts, build system etc 
----------------------------------------------------------------------

### Documentation

* Corrected broken links, causing 404 Not Found error
