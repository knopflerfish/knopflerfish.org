/*
 * Copyright (c) 2003-2012, KNOPFLERFISH project
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
  private PropertiesDictionary properties;

  /**
   * Bundles dependent on this service. Integer is used as
   * reference counter, counting number of unbalanced getService().
   */
  private Hashtable /*Bundle->Integer*/ dependents = new Hashtable();

  /**
   * Object instances that factory has produced.
   */
  private HashMap /*Bundle->Object*/ serviceInstances = new HashMap();

  /**
   * Unget in progress for bundle in set.
   */
  private HashSet /*Bundle*/ ungetInProgress = new HashSet();

  /**
   * Is service available. I.e., if <code>true</code> then holders
   * of a ServiceReference for the serivice are allowed to get it.
   */
  private volatile boolean available;

  /**
   * Lock object for synchronous event delivery.
   */
  private Object eventLock = new Object();

  /**
   * Avoid recursive unregistrations. I.e., if <code>true</code> then
   * unregistration of this service have started but are not yet
   * finished.
   */
  private volatile boolean unregistering = false;


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
      if (available) {
        Set before;
        // TBD, optimize the locking of services
        synchronized (bundle.fwCtx.services) {
          synchronized (properties) {
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
      } else {
        throw new IllegalStateException("Service is unregistered");
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
      available = false;
      Bundle [] using = getUsingBundles();
      if (using != null) {
        for (int i = 0; i < using.length; i++) {
          ungetService(using[i], false);
        }
      }
      synchronized (properties) {
        bundle = null;
        dependents = null;
        reference = null;
        service = null;
        serviceInstances = null;
        unregistering = false;
      }
    }
  }

  //
  // Framework internal
  //

  /**
   * Get all properties
   */
  PropertiesDictionary getProperties() {
    return properties;
  }


  /**
   * Get specified property
   */
  Object getProperty(String key) {
    return properties.get(key);
  }


  /**
   * Get the service object.
   *
   * @param bundle requester of service.
   * @return Service requested or null in case of failure.
   */
  Object getService(Bundle b) {
    Integer ref;
    synchronized (properties) {
      if (available) {
        ref = (Integer)dependents.get(b);
        if (service instanceof ServiceFactory) {
          if (ref == null) {
            dependents.put(b, new Integer(0));
          }
        } else {
          dependents.put(b, new Integer(ref != null ? ref.intValue() + 1 : 1));
          return service;
        }
      } else {
        return null;
      }
    }
    Object s = null;
    if (ref == null) {
      try {
        s = bundle.fwCtx.perm.callGetService(this, b);
        if (s == null) {
          bundle.fwCtx.listeners.frameworkError
            (bundle,
             new ServiceException("ServiceFactory produced null",
                                  ServiceException.FACTORY_ERROR));
        }
      } catch (Throwable pe) {
        bundle.fwCtx.listeners.frameworkError
          (bundle,
           new ServiceException("ServiceFactory throw an exception",
                                ServiceException.FACTORY_EXCEPTION, pe));
      }
      if (s != null) {
        String[] classes = (String[])getProperty(Constants.OBJECTCLASS);
        for (int i = 0; i < classes.length; i++) {
          final String cls = classes[i];
          if (!bundle.fwCtx.services.checkServiceClass(s, cls)) {
            bundle.fwCtx.listeners.frameworkError
              (bundle,
               new ServiceException("ServiceFactory produced an object " +
                                    "that did not implement: " + cls,
                                    ServiceException.FACTORY_ERROR));
            s = null;
            break;
          }
        }
      }
      if (s == null) {
        synchronized (properties) {
          if (dependents != null) {
            dependents.remove(b);
          }
          return null;
        }
      }
    }

    boolean recall = false;
    synchronized (properties) {
      if (s == null) {
        // Wait for service factory
        while (true) {
          s = serviceInstances.get(b);
          if (s != null) {
            break;
          }
          try {
            properties.wait(500);
          } catch (InterruptedException ie) { }
          if (dependents == null || !dependents.containsKey(b)) {
            // Service has been returned
            break;
          }
        }
      } else {
        serviceInstances.put(b, s);
        properties.notifyAll();
      }
      if (s != null) {
        ref = dependents != null ? (Integer)dependents.get(b) : null;
        if (ref != null) {
          dependents.put(b, new Integer(ref.intValue() + 1));
        } else {
          // ungetService has been called during factory call.
          // Recall service
          recall = true;
        }
      }
    }
    if (recall) {
      try {
        bundle.fwCtx.perm.callUngetService(this, b, s);
      } catch (Throwable e) {
        bundle.fwCtx.listeners.frameworkError(bundle, e);
      }
      return null;
    } else {
      return s;
    }
  }

  Bundle[] getUsingBundles() {
    Hashtable d = dependents;
    if (d != null) {
      synchronized (d) {
        int size = d.size() + ungetInProgress.size();
        if (size > 0) {
          Bundle[] res =  new Bundle[size];
          for (Enumeration e = d.keys(); e.hasMoreElements(); ) {
            res[--size] = (Bundle)e.nextElement();
          }
          for (Iterator i = ungetInProgress.iterator(); i.hasNext(); ) {
            res[--size] = (Bundle)i.next();
          }
          return res;
        }
      }
    }
    return null;
  }

  /**
   * Check if a bundle uses this service
   *
   * @param b Bundle to check
   * @return true if bundle uses this service
   */
  boolean isUsedByBundle(Bundle b) {
    Hashtable deps = dependents;
    if (deps != null) {
      return deps.containsKey(b);
    } else {
      return false;
    }
  }

  /**
   * Unget the service object.
   *
   * @param b Bundle who wants remove service.
   * @param checkRefCounter If true decrement refence counter and remove service
   *                        if we reach zero. If false remove service without
   *                        checking refence counter.
   * @return True if service was used, otherwise false.
   *          
   */
  boolean ungetService(Bundle b, boolean checkRefCounter) {
    Object serviceToRemove = null;
    synchronized (properties) {
      if (dependents == null) {
        return false;
      }
      Object countInteger = dependents.get(b);
      if (countInteger == null) {
        return false;
      }
  
      int count = ((Integer) countInteger).intValue();
      if (checkRefCounter && count > 1) {
        dependents.put(b, new Integer(count - 1));
      } else {
        synchronized (dependents) {
          ungetInProgress.add(b);
          dependents.remove(b);
        }
        serviceToRemove = serviceInstances.remove(b);
      }
    }

    if (serviceToRemove != null) {
      if (service instanceof ServiceFactory) {
        try {
          bundle.fwCtx.perm.callUngetService(this, b, serviceToRemove);
        } catch (Throwable e) {
          bundle.fwCtx.listeners.frameworkError(bundle, e);
        }
      }
    }
    synchronized (dependents) {
      ungetInProgress.remove(b);
    }
    return true;
  }

}
