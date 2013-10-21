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
import java.util.StringTokenizer;
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
import org.osgi.service.cm.SynchronousConfigurationListener;

/**
 * ConfigurationAdmin implementation
 *
 * @author Per Gustafson
 * @author Gunnar Ekolin
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
    if (event==null) {
      return;
    }
    // First send to synchronous configuration listeners
    sendEventSync(event);
    // then enqueue for asynchronous delivery to configuration listeners.
    postEvent(event);
  }

  // Send configuration event to synchronous configuration listeners.
  // Note that the even must be delivered using the thread that called update/delete
  // thus there is no way for us to protect against listeners that does not return...
  private void sendEventSync(final ConfigurationEvent event)
  {
    Collection<ServiceReference<SynchronousConfigurationListener>> lReferences = null;

    try {
      lReferences =
        Activator.bc.getServiceReferences(SynchronousConfigurationListener.class, null);
    } catch (final InvalidSyntaxException ignored) {
    }

    for (final ServiceReference<SynchronousConfigurationListener> listenerRef : lReferences) {
      try {
        new ListenerEvent(listenerRef, event).sendEvent(Activator.bc);
      } catch (final Exception e) {
        Activator.log.error("Failed to call synchronous configuration listener: " +e, e);
      }
    }
  }

  private void postEvent(final ConfigurationEvent event) {
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

  private <C> ConfigurationDictionary bindLocationIfNecessary(ServiceReference<C> sr,
                                                              ConfigurationDictionary d)
      throws IOException
  {
    if (d == null) {
      return null;
    }
    if (sr == null) {
      return d;
    }
    String configLocation = d.getLocation();

    if (isNonExistingBundleLocation(configLocation)) {
      final Boolean dynamicLocation = (Boolean) d.get(DYNAMIC_BUNDLE_LOCATION);
      if (dynamicLocation != null && dynamicLocation.booleanValue()) {
        configLocation = null;
        d.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        d.remove(DYNAMIC_BUNDLE_LOCATION);
      }
    }

    if (configLocation == null) {
      final String fpid = d.getFactoryPid();
      final String pid = d.getPid();
      final String serviceLocation = sr.getBundle().getLocation();
      final ConfigurationDictionary copy = d.createCopy();
      copy.put(ConfigurationAdmin.SERVICE_BUNDLELOCATION, serviceLocation);
      copy.put(DYNAMIC_BUNDLE_LOCATION, Boolean.TRUE);

      store.store(pid, fpid, copy, false);
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
    final String configLocation = d.getLocation();
    if (isNonExistingBundleLocation(configLocation)) {
      final Boolean dynamicLocation = (Boolean) d.get(DYNAMIC_BUNDLE_LOCATION);
      if (dynamicLocation != null && dynamicLocation.booleanValue()) {
        d.remove(DYNAMIC_BUNDLE_LOCATION);
        d.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        final String fpid = d.getFactoryPid();
        final String pid = d.getPid();
        store.store(pid, fpid, d, false);
      }
    }
  }

  /**
   * Remove all service references belonging to bundles that does not match the
   * bundle target specification in the given targeted PID.
   *
   * @param srs
   *          collection of service references to filter.
   * @param targetedPid
   *          a targeted PID with a specification that the registering bundle
   *          must match.
   * @return List with those service references in {@code srs} that are
   *         registered by bundles that matches the given target specification.
   */
  private <S> Collection<ServiceReference<S>> filterOnTargetedPid(Collection<ServiceReference<S>> srs,
                                                                  String targetedPid)
  {
    final Collection<ServiceReference<S>> res =
      new ArrayList<ServiceReference<S>>(srs.size());
    for (final ServiceReference<S> sr : srs) {
      if (targetedPidMatches(targetedPid, sr.getBundle())) {
        res.add(sr);
      }
    }
    return res;
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

  private void addToLocationToPidsAndCheck(ServiceReference<?> sr, String[] pids)
  {
    if (sr == null) {
      return;
    }
    final String bundleLocation = sr.getBundle().getLocation();
    if (pids == null || pids.length == 0) {
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
      addToLocationToPidsAndCheck(sr, newPids);
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
      addToLocationToPidsAndCheck(sr, newPids);
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
    final String cfgPid = cd.getPid();
    final String cfgFactoryPid = cd.getFactoryPid();
    final String bundleLocation = cd.getLocation();

    if (cfgPid == null) {
      return;
    }
    if (cfgFactoryPid == null) {
      updateManagedServicesMatching(cfgPid, bundleLocation);
    } else {
      updateManagedServiceFactoriesMatching(cfgPid, cfgFactoryPid,
                                            bundleLocation);
    }
  }

  private void updateManagedServiceFactoriesMatching(String servicePid,
                                                     String targetedFactoryPid,
                                                     String oldBundleLocation)
      throws IOException
  {
    String factoryPid = targetedFactoryPid;
    Collection<ServiceReference<ManagedServiceFactory>> srs =
      getServiceReferencesWithPid(ManagedServiceFactory.class, factoryPid);

    if (srs.isEmpty()) {
      // No MSF with PID factoryPid, check it the given targeted factory PID
      // specifies a target.
      final int barPos = targetedFactoryPid.indexOf('|');
      if (barPos > 0) {
        factoryPid = targetedFactoryPid.substring(0, barPos);
        srs =
          getServiceReferencesWithPid(ManagedServiceFactory.class, factoryPid);
        // Here we can filter {@code srs} on the target specification since we
        // do not need to replace a bound configuration with another more / less
        // specific one based on the matching rules for targeted PIDs.
        srs = filterOnTargetedPid(srs, targetedFactoryPid);
      }
    }

    if (srs.isEmpty()) {
      // No managed service factories matching this factory PID.
      return;
    }

    final ConfigurationDictionary cd = load(servicePid, null);
    if (cd == null) {
      // Deleted configuration instance, tell the MSFs.
      updateManagedServiceFactories(srs, servicePid, factoryPid, oldBundleLocation);
    } else {
      // New or updated factory configuration instance
      final ServiceReference<ManagedServiceFactory> bestSr = srs.iterator().next();
      final ConfigurationDictionary bound = bindLocationIfNecessary(bestSr, cd);
      final String newBundleLocation = bound.getLocation();

      final Collection<ServiceReference<ManagedServiceFactory>> filtered =
        filterOnMatchingLocations(srs, newBundleLocation);
      for (final ServiceReference<ManagedServiceFactory> sr : filtered) {
        if (targetedPidMatches(targetedFactoryPid, sr.getBundle())) {
          // This factory configuration instance is targeted at the bundle that
          // has registered the MSF
          configurationDispatcher.dispatchUpdateFor(sr, servicePid, factoryPid,
                                                    bound);
        }
      }
    }
  }

  void updateManagedServiceFactories(ServiceReference<ManagedServiceFactory> sr,
                                     String servicePid,
                                     String factoryPid,
                                     ConfigurationDictionary cd)
      throws IOException
  {
    final ConfigurationDictionary bound = bindLocationIfNecessary(sr, cd);
    final String boundLocation = bound.getLocation();
    if (locationsMatch(sr.getBundle(), boundLocation)) {
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
    // Newly registered managed service factory; all PIDs are added.
    final ChangedPids cps = new ChangedPids();
    cps.added.addAll(Arrays.asList(getPids(sr)));
    updateManagedServiceFactory(sr, cps);
  }

  private void updateManagedServiceFactory(ServiceReference<ManagedServiceFactory> sr,
                                           ChangedPids cps)
      throws IOException
  {
    for (final String factoryPid : cps.added) {
      final List<ConfigurationDictionary> cds =
        loadAll(factoryPid, sr.getBundle());
      for (final ConfigurationDictionary cd : cds) {
        final String servicePid = cd.getPid();
        updateManagedServiceFactories(sr, servicePid, factoryPid, cd);
      }
    }

    for (final String factoryPid : cps.deleted) {
      final List<ConfigurationDictionary> cds =
        loadAll(factoryPid, sr.getBundle());
      for (final ConfigurationDictionary cd : cds) {
        final String servicePid = cd.getPid();
        configurationDispatcher.dispatchUpdateFor(sr, servicePid, factoryPid,
                                                  null);
      }
    }
  }

  private void updateManagedServicesMatching(final String targetedPid,
                                             final String oldBundleLocation)
      throws IOException
  {
    boolean isTargetedPID = false;
    String pid = targetedPid;
    Collection<ServiceReference<ManagedService>> srs =
      getServiceReferencesWithPid(ManagedService.class, pid);

    if (srs.isEmpty()) {
      // No MS with the given PID, try to handle it as a targetedPID:
      final int barPos = targetedPid.indexOf('|');
      isTargetedPID = barPos > 0; // At least one char in the PID.
      if (isTargetedPID) {
        pid = targetedPid.substring(0, barPos);
        srs = getServiceReferencesWithPid(ManagedService.class, pid);
      }
    }

    if (srs.isEmpty()) {
      // No managed services registered for this PID.
      return;
    }

    // Note: We can not filter the set of MSs based on the target specification
    // in the PID since we must select the most specific matching targeted
    // configuration for each MS. Thus it may be that we need to update other
    // MSs with this PID than those that does match on the current target
    // specification.

    final boolean isDeleted = null == load(targetedPid, null);
    if (isDeleted) {
      if (!isTargetedPID) {
        // A non-targeted configuration has been deleted, no other configuration
        // will be available for any matching managed service!
        updateManagedServices(srs, pid, oldBundleLocation);
      } else {
        updateManagedServicesForDeletedTargetedPID(targetedPid,
                                                   oldBundleLocation, pid, srs);
      }
    } else {
      // New or updated configuration
      final ServiceReference<ManagedService> bestSr = srs.iterator().next();
      final ConfigurationDictionary cd = load(pid, bestSr.getBundle());
      final ConfigurationDictionary bound = bindLocationIfNecessary(bestSr, cd);
      final String newBundleLocation = bound.getLocation();

      final Collection<ServiceReference<ManagedService>> filtered =
        filterOnMatchingLocations(srs, newBundleLocation);
      for (final ServiceReference<ManagedService> sr : filtered) {
        ConfigurationDictionary srCd = bound;
        if (newBundleLocation.charAt(0) == '?') {
          // There may be another configuration for this SR when multi-locations
          // are in use!
          srCd = load(pid, sr.getBundle());
          if (!targetedPid.equals(srCd.getPid())) {
            // This MS uses another targeted configuration than the changed one,
            // thus no update call.
            continue;
          }
        }
        configurationDispatcher.dispatchUpdateFor(sr, pid, null, srCd);
      }
    }
  }

  /**
   * Check if the target part of the given PID matches the specified bundle.
   *
   * @param targetedPid
   *          targeted PID to check.
   * @param bundle
   *          bundle to check against.
   *
   * @return returns {@code true} if the target part of the targeted PID matches
   *         the given bundle.
   */
  private boolean targetedPidMatches(final String targetedPid,
                                     final Bundle bundle)
  {
    final StringTokenizer st = new StringTokenizer(targetedPid, "|");

    // Skip PID token
    if (!st.hasMoreTokens()) {
      return false;
    }
    st.nextToken();

    // Check Bundle Symbolic Name
    final String bsn = bundle.getSymbolicName();
    if (bsn==null) {
      // Targeted PIDs only supported for bundles with a BSN.
      return false;
    }
    if (st.hasMoreTokens() && !st.nextToken().equals(bsn)) {
      return false;
    }

    // Check Bundle version
    if (st.hasMoreTokens() && !st.nextToken().equals(bundle.getVersion().toString())) {
      return false;
    }

    // Check Bundle location
    if (st.hasMoreTokens() && !st.nextToken().equals(bundle.getLocation())) {
      return false;
    }

    return !st.hasMoreTokens();
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


  /**
   * Update managed services for a deleted targeted PID. This will switch to
   * another, less specific, targeted configuration if one exists.
   *
   * @param targetedPid
   *          the PID of the deleted configuration.
   * @param oldBundleLocation
   *          the location that the deleted configuration was bound to.
   * @param pid
   *          the PID of the deleted configuration without target specification.
   * @param srs
   *          collection of matching (on {@code pid}) managed services.
   * @throws IOException
   */
  private void updateManagedServicesForDeletedTargetedPID(final String targetedPid,
                                                          final String oldBundleLocation,
                                                          final String pid,
                                                          final Collection<ServiceReference<ManagedService>> srs)
      throws IOException
  {
    {
      final Collection<ServiceReference<ManagedService>> filtered =
        filterOnMatchingLocations(srs, oldBundleLocation);
      for (final ServiceReference<ManagedService> sr : filtered) {
        if (targetedPidMatches(targetedPid, sr.getBundle())) {
          // The target specification in the PID of the deleted configuration
          // matches the bundle owning the current MS, find the currently best
          // matching configuration if any.
          ConfigurationDictionary srCd = load(pid, sr.getBundle());
          if (srCd != null) {
            // Found matching configuration, is this a change of configuration?
            if (targetedPid.length() > srCd.getPid().length()) {
              // The deleted PID was a better match, must update MS with srCd.
              srCd = bindLocationIfNecessary(sr, srCd);
              final String newLocation = srCd.getLocation();
              if (!locationsMatch(sr.getBundle(), newLocation)) {
                // Multi-location region did not match, skip.
                srCd = null;
              }
            } else {
              srCd = null;
            }
          }
          configurationDispatcher.dispatchUpdateFor(sr, pid, null, srCd);
        }
      }
    }
  }

  private void updateManagedService(ServiceReference<ManagedService> sr)
      throws IOException
  {
    // Newly registered managed service; all PIDs are added.
    final ChangedPids cps = new ChangedPids();
    cps.added.addAll(Arrays.asList(getPids(sr)));
    updateManagedService(sr, cps);
  }

  private void updateManagedService(ServiceReference<ManagedService> sr,
                                    ChangedPids cps)
      throws IOException
  {
    for (final String servicePid : cps.added) {
      ConfigurationDictionary cd = load(servicePid, sr.getBundle());
      if (cd == null) {
        configurationDispatcher.dispatchUpdateFor(sr, servicePid, null, null);
      } else {
        cd = bindLocationIfNecessary(sr, cd);
        final String boundLocation = cd.getLocation();
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
      final ConfigurationDictionary cd = load(servicePid, sr.getBundle());
      if (cd != null) {
        configurationDispatcher.dispatchUpdateFor(sr, servicePid, null, null);
      }
    }
  }

  /**
   * Fetch all service references of the given service type that has a PID
   * matching the given one.
   *
   * @param c
   *          The service class.
   * @param pid
   *          The PID to match for.
   * @return collection with all service references that matches.
   */
  <C> Collection<ServiceReference<C>> getServiceReferencesWithPid(Class<C> c,
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
        @Override
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

  /**
   * Save configuration, increment change count and dispatch updates.
   *
   * @param c
   *          The configuration to save.
   * @param incrementChangeCount
   *          If {@code true} increment the change count.
   * @param dispatchUpdate
   *          If {@code true} dispatch updates to matching targets.
   * @throws IOException
   */
  void update(final ConfigurationImpl c,
              final boolean incrementChangeCount,
              final boolean dispatchUpdate)
      throws IOException
  {
    // TODO:
    // Should plugins still be called if service with
    // servicePid is not registered?

    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        @Override
        public Object run()
            throws IOException
        {
          store.store(c.getPid(), c.getFactoryPid(), c.properties, incrementChangeCount);
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
            @Override
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

  ConfigurationDictionary load(final String pid, final Bundle bundle)
      throws IOException
  {
    try {
      return AccessController
          .doPrivileged(new PrivilegedExceptionAction<ConfigurationDictionary>() {
            @Override
            public ConfigurationDictionary run()
                throws IOException
            {
              final List<String> targetPIDsuffixes = getTargetedPidSuffixes(bundle);
              for (int i = targetPIDsuffixes.size() - 1; i >= 0; i--) {
                final ConfigurationDictionary cd =
                  store.load(pid + targetPIDsuffixes.get(i));
                if (cd != null) {
                  return cd;
                }
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

  List<ConfigurationDictionary> loadAll(final String factoryPid, final Bundle bundle)
      throws IOException
  {
    try {
      return AccessController
          .doPrivileged(new PrivilegedExceptionAction<List<ConfigurationDictionary>>() {
            @Override
            public List<ConfigurationDictionary> run()
                throws IOException
            {
              final List<ConfigurationDictionary> res = new ArrayList<ConfigurationDictionary>();
              final List<String> targetedPidSuffixes = getTargetedPidSuffixes(bundle);
              for (int i = targetedPidSuffixes.size() - 1; i >= 0; i--) {
                final ConfigurationDictionary[] cda = store.loadAll(factoryPid + targetedPidSuffixes.get(i));
                if (cda != null && cda.length>0) {
                  res.addAll(Arrays.asList(cda));
                }
              }
              return res;
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
      final ConfigurationDictionary d = load(pid, null);
      if (d == null) {
        continue;
      }
      if (activeOnly && d.isNullDictionary()) {
        continue;
      }
      if (filter == null || filter.match(d)) {
        String configurationLocation = d.getLocation();
        configurationLocation =
          configurationLocation == null ? "*" : configurationLocation;
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

  @Override
  public ConfigurationAdmin getService(Bundle bundle,
                                       ServiceRegistration<ConfigurationAdmin> registration)
  {
    // For now we don't keep track of the services returned internally
    return new ConfigurationAdminImpl(bundle);
  }

  @Override
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
      this.factoryPid = properties.getFactoryPid();
      this.servicePid = properties.getPid();
      this.properties = properties;
    }


    /**
     * Create a configuration event with the specified type for this configuration
     * admin instance.
     *
     * @param type
     *          The type of the event to be created.
     * @return configuration event or {@code null} if unable to get the service
     *         reference for the current configuration admin service.
     */
    private ConfigurationEvent createEvent(final int type)
    {
      final ServiceReference<ConfigurationAdmin> reference =
        Activator.serviceRegistration.getReference();
      if (reference == null) {
        Activator.log
            .error("Could not get configuration admin "
                   + "service reference for configuariotn event creation.");
        return null;
      }
      return new ConfigurationEvent(reference, type, factoryPid, servicePid);
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

    @Override
    public void delete()
        throws IOException
    {
      throwIfDeleted();
      ConfigurationAdminFactory.this.delete(this);
      deleted = true;

      ConfigurationAdminFactory.this
          .sendEvent(createEvent(ConfigurationEvent.CM_DELETED));
    }

    private String getLocation()
    {
      final ConfigurationDictionary old = properties;
      try {
        properties = ConfigurationAdminFactory.this.load(servicePid, null);
        ConfigurationAdminFactory.this.unbindIfNecessary(properties);
      } catch (final IOException e) {
        properties = old;
      }
      return properties == null ? null : properties.getLocation();
    }

    @Override
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

    @Override
    public String getFactoryPid()
    {
      throwIfDeleted();
      return factoryPid;
    }

    @Override
    public String getPid()
    {
      throwIfDeleted();
      return servicePid;
    }

    @Override
    public Dictionary<String, Object> getProperties()
    {
      throwIfDeleted();
      // Ensure that this configuration object sees the most recent set of properties.
      try {
        this.properties = ConfigurationAdminFactory.this.load(servicePid, null);
      } catch (final IOException e) {
        this.properties = null; // TODO: proper error handling
      }

      if (properties == null) {
        return null;
      }
      return properties.createCopyIfRealAndRemoveLocation();
    }

    @Override
    public long getChangeCount()
    {
      throwIfDeleted();
      // Ensure that this configuration object sees the most recent set of properties.
      try {
        this.properties = ConfigurationAdminFactory.this.load(servicePid, null);
      } catch (final IOException e) {
        this.properties = null; // TODO: proper error handling
      }

      return properties != null ? properties.getChangeCount() : 0;
    }

    @Override
    public void setBundleLocation(String bundleLocation)
    {
      throwIfDeleted();
      checkConfigPerm(bundleLocation == null ? "*" : bundleLocation);
      final String oldLoc = getLocation();
      checkConfigPerm(oldLoc == null ? "*" : oldLoc);
      setBundleLocationAndPersist(bundleLocation);

      ConfigurationAdminFactory.this
          .sendEvent(createEvent(ConfigurationEvent.CM_LOCATION_CHANGED));

      Collection<ServiceReference<ManagedService>> srs = null;
      Collection<ServiceReference<ManagedServiceFactory>> srsF = null;
      if (factoryPid == null) {
        srs =
          ConfigurationAdminFactory.this
              .getServiceReferencesWithPid(ManagedService.class, servicePid);
      } else {
        srsF =
          ConfigurationAdminFactory.this
              .getServiceReferencesWithPid(ManagedServiceFactory.class,
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
        update(false, false);
      } catch (final IOException e) {
        e.printStackTrace();
        this.properties = old;
      }
    }

    @Override
    public void update()
        throws IOException
    {
      update(false, true);
    }

    /**
     * Save this configuration.
     *
     * @param incrementChangeCount
     *          If {@code true} increment the change count of this
     *          configuration.
     * @param dispatchUpdate
     *          If {@code true} inform managed services as well as configuration
     *          listeners about the update. If {@code false} this is an internal
     *          save operation that has not changed the configuration properties
     *          (except the bound location).
     * @throws IOException
     */
    void update(boolean incrementChangeCount, boolean dispatchUpdate)
        throws IOException
    {
      throwIfDeleted();
      ensureAutoPropertiesAreWritten();
      ConfigurationAdminFactory.this.update(this, incrementChangeCount, dispatchUpdate);
    }

    @Override
    public void update(Dictionary<String, ?> properties)
        throws IOException
    {
      throwIfDeleted();
      ConfigurationDictionary.validateDictionary(properties);
      try {
        this.properties = ConfigurationAdminFactory.this.load(servicePid, null);
      } catch (final IOException e) {
        this.properties = null; // TODO: proper error handling
      }
      final ConfigurationDictionary old = this.properties;
      if (properties == null) {
        this.properties = new ConfigurationDictionary();
      } else {
        this.properties = ConfigurationDictionary.createDeepCopy(properties);
        // Avoid overriding of CM internal props from properties
        this.properties.removeLocation();
      }

      // Copy CM internal props from the old configuration dictionary
      copyChangeCountFrom(old);
      copyBundleLocationFrom(old); // TODO: THIS IS WRONG!!!

      try {
        update(true, true);
        ConfigurationAdminFactory.this
            .sendEvent(createEvent(ConfigurationEvent.CM_UPDATED));
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
      final String location = old.getLocation();
      if (location != null) {
        putLocation(location);
      }

      final Object dynamic = old.get(DYNAMIC_BUNDLE_LOCATION);
      if (dynamic != null) {
        properties.put(DYNAMIC_BUNDLE_LOCATION, dynamic);
      }
    }

    void copyChangeCountFrom(ConfigurationDictionary old)
    {
      if (old!=null) {
        properties.setChangeCount(old.getChangeCount());
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

    @Override
    public Configuration createFactoryConfiguration(String factoryPid)
        throws IOException
    {
      // TODO: location check does not work for multi-location configurations.
      List<ConfigurationDictionary> d;
      try {
        d = ConfigurationAdminFactory.this.loadAll(factoryPid, null);
      } catch (final IOException ex) {
        d = null;
      }

      String locationFactoryPidIsBoundTo = null;
      if (d != null && !d.isEmpty()) {
        locationFactoryPidIsBoundTo = d.iterator().next().getLocation();
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
      c.update(false, false);
      return c;
    }

    @Override
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
      c.update(false, false);
      return c;
    }

    @Override
    public Configuration getConfiguration(String pid)
    {
      ConfigurationDictionary d;
      try {
        d = ConfigurationAdminFactory.this.load(pid, null);
      } catch (final IOException e) {
        d = null;
      }
      if (d == null) {
        final ConfigurationImpl c =
          new ConfigurationImpl(callingBundle, callingBundleLocation, null, pid);
        c.setBundleLocationAndPersist(callingBundleLocation);
        return c;
      }

      final String bundleLocation = d.getLocation();
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

    @Override
    public Configuration getConfiguration(String pid, String location)
    {
      ConfigurationDictionary d;
      /*
       * if(!this.callingBundleLocation.equals(location)) {
       * checkConfigPerm(location == null ? "*" : location); }
       */
      checkConfigPerm(location);
      try {
        d = ConfigurationAdminFactory.this.load(pid, null);
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
            c.update(false, false);
          } catch (final Exception e) {
            e.printStackTrace();
          }
        }
        return c;
      } else {
        final String bundleLocation = d.getLocation();
        checkConfigPerm(bundleLocation);
        final String factoryPid = d.getFactoryPid();
        return new ConfigurationImpl(callingBundle, bundleLocation, factoryPid,
                                     pid, d);
      }
    }

    @Override
    public Configuration[] listConfigurations(final String filterString)
        throws IOException, InvalidSyntaxException
    {
      Configuration[] configurations = null;

      try {
        configurations =
          AccessController
              .doPrivileged(new PrivilegedExceptionAction<Configuration[]>() {
                @Override
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

  @Override
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

  @Override
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
      if (factoryPids == null || factoryPids.length == 0) {
        final String bundleLocation = sr.getBundle().getLocation();
        Activator.log
            .error("[CM] ManagedServiceFactory w/o valid service.pid registered by "
                   + bundleLocation);
        return;
      }
      addToLocationToPidsAndCheck(sr, factoryPids);
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
      if (servicePids == null || servicePids.length == 0) {
        final String bundleLocation = sr.getBundle().getLocation();
        Activator.log
            .error("[CM] ManagedService w/o valid service.pid registered by "
                   + bundleLocation);
        return;
      }
      addToLocationToPidsAndCheck(sr, servicePids);
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

  /**
   * Build a list with targeted PID suffixes for the given bundle.
   *
   * @param bundle The bundle to create PID suffixes for.
   * @return List of PID suffixes in reverse priority order.
   */
  private List<String> getTargetedPidSuffixes(final Bundle bundle)
  {
    final List<String> targetPIDsuffixes = new ArrayList<String>(4);
    targetPIDsuffixes.add("");
    if (bundle != null) {
      final String bsn = bundle.getSymbolicName();
      // A bundle symbolic name is required for this feature!
      if (bsn != null) {
        final StringBuffer suffix = new StringBuffer("|").append(bsn);
        targetPIDsuffixes.add(suffix.toString());
        suffix.append('|').append(bundle.getVersion().toString());
        targetPIDsuffixes.add(suffix.toString());
        suffix.append('|').append(bundle.getLocation());
        targetPIDsuffixes.add(suffix.toString());
      }
    }
    return targetPIDsuffixes;
  }
}
