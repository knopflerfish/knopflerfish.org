This is a ConnectionFactory which are typically used together with the
IO-Connector. What it does is to provide a convenient way to open
connections to remote hosts and communicate with these using the HTTP.

This ConnectionFactory like another ConnectionFactory distributed 
with KF supports the http-scheme (see the connectors bundle). These
two connectors *should* be more or less equivalent, except for that
this ConnectionFactory can be configured to work with proxies. If your
application is to run in an environment where you do not need this 
functionality you may want to use other connector. This to avoid
unnecessary dependices. 

This bundle introduces three properties:

org.knopflerfish.httpclient.proxy.server=<host> 
org.knopflerfish.httpclient.proxy.port=<int> 
org.knopflerfish.httpclient.so_timeout=<int> 




