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

### Framework 8.0.4

* Fixed issue #15. If you had two fragment or more that imports and
  exports two packages or more between themselves and they had bundle
  ids less than the host bundle, then if you resolved the host the
  fragments would not attach properly.

### Framework 8.0.3

* Fixed issue #7. Always allow start of read-only unpacked bundles.

* Fixed issue #11. Do not try to override framework internal export
  package resources with matching files from current directory.
  To override, use framework property
  **org.knopflerfish.framework.system.packages.file**

### Framework 8.0.2

* Fixed issue #8. Could not start on Java 9 because of new version
  schema.

* Set default value for framework property
  **org.knopflerfish.framework.is_doublechecked_locking_safe**
  to **TRUE** for all instances since we don't run on Java 1.4
  anymore.

### Framework 8.0.1

* Fixed issue #12. Could cause NPE if there were concurrent dynamic
  imports by different bundles being resolved.


OSGi Compendium Specification
----------------------------------------------------------------------

### Component (SCR) 6.0.3

* Fixed issue #17, NPE in Component.newComponentConfiguration when
  multiple component instances are bound to the same service and have
  the same target propertymultiple Declarative Services component
  instances are not created for multiple factory configurations.

* Fixed NPE, that occured if you inspected a component with CM config
  that had been disabled.

* Fixed problem with components not getting factory configuration
  data present before activation.
 
* Fixed problem with component deactivation method not getting
  factory configuration properties if multiple factory configuration
  instances were registered.
 
### Component (SCR) 6.0.2

* Fixed issue #16, multiple Declarative Services component instances
  are not created for multiple factory configurations.

### Component (SCR) 6.0.1

* Fixed issue #14, StackOverflowError when using JAXRS publisher.
  Removed a faulty re-register of service during deactivation of
  a delayed component configuration.

* Fixed missing reference DTOs for
  ServiceComponentRuntime.getComponentConfigurationDTOs.


Knopflerfish Services
----------------------------------------------------------------------



Misc, start scripts, build system etc 
----------------------------------------------------------------------

