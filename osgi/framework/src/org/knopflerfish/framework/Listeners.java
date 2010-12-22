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
   * Queue of async events to deliver
   */
  private LinkedList asyncEventQueue = null;

  /**
   * All threads for delivering async events
   */
  private AsyncEventThread [] threads = null;

  /**
   * Map of active listeners to thread.
   */
  private HashMap activeListeners = null;

  /**
   * Handle to secure call class.
   */
  private PermissionOps secure;

  FrameworkContext framework;

  boolean nocacheldap;

  volatile boolean quit = false;


  Listeners(FrameworkContext framework, PermissionOps perm) {
    this.framework = framework;
    secure = perm;
    nocacheldap = framework.props.getBooleanProperty(FWProps.LDAP_NOCACHE_PROP);
    serviceListeners = new ServiceListenerState(this);
    String ets = framework.props.getProperty(FWProps.LISTENER_N_THREADS_PROP);
    int n_threads = 1;
    if (ets != null) {
      try {
        n_threads = Integer.parseInt(ets);
      } catch (NumberFormatException nfe) {
        // NYI, report error
      }
    }
    if (n_threads > 0) {
      asyncEventQueue = new LinkedList();
      threads = new AsyncEventThread[n_threads];
      for (int i = 0; i < n_threads; i++) {
        threads[i] = new AsyncEventThread(i);
        threads[i].start();
      }
      if (n_threads > 1) {
        activeListeners = new HashMap();
      }
    }
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
  void addBundleListener(BundleContextImpl bc, BundleListener listener) {
    ListenerEntry le = new ListenerEntry(bc, listener);
    if (listener instanceof SynchronousBundleListener) {
      secure.checkListenerAdminPerm(bc.bundle);
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
  void removeBundleListener(BundleContextImpl bc, BundleListener listener) {
    ListenerEntry le = new ListenerEntry(bc, listener);
    if (listener instanceof SynchronousBundleListener) {
      synchronized (syncBundleListeners) {
        secure.checkListenerAdminPerm(bc.bundle);
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
  void addFrameworkListener(BundleContextImpl bc, FrameworkListener listener) {
    ListenerEntry le = new ListenerEntry(bc, listener);
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
  void removeFrameworkListener(BundleContextImpl bc, FrameworkListener listener) {
    synchronized (frameworkListeners) {
      frameworkListeners.remove(new ListenerEntry(bc, listener));
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
  void addServiceListener(BundleContextImpl bc, ServiceListener listener, String filter)
    throws InvalidSyntaxException {
    serviceListeners.add(bc, listener, filter);
  }


  /**
   * Remove service listener from current framework. Silently ignore
   * if listener doesn't exist. If listener is registered more than
   * once remove all instances.
   *
   * @param bundle Who wants to remove listener.
   * @param listener Object to remove.
   */
  void removeServiceListener(BundleContextImpl bc, ServiceListener listener) {
    serviceListeners.remove(bc, listener);
  }


  /**
   * Remove all listener registered by a bundle in the current framework.
   *
   * @param b Bundle which listeners we want to remove.
   */
  void removeAllListeners(BundleContextImpl bc) {    
    removeAllListeners(syncBundleListeners, bc);
    removeAllListeners(bundleListeners, bc);
    removeAllListeners(frameworkListeners, bc);
    serviceListeners.removeAll(bc);
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
   * Convenience method for throwing framework error event.
   *
   * @param bc BundleContext for bundle which caused the error.
   * @param t Throwable generated.
   */
  void frameworkError(BundleContextImpl bc, Throwable t) {
    frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bc.bundle, t));
  }


  /**
   * Convenience method for throwing framework info event.
   *
   * @param b Bundle which caused the throwable.
   * @param t Throwable generated.
   */
  void frameworkInfo(Bundle b, Throwable t) {
    frameworkEvent(new FrameworkEvent(FrameworkEvent.INFO, b, t));
  }


  /**
   * Convenience method for throwing framework warning event.
   *
   * @param b Bundle which caused the throwable.
   * @param t Throwable generated.
   */
  void frameworkWarning(Bundle b, Throwable t) {
    frameworkEvent(new FrameworkEvent(FrameworkEvent.WARNING, b, t));
  }


  /**
   * Receive notification that a bundle has had a change occur in its lifecycle.
   * NOTE! Must be called with AllPermission!?
   *
   * @see org.osgi.framework.BundleListener#bundleChanged
   */
  void bundleChanged(final BundleEvent evt) {
    ListenerEntry [] sbl, bl = null;
    int type = evt.getType();
    synchronized (syncBundleListeners) {
      sbl = new ListenerEntry[syncBundleListeners.size()];
      syncBundleListeners.toArray(sbl);
    }
    if (type != BundleEvent.LAZY_ACTIVATION &&
        type != BundleEvent.STARTING &&
        type != BundleEvent.STOPPING) {
      synchronized (bundleListeners) {
        bl = new ListenerEntry[bundleListeners.size()];
        bundleListeners.toArray(bl);
      }
    }
    for (int i = 0; i < sbl.length; i++) {
      bundleChanged(sbl[i], evt);
    }
    if (bl != null) {
      if (asyncEventQueue != null) {
        synchronized (asyncEventQueue) {
          for (int i = 0; i < bl.length; i++) {
            asyncEventQueue.addLast(new AsyncEvent(bl[i], evt));
          }
          asyncEventQueue.notify();
        }
      } else {
        for (int i = 0; i < bl.length; i++) {
          bundleChanged(bl[i], evt);
        }
      }
    }
  }


  /**
   * Receive notification of a general framework event.
   *
   * @see org.osgi.framework.FrameworkListener#frameworkEvent
   */
  void frameworkEvent(final FrameworkEvent evt) {
    if (framework.debug.errors) {
      if (evt.getType() == FrameworkEvent.ERROR) {
        framework.debug.println("errors - FrameworkErrorEvent bundle #" +
                                evt.getBundle().getBundleId());
        framework.debug.printStackTrace("errors - FrameworkErrorEvent throwable: ",
                                        evt.getThrowable());
      }
    }
    if (framework.debug.warnings) {
      if (evt.getType() == FrameworkEvent.WARNING) {
        framework.debug.println("warnings - FrameworkErrorEvent bundle #" +
                                evt.getBundle().getBundleId());
        framework.debug.printStackTrace("warnings - FrameworkErrorEvent throwable: ",
                                        evt.getThrowable());
      }
    }
    if (asyncEventQueue != null) {
      synchronized (asyncEventQueue) {
        synchronized (frameworkListeners) {
          for (Iterator i = frameworkListeners.iterator(); i.hasNext(); ) {
            asyncEventQueue.addLast(new AsyncEvent((ListenerEntry)i.next(), evt));
          }
          asyncEventQueue.notify();
        }
      }
    } else {
      ListenerEntry [] fl;
      synchronized (frameworkListeners) {
        fl = new ListenerEntry[frameworkListeners.size()];
        frameworkListeners.toArray(fl);
      }
      for (int i = 0; i < fl.length; i++) {
        frameworkEvent(fl[i], evt);
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
      try {
        if (!l.isRemoved()
            && l.bc.bundle.hasPermission(new ServicePermission(sr, ServicePermission.GET))) {
          boolean testAssignable = !(l.listener instanceof AllServiceListener);
          for (int i = 0; i < classes.length; i++) {
            if (testAssignable && !sr.isAssignableTo(l.bc.bundle, classes[i])){
              continue;
            }
            try {
              ((ServiceListener)l.listener).serviceChanged(evt);
            } catch (Throwable pe) {
              frameworkError(l.bc, pe);
            }
            break;
          }
        }
      } catch (IllegalStateException ignore) {
        // Bundle got UNINSTALLED, skip it
      }
    }
    if (framework.debug.ldap) {
      framework.debug.println("Notified " + n + " listeners");
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
  private void removeAllListeners(Set s, BundleContext bc) {
    synchronized (s) {
      for (Iterator i = s.iterator(); i.hasNext();) {
        if (((ListenerEntry)i.next()).bc == bc) {
          i.remove();
        }
      }
    }
  }


  /**
   *
   */
  private void bundleChanged(final ListenerEntry le, final BundleEvent evt) {
    try {
      ((BundleListener)le.listener).bundleChanged(evt);
    } catch (Throwable pe) {
      frameworkError(le.bc, pe);
    }
  }


  /**
   *
   */
  private void frameworkEvent(final ListenerEntry le, FrameworkEvent evt) {
    try {
      ((FrameworkListener)le.listener).frameworkEvent(evt);
    } catch (Exception pe) {
      // Don't report Error events again, since probably would go into an infinite loop.
      if (evt.getType() != FrameworkEvent.ERROR) {
        frameworkError(le.bc, pe);
      }
    }
  }

  //
  // Private classes
  //

  static class AsyncEvent {
    final ListenerEntry le;
    final EventObject evt;

    AsyncEvent(ListenerEntry le, EventObject evt) {
      this.le = le;
      this.evt = evt;
    }
  }


  /**
   * Thread that deliver asynchronous events.
   */
  private class AsyncEventThread extends Thread {

    AsyncEventThread(int i) {
      super(framework.threadGroup, "AsyncEventThread#" + i);
    }


    public void run() {
      while (true) {
        AsyncEvent ae;

        synchronized (asyncEventQueue) {
          while (!quit && asyncEventQueue.isEmpty()) {
            try {
              asyncEventQueue.wait();
            } catch (InterruptedException ignored) { }
          }
          if (quit) {
            break;
          }
          ae = (AsyncEvent) asyncEventQueue.removeFirst();
        }

        if (activeListeners != null) {
          synchronized (activeListeners) {
            while (activeListeners.containsKey(ae.le)) {
              // TBD, implement detection of hanging listeners?
              try {
                activeListeners.wait();
              } catch (InterruptedException ignore) { }
            }
            activeListeners.put(ae.le, Thread.currentThread());
          }
        }
        if (ae.le.bc.isValid()) {
          if (ae.evt instanceof BundleEvent) {
            bundleChanged(ae.le, (BundleEvent)ae.evt);
          } else {
            frameworkEvent(ae.le, (FrameworkEvent)ae.evt);
          }
        }
        if (activeListeners != null) {
          synchronized (activeListeners) {
            activeListeners.remove(ae.le);
            activeListeners.notifyAll();
          }
        }
      }
    }
  }

}
