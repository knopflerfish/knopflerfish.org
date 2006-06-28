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
import java.net.URL;
import java.net.MalformedURLException;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.permissionadmin.PermissionAdmin;

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
  private static final int AP_MAX = 7;

  private static String [] AP_TO_STRING = new String [] {
    AdminPermission.CLASS,
    AdminPermission.EXECUTE,
    AdminPermission.EXTENSIONLIFECYCLE,
    AdminPermission.LIFECYCLE,
    AdminPermission.LISTENER,
    AdminPermission.METADATA,
    AdminPermission.RESOURCE,
  };

  private final Framework framework;
  private final PermissionsHandle ph;

  private AdminPermission ap_resolve_perm = null;
  private AdminPermission ap_startlevel_perm = null;


  Hashtable /* Bundle -> AdminPermission [] */ adminPerms = new Hashtable();


  SecurePermissionOps(Framework fw) {
    framework = fw;
    ph = new PermissionsHandle(fw);
  }


  void registerService() {
    String[] classes = new String [] { PermissionAdmin.class.getName() };
    framework.services.register(framework.systemBundle, classes,
                                ph.getPermissionAdminService(), null);
  }


  boolean checkPermissions() {
    return true;
  }

  //
  // Permission checks
  //

  boolean okClassAdminPerm(Bundle b) {
    try {
      AccessController.checkPermission(getAdminPermission(b, AP_CLASS));
      return true;
    } catch (AccessControlException _ignore) {
      return false;
    }
  }

  void checkExecuteAdminPerm(Bundle b){
    AccessController.checkPermission(getAdminPermission(b, AP_EXECUTE));
  }

  void checkExtensionLifecycleAdminPerm(Bundle b) {
    AccessController.checkPermission(getAdminPermission(b, AP_EXTENSIONLIFECYCLE));
  }

  void checkExtensionLifecycleAdminPerm(Bundle b, Object checkContext) {
    ((AccessControlContext)checkContext).
      checkPermission(getAdminPermission(b, AP_EXTENSIONLIFECYCLE));
  }

  void checkLifecycleAdminPerm(Bundle b) {
    AccessController.checkPermission(getAdminPermission(b, AP_LIFECYCLE));
  }

  void checkLifecycleAdminPerm(Bundle b, Object checkContext) {
    ((AccessControlContext)checkContext).
      checkPermission(getAdminPermission(b, AP_LIFECYCLE));
  }

  void checkListenerAdminPerm(Bundle b) {
    AccessController.checkPermission(getAdminPermission(b, AP_LISTENER));
  }

  void checkMetadataAdminPerm(Bundle b) {
    AccessController.checkPermission(getAdminPermission(b, AP_METADATA));
  }

  void checkResolveAdminPerm() {
    if (ap_resolve_perm == null) {
      ap_resolve_perm = new AdminPermission(framework.systemBundle, AdminPermission.RESOLVE);
    }
    AccessController.checkPermission(ap_resolve_perm);
  }
  
  void checkResourceAdminPerm(Bundle b) {
    AccessController.checkPermission(getAdminPermission(b, AP_RESOURCE));
  }

  boolean okResourceAdminPerm(Bundle b) {
    try {
      checkResourceAdminPerm(b);
      return true;
    } catch (AccessControlException _ignore) {
      return false;
    }
  }
  
  void checkStartLevelAdminPerm(){
    if (ap_startlevel_perm == null) {
      ap_startlevel_perm = new AdminPermission(framework.systemBundle,
                                               AdminPermission.STARTLEVEL);
    }
    AccessController.checkPermission(ap_startlevel_perm);
  }

  //
  // Bundle permission checks
  //

  boolean okFragmentBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.symbolicName, BundlePermission.FRAGMENT));
  }

  boolean okHostBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.symbolicName, BundlePermission.HOST));
  }

  boolean okProvideBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.symbolicName, BundlePermission.PROVIDE));
  }

  boolean okRequireBundlePerm(BundleImpl b) {
    PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
    return pc.implies(new BundlePermission(b.symbolicName, BundlePermission.REQUIRE));
  }

  //
  // Package permission checks
  //  

  boolean hasImportPackagePermission(BundleImpl b, String pkg) {
    if (b.id != 0) {
      PermissionCollection pc = ph.getPermissionCollection(new Long(b.id));
      return pc.implies(new PackagePermission(pkg, PackagePermission.IMPORT));
    }
    return true;
  }

  /**
   * Check that we have right export and import package permission for the bundle.
   *
   * @return Returns null if we have correct permission for listed package.
   *         Otherwise a string of failed entries.
   */
  String missingMandatoryPackagePermissions(BundlePackages bpkgs, List okImports) {
    if (bpkgs.bundle.id == 0) {
      return null;
    }
    PermissionCollection pc = ph.getPermissionCollection(new Long(bpkgs.bundle.id));
    String e_res = null;
    for (Iterator i = bpkgs.getExports(); i.hasNext();) {
      ExportPkg p = (ExportPkg)i.next();
      if (!pc.implies(new PackagePermission(p.name, PackagePermission.EXPORT))) {
        if (e_res != null) {
          e_res = e_res + ", " + p.name;
        } else {
          e_res = "missing export permission for package(s): " + p.name;
          e_res = p.name;
        }
      }
    }
    String i_res = null;
    for (Iterator i = bpkgs.getImports(); i.hasNext();) {
      ImportPkg p = (ImportPkg)i.next();
      if (!pc.implies(new PackagePermission(p.name, PackagePermission.IMPORT))) {
        if (p.resolution == Constants.RESOLUTION_OPTIONAL) {
          // Ok, that we do not have permission to optional packages
          continue;
        }
        if (i_res != null) {
          i_res = i_res + ", " + p.name;
        } else {
          i_res = "missing import permission for package(s): " + p.name;
        }
      } else {
        okImports.add(p);
      }
    }
    if (e_res != null) {
      if (i_res != null) {
        return e_res + "; " + i_res;
      } else {
        return e_res;
      }
    } else {
      return i_res;
    }
  }

  //
  // Service permission checks
  //  

  void checkRegisterServicePerm(String clazz) {
    AccessController.checkPermission(new ServicePermission(clazz, ServicePermission.REGISTER));
  }

  boolean okGetServicePerm(String clazz) {
    String c = (clazz != null) ? clazz : "*";
    try {
      AccessController.checkPermission(new ServicePermission(c, ServicePermission.GET));
      return true;
    } catch (AccessControlException _ignore) {
      return false;
    }
  }

  void checkGetServicePerms(String [] classes) {
    if (!okGetServicePerms(classes)) {
      throw new SecurityException("Missing permission to get the service.");
    }
  }

  boolean okGetServicePerms(String [] classes) {
    AccessControlContext acc = AccessController.getContext();
    return okGetServicePerms(acc, classes);
  }


  private boolean okGetServicePerms(AccessControlContext acc, String [] classes) {
    for (int i = 0; i < classes.length; i++) {
      try { 
        acc.checkPermission(new ServicePermission(classes[i], ServicePermission.GET));
        return true;
      } catch (AccessControlException ignore) { }
    }
    return false;
  }

  /**
   * Filter out all services that we don't have permission to get.
   *
   * @param srs Set of ServiceRegistrationImpls to check.
   */
  void filterGetServicePermission(Set srs) {
    AccessControlContext acc = AccessController.getContext();
    for (Iterator i = srs.iterator(); i.hasNext();) {
      ServiceRegistrationImpl sr = (ServiceRegistrationImpl)i.next();;
      String[] classes = (String[])sr.properties.get(Constants.OBJECTCLASS);
      if (!okGetServicePerms(acc, classes)) {
        i.remove();
      }
    }
  }

  //
  // BundleArchive secure operations
  //

  InputStream callGetInputStream(final BundleArchive archive,
                                 final String name,
                                 final int ix) {
    return (InputStream)AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return archive.getInputStream(name, ix);
        }
      });
  }


  Enumeration callFindResourcesPath(final BundleArchive archive,
                                   final String path) {
    return (Enumeration)AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return archive.findResourcesPath(path);
        }
      });
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
    return AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return cl.searchFor(name, pkg, path, action, onlyFirst, requestor, visited);
        }
      });
  }


  //
  // BundleImpl secure operations
  //

  void callStart0(final BundleImpl b) throws BundleException {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run() throws BundleException {
            b.start0();
            return null;
          }
        });
    } catch (PrivilegedActionException e) {
      throw (BundleException) e.getException();
    }
  }


  BundleException callStop0(final BundleImpl b, final boolean resetPersistent)  {
    return (BundleException)
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return b.stop0(resetPersistent);
          }
        });
  }


  void callUpdate0(final BundleImpl b, final InputStream in, final boolean wasActive)
    throws BundleException {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run() throws BundleException {
            b.update0(in, wasActive);
            return null;
          }
        });
    } catch (PrivilegedActionException e) {
      throw (BundleException) e.getException();
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


  void callStartOnLaunch(final BundleImpl b, final boolean flag) {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          b.startOnLaunch(flag);
          return null;
        }
      });
  }


  void callSetPersistent(final BundleImpl b, final boolean flag) {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          b.setPersistent(flag);
          return null;
        }
      });
  }


  ClassLoader callGetClassLoader0(final BundleImpl b) {
    return (ClassLoader)
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return b.getClassLoader0();
          }
        });
  }


  HeaderDictionary callGetHeaders0(final BundleImpl b, final String locale) {
    return (HeaderDictionary)
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return b.getHeaders0(locale);
          }
        });
  }


  Enumeration callFindEntries0(final BundleImpl b, final String path,
                               final String filePattern, final boolean recurse) {
    return (Enumeration)
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return b.findEntries0(path, filePattern, recurse);
          }
        });
  }

  //
  // Bundles Secure operation
  //

  BundleImpl callInstall0(final Bundles bs, final String location, final InputStream in)
    throws BundleException {
    try {
      final AccessControlContext acc = AccessController.getContext();
      return (BundleImpl)
        AccessController.doPrivileged(new PrivilegedExceptionAction() {
            public Object run() throws BundleException {
              return bs.install0(location, in, acc);
            }
          });
    } catch (PrivilegedActionException e) {
      throw (BundleException) e.getException();
    }
  }

  //
  // Listeners Secure operations
  //

  void callBundleChanged(final BundleListener bl, final BundleEvent evt) {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          bl.bundleChanged(evt);
          return null;
        }
      });
  }


  void callFrameworkEvent(final FrameworkListener fl, final FrameworkEvent evt) {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          fl.frameworkEvent(evt);
          return null;
        }
      });
  }


  void callServiceChanged(final ServiceListener sl, final ServiceEvent evt) {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          sl.serviceChanged(evt);
          return null;
        }
      });
  }

  //
  // Main Secure operations
  //

  void callMainRestart() {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          Main.restart();
          return null;
        }
      });
  }

  void callMainShutdown(final int exitcode) {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          Main.shutdown(exitcode);
          return null;
        }
      });
  }

  //
  // PackageAdmin secure operations
  //

  void callRefreshPackages0(final PackageAdminImpl pa, final Bundle [] bundles) {
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

  Object callGetService(final ServiceFactory sf,
                        final Bundle b,
                        final ServiceRegistration sr) {
    return
      AccessController.doPrivileged(new PrivilegedAction() {
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
  // Permissions package functionality
  //

  /**
   * Get protection domain for bundle
   */
  ProtectionDomain getProtectionDomain(BundleImpl b) {
    try {
      String h = Long.toString(b.id) + "." + Long.toString(b.generation);
      URL bundleUrl = new URL(BundleURLStreamHandler.PROTOCOL, h, "");
      InputStream pis = b.archive.getInputStream("OSGI-INF/permissions.perm", 0);
      PermissionCollection pc = ph.createPermissionCollection(b.location, b, pis);
      return new ProtectionDomain(new CodeSource(bundleUrl, (Certificate[])null), pc);
    } catch (MalformedURLException _ignore) { }
    return null;
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
    AdminPermission [] res;
    res = (AdminPermission [])adminPerms.get(b);
    if (res != null) {
      if (res[ti] != null) {
        return res[ti];
      }
    } else {
      res = new AdminPermission [AP_MAX];
      adminPerms.put(b, res);
    }
    res[ti] = new AdminPermission(b, AP_TO_STRING[ti]);
    return res[ti];
  } 
   
}
