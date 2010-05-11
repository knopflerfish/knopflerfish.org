/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.component.ComponentConstants;


/**
 *
 */
class ReferenceListener implements ServiceListener
{
  final Reference ref;
  private TreeSet serviceRefs = new TreeSet();
  private TreeSet pids = new TreeSet();
  private Filter cmTarget;


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
   * Stop listening for services fulfilling this reference
   *
   */
  void stop() {
    ref.comp.bc.removeServiceListener(this);
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
    String filter = getFilter();
    Activator.logDebug("Listening ref=" + getName() + " with filter=" + filter);
    TreeSet oldServiceRefs = (TreeSet)serviceRefs.clone();
    TreeSet newServiceRefs = new TreeSet();
    try {
      ref.comp.bc.addServiceListener(this, filter);
      ServiceReference [] srs = ref.comp.bc.getServiceReferences(null, filter);
      if (srs != null) {
        for (int i = 0; i < srs.length; i++) {
          newServiceRefs.add(srs[i]);
        }
      }
    } catch (InvalidSyntaxException ise) {
      throw new RuntimeException("Should not occur, Filter already checked");
    }
    addPid(config != null ? config.getPid() : Component.NO_PID, true);
    int i = oldServiceRefs.size();
    ServiceReference [] oldSR = 
      (ServiceReference [])oldServiceRefs.toArray(new ServiceReference [i--]);
    int j = newServiceRefs.size();
    ServiceReference [] newSR =
      (ServiceReference [])newServiceRefs.toArray(new ServiceReference [j--]);
    LinkedList el = new LinkedList();
    while (true) {
      if (i < 0) {
        if (j < 0) {
          break;
        }
        el.addLast(new ServiceEvent(ServiceEvent.REGISTERED, newSR[j--]));
      } else if (j < 0) {
        el.addFirst(new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, oldSR[i--]));
      } else {
        int c = oldSR[i].compareTo(newSR[j]);
        if (c < 0) {
          el.addLast(new ServiceEvent(ServiceEvent.REGISTERED, newSR[j--]));
        } else if (c == 0) {
          i--;
          j--;
        } else {
          el.addFirst(new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, oldSR[i--]));
        }
      }
    }
    for (Iterator e = el.iterator(); e.hasNext(); ) {
      serviceChanged((ServiceEvent)e.next());
    }
  }


  /**
   *
   */
  boolean checkTargetChanged(Configuration config) {
    Filter f = ref.getTarget(config.getProperties(), "CM pid = " + config.getPid());
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
  Iterator getPids() {
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
  ServiceReference getServiceReference() {
    synchronized (serviceRefs) {
      if (!serviceRefs.isEmpty()) {
        return (ServiceReference)serviceRefs.last();
      }
    }
    return null;
  }


  /**
   * Get highest ranked service.
   */
  Object getService(Bundle usingBundle) {
    ServiceReference sr = getServiceReference();
    if (sr != null) {
      return getServiceCheckActivate(sr, usingBundle);
    }
    return null;
  }


  /**
   * Get service if it belongs to this reference.
   */
  Object getService(ServiceReference sr, Bundle usingBundle) {
    if (serviceRefs.contains(sr)) {
      return getServiceCheckActivate(sr, usingBundle);
    }
    return null;
  }


  ServiceReference [] getServiceReferences() {
    synchronized (serviceRefs) {
      return (ServiceReference [])serviceRefs.toArray(new ServiceReference[serviceRefs.size()]);
    }
  }


  /**
   * Get all services.
   */
  Object [] getServices(Bundle usingBundle) {
    ServiceReference [] srs = getServiceReferences();
    ArrayList res = new ArrayList(srs.length);
    for (int i = 0; i < srs.length; i++) {
      res.add(getServiceCheckActivate(srs[i], usingBundle));
    }
    return res.toArray();
  }

  //
  // ServiceListener
  //

  /**
   *
   */
  public void serviceChanged(ServiceEvent se) {
    ServiceReference s = se.getServiceReference();
    int cnt;
    boolean best;
    switch (se.getType()) {
    case ServiceEvent.REGISTERED:
      synchronized (serviceRefs) {
        serviceRefs.add(s);
        best = serviceRefs.last() == s;
        cnt = serviceRefs.size();
      }
      if (cnt != 1 || !ref.refAvailable()) {
        refUpdated(s, false, best);
      }
      break;
    case ServiceEvent.MODIFIED:
      refUpdated(s, false, serviceRefs.last() == s);
      break;
    case ServiceEvent.MODIFIED_ENDMATCH:
    case ServiceEvent.UNREGISTERING:
      synchronized (serviceRefs) {
        best = (serviceRefs.last() == s);
        serviceRefs.remove(s);
        cnt = serviceRefs.size();
      }
      if (cnt != 0 || !ref.refUnavailable()) {
        refUpdated(s, true, best);
      }
    }
  }

  //
  // Private methods
  //

  /**
   * Get service if it belongs to this reference.
   */
  private Object getServiceCheckActivate(ServiceReference sr, Bundle usingBundle) {
    Object o = sr.getProperty(ComponentConstants.COMPONENT_NAME);
    if (o != null && o instanceof String) {
      Component [] cs = ref.comp.scr.getComponent((String)o);
      if (o != null) {
        for (int i = 0; i < cs.length; i++) {
          ComponentConfiguration cc = cs[i].getComponentConfiguration(sr);
          if (cc != null) {
            cc.activate(usingBundle);
            break;
          }
        }
      }
    }
    return ref.comp.bc.getService(sr);
  }


  /**
   * Get filter string for finding this reference.
   */
  private String getFilter() {
    Filter target = getTargetFilter();
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
  private void refUpdated(ServiceReference s, boolean deleted, boolean wasBest) {
    Iterator i = getPids();
    if (i != null) {
      while (i.hasNext()) {
        ComponentConfiguration cc = (ComponentConfiguration)ref.comp.compConfigs.get(i.next());
        if (cc != null) {
          cc.refUpdated(this, s, deleted, wasBest);
        }
      }
    }
  }

}
