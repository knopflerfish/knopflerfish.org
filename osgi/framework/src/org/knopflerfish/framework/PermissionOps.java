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

package org.knopflerfish.framework;

import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.*;

import org.osgi.framework.*;


/**
 * This is a wrapper class for operations that requires some kind
 * of security checks.
 */
class PermissionOps {

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
  
  void checkStartLevelAdminPerm() {
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

  //
  // Package permission checks
  //

  boolean hasImportPackagePermission(BundleImpl b, String pkg) {
    return true;
  }


  /**
   * Check that we have right export and import package permission for the bundle.
   *
   * @return Returns null if we have correct permission for listed package.
   *         Otherwise a string of failed entries.
   */
  String missingMandatoryPackagePermissions(BundlePackages bpkgs, List okImports) {
    for (Iterator i = bpkgs.getImports(); i.hasNext(); ) {
      okImports.add(i.next());
    }
    return null;
  }

  //
  // Service permission checks
  //  

  void checkRegisterServicePerm(String clazz) {
  }

  boolean okGetServicePerm(String clazz) {
    return true;
  }

  void checkGetServicePerms(String [] classes) {
  }

  boolean okGetServicePerms(String [] classes) {
    return true;
  }

  void filterGetServicePermission(Set srs) {
  }

  //
  // BundleArchive secure operations
  //

  InputStream callGetInputStream(final BundleArchive archive,
                                 final String name,
                                 final int ix) {
    return archive.getInputStream(name, ix);
  }


  Enumeration callFindResourcesPath(final BundleArchive archive,
                                    final String path) {
    return archive.findResourcesPath(path);
  }

  //
  // BundleClassLoader secure operations
  //

  Object callSearchFor(final BundleClassLoader cl,
                       final String name,
                       final String pkg,
                       final String path,
                       final BundleClassLoader.SearchAction action,
                       final boolean onlyFirst,
                       final BundleClassLoader requestor,
                       final HashSet visited) {
    return cl.searchFor(name, pkg, path, action, onlyFirst, requestor, visited);
  }

  //
  // BundleImpl Secure operations
  //

  void callStart0(final BundleImpl b) throws BundleException {
    b.start0();
  }


  BundleException callStop0(final BundleImpl b, final boolean resetPersistent)  {
    return b.stop0(resetPersistent);
  }


  void callUpdate0(final BundleImpl b, final InputStream in, final boolean wasActive)
    throws BundleException {
    b.update0(in, wasActive);
  }


  void callUninstall0(final BundleImpl b) {
    b.uninstall0();
  }


  void callStartOnLaunch(final BundleImpl b, final boolean flag) {
    b.startOnLaunch(flag);
  }


  void callSetPersistent(final BundleImpl b, final boolean flag) {
    b.setPersistent(flag);
  }


  ClassLoader callGetClassLoader0(final BundleImpl b) {
    return b.getClassLoader0();
  }


  HeaderDictionary callGetHeaders0(final BundleImpl b, final String locale) {
    return b.getHeaders0(locale);
  }


  Enumeration callFindEntries0(final BundleImpl b, final String path,
                               final String filePattern, final boolean recurse) {
    return b.findEntries0(path, filePattern, recurse);
  }

  //
  // Bundles Secure operation
  //

  BundleImpl callInstall0(final Bundles bs, final String location, final InputStream in)
    throws BundleException {
    return bs.install0(location, in, null);
  }

  //
  // Listeners Secure operations
  //

  void callBundleChanged(final BundleListener bl, final BundleEvent evt) {
     bl.bundleChanged(evt);
  }

  void callFrameworkEvent(final FrameworkListener fl, final FrameworkEvent evt) {
     fl.frameworkEvent(evt);
  }

  void callServiceChanged(final ServiceListener sl, final ServiceEvent evt) {
     sl.serviceChanged(evt);
  }

  //
  // Main Secure operations
  //

  void callMainRestart() {
     Main.restart();
  }

  void callMainShutdown(final int exitcode) {
     Main.shutdown(exitcode);
  }

  //
  // PackageAdmin secure operations
  //

  void callRefreshPackages0(final PackageAdminImpl pa, final Bundle [] bundles) {
    pa.refreshPackages0(bundles);
  }

  //
  // ServiceReferenceImpl secure operations
  //

  Object callGetService(final ServiceFactory sf,
                        final Bundle b,
                        final ServiceRegistration sr) {
    return sf.getService(b, sr);
  }

  //
  // ServiceRegisterationImpl secure operations
  //

  void callUnregister0(final ServiceRegistrationImpl sr) {
    sr.unregister0();
  }

  //
  // Permissions package functionality
  //

  ProtectionDomain getProtectionDomain(BundleImpl b) {
    return null;
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
