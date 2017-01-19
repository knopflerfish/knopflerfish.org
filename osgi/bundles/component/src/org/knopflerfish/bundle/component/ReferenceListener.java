/*
 * Copyright (c) 2010-2017, KNOPFLERFISH project
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;


/**
 *
 */
class ReferenceListener
{
  private static final int ADD_OP = 1;
  private static final int DELETE_OP = 2;
  private static final int UPDATE_OP = 3;
  
  final Reference ref;
  private final HashMap<ServiceReference<?>, Object>  bundleBound =
      new HashMap<ServiceReference<?>, Object>();
  private final SortedSet<ServiceReference<?>> serviceRefs =
      Collections.synchronizedSortedSet(new TreeSet<ServiceReference<?>>());
  private final HashSet<ServiceReference<?>> unbinding = new HashSet<ServiceReference<?>>();
  private ServiceReference<?> selectedServiceRef = null;
  private final TreeSet<String> ids = new TreeSet<String>();
  private Filter cmTarget;
  private final LinkedList<RefServiceEvent> sEventQueue = new LinkedList<RefServiceEvent>();
  private final HashMap<ServiceReference<?>, HashSet<ComponentContextImpl>> cciBound =
      new HashMap<ServiceReference<?>, HashSet<ComponentContextImpl>>();


  /**
   * Create a listener for services fulfilling this reference
   * with specified CM configuration.
   *
   */
  ReferenceListener(Reference ref) {
    this.ref = ref;
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
    ref.comp.listener.removeServiceListener(this);
    Activator.logDebug("Stop listening, ref=" + getName());
  }


