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
import java.util.Enumeration; // TODO: remove
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
  
  private Hashtable components = new Hashtable();
  
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
          config.enable();
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
        }
      }
      break;
    }
  }

  public void initComponent(Component component) {
    component.setProperty(ComponentConstants.COMPONENT_ID, 
                          new Long(++componentId));
    initConfig(component);
      
    // build graph here.
  }

  private ConfigurationAdmin getCM(Component component) {
    BundleContext bc = component.getBundleContext();
    ServiceReference ref = bc.getServiceReference(ConfigurationAdmin.class.getName());

    if (ref == null)
      return null;

    return (ConfigurationAdmin) bc.getService(ref);
  }


  private void initConfig(Component component) {

    Config config = component.getConfig();
    ConfigurationAdmin admin = getCM(component);
    String name = config.getName();

    components.put(name, component);

    if (admin == null)
      return ;
    
    try {
       
      Configuration[] conf = 
        admin.listConfigurations("(" + Constants.SERVICE_PID + "=" + name + ")");
      
      if (conf != null &&
          conf.length == 1) {
        config.overrideProperties(conf[0].getProperties());
        
      } else {
        conf = 
          admin.listConfigurations("(" + ConfigurationAdmin.SERVICE_FACTORYPID + 
                                   "=" + name + ")");
        
        if (conf == null) {
          return ;
        }
        
        for (int i = 1; i < conf.length; i++) {
          Config copy = config.copy();
          bundleConfigs.put(config.getBundle(), copy);
          copy.setName(conf[i].getPid());  
          copy.enable();
        }

        Dictionary dict = conf[0].getProperties();
        
        if (dict != null) {
          config.overrideProperties(dict);
        }
        
        components.put(conf[0].getPid(), component);
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
    
    // hideous, needs a rewrite.
    public void configurationEvent(ConfigurationEvent evt) {
      Component component = (Component)components.get(evt.getPid());

      if (component != null) {
        component.unregisterService();
        component.deactivate();
        
        try { 
          Config config = component.getConfig();

          if (evt.getType() != ConfigurationEvent.CM_DELETED) {
            
            ConfigurationAdmin admin = getCM(component);
            
            if (admin == null)
              return ;
            
            Configuration[] conf = 
              admin.listConfigurations("(" + Constants.SERVICE_PID + "=" + evt.getPid() + ")");
          
            
            if (conf != null && conf.length == 1) {
              
              Dictionary dict = conf[0].getProperties();
              if (dict != null) {
                config.overrideProperties(dict);
              }
            }
          }
           
          if (config.isSatisfied()) {
            component.registerService();
            component.activate();
          }
          

        } catch (IOException e) {
          Activator.log.error("Declarative Services could not retrieve " + 
                              "the configuration for component " + evt.getPid() + 
                              ". Got IOException.", e);
          
        } catch (InvalidSyntaxException e) {
          throw new RuntimeException("This is a bug.");
        }

      } else {

        component = 
            (Component)components.get(evt.getFactoryPid());

        if (component != null) {
          component.unregisterService();
          component.deactivate();
          
          try { 
            Config config = component.getConfig();
            
            if (evt.getType() != ConfigurationEvent.CM_DELETED) {
              
              ConfigurationAdmin admin = getCM(component);
              
              if (admin == null)
                return ;
            
              Configuration[] conf = 
                admin.listConfigurations("(" + ConfigurationAdmin.SERVICE_FACTORYPID + 
                                         "=" + evt.getFactoryPid() + ")");
          
            
              if (conf != null) {

                for (int i = 1; i < conf.length; i++) {
                  Config copy = config.copy();
                  copy.setName(conf[i].getPid());
                  Dictionary dict = conf[i].getProperties();
                  
                  if(dict != null) {
                    copy.overrideProperties(dict);
                  }
                  
                  copy.enable();
                }
              
                Dictionary dict = conf[0].getProperties();
                if (dict != null) {
                  config.overrideProperties(dict);
                }

                components.put(conf[0].getPid(), component); // TODO: this MUST be removed..
              }
            }
           
            if (config.isSatisfied()) {
              component.registerService();
              component.activate();
            }
          
          
          } catch (IOException e) {
          
            Activator.log.error("Declarative Services could not retrieve " + 
                                "the configuration for component " + evt.getPid() + 
                                ". Got IOException.", e);


          } catch (InvalidSyntaxException e) {
            throw new RuntimeException("This is a bug.");
          }
        }
      }
    }
  }
}

