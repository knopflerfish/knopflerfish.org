The HTTP bundle can be configured by both CM or System properties

As soon as the http bundle gets a valid configuration it creates and
registers an HttpService instance into the framework. 

Note: If the server fails to bind to a port, an HttpService will still
be registered, but the service property "port" will not be present!


Using System properties
=======================

org.osgi.service.http.port       
  server port number

org.osgi.service.http.hostname
  server interface name

org.knopflerfish.http.mime.props
  URL to mime map

org.knopflerfish.http.dnslookup
  if true, do DNS lookup

org.knopflerfish.http.response.buffer.size.default 
 response buffer size in bytes

org.knopflerfish.http.connection.max
 max number of connections

org.knopflerfish.http.connection.timeout
 connection timeout

org.knopflerfish.http.session.timeout.default
 session timeout


Using the Configuration Manager
===============================

The http bundle accepts Factory configurations on the PID

  org.knopflerfish.bundle.http.factory.HttpServer

..with the following properties:

Property name                Description                      Type (default)

port                         server port number               Integer (80)
host                         server interface name            String ("")
mime.map                     mime type map                    Vector (String, String)
session.timeout.default      connection timeout (ms)          Integer (30)
connection.max               Number of connections            Integer (50)
dns.lookup                   Do DNS lookup                    Boolean (false)
response.buffer.size.default Size of response buffer (bytes)  Integer (16384)

