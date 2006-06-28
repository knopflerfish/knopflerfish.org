/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

import java.security.*;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.EventListener;

import org.osgi.framework.*;

/**
 * Here we handle all listeners that bundles have registered.
 *
 * @author Jan Stein
 * @author Philippe Laporte
 */
public class Listeners
  implements BundleListener, FrameworkListener, ServiceListener {

  /**
   * All bundle event listeners.
   */
  private HashSet bundleListeners = new HashSet();
  private HashSet syncBundleListeners = new HashSet();

  /**
   * All framework event listeners.
   */
  private HashSet frameworkListeners = new HashSet();

  /**
   * All service event listeners.
   */
  private ServiceListenerState serviceListeners = new ServiceListenerState();

  /**
   * Handle to secure call class.
   */
  private PermissionOps secure;


  Listeners(PermissionOps perm) {
    secure = perm;
  }


  /**
   * Add a bundle listener to the current framework.
   *
   * @param bundle Who wants to add listener.
   * @param listener Object to add.
   */
  void addBundleListener(Bundle bundle, BundleListener listener) {
    ListenerEntry le = new ListenerEntry(bundle, listener);
    if (listener instanceof SynchronousBundleListener) {
      secure.checkListenerAdminPerm(bundle);
      synchronized (syncBundleListeners) {
    	  syncBundleListeners.add(le);
      }
    } 
    else {
      synchronized (bundleListeners) {
    	  bundleListeners.add(le);
      }
    }
  }


  /**
   * Remove bundle listener from current framework. Silently ignore
   * if listener doesn't exist. If listener is registered more than
   * once remove one instances.
   *
   * @param bundle Who wants to remove listener.
   * @param listener Object to remove.
   */
  void removeBundleListener(Bundle bundle, BundleListener listener) {
    ListenerEntry le = new ListenerEntry(bundle, listener);
    if (listener instanceof SynchronousBundleListener) {
      synchronized (syncBundleListeners) {
        secure.checkListenerAdminPerm(bundle);
	syncBundleListeners.remove(le);
      }
    } else {
      synchronized (bundleListeners) {
	bundleListeners.remove(le);
      }
    }
  }


  /**
   * Add a bundle listener to current framework.
   *
   * @param bundle Who wants to add listener.
   * @param listener Object to add.
   */
  void addFrameworkListener(Bundle bundle, FrameworkListener listener) {
    ListenerEntry le = new ListenerEntry(bundle, listener);
    synchronized (frameworkListeners) {
      frameworkListeners.add(le);
    }
  }


  /**
   * Remove framework listener from current framework. Silently ignore
   * if listener doesn't exist. If listener is registered more than
   * once remove all instances.
   *
   * @param bundle Who wants to remove listener.
   * @param listener Object to remove.
   */
  void removeFrameworkListener(Bundle bundle, FrameworkListener listener) {
    synchronized (frameworkListeners) {
      frameworkListeners.remove(new ListenerEntry(bundle, listener));
    }
  }


  /**
   * Add a service listener with filter to current framework.
   * If no filter is wanted, call with filter param set to null.
   *
   * @param bundle Who wants to add listener.
   * @param listener Object to add.
   * @param filter LDAP String used for filtering event before calling listener.
   */
  void addServiceListener(Bundle bundle, ServiceListener listener, String filter)
    throws InvalidSyntaxException {
    serviceListeners.add(bundle, listener, filter);
  }


  /**
   * Remove service listener from current framework. Silently ignore
   * if listener doesn't exist. If listener is registered more than
   * once remove all instances.
   *
   * @param bundle Who wants to remove listener.
   * @param listener Object to remove.
   */
  void removeServiceListener(Bundle bundle, ServiceListener listener) {
    serviceListeners.remove(bundle, listener);
  }


  /**
   * Remove all listener registered by a bundle in the current framework.
   *
   * @param b Bundle which listeners we want to remove.
   */
  void removeAllListeners(Bundle b) {
    removeAllListeners(syncBundleListeners, b);
    removeAllListeners(bundleListeners, b);
    removeAllListeners(frameworkListeners, b);
    serviceListeners.removeAll(b);
  }


  /**
   * Convenience method for throwing framework error event.
   *
   * @param b Bundle which caused the error.
   * @param t Throwable generated.
   */
  void frameworkError(Bundle b, Throwable t) {
    frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, b, t));
  }


  /**
   * Convenience method for throwing framework info event.
   *
   * @param b Bundle which caused the error.
   * @param t Throwable generated.
   */
  void frameworkInfo(Bundle b, Throwable t) {
    frameworkEvent(new FrameworkEvent(FrameworkEvent.INFO, b, t));
  }

  //
  // BundleListener interface
  //

  /**
   * Receive notification that a bundle has had a change occur in its lifecycle.
   *
   * @see org.osgi.framework.BundleListener#bundleChanged
   */
  public void bundleChanged(final BundleEvent evt) {
    ListenerEntry [] bl, tmp;
    int type = evt.getType();
    if(type == BundleEvent.STARTING || type == BundleEvent.STOPPING){
    	synchronized (syncBundleListeners) {
    		bl = new ListenerEntry[syncBundleListeners.size()];
    		syncBundleListeners.toArray(bl);
    	}
    }
    else{
    	synchronized (bundleListeners) {
    		tmp = new ListenerEntry[bundleListeners.size()];
    	    bundleListeners.toArray(tmp);
    	}
    	synchronized (syncBundleListeners) {
    		bl = new ListenerEntry[tmp.length + syncBundleListeners.size()];
    	    syncBundleListeners.toArray(bl);
    	}
    	System.arraycopy(tmp, 0, bl, bl.length - tmp.length, tmp.length);
    }
    
    for (int i = 0; i < bl.length; i++) {
      final ListenerEntry l = bl[i];
      try {
        secure.callBundleChanged((BundleListener)l.listener, evt);
      } catch (Throwable pe) {
	frameworkError(l.bundle, pe);
      }
    }
  }

  //
  // FrameworkListener interface
  //

  /**
   * Receive notification of a general framework event.
   *
   * @see org.osgi.framework.FrameworkListener#frameworkEvent
   */
  public void frameworkEvent(final FrameworkEvent evt) {
    if (Debug.errors) {
      if (evt.getType() == FrameworkEvent.ERROR) {
	Debug.println("errors - FrameworkErrorEvent bundle #" + evt.getBundle().getBundleId());
	Debug.printStackTrace("errors - FrameworkErrorEvent throwable: ", evt.getThrowable());
      }
    }
    ListenerEntry [] fl;
    synchronized (frameworkListeners) {
      fl = new ListenerEntry[frameworkListeners.size()];
      frameworkListeners.toArray(fl);
    }
    for (int i = 0; i < fl.length; i++) {
      final ListenerEntry l = fl[i];
      try {
        secure.callFrameworkEvent((FrameworkListener)l.listener, evt);
      } catch (Throwable pe) {
	// Don't report Error events again, since probably would go into an infinite loop.
	if (evt.getType() != FrameworkEvent.ERROR) {
	  frameworkError(l.bundle, pe);
	}
      }
    }
  }

  //
  // ServiceListener interface
  //

  /**
   * Receive notification that a service has had a change occur in its lifecycle.
   *
   * @see org.osgi.framework.ServiceListener#serviceChanged
   */
  public void serviceChanged(final ServiceEvent evt) {
    ServiceReferenceImpl sr = (ServiceReferenceImpl)evt.getServiceReference();
    String[] classes = (String[])sr.getProperty(Constants.OBJECTCLASS);
    Set sl = serviceListeners.getMatchingListeners(sr);
    int n = 0;
    for (Iterator it = sl.iterator(); it.hasNext(); n++) {
      final ServiceListenerEntry l = (ServiceListenerEntry)it.next();
      boolean testAssignable = false;
      if(!(l.listener instanceof AllServiceListener)){
        testAssignable = true;
      }
      try {
        int length = classes.length;
        for (int i = 0; i < length; i++) {
          if(testAssignable && !sr.isAssignableTo(l.bundle, classes[i])){
            continue;
          }
          if (l.bundle.hasPermission(new ServicePermission(classes[i], 
                                                           ServicePermission.GET))) {
            try {
              secure.callServiceChanged((ServiceListener)l.listener, evt);
            } catch (Throwable pe) {
              frameworkError(l.bundle, pe);
            }
            break;
          }
        }//for
      } catch (Exception le) {
        frameworkError(l.bundle, le);
      }
    }//for
    if (Debug.ldap) {
      Debug.println("Notified " + n + " listeners");
    }
  }

  //
  // Private methods
  //

  /**
   * Remove all listener registered by a bundle in specified list within
   * the current framework. Silently ignore if listener doesn't exist.
   *
   * @param s Which set to remove from bundle, framework or service.
   * @param b Bundle which listeners we want to remove.
   */
  private void removeAllListeners(Set s, Bundle b) {
    synchronized (s) {
      for (Iterator i = s.iterator(); i.hasNext();) {
	if (((ListenerEntry)i.next()).bundle == b) {
	  i.remove();
	}
      }
    }
  }

  static boolean nocacheldap = 
    "true".equals(System.getProperty("org.knopflerfish.framework.ldap.nocache"));
}