  /**
   * Start listening for this reference. It is a bit tricky to
   * get initial state synchronized with the listener.
   *
   */
  void setTarget(String ccid, Map<String, Object> dict) {
    if (ccid != null) {
      cmTarget = ref.getTarget(dict, "CC id = " + ccid);
    } else {
      cmTarget = null;
    }
    final String filter = getFilter();
    Activator.logDebug("Start listening, ref=" + getName() + " with filter=" + filter);
    ref.comp.listener.removeServiceListener(this);
    synchronized (serviceRefs) {
      final HashSet<ServiceReference<?>> oldServiceRefs
        = new HashSet<ServiceReference<?>>(serviceRefs);
      serviceRefs.clear();
      ServiceReference<?> [] srs;
      try {
        ref.comp.listener.addServiceListener(this);
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
            final RefServiceEvent e = new RefServiceEvent(ServiceEvent.REGISTERED, sr);
            if (best == null || best.compareTo(sr) < 0) {
              best = sr;
              sEventQueue.addFirst(e);
            } else {
              sEventQueue.addLast(e);
            }
          }
        }
      }
      addId(ccid != null ? ccid : Component.NO_CCID, true);
      for (final ServiceReference<?> oldSR : oldServiceRefs) {
        sEventQueue.addLast(new RefServiceEvent(ServiceEvent.MODIFIED_ENDMATCH,
                                             oldSR));
      }
    }
    serviceChanged(null);
  }


  /**
   *
   */
  boolean checkTargetChanged(String ccid, Map<String, Object> dict) {
    final Filter f = ref.getTarget(dict, "CC id = " + ccid);
    if (f != null) {
      return !f.equals(cmTarget);
    } else {
      return cmTarget != null;
    }
  }


  /**
   * Add CC id that is connected to this reference listener.
   *
   * @param id String with CC id.
   * @param clear Clear all previous ids connected to this reference listener.
   */
  void addId(String id, boolean clear) {
    if (clear) {
      ids.clear();
    }
    ids.add(id);
  }


  /**
   *
   */
  void removeId(String id) {
    ids.remove(id);
  }


  /**
   *
   */
  boolean noIds() {
    return ids.isEmpty();
  }


  /**
   *
   */
  Set<String> getIds() {
    return ids;
  }


  /**
   *
   */
  boolean isOnlyId(String id) {
    return ids.size() == 1 && ids.contains(id);
  }


  /**
   * Is there enough services available for this reference and always at least 1?
   */
  boolean isAvailable() {
    synchronized (serviceRefs) {
      return !serviceRefs.isEmpty() && serviceRefs.size() >= ref.getMinimumCardinality();
    }
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
   * True if this reference has policy-option=greedy;
   */
  private boolean isGreedy() {
    return ref.refDesc.greedy;
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
  String getInterface() {
    return ref.refDesc.interfaceName;
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
      if (selectedServiceRef == null) {
        setSelected(getHighestRankedServiceReference());
      }
    }
    return selectedServiceRef;
  }


  private ServiceReference<?> getHighestRankedServiceReference() {
    synchronized (serviceRefs) {
      return serviceRefs.isEmpty() ? null : serviceRefs.last();      
    }
  }


  void setSelected(ServiceReference<?> s) {
    synchronized (serviceRefs) {
      selectedServiceRef = s;
    }
  }


  /**
   * Get highest ranked service.
   */
  Object getService(ComponentContextImpl cci) {
    final ServiceReference<?> sr = getServiceReference();
    if (sr != null) {
      return getServiceCheckActivate(sr, cci);
    }
    return null;
  }


  /**
   * Get service if it belongs to this reference.
   */
  Object getService(ServiceReference<?> sr, ComponentContextImpl cci) {
    boolean c;
    synchronized (serviceRefs) {
      c = serviceRefs.contains(sr) || unbinding.contains(sr);
    }
    Activator.logDebug("RL.getService " + Activator.srInfo(sr) + " available: " + c);
    return c ? getServiceCheckActivate(sr, cci) : null;
  }


  ServiceReference<?> [] getServiceReferences() {
    return serviceRefs.toArray(new ServiceReference[serviceRefs.size()]);
  }


  ServiceReference<?> [] getBoundServiceReferences() {
    synchronized (cciBound) {
      Set<ServiceReference<?>> srs = cciBound.keySet();
      return srs.isEmpty() ? null : srs.toArray(new ServiceReference<?> [srs.size()]);
    }
  }


  ArrayList<ServiceReference<?>> getBoundServiceReferences(ComponentContextImpl cci) {
    ArrayList<ServiceReference<?>> res = new ArrayList<ServiceReference<?>>();
    synchronized (cciBound) {
      for (Entry<ServiceReference<?>, HashSet<ComponentContextImpl>> e : cciBound.entrySet()) {
        if (e.getValue().contains(cci)) {
          res.add(e.getKey());
        }
      }
    }
    return res;
  }


  void bound(ServiceReference<?> sr, Object s, ComponentContextImpl cci) {
    synchronized (cciBound) {
      HashSet<ComponentContextImpl> ccis = cciBound.get(sr);
      if (ccis == null) {
        ccis = new HashSet<ComponentContextImpl>();
        cciBound.put(sr, ccis);
      }
      ccis.add(cci);
      if (ref.isScopeBundle()) {
        bundleBound.put(sr, s);
      }
    }
  }


  private Object getBound(ServiceReference<?> sr, ComponentContextImpl cci) {
    synchronized (cciBound) {
      Object res;
      if (ref.isScopeBundle()) {
        res = bundleBound.get(sr);
      } else {
        res = cci.getCciBound(sr, this);
      }
      return res;
    }
  }


  boolean isBound(ServiceReference<?> sr, ComponentContextImpl cci) {
    synchronized (cciBound) {
      HashSet<ComponentContextImpl> ccis = cciBound.get(sr);
      return ccis != null && ccis.contains(cci);
    }
  }


  void unbound(ServiceReference<?> sr, ComponentContextImpl cci) {
    Object s = null;
    synchronized (cciBound) {
      HashSet<ComponentContextImpl> ccis = cciBound.get(sr);
      if (ccis != null) {
        ccis.remove(cci);
        if  (ccis.isEmpty()) {
          cciBound.remove(sr);
          // Remove bundle bound service
          s = bundleBound.remove(sr);
        }
      }
    }
    if (s != null) {
      ref.comp.bc.ungetService(sr);
    }
  }


  /**
   * Get all services.
   */
  Object[] getServices(ComponentContextImpl cci)
  {
    final ServiceReference<?>[] srs = getServiceReferences();
    final ArrayList<Object> res = new ArrayList<Object>(srs.length);
    for (final ServiceReference<?> sr : srs) {
      res.add(getServiceCheckActivate(sr, cci));
    }
    return res.toArray();
  }


  int numAvailable()
  {
    return serviceRefs.size();
  }


  /**
   *
   */
  void serviceEvent(ServiceReference<?> s, ServiceEvent se) {
    Filter f = getTargetFilter();
    boolean match = f == null || f.match(s);
    if (match && ReferenceDescription.SCOPE_PROTOTYPE_REQUIRED.equals(ref.getScope())) {
      match = Constants.SCOPE_PROTOTYPE.equals(s.getProperty(Constants.SERVICE_SCOPE));
    }
    int type = se.getType();
    if (!match) {
      if (type != ServiceEvent.UNREGISTERING && serviceRefs.contains(s)) {
        type = ServiceEvent.MODIFIED_ENDMATCH;
      } else {
        return;
      }
    }
    serviceChanged(new RefServiceEvent(type, s, se));
  }


  private void serviceChanged(RefServiceEvent se) {
    ref.comp.scr.postponeCheckin();
    try {
      do {
        boolean wasSelected = false;
        int op;
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
          switch (se.type) {
          case ServiceEvent.MODIFIED:
          case ServiceEvent.REGISTERED:
            op = serviceRefs.add(se.sr) ? ADD_OP : UPDATE_OP;
            break;
          case ServiceEvent.MODIFIED_ENDMATCH:
          case ServiceEvent.UNREGISTERING:
            serviceRefs.remove(se.sr);
            if  (selectedServiceRef == se.sr) {
              wasSelected = true;
            }
            unbinding.add(se.sr);
            op = DELETE_OP;
            break;
          default:
            // To keep compiler happy
            throw new RuntimeException("Internal error");
          }
        }
        switch (op) {
        case ADD_OP:
          if (!ref.refAvailable()) {
            refAdded(se);
          }
          break;
        case DELETE_OP:
          boolean deleted = false;
          if (!ref.refUnavailable()) {
            if (ref.isMultiple() || wasSelected) {
              refDeleted(se);
              deleted = true;
            }
          }
          synchronized (serviceRefs) {
            unbinding.remove(se.sr);
            if (deleted) {
              setSelected(null);
            }
          }
          break;
        case UPDATE_OP:
          refUpdated(se);
          break;
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
  private Object getServiceCheckActivate(ServiceReference<?> sr, ComponentContextImpl cci) {
    Object s = getBound(sr, cci);
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
                if (cc.getState() != ComponentConfiguration.STATE_DEACTIVATING ||
                    !cc.isUnregistering()) {
                  cc.activate(ref.comp.bc.getBundle(), false);
                }
                break;
              }
            }
          } finally {
            ref.comp.scr.postponeCheckout();
          }
        }
      }
      if (ref.isScopeBundle()) {
        s = ref.comp.bc.getService(sr);
      }
    }
    bound(sr, s, cci);
    return s;
  }


  /**
   * Get filter string for finding this reference.
   */
  private String getFilter() {
    final Filter target = getTargetFilter();
    boolean addScope = ReferenceDescription.SCOPE_PROTOTYPE_REQUIRED.equals(ref.getScope());
    if (addScope || target != null) {
      StringBuilder sb = new StringBuilder("(&(");
      sb.append(Constants.OBJECTCLASS).append('=').append(getInterface()).append(')');
      if (addScope) {
        sb.append('(').append(Constants.SERVICE_SCOPE).append('=').append(Constants.SCOPE_PROTOTYPE).append(')');
      }
      if (target != null) {
        sb.append(target);
      }
      sb.append(')');
      return sb.toString();
    } else {
      return "(" + Constants.OBJECTCLASS + "=" + getInterface() +")";
    }
  }

  
  /**
   * The tracked reference has been added.
   *
   */
  private void refAdded(RefServiceEvent se)
  {
    Activator.logDebug("refAdded, " + toString() + ", " + se.sr);
    CompConfigs ccs = getComponentConfigs();
    if (isDynamic()) {
      if (isMultiple()) {
        bindReference(se.sr, ccs.active);
      } else {
        final ServiceReference<?> selected = getServiceReference();
        if (se.sr == selected) {
          bindReference(se.sr, ccs.active);
        } else if (isGreedy()) {
          setSelected(se.sr);
          bindReference(se.sr, ccs.active);
          unbindReference(selected, ccs.active, false);         
        }
      }
    } else {
      if (isGreedy()) {
        if (isMultiple() || selectedServiceRef == null || getServiceReference().compareTo(se.sr) < 0) {
          reactivate(ccs.active, se.src);
        }
      }
    }
    if (ref.comp.isSatisfied()) {
      for (String id : ccs.inactive) {
        ComponentConfiguration cc = ref.comp.newComponentConfiguration(id, null);
        if (cc != null) {
          ref.comp.activateComponentConfiguration(cc, null);
        }
      }
    }
  }

  /**
   * The tracked reference has been deleted.
   *
   */
  private void refDeleted(RefServiceEvent se)
  {
    Activator.logDebug("refDeleted, " + toString() + ", " + se.sr);
    ArrayList<ComponentConfiguration> ccs = getComponentConfigs().active;
    if (isDynamic()) {
      setSelected(null);
      if (isMultiple()) {
        unbindReference(se.sr, ccs, true);
      } else {
        final ServiceReference<?> newS = getServiceReference();
        if (newS != null) {
          bindReference(newS, ccs);
        }
        unbindReference(se.sr, ccs, newS == null);
      }
    } else {
      // If it is a service we use, check what to do
      reactivate(ccs, se.src);
      // Hold on to service during deactivation
      for (ComponentConfiguration cc : ccs) {
        cc.waitForDeactivate();
      }
    }
  }

  /**
   * The tracked reference has been updated.
   *
   */
  private void refUpdated(RefServiceEvent se)
  {
    Activator.logDebug("refUpdated, " + toString() + ", " + se.sr);
    ArrayList<ComponentConfiguration> ccs = getComponentConfigs().active;
    if (isMultiple()) {
      if (isDynamic()) {
        updatedReference(se.sr, ccs);
      } else if (isGreedy()) {
        reactivate(ccs, se.src);         
      }
    } else {
      ServiceReference<?> selected = getServiceReference();
      if (selected == se.sr) {
        if (isGreedy()) {
          selected = getHighestRankedServiceReference();
          if (se.sr == selected) {
            updatedReference(se.sr, ccs);
          } else {
            if (isDynamic()) {
              // Ranked lowered, use new highest ranked
              setSelected(selected);
              bindReference(selected, ccs);
              unbindReference(se.sr, ccs, false);
            } else {
              reactivate(ccs, se.src);
            }
          }
        } else if (isDynamic()) {
          updatedReference(se.sr, ccs);
        } else {
          reactivate(ccs, se.src);
        }
      } else if (isGreedy() && selected.compareTo(se.sr) < 0) {
        // New ranked higher than selected
        if (isDynamic()) {
          setSelected(se.sr);
          bindReference(se.sr, ccs);
          unbindReference(selected, ccs, false);
        } else {
          reactivate(ccs, se.src);
        }
      }
    }
  }

  private void bindReference(ServiceReference<?> s, ArrayList<ComponentConfiguration> ccs) {
    for (ComponentConfiguration cc : ccs) {
      cc.bindReference(this, s);
    }
  }


  private void unbindReference(ServiceReference<?> s, ArrayList<ComponentConfiguration> ccs, boolean resetField) {
    for (ComponentConfiguration cc : ccs) {
      cc.unbindReference(this, s, resetField);
    }
  }


  private void updatedReference(ServiceReference<?> s, ArrayList<ComponentConfiguration> ccs) {
    for (ComponentConfiguration cc : ccs) {
      cc.updatedReference(this, s);
    }
  }

  private void reactivate(ArrayList<ComponentConfiguration> ccs, final ServiceEvent se) {
    @SuppressWarnings("unchecked")
    final List<ComponentConfiguration> cccs = (List<ComponentConfiguration>) ccs.clone();
    for (Iterator<ComponentConfiguration> i = cccs.iterator(); i.hasNext(); ) {
      ComponentConfiguration cc = i.next();
      if (se == null || cc.setAndTestLastReactivateEvent(se)) {
        cc.dispose(ComponentConstants.DEACTIVATION_REASON_REFERENCE, false);
      } else {
        i.remove();
      }
    }
    if (!cccs.isEmpty()) {
      Runnable delayed = new Runnable() {      
        @Override
        public void run() {
          for (ComponentConfiguration cc : cccs) {
            ComponentConfiguration [] nccs = cc.remove(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
            if (nccs != null) {
              for (ComponentConfiguration ncc : nccs) {
                ncc.setAndTestLastReactivateEvent(se);
              }
            }
          }
        }
      };
      if (se != null) {
        ref.comp.listener.addAfterServiceEvent(se, delayed);
      } else {
        delayed.run();
      }
    }
  }


    /**
   * Call refUpdated for component configurations that has bound this reference.
   */
  private CompConfigs getComponentConfigs() {
    ArrayList<ComponentConfiguration> active = new ArrayList<ComponentConfiguration>();
    ArrayList<String> inactive = new ArrayList<String>();
    for (String id : getIds()) {
      final ComponentConfiguration [] componentConfigurations = ref.comp.compConfigs.get(id);
      if (componentConfigurations != null) {
        Collections.addAll(active, componentConfigurations);
      } else if (!(ref.comp instanceof FactoryComponent)) {
        inactive.add(id);
      }
    }
    return new CompConfigs(active, inactive);
  }

  private static class CompConfigs {
    final ArrayList<ComponentConfiguration> active;
    final ArrayList<String> inactive;
   
    CompConfigs(ArrayList<ComponentConfiguration> active,
                ArrayList<String> inactive) {
      this.active = active;
      this.inactive = inactive;
    }
  }

  private static class RefServiceEvent {

    final int type;
    final ServiceReference<?> sr;
    final ServiceEvent src;


    RefServiceEvent(int type, ServiceReference<?> sr, ServiceEvent src) {
      this.type = type;
      this.sr = sr;
      this.src = src;
    }


    RefServiceEvent(int type, ServiceReference<?> sr) {
      this(type, sr, null);
    }

  }
}
