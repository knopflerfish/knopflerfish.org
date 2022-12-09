/*
 * Copyright (c) 2016-2022, KNOPFLERFISH project
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

class ComponentServiceListener
  implements ServiceListener
{
  private final BundleContext bc;
  private final Hashtable<String, HashSet<ReferenceListener>> serviceListeners = new Hashtable<>();
  private final Hashtable<ServiceEvent, List<Runnable>> afterServiceEvent = new Hashtable<>();


  public ComponentServiceListener(BundleContext bc)
  {
    this.bc = bc;
  }


  @Override
  public void serviceChanged(ServiceEvent event)
  {
    ServiceReference<?> sr = event.getServiceReference();
    for (String cn : (String [])sr.getProperty(Constants.OBJECTCLASS)) {
      Set<ReferenceListener> rls = serviceListeners.get(cn);
      if (rls != null) {
        for (ReferenceListener rl : rls) {
          rl.serviceEvent(sr, event);
        }
      }
    }
    List<Runnable> postrun = afterServiceEvent.get(event);
    if (postrun != null) {
      for (Runnable r : postrun) {
        r.run();
      }
    }
  }

  synchronized void addServiceListener(ReferenceListener rl) {
    HashSet<ReferenceListener> rls = serviceListeners.get(rl.getInterface());
    if (rls == null) {
      rls = new HashSet<>();
      rls.add(rl);
      serviceListeners.put(rl.getInterface(), rls);
      try {
        bc.addServiceListener(this, getObjectClassFilter(serviceListeners.keySet()));
      } catch (InvalidSyntaxException ise) {
        Activator.logError("Internal", ise);
      }
    } else {
      //noinspection unchecked
      rls = (HashSet<ReferenceListener>) rls.clone();
      rls.add(rl);
      serviceListeners.put(rl.getInterface(), rls);
    }
  }


  synchronized void removeServiceListener(ReferenceListener rl)
  {
    String sn = rl.getInterface();
    HashSet<ReferenceListener> rls = serviceListeners.get(sn);
    if (rls != null) {
      //noinspection unchecked
      rls = (HashSet<ReferenceListener>) rls.clone();
      rls.remove(rl);
      if (rls.isEmpty()) {
        serviceListeners.remove(sn);
        if (serviceListeners.isEmpty()) {
          bc.removeServiceListener(this);
        } else {
          try {
            bc.addServiceListener(this, getObjectClassFilter(serviceListeners.keySet()));
          } catch (InvalidSyntaxException ise) {
            Activator.logError("Internal", ise);
          }
        }
      } else {
        serviceListeners.put(sn, rls);
      }
    }
    
  }


  void addAfterServiceEvent(ServiceEvent se, Runnable r) {
    List<Runnable> ase = afterServiceEvent.computeIfAbsent(se, k -> new ArrayList<>());
    ase.add(r);
  }


  void close()
  {
    bc.removeServiceListener(this);
  }

  //
  // Private methods
  //

  private String getObjectClassFilter(Set<String> cns)
  {
    if (cns.size() == 1) {
      return "(" + Constants.OBJECTCLASS + "=" + cns.iterator().next() +")";
    } else {
      StringBuilder sb = new StringBuilder("(|");
      // TODO? Should we limit this and listen to all events?
      for (String cn : cns) {
        sb.append('(').append(Constants.OBJECTCLASS).append('=').append(cn).append(')');
      }
      sb.append(')');
      return sb.toString();
    }
  }

}
