Knopflerfish 2.0 BETA 3
-----------------------

  This is a pre release of Knopflerfish 2.0 with part of the OSGi R4
  specification implemented.

  Some of the optional features are not yet implemented and there are 
  some known bugs.

  Here follows a breakdown of what has been changed and what is still
  pending.


=== Framework (r4.core) ===

  --- Security Layer ---

    Signed bundles are not supported yet.

  --- Module Layer ---

    Module layer now has support for all the required features in R4. 
    The following optional feature is not supported yet:
    * Permissions
    
    To use the extension bundle support you need to use a wrapper script
    that will restart framework when needed. An example shell-script is
    included (osgi/kf2).

  --- Life Cycle Layer ---

    The following method is not implemented yet:
    * Bundle.findEntries(String, String, boolean)

  --- Service Layer ---

    Done.

  --- Framework Services ---

    Start Level, Package Admin, Permission Admin and URL Handler services have been 
    updated to conform to R4. The Conditional Permission Admin is not 
    yet included.


=== Services (r4.cmpn) ===

  --- Log ---

    Done.

  --- HTTP ---

    Done.

  --- Configuration Admin ---

    Done.

  --- Preferences ---

    Done.

  --- Metatype  ---

    Done.

  --- User Admin ---

    Done.

  --- IO Connector ---

    Done.

  --- Declarative Services ---

    Done. New service. New implementation since the alpha release.

  --- Event Admin ---

    Done. New service.

  --- Service Tracker ---

    Done. Updated to version 1.3.1.


=== Knopflerfish Extras ===

  The desktop and framework commands have been updated in a number of 
  ways to reflect new features in K4. Notable changes are listed below.
  
  --- Framework Commands ---
  
    New commands: closure, resolve, findbundles.
    The output of the bundles command has been changed (fragments and 
    hosts are indicated, last modified is listed in verbose output, it 
    is possible to sort on last modified).
    
  --- Desktop ---
  
    Fragments, hosts and required bundles are listed on the Closure tab.
    Symbolic name and last modified information is displayed on the 
    Manifest tab.


