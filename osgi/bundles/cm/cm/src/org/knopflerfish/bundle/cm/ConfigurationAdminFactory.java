/**
 * Copyright (c) 1999-2001 Gatespace AB. All Rights Reserved.
 * 
 */

package org.knopflerfish.bundle.cm;

import org.osgi.service.cm.*;
import org.knopflerfish.service.log.*;
import org.osgi.framework.*;
import java.security.*;
import java.io.*;
import java.lang.*;
import java.util.*;

/**
 * ConfigurationAdmin implementation
 *
 * @author Per Gustafson
 * @version $Revision: 1.2 $
 */

class ConfigurationAdminFactory
    implements ServiceFactory, ServiceListener, BundleListener {

  private Hashtable locationToPids = new Hashtable();
  private Hashtable existingBundleLocations = new Hashtable();
  private ConfigurationStore store;
  private PluginManager pluginManager;
  private ConfigurationDispatcher configurationDispatcher;
  
  // Constants
  static AdminPermission ADMIN_PERMISSION = new AdminPermission();
  static String SERVICE_PID = Constants.SERVICE_PID;
  static String FACTORY_PID = "service.factoryPid";
  static String BUNDLE_LOCATION = "service.bundleLocation";
  static String DYNAMIC_BUNDLE_LOCATION = "dynamic.service.bundleLocation";
  static String DUMMY_PROPERTY = "org.knopflerfish.dummy.property";
  static String CM_RANKING = "service.cmRanking";
  static String ANY_LOCATION = "*";

  public ConfigurationAdminFactory(File storeDir) {
    storeDir.mkdirs();
    try {
      this.store = new ConfigurationStore(storeDir);
    } catch(Exception e) {
      Activator.log.error("Error while initializing configurations store", e);
    }

    pluginManager = new PluginManager();

    configurationDispatcher = new ConfigurationDispatcher(pluginManager);
    
    lookForExisitingBundleLocations();

    String filter = "(|(objectClass=" + ManagedServiceFactory.class.getName() + ")" +
                      "(objectClass=" + ManagedService.class.getName() + ")" +
                      "(objectClass=" + ConfigurationPlugin.class.getName() + "))";
    try {
      Activator.bc.addServiceListener(this, filter);
      Activator.bc.addBundleListener(this);
    } catch(InvalidSyntaxException ignored) {}

    lookForAlreadyRegisteredServices();
  }

  private void lookForAlreadyRegisteredServices() {
    lookForAlreadyRegisteredServices(ConfigurationPlugin.class);
    lookForAlreadyRegisteredServices(ManagedServiceFactory.class);
    lookForAlreadyRegisteredServices(ManagedService.class);
  }

  private void lookForAlreadyRegisteredServices(Class c) {
    ServiceReference[] srs = null;
    try {
      srs = Activator.bc.getServiceReferences(c.getName(), null);
    } catch(InvalidSyntaxException ignored) {}
    if(srs == null) {
      return;
    }
    for(int i = 0; i < srs.length; ++i) {
      serviceChanged(srs[i], ServiceEvent.REGISTERED, c.getName());
    }
  }

  private void lookForExisitingBundleLocations() {
    Bundle[] bs = Activator.bc.getBundles();
    for(int i = 0; bs != null && i < bs.length; ++i) {
      existingBundleLocations.put(bs[i].getLocation(), bs[i].getLocation());
    }
  }
  
  private boolean isNonExistingBundleLocation(String bundleLocation) {
    return bundleLocation != null && existingBundleLocations.get(bundleLocation) == null;
  }

  private ConfigurationDictionary bindLocationIfNeccesary(ServiceReference[] srs, ConfigurationDictionary d) throws IOException {
    if(d == null) {
      return null;
    }
    if(srs == null || srs.length == 0) {
      return d;
    }
    String configLocation = (String)d.get(BUNDLE_LOCATION);
    
    if(isNonExistingBundleLocation(configLocation)) {
      Boolean dynamicLocation = (Boolean)d.get(DYNAMIC_BUNDLE_LOCATION);
      if(dynamicLocation != null && dynamicLocation.booleanValue()) {
        configLocation = null;
      }
    }

    if(configLocation == null) {

      String fpid = (String)d.get(FACTORY_PID);
      String pid  = (String)d.get(SERVICE_PID);
      String serviceLocation = srs[0].getBundle().getLocation();

      ConfigurationDictionary copy = d.createCopy();
      copy.put(BUNDLE_LOCATION, serviceLocation);
      copy.put(DYNAMIC_BUNDLE_LOCATION, Boolean.TRUE);
      
      store.store(pid, fpid, copy);
      return copy;
    } else {
      return d;
    }
  }

  private void findAndUnbindConfigurationsDynamicallyBoundTo(String bundleLocation) {
    String filter = "(&(" + BUNDLE_LOCATION + "=" + bundleLocation + ")" +
                      "(" + DYNAMIC_BUNDLE_LOCATION + "=" + Boolean.TRUE + "))";
    try {
      Configuration[] configurations = listConfigurations(filter);
      for(int i = 0; configurations != null && i < configurations.length; ++i) {
        configurations[i].setBundleLocation(null);
      }
    } catch(Exception e) {
      Activator.log.error("[CM] Error while unbinding configurations bound to " + bundleLocation, e);
    }
  }

  private ServiceReference[] filterOnMatchingLocations(ServiceReference[] srs, String configLocation) {
    if(srs.length == 1) {
      String serviceLocation = srs[0].getBundle().getLocation();
      if(locationsMatch(serviceLocation, configLocation)) {
        return srs;
      } else {
        Activator.log.error("[CM] The bundle " + serviceLocation + " has registered a ManagedService(Factory) for a pid bound to " + configLocation);
        return new ServiceReference[0];
      }
    } else {
      Vector v = new Vector();
      for(int i = 0; i < srs.length; ++i) {
        String serviceLocation = srs[i].getBundle().getLocation();
        if(locationsMatch(serviceLocation, configLocation)) {
          v.addElement(srs[i]);
        } else {
          Activator.log.error("[CM] The bundle " + serviceLocation + " has registered a ManagedService(Factory) for a pid bound to " + configLocation);
        }
      }
      ServiceReference[] matching = new ServiceReference[v.size()];
      v.copyInto(matching);
      return matching;
    }
  }

  private boolean locationsMatch(String serviceLocation, String configLocation) {
    if(configLocation == null) {
      return false;
    } else if(configLocation.equals(ANY_LOCATION)) {
      return true;
    } else if(configLocation.equals(serviceLocation)) {
      return true;
    } else {
      return false;
    }
  }

  private void addToLocationToPidsAndCheck(ServiceReference sr) {
    if(sr == null) {
      return;
    }
    String bundleLocation = sr.getBundle().getLocation();
    String pid = (String)sr.getProperty(SERVICE_PID);
    if(pid == null) {
      return;
    }
    Hashtable pidsForLocation = (Hashtable)locationToPids.get(bundleLocation);
    if(pidsForLocation == null) {
      pidsForLocation = new Hashtable();
      locationToPids.put(bundleLocation, pidsForLocation);
    }      
    if(pidsForLocation.contains(pid)) {
       Activator.log.error("[CM] Multiple ManagedServices registered from bundle " + bundleLocation + " for " + pid);
    }
    pidsForLocation.put(sr, pid);
  }

  private void removeFromLocationToPids(ServiceReference sr) {
    if(sr == null) {
      return;
    }
    String bundleLocation = sr.getBundle().getLocation();
    String pid = (String)sr.getProperty(SERVICE_PID);
    Hashtable pidsForLocation = (Hashtable)locationToPids.get(bundleLocation);
    if(pidsForLocation == null) {
      return;
    } else {
      pidsForLocation.remove(sr);
      if(pidsForLocation.isEmpty()) {
        locationToPids.remove(bundleLocation);
      }
    }
  }
/*
  private ConfigurationDictionary callPluginsAndCreateACopy(ServiceReference sr, ConfigurationDictionary d) {
    if(d == null) {
      return d;
    } else {
      return pluginManager.callPluginsAndCreateACopy(sr, d);
    }
  }
*/  
  private void updateTargetServicesMatching(ConfigurationDictionary cd) throws IOException {
    String servicePid = (String)cd.get(SERVICE_PID);
    String factoryPid = (String)cd.get(FACTORY_PID);
    String bundleLocation = (String)cd.get(BUNDLE_LOCATION);

    if(servicePid == null) {
      return;
    }
    if(factoryPid == null) {
      updateManagedServicesMatching(servicePid, bundleLocation);
    } else {
      updateManagedServiceFactoriesMatching(servicePid, factoryPid, bundleLocation);
    }
  }

  private void updateManagedServiceFactoriesMatching(String servicePid, String factoryPid, String bundleLocation) throws IOException {
    ServiceReference[] srs = getTargetServiceReferences(ManagedServiceFactory.class, factoryPid);
    ConfigurationDictionary cd = store.load(servicePid);
    if(cd == null) {
      updateManagedServiceFactories(srs, servicePid, factoryPid, bundleLocation);
    } else {
      updateManagedServiceFactories(srs, servicePid, factoryPid, cd);
    }
  }

  void updateManagedServiceFactories(ServiceReference[] srs, String servicePid, String factoryPid, ConfigurationDictionary cd) throws IOException {
    ConfigurationDictionary bound = bindLocationIfNeccesary(srs, cd); 
    String boundLocation = (String)bound.get(BUNDLE_LOCATION);
    ServiceReference[] filtered = filterOnMatchingLocations(srs, boundLocation); 
    for(int i = 0; i < filtered.length; ++i) {
      configurationDispatcher.dispatchUpdateFor(filtered[i], servicePid, factoryPid, bound);
    }

  }

  void updateManagedServiceFactories(ServiceReference[] srs, String servicePid, String factoryPid, String boundLocation) {
    ServiceReference[] filtered = filterOnMatchingLocations(srs, boundLocation); 
    for(int i = 0; i < filtered.length; ++i) {
      configurationDispatcher.dispatchUpdateFor(filtered[i], servicePid, factoryPid, null);
    }
  }

  private void updateManagedServiceFactory(ServiceReference sr) throws IOException {
    final String factoryPid = (String)sr.getProperty(SERVICE_PID);
    ConfigurationDictionary[] cds = store.loadAll(factoryPid);
    if(cds == null || cds.length == 0) {
      return;
    }
    ServiceReference[] srs = new ServiceReference[] {sr};
    for(int i = 0; i < cds.length; ++i) {
      String servicePid = (String)cds[i].get(SERVICE_PID);
      updateManagedServiceFactories(srs, servicePid, factoryPid, cds[i]);
    }
  }

////
  private void updateManagedServicesMatching(String servicePid, String bundleLocation)  throws IOException {
    ServiceReference[] srs = getTargetServiceReferences(ManagedService.class, servicePid);
    ConfigurationDictionary cd = store.load(servicePid);
    if(cd == null) {
      updateManagedServices(srs, servicePid, bundleLocation);
    } else {
      updateManagedServices(srs, servicePid, cd);
    }
  }

  private void updateManagedServices(ServiceReference[] srs, String servicePid, String boundLocation) {
    ServiceReference[] filtered = filterOnMatchingLocations(srs, boundLocation); 
    for(int i = 0; i < filtered.length; ++i) {
      configurationDispatcher.dispatchUpdateFor(filtered[i], servicePid, null, null);
    }
  }

  private void updateManagedServices(ServiceReference[] srs, String servicePid, ConfigurationDictionary cd) throws IOException {
    ConfigurationDictionary bound = bindLocationIfNeccesary(srs, cd); 
    String boundLocation = (String)bound.get(BUNDLE_LOCATION);
    ServiceReference[] filtered = filterOnMatchingLocations(srs, boundLocation); 
    for(int i = 0; i < filtered.length; ++i) {
      configurationDispatcher.dispatchUpdateFor(filtered[i], servicePid, null, bound);
    }
  }

  private void updateManagedService(ServiceReference sr) throws IOException {
    final String servicePid = (String)sr.getProperty(SERVICE_PID);
    ServiceReference[] srs = new ServiceReference[] {sr};
    ConfigurationDictionary cd = store.load(servicePid);
    if(cd == null) {
      for(int i = 0; i < srs.length; ++i) {
        configurationDispatcher.dispatchUpdateFor(srs[i], servicePid, null, null);
      }
    } else {
      cd = bindLocationIfNeccesary(srs, cd);
      String boundLocation = (String)cd.get(BUNDLE_LOCATION);
      srs = filterOnMatchingLocations(srs, boundLocation); 
      for(int i = 0; i < srs.length; ++i) {
        configurationDispatcher.dispatchUpdateFor(srs[i], servicePid, null, cd);
      }
    }
  }

  ServiceReference[] getTargetServiceReferences(Class c, String pid) {
    String filter = "(" + SERVICE_PID + "=" + pid + ")";
    try {
      ServiceReference[] srs = 
        Activator.bc.getServiceReferences(c.getName(), filter);
      return srs == null ? new ServiceReference[0] : srs;
    } catch(InvalidSyntaxException e) {
      Activator.log.error("Faulty ldap filter " + filter, e);
      return new ServiceReference[0];
    }
  }

  private void delete(final ConfigurationImpl c) throws IOException {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws IOException {
          ConfigurationDictionary cd = store.delete(c.getPid());
          if(cd != null) {
            updateTargetServicesMatching(cd);
          }
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw (IOException)e.getException();
    }
  }

  private void update(final ConfigurationImpl c) throws IOException {
    // TODO:
    // Should plugins still be called if service with 
    // servicePid is not registered?

    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws IOException {
          store.store(c.getPid(), c.getFactoryPid(), c.properties);
          updateTargetServicesMatching(c.properties);
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw (IOException)e.getException();
    }
  }

  private String generatePid(final String factoryPid) throws IOException {
    try {
      return(String)
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws IOException {
          return store.generatePid(factoryPid);
        }
      });
    } catch (PrivilegedActionException e) {
      throw (IOException)e.getException();
    }
  }

  private ConfigurationDictionary load(final String pid) throws IOException {
    try {
      return(ConfigurationDictionary)
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws IOException {
          return store.load(pid);
        }
      });
    } catch (PrivilegedActionException e) {
      throw (IOException)e.getException();
    }
  }

  private Configuration[] listConfigurations(String filterString) throws IOException, InvalidSyntaxException {
    Enumeration configurationPids = store.listPids();
    Vector matchingConfigurations = new Vector();
    while(configurationPids.hasMoreElements()) {
      String pid = (String)configurationPids.nextElement();
      ConfigurationDictionary d = store.load(pid);
      if(d == null) {
        continue;
      }
      if(filterString == null) {
        matchingConfigurations.addElement(new ConfigurationImpl(d));
      } else {
        Filter filter = Activator.bc.createFilter(filterString);
        if(filter.match(d)) {
          matchingConfigurations.addElement(new ConfigurationImpl(d));
        }
      }
    }
    Configuration[] c = null;
    if(matchingConfigurations.size() > 0) {
      c = new Configuration[matchingConfigurations.size()];
      matchingConfigurations.copyInto(c);
    }
    return c;
  }

  ///////////////////////////////////////////////////////////////////////////
  // ServiceFactory Implementation
  ///////////////////////////////////////////////////////////////////////////
  public Object getService(Bundle bundle, ServiceRegistration registration) {
    // For now we don't keep track of the services returned internally
    return new ConfigurationAdminImpl(bundle);
  }
  public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
    // For now we do nothing here
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Configuration implementation 
  ///////////////////////////////////////////////////////////////////////////
  class ConfigurationImpl implements Configuration {
    private String bundleLocation;
    private final String factoryPid;
    private final String servicePid;
    ConfigurationDictionary properties;
    private boolean deleted = false;

    ConfigurationImpl(String bundleLocation,
                      String factoryPid,
                      String servicePid) {
      this(bundleLocation, factoryPid, servicePid, null);
    }

    ConfigurationImpl(String bundleLocation,
                      String factoryPid,
                      String servicePid,
                      ConfigurationDictionary properties) {
      this.bundleLocation = bundleLocation;
      this.factoryPid = factoryPid;
      this.servicePid = servicePid;
      this.properties = properties;
      if(this.properties == null) this.properties = new ConfigurationDictionary();
    }

    ConfigurationImpl(ConfigurationDictionary properties) {
      this.bundleLocation =
          (String)properties.get(BUNDLE_LOCATION);
      this.factoryPid =
          (String)properties.get(FACTORY_PID);
      this.servicePid =
          (String)properties.get(SERVICE_PID);
      this.properties = properties;
    }

    public void delete() throws IOException {
      throwIfDeleted();
      ConfigurationAdminFactory.this.delete(this);
      deleted = true;
    }

    public String getBundleLocation() {
      throwIfDeleted();
      return bundleLocation;
    }

    public String getFactoryPid() {
      throwIfDeleted();
      return factoryPid;
    }

    public String getPid() {
      throwIfDeleted();
      return servicePid;
    }

    public Dictionary getProperties() {
      throwIfDeleted();
      if(properties == null) {
        return null;
      } else {
        return properties.createCopyIfRealAndRemoveLocation();
      }
    }

    public void setBundleLocation(String bundleLocation) {
      throwIfDeleted();
	    this.bundleLocation = bundleLocation;
      ConfigurationDictionary old = properties;
      if(properties == null) {
        properties = new ConfigurationDictionary();
      } else {
        properties = properties.createCopy();
      }

      properties.remove(DYNAMIC_BUNDLE_LOCATION);
      if(bundleLocation == null) {
        properties.remove(BUNDLE_LOCATION);
      } else {
        properties.put(BUNDLE_LOCATION, bundleLocation);
      }
      try {
        update();
      } catch(IOException e) {
        this.properties = old;
      }
    }

    public void update() throws IOException {
      throwIfDeleted();
      ensureAutoPropertiesAreWritten();
      ConfigurationAdminFactory.this.update(this);
    }

    public void update(Dictionary properties) throws IOException {
      throwIfDeleted();
      ConfigurationDictionary.validateDictionary(properties);

      ConfigurationDictionary old = this.properties;

      if(properties == null) {
        this.properties = new ConfigurationDictionary();        
      } else {
        this.properties = ConfigurationDictionary.createDeepCopy(properties);
      }
      
      try {
        update();
      } catch(IOException e) {
        this.properties = old;
        throw e;
      } catch(Exception e) {
        Activator.log.error("Error while updating properties.", e);
        this.properties = old;
      }
    }

    void ensureAutoPropertiesAreWritten() {
      if(this.properties == null) return;
      this.properties.put(SERVICE_PID, getPid());
      if(getFactoryPid() != null) {
        this.properties.put(FACTORY_PID, getFactoryPid());
      }
      if(getBundleLocation() != null) {
        this.properties.put(BUNDLE_LOCATION, getBundleLocation());
      }
    }

    private void throwIfDeleted() {
      if(deleted) {
        throw new IllegalStateException("Configuration for " + servicePid + " has been deleted.");
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // ConfigurationAdmin implementation
  ///////////////////////////////////////////////////////////////////////////
  class ConfigurationAdminImpl implements ConfigurationAdmin {
    private Bundle callingBundle;
    private String callingBundleLocation;

    ConfigurationAdminImpl(Bundle callingBundle) {
      this.callingBundle = callingBundle;
      this.callingBundleLocation = callingBundle.getLocation();
    }

    public Configuration createFactoryConfiguration(String factoryPid) 
        throws IOException {
      return createFactoryConfiguration(factoryPid, callingBundleLocation);
    }

    public Configuration createFactoryConfiguration(String factoryPid, String location)
        throws IOException {
      ConfigurationImpl c = new ConfigurationImpl(location, factoryPid, ConfigurationAdminFactory.this.generatePid(factoryPid));
      if(Activator.r3TestCompliant()) c.update();
      return c;
    }
  
    public Configuration getConfiguration(String pid)
        throws IOException {
      ConfigurationDictionary d;
      try {
        d = ConfigurationAdminFactory.this.load(pid);
      } catch (IOException e) { d = null;}
      if(d == null) {
        return new ConfigurationImpl(callingBundleLocation, null, pid);
      } else {
        String bundleLocation = (String)d.get(BUNDLE_LOCATION);
        if(bundleLocation != null &&
          !bundleLocation.equals(callingBundleLocation) &&
          !callingBundle.hasPermission(ADMIN_PERMISSION)) {
          throw new SecurityException(
            "Not owner of the requested configuration, owned by " +
            bundleLocation + " caller is " + callingBundleLocation );
        }
        String factoryPid = (String)d.get(FACTORY_PID);
        return new ConfigurationImpl(bundleLocation, factoryPid, pid, d);
      }
    }

    public Configuration getConfiguration(String pid, String location)
        throws IOException {
      ConfigurationDictionary d;
      try {
        d = ConfigurationAdminFactory.this.load(pid);
      } catch (IOException e) { d = null;}
      if(d == null) {
        ConfigurationImpl c = new ConfigurationImpl(location, null, pid);
        if(location != null) c.setBundleLocation(location);
        return c;
      } else {
        String bundleLocation = (String)d.get(BUNDLE_LOCATION);
        String factoryPid = (String)d.get(FACTORY_PID);
        return new ConfigurationImpl(bundleLocation, factoryPid, pid, d);
      }
    }

    public Configuration[] listConfigurations(final String filterString)
        throws IOException, InvalidSyntaxException {
      Configuration[] configurations = null;
      try {
        configurations = (Configuration[])
        AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run() throws IOException, InvalidSyntaxException {
            return ConfigurationAdminFactory.this.listConfigurations(filterString);
          }
        });
      } catch (PrivilegedActionException e) {
        configurations = null;
        if(e.getException().getClass() == InvalidSyntaxException.class) {
          throw (InvalidSyntaxException)e.getException();
        } else {
          throw (IOException)e.getException();
        }
      }
      return configurations;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Service Event handling
  ///////////////////////////////////////////////////////////////////////////

  public void bundleChanged(BundleEvent event) {
    if(event.getType() == BundleEvent.UNINSTALLED) {
      String uninstalledBundleLocation = event.getBundle().getLocation();
      existingBundleLocations.remove(uninstalledBundleLocation);
      findAndUnbindConfigurationsDynamicallyBoundTo(uninstalledBundleLocation);
    } else if(event.getType() == BundleEvent.INSTALLED) {
      String installedBundleLocation = event.getBundle().getLocation();
      existingBundleLocations.put(installedBundleLocation, installedBundleLocation);
    }
  }

  public void serviceChanged(ServiceEvent event) {
    ServiceReference sr = event.getServiceReference();
    int eventType = event.getType();
    String[] objectClasses =
      (String[]) sr.getProperty("objectClass");
    for(int i = 0; i < objectClasses.length; ++i) {
      serviceChanged(sr, eventType, objectClasses[i]);
    }
  }

  private void serviceChanged(ServiceReference sr, int eventType, String objectClass) {
    if(ManagedServiceFactory.class.getName().equals(objectClass)) {
      managedServiceFactoryChanged(sr, eventType);
    } else  if(ManagedService.class.getName().equals(objectClass)) {
      managedServiceChanged(sr, eventType);
    } else  if(ConfigurationPlugin.class.getName().equals(objectClass)) {
      pluginManager.configurationPluginChanged(sr, eventType);
    }
  }

  private void managedServiceFactoryChanged(ServiceReference sr, int eventType) {
    final String factoryPid = (String)sr.getProperty(SERVICE_PID);
    switch(eventType) {
      case ServiceEvent.REGISTERED:
        configurationDispatcher.addQueueFor(sr);
        if(factoryPid == null) {
          String bundleLocation = sr.getBundle().getLocation();
          Activator.log.error("[CM] ManagedServiceFactory w/o valid service.pid registered by " + bundleLocation);
          return;
        }
        addToLocationToPidsAndCheck(sr);
        if(Activator.log.doDebug()) {
          Activator.log.debug("[CM] ManagedServiceFactory registered: " + factoryPid);
        }
        try {
          updateManagedServiceFactory(sr);
        } catch(IOException e) {
          Activator.log.error("Error while notifying services.", e);
        }
      break;
      case ServiceEvent.MODIFIED:
      break;
      case ServiceEvent.UNREGISTERING:
        removeFromLocationToPids(sr);
        configurationDispatcher.removeQueueFor(sr);
      break;
    }
  }

  private void managedServiceChanged(ServiceReference sr, int eventType) {
    final String servicePid = (String)sr.getProperty(SERVICE_PID);
    switch(eventType) {
      case ServiceEvent.REGISTERED:
        configurationDispatcher.addQueueFor(sr);
        if(servicePid == null) {
          String bundleLocation = sr.getBundle().getLocation();
          Activator.log.error("[CM] ManagedService w/o valid service.pid registered by " + bundleLocation);
          return;
        }
        addToLocationToPidsAndCheck(sr);
        if(Activator.log.doDebug()) {
          Activator.log.debug("[CM] ManagedService registered: " + servicePid);
        }
        try {
          updateManagedService(sr);
        } catch(IOException e) {
          Activator.log.error("Error while notifying services.", e);
        }
      break;
      case ServiceEvent.MODIFIED:
      break;
      case ServiceEvent.UNREGISTERING:
        removeFromLocationToPids(sr);
        configurationDispatcher.removeQueueFor(sr);
      break;
    }
  }
}
