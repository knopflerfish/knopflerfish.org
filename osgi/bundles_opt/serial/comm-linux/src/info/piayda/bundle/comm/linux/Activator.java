package info.piayda.bundle.comm.linux;

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
      // Initizalize ports manually
      CommDriver cd =
        
(CommDriver)Class.forName("gnu.io.RXTXCommDriver").newInstance();
      cd.initialize();
    } catch (Exception e) {
      System.out.println("Failed to init linux serial\n" + e);
      throw e;
    }
  }

  public void stop(BundleContext bundleContext) { }

}
