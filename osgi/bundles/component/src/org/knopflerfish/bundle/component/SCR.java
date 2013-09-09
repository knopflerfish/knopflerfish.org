/*
 * Copyright (c) 2006-2013, KNOPFLERFISH project
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Service Component Runtime
 */
class SCR implements SynchronousBundleListener, ConfigurationListener
{
  final private BundleContext bc;

  private final Set<Bundle> lazy = Collections.synchronizedSet(new HashSet<Bundle>());
  private final Hashtable<Bundle, Component[]> bundleComponents = new Hashtable<Bundle, Component[]>();
  private final Hashtable<String, Component[]> components = new Hashtable<String, Component[]>();
  private final Hashtable<String, Component[]> serviceComponents = new Hashtable<String, Component[]>();
  private final Hashtable<String, Component[]> configSubscriber = new Hashtable<String, Component[]>();
  private ServiceRegistration<ConfigurationListener> cmListener = null;
  private final ServiceTracker<ConfigurationAdmin,ConfigurationAdmin> cmAdminTracker;
  private long nextId = 0;
  private final HashMap<Thread, List<PostponedBind>> postponedBind = new HashMap<Thread, List<PostponedBind>>();
  private final HashMap<Thread, Integer> ppRef = new HashMap<Thread, Integer>();

  /**
   *
   */
  SCR(BundleContext bc) {
    this.bc = bc;
    cmAdminTracker = new ServiceTracker<ConfigurationAdmin,ConfigurationAdmin>(bc, ConfigurationAdmin.class, null);
  }


