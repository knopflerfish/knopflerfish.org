/**
 ** Copyright (c) 2002 Gatespace AB. All Rights Reserved.
 **/

package org.knopflerfish.bundle.cm;

import org.osgi.service.cm.*;
import org.osgi.framework.*;

import java.util.*;

final class Update {
  ServiceReference sr;
  String pid;
  String factoryPid;
  ConfigurationDictionary configuration;

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
    configuration = pm.callPluginsAndCreateACopy(sr, configuration);
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
    targetService.updated(configuration);
  }

  private void update(ManagedServiceFactory targetService) throws ConfigurationException {
    if(targetService == null) {
      return;
    }
    if(configuration == null) {
      targetService.deleted(pid);
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
