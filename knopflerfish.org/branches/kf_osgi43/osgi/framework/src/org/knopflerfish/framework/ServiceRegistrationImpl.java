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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * Implementation of the ServiceRegistration object.
 *
 * @see org.osgi.framework.ServiceRegistration
 * @author Jan Stein
 */
public class ServiceRegistrationImpl<S> implements ServiceRegistration<S>
{
  /**
   * Bundle registering this service.
   */
  final FrameworkContext fwCtx;

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
  ServiceReferenceImpl<S> reference;

  /**
   * Service properties.
   */
  private PropertiesDictionary properties;

  /**
   * Bundles dependent on this service. Integer is used as
   * reference counter, counting number of unbalanced getService().
   */
  private Hashtable<Bundle,Integer> dependents = new Hashtable<Bundle, Integer>();

  /**
   * Object instances that factory has produced.
   */
  private HashMap<Bundle,S> serviceInstances = new HashMap<Bundle, S>();

  /**
   * Unget in progress for bundle in set.
   */
  private final HashSet<Bundle> ungetInProgress = new HashSet<Bundle>();

  /**
   * Is service available. I.e., if <code>true</code> then holders
   * of a ServiceReference for the service are allowed to get it.
   */
  private volatile boolean available;

  /**
   * Lock object for synchronous event delivery.
   */
  private final Object eventLock = new Object();

  /**
   * Avoid recursive unregistrations. I.e., if <code>true</code> then
   * unregistration of this service have started but are not yet
   * finished.
   */
  private volatile boolean unregistering = false;


  /**
   * Detect recursive service factory calls. Is set to thread executing
   * service factory code, otherwise <code>null</code>.
   */
  private Thread factoryThread = null;


