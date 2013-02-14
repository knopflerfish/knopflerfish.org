/*
 * Copyright (c) 2010-2013, KNOPFLERFISH project
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentConstants;


/**
 *
 */
class ReferenceListener implements ServiceListener
{
  final Reference ref;
  private final HashMap<ServiceReference<?>, Object>  bound = new HashMap<ServiceReference<?>, Object>();
  private final HashSet<ServiceReference<?>> serviceRefs = new HashSet<ServiceReference<?>>();
  private HashSet<ServiceReference<?>> unbinding = new HashSet<ServiceReference<?>>();
  private ServiceReference<?> selectedServiceRef = null;
  private final TreeSet<String> pids = new TreeSet<String>();
  private Filter cmTarget;
  private final LinkedList<ServiceEvent> sEventQueue = new LinkedList<ServiceEvent>();


  /**
   * Create a listener for services fulfilling this reference
   * with specified CM configuration.
   *
   */
  ReferenceListener(Reference ref, Configuration config) {
    this.ref = ref;
    setTarget(config);
  }


  /**
   *
   */
  @Override
  public String toString() {
    return "ReferenceListener(" + ref + ", target=" + cmTarget + ")";
  }


  /**
   * Stop listening for services fulfilling this reference
   *
   */
  void stop() {
    ref.comp.bc.removeServiceListener(this);
    Activator.logDebug("Stop listening, ref=" + getName());
  }


