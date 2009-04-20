package org.knopflerfish.tutorial.dateserviceuserbad.impl; 

import java.util.Date;
import org.osgi.framework.BundleActivator; 
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants; 
import org.osgi.framework.ServiceReference; 

import org.knopflerfish.tutorial.dateservice.DateService; 

public class Activator implements BundleActivator { 
  public static BundleContext bc = null; 

  public void start(BundleContext bc) throws Exception { 
    System.out.println(bc.getBundle().getHeaders()
                       .get(Constants.BUNDLE_NAME) + 
                       " starting..."); 
    Activator.bc = bc; 
    ServiceReference reference = 
      bc.getServiceReference(DateService.class.getName()); 

    DateService service = (DateService)bc.getService(reference); 
    System.out.println("Using DateService: formatting date: " + 
                       service.getFormattedDate(new Date())); 
    bc.ungetService(reference); 
  }

  public void stop(BundleContext bc) throws Exception { 
    System.out.println(bc.getBundle().getHeaders()
                       .get(Constants.BUNDLE_NAME) + 
                       " stopping..."); 
    Activator.bc = null; 
  }
} 

