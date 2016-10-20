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

package org.knopflerfish.framework;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WovenClassListener;

/**
 * This is a wrapper class for operations that requires some kind of security
 * checks or privileged operations.
 */
class PermissionOps {

  void init() {
  }


  void registerService() {
  }


  boolean checkPermissions() {
    return false;
  }


  //
  // Permission checks
  //

  boolean okClassAdminPerm(Bundle b) {
    return true;
  }


  void checkExecuteAdminPerm(Bundle b) {
  }


  void checkExtensionLifecycleAdminPerm(Bundle b) {
  }


  void checkExtensionLifecycleAdminPerm(Bundle b, Object checkContext) {
  }


  void checkLifecycleAdminPerm(Bundle b) {
  }


  void checkLifecycleAdminPerm(Bundle b, Object checkContext) {
  }


  void checkListenerAdminPerm(Bundle b) {
  }


  void checkMetadataAdminPerm(Bundle b) {
  }


  void checkResolveAdminPerm() {
  }


  void checkResourceAdminPerm(Bundle b) {
  }


  boolean okResourceAdminPerm(Bundle b) {
    return true;
  }


  void checkContextAdminPerm(Bundle b) {
  }


  void checkStartLevelAdminPerm() {
  }


  void checkGetProtectionDomain() {
  }


  void checkWeaveAdminPerm(Bundle b) {
  }

  //
  // Bundle permission checks
  //

  boolean okFragmentBundlePerm(BundleImpl b) {
    return true;
  }


  boolean okHostBundlePerm(BundleImpl b) {
    return true;
  }


  boolean okProvideBundlePerm(BundleImpl b) {
    return true;
  }


  boolean okRequireBundlePerm(BundleImpl b) {
    return true;
  }


  boolean okAllPerm(BundleImpl b) {
    return true;
  }


  //
  // Package permission checks
  //

  boolean hasExportPackagePermission(ExportPkg ep) {
    return true;
  }


  boolean hasImportPackagePermission(BundleImpl b, ExportPkg ep) {
    return true;
  }


  void checkImportPackagePermission(String pkg) {
  }

  //
  // Service permission checks
  //

  void checkRegisterServicePerm(String clazz) {
  }


  void checkGetServicePerms(ServiceReference<?> sr) {
  }


  boolean okGetServicePerms(ServiceReference<?> sr) {
    return true;
  }


  void filterGetServicePermission(Set<ServiceRegistrationImpl<?>> srs) {
  }

  //
  // Capability and Requirement checks
  //

  boolean hasProvidePermission(BundleCapabilityImpl bc) {
    return true;
  }


  boolean hasRequirePermission(BundleRequirementImpl br) {
    return true;
  }


  boolean hasRequirePermission(BundleRequirementImpl br, BundleCapabilityImpl bc) {
    return true;
  }

  //
  // AdaptPermission checks
  //

  <A> void checkAdaptPerm(BundleImpl b, Class<A> type) {
  }

  //
  // BundleArchive secure operations
  //

  BundleResourceStream callGetBundleResourceStream(final BundleArchive archive,
                                                   final String name, final int ix) {
    return archive.getBundleResourceStream(name, ix);
  }


  Enumeration<String> callFindResourcesPath(final BundleArchive archive, final String path) {
    return archive.findResourcesPath(path);
  }


  //
  // BundleClassLoader secure operations
  //

  Object callSearchFor(final BundleClassLoader cl, final String name, final String pkg,
                       final String path, final BundleClassLoader.SearchAction action,
                       final int options,
                       final BundleClassLoader requestor, final HashSet<BundleClassLoader> visited) {
    return cl.searchFor(name, pkg, path, action, options, requestor, visited);
  }


  String callFindLibrary0(final BundleClassLoader cl, final String name) {
    return cl.findLibrary0(name);
  }


  //
  // BundleImpl secure operations
  //

  void callFinalizeActivation(final BundleImpl b) throws BundleException {
    b.finalizeActivation();
  }


  BundleThread createBundleThread(final FrameworkContext fc) {
    return new BundleThread(fc);
  }


  void callUpdate0(final BundleImpl b, final InputStream in, final boolean wasActive)
      throws BundleException {
    b.update0(in, wasActive, null);
  }


