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
import java.net.URLStreamHandler;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.knopflerfish.framework.permissions.PermissionsHandle;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WovenClassListener;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;

public class SecurePermissionOps
  extends PermissionOps
{

  private static final int AP_CLASS = 0;
  private static final int AP_EXECUTE = 1;
  private static final int AP_EXTENSIONLIFECYCLE = 2;
  private static final int AP_LIFECYCLE = 3;
  private static final int AP_LISTENER = 4;
  private static final int AP_METADATA = 5;
  private static final int AP_RESOURCE = 6;
  private static final int AP_CONTEXT = 7;
  private static final int AP_WEAVE = 8;
  private static final int AP_MAX = 9;

  private static String[] AP_TO_STRING = new String[] {
                                                       AdminPermission.CLASS,
                                                       AdminPermission.EXECUTE,
                                                       AdminPermission.EXTENSIONLIFECYCLE,
                                                       AdminPermission.LIFECYCLE,
                                                       AdminPermission.LISTENER,
                                                       AdminPermission.METADATA,
                                                       AdminPermission.RESOURCE,
                                                       AdminPermission.CONTEXT,
                                                       AdminPermission.WEAVE };

  private final FrameworkContext framework;
  private PermissionsHandle ph;

  private AdminPermission ap_resolve = null;
  private AdminPermission ap_startlevel = null;

  private RuntimePermission rp_getprotectiondomain = null;

  Hashtable<Bundle, AdminPermission[]> adminPerms = new Hashtable<Bundle, AdminPermission[]>();

  public SecurePermissionOps(FrameworkContext fw)
  {
    framework = fw;
  }

  @Override
  void init()
  {
    ph = new PermissionsHandle(framework);
  }

  @Override
  void registerService()
  {
    if (framework.props
        .getBooleanProperty(FWProps.SERVICE_PERMISSIONADMIN_PROP)) {
      final String[] classes = new String[] { PermissionAdmin.class.getName() };
      framework.services.register(framework.systemBundle, classes,
                                  ph.getPermissionAdminService(), null);
    }
    if (framework.props
        .getBooleanProperty(FWProps.SERVICE_CONDITIONALPERMISSIONADMIN_PROP)) {
      final ConditionalPermissionAdmin cpa = ph
          .getConditionalPermissionAdminService();
      if (cpa != null) {
        final String[] classes = new String[] { ConditionalPermissionAdmin.class
            .getName() };
        framework.services.register(framework.systemBundle, classes, cpa, null);
      }
    }
  }

  @Override
  boolean checkPermissions()
  {
    return true;
  }

  //
  // Permission checks
  //

  @Override
  boolean okClassAdminPerm(Bundle b)
  {
    try {
      final SecurityManager sm = System.getSecurityManager();
      if (null != sm) {
        sm.checkPermission(getAdminPermission(b, AP_CLASS));
      }
      return true;
    } catch (final SecurityException _ignore) {
      return false;
    }
  }

  @Override
  void checkExecuteAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_EXECUTE));
    }
  }

  @Override
  void checkExtensionLifecycleAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_EXTENSIONLIFECYCLE));
    }
  }

  @Override
  void checkExtensionLifecycleAdminPerm(Bundle b, Object checkContext)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm && checkContext != null) {
      sm.checkPermission(getAdminPermission(b, AP_EXTENSIONLIFECYCLE),
                         checkContext);
    }
  }

  @Override
  void checkLifecycleAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_LIFECYCLE));
    }
  }

  @Override
  void checkLifecycleAdminPerm(Bundle b, Object checkContext)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm && checkContext != null) {
      sm.checkPermission(getAdminPermission(b, AP_LIFECYCLE), checkContext);
    }
  }

  @Override
  void checkListenerAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_LISTENER));
    }
  }

  @Override
  void checkMetadataAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_METADATA));
    }
  }

  @Override
  void checkResolveAdminPerm()
  {
    if (ap_resolve == null) {
      ap_resolve = new AdminPermission(framework.systemBundle,
                                       AdminPermission.RESOLVE);
    }
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(ap_resolve);
    }
  }

  @Override
  void checkResourceAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_RESOURCE));
    }
  }

  @Override
  boolean okResourceAdminPerm(Bundle b)
  {
    try {
      checkResourceAdminPerm(b);
      return true;
    } catch (final SecurityException ignore) {
      if (framework.debug.bundle_resource) {
        framework.debug
            .printStackTrace("No permission to access resources in bundle #"
                             + b.getBundleId(), ignore);
      }
      return false;
    }
  }

  @Override
  void checkContextAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_CONTEXT));
    }
  }

  @Override
  void checkStartLevelAdminPerm()
  {
    if (ap_startlevel == null) {
      ap_startlevel = new AdminPermission(framework.systemBundle,
                                          AdminPermission.STARTLEVEL);
    }
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(ap_startlevel);
    }
  }

  @Override
  void checkGetProtectionDomain()
  {
    if (rp_getprotectiondomain == null) {
      rp_getprotectiondomain = new RuntimePermission("getProtectionDomain");
    }
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(rp_getprotectiondomain);
    }
  }

  @Override
  void checkWeaveAdminPerm(Bundle b)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_WEAVE));
    }
  }

  //
  // Bundle permission checks
  //

  @Override
  boolean okFragmentBundlePerm(BundleImpl b)
  {
    final PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.getSymbolicName(),
                                           BundlePermission.FRAGMENT));
  }

  @Override
  boolean okHostBundlePerm(BundleImpl b)
  {
    final PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.getSymbolicName(),
                                           BundlePermission.HOST));
  }

  @Override
  boolean okProvideBundlePerm(BundleImpl b)
  {
    final PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.getSymbolicName(),
                                           BundlePermission.PROVIDE));
  }

  @Override
  boolean okRequireBundlePerm(BundleImpl b)
  {
    final PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.getSymbolicName(),
                                           BundlePermission.REQUIRE));
  }

  @Override
  boolean okAllPerm(BundleImpl b)
  {
    final PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new AllPermission());
  }

  //
  // Package permission checks
  //

  @Override
  boolean hasExportPackagePermission(ExportPkg ep)
  {
    final BundleImpl b = ep.bpkgs.bg.bundle;
    if (b.id != 0) {
      final PermissionCollection pc = ph
          .getPermissionCollection(new Long(b.id));
      return pc.implies(new PackagePermission(ep.name,
                                              PackagePermission.EXPORTONLY));
    }
    return true;
  }

  @Override
  boolean hasImportPackagePermission(BundleImpl b, ExportPkg ep)
  {
    if (b.id != 0) {
      final PermissionCollection pc = ph
          .getPermissionCollection(new Long(b.id));
      return pc.implies(new PackagePermission(ep.name, ep.bpkgs.bg.bundle,
                                              PackagePermission.IMPORT));
    }
    return true;
  }

  @Override
  void checkImportPackagePermission(String pkg)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(new PackagePermission(pkg, PackagePermission.IMPORT));
    }
  }

  //
  // Service permission checks
  //

  @Override
  void checkRegisterServicePerm(String clazz)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(new ServicePermission(clazz,
                                               ServicePermission.REGISTER));
    }
  }

  @Override
  void checkGetServicePerms(ServiceReference<?> sr)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(new ServicePermission(sr, ServicePermission.GET));
    }
  }

  @Override
  boolean okGetServicePerms(ServiceReference<?> sr)
  {
    try {
      checkGetServicePerms(sr);
      return true;
    } catch (final SecurityException ignore) {
      if (framework.debug.service_reference) {
        framework.debug
            .printStackTrace("No permission to get service ref: "
                                 + sr.getProperty(Constants.OBJECTCLASS),
                             ignore);
      }
    }
    return false;
  }

  /**
   * Filter out all services that we don't have permission to get.
   *
   * @param srs
   *          Set of ServiceRegistrationImpls to check.
   */
  @Override
  void filterGetServicePermission(Set<ServiceRegistrationImpl<?>> srs)
  {
    for (final Iterator<ServiceRegistrationImpl<?>> i = srs.iterator(); i
        .hasNext();) {
      final ServiceRegistrationImpl<?> sr = i.next();
      ;
      if (!okGetServicePerms(sr.getReference())) {
        i.remove();
      }
    }
  }

  //
  // Capability and Requirement checks
  //

  @Override
  boolean hasProvidePermission(BundleCapabilityImpl bc) {
    final BundleImpl b = bc.getBundleGeneration().bundle;
    if (b.id != 0) {
      final PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
      return pc.implies(new CapabilityPermission(bc.getNamespace(),
                                                 CapabilityPermission.PROVIDE));
    }
    return true;
  }


  @Override
  boolean hasRequirePermission(BundleRequirementImpl br) {
    final BundleImpl b = br.getBundleGeneration().bundle;
    if (b.id != 0) {
      final PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
      return pc.implies(new CapabilityPermission(br.getNamespace(),
                                                 CapabilityPermission.REQUIRE));
    }
    return true;
  }


  @Override
  boolean hasRequirePermission(BundleRequirementImpl br, BundleCapabilityImpl bc) {
    final BundleImpl bbr = br.getBundleGeneration().bundle;
    if (bbr.id != 0) {
      final PermissionCollection pc = ph.getPermissionCollection(new Long(bbr.id));
      return pc.implies(new CapabilityPermission(bc.getNamespace(),
                                                 bc.getAttributes(),
                                                 bc.getBundleGeneration().bundle,
                                                 CapabilityPermission.REQUIRE));
    }
    return true;
  }

  //
  // AdaptPermission checks
  //

  @Override
  <A> void checkAdaptPerm(BundleImpl b, Class<A> type)
  {
    final SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(new AdaptPermission(type.getName(), b,
                                             AdaptPermission.ADAPT));
    }
  }

  //
  // BundleArchive secure operations
  //

  @Override
  BundleResourceStream callGetBundleResourceStream(final BundleArchive archive,
                                                   final String name,
                                                   final int ix)
  {
    return AccessController
        .doPrivileged(new PrivilegedAction<BundleResourceStream>() {
          public BundleResourceStream run()
          {
            return archive.getBundleResourceStream(name, ix);
          }
        });
  }

  @Override
  Enumeration<String> callFindResourcesPath(final BundleArchive archive,
                                            final String path)
  {
    return AccessController
        .doPrivileged(new PrivilegedAction<Enumeration<String>>() {
          public Enumeration<String> run()
          {
            return archive.findResourcesPath(path);
          }
        });
  }

  //
  // BundleClassLoader secure operations
  //

  @Override
  Object callSearchFor(final BundleClassLoader cl,
                       final String name,
                       final String pkg,
                       final String path,
                       final BundleClassLoader.SearchAction action,
                       final int options,
                       final BundleClassLoader requestor,
                       final HashSet<BundleClassLoader> visited)
  {
    return AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        return cl.searchFor(name, pkg, path, action, options, requestor,
                            visited);
      }
    });
  }

  @Override
  String callFindLibrary0(final BundleClassLoader cl, final String name)
  {
    return AccessController.doPrivileged(new PrivilegedAction<String>() {
      public String run()
      {
        return cl.findLibrary0(name);
      }
    });
  }

  //
  // BundleImpl secure operations
  //

  @Override
  void callFinalizeActivation(final BundleImpl b)
      throws BundleException
  {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        public Object run()
            throws BundleException
        {
          b.finalizeActivation();
          return null;
        }
      });
    } catch (final PrivilegedActionException e) {
      throw (BundleException) e.getException();
    }
  }

  @Override
  BundleThread createBundleThread(final FrameworkContext fc)
  {
    return AccessController.doPrivileged(new PrivilegedAction<BundleThread>() {
      public BundleThread run()
      {
        return new BundleThread(fc);
      }
    });
  }

  @Override
  void callUpdate0(final BundleImpl b,
                   final InputStream in,
                   final boolean wasActive)
      throws BundleException
  {
    try {
      final AccessControlContext acc = AccessController.getContext();
      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        public Object run()
            throws BundleException
        {
          b.update0(in, wasActive, acc);
          return null;
        }
      });
    } catch (final PrivilegedActionException e) {
      throw (BundleException) e.getException();
    }
  }

  @Override
  void callUninstall0(final BundleImpl b)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        b.uninstall0();
        return null;
      }
    });
  }

  @Override
  void callSetAutostartSetting(final BundleImpl b, final int settings)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        b.setAutostartSetting0(settings);
        return null;
      }
    });
  }

  @Override
  HeaderDictionary callGetHeaders0(final BundleGeneration bg,
                                   final String locale)
  {
    return AccessController
        .doPrivileged(new PrivilegedAction<HeaderDictionary>() {
          public HeaderDictionary run()
          {
            return bg.getHeaders0(locale);
          }
        });
  }

  @Override
  Vector<URL> callFindEntries(final BundleGeneration bg,
                              final String path,
                              final String filePattern,
                              final boolean recurse)
  {
    return AccessController.doPrivileged(new PrivilegedAction<Vector<URL>>() {
      public Vector<URL> run()
      {
        return bg.findEntries(path, filePattern, recurse);
      }
    });
  }

  @Override
  BundleClassLoader newBundleClassLoader(final BundleGeneration bg)
      throws BundleException
  {
    try {
      return AccessController
          .doPrivileged(new PrivilegedExceptionAction<BundleClassLoader>() {
            public BundleClassLoader run()
                throws Exception
            {
              return new BundleClassLoader(bg);
            }
          });
    } catch (final PrivilegedActionException pe) {
      throw (BundleException) pe.getException();
    }
  }

  Vector<URL> getBundleClassPathEntry(final BundleGeneration bg,
                                      final String name,
                                      final boolean onlyFirst)
  {
    return  AccessController.doPrivileged(new PrivilegedAction<Vector<URL>>() {
      public Vector<URL> run()
      {
        return bg.getBundleClassPathEntries(name, onlyFirst);
      }
    });
  }

  @Override
  AccessControlContext getAccessControlContext(BundleImpl bundle) {
    ProtectionDomain pd = bundle.current().getProtectionDomain();
    return pd != null ?new AccessControlContext(new ProtectionDomain[] {pd}) : null;
  }

  //
  // Bundles Secure operation
  //

  @Override
  BundleImpl callInstall0(final Bundles bs,
                          final String location,
                          final InputStream in,
                          final Bundle caller)
      throws BundleException
  {
    try {
      final AccessControlContext acc = AccessController.getContext();
      return  AccessController
          .doPrivileged(new PrivilegedExceptionAction<BundleImpl>() {
            public BundleImpl run()
                throws BundleException
            {
              return bs.install0(location, in, acc, caller);
            }
          });
    } catch (final PrivilegedActionException e) {
      throw (BundleException) e.getException();
    }
  }

  //
  // Listeners Secure operations
  //

  @Override
  void callBundleChanged(final FrameworkContext fwCtx, final BundleEvent evt)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        fwCtx.listeners.bundleChanged(evt);
        return null;
      }
    });
  }

  @Override
  void callServiceChanged(final FrameworkContext fwCtx,
                          final Collection<ServiceListenerEntry> receivers,
                          final ServiceEvent evt,
                          final Set<ServiceListenerEntry> matchBefore)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        fwCtx.listeners.serviceChanged(receivers, evt, matchBefore);
        return null;
      }
    });
  }

  //
  // PackageAdmin secure operations
  //

  @Override
  void callRefreshPackages0(final PackageAdminImpl pa,
                            final Bundle[] bundles,
                            final FrameworkListener[] fl)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        pa.refreshPackages0(bundles, fl);
        return null;
      }
    });
  }

  //
  // ServiceRegisterationImpl secure operations
  //

  @Override
  <S> S callGetService(final ServiceRegistrationImpl<S> sr, final Bundle b)
  {
    return AccessController.doPrivileged(new PrivilegedAction<S>() {
      public S run()
      {
        @SuppressWarnings("unchecked")
        final ServiceFactory<S> srf = (ServiceFactory<S>) sr.service;
        return srf.getService(b, sr);
      }
    });
  }

  @Override
  <S> void callUngetService(final ServiceRegistrationImpl<S> sr,
                            final Bundle b,
                            final S instance)
  {
    @SuppressWarnings("unchecked")
    final ServiceFactory<S> srf = (ServiceFactory<S>) sr.service;
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        srf.ungetService(b, sr, instance);
        return null;
      }
    });
  }

  //
  // StartLevelController secure operations
  //

  @Override
  void callSetStartLevel(final BundleImpl b, final int startlevel)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        b.setStartLevel(startlevel);
        return null;
      }
    });
  }

  @Override
  void callSetInitialBundleStartLevel0(final StartLevelController slc,
                                       final int startlevel)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        slc.setInitialBundleStartLevel0(startlevel, true);
        return null;
      }
    });
  }

  //
  // SystemBundle secure operations
  //

  @Override
  void callShutdown(final SystemBundle sb, final boolean restart)
  {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run()
      {
        sb.shutdown(restart);
        return null;
      }
    });
  }

  //
  // Permissions package functionality
  //

  /**
   * Get protection domain for bundle
   */
  @Override
  ProtectionDomain getProtectionDomain(final BundleGeneration bg)
  {
    try {
      // We cannot use getBundleURL() here because that will
      // trigger a persmission check while we're still in
      // the phase of building permissions
      String h = Long.toString(bg.bundle.id);
      if (bg.generation != 0) {
        h += "." + Long.toString(bg.generation);
      }
      final URLStreamHandler ush = bg.bundle.fwCtx.urlStreamHandlerFactory
          .createURLStreamHandler(BundleURLStreamHandler.PROTOCOL);
      final URL bundleUrl = new URL(BundleURLStreamHandler.PROTOCOL, h, -1, "",
                                    ush);

      final InputStream pis = bg.archive
          .getBundleResourceStream("OSGI-INF/permissions.perm", 0);
      final PermissionCollection pc = ph
          .createPermissionCollection(bg.bundle.location, bg.bundle, pis);
      final ArrayList<List<X509Certificate>> cc = bg.archive.getCertificateChains(false);
      Certificate[] cca;
      if (cc != null) {
        final ArrayList<X509Certificate> tmp = new ArrayList<X509Certificate>();
        for (final List<X509Certificate> list : cc) {
          tmp.addAll(list);
        }
        cca = tmp.toArray(new Certificate[tmp.size()]);
      } else {
        cca = null;
      }
      return new ProtectionDomain(new CodeSource(bundleUrl, cca), pc);
    } catch (final MalformedURLException _ignore) {
    }
    return null;
  }

  @Override
  URL getBundleURL(final FrameworkContext fwCtx, final String s)
      throws MalformedURLException
  {
    try {
      return AccessController
          .doPrivileged(new PrivilegedExceptionAction<URL>() {
            public URL run()
                throws MalformedURLException
            {
              return new URL(null, s, fwCtx.urlStreamHandlerFactory
                  .createURLStreamHandler(BundleURLStreamHandler.PROTOCOL));
            }
          });
    } catch (final PrivilegedActionException e) {
      throw (MalformedURLException) e.getException();
    }
  }

  @Override
  WovenClassListener getWovenClassListener() {
    return ph;
  }

  //
  // Privileged system calls
  //

  @Override
  ClassLoader getClassLoaderOf(final Class<?> c)
  {
    return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
      public ClassLoader run()
      {
        return c.getClassLoader();
      }
    });
  }

  //
  // Cleaning
  //

  /**
   * Purge all cached information for specified bundle.
   */
  @Override
  void purge(BundleImpl b, ProtectionDomain pd)
  {
    if (ph.purgePermissionCollection(new Long(b.id), pd.getPermissions())) {
      adminPerms.remove(b);
    }
  }

  //
  // Private
  //

  AdminPermission getAdminPermission(Bundle b, int ti)
  {
    AdminPermission[] res;
    res = adminPerms.get(b);
    if (res != null) {
      if (res[ti] != null) {
        return res[ti];
      }
    } else {
      res = new AdminPermission[AP_MAX];
      adminPerms.put(b, res);
    }
    res[ti] = new AdminPermission(b, AP_TO_STRING[ti]);
    return res[ti];
  }
}
