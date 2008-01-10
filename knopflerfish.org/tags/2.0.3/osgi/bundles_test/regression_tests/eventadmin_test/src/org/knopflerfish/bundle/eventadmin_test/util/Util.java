package org.knopflerfish.bundle.eventadmin_test.util;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class Util{

  private static final String LOC_PROT = "internal:";

  public static Bundle installBundle(BundleContext context, String resource){
    try{
      /* get the class loader */
      //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      /* get the resource */
      //URL url=classLoader.getResource(resource);
      URL url = context.getBundle().getResource(resource);
      /* print the url */
      System.out.println("URL:" + url);
      /* install the bundle */
      return context.installBundle(LOC_PROT + resource, url.openStream());
      //Bundle bundle = context.installBundle(url.toString());
      //return bundle;

    }catch(Exception e){
      /* print the error */
      System.out.println("ERROR in Util installBundle()" + e);
      e.printStackTrace();
      return null;
    }

  }

  public static void updateBundle(BundleContext context, Bundle bundle) {
    try{
      if (bundle.getLocation().startsWith(LOC_PROT)) {
        String resource = bundle.getLocation().substring(LOC_PROT.length());
        URL url = context.getBundle().getResource(resource);
        bundle.update(url.openStream());
      } else {
        bundle.update();
      }
    }catch(Exception e){
      /* print the error */
      System.out.println("ERROR in Util installBundle()" + e);
      e.printStackTrace();
    }
  }

}
