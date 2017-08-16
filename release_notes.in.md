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

### Framework 8.0.5

* Fixed issue #23. If you change the filtering of a service listener
  by calling BundleContext.addServiceListener() when an event
  delivery is in progress, then the event calls that are queued will
  be dropped. This is true even if the event matched the filte both
  before and after the filtering change.


OSGi Compendium Specification
----------------------------------------------------------------------



Knopflerfish Services
----------------------------------------------------------------------



Misc, start scripts, build system etc 
----------------------------------------------------------------------

### Build system

* Changed download location for asm_all jar.
