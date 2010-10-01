This is a ConnectionFactory which are typically used together with the
IO-Connector. The IO-Connector provides a convenient way to open
connections to remote hosts and communicate with these using the HTTP.

This ConnectionFactory like another ConnectionFactory distributed 
with KF supports the http-scheme (see the connectors bundle). These
two connectors *should* be more or less equivalent, except for that
this ConnectionFactory can be configured to work with proxies. If your
application is to run in an environment where you do not need this 
functionality you may want to use other connector. This to avoid
an unnecessary dependency to the commons-logging bundle (which is 
found in ../commons-logging). To build and use this bundle you need to 
build and install the commons-logging bundle.

This bundle introduces a few (optional) properties:

org.knopflerfish.httpclient_connector.proxy.server=<host> 
org.knopflerfish.httpclient_connector.proxy.port=<int> 
org.knopflerfish.httpclient_connector.proxy.nonProxyHosts=<host regexp>|<host regexp>|...

When any of the above properties are not set the bundle fall backs to
use the standard ones "http.proxyHost", "http.proxyPort" and
"http.nonProxyHosts". Note that if this bundle is used on a runtime
with a String-class that does not have a matches(String)-method the
regular expressions for the non-proxy hosts must not contain any kind
of wild carding (the match will in this case be performed by checking
if the host-regexp is equal to the host part of the connection URI.


Some proxies require authenication. The following properties allow
you to set up the connector properly in such environments. (Requires 
the proxy server and port to be set)

org.knopflerfish.httpclient_connector.proxy.username=<username>
org.knopflerfish.httpclient_connector.proxy.password=<password>

org.knopflerfish.httpclient_connector.proxy.realm=<realm>   (optional)
org.knopflerfish.httpclient_connector.proxy.scheme=<scheme> (optional)

You can also set SO_TIMEOUT using
org.knopflerfish.httpclient_connector.so_timeout=<int> 
