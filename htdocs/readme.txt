Knopflerfish 2.0 ALPHA
----------------------

  This is a pre release of Knopflerfish 2.0 with part of the OSGi R4
  specification implemented.

  The framework will run and handles bundles with the new manifest format.
  However, everything is not implemented yet.

  Here follows a breakdown of what has been changed and what is still
  pending.


=== Framework (r4.core) ===

  --- Security Layer ---

    Signed bundles are not supported yet.

  --- Module Layer ---

    Module layer now has support for the new manifest format. It will handle
    the new version range directive and select appropriate packages. The
    following are not supported yet:

    * New package attribute matching.

    * Class filtering on export and import.

    * Multiple exports of same package.

    * Extension and fragment bundles.

    * Package linking on bundle level.

    * New bundle permission and signing of bundles.

  --- Life Cycle Layer ---

    Some changes are still pending.

  --- Service Layer ---

    Some changes are still pending.

  --- Framework Services ---

    Start Level, Permission Admin and URL Handler services have been updated
    to conform to R4. The following are not finished yet:

    * Package Admin

    * Conditional Permission Admin


=== Services (r4.cmpn) ===

  --- Log ---

    Done. New events added.

  --- HTTP ---

    Done. Minor API change.

  --- Configuration Admin ---

    Done. ConfigurationListener, ConfigurationPermission and Event Admin
    mapping added.

  --- Preferences ---

    Not yet updated.

  --- Metatype  ---

    Not yet updated.

  --- User Admin ---

    Not yet updated.

  --- IO Connector ---

    Done, but datagram is not yet supported.

  --- Declarative Services ---

    Done. New service. There is a known bug in the parsing of the XML file.
    The component tag has to look like
    <scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.0.0"...
    and not just
    <component...

  --- Event Admin ---

    Done. New service.

  --- Service Tracker ---

    Not yet updated.


=== Knopflerfish Extras ===

  The extra tools provided by Knopflerfish (e.g. the desktop and console
  command groups) have not yet been updated to reflect new features in K4.


