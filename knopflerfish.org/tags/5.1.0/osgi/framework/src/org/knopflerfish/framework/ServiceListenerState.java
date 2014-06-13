/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;

/**
 * Container of all service listeners.
 */
class ServiceListenerState {
  protected final static String[] hashedKeys =
    new String[] { Constants.OBJECTCLASS.toLowerCase(),
                   Constants.SERVICE_ID.toLowerCase(),
                   Constants.SERVICE_PID.toLowerCase()
    };
  private final static int OBJECTCLASS_IX = 0;
  private final static int SERVICE_ID_IX  = 1;
  private final static int SERVICE_PID_IX = 2;
  protected static List<String> hashedKeysV;

  /* Service listeners with complicated or empty filters */
  List<ServiceListenerEntry> complicatedListeners = new ArrayList<ServiceListenerEntry>();

  /* Service listeners with "simple" filters are cached. */
  /* [Value -> List(ServiceListenerEntry)] */
  @SuppressWarnings("unchecked")
  Map<Object,List<ServiceListenerEntry>>[] cache
    = new HashMap[hashedKeys.length];

  Set<ServiceListenerEntry> serviceSet = new HashSet<ServiceListenerEntry>();

  Listeners listeners;

  ServiceListenerState(Listeners listeners) {
    this.listeners = listeners;
    hashedKeysV = new ArrayList<String>();
    for (int i = 0; i < hashedKeys.length; i++) {
      hashedKeysV.add(hashedKeys[i]);
      cache[i] = new HashMap<Object,List<ServiceListenerEntry>>();
    }
  }

  void clear()
  {
    hashedKeysV.clear();
    complicatedListeners.clear();
    for (int i = 0; i < hashedKeys.length; i++) {
      cache[i].clear();
    }
    serviceSet.clear();
    listeners = null;;
  }

  /**
   * Add a new service listener. If an old one exists, and it has the
   * same owning bundle, the old listener is removed first.
   *
   * @param bc The bundle context adding this listener.
   * @param listener The service listener to add.
   * @param filter An LDAP filter string to check when a service is modified.
   * @exception org.osgi.framework.InvalidSyntaxException
   * If the filter is not a correct LDAP expression.
   */
  synchronized void add(BundleContextImpl bc,
                        ServiceListener listener,
                        String filter)
      throws InvalidSyntaxException
  {
    final ServiceListenerEntry sle = new ServiceListenerEntry(bc, listener, filter);
    if (serviceSet.contains(sle)) {
      remove(bc, listener);
    }
    serviceSet.add(sle);
    listeners.fwCtx.serviceHooks.handleServiceListenerReg(sle);
    checkSimple(sle);
  }

  /**
   * Remove a service listener.
   *
   * @param bc The bundle context removing this listener.
   * @param listener The service listener to remove.
   */
  synchronized void remove(BundleContextImpl bc, ServiceListener listener) {
    for (final Iterator<ServiceListenerEntry> it = serviceSet.iterator(); it.hasNext();) {
      final ServiceListenerEntry sle = it.next();
      if (sle.bc == bc && sle.listener == listener) {
        sle.setRemoved(true);
        listeners.fwCtx.serviceHooks.handleServiceListenerUnreg(sle);
        removeFromCache(sle);
        it.remove();
        break;
      }
    }
  }

  /**
   * Remove all references to a service listener from the service listener
   * cache.
   */
  private void removeFromCache(ServiceListenerEntry sle) {
    if (sle.local_cache != null) {
      for (int i = 0; i < hashedKeys.length; i++) {
        final Map<Object, List<ServiceListenerEntry>> keymap = cache[i];
        final List<Object> l = sle.local_cache[i];
        if (l != null) {
          for (final Object value : l) {
            final List<ServiceListenerEntry> sles = keymap.get(value);
            if(sles != null) {
              sles.remove(sles.indexOf(sle));
              if (sles.isEmpty()) {
                keymap.remove(value);
              }
            }
          }
        }
      }
    } else {
      complicatedListeners.remove(sle);
    }
  }


  /**
   * Remove all service listeners registered by the specified bundle context.
   *
   * @param bc The bundle context to remove listeners for.
   */
  synchronized void removeAll(BundleContextImpl bc) {
    for (final Iterator<ServiceListenerEntry> it = serviceSet.iterator(); it.hasNext();) {
      final ServiceListenerEntry sle = it.next();
      if (sle.bc == bc) {
        removeFromCache(sle);
        it.remove();
      }
    }
  }


