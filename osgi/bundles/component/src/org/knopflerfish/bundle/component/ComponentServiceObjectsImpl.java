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

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map.Entry;

import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;


class ComponentServiceObjectsImpl<S>
  implements ComponentServiceObjects<S>
{
  final private ComponentContextImpl cci;
  final private ServiceReference<S> sr;
  final private ServiceObjects<S> so;
  final private IdentityHashMap<S, Integer> services = new IdentityHashMap<>();
  final private HashSet<ReferenceListener> rls = new HashSet<>();
  private S cciService = null;


  ComponentServiceObjectsImpl(ComponentContextImpl cci,
                              ServiceReference<S> sr,
                              ReferenceListener rl)
  {
    this.cci = cci;
    this.sr = sr;
    this.so = rl.ref.comp.bc.getServiceObjects(sr);
    rls.add(rl);
  }

  /**
   * Returns a service object for the {@link #getServiceReference()
   * associated} service.
   * 
   * @see org.osgi.framework.ServiceObjects#getService()
   */
  public S getService() {
    synchronized (rls) {
      if (cci.cc.getState() == ComponentConfiguration.STATE_DEACTIVE) {
        throw new IllegalStateException("Reference unbound");
      }
      if (rls.isEmpty()) {
        return null;
      }
    }
    S service = so.getService();
    synchronized (rls) {
      Integer refCnt = services.get(service);
      refCnt = refCnt != null ? refCnt + 1 : 1;
      services.put(service, refCnt);
    }
    return service;
  }

  /**
   * Releases a service object for the {@link #getServiceReference()
   * associated} service.
   *
   * @see org.osgi.framework.ServiceObjects#ungetService
   */
  public void ungetService(S service) {
    synchronized (rls) {
      if (rls.isEmpty()) {
        throw new IllegalStateException("Reference unbound");
      }
      Integer refCnt = services.get(service);
      if (refCnt != null) {
        int cnt = refCnt - 1;
        if (cnt == 0) {
          services.remove(service);
        } else {
          services.put(service, cnt);
        }
      } else {
        throw new IllegalArgumentException("Service not from this ComponentServiceObjects");
      }
    }
    so.ungetService(service);      
  }

  /**
   * Returns the {@link ServiceReference} for the service associated with this
   * {@code ServiceObjects} object.
   * 
   * @see org.osgi.framework.ServiceObjects#getServiceReference()
   */
  public ServiceReference<S> getServiceReference() {
    return sr;
  }


  private void close() {
    for (Entry<S, Integer> e : services.entrySet()) {
      for (int i = e.getValue(); i > 0; i--) {
        so.ungetService(e.getKey());
      }
    }
    services.clear();
    cciService = null;
  }


  void addReferenceListener(ReferenceListener rl)
  {
    synchronized (rls) {
      rls.add(rl);
    }
  }


  Object getCciService()
  {
    synchronized (rls) {
      if (cciService == null) {
        cciService = getService();
      }
      return cciService;
    }
  }

  void removeReferenceListener(ReferenceListener rl)
  {
    rls.remove(rl);
    if (rls.isEmpty()) {
      close();
    }
  }
}