/**
 * Data structure for saving listener info. Contains bundle
 * who registered listener, listener object and if a service
 * listener any associated filter.
 */
class ListenerEntry {
  Bundle bundle;
  EventListener listener;

  ListenerEntry(Bundle b, EventListener l) {
    bundle = b;
    listener = l;
  }

  public boolean equals(Object o) {
    if (o instanceof ListenerEntry) {
      return bundle == ((ListenerEntry)o).bundle &&
	listener == ((ListenerEntry)o).listener;
    }
    return false;
  }

  public int hashCode() {
    return bundle.hashCode();
  }
}

/**
 * Data structure for saving service listener info. Contains
 * the optional service listener filter, in addition to the info 
 * in ListenerEntry.
 */
class ServiceListenerEntry extends ListenerEntry {
  LDAPExpr ldap;

  /**
   * The elements of "simple" filters are cached, for easy lookup.
   * 
   * The grammar for simple filters is as follows:
   *
   * <pre>
   * Simple = '(' attr '=' value ')'
   *        | '(' '|' Simple+ ')'
   * </pre>
   * where <code>attr</code> is one of {@link Constants#OBJECTCLASS}, 
   * {@link Constants#SERVICE_ID} or {@link Constants#SERVICE_PID}, and
   * <code>value</code> must not contain a wildcard character.
   * <p>
   * The index of the vector determines which key the cache is for
   * (see {@link ServiceListenerState#hashedKeys}). For each key, there is
   * a vector pointing out the values which are accepted by this
   * ServiceListenerEntry's filter. This cache is maintained to make
   * it easy to remove this service listener.
   */
  List[] local_cache;

