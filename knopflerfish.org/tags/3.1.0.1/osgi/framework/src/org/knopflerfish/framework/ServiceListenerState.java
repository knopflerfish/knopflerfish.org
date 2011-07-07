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
  protected static List hashedKeysV;

  /* Service listeners with complicated or empty filters */
  List complicatedListeners = new ArrayList();

  /* Service listeners with "simple" filters are cached. */
  Map[] /* [Value -> List(ServiceListenerEntry)] */
    cache = new HashMap[hashedKeys.length];

  Set /* ServiceListenerEntry */ serviceSet = new HashSet();

  Listeners listeners;

  ServiceListenerState(Listeners listeners) {
    this.listeners = listeners;
    hashedKeysV = new ArrayList();
    for (int i = 0; i < hashedKeys.length; i++) {
      hashedKeysV.add(hashedKeys[i]);
      cache[i] = new HashMap();
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
  synchronized void add(BundleContextImpl bc, ServiceListener listener, String filter)
  throws InvalidSyntaxException {
    ServiceListenerEntry sle = new ServiceListenerEntry(bc, listener, filter);
    if (serviceSet.contains(sle)) {
      remove(bc, listener);
    }
    serviceSet.add(sle);
    listeners.framework.hooks.handleServiceListenerReg(sle);
    checkSimple(sle);
  }

  /**
   * Remove a service listener.
   *
   * @param bc The bundle context removing this listener.
   * @param listener The service listener to remove.
   */
  synchronized void remove(BundleContextImpl bc, ServiceListener listener) {
    for (Iterator it = serviceSet.iterator(); it.hasNext();) {
      ServiceListenerEntry sle = (ServiceListenerEntry)it.next();
      if (sle.bc == bc && sle.listener == listener) {
        sle.setRemoved(true);
        listeners.framework.hooks.handleServiceListenerUnreg(sle);
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
        HashMap keymap = (HashMap)cache[i];
        List l = (List)sle.local_cache[i];
        if (l != null) {
          for (Iterator it = l.iterator(); it.hasNext();) {
            Object value = it.next();
            List sles = (List)keymap.get(value);
            sles.remove(sles.indexOf(sle));
            if (sles.isEmpty()) {
              keymap.remove(value);
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
    for (Iterator it = serviceSet.iterator(); it.hasNext();) {
      ServiceListenerEntry sle = (ServiceListenerEntry)it.next();
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
    List entries = new ArrayList();
    for (Iterator it = serviceSet.iterator(); it.hasNext();) {
      ServiceListenerEntry sle = (ServiceListenerEntry)it.next();
      if (sle.bc == bc) {
        entries.add(sle);
      }
    }
    listeners.framework.hooks.handleServiceListenerUnreg(Collections.unmodifiableList(entries));
  }


  /**
   * Checks if the specified service listener's filter is simple enough
   * to cache.
   */
  public void checkSimple(ServiceListenerEntry sle) {
    if (sle.ldap == null || listeners.nocacheldap) {
      complicatedListeners.add(sle);
    } else {
      List[] /* Value */ local_cache = new List[hashedKeys.length];
      if (sle.ldap.isSimple(hashedKeysV, local_cache, false)) {
        sle.local_cache = local_cache;
        for (int i = 0; i < hashedKeys.length; i++) {
          if (local_cache[i] != null) {
            for (Iterator it = local_cache[i].iterator(); it.hasNext();) {
              Object value = it.next();
              List sles = (List)cache[i].get(value);
              if (sles == null)
                cache[i].put(value, sles = new ArrayList());
              sles.add(sle);
            }
          }
        }
      } else {
        if (listeners.framework.debug.ldap) {
          listeners.framework.debug.println("Too complicated filter: " + sle.ldap);
        }
        complicatedListeners.add(sle);
      }
    }
  }

  /**
   * Gets the listeners interested in modifications of the service reference
   *
   * @param The reference related to the event describing the service modification.
   * @return A set of listeners to notify.
   */
  synchronized Set getMatchingListeners(ServiceReferenceImpl sr) {
    Set set = new HashSet();
    // Check complicated or empty listener filters
    int n = 0;
    for (Iterator it = complicatedListeners.iterator(); it.hasNext(); n++) {
      ServiceListenerEntry sle = (ServiceListenerEntry)it.next();
      if (sle.ldap == null || sle.ldap.evaluate(sr.getProperties(), false)) {
        set.add(sle);
      }
    }
    if (listeners.framework.debug.ldap) {
      listeners.framework.debug.println("Added " + set.size() + " out of " + n +
                                        " listeners with complicated filters");
    }
    // Check the cache
    String[] c = (String[])sr.getProperty(Constants.OBJECTCLASS);
    for (int i = 0; i < c.length; i++) {
      addToSet(set, OBJECTCLASS_IX, c[i]);
    }
    Long service_id = (Long)sr.getProperty(Constants.SERVICE_ID);
    if (service_id != null) {
      addToSet(set, SERVICE_ID_IX, service_id.toString());
    }
    Object service_pid = sr.getProperty(Constants.SERVICE_PID);
    if (service_pid != null) {
      if (service_pid instanceof String) {
        addToSet(set, SERVICE_PID_IX, service_pid);
      } else if (service_pid instanceof String []) {
        String [] sa = (String [])service_pid;
        for (int i = 0; i < sa.length; i++) {
          addToSet(set, SERVICE_PID_IX, sa[i]);
        }
      } else if (service_pid instanceof Collection) {
        for (Iterator i = ((Collection)service_pid).iterator(); i.hasNext(); ) {
          // TBD should we report if type isn't a String?
          addToSet(set, SERVICE_PID_IX, i.next());
        }
      }
    }
    return set;
  }

  /**
   * Add all members of the specified list to the specified set.
   */
  private void addToSet(Set set, int cache_ix, Object val) {
    List l = (List)cache[cache_ix].get(val);
    if (l != null) {
      if (listeners.framework.debug.ldap) {
        listeners.framework.debug.println(hashedKeys[cache_ix] + " matches " + l.size());
      }
      for (Iterator it = l.iterator(); it.hasNext();) {
        set.add(it.next());
      }
    } else {
      if (listeners.framework.debug.ldap) {
        listeners.framework.debug.println(hashedKeys[cache_ix] + " matches none");
      }
    }
  }
}
