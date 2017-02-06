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

### HTTP 5.2.1

* Fixed issue #10. ServletResponse.setContentType(null) now resets
  the content type to default values. This is the same behaviour as jetty.

* ServletResponse.getContentType() is changed to only appends the
  charset if it has been set explicitly. This is the correct behavior
  according to the Servlet API Documentation. 

* Fixed issue #18. The HTTP server error pages could get an incorrect
  page encoding.


Knopflerfish Services
----------------------------------------------------------------------



Misc, start scripts, build system etc 
----------------------------------------------------------------------

