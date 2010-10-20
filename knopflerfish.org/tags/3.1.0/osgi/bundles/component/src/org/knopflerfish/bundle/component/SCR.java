/*
 * Copyright (c) 2006-2010, KNOPFLERFISH project
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

import java.io.*;
import java.net.URL;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.*;

import org.xmlpull.v1.*;

/**
 *
 */
class SCR implements SynchronousBundleListener, ConfigurationListener
{
  final private BundleContext bc;

  private HashSet /* Bundle */ lazy = new HashSet();
  private Hashtable /* Bundle -> Component[] */ bundleComponents = new Hashtable();
  private Hashtable /* String -> Component[] */ components = new Hashtable();
  private Hashtable /* String -> Component[] */ serviceComponents = new Hashtable();
  private Hashtable /* String -> Component[] */ configSubscriber = new Hashtable();
  private ServiceRegistration cmListener = null;
  private ServiceTracker cmAdminTracker;
  private volatile long nextId = 0;


  /**
   *
   */
  SCR(BundleContext bc) {
    this.bc = bc;
    cmAdminTracker = new ServiceTracker(bc, ConfigurationAdmin.class.getName(), null);
  }


  /**
   * Start SCR.
   *
   */
  void start() {
    cmAdminTracker.open();
    cmListener = bc.registerService(ConfigurationListener.class.getName(), this, null);
    bc.addBundleListener(this);
    final Bundle [] bundles = bc.getBundles();
    for (int i = 0; i < bundles.length; i++)  {
      if ((bundles[i].getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
        processBundle(bundles[i]);
      }
    }
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
    Bundle [] b = (Bundle [])bundleComponents.keySet().toArray(new Bundle[bundleComponents.size()]);
    for (int i = 0; i < b.length; i++) {
      removeBundle(b[i], ComponentConstants.DEACTIVATION_REASON_DISABLED);
    }
  }


  //
  // BundleListener method
  //

  /**
   * Process bundle components when it starts.
   */
  public synchronized void bundleChanged(BundleEvent event) {
    Bundle bundle = event.getBundle();

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
  }

  //
  // ConfigurationListener method
  //

  /**
   *
   */
  public void configurationEvent(ConfigurationEvent evt) {
    try {
    String name = evt.getFactoryPid();
    String pid = evt.getPid();
    if (name == null) {
      name = pid;
    }
    Component [] comp = (Component [])configSubscriber.get(name);
    if (comp != null) {
      switch (evt.getType()) {
      case ConfigurationEvent.CM_DELETED:
        for (int i = 0; i < comp.length; i++) {
          comp[i].cmConfigDeleted(pid);
        }
        break;
      case ConfigurationEvent.CM_UPDATED:
        for (int i = 0; i < comp.length; i++) {
          comp[i].cmConfigUpdated(pid, getConfiguration(pid));
        }
        break;
      default:
        Activator.logWarning("Unknown ConfigurationEvent type: " + evt.getType());
        break;
      }
    }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //
  // Package methods
  //

  /**
   * Get next unique component identifier.
   */
  synchronized Long getNextId() {
    return new Long(nextId++);
  }


  /**
   * Check if bundle has service components and process them.
   *
   * @param b Bundle to check
   */
  void processBundle(Bundle b) {
    String sc = (String)b.getHeaders().get(ComponentConstants.SERVICE_COMPONENT);
    Activator.logDebug("Process header " + ComponentConstants.SERVICE_COMPONENT +
                       " for bundle#" + b.getBundleId() + ": " + sc);
    if (sc != null) {
      ArrayList entries = splitwords(sc);
      if (entries.size() == 0) {
        Activator.logError(b, "Header " + ComponentConstants.SERVICE_COMPONENT + " empty.", null);
        return;
      }
      XmlPullParser p;
      try {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(true);
        p = f.newPullParser();
      } catch (XmlPullParserException xppe) {
        Activator.logError("Could not fina a XML parser", xppe);
        return;
      }
      ArrayList cds = new ArrayList();
      for (Iterator i = entries.iterator(); i.hasNext(); ) {
        String me = (String)i.next();
        int pos = me.lastIndexOf('/');
        String path = pos > 0 ? me.substring(0, pos) : "/";
        Enumeration e = b.findEntries(path, me.substring(pos+1), false);
        if (null != e) {
          while (e.hasMoreElements()) {
            URL u = (URL)e.nextElement();
            InputStream is = null;
            try {
              is = u.openStream();
            } catch (IOException ioe) {
              Activator.logError(b, "Failed to open: " + u, ioe);
              return;
            }
            try {
              p.setInput(is, null);
              Activator.logDebug("Parse component description: " + u);
              while (true) {
                try {
                  ComponentDescription cd = ComponentDescription.parseComponent(b, p);
                  Activator.logDebug("Got component description: " + cd);
                  if (cd != null) {
                    cds.add(cd);
                  } else {
                    break;
                  }
                } catch (XmlPullParserException pe) {
                  Activator.logError(b, "Componenent description in '" + u +"'. " +
                                     "Got " + pe, pe);
                } finally {
                }
              }
            } catch (Exception exc) {
              Activator.logError(b, "Failed to read componenent description '" + u +"'.", exc);
            } finally {
              try {
                is.close();
              } catch (IOException ioe) {
                Activator.logError(b, "Failed to close: " + u, ioe);
              }
            }
          }
        } else {
          Activator.logError(b, "Resource not found: " + me, null);
        }
      }
      if (cds.size() > 0) {
        Component [] carray = new Component [cds.size()];
        bundleComponents.put(b, carray);
        for (int i = 0; i < carray.length; i++) {
          ComponentDescription cd = (ComponentDescription)cds.get(i);
          Component c;
          if (cd.isImmediate()) {
            c = new ImmediateComponent(this, cd);
          } else if (cd.getFactory() != null) {
            c = new FactoryComponent(this, cd);
          } else {
            c = new DelayedComponent(this, cd);
          }
          carray[i] = c;
          addComponentArray(components, c.compDesc.getName(), c);
          String [] s = cd.getServices();
          if (s != null) {
            for (int si = 0; si < s.length; si++) {
              addComponentArray(serviceComponents, s[si], c);
            }
          }
        }
        for (int i = 0; i < carray.length; i++) {
          if (carray[i].compDesc.isEnabled()) {
            carray[i].enable();
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
    Component [] ca = (Component [])bundleComponents.remove(b);
    if (ca != null) {
      for (int i = 0; i < ca.length; i++) {
        ca[i].disable(reason);
        components.remove(ca[i].compDesc.getName());
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
    Component [] ca = (Component [])bundleComponents.get(b);
    if (ca != null) {
      for (int i = 0; i < ca.length; i++) {
        if (name == null || name.equals(ca[i].compDesc.getName())) {
          ca[i].disable(ComponentConstants.DEACTIVATION_REASON_DISABLED);
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
    Component [] ca = (Component [])bundleComponents.get(b);
    if (ca != null) {
      for (int i = 0; i < ca.length; i++) {
        if (name == null || name.equals(ca[i].compDesc.getName())) {
          ca[i].enable();
        }
      }
    }
  }


  /**
   * Get components with specified name.
   */
  Component [] getComponent(String name) {
    return (Component [])components.get(name);
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
                                 String pid,
                                 ArrayList path) {
    int len = path.size();
    if (len > 0 && path.get(0) == component) {
      StringBuffer sb = new StringBuffer("Found circular component chain: ");
      for (Iterator pi = path.iterator(); pi.hasNext(); ) {
        sb.append(((Component)pi.next()).compDesc.getName());
        sb.append("->");
      }
      sb.append(component.compDesc.getName());
      return sb.toString();
    } else if (!component.isSatisfied() && !path.contains(component)) {
      // We have not checked component before and it is inactive
      Reference [] rs = component.getReferences();
      if (rs != null) {
        path.add(component);
        for (int i = 0; i < rs.length; i++) {
          // Loop through all mandatory references
          if (!rs[i].isOptional()) {
            Component [] cs = (Component [])serviceComponents.get(rs[i].refDesc.interfaceName);
            if (cs != null) {
              // Components for service found
              // Get target filter to
              // NYI! optimize when several pids has same filter
              Filter f = rs[i].getListener(pid).getTargetFilter();
              // Loop through all found components
              for (int cx = 0; cx < cs.length; cx++) {
                String [] pids = cs[cx].getAllServicePids();
                // Loop through service property configurations
                for (int px = 0; px < pids.length; px++) {
                  if (f == null || f.match(cs[cx].getServiceProperties(pids[px]))) {
                    String res = checkCircularReferences(cs[cx], pids[px], path);
                    if (res != null) {
                      return res;
                    }
                  }
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
   * Register Component as a subscriber of CM events which have
   * component name as CM pid.
   *
   * @param comp Component which config needs to be tracked
   */
  Configuration [] subscribeCMConfig(Component comp) {
    String name = comp.compDesc.getName();
    Component [] old = (Component [])configSubscriber.get(name);
    if (old != null) {
      Component [] n = new Component[old.length + 1];
      System.arraycopy(old, 0, n, 0, old.length);
      n[old.length] = comp;
      configSubscriber.put(name, n);
    } else {
      configSubscriber.put(name, new Component [] {comp});
    }

    Configuration [] conf = listConfigurations(ConfigurationAdmin.SERVICE_FACTORYPID, name);
    if (conf == null) {
      conf = listConfigurations(Constants.SERVICE_PID, name);
    }
    return conf;
  }


  /**
   * Unregister subscription of CM events for component.
   *
   * @param comp Component which config doesn't need to be tracked
   */
  void unsubscribeCMConfig(Component comp) {
    String name = comp.compDesc.getName();
    Component [] old = (Component [])configSubscriber.remove(name);
    if (old != null) {
      if (old.length != 1) {
        Component [] n = new Component[old.length - 1];
        int j = 0;
        for (int i = 0; i < old.length; i++) {
          if (old[i] != comp) {
            n[j++] = old[i];
          }
        }
        configSubscriber.put(name, n);
      }
    } else {
      Activator.logError("Removed unknown subscriber: " + comp);
    }
  }

  //
  // Private methods
  //

  /**
   * Get all CM configurations for specified CM pid or factory pid.
   */
  private Configuration[] listConfigurations(String key, String pid) {
    ConfigurationAdmin cm = (ConfigurationAdmin)cmAdminTracker.getService();
    try {
      return cm.listConfigurations("(" + key + "=" + pid + ")");
    } catch (InvalidSyntaxException e) {
      Activator.logError("Strange CM PID: " + pid, e);
    } catch (IOException e) {
      Activator.logError("SCR could not retrieve the configuration for pid: " +
                          pid + ". Got IOException.", e);
    }
    return null;
  }


  /**
   *
   */
  private Configuration getConfiguration(String pid) {
    Configuration[] conf = listConfigurations(Constants.SERVICE_PID, pid);
    if (conf != null) {
      return conf[0];
    }
    return null;
  }


  /**
   * Add component inside an array to map. Merge arrays if several
   * components has the same key.
   *
   */
  private static void addComponentArray(Map map, String key, Component c) {
    Component [] a = (Component [])map.get(key);
    if (a == null) {
      map.put(key, new Component [] {c});
    } else {
      Component [] n = new Component[a.length + 1];
      System.arraycopy(a, 0, n, 0, a.length);
      n[a.length] = c;
      map.put(key, n);
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
  private static ArrayList splitwords(String s) {
    boolean bCit = false;            // true when inside citation chars.
    ArrayList res = new ArrayList();
    int first = -1;
    int last = -1;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
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
