/**
 ** Copyright (c) 2002 Gatespace AB. All Rights Reserved.
 **/

package org.knopflerfish.bundle.cm;

import org.osgi.service.cm.*;
import org.osgi.framework.*;

import java.util.*;

final class Update {
  final ServiceReference sr;
  final String pid;
  final String factoryPid;
  final ConfigurationDictionary configuration;
  ConfigurationDictionary processedConfiguration = null;

  public Update(ServiceReference sr, String pid, String factoryPid, ConfigurationDictionary configuration) {
    this.sr = sr;
    this.pid = pid;
    this.factoryPid = factoryPid;
    this.configuration = configuration;
  }

  public void doUpdate(PluginManager pm) throws ConfigurationException {
    if(sr == null) {
      return;
    }
    Object targetService = getTargetService();
    if(targetService == null) {
      return;
    }
    processedConfiguration = pm.callPluginsAndCreateACopy(sr, configuration);
    if(factoryPid == null) {
      update((ManagedService)targetService);
    } else {
      update((ManagedServiceFactory)targetService);
    }
  }

  private void update(ManagedService targetService) throws ConfigurationException {
    if(targetService == null) {
      return;
    }
    targetService.updated(processedConfiguration);
  }

  private void update(ManagedServiceFactory targetService) throws ConfigurationException {
    if(targetService == null) {
      return;
    }
    if(configuration == null) {
      targetService.deleted(pid);
    } else if(processedConfiguration == null) {
      if(Activator.r3TestCompliant()) {
        targetService.updated(pid, null);
      } else {
        targetService.deleted(pid);
      }
    } else {
      targetService.updated(pid, configuration);
    }
  }

  private Object getTargetService() {
    if(sr == null) {
      return null;
    } else {
      return Activator.bc.getService(sr);
    }
  }
}
