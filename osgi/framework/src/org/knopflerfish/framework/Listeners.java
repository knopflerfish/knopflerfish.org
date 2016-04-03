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

import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;

/**
 * Here we handle all listeners that bundles have registered.
 *
 * @author Jan Stein, Philippe Laporte, Gunnar Ekolin
 */
class Listeners {

  /**
   * All bundle event listeners.
   */
  HashSet<ListenerEntry> bundleListeners = new HashSet<ListenerEntry>();
  HashSet<ListenerEntry> syncBundleListeners = new HashSet<ListenerEntry>();

  /**
   * Init framework event listeners.
   */
  private FrameworkListener [] initListeners;

  /**
   * All framework event listeners.
   */
  private final HashSet<ListenerEntry> frameworkListeners = new HashSet<ListenerEntry>();

  /**
   * All service event listeners.
   */
  ServiceListenerState serviceListeners;

  /**
   * Queue of async events to deliver
   */
  private LinkedList<AsyncEvent> asyncEventQueue = null;

  /**
   * All threads for delivering async events
   */
  private AsyncEventThread [] threads = null;

  /**
   * Map of active listeners to thread.
   */
  private HashMap<ListenerEntry, Thread> activeListeners = null;

  /**
   * Handle to secure call class.
   */
  private PermissionOps secure;

  FrameworkContext fwCtx;

  boolean nocacheldap;

  volatile boolean quit = false;


  Listeners(FrameworkContext framework, PermissionOps perm, FrameworkListener... initListeners) {
    this.fwCtx = framework;
    secure = perm;
    nocacheldap = framework.props.getBooleanProperty(FWProps.LDAP_NOCACHE_PROP);
    serviceListeners = new ServiceListenerState(this);
    final String ets = framework.props.getProperty(FWProps.LISTENER_N_THREADS_PROP);
    int n_threads = 1;
    Throwable error = null;
    if (ets != null) {
      try {
        n_threads = Integer.parseInt(ets);
      } catch (final NumberFormatException nfe) {
        error = nfe;
      }
    }
    this.initListeners = initListeners;
    if (n_threads > 0) {
      asyncEventQueue = new LinkedList<AsyncEvent>();
      threads = new AsyncEventThread[n_threads];
      for (int i = 0; i < n_threads; i++) {
        threads[i] = new AsyncEventThread(i);
        threads[i].start();
      }
      if (n_threads > 1) {
        activeListeners = new HashMap<ListenerEntry, Thread>();
      }
      if (error != null) {
        Throwable t = new Exception("Failed to parse " + FWProps.LISTENER_N_THREADS_PROP, error);
        framework.frameworkWarning(framework.systemBundle, t);
      }
    }
  }


  void clear()
  {
    bundleListeners.clear();
    syncBundleListeners.clear();
    frameworkListeners.clear();
    serviceListeners.clear();
    initListeners = null;
    secure = null;
    fwCtx = null;
  }


  void initDone()
  {
    initListeners = null;
  }


