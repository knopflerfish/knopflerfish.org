package org.knopflerfish.bundle.bundleS_test;

import java.util.*;
import java.lang.reflect.*;
import java.security.*;
import java.io.*;
import org.knopflerfish.service.bundleS_test.*;
import org.osgi.framework.*;

public class BundS implements BundleS0, BundleS1, BundleS2, BundleS3 {
  BundleContext bc;
  String base_url;
  final static String CONSOLE_SERVICE = "org.knopflerfish.service.console.CommandGroup";
  final static String SERVICE = "org.knopflerfish.service.bundleS_test.BundleS";
  final static String SERVICE0 = "org.knopflerfish.service.bundleS_test.BundleS0";
  final static String SERVICE1 = "org.knopflerfish.service.bundleS_test.BundleS1";
  final static String SERVICE2 = "org.knopflerfish.service.bundleS_test.BundleS2";
  final static String SERVICE3 = "org.knopflerfish.service.bundleS_test.BundleS3";

  String []serviceClasses = { SERVICE0  };

  ServiceRegistration [] servregs = new ServiceRegistration [4];

  ServiceRegistration sreg;
  String LogServiceName = "org.osgi.service.log.LogService";

  public BundS (BundleContext bc) {
    this.bc = bc;
    Hashtable props = new Hashtable();
    try {
      sreg = bc.registerService(serviceClasses, this, props);
    }

    catch (AccessControlException ace) {
      System.out.println ("Exception " + ace + " in BundleS start"); 
    }
    catch (RuntimeException ru) {
      System.out.println ("Exception " + ru + " in BundleS start"); 
      ru.printStackTrace();
    }
  }

  // An entrypoint where to send a command to register/unregister 
  // a specific service (via reflection from the FrameworkTest ?)
  //
  public void controlService (String service, String operation, String rank) {
    execReg (service, operation, rank);
  }


// Common help routine 

  private boolean execReg (String serviceId, String operation, String ranking) {
    boolean status = false;
    String servicename = null;
    int offset = Integer.parseInt(serviceId);
    if ( 0 <= offset && offset <= 3) {
      if (operation.equals("register")) {
        if (servregs[offset] == null) {
          servicename = SERVICE + String.valueOf(offset);
          String []serviceDescription1 = { servicename };
          Hashtable d1 = new Hashtable();
          Integer rank = new Integer(ranking);
          d1.put(Constants.SERVICE_RANKING, rank);
          servregs[offset] = bc.registerService(serviceDescription1, this, d1);
          status = true;
          // System.out.println("BUNDLES: Registered service " + serviceId);
        }
      }
      if (operation.equals("unregister")) {
        if (servregs[offset] != null) {
          ServiceRegistration sr1 = servregs[offset];
          sr1.unregister();
          servregs[offset] = null;
          status = true;
        }
      }
   }
   return status;
 }


 /*
   Utility printout for standard response
  */

  private void printResult (boolean status, PrintWriter out, String s1) {
    if (status == true ) {
      out.println(s1 + ": PASS");
    }
    else {
      out.println(s1 + ": FAIL");
    }
  }


}
