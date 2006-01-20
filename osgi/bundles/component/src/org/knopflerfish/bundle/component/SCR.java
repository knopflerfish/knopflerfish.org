/*
 * Copyright (c) 2006, KNOPFLERFISH project
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ConfigurationListener;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

class SCR implements SynchronousBundleListener {

  private BundleContext bc;
  
  private Hashtable bundleConfigs = new Hashtable();
  
  private Collection components = new ArrayList();
  private Hashtable factoryConfigs = new Hashtable();
  private Hashtable singleConfigs = new Hashtable();
  
  private Hashtable serviceConfigs = new Hashtable();
  
  private static long componentId = 0;
  private static SCR instance;

  /* 
     This might seem a bit strange, that we are not using 
     the constructor directly, but it's for convience. In the C 
     world I guess this would be called a global variable.
  */ 
  
  public static void init(BundleContext bc) {
    if (instance == null) {
      instance = new SCR(bc);
      
      Bundle[] bundles = bc.getBundles();
      for(int i=0;i<bundles.length;i++){
        if (bundles[i].getState() == Bundle.ACTIVE) {
          instance.bundleChanged(new BundleEvent(BundleEvent.STARTED, bundles[i]));
        }
      }      
    }
  }

  public static SCR getInstance() {
    return instance;
  }
  
  private SCR(BundleContext bc) {
    this.bc = bc;
    
    bc.addBundleListener(this);
    bc.registerService(ConfigurationListener.class.getName(),
                       new CMListener(),
                       new Hashtable());
  }

  public void shutdown() {
    
    for (Iterator iter = bundleConfigs.keySet().iterator(); iter.hasNext();) {
      bundleChanged(new BundleEvent(BundleEvent.STOPPING, (Bundle) iter.next()));
    }
    
    instance = null;
  }

  public void bundleChanged(BundleEvent event) {
    Bundle bundle = event.getBundle();
    String manifestEntry = (String) bundle.getHeaders().get(ComponentConstants.SERVICE_COMPONENT);
    if (manifestEntry == null) {
      return;
    }

    switch (event.getType()) {
    case BundleEvent.STARTED:

      // Create components
      Collection addedConfigs = new ArrayList();
      String[] manifestEntries = manifestEntry.split(",");
      for (int i = 0; i < manifestEntries.length; i++) {
        URL resourceURL = bundle.getResource(manifestEntries[i]);
        if (resourceURL == null) {
          Activator.log.error("Resource not found: " + manifestEntries[i]);
          continue;
        }
        try {
          Collection configs = Parser.readXML(bundle, resourceURL);
          addedConfigs.addAll(configs);

        } catch (Throwable e) {
          Activator.log.error("Failed to parse " + resourceURL);
        }
      }
      bundleConfigs.put(bundle, addedConfigs);
      for (Iterator iter = addedConfigs.iterator(); iter.hasNext();) {
        Config config = (Config) iter.next();
       
        if (config.isAutoEnabled()) {
          Component component = config.createComponent();
          component.enable();
          // WAS:config.enable();
        }
        
        // Add to cycle finder:
        String[] services = config.getServices();
        for (int i=0; i<services.length; i++) {
          ArrayList existing = (ArrayList) serviceConfigs.get(services[i]);
          if (existing == null) {
            existing = new ArrayList();
            serviceConfigs.put(services[i], existing);
          }
          existing.add(config);
        }
        // Find cycles:
        ArrayList cycle = new ArrayList();
        if (findCycle(config, cycle)) {
          String message = "Possible cycle found in references of " + config.getName() + ": ";
          Iterator citer = cycle.iterator();
          if (citer.hasNext()) {
            Config cycleItem = (Config) citer.next();
            message += cycleItem.getName();
            while (citer.hasNext()) {
              cycleItem = (Config) citer.next();
              message += " references " + cycleItem.getName();
            }
          }
          Activator.log.error(message);
        }
      }
      break;
    case BundleEvent.STOPPING:
      if (Activator.log.doDebug()) {
        Activator.log.debug("Bundle is STOPPING. Disable components.");
      }
      Collection removedConfigs = (Collection) bundleConfigs.remove(bundle);
      if (removedConfigs != null) {
        for (Iterator iter = removedConfigs.iterator(); iter.hasNext();) {
          Config config = (Config) iter.next();
          config.disable();
          // Remove from cycle finder:
          String[] services = config.getServices();
          for (int i=0; i<services.length; i++) {
            ArrayList existing = (ArrayList) serviceConfigs.get(services[i]);
            if (existing == null) continue;
            existing.remove(config);
            if (existing.size() == 0) {
              serviceConfigs.remove(services[i]);
            }
          }
        }
      }
      break;
    }
  }

  private boolean findCycle(Config config, ArrayList visited) {
    if (visited.contains(config)) {
      visited.add(config);
      return true;
    }
    visited.add(config);
    ArrayList references = config.getReferences();
    for (Iterator riter = references.iterator(); riter.hasNext();) {
      Reference ref = (Reference) riter.next();
      if (ref.isSatisfied()) continue;
      String service = ref.getInterfaceName();
      ArrayList providers = (ArrayList) serviceConfigs.get(service);
      if (providers != null) {
        for (Iterator piter = providers.iterator(); piter.hasNext();) {
          Config provider = (Config) piter.next();
          if (findCycle(provider, visited)) {
            return true;
          }
        }
      }
    }
    visited.remove(config);
    return false;
  }
    
  public Collection getComponents(Bundle bundle) {
    return (Collection)bundleConfigs.get(bundle);
  }

  public void initComponent(Component component) { // synchronized?
    Config config = component.getConfig();
    component.setProperty(ComponentConstants.COMPONENT_ID, 
                          new Long(++componentId));
    initConfig(component);
    components.add(component);
    // build graph here.
  }

  public void removeComponent(Component component) { // synchronized?
    removeConfig(component);
    components.remove(component);
  }

  private ConfigurationAdmin getCM(Component component) {

    BundleContext bc = component.getBundleContext();
    ServiceReference ref = bc.getServiceReference(ConfigurationAdmin.class.getName());
    
    if (ref == null)
      return null;

    return (ConfigurationAdmin) bc.getService(ref);
  }

  private void removeConfig(Component component) {
    Config config = component.getConfig();
    Dictionary dict = (Dictionary)factoryConfigs.get(config.getName());

    if (dict != null) {
      for (Enumeration e = dict.keys();
           e.hasMoreElements();) {
        Object key = e.nextElement();
        
        if (dict.get(key) == component) {
          dict.remove(component);
          break;
        }
      }
      
      if (dict.isEmpty()) {
        factoryConfigs.remove(config.getName());
      }
    }
  }

  private void initConfig(Component component) {

    Config config = component.getConfig();
    ConfigurationAdmin admin = getCM(component);
    String name = config.getName();

    if (admin == null)
      return ;

    try {
      Configuration[] conf = 
        admin.listConfigurations("(" + ConfigurationAdmin.SERVICE_FACTORYPID + 
                                 "=" + name + ")");
      
      if (conf != null) {
        Dictionary table = (Dictionary)factoryConfigs.get(name);
	
        if (table == null) {
          table = new Hashtable();
          factoryConfigs.put(name, table);
        }

        Collection configs = (Collection)bundleConfigs.get(config.getBundle());
	
        if (conf.length > 1) {

          for (int i = 1; i < conf.length; i++) {
            String pid = conf[i].getPid();
            Component instance = (Component)table.get(pid);

            if (instance == null) {
              Config copy = config.copy();
              instance = copy.createComponent();
              instance.cmUpdated(conf[i].getProperties());
              table.put(pid, instance);
              configs.add(copy);
              instance.enable();
            }
	  }
          
          if (table.get(conf[0]) == null) {
            component.cmUpdated(conf[0].getProperties());
            table.put(conf[0].getPid(), component);
          }
        }

        // end factory configuration
      } else {

        // regular single configuration
        conf = 
          admin.listConfigurations("(" + Constants.SERVICE_PID + "=" + name + ")");
        
        if (conf != null &&
            conf.length == 1) {
          component.cmUpdated(conf[0].getProperties());
        }
      }

    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("This is a bug.", e);
    } catch (IOException e) {
      Activator.log.error("Declarative Services could not retrieve " + 
                          "the configuration for component " + name + 
                          ". Got IOException.", e);
    }
  }

  private class CMListener implements ConfigurationListener {


    private Configuration getConfiguration(Component component, String pid) {

      ConfigurationAdmin admin = getCM(component);

      if (admin == null)
        return null;

      try {
        Configuration[] conf = 
          admin.listConfigurations("(" + Constants.SERVICE_PID + 
                                   "=" + pid + ")");

        return conf == null ? null : conf[0];
      } catch (InvalidSyntaxException e) {
        throw new RuntimeException("This is a bug", e);
      } catch (IOException e) {
        Activator.log.error("Declarative Services could not retrieve " + 
                            "the configuration for component with pid " + pid + 
                            ". Got IOException.", e);
        return null;
      }
      
    }

    private void restart(Component component) {
      restart(component, null);
    }
    
    private void restart(Component component, Configuration configuration) {
      
      component.deactivate();
      component.unregisterService();

      if (configuration != null) {
        component.cmUpdated(configuration.getProperties());
      } else {
        component.cmDeleted();
      }
      
      component.registerService();
      component.activate();
   
    }
    
    public void configurationEvent(ConfigurationEvent evt) {

      System.out.println("DEBUG ::: " + evt.getPid());
      
      String factoryPid = evt.getFactoryPid();
      String pid = evt.getPid();

      if (factoryPid != null) {
        Dictionary table = (Dictionary) factoryConfigs.get(factoryPid);
        
        if (table != null) {
          Component component = (Component)table.get(pid);
          if (component != null) {
            if (evt.getType() == ConfigurationEvent.CM_DELETED) {
              restart(component);
            }

            Configuration conf = getConfiguration(component, pid);
            if (conf == null)
              return ;
            
            restart(component, conf);

            
          } else { // we need to create a new component

            Object key = table.keys().nextElement();
            Component src = (Component)table.get(key);
            Config config = src.getConfig();
            Config copy = config.copy();
            
            Component instance = copy.createComponent();
            Configuration conf = 
              getConfiguration(instance, pid);

            if (conf == null) 
              return ;

            instance.cmUpdated(conf.getProperties());
            Collection collection = (Collection)bundleConfigs.get(copy.getBundle());
            collection.add(copy);
            instance.enable();

          }
        }

        for (Iterator i = components.iterator();
             i.hasNext(); ) { // start looking for a potential target.
          
          Component component = (Component)i.next();
          Config config = component.getConfig();

          if (factoryPid.equals(config.getName())) {

            if (table == null) {

              if (evt.getType() == ConfigurationEvent.CM_DELETED) {
                restart(component);
              }
              // this is a new factory configuration (and has a corresponding component)
              Configuration conf = 
                getConfiguration(component, pid);

              if (conf == null) 
                continue; // does not have permission.
              
              table = new Hashtable();
              factoryConfigs.put(factoryPid, table);
              table.put(evt.getPid(), component);
              restart(component, conf);
              
                
              return ;
            } 
          }
        }
      } else { // just a regular Single Configuration.
        
        for (Iterator i = components.iterator();
             i.hasNext(); ) { // start looking for a potential target.

          Component component = (Component)i.next();
          Config config = component.getConfig();
          
          if (pid.equals(config.getName())) {

            if (evt.getType() == ConfigurationEvent.CM_DELETED) {
              restart(component);
            }


            Configuration conf =
              getConfiguration(component, pid);
            
            if (conf == null) 
              continue ; // might not have permission
            
            restart(component, conf);
            // note that there might be other that matches as well..
          }
        }
      }
    }
  }

}

