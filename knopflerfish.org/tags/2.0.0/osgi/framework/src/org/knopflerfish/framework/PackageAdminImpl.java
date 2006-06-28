/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

import java.security.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.framework.AdminPermission;
import org.osgi.service.packageadmin.*;

/**
 * Framework service which allows bundle programmers to inspect the packages
 * exported in the framework and eagerly update or uninstall bundles.
 *
 * If present, there will only be a single instance of this service
 * registered in the framework.
 * 
 * <p> The term <i>exported package</i> (and the corresponding interface
 * {@link ExportedPackage}) refers to a package that has actually been
 * exported (as opposed to one that is available for export).
 *
 * <p> Note that the information about exported packages returned by this
 * service is valid only until the next time {@link #refreshPackages} is
 * called.
 * If an ExportedPackage becomes stale, (that is, the package it references
 * has been updated or removed as a result of calling 
 * PackageAdmin.refreshPackages()),
 * its getName() and getSpecificationVersion() continue to return their
 * old values, isRemovalPending() returns true, and getExportingBundle()
 * and getImportingBundles() return null.
 *
 * @see org.osgi.service.packageadmin.PackageAdmin
 * @author Jan Stein
 * @author Erik Wistrand
 * @author Robert Shelley
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */
public class PackageAdminImpl implements PackageAdmin {

  final static String SPEC_VERSION = "1.2";

  private Framework framework;

  
  PackageAdminImpl(Framework fw) {
    framework = fw;
  }


  /**
   * Gets the packages exported by the specified bundle.
   *
   * @param bundle The bundle whose exported packages are to be returned,
   *               or <tt>null</tt> if all the packages currently
   *               exported in the framework are to be returned.  If the
   *               specified bundle is the system bundle (that is, the
   *               bundle with id 0), this method returns all the packages
   *               on the system classpath whose name does not start with
   *               "java.".  In an environment where the exhaustive list
   *               of packages on the system classpath is not known in
   *               advance, this method will return all currently known
   *               packages on the system classpath, that is, all packages
   *               on the system classpath that contains one or more classes
   *               that have been loaded.
   *
   * @return The array of packages exported by the specified bundle,
   * or <tt>null</tt> if the specified bundle has not exported any packages.
   */
  public ExportedPackage[] getExportedPackages(Bundle bundle) {
    ArrayList pkgs = new ArrayList();
    if (bundle != null) {
      for (Iterator i = ((BundleImpl)bundle).getExports(); i.hasNext(); ) {
        pkgs.add(new ExportedPackageImpl((ExportPkg)i.next()));
      }
    } else {
      for (Iterator bi = framework.bundles.getBundles().iterator(); bi.hasNext(); ) {
        for (Iterator i = ((BundleImpl)bi.next()).getExports(); i.hasNext(); ) {
          pkgs.add(new ExportedPackageImpl((ExportPkg)i.next()));
        }
      }
    }
    int size = pkgs.size();
    if (size > 0) {
      return (ExportedPackage [])pkgs.toArray(new ExportedPackage[size]);
    } else {
      return null;
    }
  }


  /**
   * Gets the exported packages for the specified package name.
   * 
   * @param name The name of the exported packages to be returned.
   * 
   * @return An array of the exported packages, or <code>null</code> if no
   *         exported packages with the specified name exists.
   */
  public ExportedPackage[] getExportedPackages(String name) {
    Pkg pkg = framework.packages.getPkg(name);
    ExportedPackage[] res = null;
    if (pkg != null) {
      synchronized (pkg) {
        int size = pkg.exporters.size();
        if (size > 0) {
          res = new ExportedPackage[size];
          Iterator i = pkg.exporters.iterator();
          for (int pos = 0; pos < size;) {
            res[pos++] = new ExportedPackageImpl((ExportPkg)i.next());
          }
        }
      }
    }
    return res;
  }

  
  /**
   * Gets the ExportedPackage with the specified package name.  All exported 
   * packages 
   * will be checked for the specified name.  In an environment where the
   * exhaustive list of packages on the system classpath is not known in
   * advance, this method attempts to see if the named package is on the
   * system classpath.
   * This
   * means that this method may discover an ExportedPackage that was
   * not present in the list returned by <tt>getExportedPackages()</tt>.
   *
   * @param name The name of the exported package to be returned.
   *
   * @return The exported package with the specified name, or <tt>null</tt>
   *         if no expored package with that name exists.
   */
  public ExportedPackage getExportedPackage(String name) {
    Pkg p = (Pkg)framework.packages.getPkg(name);
    if (p != null) {
      ExportPkg ep = p.getBestProvider();
      if (ep != null) {
        return new ExportedPackageImpl(ep);
      }
    }
    return null;
  }
  

  /**
   * Forces the update (replacement) or removal of packages exported by
   * the specified bundles.
   *
   * @see org.osgi.service.packageadmin.PackageAdmin#refreshPackages
   */
  public void refreshPackages(final Bundle[] bundles) {
    framework.perm.checkResolveAdminPerm();
    
    boolean restart = false;
    if (bundles != null) {
      for (int i = 0; i < bundles.length; i++) {
        if (((BundleImpl)bundles[i]).extensionNeedsRestart()) {
          restart = true;
          break;
        }
      }
    } else {
      for (Iterator iter = framework.bundles.getBundles().iterator();
           iter.hasNext(); ) {
        if (((BundleImpl)iter.next()).extensionNeedsRestart()) {
          restart = true;
          break;
        }
      }
    }
    if (restart) {
      try {
        // will restart the framework.
        framework.systemBundle.stop(Framework.EXIT_CODE_RESTART);
      } catch (BundleException ignored) {
        /* this can't be happening. */
      }
      return ;
    }

    final PackageAdminImpl thisClass = this;
    Thread t = new Thread() {
        public void run() {
           framework.perm.callRefreshPackages0(thisClass, bundles);
        }
      };
    t.setDaemon(false);
    t.start();
  }


