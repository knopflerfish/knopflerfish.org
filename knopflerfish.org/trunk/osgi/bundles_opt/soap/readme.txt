The Knopflerfish Axis port
==========================

The software found in the subdirectories to this directory are:

axis-osgi
---------
This is an OSGi bundlification of a standard Axis server. The standard Axis
server is designed to be running in a servlet version 2.3 environment, while
the OSGi HTTP service is based on a servlet 2.1 environment. It should be noted
that no modifications have been made to any of the Axis classes, so all code
specific to the OSGi port is made as pure "extensions". In addition to the
bundlification of the Axis server the axis-osgi bundle offers the ability to
export services, registered on the OSGi framework service registry, as SOAP
services. It should be noted that for this to work the exported objects must
not expose any data types not supported by SOAP. In order to export a service 
object as a SOAP service, the only thing needed is to set a property named
"SOAP_SERVICE_NAME" on the registered service. The property value should be
of type java.lang.String and its value will be used as the name of the
exposed SOAP service.

The inital SOAP configuration of the Axis server is given by the file
"/axis/server-config.wsdd". In case you want to add some services or for
some other reason modify the initial configuration, this is where to do
it.

The code to make all this work consist of just a few quite small classes (not
counting all Axis classes) so it is quite easy to get an overview of the
porting effort. However it took some time experimenting and studying the Axis
code to make this work, so some of the code may be harder to understand.

Note that the axis-osgi bundle depends on the commons-logging bundle to
be installed an started. Furter if your JRE does not support an XML parser
you need to provide that as well (e.g Xerces).

The axis-osgi.jar file is quite large, due to the contained axis.jar (and
other contained jars). Some of the Axis classes may be left out, e.g. client
related classes, in case you will not run any Axis clients on your OSGi
framework, WSDL related classes in case you give up the ability for the server
to generate WSDL for the SOAP services. These kind of optimizations have not
been performed, but is probably possible to perform in case it becomes critical
to reduce the required memory footprint.

soapobject
----------
This is an example of a server side SOAP object.


The soapobject bundle installs a service object into the service registry and
sets its SOAP_SERVICE_NAME property to "remoteFW". This will make the axis-osgi
bundle (must be run prior to start of the soapobject bundle) create an Axis SOAP
service named "remoteFW". Use the following URLs from a Web browser to explore
the service.

   http://localhost:8080/axis/services

   http://localhost:8080/axis/services?list

   http://localhost:8080/axis/services/remoteFW?method=getBundles

   http://localhost:8080/axis/services/remoteFW?wsdl

Beware of caching made by the browser, ensure that you are not viewing cached content
by performing explicit refresh requests.



soapclient
----------
This is an example of a client side program. The client calls the "remoteFW" service
methods, so you need to have the soapobject bundle insatlled and started. The build.xml
performs all required steps for the creation and and execution of the client program.
The only handcoded module is the Test.java (main class of the client program). The build
file will automatically fetch the WSDL from the running OSGi framework, generate the
client side stubs and support classes, compile the generated classes as well as Test.java.
When all have been built (compiled) the client program is executed and the results are
displayed.