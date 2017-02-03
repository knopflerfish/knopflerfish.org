/*
 * Copyright (c) 2016-2017, KNOPFLERFISH project
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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * Configuration Management PIDs for service component runtime
 */
class CMConfig
{
  final CMPid [] pids;
  private final CMHandler handler;
  private final int policy;
  private final Bundle bundle;
  private final Component component;
  private CMPid factory = null;
  private int nMissing;
  

  /**
   *
   */
  CMConfig(CMHandler handler, String [] pids, int policy, Bundle b, Component c) {
    this.handler = handler;
    this.policy = policy;
    this.bundle = b;
    this.component = c;
    this.pids = new CMPid [pids.length];
    for (int i = 0; i < pids.length; i++) {
      this.pids[i] = new CMPid(pids[i]);
    }
    nMissing = pids.length;
  }


  /**
   *
   */
  String [] getCCIds() {
    if (factory != null) {
      return factory.getActivePids();
    } else {
      return new String [] { pids[0].pid };
    }
  }


  /**
   *
   */
  @SuppressWarnings("unchecked")
  Map<String, Object> getProperties(String ccid) {
    if (factory == null && !pids[0].pid.equals(ccid)) {
      return null;
    }
    Map<String, Object> res = new HashMap<String, Object>();
    for (int i = 0; i < pids.length; i++) {
      CMPid c = pids[i];
      Dictionary<String, Object> d = c.getDictionary(factory == c ? ccid : c.pid);
      if (d != null) {
        for (Enumeration<String> ks = d.keys(); ks.hasMoreElements(); ) {
          String k = ks.nextElement();
          Object value = d.get(k);
          if (Constants.SERVICE_PID.equals(k)) {
            Object sp = res.get(k);
            if (sp instanceof LinkedList) {
              ((LinkedList<String>)sp).add((String) value);
              continue;
            } else if (sp instanceof String) {
              LinkedList<String> ll = new LinkedList<String>();
              ll.add((String) sp);
              ll.add((String) value);
              value = ll;
            }
          }
          res.put(k, value);
        }
      } else if (factory == c) {
        return null;
      }
    }
    return res;
  }


