/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
 * Here we handle all listeners that bundles have registered.
 *
 * @author Jan Stein, Philippe Laporte, Gunnar Ekolin
 */
class Listeners {

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
  ServiceListenerState serviceListeners;

  /**
   * Handle to secure call class.
   */
  private PermissionOps secure;

  FrameworkContext framework;

  boolean nocacheldap;

  Listeners(FrameworkContext framework, PermissionOps perm) {
    this.framework = framework;
    secure = perm;
    nocacheldap = "true".equals(framework.props.getProperty("org.knopflerfish.framework.ldap.nocache"));
    serviceListeners = new ServiceListenerState(this);
  }

  void clear()
  {
    bundleListeners.clear();
    syncBundleListeners.clear();
    frameworkListeners.clear();
    serviceListeners.clear();
    secure = null;
    framework = null;
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

  /**
   * Receive notification that a bundle has had a change occur in its lifecycle.
   *
   * @see org.osgi.framework.BundleListener#bundleChanged
   */
  void bundleChanged(final BundleEvent evt) {
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


  /**
   * Receive notification of a general framework event.
   *
   * @see org.osgi.framework.FrameworkListener#frameworkEvent
   */
  void frameworkEvent(final FrameworkEvent evt) {
    if (framework.props.debug.errors) {
      if (evt.getType() == FrameworkEvent.ERROR) {
        framework.props.debug.println("errors - FrameworkErrorEvent bundle #" + evt.getBundle().getBundleId());
        framework.props.debug.printStackTrace("errors - FrameworkErrorEvent throwable: ", evt.getThrowable());
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


  /**
   * Receive notification that a service has had a change occur in its lifecycle.
   *
   * @see org.osgi.framework.ServiceListener#serviceChanged
   */
  void serviceChanged(final Collection receivers,
                      final ServiceEvent evt,
                      final Set matchBefore) {
    ServiceReferenceImpl sr = (ServiceReferenceImpl)evt.getServiceReference();
    String[] classes = (String[])sr.getProperty(Constants.OBJECTCLASS);
    int n = 0;
    
    framework.hooks.filterServiceEventReceivers(evt, receivers);

    for (Iterator it = receivers.iterator(); it.hasNext(); n++) {
      final ServiceListenerEntry l = (ServiceListenerEntry)it.next();
      if (matchBefore != null) {
        matchBefore.remove(l);
      }
      boolean testAssignable = false;
      if(!(l.listener instanceof AllServiceListener)){
        testAssignable = true;
      }
      try {
        if (l.bundle.hasPermission(new ServicePermission(sr, ServicePermission.GET))) {
          for (int i = 0; i < classes.length; i++) {
            if(testAssignable && !sr.isAssignableTo(l.bundle, classes[i])){
              continue;
            }
            try {
              secure.callServiceChanged((ServiceListener)l.listener, evt);
            } catch (Throwable pe) {
              frameworkError(l.bundle, pe);
            }
            break;
          }
        }
      } catch (Exception le) {
        frameworkError(l.bundle, le);
      }
    }
    if (framework.props.debug.ldap) {
      framework.props.debug.println("Notified " + n + " listeners");
    }
  }


  /**
   *
   *
   */
  Set getMatchingServiceListeners(final ServiceReference sr) {
    return serviceListeners.getMatchingListeners((ServiceReferenceImpl)sr);
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

}
