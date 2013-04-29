/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.cm;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * ConfigurationAdmin implementation
 *
 * @author Per Gustafson
 * @author Philippe Laporte
 */

class ConfigurationAdminFactory
  implements ServiceFactory<ConfigurationAdmin>, ServiceListener,
  SynchronousBundleListener
{

  // The service reference is either for a ManagedService or a
  // ManagedServiceFactory
  private final Hashtable<String, Hashtable<String, ServiceReference<?>>> locationToPids =
    new Hashtable<String, Hashtable<String, ServiceReference<?>>>();

  private final Hashtable<String, String> existingBundleLocations =
    new Hashtable<String, String>();

  ConfigurationStore store;

  private final PluginManager pluginManager;

  private final ConfigurationDispatcher configurationDispatcher;

  private final ListenerEventQueue listenerEventQueue;

  // Constants

  static final String DYNAMIC_BUNDLE_LOCATION =
    "dynamic.service.bundleLocation";

  static void checkConfigPerm(String location)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(new ConfigurationPermission(
                                                     location,
                                                     ConfigurationPermission.CONFIGURE));
    }
  }

  public ConfigurationAdminFactory(File storeDir)
  {
    storeDir.mkdirs();
    try {
      this.store = new ConfigurationStore(storeDir);
    } catch (final Exception e) {
      Activator.log.error("Error while initializing configurations store", e);
    }

    pluginManager = new PluginManager();

    listenerEventQueue = new ListenerEventQueue(Activator.bc);

    configurationDispatcher = new ConfigurationDispatcher(pluginManager);

    lookForExisitingBundleLocations();

    final String filter =
      "(|(objectClass=" + ManagedServiceFactory.class.getName() + ")"
          + "(objectClass=" + ManagedService.class.getName() + ")"
          + "(objectClass=" + ConfigurationPlugin.class.getName() + "))";
    try {
      Activator.bc.addServiceListener(this, filter);
      Activator.bc.addBundleListener(this);
    } catch (final InvalidSyntaxException ignored) {
    }

    lookForAlreadyRegisteredServices();
  }

  /**
   *
   */
  void stop()
  {
    listenerEventQueue.stop();
  }

  private void lookForAlreadyRegisteredServices()
  {
    lookForAlreadyRegisteredServices(ConfigurationPlugin.class);
    lookForAlreadyRegisteredServices(ManagedServiceFactory.class);
    lookForAlreadyRegisteredServices(ManagedService.class);
  }

  private void sendEvent(final ConfigurationEvent event)
  {
    Collection<ServiceReference<ConfigurationListener>> lReferences = null;

    try {
      lReferences =
        Activator.bc.getServiceReferences(ConfigurationListener.class, null);
    } catch (final InvalidSyntaxException ignored) {
    }

    for (final ServiceReference<ConfigurationListener> listenerRef : lReferences) {
      listenerEventQueue.enqueue(new ListenerEvent(listenerRef, event));
    }
  }

  private <C> void lookForAlreadyRegisteredServices(Class<C> c)
  {
    Collection<ServiceReference<C>> srs = null;
    try {
      srs = Activator.bc.getServiceReferences(c, null);
    } catch (final InvalidSyntaxException ignored) {
    }

    for (final ServiceReference<C> sr : srs) {
      serviceChanged(sr, ServiceEvent.REGISTERED, c.getName());
    }
  }

  private void lookForExisitingBundleLocations()
  {
    final Bundle[] bs = Activator.bc.getBundles();
    for (int i = 0; bs != null && i < bs.length; ++i) {
      existingBundleLocations.put(bs[i].getLocation(), bs[i].getLocation());
    }
  }

  private boolean isNonExistingBundleLocation(String bundleLocation)
  {
    return bundleLocation != null
           && existingBundleLocations.get(bundleLocation) == null;
  }

  private <C> ConfigurationDictionary bindLocationIfNecessary(Collection<ServiceReference<C>> srs,
                                                              ConfigurationDictionary d)
      throws IOException
  {
    if (d == null) {
      return null;
    }
    if (srs.isEmpty()) {
      return d;
    }
    String configLocation =
      (String) d.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);

    if (isNonExistingBundleLocation(configLocation)) {
      final Boolean dynamicLocation = (Boolean) d.get(DYNAMIC_BUNDLE_LOCATION);
      if (dynamicLocation != null && dynamicLocation.booleanValue()) {
        configLocation = null;
        d.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        d.remove(DYNAMIC_BUNDLE_LOCATION);
      }
    }

    if (configLocation == null) {
      final String fpid = (String) d.get(ConfigurationAdmin.SERVICE_FACTORYPID);
      final String pid = (String) d.get(Constants.SERVICE_PID);
      final String serviceLocation =
        srs.iterator().next().getBundle().getLocation();
      final ConfigurationDictionary copy = d.createCopy();
      copy.put(ConfigurationAdmin.SERVICE_BUNDLELOCATION, serviceLocation);
      copy.put(DYNAMIC_BUNDLE_LOCATION, Boolean.TRUE);

      store.store(pid, fpid, copy);
      return copy;
    }
    return d;
  }

  private void findAndUnbindConfigurationsDynamicallyBoundTo(String bundleLocation)
  {
    final String filter =
      "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + bundleLocation
          + ")" + "(" + DYNAMIC_BUNDLE_LOCATION + "=" + Boolean.TRUE + "))";
    try {
      final Configuration[] configurations = listConfigurations(filter, null);
      for (int i = 0; configurations != null && i < configurations.length; ++i) {
        ((ConfigurationImpl) configurations[i])
            .setBundleLocationAndPersist(null);
        ((ConfigurationImpl) configurations[i]).update();
      }
    } catch (final Exception e) {
      Activator.log.error("[CM] Error while unbinding configurations bound to "
                          + bundleLocation, e);
    }
  }

  private void unbindIfNecessary(final ConfigurationDictionary d)
      throws IOException
  {
    if (d == null) {
      return;
    }
    final String configLocation =
      (String) d.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
    if (isNonExistingBundleLocation(configLocation)) {
      final Boolean dynamicLocation = (Boolean) d.get(DYNAMIC_BUNDLE_LOCATION);
      if (dynamicLocation != null && dynamicLocation.booleanValue()) {
        d.remove(DYNAMIC_BUNDLE_LOCATION);
        d.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        final String fpid =
          (String) d.get(ConfigurationAdmin.SERVICE_FACTORYPID);
        final String pid = (String) d.get(Constants.SERVICE_PID);
        store.store(pid, fpid, d);
      }
    }
  }

  private <S> Collection<ServiceReference<S>> filterOnMatchingLocations(Collection<ServiceReference<S>> srs,
                                                                        String configLocation)
  {
    if (srs.size() == 1) {
      if (locationsMatch(srs.iterator().next().getBundle(), configLocation)) {
        return srs;
      }
      @SuppressWarnings("unchecked")
      final Collection<ServiceReference<S>> res = Collections.EMPTY_LIST;
      return res;
    }
    final List<ServiceReference<S>> res = new ArrayList<ServiceReference<S>>();
    for (final ServiceReference<S> sr : srs) {
      if (locationsMatch(sr.getBundle(), configLocation)) {
        res.add(sr);
      }
    }
    return res;
  }

  private <S> boolean locationsMatch(ServiceReference<S> sr,
                                     String configLocation)
  {
    if (sr == null) {
      // TODO: Log?
      return false;
    } else {
      return locationsMatch(sr.getBundle(), configLocation);
    }
  }

  private boolean locationsMatch(Bundle targetBundle, String configLocation)
  {
    if (targetBundle == null || configLocation == null) {
      return false;
    } else if (configLocation.startsWith("?")) {
      if (System.getSecurityManager() == null) {
        return true;
      } else {
        return targetBundle
            .hasPermission(new ConfigurationPermission(
                                                       configLocation,
                                                       ConfigurationPermission.TARGET));
      }
    } else if (configLocation.equals(targetBundle.getLocation())) {
      return true;
    } else {
      return false;
    }
  }

  private void addToLocationToPidsAndCheck(ServiceReference<?> sr)
  {
    if (sr == null) {
      return;
    }
    final String bundleLocation = sr.getBundle().getLocation();
    final String[] pids = getPids(sr);
    if (pids == null) {
      return;
    }
    Hashtable<String, ServiceReference<?>> pidsForLocation =
      locationToPids.get(bundleLocation);
    if (pidsForLocation == null) {
      pidsForLocation = new Hashtable<String, ServiceReference<?>>();
      locationToPids.put(bundleLocation, pidsForLocation);
    }
    for (final String pid : pids) {
      if (pidsForLocation.containsKey(pid)) {
        Activator.log
            .error("[CM] Multiple ManagedServices registered from bundle "
                   + bundleLocation + " for " + pid);
      }
      pidsForLocation.put(pid, sr);
    }
  }

  class ChangedPids
  {
    Vector<String> added = new Vector<String>();
    Vector<String> deleted = new Vector<String>();
  }

  private ChangedPids updateLocationToPidsAndCheck(ServiceReference<?> sr)
  {
    if (sr == null) {
      return null;
    }
    final String bundleLocation = sr.getBundle().getLocation();

    final Hashtable<String, ServiceReference<?>> oldPids =
      locationToPids.get(bundleLocation);
    final String[] newPids = getPids(sr);
    if (newPids == null || newPids.length == 0) {
      if (oldPids == null || oldPids.size() == 0) {
        return null;
      } else {
        removeFromLocationToPids(sr);
        final ChangedPids changes = new ChangedPids();
        changes.deleted.addAll(oldPids.keySet());
        return changes;
      }
    } else if (oldPids == null || oldPids.size() == 0) {
      addToLocationToPidsAndCheck(sr);
      final ChangedPids changes = new ChangedPids();
      changes.added.addAll(Arrays.asList(newPids));
      return changes;
    } else {
      final ChangedPids changes = new ChangedPids();
      for (final String pid : newPids) {
        final ServiceReference<?> osr = oldPids.get(pid);
        if (osr == null) {
          changes.added.add(pid);
        } else {
          if (osr.equals(sr)) {
            oldPids.remove(pid);
          } else {
            oldPids.remove(pid);
            changes.added.add(pid);
          }
        }
      }
      if (oldPids.size() > 0) {
        changes.deleted.addAll(oldPids.keySet());
      }
      removeFromLocationToPids(sr);
      addToLocationToPidsAndCheck(sr);
      return changes.added.isEmpty() && changes.deleted.isEmpty()
        ? null
        : changes;
    }
  }

  private void removeFromLocationToPids(ServiceReference<?> sr)
  {
    if (sr == null) {
      return;
    }
    final String bundleLocation = sr.getBundle().getLocation();
    final Hashtable<String, ServiceReference<?>> pidsForLocation =
      locationToPids.get(bundleLocation);

    for (final Iterator<Entry<String, ServiceReference<?>>> it =
      pidsForLocation.entrySet().iterator(); it.hasNext();) {
      final Entry<String, ServiceReference<?>> entry = it.next();
      if (entry.getValue().equals(sr)) {
        it.remove();
      }
    }

    if (pidsForLocation.isEmpty()) {
      locationToPids.remove(bundleLocation);
    }
  }

  void updateTargetServicesMatching(ConfigurationDictionary cd)
      throws IOException
  {
    final String servicePid = (String) cd.get(Constants.SERVICE_PID);
    final String factoryPid =
      (String) cd.get(ConfigurationAdmin.SERVICE_FACTORYPID);
    final String bundleLocation =
      (String) cd.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);

    if (servicePid == null) {
      return;
    }
    if (factoryPid == null) {
      updateManagedServicesMatching(servicePid, bundleLocation);
    } else {
      updateManagedServiceFactoriesMatching(servicePid, factoryPid,
                                            bundleLocation);
    }
  }

  private void updateManagedServiceFactoriesMatching(String servicePid,
                                                     String factoryPid,
                                                     String bundleLocation)
      throws IOException
  {
    final Collection<ServiceReference<ManagedServiceFactory>> srs =
      getTargetServiceReferences(ManagedServiceFactory.class, factoryPid);
    final ConfigurationDictionary cd = /* store. */load(servicePid);
    if (cd == null) {
      updateManagedServiceFactories(srs, servicePid, factoryPid, bundleLocation);
    } else {
      updateManagedServiceFactories(srs, servicePid, factoryPid, cd);
    }
  }

  void updateManagedServiceFactories(Collection<ServiceReference<ManagedServiceFactory>> srs,
                                     String servicePid,
                                     String factoryPid,
                                     ConfigurationDictionary cd)
      throws IOException
  {
    final ConfigurationDictionary bound = bindLocationIfNecessary(srs, cd);
    final String boundLocation =
      (String) bound.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
    final Collection<ServiceReference<ManagedServiceFactory>> filtered =
      filterOnMatchingLocations(srs, boundLocation);
    for (final ServiceReference<ManagedServiceFactory> sr : filtered) {
      configurationDispatcher.dispatchUpdateFor(sr, servicePid, factoryPid,
                                                bound);
    }
  }

  void updateManagedServiceFactories(Collection<ServiceReference<ManagedServiceFactory>> srs,
                                     String servicePid,
                                     String factoryPid,
                                     String boundLocation)
  {
    final Collection<ServiceReference<ManagedServiceFactory>> filtered =
      filterOnMatchingLocations(srs, boundLocation);
    for (final ServiceReference<ManagedServiceFactory> sr : filtered) {
      configurationDispatcher.dispatchUpdateFor(sr, servicePid, factoryPid,
                                                null);
    }
  }

  private void updateManagedServiceFactory(ServiceReference<ManagedServiceFactory> sr)
      throws IOException
  {
    updateManagedServiceFactory(sr, null);
  }

  private void updateManagedServiceFactory(ServiceReference<ManagedServiceFactory> sr,
                                           ChangedPids cps)
      throws IOException
  {
    if (null == cps) {
      // Newly registered managed service; all PIDs are added.
      cps = new ChangedPids();
      cps.added.addAll(Arrays.asList(getPids(sr)));
    }

    final Collection<ServiceReference<ManagedServiceFactory>> srs =
      Collections.singleton(sr);

    for (final String factoryPid : cps.added) {
      final ConfigurationDictionary[] cds = store.loadAll(factoryPid);
      if (cds == null || cds.length == 0) {
        return;
      }
      for (final ConfigurationDictionary cd : cds) {
        final String servicePid = (String) cd.get(Constants.SERVICE_PID);
        updateManagedServiceFactories(srs, servicePid, factoryPid, cd);
      }
    }

    for (final String factoryPid : cps.deleted) {
      final ConfigurationDictionary[] cds = store.loadAll(factoryPid);
      if (cds == null || cds.length == 0) {
        continue;
      } else {
        for (final ConfigurationDictionary cd : cds) {
          final String servicePid = (String) cd.get(Constants.SERVICE_PID);
          configurationDispatcher.dispatchUpdateFor(sr, servicePid, factoryPid,
                                                    null);
        }
      }
    }
  }

  private void updateManagedServicesMatching(String servicePid,
                                             String bundleLocation)
      throws IOException
  {
    final Collection<ServiceReference<ManagedService>> srs =
      getTargetServiceReferences(ManagedService.class, servicePid);
    final ConfigurationDictionary cd = load(servicePid);
    if (cd == null) {
      updateManagedServices(srs, servicePid, bundleLocation);
    } else {
      updateManagedServices(srs, servicePid, cd);
    }
  }

  private void updateManagedServices(Collection<ServiceReference<ManagedService>> srs,
                                     String servicePid,
                                     String boundLocation)
  {
    final Collection<ServiceReference<ManagedService>> filtered =
      filterOnMatchingLocations(srs, boundLocation);
    for (final ServiceReference<ManagedService> sr : filtered) {
      configurationDispatcher.dispatchUpdateFor(sr, servicePid, null, null);
    }
  }

  private void updateManagedServices(Collection<ServiceReference<ManagedService>> srs,
                                     String servicePid,
                                     ConfigurationDictionary cd)
      throws IOException
  {
    final ConfigurationDictionary bound = bindLocationIfNecessary(srs, cd);
    final String boundLocation =
      (String) bound.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
    final Collection<ServiceReference<ManagedService>> filtered =
      filterOnMatchingLocations(srs, boundLocation);
    for (final ServiceReference<ManagedService> sr : filtered) {
      configurationDispatcher.dispatchUpdateFor(sr, servicePid, null, bound);
    }
  }

  private void updateManagedService(ServiceReference<ManagedService> sr)
      throws IOException
  {
    updateManagedService(sr, null);
  }

  private void updateManagedService(ServiceReference<ManagedService> sr,
                                    ChangedPids cps)
      throws IOException
  {
    if (null == cps) {
      // Newly registered managed service; all PIDs are added.
      cps = new ChangedPids();
      cps.added.addAll(Arrays.asList(getPids(sr)));
    }

    final Collection<ServiceReference<ManagedService>> srs =
      Collections.singleton(sr);

    for (final String servicePid : cps.added) {
      ConfigurationDictionary cd = /* store. */load(servicePid);
      if (cd == null) {
        configurationDispatcher.dispatchUpdateFor(sr, servicePid, null, null);
      } else {
        cd = bindLocationIfNecessary(srs, cd);
        final String boundLocation =
          (String) cd.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        configurationDispatcher.dispatchUpdateFor(sr,
                                                  servicePid,
                                                  null,
                                                  locationsMatch(sr,
                                                                 boundLocation)
                                                    ? cd
                                                    : null);
      }
    }

    for (final String servicePid : cps.deleted) {
      final ConfigurationDictionary cd = /* store. */load(servicePid);
      if (cd != null) {
        configurationDispatcher.dispatchUpdateFor(sr, servicePid, null, null);
      }
    }
  }

  <C> Collection<ServiceReference<C>> getTargetServiceReferences(Class<C> c,
                                                                 String pid)
  {
    final String filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
    try {
      return Activator.bc.getServiceReferences(c, filter);
    } catch (final InvalidSyntaxException e) {
      Activator.log.error("Faulty ldap filter " + filter + ": "
                              + e.getMessage(), e);
      @SuppressWarnings("unchecked")
      final Collection<ServiceReference<C>> res = Collections.EMPTY_LIST;
      return res;
    }
  }

  void delete(final ConfigurationImpl c)
      throws IOException
  {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        public Object run()
            throws IOException
        {
          final ConfigurationDictionary cd = store.delete(c.getPid());
          if (cd != null) {
            updateTargetServicesMatching(cd);
          }
          return null;
        }
      });
    } catch (final PrivilegedActionException e) {
      final IOException ee = (IOException) e.getException();
      // Android don't supply nested exception
      if (ee != null) {
        throw ee;
      } else {
        throw new IOException("Failed to handle persistent CM data");
      }
    }
  }

  void update(final ConfigurationImpl c)
      throws IOException
  {
    update(c, true);
  }

  void update(final ConfigurationImpl c, final boolean dispatchUpdate)
      throws IOException
  {
    // TODO:
    // Should plugins still be called if service with
    // servicePid is not registered?

    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        public Object run()
            throws IOException
        {
          store.store(c.getPid(), c.getFactoryPid(), c.properties);
          if (dispatchUpdate) {
            updateTargetServicesMatching(c.properties);
          }
          return null;
        }
      });
    } catch (final PrivilegedActionException e) {
      final IOException ee = (IOException) e.getException();
      // Android don't supply nested exception
      if (ee != null) {
        throw ee;
      } else {
        throw new IOException("Failed to handle persistent CM data");
      }
    }
  }

  String generatePid(final String factoryPid)
      throws IOException
  {
    try {
      return AccessController
          .doPrivileged(new PrivilegedExceptionAction<String>() {
            public String run()
                throws IOException
            {
              return store.generatePid(factoryPid);
            }
          });
    } catch (final PrivilegedActionException e) {
      final IOException ee = (IOException) e.getException();
      // Android don't supply nested exception
      if (ee != null) {
        throw ee;
      } else {
        throw new IOException("Failed to handle persistent CM data");
      }
    }
  }

  ConfigurationDictionary load(final String pid)
      throws IOException
  {
    try {
      return AccessController
          .doPrivileged(new PrivilegedExceptionAction<ConfigurationDictionary>() {
            public ConfigurationDictionary run()
                throws IOException
            {
              final ConfigurationDictionary cd = store.load(pid);
              // unbindIfNecessary(cd);
              return cd;
            }
          });
    } catch (final PrivilegedActionException e) {
      final IOException ee = (IOException) e.getException();
      // Android don't supply nested exception
      if (ee != null) {
        throw ee;
      } else {
        throw new IOException("Failed to handle persistent CM data");
      }
    }
  }

  ConfigurationDictionary[] loadAll(final String factoryPid)
      throws IOException
  {
    try {
      return AccessController
          .doPrivileged(new PrivilegedExceptionAction<ConfigurationDictionary[]>() {
            public ConfigurationDictionary[] run()
                throws IOException
            {
              final ConfigurationDictionary[] cds = store.loadAll(factoryPid);
              for (int i = 0; cds != null && i < cds.length; ++i) {
                // unbindIfNecessary(cds[i]);
              }
              return cds;
            }
          });
    } catch (final PrivilegedActionException e) {
      final IOException ee = (IOException) e.getException();
      // Android don't supply nested exception
      if (ee != null) {
        throw ee;
      } else {
        throw new IOException("Failed to handle persistent CM data");
      }
    }
  }

  Configuration[] listConfigurations(String filterString, Bundle callingBundle)
      throws IOException, InvalidSyntaxException
  {
    return listConfigurations(filterString, callingBundle, false);
  }

  Configuration[] listConfigurations(String filterString,
                                     Bundle callingBundle,
                                     boolean activeOnly)
      throws IOException, InvalidSyntaxException
  {
    final Enumeration<Object> configurationPids = store.listPids();
    final Vector<ConfigurationImpl> matchingConfigurations =
      new Vector<ConfigurationImpl>();
    final Filter filter =
      filterString == null ? null : Activator.bc.createFilter(filterString);
    while (configurationPids.hasMoreElements()) {
      final String pid = (String) configurationPids.nextElement();
      final ConfigurationDictionary d = load(pid);
      if (d == null) {
        continue;
      }
      if (activeOnly && d.isNullDictionary()) {
        continue;
      }
      String configurationLocation =
        (String) d.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
      configurationLocation =
        configurationLocation == null ? "*" : configurationLocation;
      if (filter == null || filter.match(d)) {
        if ((System.getSecurityManager() == null)
            || (callingBundle == null)
            || (callingBundle.getLocation().equals(configurationLocation))
            || (callingBundle
                .hasPermission(new ConfigurationPermission(
                                                           configurationLocation,
                                                           ConfigurationPermission.CONFIGURE)))) {
          matchingConfigurations
              .addElement(new ConfigurationImpl(callingBundle, d));
        }
      }
    }
    Configuration[] c = null;
    if (matchingConfigurations.size() > 0) {
      c = new Configuration[matchingConfigurations.size()];
      matchingConfigurations.copyInto(c);
    }
    return c;
  }

  // /////////////////////////////////////////////////////////////////////////
  // ServiceFactory Implementation
  // /////////////////////////////////////////////////////////////////////////

  public ConfigurationAdmin getService(Bundle bundle,
                                       ServiceRegistration<ConfigurationAdmin> registration)
  {
    // For now we don't keep track of the services returned internally
    return new ConfigurationAdminImpl(bundle);
  }

  public void ungetService(Bundle bundle,
                           ServiceRegistration<ConfigurationAdmin> registration,
                           ConfigurationAdmin service)
  {
    // For now we do nothing here
  }

  // /////////////////////////////////////////////////////////////////////////
  // Configuration implementation
  // /////////////////////////////////////////////////////////////////////////
  class ConfigurationImpl
    implements Configuration
  {
    private final Bundle callingBundle;

    private final String factoryPid; // Factory PID

    private final String servicePid; // PID

    ConfigurationDictionary properties;

    private boolean deleted = false;

    ConfigurationImpl(Bundle callingBundle, String bundleLocation,
                      String factoryPid, String servicePid)
    {
      this(callingBundle, bundleLocation, factoryPid, servicePid, null);
    }

    ConfigurationImpl(Bundle callingBundle, String bundleLocation,
                      String factoryPid, String servicePid,
                      ConfigurationDictionary properties)
    {
      this.callingBundle = callingBundle;
      this.factoryPid = factoryPid;
      this.servicePid = servicePid;

      if (properties == null) {
        this.properties = new ConfigurationDictionary();
      } else {
        this.properties = properties;
      }
      putLocation(bundleLocation);
    }

    ConfigurationImpl(Bundle callingBundle, ConfigurationDictionary properties)
    {
      this.callingBundle = callingBundle;
      this.factoryPid =
        (String) properties.get(ConfigurationAdmin.SERVICE_FACTORYPID);
      this.servicePid = (String) properties.get(Constants.SERVICE_PID);
      this.properties = properties;
    }

    private void putLocation(String l)
    {
      if (l == null) {
        return;
      }
      if (this.properties == null) {
        this.properties = new ConfigurationDictionary();
      }
      this.properties.put(ConfigurationAdmin.SERVICE_BUNDLELOCATION, l);
    }

    public void delete()
        throws IOException
    {
      throwIfDeleted();
      ConfigurationAdminFactory.this.delete(this);
      deleted = true;

      final ServiceReference<ConfigurationAdmin> reference =
        Activator.serviceRegistration.getReference();
      if (reference == null) {
        Activator.log
            .error("ConfigurationImpl.delete: Could not get service reference");
        return;
      }

      // TODO join this with the update call. no need for parallel async
      // delivery
      ConfigurationAdminFactory.this
          .sendEvent(new ConfigurationEvent(reference,
                                            ConfigurationEvent.CM_DELETED,
                                            factoryPid, servicePid));
    }

    private String getLocation()
    {
      final ConfigurationDictionary old = properties;
      try {
        properties = ConfigurationAdminFactory.this.load(servicePid);
        ConfigurationAdminFactory.this.unbindIfNecessary(properties);
      } catch (final IOException e) {
        properties = old;
      }
      return properties == null ? null : (String) properties
          .get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
    }

    public String getBundleLocation()
    {
      throwIfDeleted();
      final String location = getLocation();
      if (callingBundle != null
          && !callingBundle.getLocation().equals(location)) {
        checkConfigPerm(location == null ? "*" : location);
      }
      return location;
    }

    public String getFactoryPid()
    {
      throwIfDeleted();
      return factoryPid;
    }

    public String getPid()
    {
      throwIfDeleted();
      return servicePid;
    }

    public Dictionary<String, Object> getProperties()
    {
      throwIfDeleted();
      if (properties == null) {
        return null;
      }
      return properties.createCopyIfRealAndRemoveLocation();
    }

    public void setBundleLocation(String bundleLocation)
    {
      throwIfDeleted();
      checkConfigPerm(bundleLocation == null ? "*" : bundleLocation);
      final String oldLoc = getLocation();
      checkConfigPerm(oldLoc == null ? "*" : oldLoc);
      setBundleLocationAndPersist(bundleLocation);

      Collection<ServiceReference<ManagedService>> srs = null;
      Collection<ServiceReference<ManagedServiceFactory>> srsF = null;
      if (factoryPid == null) {
        srs =
          ConfigurationAdminFactory.this
              .getTargetServiceReferences(ManagedService.class, servicePid);
      } else {
        srsF =
          ConfigurationAdminFactory.this
              .getTargetServiceReferences(ManagedServiceFactory.class,
                                          factoryPid);
      }

      // Notify of loss
      if (oldLoc != null && srs != null) {
        for (final ServiceReference<?> sr : (srs != null) ? srs : srsF) {
          if (locationsMatch(sr.getBundle(), oldLoc)) {
            configurationDispatcher.dispatchUpdateFor(sr, servicePid,
                                                      factoryPid, null);
          }
        }
      }

      if (bundleLocation == null) {
        // Assume should Dynamically rebind
        for (final ServiceReference<?> sr : (srs != null) ? srs : srsF) {
          bundleLocation = sr.getBundle().getLocation();
          setBundleLocationAndPersist(bundleLocation, true);
          break;
        }
      }

      // We should tell new bundle about config
      if (bundleLocation != null && !bundleLocation.equals(oldLoc)) {
        try {
          update();
        } catch (final Exception e) {
          Activator.log.error("Error while updating location.", e);
        }
      }
    }

    void setBundleLocationAndPersist(String bundleLocation)
    {
      setBundleLocationAndPersist(bundleLocation, false);
    }

    void setBundleLocationAndPersist(String bundleLocation, boolean dynamic)
    {
      final ConfigurationDictionary old = properties;
      if (properties == null) {
        properties = new ConfigurationDictionary();
      } else {
        properties = properties.createCopy();
      }
      if (dynamic) {
        properties.put(DYNAMIC_BUNDLE_LOCATION, Boolean.TRUE);
      } else {
        properties.remove(DYNAMIC_BUNDLE_LOCATION);
      }

      if (bundleLocation == null) {
        properties.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
      } else {
        putLocation(bundleLocation);
      }
      try {
        update(false);
      } catch (final IOException e) {
        e.printStackTrace();
        this.properties = old;
      }
    }

    public void update()
        throws IOException
    {
      update(true);
    }

    void update(boolean dispatchUpdate)
        throws IOException
    {
      throwIfDeleted();
      ensureAutoPropertiesAreWritten();
      ConfigurationAdminFactory.this.update(this, dispatchUpdate);

      if (!dispatchUpdate) {
        return;
      }
      final ServiceReference<ConfigurationAdmin> reference =
        Activator.serviceRegistration.getReference();
      if (reference == null) {
        Activator.log
            .error("ConfigurationImpl.update: Could not get service reference for event delivery");
        return;
      }
      // TODO join this with the update call. no need for parallel async
      // delivery
      ConfigurationAdminFactory.this
          .sendEvent(new ConfigurationEvent(reference,
                                            ConfigurationEvent.CM_UPDATED,
                                            factoryPid, servicePid));
    }

    public void update(Dictionary<String, ?> properties)
        throws IOException
    {
      throwIfDeleted();
      ConfigurationDictionary.validateDictionary(properties);
      try {
        this.properties = ConfigurationAdminFactory.this.load(servicePid);
      } catch (final IOException e) {
        this.properties = null; // TODO: proper error handling
      }
      final ConfigurationDictionary old = this.properties;
      if (properties == null) {
        this.properties = new ConfigurationDictionary();
      } else {
        this.properties = ConfigurationDictionary.createDeepCopy(properties);
      }

      copyBundleLocationFrom(old); // TODO: THIS IS WRONG!!!

      try {
        update();
      } catch (final IOException e) {
        this.properties = old;
        throw e;
      } catch (final Exception e) {
        Activator.log.error("Error while updating properties.", e);
        this.properties = old;
      }
    }

    void copyBundleLocationFrom(ConfigurationDictionary old)
    {
      final Object location =
        old.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
      if (location != null) {
        properties.put(ConfigurationAdmin.SERVICE_BUNDLELOCATION, location);
      }

      final Object dynamic = old.get(DYNAMIC_BUNDLE_LOCATION);
      if (dynamic != null) {
        properties.put(DYNAMIC_BUNDLE_LOCATION, dynamic);
      }
    }

    void ensureAutoPropertiesAreWritten()
    {
      if (this.properties == null) {
        return;
      }
      this.properties.put(Constants.SERVICE_PID, getPid());
      if (getFactoryPid() != null) {
        this.properties.put(ConfigurationAdmin.SERVICE_FACTORYPID,
                            getFactoryPid());
      } else {
        this.properties.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
      }
    }

    private void throwIfDeleted()
    {
      if (deleted) {
        throw new IllegalStateException("Configuration for " + servicePid
                                        + " has been deleted.");
      }
    }

    @Override
    public boolean equals(Object obj)
    {
      if (!(obj instanceof Configuration)) {
        return false;
      }
      return servicePid.equals(((Configuration) obj).getPid());
    }

    @Override
    public int hashCode()
    {
      return servicePid.hashCode();
    }
  }

  // /////////////////////////////////////////////////////////////////////////
  // ConfigurationAdmin implementation
  // /////////////////////////////////////////////////////////////////////////
  class ConfigurationAdminImpl
    implements ConfigurationAdmin
  {
    private final Bundle callingBundle;

    private final String callingBundleLocation;

    ConfigurationAdminImpl(final Bundle callingBundle)
    {
      this.callingBundle = callingBundle;
      this.callingBundleLocation = callingBundle.getLocation();
    }

    public Configuration createFactoryConfiguration(String factoryPid)
        throws IOException
    {
      ConfigurationDictionary[] d;
      try {
        d = ConfigurationAdminFactory.this.loadAll(factoryPid);
      } catch (final IOException ex) {
        d = null;
      }

      String locationFactoryPidIsBoundTo = null;
      if (d != null && d.length > 0) {
        locationFactoryPidIsBoundTo =
          (String) d[0].get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
      }
      if (locationFactoryPidIsBoundTo != null
          && !callingBundleLocation.equals(locationFactoryPidIsBoundTo)) {
        // TODO: OSGI43
        throw new SecurityException("Not owner of the factoryPid");

      }
      final ConfigurationImpl c =
        new ConfigurationImpl(callingBundle, callingBundleLocation, factoryPid,
                              ConfigurationAdminFactory.this
                                  .generatePid(factoryPid));
      c.update(false);
      return c;
    }

    public Configuration createFactoryConfiguration(String factoryPid,
                                                    String location)
        throws IOException
    {
      if (!callingBundle.getLocation().equals(location)) {
        checkConfigPerm(location == null ? "*" : location);
      }
      final ConfigurationImpl c =
        new ConfigurationImpl(callingBundle, location, factoryPid,
                              ConfigurationAdminFactory.this
                                  .generatePid(factoryPid));
      c.update(false);
      return c;
    }

    public Configuration getConfiguration(String pid)
    {
      ConfigurationDictionary d;
      try {
        d = ConfigurationAdminFactory.this.load(pid);
      } catch (final IOException e) {
        d = null;
      }
      if (d == null) {
        final ConfigurationImpl c =
          new ConfigurationImpl(callingBundle, callingBundleLocation, null, pid);
        c.setBundleLocationAndPersist(callingBundleLocation);
        return c;
      }

      final String bundleLocation =
        (String) d.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
      if (bundleLocation == null) {
        final ConfigurationImpl c =
          new ConfigurationImpl(callingBundle, callingBundleLocation, null,
                                pid, d);
        c.setBundleLocationAndPersist(callingBundleLocation);
        return c;
      } else if (!bundleLocation.equals(callingBundleLocation)
                 && !callingBundle
                     .hasPermission(new ConfigurationPermission(
                                                                bundleLocation,
                                                                ConfigurationPermission.CONFIGURE))) {
        throw new SecurityException(
                                    "Not owner of the requested configuration, owned by "
                                        + bundleLocation + " caller is "
                                        + callingBundleLocation);
      }
      final String factoryPid = (String) d.get(SERVICE_FACTORYPID);
      return new ConfigurationImpl(callingBundle, bundleLocation, factoryPid,
                                   pid, d);
    }

    private void checkConfigPerm(String location)
    {
      if ((System.getSecurityManager() == null)
          || (callingBundle.getLocation().equals(location))
          || (callingBundle
              .hasPermission(new ConfigurationPermission(location == null
                ? "*"
                : location, ConfigurationPermission.CONFIGURE)))) {
        return;
      } else {
        throw new SecurityException(
                                    "Not allowed to access configuration owned by "
                                        + location + " caller is "
                                        + callingBundleLocation);
      }
    }

    public Configuration getConfiguration(String pid, String location)
    {
      ConfigurationDictionary d;
      /*
       * if(!this.callingBundleLocation.equals(location)) {
       * checkConfigPerm(location == null ? "*" : location); }
       */
      checkConfigPerm(location);
      try {
        d = ConfigurationAdminFactory.this.load(pid);
      } catch (final IOException e) {
        d = null;
      }
      if (d == null) {
        final ConfigurationImpl c =
          new ConfigurationImpl(callingBundle, location, null, pid);
        if (location != null) {
          c.setBundleLocationAndPersist(location);
        } else {
          try {
            c.update(false);
          } catch (final Exception e) {
            e.printStackTrace();
          }
        }
        return c;
      } else {
        final String bundleLocation =
          (String) d.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        checkConfigPerm(bundleLocation);
        final String factoryPid =
          (String) d.get(ConfigurationAdmin.SERVICE_FACTORYPID);
        return new ConfigurationImpl(callingBundle, bundleLocation, factoryPid,
                                     pid, d);
      }
    }

    public Configuration[] listConfigurations(final String filterString)
        throws IOException, InvalidSyntaxException
    {
      Configuration[] configurations = null;

      try {
        configurations =
          AccessController
              .doPrivileged(new PrivilegedExceptionAction<Configuration[]>() {
                public Configuration[] run()
                    throws IOException, InvalidSyntaxException
                {
                  return ConfigurationAdminFactory.this
                      .listConfigurations(filterString,
                                          ConfigurationAdminImpl.this.callingBundle,
                                          true);
                }
              });
      } catch (final PrivilegedActionException e) {
        final Exception ee = e.getException();
        // Android don't supply nested exception
        if (ee != null) {
          if (ee instanceof InvalidSyntaxException) {
            throw (InvalidSyntaxException) ee;
          }
          throw (IOException) ee;
        } else {
          throw new IOException("Failed to handle CM data");
        }
      }
      return configurations;
    }

  }

  // /////////////////////////////////////////////////////////////////////////
  // Service Event handling
  // /////////////////////////////////////////////////////////////////////////

  public void bundleChanged(BundleEvent event)
  {
    if (event.getType() == BundleEvent.UNINSTALLED) {
      final String uninstalledBundleLocation = event.getBundle().getLocation();
      existingBundleLocations.remove(uninstalledBundleLocation);
      findAndUnbindConfigurationsDynamicallyBoundTo(uninstalledBundleLocation);
    } else if (event.getType() == BundleEvent.INSTALLED) {
      final String installedBundleLocation = event.getBundle().getLocation();
      existingBundleLocations.put(installedBundleLocation,
                                  installedBundleLocation);
    }
  }

  public void serviceChanged(ServiceEvent event)
  {
    final ServiceReference<?> sr = event.getServiceReference();
    final int eventType = event.getType();
    final String[] objectClasses = (String[]) sr.getProperty("objectClass");
    for (final String objectClasse : objectClasses) {
      serviceChanged(sr, eventType, objectClasse);
    }
  }

  private void serviceChanged(ServiceReference<?> sr,
                              int eventType,
                              String objectClass)
  {
    if (ManagedServiceFactory.class.getName().equals(objectClass)) {
      @SuppressWarnings("unchecked")
      final ServiceReference<ManagedServiceFactory> srF =
        (ServiceReference<ManagedServiceFactory>) sr;
      managedServiceFactoryChanged(srF, eventType);
    } else if (ManagedService.class.getName().equals(objectClass)) {
      @SuppressWarnings("unchecked")
      final ServiceReference<ManagedService> srM =
        (ServiceReference<ManagedService>) sr;
      managedServiceChanged(srM, eventType);
    } else if (ConfigurationPlugin.class.getName().equals(objectClass)) {
      @SuppressWarnings("unchecked")
      final ServiceReference<ConfigurationPlugin> srC =
        (ServiceReference<ConfigurationPlugin>) sr;
      pluginManager.configurationPluginChanged(srC, eventType);
    }
  }

  private void managedServiceFactoryChanged(ServiceReference<ManagedServiceFactory> sr,
                                            int eventType)
  {
    final String[] factoryPids = getPids(sr);
    switch (eventType) {
    case ServiceEvent.REGISTERED:
      configurationDispatcher.addQueueFor(sr);
      if (factoryPids == null) {
        final String bundleLocation = sr.getBundle().getLocation();
        Activator.log
            .error("[CM] ManagedServiceFactory w/o valid service.pid registered by "
                   + bundleLocation);
        return;
      }
      addToLocationToPidsAndCheck(sr);
      if (Activator.log.doDebug()) {
        Activator.log.debug("[CM] ManagedServiceFactory registered: "
                            + factoryPids);
      }
      try {
        updateManagedServiceFactory(sr);
      } catch (final IOException e) {
        Activator.log.error("Error while notifying services.", e);
      }
      break;
    case ServiceEvent.MODIFIED:
      final ChangedPids cp = updateLocationToPidsAndCheck(sr);
      if (cp != null) {
        try {
          updateManagedServiceFactory(sr, cp);
        } catch (final IOException e) {
          Activator.log.error("Error while notifying services.", e);
        }
      }
      break;
    case ServiceEvent.UNREGISTERING:
      removeFromLocationToPids(sr);
      configurationDispatcher.removeQueueFor(sr);
      break;
    }
  }

  static <S> String[] getPids(ServiceReference<S> sr)
  {
    final Object prop = sr.getProperty(Constants.SERVICE_PID);
    if (prop == null) {
      return new String[0];
    } else if (prop instanceof String) {
      return new String[] { (String) prop };
    } else if (prop instanceof String[]) {
      return (String[]) prop;
    } else if (prop instanceof Collection) {
      @SuppressWarnings("unchecked")
      final Collection<String> propCS = (Collection<String>) prop;
      return propCS.toArray(new String[propCS.size()]);
    } else {
      return new String[0];
    }
  }

  private void managedServiceChanged(ServiceReference<ManagedService> sr,
                                     int eventType)
  {
    final String[] servicePids = getPids(sr);
    switch (eventType) {
    case ServiceEvent.REGISTERED:
      configurationDispatcher.addQueueFor(sr);
      if (servicePids == null) {
        final String bundleLocation = sr.getBundle().getLocation();
        Activator.log
            .error("[CM] ManagedService w/o valid service.pid registered by "
                   + bundleLocation);
        return;
      }
      addToLocationToPidsAndCheck(sr);
      if (Activator.log.doDebug()) {
        Activator.log.debug("[CM] ManagedService registered: " + servicePids);
      }
      try {
        updateManagedService(sr);
      } catch (final IOException e) {
        Activator.log.error("Error while notifying services.", e);
      }
      break;
    case ServiceEvent.MODIFIED:
      final ChangedPids cp = updateLocationToPidsAndCheck(sr);
      if (cp != null) {
        try {
          updateManagedService(sr, cp);
        } catch (final IOException e) {
          Activator.log.error("Error while notifying services.", e);
        }
      }
      break;
    case ServiceEvent.UNREGISTERING:
      removeFromLocationToPids(sr);
      configurationDispatcher.removeQueueFor(sr);
      break;
    }
  }
}
