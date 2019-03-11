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


OSGi Compendium Specification
----------------------------------------------------------------------



Knopflerfish Services
----------------------------------------------------------------------



Misc, start scripts, build system etc 
----------------------------------------------------------------------

