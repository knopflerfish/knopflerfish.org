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

<b>NOTE!</b> Version numbers are shown as the expected version numbers
in the first official release.

Knopflerfish Framework - OSGi Core Specification
----------------------------------------------------------------------

### OSGi R6 Specifications
*  All the Core Specification Services and API:s are included and are
   designed to be compliant with the OSGi R6 specification.
   Please refer to the <a href="components.html">Contents</a> page in the
   release documentation for a more detailed description of news in R6
   and KF6 implementation status.

### Framework 8.0.0

* Updated framework to be OSGi R6 compliant.

* New property <code>org.knopflerfish.framework.resolver.implicituses</code>.
  Controls if framework should implicitly check all imports if no uses directive is
  specified. This used to be the default behaviour in previous releases.

* Fixed too strict check in <code>ServiceReference.isAssignableTo(Bundle,String)</code>.


OSGi Compendium Specification
----------------------------------------------------------------------

### Application Admin 4.1.0
*   Updated API classes to match OSGi R6.

### Blueprint 5.1.0
*   Updated API classes to match OSGi R6. 

### CM 5.1.0
*   Updated API classes to match OSGi R6. 
*   Fixed missing CM_LOCATION_UPDATE events.
*   Fixed missing security check for <code>Configuration.getBundleLocation()</code>.

### Component (SCR) 6.0.0
*   Updated API and functionality to match OSGI R6, new minor version 1.3.
*   Added dependency to new Promise bundle.

### Component Annotation 1.1.0
*   Updated API classes to match OSGi R6, new minor version 1.3.

### Coordinator API 1.1.0
*   Updated API classes to match OSGi R6. 

### DMT 5.1.0
*   Updated API classes to match OSGi R6. 

### Event 4.1.0
*   Updated API classes to match OSGi R6. 

### Http 5.2.0
*   Updated API classes to match OSGi R6. 

### IO 4.1.0
*   Updated API classes to match OSGi R6. 

### KF Metatype 5.1.0
*   Updated to match OSGi R6 API minor version 1.3.

### Measurement 4.1.0
*   Updated API classes to match OSGi R6. 

### Metatype API 5.1.0
*   Updated API classes to match OSGi R6, new minor version 1.3.

### Metatype Annotations API 1.0.0
*   New API bundle providing the
    <code>org.osgi.service.metatype.annotations</code> package.
*   This API-bundle is for compile time use, since the annotations
    are used by the build system to create the XML-file describing
    metatype information.

### Position 4.1.0
*   Updated API classes to match OSGi R6.

### Prefs 4.1.0
*   Updated API classes to match OSGi R6.

### Promise 1.0.0
*   New library bundle providing the
    <code>org.osgi.util.function.</code> and
    <code>org.osgi.util.promise</code> packages.

### Provisioning 4.1.0
*   Updated API classes to match OSGi R6.

### Remote service admin 1.1.0
*   Updated API classes to match OSGi R6, new minor version 1.1.

### Repository API 1.1.0
*   Updated API classes to match OSGi R6, new minor version 1.1.

### Repository XML 1.1.0
*   Support for new methods in OSGi R6 Repository API version 1.1.
*   Added dependency to new Promise bundle.

### Resolver API 1.1.0
*   Updated API classes to match OSGi R6.

### Service loader API 1.1.0
*   Updated API classes to match OSGi R6.

### UPnP 4.1.0
*   Updated API classes to match OSGi R6.

### WireAdmin 5.1.0
*   Updated API classes to match OSGi R6.

### XML 4.1.0
*   Updated API classes to match OSGi R6.


Knopflerfish Services
----------------------------------------------------------------------

### Desktop 6.0.0
*   Added new Promise bundle.

### Repository manager 1.3.0
*   Include new Promise bundle packages.


Misc, start scripts, build system etc 
----------------------------------------------------------------------

### Distribution build
 *  Add timestamp to bundle and SDK version numbers by default
    during distribution builds.
 *  Changed SVN references to Git.

### Travis CI
 *  Added support for CI builds using travis. Check
    <a href="http://travis-ci.org">http://travis-ci.org</a>
    for more information.

