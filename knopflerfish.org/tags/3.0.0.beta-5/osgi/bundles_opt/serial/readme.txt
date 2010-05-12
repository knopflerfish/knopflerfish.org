This directory contains bundles for handling serial ports using the
gnu.io or javax.comm API

OLD version used in KF 2 and earlier (deprecated)
=================================================

 serialportdevice

  Bundle that wraps serial ports in the OSGi Device API.
 
 comm-win32

  Implementation for windows platforms using Sun's COMM 2.0 release.
  Note that this bundles contains code with license from Sun, for 
  details, see
  
   comm-win32/resources/COMM2.0_license.txt

 comm-linux

  Implementation for x86 linux platforms using the RXTX library.
  Note that this bundles contains code with license from Sun AND
  is licensed under GPL. For details, see
  
   comm-linux/resources/COMM2.0_license.txt
   comm-linux/resources/rxtx-license.txt

  The RXTX source is available at

   http://users.frii.com/jarvi/rxtx/


NEW version to be used with KF 3
================================

 rxtxcomm

   RXTX java library, this contains the processor and os independent
   part. Needs a fragement bundle with the native code to function.

 rxtxcomm-linux-arm

   Fragment bundle containing the native code for a linux 2.6 system
   running on an armv4t based system.


 Note! Support for javax.comm, more supported systems and documentation
       will be added later.

 Note! These bundles contains code which is licensed under LGPL.
        For details, see, rxtxcomm/resources/OSGI-OPT/COPYING.
