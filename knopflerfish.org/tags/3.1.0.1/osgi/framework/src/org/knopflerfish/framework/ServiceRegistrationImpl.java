/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.util.*;

import org.osgi.framework.*;


/**
 * Implementation of the ServiceRegistration object.
 *
 * @see org.osgi.framework.ServiceRegistration
 * @author Jan Stein
 */
public class ServiceRegistrationImpl implements ServiceRegistration
{
  /**
   * Bundle registering this service.
   */
  BundleImpl bundle;

  /**
   * Service or ServiceFactory object.
   */
  Object service;

  /**
   * Reference object to this service registration.
   */
  ServiceReferenceImpl reference;

  /**
   * Service properties.
   */
  PropertiesDictionary properties;

  /**
   * Bundles dependent on this service. Integer is used as
   * reference counter, counting number of unbalanced getService().
   */
  HashMap /*Bundle->Integer*/ dependents = new HashMap();

  /**
   * Object instances that factory has produced.
   */
  HashMap /*Bundle->Object*/ serviceInstances = new HashMap();

  /**
   * Is service available. I.e., if <code>true</code> then holders
   * of a ServiceReference for the serivice are allowed to get it.
   */
  volatile boolean available;

  /**
   * Lock object for synchronous event delivery.
   */
  private Object eventLock = new Object();

  /**
   * Avoid recursive unregistrations. I.e., if <code>true</code> then
   * unregistration of this service have started but are not yet
   * finished.
   */
  volatile boolean unregistering = false;


  /**
   * Construct a ServiceRegistration for a registered service.
   *
   * @param b Bundle providing service.
   * @param s Service object.
   * @param props Properties describing service.
   */
  ServiceRegistrationImpl(BundleImpl b, Object s, PropertiesDictionary props) {
    bundle = b;
    service = s;
    properties = props;
    reference = new ServiceReferenceImpl(this);
    available = true;
  }

  //
  // ServiceRegistration interface
  //

  /**
   * Returns a ServiceReference object for this registration.
   *
   * @see org.osgi.framework.ServiceRegistration#getReference
   */
  public ServiceReference getReference() {
    ServiceReference res = reference;
    if (res != null) {
      return res;
    } else {
      throw new IllegalStateException("Service is unregistered");
    }
  }


  /**
   * Update the properties associated with this service.
   *
   * @see org.osgi.framework.ServiceRegistration#setProperties
   */
  public void setProperties(Dictionary props) {
    synchronized (eventLock) {
      Set before;
      // TBD, optimize the locking of services
      synchronized (bundle.fwCtx.services) {
        synchronized (properties) {
          if (available) {
            // NYI! Optimize the MODIFIED_ENDMATCH code
            Object old_rank = properties.get(Constants.SERVICE_RANKING);
            before = bundle.fwCtx.listeners.getMatchingServiceListeners(reference);
            String[] classes = (String[])properties.get(Constants.OBJECTCLASS);
            Long sid = (Long)properties.get(Constants.SERVICE_ID);
            properties = new PropertiesDictionary(props, classes, sid);
            Object new_rank = properties.get(Constants.SERVICE_RANKING);
            if (old_rank != new_rank && new_rank instanceof Integer &&
                !((Integer)new_rank).equals(old_rank)) {
              bundle.fwCtx.services.updateServiceRegistrationOrder(this, classes);
            }
          } else {
            throw new IllegalStateException("Service is unregistered");
          }
        }
      }
      bundle.fwCtx.perm
        .callServiceChanged(bundle.fwCtx,
                            bundle.fwCtx.listeners.getMatchingServiceListeners(reference),
                            new ServiceEvent(ServiceEvent.MODIFIED, reference),
                            before);
      if (!before.isEmpty()) {
        bundle.fwCtx.perm
          .callServiceChanged(bundle.fwCtx,
                              before,
                              new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, reference),
                              null);
      }
    }
  }

  /**
   * Unregister the service.
   *
   * @see org.osgi.framework.ServiceRegistration#unregister
   */
  public void unregister() {
    if (unregistering) return; // Silently ignore redundant unregistration.
    synchronized (eventLock) {
      if (unregistering) return;
      unregistering = true;

      if (available) {
        if (null!=bundle) {
          bundle.fwCtx.services.removeServiceRegistration(this);
        }
      } else {
        throw new IllegalStateException("Service is unregistered");
      }
    }

    if (null!=bundle) {
      bundle.fwCtx.perm
        .callServiceChanged(bundle.fwCtx,
                            bundle.fwCtx.listeners.getMatchingServiceListeners(reference),
                            new ServiceEvent(ServiceEvent.UNREGISTERING, reference),
                            null);
    }
    synchronized (eventLock) {
      synchronized (properties) {
        available = false;
        if (null!=bundle)
          bundle.fwCtx.perm.callUnregister0(this);
        bundle = null;
        dependents = null;
        reference = null;
        service = null;
        serviceInstances = null;
        unregistering = false;
      }
    }
  }

  /**
   * Unget all remaining ServiceFactory service instances.
   */
  void unregister0() {
    for (Iterator i = serviceInstances.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry)i.next();
      try {
        // NYI, don't call inside lock
        ((ServiceFactory)service).ungetService((Bundle)e.getKey(),
                                               this,
                                               e.getValue());
      } catch (Throwable ue) {
        bundle.fwCtx.listeners.frameworkError(bundle, ue);
      }
    }
  }

  //
  // Framework internal
  //

  /**
   * Check if a bundle uses this service
   *
   * @param b Bundle to check
   * @return true if bundle uses this service
   */
  boolean isUsedByBundle(Bundle b) {
    Map deps = dependents;
    if (deps != null) {
      return deps.containsKey(b);
    } else {
      return false;
    }
  }

}