  /**
   * Start listening for this reference. It is a bit tricky to
   * get initial state synchronized with the listener.
   *
   */
  void setTarget(Configuration config) {
    if (config != null) {
      cmTarget = ref.getTarget(config.getProperties(), "CM pid = " + config.getPid());
    } else {
      cmTarget = null;
    }
    final String filter = getFilter();
    Activator.logDebug("Start listening, ref=" + getName() + " with filter=" + filter);
    ref.comp.bc.removeServiceListener(this);
    synchronized (serviceRefs) {
      final HashSet<ServiceReference<?>> oldServiceRefs
        = new HashSet<ServiceReference<?>>(serviceRefs);
      serviceRefs.clear();
      ServiceReference<?> [] srs;
      try {
        ref.comp.bc.addServiceListener(this, filter);
        srs = ref.comp.bc.getServiceReferences((String)null, filter);
      } catch (final InvalidSyntaxException ise) {
        throw new RuntimeException("Should not occur, Filter already checked");
      }
      if (srs != null) {
        ServiceReference<?> best = null;
        for (final ServiceReference<?> sr : srs) {
          if (oldServiceRefs.remove(sr)) {
            serviceRefs.add(sr);
          } else {
            final ServiceEvent e = new ServiceEvent(ServiceEvent.REGISTERED, sr);
            if (best == null || best.compareTo(sr) < 0) {
              best = sr;
              sEventQueue.addFirst(e);
            } else {
              sEventQueue.addLast(e);
            }
          }
        }
      }
      addPid(config != null ? config.getPid() : Component.NO_PID, true);
      for (final ServiceReference<?> oldSR : oldServiceRefs) {
        sEventQueue.addLast(new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH,
                                             oldSR));
      }
    }
    serviceChanged(null);
  }


  /**
   *
   */
  boolean checkTargetChanged(Configuration config) {
    final Filter f = ref.getTarget(config.getProperties(), "CM pid = " + config.getPid());
    if (f != null) {
      return !f.equals(cmTarget);
    } else {
      return cmTarget != null;
    }
  }


  /**
   * Add CM pid that is connected to this reference listener.
   *
   * @param pid String with CM pid.
   * @param clear Clear all previous pids connected to this reference listener.
   */
  void addPid(String pid, boolean clear) {
    if (clear) {
      pids.clear();
    }
    pids.add(pid);
  }


  /**
   *
   */
  void removePid(String pid) {
    pids.remove(pid);
  }


  /**
   *
   */
  boolean noPids() {
    return pids.isEmpty();
  }


  /**
   *
   */
  Iterator<String> getPids() {
    return pids != null ? pids.iterator() : null;
  }


  /**
   *
   */
  boolean isOnlyPid(String pid) {
    return pids == null || (pids.size() == 1 && pids.contains(pid));
  }


  /**
   * Is there a service available for this reference?
   */
  boolean isAvailable() {
    return !serviceRefs.isEmpty();
  }


  /**
   * Is this reference dynamic;
   */
  boolean isDynamic() {
    return ref.refDesc.dynamic;
  }


  /**
   * Is this reference of multiple cardinality;
   */
  boolean isMultiple() {
    return ref.refDesc.multiple;
  }


  /**
   * Is this reference optional;
   */
  boolean isOptional() {
    return ref.refDesc.optional;
  }


  /**
   * Get CM filter.
   */
  Filter getTargetFilter() {
    return (cmTarget != null) ? cmTarget : ref.targetFilter;
  }


  /**
   * Get name of reference.
   */
  String getName() {
    return ref.refDesc.name;
  }


  /**
   * Get highest ranked service reference.
   */
  ServiceReference<?> getServiceReference() {
    synchronized (serviceRefs) {
      if (selectedServiceRef == null &&
          !serviceRefs.isEmpty()) {
        final Iterator<ServiceReference<?>> i = serviceRefs.iterator();
        selectedServiceRef = i.next();
        while (i.hasNext()) {
          final ServiceReference<?> tst = i.next();
          if (selectedServiceRef.compareTo(tst) < 0) {
            selectedServiceRef = tst;
          }
        }
      }
    }
    return selectedServiceRef;
  }


  /**
   * Get highest ranked service.
   */
  Object getService() {
    final ServiceReference<?> sr = getServiceReference();
    if (sr != null) {
      return getServiceCheckActivate(sr);
    }
    return null;
  }


  /**
   * Get service if it belongs to this reference.
   */
  Object getService(ServiceReference<?> sr) {
    boolean c;
    synchronized (serviceRefs) {
      c = serviceRefs.contains(sr) || unbinding.contains(sr);
    }
    return c ? getServiceCheckActivate(sr) : null;
  }


  ServiceReference<?> [] getServiceReferences() {
    synchronized (serviceRefs) {
      return serviceRefs.toArray(new ServiceReference[serviceRefs.size()]);
    }
  }


  ServiceReference<?> [] getBoundServiceReferences() {
    synchronized (bound) {
      return bound.keySet().toArray(new ServiceReference[bound.size()]);
    }
  }


  void bound(ServiceReference<?> sr, Object s) {
    synchronized (bound) {
      if (bound.get(sr) == null) {
        bound.put(sr, s);
      }
    }
  }


  Object getBound(ServiceReference<?> sr) {
    synchronized (bound) {
      return bound.get(sr);
    }
  }


  boolean doUnbound(ServiceReference<?> sr) {
    synchronized (bound) {
      return bound.containsKey(sr);
    }
  }


  void unbound(ServiceReference<?> sr) {
    Object s;
    synchronized (bound) {
      s = bound.remove(sr);
    }
    if (s != null) {
      ref.comp.bc.ungetService(sr);
    }
  }


  /**
   * Get all services.
   */
  Object[] getServices()
  {
    final ServiceReference<?>[] srs = getServiceReferences();
    final ArrayList<Object> res = new ArrayList<Object>(srs.length);
    for (final ServiceReference<?> sr : srs) {
      res.add(getServiceCheckActivate(sr));
    }
    return res.toArray();
  }

  HashSet<ServiceReference<?>> getUnbinding() {
    synchronized (serviceRefs) {
      final HashSet<ServiceReference<?>> res = unbinding;
      unbinding = new HashSet<ServiceReference<?>>();
      return res;
    }
  }

  //
  // ServiceListener
  //

  /**
   *
   */
  public void serviceChanged(ServiceEvent se) {
    ref.comp.scr.postponeCheckin();
    try {
      do {
        boolean doAdd;
        int cnt;
        boolean wasSelected = false;
        ServiceReference<?> s;
        synchronized (serviceRefs) {
          if (sEventQueue.isEmpty()) {
            if (se == null) {
              return;
            }
          } else {
            if (se != null) {
              sEventQueue.addLast(se);
            }
            se = sEventQueue.removeFirst();
          }
          s = se.getServiceReference();
          switch (se.getType()) {
          case ServiceEvent.MODIFIED:
          case ServiceEvent.REGISTERED:
            if (!serviceRefs.add(s)) {
              // Service properties just changed,
              // ignore
              continue;
            }
            cnt = serviceRefs.size();
            doAdd = true;
            break;
          case ServiceEvent.MODIFIED_ENDMATCH:
          case ServiceEvent.UNREGISTERING:
            serviceRefs.remove(s);
            if  (selectedServiceRef == s) {
              selectedServiceRef = null;
              wasSelected = true;
            }
            cnt = serviceRefs.size();
            unbinding.add(s);
            doAdd = false;
            break;
          default:
            // To keep compiler happy
            throw new RuntimeException("Internal error");
          }
        }
        if (doAdd) {
          if (cnt != 1 || !ref.refAvailable()) {
            refUpdated(s, false, false);
          }
        } else {
          if (cnt != 0 || !ref.refUnavailable()) {
            refUpdated(s, true, wasSelected);
            synchronized (serviceRefs) {
              unbinding.remove(s);
            }
          }
        }
        se = null;
      } while (!sEventQueue.isEmpty());
    } finally {
      ref.comp.scr.postponeCheckout();
    }
  }

  //
  // Private methods
  //

  /**
   * Get service, but activate before so that we will
   * get any ComponentExceptions.
   */
  private Object getServiceCheckActivate(ServiceReference<?> sr) {
    Object s = getBound(sr);
    if (s == null) {
      final Object o = sr.getProperty(ComponentConstants.COMPONENT_NAME);
      if (o != null && o instanceof String) {
        final Component [] cs = ref.comp.scr.getComponent((String)o);
        if (cs != null) {
          ref.comp.scr.postponeCheckin();
          try {
            for (final Component element : cs) {
              final ComponentConfiguration cc = element.getComponentConfiguration(sr);
              if (cc != null) {
                cc.activate(ref.comp.bc.getBundle(), false);
                break;
              }
            }
          } finally {
            ref.comp.scr.postponeCheckout();
          }
        }
      }
      s = ref.comp.bc.getService(sr);
      bound(sr, s);
    }
    return s;
  }


  /**
   * Get filter string for finding this reference.
   */
  private String getFilter() {
    final Filter target = getTargetFilter();
    if (target != null) {
      return "(&(" + Constants.OBJECTCLASS + "=" +
        ref.refDesc.interfaceName +")" + target.toString() + ")";
    } else {
      return "(" + Constants.OBJECTCLASS + "=" + ref.refDesc.interfaceName +")";
    }
  }


  /**
   * Call refUpdated for component configurations that has bound this reference.
   */
  private void refUpdated(ServiceReference<?> s, boolean deleted, boolean wasSelected) {
    final Iterator<String> i = getPids();
    if (i != null) {
      while (i.hasNext()) {
        final ComponentConfiguration cc = ref.comp.compConfigs.get(i.next());
        if (cc != null) {
          cc.refUpdated(this, s, deleted, wasSelected);
        }
      }
    }
  }

}