  void refreshPackages0(final Bundle[] bundles) {
    BundleImpl bi[] = (BundleImpl[])framework.packages
      .getZombieAffected(bundles).toArray(new BundleImpl[0]);
    ArrayList startList = new ArrayList();

    // Stop affected bundles and remove their classloaders
    // in reverse start order
    for (int bx = bi.length; bx-- > 0; ) {
      BundleException be = null;
      synchronized (bi[bx]) {
        if (bi[bx].state == Bundle.ACTIVE) {
          startList.add(0, bi[bx]);
          be = bi[bx].stop0(false);
        }
      }
      if (be != null) {
        framework.listeners.frameworkError(bi[bx], be);
      }
    }

    synchronized (framework.packages) {
      // Do this again in case something changed during the stop
      // phase, this time synchronized with packages to prevent
      // resolving of bundles.
      bi = (BundleImpl[])framework.packages
        .getZombieAffected(bundles).toArray(new BundleImpl[0]);

      // Update the affected bundle states in normal start order
      int startPos = startList.size() - 1;
      BundleImpl nextStart =  startPos >= 0 ? (BundleImpl)startList.get(startPos) : null;
      for (int bx = bi.length; bx-- > 0; ) {
        BundleException be = null;
        synchronized (bi[bx]) {
          switch (bi[bx].state) {
          case Bundle.STARTING:
          case Bundle.ACTIVE:
            synchronized (bi[bx]) {
              if (bi[bx].state == Bundle.ACTIVE) {
                be = bi[bx].stop0(false);
                if (nextStart != bi[bx]) {
                  startList.add(startPos + 1, bi[bx]);
                }
              }
            }
          case Bundle.STOPPING:
          case Bundle.RESOLVED:
            bi[bx].setStateInstalled(true);
            if (bi[bx] == nextStart) {
              nextStart = --startPos >= 0 ? (BundleImpl)startList.get(startPos) : null;
            }
          case Bundle.INSTALLED:
          case Bundle.UNINSTALLED:
            break;
          }
          bi[bx].purge();
        }
        if (be != null) {
          framework.listeners.frameworkError(bi[bx], be);
        }
      }
    }

    // Restart previously active bundles in normal start order
    framework.bundles.startBundles(startList);
    framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, this));
  }

  public boolean resolveBundles(Bundle[] bundles) {
    framework.perm.checkResolveAdminPerm();
    List bl;
    if (bundles != null) {
      bl =  new ArrayList();
      for (int bx = 0 ; bx < bundles.length; bx++ ) {
        bl.add(bundles[bx]);
      }
    } else {
      bl = framework.bundles.getBundles();
    }
    boolean res = true;
    for (Iterator i = bl.iterator(); i.hasNext(); ) {
      BundleImpl b = (BundleImpl)i.next();
      if (b.getUpdatedState() == Bundle.INSTALLED) {
        res = false;
      }
    }
    return res;
  }


  public RequiredBundle[] getRequiredBundles(String symbolicName) {
    List bs;
    ArrayList res = new ArrayList();
    if (symbolicName != null) {
      bs = framework.bundles.getBundles(symbolicName);
    } else {
      bs = framework.bundles.getBundles();
    }
    for (Iterator i = bs.iterator(); i.hasNext(); ) {
      BundleImpl b = (BundleImpl)i.next();
      if ((b.state & BundleImpl.RESOLVED_FLAGS) != 0 && !b.isFragment()) {
        res.add(new RequiredBundleImpl(b.bpkgs));
      }
    }
    int s = res.size();
    if (s > 0) {
      return (RequiredBundle [])res.toArray(new RequiredBundle[s]);
    } else {
      return null;
    }
  }


  public Bundle[] getBundles(String symbolicName, String versionRange) {
    VersionRange vr = versionRange != null ? new VersionRange(versionRange.trim()) :
      VersionRange.defaultVersionRange;
    List bs = framework.bundles.getBundles(symbolicName, vr);
    int size = bs.size();
    if (size > 0) {
      Bundle[] res = new Bundle[size];
      Iterator i = bs.iterator();
      for (int pos = 0; pos < size;) {
        res[pos++] = (Bundle)i.next();
      }
      return res;
    } else {
      return null;
    }
  }


  public Bundle[] getFragments(Bundle bundle) {
    if (bundle == null) {
      return null;
    }

    BundleImpl b = (BundleImpl)bundle;

    if (b.isFragment()) {
      return null;
    }

    if (b.isFragmentHost()) {
      return (Bundle[])b.fragments.toArray(new Bundle[0]);
    } else {
      return null;
    }
  }


  public Bundle[] getHosts(Bundle bundle) {
    if (bundle == null) {
      return null;
    }

    BundleImpl b = (BundleImpl)bundle;
    if (b.isFragment() && 
        b.isAttached()) {
      return new Bundle[]{b.getFragmentHost()};
    }

    return null;
  }


  public Bundle getBundle(Class clazz) {
    ClassLoader cl = clazz.getClassLoader();
    if (cl instanceof BundleClassLoader) {
      return ((BundleClassLoader)cl).getBundle();
    } else {
      return null;
    }
  }


  public int getBundleType(Bundle bundle) {
    BundleImpl b = (BundleImpl)bundle;

    if (b.isFragment() && !b.isExtension()) {
      return BUNDLE_TYPE_FRAGMENT;
    }

    return 0;
  }

}
