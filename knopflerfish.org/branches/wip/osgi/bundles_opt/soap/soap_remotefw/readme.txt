This bundle exports (server) and imports (client) remote 
framework, using SOAP.


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



