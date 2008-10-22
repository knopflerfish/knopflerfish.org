INTRODUCTION
============
This bundle implements an HTTP service as specified by the OSGi Service
Plaftform, R2, R3.


CONFIGURATION:
==============

The HTTP bundle can be configured by both CM or System properties

As soon as the http bundle gets a valid configuration it creates and
registers an HttpService instance into the framework.

Note: If the server fails to bind to a port, an HttpService will still
be registered, but the service property "port" will not be present!


System properties
=======================

The following system properties will be read when no CM is available

 org.knopflerfish.http.enabled
 org.knopflerfish.http.secure.enabled
 org.osgi.service.http.port       
 org.osgi.service.http.port.secure       
 org.osgi.service.http.hostname
 org.knopflerfish.http.mime.props
 org.knopflerfish.http.dnslookup
 org.knopflerfish.http.response.buffer.size.default
 org.knopflerfish.http.connection.max
 org.knopflerfish.http.connection.timeout
 org.knopflerfish.http.session.timeout.default
 org.knopflerfish.http.encoding.default

See CM description below for descriptions


Using the Configuration Manager
===============================

The http bundle accepts Factory configurations on the PID

  org.knopflerfish.bundle.http.factory.HttpServer

..with the following properties:

http.enabled (Integer)
  If true, the bundle will start to listen on the http port.
  The default is true.

https.enabled (Integer)
  If true, the bundle will start to listen on the https port.
  NOTE: This functionality requires that the bundle is able to 
  obtain a "SslServerSocketFactory" service instance from the 
  framework.  
  The default is true.
  
port.http (Integer)
  The port number to listen for http requests.
  The default port is 80.

port.https (Integer)
  The port number to listen for https requests.
  The default port is 443.

host (String)
  This string property decides the default hostname for the server
  instance. If the server is running on a multihomed machine this
  property will be used to decide which network interface the server will
  listen to. If this property is not set the server will listen to all network
  interfaces. The default is to listen to all network interfaces.

mime.map (Vector of String[2])
  This property is a vector of arrays defining MIME type mappings. Each
  entry in the vector is an array with two elements where the first is
  the file name extension and the second is the associated MIME type.
  By default the most common file types are defined.


session.timeout.default (Integer)
  This integer property decides the default timeout in seconds for an
  HTTP session. The default is 1200 seconds.

connection.timeout (Integer)
  This integer property decides the timeout in seconds for a persistent
  connection to the HTTP server. The default is 30 seconds.

connection.max (Integer)
  This integer property decides the maximum number of concurrent
  connections to the HTTP server. The default is 50.

dns.lookup (Boolean)
  This boolean property decides if the server will use DNS lookup when a
  servlet calls the HttpServletRequest.getRemoteHost method. In some
  environments DNS lookup will cause the current transaction to hang for
  a long period of time. The default is to do DNS lookup.

response.buffer.size.default (Integer)
  This integer property decides the default buffer size in bytes for an
  HTTP response. If a servlet or publisher does not exceed this buffer,
  the server will calculate and send the content length header in the
  response. If the buffer is exceeded the servlet or publisher need to
  set the content length header explicitly. The content length header is
  required for persistent connections. If the content length is unknown
  the server will send a connection close header. The buffer size can be
  set runtime by the servlet using the HttpResponse.setBufferSize()
  method. The default is 16384 bytes.

org.knopflerfish.http.encoding.default (String)
  This string will be used as default character encoding for
  PrintWriter streams, if no encoding is set in using 
  ServletResponse.setContentType().

  The default encoding is ISO-8859-1


RELEASE NOTES:
==============
[te 20040822] - added HTTPS support to existing bundle. 
    Whenever applicable this bundle will now use an external 
    SslServerSocketFactory service to listen on the HTTPS (secure) port.
  - removed non-standard dependencies to the KF's log, user admin 
    and others. 
[te 20050228] 
  - in HttpConfig fixed minor property bug and removed unnecessary 
    ManagedService interface
  - added support for ClientAuthentication.
    To do that it is required for the SSLServerSocketFactory service 
    was created with a TrustManagers which contain valid client certs
    and the HttpService has to have the use client authentication prop
    enabled and it must end up using this SSLServerSocketFactory 
    service to create the SSL part.
    (NOTE: currently the Browser will notice that the socket 
    establishment failed in the SSL Handshaking due to a bad certificate.
    An alternative would be to allow the socket first to be established
    and then return HttpServletResponse.SC_FORBIDDEN.) 
