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

}