  /**
   * Add a bundle listener to the current framework.
   *
   * @param bundle Who wants to add listener.
   * @param listener Object to add.
   */
  void addBundleListener(BundleContextImpl bc, BundleListener listener) {
    final ListenerEntry le = new ListenerEntry(bc, listener);
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
    final ListenerEntry le = new ListenerEntry(bc, listener);
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
   * @param bc Who wants to add listener.
   * @param listener Object to add.
   */
  void addFrameworkListener(BundleContextImpl bc, FrameworkListener listener) {
    final ListenerEntry le = new ListenerEntry(bc, listener);
    synchronized (frameworkListeners) {
      frameworkListeners.add(le);
    }
  }


  /**
   * Remove framework listener from current framework. Silently ignore
   * if listener doesn't exist. If listener is registered more than
   * once remove all instances.
   *
   * @param bc Who wants to remove listener.
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
   * @param bi Bundle which listeners we want to remove.
   */
  void removeAllListeners(BundleContextImpl bc) {
    removeAllListeners(syncBundleListeners, bc);
    removeAllListeners(bundleListeners, bc);
    removeAllListeners(frameworkListeners, bc);
    serviceListeners.removeAll(bc);
  }



  /**
   * Receive notification that a bundle has had a change occur in its lifecycle.
   * NOTE! Must be called with AllPermission!?
   *
   * @see org.osgi.framework.BundleListener#bundleChanged
   */
  void bundleChanged(final BundleEvent evt) {
    final HashSet<ListenerEntry> filteredSyncBundleListeners = new HashSet<ListenerEntry>();
    HashSet<ListenerEntry> filteredBundleListeners = null;


    final int type = evt.getType();

    if (type != BundleEvent.LAZY_ACTIVATION &&
        type != BundleEvent.STARTING &&
        type != BundleEvent.STOPPING) {
      filteredBundleListeners = new HashSet<ListenerEntry>();
    }

    fwCtx.bundleHooks.filterBundleEventReceivers(
        evt,
        filteredSyncBundleListeners,
        filteredBundleListeners);

    for(final ListenerEntry le : filteredSyncBundleListeners) {
      bundleChanged(le, evt);
    }
    if (filteredBundleListeners != null) {
      if (asyncEventQueue != null) {
        synchronized (asyncEventQueue) {
          for(final ListenerEntry le : filteredBundleListeners) {
            asyncEventQueue.addLast(new AsyncEvent(le, evt));
          }
          asyncEventQueue.notify();
        }
      } else {
        for(final ListenerEntry le : filteredBundleListeners) {
          bundleChanged(le, evt);
        }
      }
    }
  }


  /**
   * Receive notification of a general framework event.
   *
   * @see org.osgi.framework.FrameworkListener#frameworkEvent
   */
  void frameworkEvent(final FrameworkEvent evt, FrameworkListener... oneTimeListeners) {
    if (fwCtx.debug.errors) {
      if (evt.getType() == FrameworkEvent.ERROR) {
        fwCtx.debug.println("errors - FrameworkErrorEvent bundle #" +
                                evt.getBundle().getBundleId());
        fwCtx.debug.printStackTrace("errors - FrameworkErrorEvent throwable: ",
                                        evt.getThrowable());
      }
    }
    if (fwCtx.debug.warnings) {
      if (evt.getType() == FrameworkEvent.WARNING) {
        fwCtx.debug.println("warnings - FrameworkErrorEvent bundle #" +
                                evt.getBundle().getBundleId());
        fwCtx.debug.printStackTrace("warnings - FrameworkErrorEvent throwable: ",
                                        evt.getThrowable());
      }
    }
    if (fwCtx.debug.startlevel) {
      if (evt.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
        fwCtx.debug
            .println("startlevel: FrameworkEvent Startlevel Changed");
      } else if (evt.getType() == FrameworkEvent.STARTED) {
        fwCtx.debug.println("startlevel: FrameworkEvent Started");
      }
    }

    if (asyncEventQueue != null) {
      synchronized (asyncEventQueue) {
        if (oneTimeListeners!=null) {
          for (final FrameworkListener fl : oneTimeListeners) {
            asyncEventQueue.addLast(new AsyncEvent(new ListenerEntry(null, fl), evt));
          }
        }
        if (initListeners != null) {
          for (FrameworkListener il : initListeners) {
            asyncEventQueue.addLast(new AsyncEvent(new ListenerEntry(null, il), evt));
          }
        }
        synchronized (frameworkListeners) {
          for (final ListenerEntry listenerEntry : frameworkListeners) {
            asyncEventQueue.addLast(new AsyncEvent(listenerEntry, evt));
          }
        }
        asyncEventQueue.notify();
      }
    } else {
      if (oneTimeListeners != null) {
        for (final FrameworkListener ofl : oneTimeListeners) {
          frameworkEvent(new ListenerEntry(null, ofl), evt);
        }
      }
      if (initListeners != null) {
        for (FrameworkListener il : initListeners) {
          frameworkEvent(new ListenerEntry(null, il), evt);
        }
      }
      ListenerEntry [] fl;
      synchronized (frameworkListeners) {
        fl = new ListenerEntry[frameworkListeners.size()];
        frameworkListeners.toArray(fl);
      }
      for (final ListenerEntry element : fl) {
        frameworkEvent(element, evt);
      }
    }
  }


  /**
   * Receive notification that a service has had a change occur in its lifecycle.
   *
   * @see org.osgi.framework.ServiceListener#serviceChanged
   */
  void serviceChanged(final Collection<ServiceListenerEntry> receivers,
                      final ServiceEvent evt,
                      final Set<ServiceListenerEntry> matchBefore) {
    final ServiceReferenceImpl<?> sr = (ServiceReferenceImpl<?>)evt.getServiceReference();
    final String[] classes = (String[])sr.getProperty(Constants.OBJECTCLASS);
    final int n = 0;

    // TODO: OSGi43 the interplay between ldap filters, hooks and MODIFIED_ENDMATCH should be revised
    if (matchBefore != null) {
      for (final ServiceListenerEntry l : receivers) {
        matchBefore.remove(l);
      }
    }

    fwCtx.serviceHooks.filterServiceEventReceivers(evt, receivers);

    for (final ServiceListenerEntry l : receivers) {
      try {
        if (!l.isRemoved()
            && (!secure.checkPermissions() ||
                l.bc.bundle.hasPermission(new ServicePermission(sr, ServicePermission.GET)))) {
          final boolean testAssignable = !(l.listener instanceof AllServiceListener);
          for (int i = 0; i < classes.length; i++) {
            if (testAssignable && !sr.isAssignableTo(l.bc.bundle, classes[i])){
              continue;
            }
            try {
              ((ServiceListener)l.listener).serviceChanged(evt);
            } catch (final Throwable pe) {
              fwCtx.frameworkError(l.bc, pe);
            }
            break;
          }
        }
      } catch (final IllegalStateException ignore) {
        // Bundle got UNINSTALLED, skip it
      }
    }
    if (fwCtx.debug.ldap) {
      fwCtx.debug.println("Notified " + n + " listeners");
    }
  }


  /**
   *
   *
   */
  Set<ServiceListenerEntry> getMatchingServiceListeners(final ServiceReference<?> sr) {
    return serviceListeners.getMatchingListeners((ServiceReferenceImpl<?>)sr);
  }

  //
  // Private methods
  //

  /**
   * Remove all listener registered by a bundle in specified list within
   * the current framework. Silently ignore if listener doesn't exist.
   *
   * @param s Which set to remove from bundle, framework or service.
   * @param bc Bundle which listeners we want to remove.
   */
  private void removeAllListeners(Set<ListenerEntry> s, BundleContext bc) {
    synchronized (s) {
      for (final Iterator<ListenerEntry> i = s.iterator(); i.hasNext();) {
        if (i.next().bc == bc) {
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
    } catch (final Throwable pe) {
      fwCtx.frameworkError(le.bc, pe);
    }
  }


  /**
   *
   */
  private void frameworkEvent(final ListenerEntry le, FrameworkEvent evt) {
    try {
      ((FrameworkListener)le.listener).frameworkEvent(evt);
    } catch (final Exception pe) {
      // Don't report Error events again, since probably would go into an infinite loop.
      if (evt.getType() != FrameworkEvent.ERROR) {
        fwCtx.frameworkError(le != null ? le.bc : null, pe);
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
      super(fwCtx.threadGroup, "AsyncEventThread#" + i);
    }


    @Override
    public void run() {
      while (true) {
        AsyncEvent ae;

        synchronized (asyncEventQueue) {
          while (!quit && asyncEventQueue.isEmpty()) {
            try {
              asyncEventQueue.wait();
            } catch (final InterruptedException ignored) { }
          }
          if (quit) {
            break;
          }
          ae = asyncEventQueue.removeFirst();
        }

        if (activeListeners != null) {
          synchronized (activeListeners) {
            while (activeListeners.containsKey(ae.le)) {
              // TODO, implement detection of hanging listeners?
              try {
                activeListeners.wait();
              } catch (final InterruptedException ignore) { }
            }
            activeListeners.put(ae.le, Thread.currentThread());
          }
        }
        // Either an unregistered one-time listener or the bundle
        // owning the listener must be valid.
        if (ae.le.bc==null || ae.le.bc.isValid()) {
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
