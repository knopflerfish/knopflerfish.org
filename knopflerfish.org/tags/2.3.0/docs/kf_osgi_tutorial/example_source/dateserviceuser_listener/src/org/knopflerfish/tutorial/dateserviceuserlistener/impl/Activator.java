package org.knopflerfish.tutorial.dateserviceuserlistener.impl; 

import java.util.Date;
import org.osgi.framework.BundleActivator; 
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants; 
import org.osgi.framework.ServiceReference; 
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;

import org.knopflerfish.tutorial.dateservice.DateService; 
import org.knopflerfish.tutorial.common.ServiceUserThread;

public class Activator implements BundleActivator { 
  public static BundleContext bc = null; 

  DateService       dateService;
  ServiceUserThread thread;

  public void start(BundleContext bc) throws Exception  { 
    Activator.bc = bc; 
    log("starting");
    
    String filter = "(objectclass=" + DateService.class.getName() + ")"; 
    bc.addServiceListener(listener, filter); 
    
    ServiceReference references[] = bc.getServiceReferences(null, filter); 
    for (int i = 0; references != null && i < references.length; i++) 
      { 
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, 
                                                 references[i])); 
      } 
  } 

  public void stop(BundleContext bc) throws Exception { 
    log("stopping");
    Activator.bc = null; 
  }
  
  private void log(String message) { 
    System.out.println(Activator.bc.getBundle().getHeaders()
                       .get(Constants.BUNDLE_NAME) 
                       + ": " + message); 
  } 

  ServiceListener listener = new ServiceListener() {      
      public void serviceChanged(ServiceEvent event) { 
        switch (event.getType()) { 
        case ServiceEvent.REGISTERED: 
          log("ServiceEvent.REGISTERED"); 
          dateService = (DateService) Activator.bc.getService(event 
                                                               .getServiceReference()); 
          startUsingService(); 
          break; 
        case ServiceEvent.MODIFIED: 
          log("ServiceEvent.MODIFIED received"); 
          stopUsingService(); 
          dateService = (DateService) Activator.bc.getService(event 
                                                          .getServiceReference()); 
          startUsingService(); 
          break; 
        case ServiceEvent.UNREGISTERING: 
          log("ServiceEvent.UNREGISTERING"); 
          stopUsingService(); 
          break; 
        } 
      } 
      private void stopUsingService() { 
        thread.stopThread(); 
        try { 
          thread.join(); 
        } catch (InterruptedException e) { 
          e.printStackTrace(); 
        } 
        dateService = null; 
      } 

      private void startUsingService() { 
        thread = new ServiceUserThread(dateService, "listener example"); 
        thread.start(); 
      } 
      
    };

  
} 

