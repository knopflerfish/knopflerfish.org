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

This is a startup guide for the KF OSGi framework. Note that startup of the 
framework is not specified by OSGi, and system integrators often need to
create a wrapper script for FW startup.

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

 java -jar framework.jar [options] OR ./kf [options]

The Main class supports a number of options, which can be displayed
using 

 java -jar framework.jar -help OR ./kf -help

Options can also be specified using the -xargs option, which specifies
a .xargs text file containing lines of new options. Typically all options
are specified in .xargs files. Combining .xargs files and command line
options is possible but not recommended. .xargs files can also use recursive
.xargs files.

When the framework is started, it uses a file system directory for
storing the state of all installed bundles, "fwdir". The default
directory used for this is 

 fwdir

in the currect directory. The "fwdir" directory can also be set specifically
using the org.osgi.framework.dir system property. Note that moving "fwdir"
also changes the location for searching for default .xargs files.

If no options are specified, or a single "-init" option is present,
an implicit

  -xargs "default"

is added to the options. "default" means that the default .xargs (see
below) is selected.


Default selection of .xargs
===========================

If _no_ args are supplied, or a name of "default" is given as -xargs
argument, a default .xargs file will be searched for, by the following 
algorithm:

  1. If there exists a previous "fwdir" AND previous options
     does not contain "-init", use

       restart.xargs 

     ...in the same dir as "fwdir".

  2. If no fwdir exist, OR options contain an "-init", 
     try the first file matching:

      a) init_[osname].xargs
      b) init.xargs
      c) remote-init.xargs

    ...in the same dir as "fwdir"

    [osname] is the unified OS name as specified in Alias.java
    (see below). Case is important if the file system
    is case sensitive.

    OS aliases:

     OS2
     QNX
     Windows95
     Windows98
     WindowsNT
     WindowsCE
     Windows2000
     WindowsXP


Framework System Properties
===========================

   org.osgi.framework.dir
     Where we store persistent data.

     On systems not supporting a current working directory,
     as Pocket PC, this path should be set to an explicit
     full path.

     Default: {defaultInstDir}/fwdir

   org.knopflerfish.gosg.jars
     Base URL for relative install commands
     Default: file:jars/*

   org.osgi.framework.system.packages
     List of packages exported from system classloader,
     other than java.* and org.osgi.framework

   org.osgi.framework.system.packages.file
     File containing list of packages exported from system
     classloader,
     other than java.* and org.osgi.framework

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
     Make system classloader export all standard JVM 1.3
     packages as javax.swing.*
     Default: false

   org.knopflerfish.framework.system.export.all_14
     Make system classloader export all standard JVM 1.4
     packages as javax.swing.*
     Default: false

   org.knopflerfish.framework.system.export.all_15
     Make system classloader export all standard JVM 1.5
     packages as javax.swing.*
     Default: false

   org.knopflerfish.verbosity
     Framework verbosity level. 0 means few messages
     Default: 0

   org.knopflerfish.startlevel.use
     Use the Start Level service.
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


   org.knopflerfish.osgi.r3.testcompliant
     If set to "true", make sure that all test in the OSGi R3 test 
     suite pass, even if the tests are buggy and break the spec itself.

     This affects some very special (due to bugs in the test) handling 
     of filters and conflict between the spec concerning CM and the actual
     tests. Bundles knowning abouth these conflicts should check the 
     testcompliant flag and act appropiately.
    
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

   org.knopflerfish.framework.exitonshutdown
     If set to "true", call System.exit() when framework shutdown 
     is complete.

     If "false", don't do anything after shutdown.

     Must be set to "true" if one wants to use KF2 features such as
     extension bundles.

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
