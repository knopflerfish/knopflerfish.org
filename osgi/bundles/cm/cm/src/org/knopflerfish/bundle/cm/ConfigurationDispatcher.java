/**
 ** Copyright (c) 2001 Gatespace AB. All Rights Reserved.
 **/

package org.knopflerfish.bundle.cm;

import org.knopflerfish.service.log.*;

import org.osgi.service.cm.*;
import org.osgi.framework.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

/**
 ** This class is responsible for dispatching configurations
 ** to ManagedService(Factories).
 **
 ** It is also responsible for calling <code>ConfigurationPlugins</code>.
 **
 ** @author Per Gustafson
 ** @version 1.0
 **/

final class ConfigurationDispatcher {
  private final static String SERVICE_PID = "service.pid";
  private final static String FACTORY_PID = "service.factoryPid";

  /**
   ** The PluginManager to use.
   **/

  private PluginManager pm;

  /**
   ** One queue per target service.
   **/

  private Hashtable serviceReferenceToTargetService = new Hashtable();
  private Hashtable targetServiceToQueue = new Hashtable();

  /**
   ** Construct a ConfigurationDispatcher given a  
   ** ConfigurationServicesTracker.
   **
   ** @param tracker The ConfigurationServicesTracker to use.
   **/

  ConfigurationDispatcher(PluginManager pm) {
    this.pm = pm;
  }

  public UpdateQueue getQueueFor(ServiceReference sr) {
    synchronized(targetServiceToQueue) {
      Object targetService = serviceReferenceToTargetService.get(sr);
      if(targetService == null) {
        return null;
      } else {
        return (UpdateQueue)targetServiceToQueue.get(targetService);
      }
    }
  }

  public void addQueueFor(ServiceReference sr) {
    synchronized(targetServiceToQueue) {
      Object targetService = serviceReferenceToTargetService.get(sr);
      if(targetService == null) {
        targetService = Activator.bc.getService(sr);
        if(targetService == null) {
          Activator.log.error("Failed getting target service to build new queue for.");
          return;
        }
        serviceReferenceToTargetService.put(sr, targetService);
      }
      if(!targetServiceToQueue.containsKey(targetService)) {
        targetServiceToQueue.put(targetService, new UpdateQueue(pm));
      }
    }
  }

  public void removeQueueFor(ServiceReference sr) {
    synchronized(targetServiceToQueue) {
      Object targetService = serviceReferenceToTargetService.remove(sr);
      if(targetService == null) {
        Activator.log.error("Missing target service for a ServiceReference in removeQueueFor(ServiceReference)");
      } else if(!serviceReferenceToTargetService.contains(targetService)) {
        UpdateQueue uq = (UpdateQueue)targetServiceToQueue.remove(targetService);
        if(uq == null) {
          Activator.log.error("Missing UpdateQueue for a ServiceReference in removeQueueFor(ServiceReference)");
        }
      }
    }
  }

  public void dispatchUpdateFor(ServiceReference sr, String pid, String factoryPid, ConfigurationDictionary cd) {
    UpdateQueue uq = getQueueFor(sr);
    if(uq == null) {
      Activator.log.error("Missing UpdateQueue for " + factoryPid);
      return;
    }
    Update u = new Update(sr, pid, factoryPid, cd);
    
    uq.enqueue(u);
  }
}
