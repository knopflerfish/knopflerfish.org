Knopflerfish 2.0 BETA
----------------------

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
    The following optional features are not supported yet:
    * Requring bundles
    * Extensiont bundles
    * Fragment bundles
    * Permissions

  --- Life Cycle Layer ---

    Some changes are still pending.

  --- Service Layer ---

    Some changes are still pending.

  --- Framework Services ---

    Start Level, Permission Admin and URL Handler services have been 
    updated to conform to R4. The Conditional Permission Admin is not 
    yet included. Package Admin handles all things except those that 
    have to do with optional features.


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

    Mostly done. Some things pending spec clarifications and issues 
    resolution.

  --- User Admin ---

    Done.

  --- IO Connector ---

    Done.

  --- Declarative Services ---

    Done. New service. New implementation since the alpha release.

  --- Event Admin ---

    Done. New service.

  --- Service Tracker ---

    Done.


=== Knopflerfish Extras ===

  The extra tools provided by Knopflerfish (e.g. the desktop and 
  console command groups) have not yet been updated to reflect new 
  features in K4.


