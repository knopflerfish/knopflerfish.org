
INTRODUCTION
============
The sslj2sp bundle registers one or several SslServerSocketFactory objects as OSGi services.
These services can be used by other bundles to establish secure TCP connections.
For example, to implement HTTPS, look at the documentation of your HTTP Service bundle to see 
if it will support the use of such services.


JSSE
====
This bundle relies on the presence of Sun's "Java(TM) Secure Socket Extensions (JSSE)"; 
more specifically, jsse.jar must be available on the system classpath.
This is always the case when using Sun's Java 2 SDK, Standard Edition, v1.4+. http://java.sun.com/j2se/1.4.2/docs/guide/security/jsse/JSSERefGuide.html. 
To create your customized Ssl certificate, see section "Creating a Keystore to Use with JSSE". 
NOTE: This material is owned by Sun Microsystems, please refer to their terms
and conditions. 

You can use the Configuration Manager to tell the sslj2sp bundle what SslServerSocketFactory 
service(s) to create, see section Configuration Manager. If nothing is specified, a default 
configuration will be used.


Default Configuration
=====================

 item                   CM property		default value
 ---------------------------------------------------------------------------------------
 ssl protocol:		[none]			TLSv1

 keystore type: 	[none]			JKS	
 keystore password:	keystorepass		[internal]	
 keystore:		keystore		[internal]

 keymanager type:	[none]			SunX509	 


Using the Configuration Manager (CM)
====================================

Items that are listed and in the section "Default Configuration" and 
are not associated with a CM property are considered static in the 
current release.


The bundle accepts Factory configurations on the PID

  org.knopflerfish.bundle.ssl.j2sp


Each of the CM configurations should contain the following properties:

keystore:
  This property represents a keystore, which must be created as described in section "JSSE".
  The sslj2sp will interpret the value for this property as follows:
  - assume that the keystore has been stored to the CM as an array of bytes (byte[]).
    //TODO: To accomplish this, it would be very convenient to have a tool that could read a file
    and then store it as the value of a property in the CM.
  - assume that the value is the name of the keystore file on the local file system.
  
  If none of these assumptions lead to a valid key manager, the bundle will log a warning 
  and use the default.

keystorepass:
  This is a plain text string of the password for the store.


RELEASE NOTES:
=============
[te 20040822] : First version available in knopflerfish trunk.  

