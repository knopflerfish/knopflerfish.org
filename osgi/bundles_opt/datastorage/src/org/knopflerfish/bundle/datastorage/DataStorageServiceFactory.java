/*
 * Copyright (c) 2018, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.bundle.datastorage;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import org.knopflerfish.service.datastorage.DataStorageService;


public class DataStorageServiceFactory
  implements ServiceFactory<DataStorageService>, BundleListener
{
  
  ServiceRegistration<DataStorageService> storeReg = null;
  
  Map<Bundle, DataStorageServiceImpl> storageMap
  = new HashMap<Bundle,DataStorageServiceImpl>();

  DataStorageServiceFactory() {
  }

  public DataStorageService getService(Bundle bundle,
                                       ServiceRegistration<DataStorageService> reg)
  {
    synchronized (storageMap) {
      DataStorageServiceImpl datastorage
      = storageMap.get(bundle);

      if(datastorage == null) {
        datastorage = new DataStorageServiceImpl(bundle);
        storageMap.put(bundle, datastorage);
      }
      return datastorage;
    }
  }
  
  public void ungetService(Bundle bundle,
                           ServiceRegistration<DataStorageService> registration,
                           DataStorageService service) {
    synchronized(storageMap) {
      DataStorageServiceImpl datastorage = storageMap.get(bundle);

      if (datastorage != null) {
        storageMap.remove(bundle);
      } else {
        Activator.log.warn("No datastorage for unget from bundle #"
            +bundle.getBundleId());
      }
    }
  }
  
  // Trigger removal of preferences when a bundle is uninstalled.
  public void bundleChanged(BundleEvent event)
  {
//    if (BundleEvent.UNINSTALLED==event.getType()) {
//      synchronized(storageMap) {
//        StorageServiceImpl datastorage
//        = (StorageServiceImpl)storageMap.get(event.getBundle());
//        if(datastorage != null) {
//          datastorage.cleanup();
//        }
//      }
//    }
  }
  
  void register() {
    if(storeReg == null) {
      Activator.bc.addBundleListener(this);

      @SuppressWarnings("unchecked")
      final ServiceRegistration<DataStorageService> reg =
      (ServiceRegistration<DataStorageService>)
      Activator.bc.registerService(DataStorageService.class.getName(),
                                   this, null);
      storeReg = reg;
      Activator.log.info("DataStorageFactory registered");
    }
  }

  void unregister() {
    if(storeReg!= null) {
      Activator.bc.removeBundleListener(this);
      storeReg.unregister();
      storeReg = null;
    }
  }

}
