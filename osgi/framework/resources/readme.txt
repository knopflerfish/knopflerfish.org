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

 java -jar framework.jar [options]

The Main class supports a number of options, which can be displayed
using 

 java -jar framework.jar -help

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