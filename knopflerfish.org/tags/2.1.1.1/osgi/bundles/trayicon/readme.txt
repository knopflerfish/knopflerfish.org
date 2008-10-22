
Support for tray icons (currently windows only)

Service interface that should be registered into framework by bundles
publishing TrayIcon services.

As soon as a TrayIcon service is registered, the Tray Icon Manager
will pick up the service, read the name, icon, menu and start message, and
will try to publish the icon in a system dependent manner.

See javadocs for 

 org.knopflerfish.service.trayicon.TrayIcon

for details.

