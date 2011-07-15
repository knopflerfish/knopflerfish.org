/*
 * Copyright (c) 2006-2011, KNOPFLERFISH project
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
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;

import org.knopflerfish.framework.PermissionOps;
import org.knopflerfish.framework.permissions.PermissionsHandle;

class SecurePermissionOps extends PermissionOps {

  private static final int AP_CLASS = 0;
  private static final int AP_EXECUTE = 1;
  private static final int AP_EXTENSIONLIFECYCLE = 2;
  private static final int AP_LIFECYCLE = 3;
  private static final int AP_LISTENER = 4;
  private static final int AP_METADATA = 5;
  private static final int AP_RESOURCE = 6;
  private static final int AP_CONTEXT = 7;
  private static final int AP_MAX = 8;

  private static String[] AP_TO_STRING = new String[] { AdminPermission.CLASS,
      AdminPermission.EXECUTE, AdminPermission.EXTENSIONLIFECYCLE, AdminPermission.LIFECYCLE,
      AdminPermission.LISTENER, AdminPermission.METADATA, AdminPermission.RESOURCE,
      AdminPermission.CONTEXT, };

  private final FrameworkContext framework;
  private PermissionsHandle ph;

  private AdminPermission ap_resolve = null;
  private AdminPermission ap_startlevel = null;

  private RuntimePermission rp_getprotectiondomain = null;

  Hashtable /* Bundle -> AdminPermission [] */adminPerms = new Hashtable();


  SecurePermissionOps(FrameworkContext fw) {
    framework = fw;
  }


  void init() {
    ph = new PermissionsHandle(framework);
  }


  void registerService() {
    if (framework.props.getBooleanProperty(FWProps.SERVICE_PERMISSIONADMIN_PROP)) {
      String[] classes = new String[] { PermissionAdmin.class.getName() };
      framework.services.register(framework.systemBundle, classes,
          ph.getPermissionAdminService(), null);
    }
    if (framework.props.getBooleanProperty(FWProps.SERVICE_CONDITIONALPERMISSIONADMIN_PROP)) {
      ConditionalPermissionAdmin cpa = ph.getConditionalPermissionAdminService();
      if (cpa != null) {
        String[] classes = new String[] { ConditionalPermissionAdmin.class.getName() };
        framework.services.register(framework.systemBundle, classes, cpa, null);
      }
    }
  }


  boolean checkPermissions() {
    return true;
  }


  //
  // Permission checks
  //

  boolean okClassAdminPerm(Bundle b) {
    try {
      SecurityManager sm = System.getSecurityManager();
      if (null != sm) {
        sm.checkPermission(getAdminPermission(b, AP_CLASS));
      }
      return true;
    } catch (SecurityException _ignore) {
      return false;
    }
  }


  void checkExecuteAdminPerm(Bundle b) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_EXECUTE));
    }
  }


  void checkExtensionLifecycleAdminPerm(Bundle b) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_EXTENSIONLIFECYCLE));
    }
  }


  void checkExtensionLifecycleAdminPerm(Bundle b, Object checkContext) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm && checkContext != null) {
      sm.checkPermission(getAdminPermission(b, AP_EXTENSIONLIFECYCLE), checkContext);
    }
  }


  void checkLifecycleAdminPerm(Bundle b) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_LIFECYCLE));
    }
  }


  void checkLifecycleAdminPerm(Bundle b, Object checkContext) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm && checkContext != null) {
      sm.checkPermission(getAdminPermission(b, AP_LIFECYCLE), checkContext);
    }
  }


  void checkListenerAdminPerm(Bundle b) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_LISTENER));
    }
  }


  void checkMetadataAdminPerm(Bundle b) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_METADATA));
    }
  }


  void checkResolveAdminPerm() {
    if (ap_resolve == null) {
      ap_resolve = new AdminPermission(framework.systemBundle, AdminPermission.RESOLVE);
    }
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(ap_resolve);
    }
  }


  void checkResourceAdminPerm(Bundle b) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_RESOURCE));
    }
  }


  boolean okResourceAdminPerm(Bundle b) {
    try {
      checkResourceAdminPerm(b);
      return true;
    } catch (SecurityException ignore) {
      if (framework.debug.bundle_resource) {
        framework.debug.printStackTrace(
            "No permission to access resources in bundle #" + b.getBundleId(), ignore);
      }
      return false;
    }
  }


  void checkContextAdminPerm(Bundle b) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(getAdminPermission(b, AP_CONTEXT));
    }
  }


  void checkStartLevelAdminPerm() {
    if (ap_startlevel == null) {
      ap_startlevel = new AdminPermission(framework.systemBundle, AdminPermission.STARTLEVEL);
    }
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(ap_startlevel);
    }
  }


  void checkGetProtectionDomain() {
    if (rp_getprotectiondomain == null) {
      rp_getprotectiondomain = new RuntimePermission("getProtectionDomain");
    }
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(rp_getprotectiondomain);
    }
  }


  //
  // Bundle permission checks
  //

  boolean okFragmentBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.gen.symbolicName, BundlePermission.FRAGMENT));
  }


  boolean okHostBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.gen.symbolicName, BundlePermission.HOST));
  }


  boolean okProvideBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.gen.symbolicName, BundlePermission.PROVIDE));
  }


  boolean okRequireBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.gen.symbolicName, BundlePermission.REQUIRE));
  }


  boolean okAllPerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new AllPermission());
  }


  //
  // Package permission checks
  //

  boolean hasExportPackagePermission(ExportPkg ep) {
    BundleImpl b = ep.bpkgs.bg.bundle;
    if (b.id != 0) {
      PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
      return pc.implies(new PackagePermission(ep.name, PackagePermission.EXPORTONLY));
    }
    return true;
  }


  boolean hasImportPackagePermission(BundleImpl b, ExportPkg ep) {
    if (b.id != 0) {
      PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
      return pc.implies(new PackagePermission(ep.name, ep.bpkgs.bg.bundle,
          PackagePermission.IMPORT));
    }
    return true;
  }


  //
  // Service permission checks
  //

  void checkRegisterServicePerm(String clazz) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(new ServicePermission(clazz, ServicePermission.REGISTER));
    }
  }


  void checkGetServicePerms(ServiceReference sr) {
    SecurityManager sm = System.getSecurityManager();
    if (null != sm) {
      sm.checkPermission(new ServicePermission(sr, ServicePermission.GET));
    }
  }


  boolean okGetServicePerms(ServiceReference sr) {
    try {
      checkGetServicePerms(sr);
      return true;
    } catch (SecurityException ignore) {
      if (framework.debug.service_reference) {
        framework.debug.printStackTrace(
            "No permission to get service ref: " + sr.getProperty(Constants.OBJECTCLASS),
            ignore);
      }
    }
    return false;
  }


  /**
   * Filter out all services that we don't have permission to get.
   * 
   * @param srs Set of ServiceRegistrationImpls to check.
   */
  void filterGetServicePermission(Set srs) {
    for (Iterator i = srs.iterator(); i.hasNext();) {
      ServiceRegistrationImpl sr = (ServiceRegistrationImpl)i.next();
      ;
      if (!okGetServicePerms(sr.getReference())) {
        i.remove();
      }
    }
  }


  //
  // BundleArchive secure operations
  //

  BundleResourceStream callGetBundleResourceStream(final BundleArchive archive,
                                                   final String name, final int ix) {
    return (BundleResourceStream)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return archive.getBundleResourceStream(name, ix);
      }
    });
  }


  Enumeration callFindResourcesPath(final BundleArchive archive, final String path) {
    return (Enumeration)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return archive.findResourcesPath(path);
      }
    });
  }


  //
  // BundleClassLoader secure operations
  //

  Object callSearchFor(final BundleClassLoader cl, final String name, final String pkg,
                       final String path, final BundleClassLoader.SearchAction action,
                       final boolean onlyFirst,
                       final BundleClassLoader requestor, final HashSet visited) {
    return AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return cl.searchFor(name, pkg, path, action, onlyFirst, requestor, visited);
      }
    });
  }


  String callFindLibrary0(final BundleClassLoader cl, final String name) {
    return (String)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return cl.findLibrary0(name);
      }
    });
  }


  //
  // BundleImpl secure operations
  //

  void callFinalizeActivation(final BundleImpl b) throws BundleException {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws BundleException {
          b.finalizeActivation();
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw (BundleException)e.getException();
    }
  }


  BundleThread createBundleThread(final FrameworkContext fc) {
    return (BundleThread)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return new BundleThread(fc);
      }
    });
  }


  void callUpdate0(final BundleImpl b, final InputStream in, final boolean wasActive)
      throws BundleException {
    try {
      final AccessControlContext acc = AccessController.getContext();
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws BundleException {
          b.update0(in, wasActive, acc);
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw (BundleException)e.getException();
    }
  }


  void callUninstall0(final BundleImpl b) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        b.uninstall0();
        return null;
      }
    });
  }


  void callSetAutostartSetting(final BundleImpl b, final int settings) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        b.setAutostartSetting0(settings);
        return null;
      }
    });
  }


  HeaderDictionary callGetHeaders0(final BundleGeneration bg, final String locale) {
    return (HeaderDictionary)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return bg.getHeaders0(locale);
      }
    });
  }


  Enumeration callFindEntries(final BundleGeneration bg, final String path,
                              final String filePattern, final boolean recurse) {
    return (Enumeration)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return bg.findEntries(path, filePattern, recurse);
      }
    });
  }


  BundleClassLoader newBundleClassLoader(final BundleGeneration bg) throws BundleException {
    try {
      return (BundleClassLoader)AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws Exception {
          return new BundleClassLoader(bg);
        }
      });
    } catch (PrivilegedActionException pe) {
      throw (BundleException)pe.getException();
    }
  }


  //
  // Bundles Secure operation
  //

  BundleImpl callInstall0(final Bundles bs, final String location, final InputStream in)
      throws BundleException {
    try {
      final AccessControlContext acc = AccessController.getContext();
      return (BundleImpl)AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws BundleException {
          return bs.install0(location, in, acc);
        }
      });
    } catch (PrivilegedActionException e) {
      throw (BundleException)e.getException();
    }
  }


  //
  // Listeners Secure operations
  //

  void callBundleChanged(final FrameworkContext fwCtx, final BundleEvent evt) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        fwCtx.listeners.bundleChanged(evt);
        return null;
      }
    });
  }


  void callServiceChanged(final FrameworkContext fwCtx, final Collection receivers,
                          final ServiceEvent evt, final Set matchBefore) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        fwCtx.listeners.serviceChanged(receivers, evt, matchBefore);
        return null;
      }
    });
  }


  //
  // PackageAdmin secure operations
  //

  void callRefreshPackages0(final PackageAdminImpl pa, final Bundle[] bundles) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        pa.refreshPackages0(bundles);
        return null;
      }
    });
  }


  //
  // ServiceReferenceImpl secure operations
  //

  Object callGetService(final ServiceFactory sf, final Bundle b, final ServiceRegistration sr) {
    return AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return sf.getService(b, sr);
      }
    });
  }


  //
  // ServiceRegisterationImpl secure operations
  //

  void callUnregister0(final ServiceRegistrationImpl sr) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        sr.unregister0();
        return null;
      }
    });
  }


  //
  // StartLevelController secure operations
  //

  void callSetStartLevel(final BundleImpl b, final int startlevel) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        b.setStartLevel(startlevel);
        return null;
      }
    });
  }


  //
  // SystemBundle secure operations
  //

  void callShutdown(final SystemBundle sb, final boolean restart) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
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
  ProtectionDomain getProtectionDomain(final BundleGeneration bg) {
    try {
      // We cannot use getBundleURL() here because that will
      // trigger a persmission check while we're still in
      // the phase of building permissions
      String h = Long.toString(bg.bundle.id);
      if (bg.generation != 0) {
        h += "." + Long.toString(bg.generation);
      }
      URLStreamHandler ush = bg.bundle.fwCtx.urlStreamHandlerFactory
          .createURLStreamHandler(BundleURLStreamHandler.PROTOCOL);
      URL bundleUrl = new URL(BundleURLStreamHandler.PROTOCOL, h, -1, "", ush);

      InputStream pis = bg.archive.getBundleResourceStream("OSGI-INF/permissions.perm", 0);
      PermissionCollection pc = ph.createPermissionCollection(bg.bundle.location, bg.bundle,
          pis);
      List cc = bg.archive.getCertificateChains(false);
      Certificate[] cca;
      if (cc != null) {
        ArrayList tmp = new ArrayList();
        for (Iterator i = cc.iterator(); i.hasNext();) {
          tmp.addAll((List)i.next());
        }
        cca = (Certificate[])tmp.toArray(new Certificate[tmp.size()]);
      } else {
        cca = null;
      }
      return new ProtectionDomain(new CodeSource(bundleUrl, cca), pc);
    } catch (MalformedURLException _ignore) {
    }
    return null;
  }


  URL getBundleURL(final FrameworkContext fwCtx, final String s) throws MalformedURLException {
    try {
      return (URL)AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws MalformedURLException {
          return new URL(null, s, fwCtx.urlStreamHandlerFactory
              .createURLStreamHandler(BundleURLStreamHandler.PROTOCOL));
        }
      });
    } catch (PrivilegedActionException e) {
      throw (MalformedURLException)e.getException();
    }
  }


  //
  // Privileged system calls
  //

  ClassLoader getClassLoaderOf(final Class c) {
    return (ClassLoader)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
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
  void purge(BundleImpl b, ProtectionDomain pd) {
    if (ph.purgePermissionCollection(new Long(b.id), pd.getPermissions())) {
      adminPerms.remove(b);
    }
  }


  //
  // Private
  //

  AdminPermission getAdminPermission(Bundle b, int ti) {
    AdminPermission[] res;
    res = (AdminPermission[])adminPerms.get(b);
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
