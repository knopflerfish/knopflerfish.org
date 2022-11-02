/*
 * Copyright (c) 2003-2016, KNOPFLERFISH project
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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;


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
   * Service, ServiceFactory or ServiceFactory object.
   */
  Object service;

  /**
   * Service scope
   */
  final String scope;

  /**
   * Reference object to this service registration.
   */
  ServiceReferenceImpl<S> reference;

  /**
   * Service properties.
   */
  private PropertiesDictionary properties;

  /**
   * Bundles dependent on this service. An Integer is used as
   * reference counter, counting number of unbalanced getService().
   */
  private Hashtable<Bundle,Integer> dependents = new Hashtable<Bundle, Integer>();

  /**
   * Object instances that factory has produced.
   */
  private HashMap<Bundle,S> serviceInstances = null;

  /**
   * Object instances that factory has produced.
   */
  private HashMap<Bundle,List<S>> prototypeServiceInstances = null;

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
  ServiceRegistrationImpl(BundleImpl b, Object s, String ss, PropertiesDictionary props) {
    fwCtx = b.fwCtx;
    bundle = b;
    service = s;
    properties = props;
    reference = new ServiceReferenceImpl<S>(this);
    available = true;
    scope = ss;
    if (scope == Constants.SCOPE_BUNDLE) {
      serviceInstances = new HashMap<Bundle, S>();
    } else if (scope == Constants.SCOPE_PROTOTYPE) {
      serviceInstances = new HashMap<Bundle, S>();
      prototypeServiceInstances = new HashMap<Bundle, List<S>>();
    }
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
        // TODO, optimize the locking of services
        synchronized (fwCtx.services) {
          synchronized (properties) {
            // NYI! Optimize the MODIFIED_ENDMATCH code
            final Object old_rank = properties.get(Constants.SERVICE_RANKING);
            before = fwCtx.listeners.getMatchingServiceListeners(reference);
            final String[] classes = (String[])properties.get(Constants.OBJECTCLASS);
            final Long sid = (Long)properties.get(Constants.SERVICE_ID);
            properties = new PropertiesDictionary(props, classes, sid, new Long(bundle.id), scope);
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
          ungetService(element, false, null);
        }
      }
      synchronized (properties) {
        bundle = null;
        dependents = null;
        reference = null;
        service = null;
        serviceInstances = null;
        prototypeServiceInstances = null;
        unregistering = false;
      }
    }
  }

  //
  // Framework internal
  //

  /**
   * Is service available
   */
  boolean isAvailable() {
    return available;
  }


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
   * @param b requester of service.
   * @return Service requested or null in case of failure.
   */
  S getService(Bundle b, boolean multiple) {
    Integer ref;
    BundleImpl sBundle = null;

    if (multiple && scope != Constants.SCOPE_PROTOTYPE) {
      multiple = false;
    }
    synchronized (properties) {
      if (available) {
        ref = dependents.get(b);
        if (scope == Constants.SCOPE_SINGLETON) {
          dependents.put(b, new Integer(ref != null ? ref.intValue() + 1 : 1));
          @SuppressWarnings("unchecked")
          final
          S res = (S) service;
          return res;
        } else if (!multiple) {
          if (ref == null) {
            dependents.put(b, new Integer(0));
            factoryThread = Thread.currentThread();
          } else if (factoryThread != null) {
            if (factoryThread.equals(Thread.currentThread())) {
              throw new IllegalStateException("Recursive call of getService");
            }
          }
        }
        sBundle = bundle;
      } else {
        return null;
      }
    }
    S s = null;
    if (ref == null || multiple) {
      try {
        s = sBundle.fwCtx.perm.callGetService(this, b);
        if (s == null) {
          sBundle.fwCtx.frameworkWarning(sBundle,
             new ServiceException("ServiceFactory produced null",
                                  ServiceException.FACTORY_ERROR));
        }
      } catch (final Throwable pe) {
        sBundle.fwCtx.frameworkError(sBundle,
            new ServiceException("ServiceFactory throw an exception",
                                ServiceException.FACTORY_EXCEPTION, pe));
      }
      if (s != null) {
        final String[] classes = (String[])getProperty(Constants.OBJECTCLASS);
        for (final String cls : classes) {
          if (!sBundle.fwCtx.services.checkServiceClass(s, cls)) {
            sBundle.fwCtx.frameworkError(sBundle,
                new ServiceException("ServiceFactory produced an object " +
                                     "that did not implement: " + cls,
                                     ServiceException.FACTORY_ERROR));
            s = null;
            break;
          }
        }
      }
      if (s == null) {
        if (!multiple) {
          synchronized (properties) {
            if (dependents != null && ref == null) {
              dependents.remove(b);
            }
            factoryThread = null;
          }
        }
        return null;
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
        if (multiple) {
          List<S> il = prototypeServiceInstances.get(b);
          if (il == null) {
            il = new LinkedList<S>();
            prototypeServiceInstances.put(b, il);
          }
          il.add(s);
        } else {
          factoryThread = null;
          serviceInstances.put(b, s);
          properties.notifyAll();
        }
      }
      if (s != null && !multiple) {
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
        sBundle.fwCtx.frameworkError(sBundle, e);
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
        HashSet<Bundle> bs = new HashSet<Bundle>();
        bs.addAll(d.keySet());
        bs.addAll(ungetInProgress);
        if (prototypeServiceInstances != null) {
          bs.addAll(prototypeServiceInstances.keySet());
        }
        if (!bs.isEmpty()) {
          return bs.toArray(new Bundle[bs.size()]);
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
      boolean res = deps.containsKey(b);
      if (!res) {
        if (prototypeServiceInstances != null) {
          res = prototypeServiceInstances.containsKey(b);
        }
      }
      return res;
    }
    return false;
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
  boolean ungetService(Bundle b, boolean checkRefCounter,  S uservice) {
    List<S> servicesToRemove = new ArrayList<S>();
    Hashtable<Bundle,Integer> deps = null;
    BundleImpl sBundle;
    boolean res = false;

    synchronized (properties) {
      if (dependents == null) {
        return false;
      }
      if (scope == Constants.SCOPE_PROTOTYPE) {
        if (uservice != null) {
          List<S> sl = prototypeServiceInstances.get(b);
          if (sl != null) {
            for (Iterator<S> i = sl.iterator(); i.hasNext(); ) {
              if (i.next() == uservice) {
                i.remove();
                servicesToRemove.add(uservice);
                res = true;
                break;
              }
            }
          }
          if (!res) {
            throw new IllegalArgumentException("Service is not in use or not from this service reference");
          }
        } else if (!checkRefCounter) {
          List<S> sl = prototypeServiceInstances.get(b);
          if (sl != null && !sl.isEmpty()) {
            servicesToRemove.addAll(sl);
            sl.clear();
            res = true;
          }
        }
      }
      if (uservice == null || scope != Constants.SCOPE_PROTOTYPE) {
        final Object countInteger = dependents.get(b);
        if (countInteger != null) {
          if (uservice != null) {
            S s = scope == Constants.SCOPE_SINGLETON ? (S)service : serviceInstances.get(b);
            if (s != uservice) {
              throw new IllegalArgumentException("Service is not in user or not from this service reference");
            }
          }

          final int count = ((Integer) countInteger).intValue();
          if (checkRefCounter && count > 1) {
            dependents.put(b, new Integer(count - 1));
          } else {
            synchronized (dependents) {
              ungetInProgress.add(b);
              dependents.remove(b);
              deps = dependents;
            }
            if (scope != Constants.SCOPE_SINGLETON) {
              S s = serviceInstances.remove(b);
              if (s != null) {
                servicesToRemove.add(s);
              }
            }
          }
          res = true;
        }
      }
      sBundle = bundle;
    }

    for (S s : servicesToRemove) {
      try {
        sBundle.fwCtx.perm.callUngetService(this, b, s);
      } catch (final Throwable e) {
        sBundle.fwCtx.frameworkError(sBundle, e);
      }
    }
    if (deps != null) {
      synchronized (deps) {
        ungetInProgress.remove(b);
      }
    }
    return res;
  }

  ServiceReferenceDTO getDTO() {
    ServiceReferenceDTO res = new ServiceReferenceDTO();
    PropertiesDictionary p = properties;
    res.id = ((Long)p.get(Constants.SERVICE_ID)).longValue();
    res.properties = p.getDTO();
    Bundle [] using = getUsingBundles();
    if (using != null) {
      res.usingBundles = new long [using.length];
      for (int i = 0; i < using.length; i++) {
        res.usingBundles[i] = using[i].getBundleId();
      }
    } else {
      res.usingBundles = new long [0];
    }
    BundleImpl b = bundle;
    if (b == null) {
      return null;
    }
    res.bundle = b.id;
    return res;
  }
}