  /**
   * Start SCR.
   *
   */
  void start() {
    cmAdminTracker.open();
    cmListener = bc.registerService(ConfigurationListener.class, this, null);
    bc.addBundleListener(this);
    postponeCheckin();
    try {
      final Bundle [] bundles = bc.getBundles();
      for (final Bundle bundle : bundles) {
        if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
          processBundle(bundle);
        }
      }
    } finally {
      postponeCheckout();
    }
    bc.registerService(org.apache.felix.scr.ScrService.class.getName(),
                       new ScrServiceImpl(this), null);
  }


  /**
   * Stop SCR.
   *
   */
  void stop() {
    bc.removeBundleListener(this);
    if (cmListener != null) {
      cmListener.unregister();
      cmListener = null;
    }
    cmAdminTracker.close();
    final Bundle [] b = bundleComponents.keySet().toArray(new Bundle[bundleComponents.size()]);
    for (final Bundle element : b) {
      removeBundle(element, ComponentConstants.DEACTIVATION_REASON_DISPOSED);
    }
  }


  //
  // BundleListener method
  //

  /**
   * Process bundle components when it starts.
   */
  public void bundleChanged(BundleEvent event) {
    postponeCheckin();
    try {
      final Bundle bundle = event.getBundle();

      switch (event.getType()) {
      case BundleEvent.LAZY_ACTIVATION:
        lazy.add(bundle);
        processBundle(bundle);
        break;
      case BundleEvent.STARTED:
        if (!lazy.remove(bundle)) {
          processBundle(bundle);
        }
        break;
      case BundleEvent.STOPPING:
        lazy.remove(bundle);
        removeBundle(bundle, ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED);
        break;
      }
    } finally {
      postponeCheckout();
    }
  }

  //
  // ConfigurationListener method
  //

  /**
   *
   */
  public void configurationEvent(ConfigurationEvent evt) {
    postponeCheckin();
    try {
      final String factoryPid = evt.getFactoryPid();
      final String pid = evt.getPid();
      final Component [] comps = configSubscriber.get(factoryPid != null ? factoryPid : pid);
      if (comps != null) {
        switch (evt.getType()) {
        case ConfigurationEvent.CM_DELETED:
          for (final Component comp : comps) {
            if (factoryPid != null) {
              comp.cmConfigDeleted(pid);
            } else {
              Configuration c = getConfiguration(comp, null, null);
              if (c != null) {
                if (c.getPid().length() <= pid.length()) {
                  comp.cmConfigUpdated(comp.getCMPid(), c);
                }
              } else {
                comp.cmConfigDeleted(comp.getCMPid());
              }
            }
          }
          break;
        case ConfigurationEvent.CM_UPDATED:
          for (final Component comp : comps) {
            Configuration c = getConfiguration(comp, factoryPid, pid);
            if (c != null) {
              comp.cmConfigUpdated(factoryPid != null ? pid : comp.getCMPid(), c);
            }
          }
          break;
        case ConfigurationEvent.CM_LOCATION_CHANGED:
          break;
        default:
          Activator.logDebug("Unknown ConfigurationEvent type: " + evt.getType());
          break;
        }
      }
    } finally {
      postponeCheckout();
    }
  }

  //
  // Package methods
  //

  /**
   * Get next available component id
   *
   * @return Long component id.
   */
  synchronized Long getNextComponentId() {
    return new Long(nextId++);
  }

  /**
   * Check if bundle has service components and process them.
   *
   * @param b Bundle to check
   */
  void processBundle(Bundle b) {
    final String sc = b.getHeaders().get(ComponentConstants.SERVICE_COMPONENT);
    Activator.logDebug("Process header " + ComponentConstants.SERVICE_COMPONENT +
                       " for bundle#" + b.getBundleId() + ": " + sc);
    if (sc != null) {
      final ArrayList<String> entries = splitwords(sc);
      if (entries.size() == 0) {
        Activator.logError(b, "Header " + ComponentConstants.SERVICE_COMPONENT + " empty.", null);
        return;
      }
      XmlPullParser p;
      try {
        final XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(true);
        p = f.newPullParser();
      } catch (final XmlPullParserException xppe) {
        Activator.logError("Could not fina a XML parser", xppe);
        return;
      }
      final ArrayList<ComponentDescription> cds = new ArrayList<ComponentDescription>();
      for (String me : entries) {
        final int pos = me.lastIndexOf('/');
        final String path = pos > 0 ? me.substring(0, pos) : "/";
        final Enumeration<URL> e = b.findEntries(path, me.substring(pos+1), false);
        if (null != e) {
          while (e.hasMoreElements()) {
            final URL u = e.nextElement();
            InputStream is = null;
            try {
              is = u.openStream();
            } catch (final IOException ioe) {
              Activator.logError(b, "Failed to open: " + u, ioe);
              return;
            }
            try {
              p.setInput(is, null);
              Activator.logDebug("Parse component description: " + u);
              while (true) {
                try {
                  final ComponentDescription cd = ComponentDescription.parseComponent(b, p);
                  Activator.logDebug("Got component description: " + cd);
                  if (cd != null) {
                    cds.add(cd);
                  } else {
                    break;
                  }
                } catch (final XmlPullParserException pe) {
                  Activator.logError(b, "Componenent description in '" + u +"'. " +
                                     "Got " + pe, pe);
                }
              }
            } catch (final Exception exc) {
              Activator.logError(b, "Failed to read componenent description '" + u +"'.", exc);
            } finally {
              try {
                is.close();
              } catch (final IOException ioe) {
                Activator.logError(b, "Failed to close: " + u, ioe);
              }
            }
          }
        } else {
          Activator.logError(b, "Resource not found: " + me, null);
        }
      }
      if (cds.size() > 0) {
        final Component [] carray = new Component [cds.size()];
        for (int i = 0; i < carray.length; i++) {
          final ComponentDescription cd = cds.get(i);
          Component c;
          if (cd.isImmediate()) {
            c = new ImmediateComponent(this, cd);
          } else if (cd.getFactory() != null) {
            c = new FactoryComponent(this, cd);
          } else {
            c = new DelayedComponent(this, cd);
          }
          carray[i] = c;
          addComponentArray(components, cd.getName(), c);
          final String [] s = cd.getServices();
          if (s != null) {
            for (final String element : s) {
              addComponentArray(serviceComponents, element, c);
            }
          }
        }
        bundleComponents.put(b, carray);
        for (final Component element : carray) {
          if (element.compDesc.isEnabled()) {
            element.enable();
          }
        }
      }
    }
  }


  /**
   * Remove service resources for bundle
   *
   * @param b Bundle to check
   */
  void removeBundle(Bundle b, int reason) {
    Activator.logDebug("Remove " + b +" from SCR");
    final Component [] ca = bundleComponents.remove(b);
    if (ca != null) {
      for (final Component component : ca) {
        component.disable(reason);
        final ComponentDescription cd = component.compDesc;
        removeComponentArray(components, cd.getName(), component);
        final String [] services = cd.getServices();
        if (services != null) {
          for (final String service : services) {
            removeComponentArray(serviceComponents, service, component);
          }
        }
      }
    }
  }


  /**
   * Disabled named component or all components owned by
   * specified bundle.
   *
   * @param name Component to disable or null if we want all
   * @param b Bundle owning component
   */
  void disableComponent(String name, Bundle b) {
    final Component [] ca = bundleComponents.get(b);
    if (ca != null) {
      for (final Component element : ca) {
        if (name == null || name.equals(element.compDesc.getName())) {
          element.disable(ComponentConstants.DEACTIVATION_REASON_DISABLED);
        }
      }
    }
  }


  /**
   * Enabled named component or all components owned by
   * specified bundle.
   *
   * @param name Component to enable or null if we want all
   * @param b Bundle owning component
   */
  void enableComponent(String name, Bundle b) {
    final Component [] ca = bundleComponents.get(b);
    if (ca != null) {
      for (final Component element : ca) {
        if (name == null || name.equals(element.compDesc.getName())) {
          element.enable();
        }
      }
    }
  }


  /**
   * Get all components.
   */
  List<Component> getAllComponents() {
    final ArrayList<Component> res = new ArrayList<Component>();
    while (true) {
      try {
        for (Component[] e : components.values()) {
          for (final Component element : e) {
            res.add(element);
          }
        }
        return res;
      } catch (final ConcurrentModificationException ignore) { }
      res.clear();
    }
  }


  /**
   * Get components with specified name.
   */
  Component [] getComponent(String name) {
    return components.get(name);
  }


  /**
   * Check if component is part of a circular reference chain.
   *
   * @param component Component to check
   * @param path Stack of referenced components
   * @return If circle found return message showing circle,
   *         otherwise return null.
   */
  String checkCircularReferences(Component component,
                                 ArrayList<Component> path) {
    final int len = path.size();
    if (len > 0 && path.get(0) == component) {
      final StringBuffer sb = new StringBuffer("Found circular component chain: ");
      for (final Component element : path) {
        sb.append((element).compDesc.getName());
        sb.append("->");
      }
      sb.append(component.compDesc.getName());
      return sb.toString();
    } else if (!component.isSatisfied() && !path.contains(component)) {
      // We have not checked component before and it is inactive
      final Reference [] rs = component.getRawReferences();
      if (rs != null) {
        path.add(component);
        for (int i = 0; i < rs.length; i++) {
          // Loop through all mandatory references
          if (!rs[i].isOptional()) {
            final Component [] cs = serviceComponents.get(rs[i].refDesc.interfaceName);
            if (cs != null) {
              // Loop through all found components
              for (final Component c : cs) {
                final String res = checkCircularReferences(c, path);
                if (res != null) {
                  return res;
                }
              }
            }
          }
        }
        path.remove(len);
      }
    }
    return null;
  }


  /**
   * Save binds that where circular binds.
   */
  void postponeBind(ComponentContextImpl cci, ReferenceListener rl, ServiceReference<?> sr) {
    if (rl.isDynamic() && rl.isOptional()) {
      final Thread ct = Thread.currentThread();
      synchronized (ppRef) {
        List<PostponedBind> ppBinds = postponedBind.get(ct);
        if (ppBinds == null) {
          ppBinds = new ArrayList<PostponedBind>();
          postponedBind.put(ct, ppBinds);
        }
        ppBinds.add(new PostponedBind(cci, rl, sr));
      }
      Activator.logDebug("Postpone bind service " + Activator.srInfo(sr) + " to " + cci);
    } else {
      Activator.logDebug("Not dynamic, skip postpone bind service " + Activator.srInfo(sr) + " to " + cci);
    }
  }


  /**
   * Save binds that where circular binds.
   */
  void clearPostponeBind(ComponentContextImpl cci, ReferenceListener rl, ServiceReference<?> sr) {
    if (rl.isDynamic() && rl.isOptional()) {
      synchronized (ppRef) {
        for (final List<PostponedBind> list : postponedBind.values()) {
          for (final Iterator<PostponedBind> j = list.iterator(); j.hasNext(); ) {
            final PostponedBind pb = j.next();
            if (pb.cci == cci && pb.rl == rl && pb.sr == sr) {
              j.remove();
              Activator.logDebug("Cleared postponed bind " + Activator.srInfo(sr)
                                 + " to " + cci);
            }
          }
        }
      }
    }
  }


  /**
   * Check-in thread for saving postponed binds.
   */
  void postponeCheckin() {
    synchronized (ppRef) {
      final Thread ct = Thread.currentThread();
      Integer refCount = ppRef.get(ct);
      if (refCount != null) {
        refCount = new Integer(refCount.intValue() + 1);
      } else {
        refCount = new Integer(1);
      }
      ppRef.put(ct, refCount);
    }
  }


  /**
   * Check-out thread for saving postponed binds.
   */
  void postponeCheckout() {
    final Thread ct = Thread.currentThread();
    List<PostponedBind> ppBinds;
    synchronized (ppRef) {
      final Integer refCount = ppRef.get(ct);
      if (refCount != null) {
        final int i = refCount.intValue() - 1;
        if (i > 0) {
          ppRef.put(ct, new Integer(i));
          return;
        }
      } else {
        Activator.logError("Ref. count for postpone failed",
                           new Throwable());
      }
      ppBinds = postponedBind.remove(ct);
    }
    if (ppBinds != null) {
      for (final PostponedBind postponedBind2 : ppBinds) {
        postponedBind2.retry();
      }
    }
    synchronized (ppRef) {
      ppBinds = postponedBind.remove(ct);
      ppRef.remove(ct);
    }
    // We don't retry failed rebinds again
    if (ppBinds != null) {
      for (final PostponedBind postponedBind2 : ppBinds) {
        Activator.logInfo("Skip, retried postponed bind: " +
                          postponedBind2.toString());
      }
    }
  }


  /**
   * Register Component as a subscriber of CM events which have
   * component name as CM pid.
   *
   * @param comp Component which config needs to be tracked
   */
  Configuration [] subscribeCMConfig(Component comp) {
    String[] cs = getTargetedPIDs(comp);
    Configuration [] conf = null;
    for (int i = cs.length - 1; i >= 0; i--) {
      addSubscriberCMConfig(cs[i], comp);
      if (conf == null) {
        conf = listConfigurations(ConfigurationAdmin.SERVICE_FACTORYPID, cs[0]);
      }
      if (conf == null) {
        conf = listConfigurations(Constants.SERVICE_PID, cs[0]);
      }
    }
    return conf;
  }


  private String[] getTargetedPIDs(Component comp) {
    final String id = comp.getCMPid();
    final Bundle b = comp.getBundle();
    final StringBuffer sb = new StringBuffer();
    String [] cs = new String [] { id,
                                   b.getSymbolicName(),
                                   b.getVersion().toString(),
                                   b.getLocation() };
    for (int i = 0; i < cs.length; i++) {
      if (i > 0) {
        sb.append('|');
      }
      cs[i] = sb.append(cs[i]).toString();
    }
    return cs;
  }


  private void addSubscriberCMConfig(final String id, Component comp) {
    final Component [] old = configSubscriber.get(id);
    if (old != null) {
      final Component [] n = new Component[old.length + 1];
      System.arraycopy(old, 0, n, 0, old.length);
      n[old.length] = comp;
      configSubscriber.put(id, n);
    } else {
      configSubscriber.put(id, new Component [] {comp});
    }
  }


  /**
   * Unregister subscription of CM events for component.
   *
   * @param comp Component which config doesn't need to be tracked
   */
  void unsubscribeCMConfig(Component comp) {
    String[] cs = getTargetedPIDs(comp);
    for (int i = cs.length - 1; i >= 0; i--) {
      final Component [] old = configSubscriber.remove(cs[i]);
      if (old != null) {
        if (old.length > 1) {
          final Component [] n = new Component[old.length - 1];
          int j = 0;
          for (final Component element : old) {
            if (element != comp) {
              n[j++] = element;
            }
          }
          configSubscriber.put(cs[i], n);
        }
      } else {
        Activator.logError("Removed unknown subscriber: " + comp);
      }
    }
  }


  /**
   * Get all components for a bundle
   */
  Component [] getComponents(Bundle b) {
    return bundleComponents.get(b);
  }

  //
  // Private methods
  //

  /**
   * Get all CM configurations for specified CM pid or factory pid.
   */
  private Configuration[] listConfigurations(String key, String pid) {
    final ConfigurationAdmin cm = cmAdminTracker.getService();
    if (cm != null) {
      try {
        return cm.listConfigurations("(" + key + "=" + pid + ")");
      } catch (final InvalidSyntaxException e) {
        Activator.logError("Strange CM PID: " + pid, e);
      } catch (final IOException e) {
        Activator.logError("SCR could not retrieve the configuration for pid: " +
                           pid + ". Got IOException.", e);
      }
    }
    return null;
  }


  /**
   * Get configuration, but if there is a more targeted version available
   * ignore it and return <code>null</code>.
   * 
   * @param comp 
   * @param name 
   * @param pid 
   *
   */
  private Configuration getConfiguration(Component comp, String factoryPid, String pid) {
    /* Check if it is a factory configuration */
    if (factoryPid != null) {
      final Configuration[] conf = listConfigurations(Constants.SERVICE_PID, pid);
      if (conf != null) {
        return conf[0];
      }      
    } else {
      final String[] cs = getTargetedPIDs(comp);
      for (int i = cs.length - 1; i >= 0; i--) {
        final Configuration[] conf = listConfigurations(Constants.SERVICE_PID, cs[i]);
        if (conf != null) {
          return pid == null || cs[i].length() <= pid.length() ? conf[0] : null;
        }
      }
    }
    return null;
  }


  /**
   * Add component inside an array in a map.
   *
   */
  private static void addComponentArray(Map<String, Component[]> map, String key, Component c) {
    final Component [] a = map.get(key);
    if (a == null) {
      map.put(key, new Component [] {c});
    } else {
      final Component [] n = new Component[a.length + 1];
      System.arraycopy(a, 0, n, 0, a.length);
      n[a.length] = c;
      map.put(key, n);
    }
  }


  /**
   * Remove component inside an array in a map. Remove array if empty.
   *
   */
  private static void removeComponentArray(Map<String, Component[]> map, String key, Component c) {
    final Component [] a = map.get(key);
    if (a != null) {
      if (a.length == 1) {
        if (a[0] == c) {
          map.remove(key);
        }
      } else {
        final Component [] n = new Component[a.length - 1];
        int offset = 0;
        for (int i = 0; i < n.length; i++) {
          if (a[i + offset] == c) {
            offset = 1;
          }
          n[i] = a[i + offset];
        }
        if (offset == 1 || a[n.length] == c) {
          map.put(key, n);
        }
      }
    }
  }


  /**
   * Split a string into words separated by specified separator characters.
   * All whitespace characters will also be removed unless inside a quote.
   * <p>
   * Citation character (") may be used to group words with embedded
   * whitespace and separator characters.
   * </p>
   *
   * @param s          String to split.
   * @param separators Character to use for splitting. Any of the
   *                   characters in the separator string are considered
   *                   to be separator between words and will be removed
   *                   from the result. If no words are found, return an
   *                   array of length zero.
   */
  private static ArrayList<String> splitwords(String s) {
    boolean bCit = false;            // true when inside citation chars.
    final ArrayList<String> res = new ArrayList<String>();
    int first = -1;
    int last = -1;

    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (bCit || c != ',') {
        if (first >= 0) {
          if (c == '"') {
            bCit = !bCit;
          } else if (bCit || !Character.isWhitespace(c)) {
            last = i;
          }
        } else if (!Character.isWhitespace(c)) {
          first = c == '"' ? i+1 : i;
          last = i;
        }
      } else {
        res.add(s.substring(first, last+1));
        first = -1;
      }
    }
    if (first >= 0) {
      res.add(s.substring(first, last+1));
    }
    return res;
  }

}
