package org.knopflerfish.bundle.soapobject;

import java.util.*;

import org.osgi.framework.*;


public class RemoteAgent  {

   private BundleContext context;

   public RemoteAgent(BundleContext context) {this.context = context;}

   public long[] getBundles() {
      org.osgi.framework.Bundle[] bundles = context.getBundles();
      long[] blist = new long[bundles.length];
     for (int i = 0; i < bundles.length; i++) {
       blist[i] = bundles[i].getBundleId();
     }
     return blist;
   }
   
   public String[] getServices(long bundleId) {
     Bundle bundle = context.getBundle(bundleId);
     ServiceReference[] regServices = bundle.getRegisteredServices();
     String[] slist = new String[regServices.length];
     for (int i = 0; i < slist.length; i++) {
       slist[i] = regServices[i].getProperty("service.id").toString();
     }
     return slist;	   
   }


  public String[] getBundleInfo(long bundleId) {
    Bundle b = context.getBundle(bundleId);

    String[] result = new String[] {
      "" + b.getBundleId(),
      "" + b.getLocation(),
      "" + b.getState(),
    };

    return result;
  }

  public void startBundle(long bundleId) {
    try {
      Bundle b = context.getBundle(bundleId);
      b.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void stopBundle(long bundleId)  {
    try {
      Bundle b = context.getBundle(bundleId);
      b.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void uninstallBundle(long bundleId) {
    try {
      Bundle b = context.getBundle(bundleId);
      b.uninstall();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public long installBundle(String url) {
    try {
      Bundle b = context.installBundle(url);
      
      return b.getBundleId();
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }




  public String[] getBundleHeaders(long bundleId) {
    Bundle b = context.getBundle(bundleId);

    Dictionary d = b.getHeaders();

    String[] result = new String[d.size() * 2];

    int i = 0;
    for(Enumeration e = d.keys(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      String val = (String)d.get(key);

      result[i]   = key;
      result[i+1] = val;

      i += 2;
    }

    return result;
  }
}