  /**
   * Check availability of active configurations.
   *
   * @return true if there are no configurations available.
   */
  boolean isEmpty() {
    if (isSatisfied()) {
      for (int i = 0; i < pids.length; i++) {
        if (!pids[i].isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }


  /**
   * Does the configuration policy says that CM data is required.
   *
   * @return true if CM is required.
   */
  boolean isRequired() {
    return ComponentDescription.POLICY_REQUIRE == policy;
  }


  /**
   * Do we have a complete configuration.
   *
   * @return true if all pids are available, otherwise false
   */
  boolean isSatisfied() {
    return ComponentDescription.POLICY_REQUIRE != policy || nMissing == 0;
  }


  /**
   * Register Component as a subscriber of CM events which have
   * component name as CM pid.
   *
   * @param comp Component which config needs to be tracked
   * @return true if all pids are available, otherwise false
   */
  boolean subscribe() {
    if (ComponentDescription.POLICY_IGNORE != policy) {
      for (int i = 0; i < pids.length; i++) {
        String[] cs = pids[i].getTargetedPIDs();
        Configuration [] conf = null;
        for (int j = cs.length - 1; j >= 0; j--) {
          handler.addSubscriberCMPid(cs[j], pids[i]);
          if (conf == null) {
            conf = handler.listConfigurations(ConfigurationAdmin.SERVICE_FACTORYPID, cs[j]);
            if (conf != null) {
              for (Configuration c : conf) {
                pids[i].configSet(pids[i].pid, c.getPid(), c, false);
              }
            }
          }
          if (conf == null) {
            conf = handler.listConfigurations(Constants.SERVICE_PID, cs[j]);
            if (conf != null) {
              pids[i].configSet(null, pids[i].pid, conf[0], false);
            }
          }
        }
      }
    }
    return isSatisfied();
  }


  /**
   * Unregister subscription of CM events for component.
   *
   * @param comp Component which config doesn't need to be tracked
   */
  void unsubscribe() {
    if (ComponentDescription.POLICY_IGNORE != policy) {
      for (int i = 0; i < pids.length; i++) {
        String[] cs = pids[i].getTargetedPIDs();
        for (int j = cs.length - 1; j >= 0; j--) {
          handler.removeSubscriberCMPid(cs[j], pids[i]);
        }
        pids[i].resetAll();
      }
    }
  }

  //
  // Private methods
  //

  private void cmPidReset(CMPid c, String cmpid) {
    boolean before = isSatisfied();
    if (c != factory || c.isEmpty()) {
      nMissing++;
    }
    if (before) {
      component.cmConfigDeleted(cmpid);
    }
  }


  private void cmPidSet(CMPid c, String cmpid, boolean doReport) {
    boolean first = !isSatisfied() || nMissing == pids.length;
    if (c != factory || c.isFirst()) {
      nMissing--;
    }
    if (isSatisfied() && doReport) {
      if (c == factory) {
        component.cmConfigUpdated(cmpid, first);
      } else {
        for (String id : getCCIds()) {
          component.cmConfigUpdated(id, first);
        }
      }
    }
  }


  private void cmPidUpdated(CMPid c, String cmpid) {
    if (isSatisfied()) {
      if (c == factory) {
        component.cmConfigUpdated(cmpid, false);
      } else {
        for (String id : getCCIds()) {
          component.cmConfigUpdated(id, false);
        }
      }
    }
  }


  private boolean factorySet(CMPid f) {
    if (factory != null) {
      return factory == f;
    }
    factory = f;
    return true;
  }


  private boolean factoryReset(CMPid f) {
    if (factory == f) {
      factory = null;
      return true;
    }
    return false;
  }

  //
  // CMPid 
  //

  class CMPid {
    final String pid;
    private final HashMap<String,Dictionary<String,Object>> cmDicts = new HashMap<String,Dictionary<String,Object>>();


    CMPid(String pid) {
      this.pid = pid;
    }


    String [] getActivePids() {
      return cmDicts.keySet().toArray(new String [cmDicts.size()]);
    }


    Dictionary<String,Object> getDictionary(String pid) {
      return cmDicts.get(pid);
    }


    /**
     * Check availability of active configurations.
     *
     * @return true if there are no configurations available.
     */
    boolean isEmpty() {
      return cmDicts.isEmpty();
    }


    /**
     * Check if we have one active configurations.
     *
     * @return true if there is one configurations available.
     */
    boolean isFirst() {
      return cmDicts.size() == 1;
    }


    void reset(String p)  {
      if (cmDicts.remove(p) != null) {
        cmPidReset(this, p);
      }
    }


    void resetAll()  {
      for (String k : new ArrayList<String>(cmDicts.keySet())) {
        reset(k);
      }
    }


    void set(String p, Dictionary<String,Object> newDict, boolean doReport)  {
      if (cmDicts.put(p, newDict) == null) {
        cmPidSet(this, p, doReport);
      } else {
        cmPidUpdated(this, p);
      }
    }


    /**
     *
     */
    void configDeleted(String factoryPid, String cpid) {
      if (factoryPid != null) {
        reset(cpid);
      } else {
        Configuration c = getConfiguration(null, null);
        if (c != null) {
          if (c.getPid().length() <= cpid.length()) {
            set(pid, c.getProperties(), true);
          }
        } else {
          reset(pid);
        }
      }
    }


    /**
     *
     */
    void configUpdated(String factoryPid, String cpid) {
      Configuration c = getConfiguration(factoryPid, cpid);
      if (c != null) {
        configSet(factoryPid, cpid, c, true);
      }
    }

    void configSet(String factoryPid, String cpid, Configuration c, boolean doReport) {
      String p;
      if (factoryPid != null) {
        if (component instanceof FactoryComponent) {
          Activator.logError(bundle.getBundleContext(), "FactoryComponent can not have factory config, ignored", null);
          return;
        }
        if (!factorySet(this)) {
          Activator.logError(bundle.getBundleContext(), "Component " + component + " has at least two factory CM configurations, " +
                             " ignoring " + factoryPid, null);
          return;
        }
        p = cpid;
      } else {
        if (factoryReset(this)) {
          resetAll();
        }
        p = pid;
      }
      set(p, c.getProperties(), doReport);
    }


    String[] getTargetedPIDs() {
      final StringBuffer sb = new StringBuffer();
      String [] cs = new String [] { pid,
                                     bundle.getSymbolicName(),
                                     bundle.getVersion().toString(),
                                     bundle.getLocation() };
      for (int i = 0; i < cs.length; i++) {
        if (i > 0) {
          sb.append('|');
        }
        cs[i] = sb.append(cs[i]).toString();
      }
      return cs;
    }

    /**
     * Get configuration, but if there is a more targeted version available
     * ignore it and return <code>null</code>.
     * 
     * @param compp 
     * @param name 
     * @param pid 
     *
     */
    Configuration getConfiguration(String factoryPid, String pid) {
      /* Check if it is a factory configuration */
      if (factoryPid != null) {
        final Configuration[] conf = handler.listConfigurations(Constants.SERVICE_PID, pid);
        if (conf != null) {
          return conf[0];
        }      
      } else {
        final String[] cs = getTargetedPIDs();
        for (int i = cs.length - 1; i >= 0; i--) {
          final Configuration[] conf = handler.listConfigurations(Constants.SERVICE_PID, cs[i]);
          if (conf != null) {
            return pid == null || cs[i].length() <= pid.length() ? conf[0] : null;
          }
        }
      }
      return null;
    }
  }

}
