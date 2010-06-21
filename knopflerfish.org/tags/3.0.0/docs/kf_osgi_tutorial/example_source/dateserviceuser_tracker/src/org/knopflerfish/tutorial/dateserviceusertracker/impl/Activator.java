package org.knopflerfish.tutorial.dateserviceusertracker.impl; 

import java.util.Date;
import org.osgi.framework.BundleActivator; 
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants; 
import org.osgi.framework.ServiceReference; 
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.knopflerfish.tutorial.dateservice.DateService; 
import org.knopflerfish.tutorial.common.ServiceUserThread;

public class Activator implements BundleActivator { 
  public static BundleContext bc = null; 

  ServiceTracker    tracker;
  ServiceUserThread thread;

  public void start(BundleContext bc) throws Exception { 
    Activator.bc = bc; 
    log("starting");
    tracker =  new ServiceTracker(bc,
                                  DateService.class.getName(), 
                                  customizer);
    tracker.open(); 
  } 

  ServiceTrackerCustomizer customizer = new ServiceTrackerCustomizer() {
      public Object addingService(ServiceReference reference) { 
        log("addingService");
        DateService service = (DateService) bc.getService(reference); 
        if (thread == null) { 
          thread = new ServiceUserThread(service, "tracker example"); 
          thread.start(); 
          return service; 
        } else {
          return service; 
        } 
      }

      public void modifiedService(ServiceReference reference, Object 
                                  serviceObject) { 
        thread.stopThread(); 
        try { 
          thread.join(); 
        } catch (InterruptedException e) { 
          e.printStackTrace(); 
        } 
        DateService service = (DateService) bc.getService(reference); 
        thread = new ServiceUserThread(service, "tracker example"); 
        thread.start(); 
      } 
      public void removedService(ServiceReference reference, Object 
                                 serviceObject) {
        log("removedService");
        thread.stopThread(); 
        try { 
          thread.join(); 
        } catch (InterruptedException e) { 
          e.printStackTrace(); 
        } 
        thread = null; 
      } 
    };
  
  
  public void stop(BundleContext bc) throws Exception { 
    log("stopping");
    tracker.close();
    Activator.bc = null; 
  }
  
  private void log(String message) { 
    System.out.println(Activator.bc.getBundle().getHeaders()
                       .get(Constants.BUNDLE_NAME) 
                       + ": " + message); 
  } 
  
} 