  ServiceListenerEntry(Bundle b, EventListener l, String filter) 
    throws InvalidSyntaxException {
    super(b, l);
    if (filter != null) {
      ldap = new LDAPExpr(filter);
    } else {
      ldap = null;
    }
  }

  ServiceListenerEntry(Bundle b, EventListener l) {
    super(b, l);
    ldap = null;
  }

}

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

  ServiceListenerState() {
    hashedKeysV = new ArrayList();
    for (int i = 0; i < hashedKeys.length; i++) {
      hashedKeysV.add(hashedKeys[i]);    
      cache[i] = new HashMap();
    }
  }

  /**
   * Add a new service listener. If an old one exists, and it has the
   * same owning bundle, the old listener is removed first.
   *
   * @param bundle The bundle adding this listener.
   * @param listener The service listener to add.
   * @param filter An LDAP filter string to check when a service is modified.
   * @exception org.osgi.framework.InvalidSyntaxException
   * If the filter is not a correct LDAP expression.
   */
  synchronized void add(Bundle bundle, ServiceListener listener, String filter)
  throws InvalidSyntaxException {
    ServiceListenerEntry sle = new ServiceListenerEntry(bundle, listener, filter);
    if (serviceSet.contains(sle)) {
      remove(bundle, listener);
    }
    serviceSet.add(sle);
    checkSimple(sle);
  }

  /**
   * Remove a service listener.
   * 
   * @param bundle The bundle removing this listener.
   * @param listener The service listener to remove.
   */
  synchronized void remove(Bundle bundle, ServiceListener listener) {
    for (Iterator it = serviceSet.iterator(); it.hasNext();) {
      ServiceListenerEntry sle = (ServiceListenerEntry)it.next();
      if (sle.bundle == bundle && sle.listener == listener) {
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
   * Remove all service listeners registered by the specified bundle.
   *
   * @param bundle The bundle to remove listeners for.
   */
  synchronized void removeAll(Bundle bundle) {
    for (Iterator it = serviceSet.iterator(); it.hasNext();) {
      ServiceListenerEntry sle = (ServiceListenerEntry)it.next();
      if (sle.bundle == bundle) {
	removeFromCache(sle);
	it.remove();
      }
    }
  }

  /**
   * Checks if the specified service listener's filter is simple enough 
   * to cache.
   */
  public void checkSimple(ServiceListenerEntry sle) {
    if (sle.ldap == null || Listeners.nocacheldap) {
      complicatedListeners.add(sle);
    } else {
      List[] /* Value */ local_cache = new List[hashedKeys.length];
      if (sle.ldap.isSimple(hashedKeysV, local_cache)) {
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
	if (Debug.ldap)
	  Debug.println("Too complicated filter: " + sle.ldap);
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
    if (Debug.ldap) {
      Debug.println("Added " + set.size() + " out of " + n + " listeners with complicated filters");
    }
    // Check the cache
    String[] c = (String[])sr.getProperty(Constants.OBJECTCLASS);
    for (int i = 0; i < c.length; i++) {
      if (Debug.ldap) {
	System.err.print("objectclass matches: ");
      }
      addToSet(set, (List)cache[OBJECTCLASS_IX].get(c[i]));
    }
    Long service_id = (Long)sr.getProperty(Constants.SERVICE_ID);
    if (service_id != null) {
      if (Debug.ldap) {
	System.err.print("service_id matches: ");
      }
      addToSet(set, (List)cache[SERVICE_ID_IX].get(service_id.toString()));
    }
    Object service_pid = sr.getProperty(Constants.SERVICE_PID);
    if (service_pid != null && service_pid instanceof String) {
      if (Debug.ldap) {
	System.err.print("service_pid matches: ");
      }
      addToSet(set, (List)cache[SERVICE_PID_IX].get(service_pid));
    }
    return set;
  }

  /**
   * Add all members of the specified list to the specified set.
   */
  private void addToSet(Set set, List l) {
    if (l != null) {
      if (Debug.ldap) {
	Debug.println(Integer.toString(l.size()));
      }
      for (Iterator it = l.iterator(); it.hasNext();) {
	set.add(it.next());
      }
    } else {
      if (Debug.ldap) { 
	Debug.println("0");
      }
    }
  }
}