  void callUninstall0(final BundleImpl b) {
    b.uninstall0();
  }


  void callSetAutostartSetting(final BundleImpl b, final int setting) {
    b.setAutostartSetting0(setting);
  }


  HeaderDictionary callGetHeaders0(final BundleGeneration bg, final String locale) {
    return bg.getHeaders0(locale);
  }


  Vector<URL> callFindEntries(final BundleGeneration bg, final String path,
                              final String filePattern, final boolean recurse) {
    return bg.findEntries(path, filePattern, recurse);
  }


  BundleClassLoader newBundleClassLoader(final BundleGeneration bg) throws BundleException {
    return new BundleClassLoader(bg);
  }


  Vector<URL> getBundleClassPathEntries(final BundleGeneration bg, final String name, final boolean onlyFirst) {
    return bg.getBundleClassPathEntries(name, onlyFirst);
  }


  AccessControlContext getAccessControlContext(BundleImpl bundle) {
    return null;
  }

  //
  // Bundles Secure operation
  //

  BundleImpl callInstall0(final Bundles bs, final String location, final InputStream in, final Bundle caller)
      throws BundleException {
    return bs.install0(location, in, null, caller);
  }


  //
  // Listeners Secure operations
  //

  void callBundleChanged(final FrameworkContext fwCtx, final BundleEvent evt) {
    fwCtx.listeners.bundleChanged(evt);
  }


  void callServiceChanged(final FrameworkContext fwCtx, final Collection<ServiceListenerEntry> receivers,
                          final ServiceEvent evt, final Set<ServiceListenerEntry> matchBefore) {
    fwCtx.listeners.serviceChanged(receivers, evt, matchBefore);
  }


  //
  // PackageAdmin secure operations
  //

  void callRefreshPackages0(final PackageAdminImpl pa, final Bundle[] bundles, final FrameworkListener[] fl) {
    pa.refreshPackages0(bundles, fl);
  }


  //
  // ServiceRegisterationImpl secure operations
  //

  <S> S callGetService(final ServiceRegistrationImpl<S> sr, final Bundle b) {
    @SuppressWarnings("unchecked")
    final ServiceFactory<S> srf = (ServiceFactory<S>)sr.service;
    return srf.getService(b, sr);
  }


  <S> void callUngetService(final ServiceRegistrationImpl<S> sr,
                            final Bundle b,
                            final S instance)
  {
    @SuppressWarnings("unchecked")
    final ServiceFactory<S> srf = (ServiceFactory<S>)sr.service;
    srf.ungetService(b, sr, instance);
  }


  //
  // StartLevelController secure operations
  //

  void callSetStartLevel(final BundleImpl b, final int startlevel) {
    b.setStartLevel(startlevel);
  }

  void callSetInitialBundleStartLevel0(final StartLevelController slc,
                                       final int startlevel) {
    slc.setInitialBundleStartLevel0(startlevel, true);
  }

  //
  // SystemBundle secure operations
  //

  void callShutdown(final SystemBundle sb, final boolean restart) {
    sb.shutdown(restart);
  }


  //
  // Permissions package functionality
  //

  ProtectionDomain getProtectionDomain(final BundleGeneration bg) {
    return null;
  }


  /**
   * Get bundle URL using a bundle: spec and the BundleURLStreamHandler.
   *
   * <p>
   * Note:<br>
   * Creating bundle: URLs by the URL(String) constructor will only work if the
   * the fw URL handler is registered, which may be turned off.
   * </p>
   */
  URL getBundleURL(FrameworkContext fwCtx, String s) throws MalformedURLException {
    return new URL(null, s,
        fwCtx.urlStreamHandlerFactory.createURLStreamHandler(BundleURLStreamHandler.PROTOCOL));
  }


  WovenClassListener getWovenClassListener() {
    return null;
  }

  //
  // Privileged system calls
  //

  ClassLoader getClassLoaderOf(final Class<?> c) {
    return c.getClassLoader();
  }


  //
  // Cleaning
  //

  /**
   * Purge all cached information for specified bundle.
   */
  void purge(BundleImpl b, ProtectionDomain pc) {
  }


}
