package org.knopflerfish.bundle.soapobject;

import java.net.URL;

import java.util.*;

import org.osgi.framework.*;


public class Activator implements BundleActivator {
   private BundleContext context;
   private ServiceReference srRA = null;

   public void start(BundleContext context) {
      this.context = context;
      srRA = registerObject("remoteFW", new RemoteAgent(context));
   }

   public void stop(BundleContext context) {
      context.ungetService(srRA);
   }

   private ServiceReference registerObject(String name, Object obj) {
      Hashtable ht = new Hashtable();
      
      ht.put("SOAP.service.name", name);
      return (context.registerService(obj.getClass().getName(), obj, ht)).getReference();
   }
}
