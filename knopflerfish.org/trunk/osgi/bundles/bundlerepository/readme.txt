Bundlerepository
=================

This bundle implements a BundleRepositoryService, allowing 
easy access to bundles store remotely. The main code in this 
bundle is taken directly from the Oscar project

  http://oscar-osgi.sourceforge.net/

and is copyright Richard S Hall and Oscar.

OBR is a bundle repository format introduced by the Oscar OSGi project. 
The format consists of an XML file describing all available bundles and 
an OSGi OBR bundle which should be installed on an OSGi framework. 
When started, this OBR bundle can read the XML file, list bundles, 
and install bundles and their dependencies. 

The KF repository URL is 

  http://www.knopflerfish.org/repo/repository.xml 

This URL is used as default repository URL *when the KF framework* is 
used, otherwise it uses default built-in to the Oscar code.

The repo URL can be set explicitly with the system property

 oscar.repository.url=<url>

In addition to the BundleRepositoryService, this bundle also registers
 
 - a KF console command group "obr" 
 - a KF desktop plugin. 
 - when having access to the Oscar console, it also registers 
   an Oscar shell plugin.

Additionally, this version has support for http proxy authentication, by
using the system property

 http.proxyAuth=<user>:<pwd>

(as of August 25, this property is also read by framework.jar, since
framework support is needed for actual bundle installation)


