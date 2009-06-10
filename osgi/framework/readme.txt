Contents
=========

  * Knopflerfish framework.jar startup
  * Initial start vs restart
  * Starting the framework
  * Default selection of .xargs
  * Framework System Properties
  * System properties fot the KF Http service
  * System properties for the KF console telnet service

Knopflerfish framework.jar startup
==================================

This is a startup guide for the KF OSGi framework. Note that
command-line startup of the framework is not specified by OSGi, and
system integrators often need to create a wrapper script for FW
startup.

The KF Main startup class is primarily intended to be used in scenarios
where current working directory is same as the one containing framework.jar,
the framework storage directory and configuration files. In these cases

 java -jar framework.jar

...is often enough. 

To enable support for new KF2 specific features, such as extension bundles, 
you can use the "kf2" shell script. Open a terminal and type

 ./kf2

Note: the script requires a "sh" shell. 

Other uses are possible, but require options and possibly some tweaking
of the default startup files.


Initial start vs restart
========================

Two cases of framework startup should be noted:

 1. Initial, bootstrap, startup

    An initial startup must contains enough options
    to install bundles allowing further management, using 
    -install options. If no bundles are installed, an empty 
    framework will be started but nothing can be done with it..

 2. Restart of previously initialized framework.

    Any OSGi framework can remember its state from previous startup.

    In this case, startup options should only contains system properties
    and a -launch option for restarting the bundles, but not any
    -install options.

 NB! Since these two different cases probably will use separate startup
     files, care must be taken so system properties a set correctly in
     both files, when so required.

It is up to a system integrator to decide when to use initial startup
or restart. The Main KF class can help somewhat in doing this (see below)
but might not be enough. In those cases, wrapper scripts, or modifications
to Main.java are recommended.


Starting the framework
======================

The framework can be started using the startup wrapper class
 
 org.knopflerfish.framework.Main

This class is also set a Main-Class in framework.jar's manifest, meaning 
framework.jar can be started using 

 java -jar framework.jar [options] OR ./kf2 [options]

The Main class supports a number of options, which can be displayed
using 

 java -jar framework.jar -help OR ./kf2 -help

Options can also be specified using the -xargs option, which specifies
a .xargs text file containing lines of new options. Typically all
options are specified in .xargs files. Combining .xargs files and
command line options is possible. .xargs files can also use recursive
.xargs files.

When the framework is started, it uses a file system directory for
storing the state of all installed bundles, "fwdir". The default
directory used for this is 

 fwdir

in the currect directory. The "fwdir" directory can also be set
specifically using the org.osgi.framework.storage property. Note that
moving "fwdir" also changes the location for searching for default
.xargs files.

If no options are specified (any "-Fx=y", "-Da=b" or "-init" does not
count as options in this case) an implicit

  -xargs "default"

is added to the options. "default" means that the default .xargs (see
below) is selected.


Default selection of .xargs
===========================

If _no_ args are supplied (arguments of the form "-Fx=y", "-Da=b" or
"-init" does not count in this case), or a name of "default" is given
as -xargs argument, a default .xargs file will be searched for, by the
following algorithm:

  1. If there exists a previous "fwdir" AND previous options
     does not contain "-init", search for a file named

       restart.xargs 

  2. If no fwdir exist, OR options contain an "-init", 
     search for a named:

      a) init_[osname].xargs
      b) init.xargs
      c) remote-init.xargs

  The search is performed in the following directories: 
  
      a) fwdir
      b) The parent directory of fwdir (if any)
      c) The current working directory


  First match winns.


  The [osname]-part of the file name is the unified OS name as
  specified in Alias.java (see below). Case is important if the file
  system is case sensitive.

    OS aliases:

     OS2
     QNX
     Windows95
     Windows98
     WindowsNT
     WindowsCE
     Windows2000
     WindowsXP


The file fwdir/restart.xargs suitable for restarting a framework
instance is written by the Knopflerfish framework on every startup
(unless disabled by setting the property
"org.knopflerfish.framework.write.restart.xargs" to false).

 