  /**
   * Remove all service listeners registered by the specified bundle.
   *
   * @param bundle The bundle to remove listeners for.
   */
  synchronized void hooksBundleStopped(BundleContextImpl bc) {
    final List<ServiceListenerEntry> entries = new ArrayList<ServiceListenerEntry>();
    for (final ServiceListenerEntry sle : serviceSet) {
      if (sle.bc == bc) {
        entries.add(sle);
      }
    }
    listeners.fwCtx.serviceHooks.handleServiceListenerUnreg(Collections.unmodifiableList(entries));
  }


  /**
   * Checks if the specified service listener's filter is simple enough
   * to cache.
   */
  public void checkSimple(ServiceListenerEntry sle) {
    if (sle.noFiltering || listeners.nocacheldap) {
      complicatedListeners.add(sle);
    } else {
      @SuppressWarnings("unchecked")
      final List<Object>[] local_cache = new List[hashedKeys.length];
      if (sle.ldap.isSimple(hashedKeysV, local_cache, false)) {
        sle.local_cache = local_cache;
        for (int i = 0; i < hashedKeys.length; i++) {
          if (local_cache[i] != null) {
            for (final Object value : local_cache[i]) {
              List<ServiceListenerEntry> sles = cache[i].get(value);
              if (sles == null)
                cache[i].put(value, sles = new ArrayList<ServiceListenerEntry>());
              sles.add(sle);
            }
          }
        }
      } else {
        if (listeners.fwCtx.debug.ldap) {
          listeners.fwCtx.debug.println("Too complicated filter: " + sle.ldap);
        }
        complicatedListeners.add(sle);
      }
    }
  }

  /**
   * Gets the listeners interested in modifications of the service reference
   *
   * @param sr The reference related to the event describing the service modification.
   * @return A set of listeners to notify.
   */
  synchronized Set<ServiceListenerEntry> getMatchingListeners(ServiceReferenceImpl<?> sr)
  {
    final Set<ServiceListenerEntry> set = new HashSet<ServiceListenerEntry>();
    // Check complicated or empty listener filters
    int n = 0;
    for (final ServiceListenerEntry sle : complicatedListeners) {
      if (sle.noFiltering || sle.ldap.evaluate(sr.getProperties(), false)) {
        set.add(sle);
      }
      ++n;
    }
    if (listeners.fwCtx.debug.ldap) {
      listeners.fwCtx.debug.println("Added " + set.size() + " out of " + n +
                                        " listeners with complicated filters");
    }
    // Check the cache
    final String[] c = (String[])sr.getProperty(Constants.OBJECTCLASS);
    for (final String element : c) {
      addToSet(set, OBJECTCLASS_IX, element);
    }
    final Long service_id = (Long)sr.getProperty(Constants.SERVICE_ID);
    if (service_id != null) {
      addToSet(set, SERVICE_ID_IX, service_id.toString());
    }
    final Object service_pid = sr.getProperty(Constants.SERVICE_PID);
    if (service_pid != null) {
      if (service_pid instanceof String) {
        addToSet(set, SERVICE_PID_IX, service_pid);
      } else if (service_pid instanceof String []) {
        final String [] sa = (String [])service_pid;
        for (final String element : sa) {
          addToSet(set, SERVICE_PID_IX, element);
        }
      } else if (service_pid instanceof Collection) {
        // TODO should we report if type isn't a String?
        @SuppressWarnings("unchecked")
        final Collection<String> pids = (Collection<String>) service_pid;
        for (final String pid : pids) {
          addToSet(set, SERVICE_PID_IX, pid);
        }
      }
    }
    return set;
  }

  /**
   * Add all members of the specified list to the specified set.
   */
  private void addToSet(Set<ServiceListenerEntry> set, int cache_ix, Object val) {
    final List<ServiceListenerEntry> l = cache[cache_ix].get(val);
    if (l != null) {
      if (listeners.fwCtx.debug.ldap) {
        listeners.fwCtx.debug.println(hashedKeys[cache_ix] + " matches " + l.size());
      }
      set.addAll(l);
    } else {
      if (listeners.fwCtx.debug.ldap) {
        listeners.fwCtx.debug.println(hashedKeys[cache_ix] + " matches none");
      }
    }
  }
}