  /**
   * Construct a ServiceRegistration for a registered service.
   *
   * @param b Bundle providing service.
   * @param s Service object.
   * @param props Properties describing service.
   */
  ServiceRegistrationImpl(BundleImpl b, Object s, PropertiesDictionary props) {
    fwCtx = b.fwCtx;
    bundle = b;
    service = s;
    properties = props;
    reference = new ServiceReferenceImpl<S>(this);
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
  public ServiceReference<S> getReference() {
    final ServiceReference<S> res = reference;
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
  public void setProperties(Dictionary<String,?> props) {
    synchronized (eventLock) {
      if (available) {
        Set<ServiceListenerEntry> before;
        // TBD, optimize the locking of services
        synchronized (fwCtx.services) {
          synchronized (properties) {
            // NYI! Optimize the MODIFIED_ENDMATCH code
            final Object old_rank = properties.get(Constants.SERVICE_RANKING);
            before = fwCtx.listeners.getMatchingServiceListeners(reference);
            final String[] classes = (String[])properties.get(Constants.OBJECTCLASS);
            final Long sid = (Long)properties.get(Constants.SERVICE_ID);
            properties = new PropertiesDictionary(props, classes, sid);
            final Object new_rank = properties.get(Constants.SERVICE_RANKING);
            if (old_rank != new_rank && new_rank instanceof Integer &&
                !((Integer)new_rank).equals(old_rank)) {
              fwCtx.services.updateServiceRegistrationOrder(this, classes);
            }
          }
        }
        fwCtx.perm
          .callServiceChanged(fwCtx,
                              fwCtx.listeners.getMatchingServiceListeners(reference),
                              new ServiceEvent(ServiceEvent.MODIFIED, reference),
                              before);
        if (!before.isEmpty()) {
          fwCtx.perm
            .callServiceChanged(fwCtx,
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
          fwCtx.services.removeServiceRegistration(this);
        }
      } else {
        throw new IllegalStateException("Service is unregistered");
      }
    }

    if (null!=bundle) {
      fwCtx.perm
        .callServiceChanged(fwCtx,
                            fwCtx.listeners.getMatchingServiceListeners(reference),
                            new ServiceEvent(ServiceEvent.UNREGISTERING, reference),
                            null);
    }
    synchronized (eventLock) {
      available = false;
      final Bundle [] using = getUsingBundles();
      if (using != null) {
        for (final Bundle element : using) {
          ungetService(element, false);
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
  S getService(Bundle b) {
    Integer ref;
    BundleImpl sBundle = null;
    synchronized (properties) {
      if (available) {
        ref = dependents.get(b);
        if (service instanceof ServiceFactory) {
          if (ref == null) {
            dependents.put(b, new Integer(0));
            factoryThread = Thread.currentThread();
          } else if (factoryThread != null) {
            if (factoryThread.equals(Thread.currentThread())) {
              throw new IllegalStateException("Recursive call of getService");
            }
          }
        } else {
          dependents.put(b, new Integer(ref != null ? ref.intValue() + 1 : 1));
          @SuppressWarnings("unchecked")
          final
          S res = (S) service;
          return res;
        }
        sBundle = bundle;
      } else {
        return null;
      }
    }
    S s = null;
    if (ref == null) {
      try {
        s = sBundle.fwCtx.perm.callGetService(this, b);
        if (s == null) {
          sBundle.fwCtx.listeners.frameworkWarning
            (sBundle,
             new ServiceException("ServiceFactory produced null",
                                  ServiceException.FACTORY_ERROR));
        }
      } catch (final Throwable pe) {
        sBundle.fwCtx.listeners.frameworkError
          (sBundle,
           new ServiceException("ServiceFactory throw an exception",
                                ServiceException.FACTORY_EXCEPTION, pe));
      }
      if (s != null) {
        final String[] classes = (String[])getProperty(Constants.OBJECTCLASS);
        for (final String classe : classes) {
          final String cls = classe;
          if (!sBundle.fwCtx.services.checkServiceClass(s, cls)) {
            sBundle.fwCtx.listeners.frameworkError
              (sBundle,
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
          factoryThread = null;
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
          } catch (final InterruptedException ie) { }
          if (dependents == null || !dependents.containsKey(b)) {
            // Service has been returned
            break;
          }
        }
      } else {
        factoryThread = null;
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
        sBundle.fwCtx.perm.callUngetService(this, b, s);
      } catch (final Throwable e) {
        sBundle.fwCtx.listeners.frameworkError(sBundle, e);
      }
      return null;
    } else {
      return s;
    }
  }

  Bundle[] getUsingBundles() {
    final Hashtable<Bundle, Integer> d = dependents;
    if (d != null) {
      synchronized (d) {
        int size = d.size() + ungetInProgress.size();
        if (size > 0) {
          final Bundle[] res =  new Bundle[size];
          for (final Enumeration<Bundle> e = d.keys(); e.hasMoreElements(); ) {
            res[--size] = e.nextElement();
          }
          for (final Bundle b : ungetInProgress) {
            res[--size] = b;
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
    final Hashtable<Bundle, Integer> deps = dependents;
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
   * @param checkRefCounter If true decrement reference counter and remove service
   *                        if we reach zero. If false remove service without
   *                        checking reference counter.
   * @return True if service was used, otherwise false.
   *
   */
  boolean ungetService(Bundle b, boolean checkRefCounter) {
    S serviceToRemove = null;
    Hashtable<Bundle,Integer> deps;
    BundleImpl sBundle;
    synchronized (properties) {
      if (dependents == null) {
        return false;
      }
      final Object countInteger = dependents.get(b);
      if (countInteger == null) {
        return false;
      }

      final int count = ((Integer) countInteger).intValue();
      if (checkRefCounter && count > 1) {
        dependents.put(b, new Integer(count - 1));
      } else {
        synchronized (dependents) {
          ungetInProgress.add(b);
          dependents.remove(b);
        }
        serviceToRemove = serviceInstances.remove(b);
      }
      deps = dependents;
      sBundle = bundle;
    }

    if (serviceToRemove != null) {
      if (service instanceof ServiceFactory) {
        try {
          sBundle.fwCtx.perm.callUngetService(this, b, serviceToRemove);
        } catch (final Throwable e) {
          sBundle.fwCtx.listeners.frameworkError(sBundle, e);
        }
      }
    }
    synchronized (deps) {
      ungetInProgress.remove(b);
    }
    return true;
  }

}
