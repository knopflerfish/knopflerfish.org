package org.knopflerfish.bundle.comm.w32;

import java.io.*;
import java.util.*;

import javax.comm.*;

import org.osgi.framework.*;

/**
 * We have this start method because SUNs commAPI only searches for the
 * javax.comm.properties where comm.jar is installed or in JDK/lib.
 */
public class Activator implements BundleActivator {

  public void start(BundleContext bundleContext) throws Exception {
    try {
      // Initialize ports manually
      CommDriver cd = 
	(CommDriver)Class.forName("com.sun.comm.Win32Driver").newInstance();
      cd.initialize();
      
      StringTokenizer st = 
	new StringTokenizer(System.getProperty("org.knopflerfish.serial.devices",
					       "COM1,COM2,COM3,COM4"),
			    ",");
      while (st.hasMoreTokens()) {
	String portName = st.nextToken().trim();
	if (portName.startsWith("COM")) {
	  CommPortIdentifier.addPortName(portName,
					 CommPortIdentifier.PORT_SERIAL, 
					 cd);
	}
      }
    } catch (Exception e) {
      System.out.println("Failed to init win32 serial\n" + e);
    }
  }

  public void stop(BundleContext bundleContext) { }

}
