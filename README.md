Knopflerfish 
======================================================================

Knopflerfish is a leading universal open source OSGi Service
Platform. Knopflerfish implements it's own OSGi framework as defined
by the OSGi Core Specification and a o a large set of the bundles /
services defined by OSGi Compendium  Specification. Knopflerfish also
includes various optional services such as OSGi wrappers for popular
3rd party libraries, knopflerfish specific bundles / services, and
utilities and development tools.

Knopflerfish is designed to be compliant with the OSGi Release 6
specifications. 

The Knopflerfish website has the full documentation and Knopflerfish
OSGi Service Platform SDK's available for download.
https://www.knopflerfish.org

Building Knopflerfish OSGi
------------------------------

###  Prerequisites
- JDK 8, available from Open JDK, Oracle or elsewhere.
- Ant 1.9.1 or later, available from ant.apache.org.
- openssl, to create and manipulate certificates when using
  security and the Conditional Permission Admin (CPA) service. Test
  suites for CPA can not be built and executed without openssl.
- ProGuard 4.10 or later, tested with 5.2. This is only need if you want
  to build the compact version of the framework. You need ProGuard 5
   or later if you want to build with Java 8.
- Knopflerfish can be built with JDK 11 or JDK 17, but there are some limitations. 
  Please see the release notes for more info

### Java compatibility

With the release of Knopflerfish 6.2 Java 8 is the baseline for building and running Knopflerfish. 

The Knopflerfish 6.1 releases remain as an alternative for building and running Knopflerfish on Java 6 or Java 7.

The Knopflerfish SDK can be rebuilt for other JDK versions
if preferred.
For a comprehensive explanation of running and building Knopflerfish
with different JDK versions please consult:
https://www.knopflerfish.org/osgi_java_compatibility_guide.html

### How to build

1. Clone this repository

2. Step into the osgi subdir and call ant
   ```
   > cd osgi
   > ant
   ```

This will build the framework and all essential OSGi bundles.

After a successful build a `framework.jar` will be created. 
Compiled bundles are placed in the `jars` directory.

To start the platform simply run:
```
java -jar framework.jar
```

To get more start-up options:
```
java -jar framework.jar -help
```

Please refer to the documentation for a complete description on
starting Knopflerfish and start options. 

Building the Knopflerfish OSGi SDK
----------------------------------------

A complete Knopflerfish OSGi SDK distribution can also be built. In
this case build from the root directory of the repo and call the
distrib target:
```
ant distrib
```

This target will compile all bundles, including test bundles. run the
knopflerfish test suite, generate javadoc and bundle specific
documentation, and finally create a self-extracting jar file with the
complete SDK.

Working with bndtools
----------------------------------------
Knopflerfish itself is not built with bnd or bndtools, but it is very
easy to integrate or use knopflerfish since an OSGi repository index
file is generated in the build process.

Insert the following in the .bnd file of your project. 
```
-plugin.org.knopflerfish.kf6: \
	aQute.bnd.repository.osgi.OSGiRepository; \
		locations=https://www.knopflerfish.org/releases/6.2.0/osgi/jars/index.xml; \
		name=kf6
```
and replace the location with a file URL pointing to your local
Knopflerfish repository.

Working with maven
----------------------------------------
During the distribution build a maven2 repository is built which can
be used as a local maven repository.

More information on user maven can be found here:
https://www.knopflerfish.org/maven.html


About Knopflerfish
----------------------------------------
Knopflerfish is a leading universal open source OSGi Service
Platform.
https://www.knopflerfish.org

The development is led and maintained by Makewave
https://www.makewave.com

As a complement to the freely available Knopflerfish, Makewave offers
Knopflerfish Pro, the certified and fully supported edition of
Knopflerfish. 
