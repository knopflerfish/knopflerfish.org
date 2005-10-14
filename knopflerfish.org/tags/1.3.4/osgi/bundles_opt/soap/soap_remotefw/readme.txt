This bundle exports (server) and imports (client) remote 
OSGI frameworks, using SOAP.

This allows a client bundle, as the Swing desktop, to remotely 
control another framework.

The bundle is part of the The Knopflerfish Axis port, see
 
 ../readme.html

for details

Server - Exporting a framework
==============================

The soap_remotefw bundle can export the entire OSGi framework as a 
SOAP service, by registering an instance of 

 org.knopflerfish.service.soap.remotefw.RemoteFW

into the framework, with the property

 SOAP.service.name = OSGiFramework

This will make the axis-osgi bundle pick up the service and export
all methods. A SOAP client on another host can then access the
server framework.

Typically, this "raw" SOAP service will be available on

 http://host:port/axis/service/OSGiFramework

...and can be viewed directly with the soap_desktop browser.

The default behavior of soap_remotefw is to register a RemoteFW 
instance.


Client - Importing a framework
==============================

The soap_remotefw bundle can also run as a client and import a remote
framework. In that case, a service of class

  org.knopflerfish.service.remotefw.RemoteFramework

will be registered into the framework. Another bundle can then get this
RemoteFramework service and call its "connect" method. This methods will
return a BundleContext representing the remote frmaework. See javadocs
for RemoteFramework for details.

The default behavior of soap_remotefw is to register a RemoteFramework 
instance.

The Swing desktop is capable of using the RemoteFramework service. If the
soap_remotefw is active, the desktop will display a connection dialog
allowing the user to connect to a named remote framework.


Controlling the soap_remotefw behavoir
======================================

The behavior of the soap_remotefw bundle can be controlled
by the following system properties:

org.knopflerfish.soap.remotefw.server

 (default "true")
 
 If set to "true", export a SOAP service allowing remote management
 of the entire platform

 If not set to "true", do not export any SOAP service


org.knopflerfish.soap.remotefw.client

 (default "true")

 If set to "true", register an implementation of 
 org.knopflerfish.service.remotefw.RemoteFramework
 allowing other bundles to remote manage a framework. This 
 service can be used by the desktop bundle.

 If not set to "true", do not register any RemoteFramework
 service


org.knopflerfish.soap.remotefw.client.eventinterval
 
 (default 3000)

 Interval in milliseconds between event notification. The remote
 bundle context's event notification works by polling the server
 at regular intervals. This property specifies the interval.