Framework System Properties
===========================

   org.osgi.framework.storage
     Where we store persistent data.

     On systems not supporting a current working directory,
     as Pocket PC, this path should be set to an explicit
     full path.

     Note: Knopflerfish 1.x and 2.x used the name
     "org.osgi.framework.dir" for this property.

     Default: {currentWorkingDirectory}/fwdir

   org.knopflerfish.gosg.jars
     Semicolon separated list of base URLs for relative install commands
     Default: file:jars/*

   org.osgi.framework.system.packages
     Complete list of packages exported by the system bundle.

     If not set the framework will export all OSGi packages and all
     standard Java packages according to the version of the running
     JRE. See also "org.knopflerfish.framework.system.export.all_*"
     and "org.osgi.framework.system.packages.extra"

   org.osgi.framework.system.packages.file
     File containing list of packages exported by the system bundle.

   org.osgi.framework.system.packages.extra
     Packages to add to the default list of packages exported by the
     system bundle.

   org.knopflerfish.framework.debug.print_with_do_privileged
     Surrond all debug print-operations originating from
     setting org.knopflerfish.debug.* properties with a
     doPrivileged() wrapper.
     Default: true

   org.knopflerfish.framework.debug.framework
     Print debug information about life-cycle events for the
     current framework instance.
     Default: false

   org.knopflerfish.framework.debug.classloader
     Print debug information from classloader
     Default: false

   org.knopflerfish.framework.debug.errors
     Print all FrameworkEvents of type ERROR
     Default: false

   org.knopflerfish.framework.debug.packages
     Print debug information about packages
     Default: false

   org.knopflerfish.framework.debug.startlevel
     Print debug information about startlevel service
     Default: false

   org.knopflerfish.framework.debug.url
     Print debug information about URL services
     Default: false

   org.knopflerfish.framework.debug.ldap
     Print debug information about LDAP filters
     Default: false

  org.knopflerfish.framework.debug.service_reference
     When security is enabled, print information about service
     reference lookups that are rejected due to missing permissions
     for calling bundle.
     Default: false

   org.knopflerfish.framework.debug.bundle_resource
     When security is enabled, print information about resource
     lookups that are rejected due to missing permissions for the
     calling bundle.

   org.knopflerfish.framework.debug.patch
     Print debug information about class patching
     Default: false

   org.knopflerfish.framework.debug.permissions
     Print debug information about permission evaluation.
     Default: false

   org.knopflerfish.framework.ldap.nocache
     Disable LDAP caching for simple filters. LDAP caching
     speeds up framework filters considerably, but uses
     more memory.

     Default: false

   org.knopflerfish.framework.bundlestorage
     Storage implementation for bundles
     [file, memory]
     Default: file

   org.knopflerfish.framework.bundlestorage.file.reference
     When using file bundle storage, file: URLs can optionally
     be referenced only, not copied to the persistant area.

     If set to true, file: URLs are referenced only.

     Note: Individual bundles can be reference installed
           by using URLs of the syntax:

              reference:file:<path>

           This works even if the global reference flag
           is not enabled.

     Default: false


   org.knopflerfish.framework.bundlestorage.file.unpack
     When using file bundle storage, bundle jars can be unpacked
     or copied as-is. Unpacking leads to faster restart and class loading
     but takes longer for initial startup.

     If set to true, unpack bundle jars.

     Default: true
      
   org.knopflerfish.framework.system.export.all_13
     Make the system class loader export all standard JRE 1.3
     packages as javax.swing.*

     Only used when "org.osgi.framework.system.packages" is not set.

     Default: false

   org.knopflerfish.framework.system.export.all_14
     Make the system class loader export all standard JRE 1.4
     packages as javax.swing.*

     Only used when "org.osgi.framework.system.packages" is not set.

     Default: false

   org.knopflerfish.framework.system.export.all_15
     Make the system class loader export all standard JRE 1.5
     packages as javax.swing.*

     Only used when "org.osgi.framework.system.packages" is not set.

     Default: false

   org.knopflerfish.framework.system.export.all_16
     Make the system class loader export all standard JRE 1.6
     packages as javax.swing.*

     Only used when "org.osgi.framework.system.packages" is not set.

     Default: false


   org.knopflerfish.framework.is_doublechecked_locking_safe
     Is it safe to use double-checked locking or not.
     It is safe if JSR 133 is included in the running JRE. I.e., for
     Java SE if version is 1.5 or higher.

     Default: true if value of the system property java.version >= 1.5,
              false otherwise


   org.knopflerfish.framework.main.verbosity
     Verbosity level of the Main class starting the framework. 0 means
     few messages.

     Default: 0

   org.knopflerfish.servicereference.valid.during.unregistering
     If set to false, then the service reference can not be used to
     fetch an instance of the service during delivery and handling of
     the UNREGISTERING service event. This (false) is the behaviour
     specified in the OSGi R4.0.1 specification, according to a
     clarification done by CPEG February 2008 it shall now be possible
     to obtain a service instance during delivery of UNREGISTERING
     events thus this property now defaults to true.
     Default: true

   org.knopflerfish.startlevel.use
     Use the Start Level service. If start level is not use then
     we do not create a non daemon thread that will keep a jvm
     with only daemon threads alive.
     Default: true

   org.knopflerfish.startlevel.level
     level of start level service if used.
     Default: 1

   org.knopflerfish.startlevel.initlevel
     Initial start level of bundles if start level
     service if used.
     Default: 1

   java.security.manager
     Class name of security manager. If set to empty string, uses
     "java.lang.SecurityManager". If unset, do not use any security
     manager.
     To use postponement features in Conditional Permission you need
     to set is to "org.knopflerfish.framework.permissions.KFSecurityManager".

     Default: unset

   java.security.policy
     Security policy file. Used by the security manager.

     Default: unset

   org.knopflerfish.framework.version.fuzzy
     If set to true, consider package version numbers
      "x.y.0" = "x.y" 
     otherwise consider
      "x.y.0" > "x.y"

     Default: true


   org.knopflerfish.framework.tck401compat
     If set to "true", make sure that all test in the OSGi R4.0.1 test 
     suite pass, even if the tests are buggy and break the spec itself.
    
     Default: false. 
     Default is a Good Thing since it means follow the spec, not the 
     buggy tests.

   org.knopflerfish.framework.restart.allow
     If set to "true", allow restart of framework by calling 
     getBundle(0).update()

     If "false", exit framework with exit code = 2. This can be useful
     when a wrapper script is better at restarting cleanly than the JVM
     itself.

     Default: true

  org.knopflerfish.osgi.setcontextclassloader
     If set to "true", set the bundle startup thread's context class
     loader to the bundle's class loader. This is useful for checking
     if an external lib will work better with a wrapped startup. It
     doesn't set the context classloader for event callbacks.

     Note that setting the context classloader is not mandated
     by OSGi, and might introduce dependecies on the KF framework,
     so this flag should only be enabled for testing purposes.
     
     Default: false

  org.knopflerfish.permissions.initialdefault
     Initial default set of permission for PermissionAdmin service.
     Format for permission is same as used by 
     org.osgi.service.permissionadmin.PermissionInfo, i.e, 

       (type)                     or
       (type "name")              or
       (type "name" "actions")

     More than on permission can be set by separating items by ";"

     An empty set can be specified by using the empty string as value.

     Default: "(java.security.AllPermission)"


  org.knopflerfish.osgi.registerserviceurlhandler
    Flag for installing OSGi service based URL handlers. 
    Since the URL handler can only be installed once, there
    might be cased where some external entity (not OSGi)
    sets this. In this case, the OSGi handler can be disabled
    by setting 

     org.knopflerfish.osgi.registerserviceurlhandler=false

    Default: true (use OSGi service handlers)


  org.knopflerfish.osgi.registerbundleurlhandler
    Flag for publicly exporting the bundle: special URL
    handler. If this is enabled, all bundles can create
    bundle: URLs for access it's own or other bundle's resources.

    The OSGi service based URL handlers (see above) _must_ 
    be active for publicly exporting bundle: URLs

    Default: false (don't export bundle: URLs publicly)

  org.knopflerfish.framework.usingwrapperscript
    If set to "true", KF will assume that it has been
    started with the "kf2" shell script, and that it will be 
    restarted if KF exits with exit code = 200. Required to be 
    able to use new KF2 features such as extension bundles. 

    This flag is set to "true" by the "kf2" shell script.

    Default: false 

  org.knopflerfish.framework.main.class.activation
   A comma-separated list of locations of bundles whose Main-Class
   (set in manifest) should be used as activator if no
   BundleActivator is specified. 

   The Main-Class will be used as activator iff the jar file 
   does not specify a Bundle-Activator header and the bundle's
   location(see Bundle.getLocation) is found in the comma-separated
   list (case-sensitive). 
   
   > java -Dorg.knopflerfish.framework.main.class.activation=\ 
         file:/foo/bar.jar,http://foo.com/bar.jar \ 
         -jar framework.jar ...
   
   Default: the empty list

  org.knopflerfish.framework.patch
   If true AND the class org.objectweb.asm.ClassReader is available
   (by putting the asm-3.1.jar library on the system class path), enable
   runtime class patching.

   Example:

    java -Dorg.knopflerfish.framework.patch=true\
         -cp framework.jar:asm-3.1.jar \
         org.knopflerfish.framework.Main        

   Default: false


  org.knopflerfish.framework.patch.configurl
   URL to class patch config file. Only used when class patching is enabled.

   This is used as a fallback if
   a bundle does not specify a Bundle-ClassPatcher-Config manifest header.


   Default: !!/patches.props

            "!!" is used to read resources from the system class path
            "!" can be used to read bundle resources.
           

  org.knopflerfish.framework.patch.dumpclasses
   If true and class patchin is enabled, dump all modified classes
   to a directory.

   Default: false
    
  org.knopflerfish.framework.patch.dumpclasses.dir
   If dumpclasses is enabled, specifies a directory where to dump
   modified classes

   Default: patchedclasses


  org.knopflerfish.framework.automanifest
   Flag to enable automatic manifest generation. If true, bundle
   manifest can be modified by a special configuration file. See
   javadoc for org.knopflerfish.framework.AutoManifest class 
   for details.

   Default: false


  org.knopflerfish.framework.automanifest.config
   Configuration URL for automatic manifest generation. Only
   valid if org.knopflerfish.framework.automanifest=true.
   An URL starting with "!!" followed by path is refer to a resource
   on the classloader that have loaded the framework.

   Default: "!!/automanifest.props"


  org.knopflerfish.framework.debug.automanifest
   Print debug output for automatic manifest actions.

   Default: false



  org.knopflerfish.framework.xargs.writesysprops
   Properties defined using -Dname=value in xargs-files are available
   for bundles using BundleContext.getProperty(name).
   This property controls weather such properties shall also be
   exported as system properties or not.
  Default: true (i.e., create a system property for each property).


  org.knopflerfish.framework.write.restart.xargs
   Property that tells the Knopflerfish framework if it shall write a
   restart.xargs file with all framework properties inside the
   framework directory on startup or not.
  Default: true (i.e., write all properties to fwdir/restart.xargs).


  org.knopflerfish.framework.strictbootclassloading
    If set to true, use strict rules for loading classes from the boot class loader.
    If false, accept class loading from the boot class path from classes themselves
    on the boot class, but which incorrectly assumes they may access all of the boot
    classes on any class loader (such as the bundle class loader).
    
    Setting this to true will, for example, result in broken serialization on the Sun 
    JVM if bootdelegation does not exposes sun.* classes

    Default: false


Using a HTTP proxy
==================

The standard JVM system properties

 http.proxyHost
 http.proxyPort
 http.nonProxyHosts 

should be used to set proxy information. This will be global to 
all HTTP request from all bundles and the framework.

Additionally, the KF-specific system property

 http.proxyAuth

can be set to a value on the form user:password

If set to non-empty, this will add the 
Proxy-Authorization header to bundle install http/https requests 
made from the framework, However, bundles using the URL class internally
must explictly set this header themselves.


System properties fot the KF Http service
=========================================

Only applicable when the Knopflerfish HTTP service is running.

 org.osgi.service.http.port       
 org.osgi.service.http.hostname
 org.knopflerfish.http.mime.props
 org.knopflerfish.http.dnslookup
 org.knopflerfish.http.response.buffer.size.default 
 org.knopflerfish.http.connection.max
 org.knopflerfish.http.connection.timeout
 org.knopflerfish.http.session.timeout.default

See 
  https://www.knopflerfish.org/svn/knopflerfish.org/trunk/osgi/bundles/http/http/readme.txt


System properties for the KF console telnet service
===================================================

Only applicable when the Knopflerfish telnet console service is running.

 org.knopflerfish.consoletelnet.user 
 org.knopflerfish.consoletelnet.pwd
 org.knopflerfish.consoletelnet.port

See
 https://www.knopflerfish.org/svn/knopflerfish.org/trunk/osgi/bundles/console/consoletelnet/readme.txt
