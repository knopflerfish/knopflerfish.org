/*
 * Copyright (c) 2006, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

public class SCR implements BundleListener {

  private BundleContext bc;

  private Hashtable bundleConfigs = new Hashtable();


  public SCR(BundleContext bc) {
    this.bc = bc;
    Bundle[] bundles = bc.getBundles();
    bc.addBundleListener(this);
    for(int i=0;i<bundles.length;i++){
      bundleChanged(new BundleEvent(BundleEvent.STARTED, bundles[i]));
    }
  }

  public void shutdown() {
    for (Iterator iter = bundleConfigs.keySet().iterator(); iter.hasNext();) {
      bundleChanged(new BundleEvent(BundleEvent.STOPPED, (Bundle) iter.next()));
    }
  }

  public void bundleChanged(BundleEvent event) {
    Bundle bundle = event.getBundle();
    String manifestEntry = (String) bundle.getHeaders().get(ComponentConstants.SERVICE_COMPONENT);
    if (manifestEntry == null) {
      return;
    }

    switch (event.getType()) {
    case BundleEvent.STARTED:
      // Create components
      Collection addedConfigs = new ArrayList();
      String[] manifestEntries = manifestEntry.split(",");
      for (int i = 0; i < manifestEntries.length; i++) {
        URL resourceURL = bundle.getResource(manifestEntries[i]);
        if (resourceURL == null) {
          Activator.log.error("Resource not found:" + manifestEntries[i]);
          continue;
        }
        try {
          addedConfigs.addAll(Parser.readXML(bundle, resourceURL));
        } catch (Throwable e) {
          Activator.log.error("Failed to parse " + resourceURL);
        }
      }
      bundleConfigs.put(bundle, addedConfigs);
      // TODO: anything else needed?
      break;
    case BundleEvent.STOPPED:
      // Kill components
      Collection removedConfigs = (Collection) bundleConfigs.remove(bundle);
      if (removedConfigs != null) {
        for (Iterator iter = removedConfigs.iterator(); iter.hasNext();) {
          Config config = (Config) iter.next();
          // TODO: kill config
        }
      }
      break;
    }
  }

}

