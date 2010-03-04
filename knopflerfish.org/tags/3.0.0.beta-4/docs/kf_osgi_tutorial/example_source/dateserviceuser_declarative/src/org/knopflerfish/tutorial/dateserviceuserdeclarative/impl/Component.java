package org.knopflerfish.tutorial.dateserviceuserdeclarative.impl; 

import org.knopflerfish.tutorial.dateservice.DateService; 
import org.knopflerfish.tutorial.common.ServiceUserThread;

public class Component {
  DateService       dateService;
  ServiceUserThread thread;

  /**
   * Called by the Declarative Service component finds
   * a registered DateService as specified in the component.xml
   */
  protected void setDateService(DateService dateService) {
    log("setDateService");
    this.dateService = dateService;

    if(thread == null) {
      thread = new ServiceUserThread(dateService, "declarative example"); 
      thread.start(); 
    }
  }
  
  /**
   * Called by the Declarative Service component notices an
   * unregistered DateService as specified in the component.xml
   */
  protected void unsetDateService(DateService dateService) { 
    log("unsetDateService");
    this.dateService = null;

    if(thread != null) {
      thread.stopThread(); 
      try { 
        thread.join(); 
      } catch (InterruptedException e) { 
        e.printStackTrace(); 
      }
      thread = null;
    }
  }
  
  private void log(String message) { 
    System.out.println("dateservice component: " + message); 
  } 
  
} 

