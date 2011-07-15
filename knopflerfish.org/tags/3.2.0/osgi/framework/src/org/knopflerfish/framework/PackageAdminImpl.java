/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

/**
 * Framework service which allows bundle programmers to inspect the packages
 * exported in the framework and eagerly update or uninstall bundles.
 * 
 * If present, there will only be a single instance of this service registered
 * in the framework.
 * 
 * <p>
 * The term <i>exported package</i> (and the corresponding interface
 * {@link ExportedPackage}) refers to a package that has actually been exported
 * (as opposed to one that is available for export).
 * 
 * <p>
 * Note that the information about exported packages returned by this service is
 * valid only until the next time {@link #refreshPackages} is called. If an
 * ExportedPackage becomes stale, (that is, the package it references has been
 * updated or removed as a result of calling PackageAdmin.refreshPackages()),
 * its getName() and getSpecificationVersion() continue to return their old
 * values, isRemovalPending() returns true, and getExportingBundle() and
 * getImportingBundles() return null.
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

  private FrameworkContext fwCtx;

  volatile private Vector refreshSync = new Vector(1);

  PackageAdminImpl(FrameworkContext fw) {
    fwCtx = fw;
  }

  /**
   * Gets the packages exported by the specified bundle.
   * 
   * @param bundle The bundle whose exported packages are to be returned, or
   *          <tt>null</tt> if all the packages currently exported in the
   *          framework are to be returned. If the specified bundle is the
   *          system bundle (that is, the bundle with id 0), this method returns
   *          all the packages on the system classpath whose name does not start
   *          with "java.". In an environment where the exhaustive list of
   *          packages on the system classpath is not known in advance, this
   *          method will return all currently known packages on the system
   *          classpath, that is, all packages on the system classpath that
   *          contains one or more classes that have been loaded.
   * 
   * @return The array of packages exported by the specified bundle, or
   *         <tt>null</tt> if the specified bundle has not exported any
   *         packages.
   */
  public ExportedPackage[] getExportedPackages(Bundle bundle) {
    ArrayList pkgs = new ArrayList();
    if (bundle != null) {
      for (Iterator i = ((BundleImpl)bundle).getExports(); i.hasNext();) {
        ExportPkg ep = (ExportPkg)i.next();
        if (ep.isExported()) {
          pkgs.add(new ExportedPackageImpl(ep));
        }
      }
    } else {
      for (Iterator bi = fwCtx.bundles.getBundles().iterator(); bi.hasNext();) {
        for (Iterator i = ((BundleImpl)bi.next()).getExports(); i.hasNext();) {
          ExportPkg ep = (ExportPkg)i.next();
          if (ep.isExported()) {
            pkgs.add(new ExportedPackageImpl(ep));
          }
        }
      }
    }
    int size = pkgs.size();
    if (size > 0) {
      return (ExportedPackage[])pkgs.toArray(new ExportedPackage[size]);
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
    Pkg pkg = fwCtx.packages.getPkg(name);
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
   * Gets the ExportedPackage with the specified package name. All exported
   * packages will be checked for the specified name. In an environment where
   * the exhaustive list of packages on the system classpath is not known in
   * advance, this method attempts to see if the named package is on the system
   * classpath. This means that this method may discover an ExportedPackage that
   * was not present in the list returned by <tt>getExportedPackages()</tt>.
   * 
   * @param name The name of the exported package to be returned.
   * 
   * @return The exported package with the specified name, or <tt>null</tt> if
   *         no expored package with that name exists.
   */
  public ExportedPackage getExportedPackage(String name) {
    Pkg p = (Pkg)fwCtx.packages.getPkg(name);
    if (p != null) {
      ExportPkg ep = p.getBestProvider();
      if (ep != null) {
        return new ExportedPackageImpl(ep);
      }
    }
    return null;
  }

  /**
   * Forces the update (replacement) or removal of packages exported by the
   * specified bundles.
   * 
   * @see org.osgi.service.packageadmin.PackageAdmin#refreshPackages
   */
  public void refreshPackages(final Bundle[] bundles) {
    fwCtx.perm.checkResolveAdminPerm();

    boolean restart = false;
    if (bundles != null) {
      for (int i = 0; i < bundles.length; i++) {
        if (bundles[i] == null) {
          throw new NullPointerException("bundle[" + i + "] cannot be null");
        }
        fwCtx.checkOurBundle(bundles[i]);
        if (((BundleImpl)bundles[i]).extensionNeedsRestart()) {
          restart = true;
          break;
        }
      }
    } else {
      for (Iterator iter = fwCtx.bundles.getBundles().iterator(); iter.hasNext();) {
        if (((BundleImpl)iter.next()).extensionNeedsRestart()) {
          restart = true;
          break;
        }
      }
    }
    if (restart) {
      try {
        // will restart the framework.
        fwCtx.systemBundle.update();
      } catch (BundleException ignored) {
        /* this can't be happening. */
      }
      return;
    }

    final PackageAdminImpl thisClass = this;
    synchronized (refreshSync) {
      Thread t = new Thread(fwCtx.threadGroup, "RefreshPackages") {
        public void run() {
          fwCtx.perm.callRefreshPackages0(thisClass, bundles);
        }
      };
      t.setDaemon(false);
      refreshSync.add(t);
      t.start();
      // Wait for a refresh thread to start
      try {
        refreshSync.wait(500);
      } catch (InterruptedException ignore) {
      }
    }
  }

  /**
   *
   */
  void refreshPackages0(final Bundle[] bundles) {
    if (fwCtx.debug.packages) {
      fwCtx.debug.println("PackageAdminImpl.refreshPackages() starting");
    }

    ArrayList startList = new ArrayList();

    synchronized (fwCtx.packages) {
      TreeSet zombies = fwCtx.packages.getZombieAffected(bundles);
      BundleImpl bi[] = (BundleImpl[])zombies.toArray(new BundleImpl[zombies.size()]);

      synchronized (refreshSync) {
        refreshSync.notifyAll();
      }
      // Stop affected bundles and remove their classloaders
      // in reverse start order
      for (int bx = bi.length; bx-- > 0;) {
        if (bi[bx].state == Bundle.ACTIVE || bi[bx].state == Bundle.STARTING) {
          startList.add(0, bi[bx]);
          try {
            bi[bx].waitOnOperation(fwCtx.packages, "PackageAdmin.refreshPackages", false);
            Exception be = bi[bx].stop0();
            if (be != null) {
              fwCtx.listeners.frameworkError(bi[bx], be);
            }
          } catch (BundleException ignore) {
            // Wait failed, we will try again
          }
        }
      }

      // Update the affected bundle states in normal start order
      int startPos = startList.size() - 1;
      BundleImpl nextStart = startPos >= 0 ? (BundleImpl)startList.get(startPos) : null;
      for (int bx = bi.length; bx-- > 0;) {
        Exception be = null;
        switch (bi[bx].state) {
        case Bundle.STARTING:
        case Bundle.ACTIVE:
          // Bundle must stop before we can continue
          // We could hang forever here.
          while (true) {
            try {
              bi[bx].waitOnOperation(fwCtx.packages, "PackageAdmin.refreshPackages", true);
              break;
            } catch (BundleException we) {
              if (fwCtx.debug.packages) {
                fwCtx.debug
                    .println("PackageAdminImpl.refreshPackages() timeout on bundle stop, retry...");
              }
              fwCtx.listeners.frameworkWarning(bi[bx], we);
            }
          }
          be = bi[bx].stop0();
          if (be != null) {
            fwCtx.listeners.frameworkError(bi[bx], be);
          }
          if (nextStart != bi[bx]) {
            startList.add(startPos + 1, bi[bx]);
          }
          // Fall through...
        case Bundle.STOPPING:
        case Bundle.RESOLVED:
          bi[bx].setStateInstalled(true);
          if (bi[bx] == nextStart) {
            nextStart = --startPos >= 0 ? (BundleImpl)startList.get(startPos) : null;
          }
          // Fall through...
        case Bundle.INSTALLED:
        case Bundle.UNINSTALLED:
          break;
        }
        bi[bx].purge();
      }
    }
    if (fwCtx.debug.packages) {
      fwCtx.debug.println("PackageAdminImpl.refreshPackages() "
                          + "all affected bundles now in state INSTALLED");
    }

    // Restart previously active bundles in normal start order
    fwCtx.bundles.startBundles(startList);
    fwCtx.listeners
        .frameworkEvent(new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED,
                                         fwCtx.systemBundle, null));
    refreshSync.remove(Thread.currentThread());
    if (fwCtx.debug.packages) {
      fwCtx.debug.println("PackageAdminImpl.refreshPackages() done.");
    }
  }

  /**
   *
   */
  public boolean resolveBundles(Bundle[] bundles) {
    fwCtx.perm.checkResolveAdminPerm();
    synchronized (fwCtx.packages) {
      List bl;
      if (bundles != null) {
        bl = new ArrayList();
        for (int bx = 0; bx < bundles.length; bx++) {
          bl.add(bundles[bx]);
        }
      } else {
        bl = fwCtx.bundles.getBundles();
      }
      boolean res = true;

      for (Iterator i = bl.iterator(); i.hasNext();) {
        BundleImpl b = (BundleImpl)i.next();
        if (b.getUpdatedState() == Bundle.INSTALLED) {
          res = false;
        }
      }
      return res;
    }
  }

  public RequiredBundle[] getRequiredBundles(String symbolicName) {
    List bs;
    ArrayList res = new ArrayList();
    if (symbolicName != null) {
      bs = fwCtx.bundles.getBundles(symbolicName);
    } else {
      bs = fwCtx.bundles.getBundles();
    }
    for (Iterator i = bs.iterator(); i.hasNext();) {
      BundleImpl b = (BundleImpl)i.next();
      if (((b.state & BundleImpl.RESOLVED_FLAGS) != 0
           || b.getRequiredBy().size() > 0)// Required, updated but not
                                           // re-resolved
          && !b.gen.isFragment()) {
        res.add(new RequiredBundleImpl(b.gen.bpkgs));
      }
    }
    int s = res.size();
    if (s > 0) {
      return (RequiredBundle[])res.toArray(new RequiredBundle[s]);
    } else {
      return null;
    }
  }

  public Bundle[] getBundles(String symbolicName, String versionRange) {
    VersionRange vr = versionRange != null ? new VersionRange(versionRange.trim()) :
        VersionRange.defaultVersionRange;
    List bs = fwCtx.bundles.getBundles(symbolicName, vr);
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

    BundleGeneration bg = ((BundleImpl)bundle).gen;

    if (bg.isFragment()) {
      return null;
    }

    if (bg.isFragmentHost()) {
      Vector fix = (Vector)bg.fragments.clone();
      Bundle[] r = new Bundle[fix.size()];
      for (int i = fix.size() - 1; i >= 0; i--) {
        r[i] = ((BundleGeneration)fix.get(i)).bundle;
      }
      return r;
    } else {
      return null;
    }
  }

  public Bundle[] getHosts(Bundle bundle) {
    BundleImpl b = (BundleImpl)bundle;
    if (b != null) {
      Vector h = b.getHosts(false);
      if (h != null) {
        Bundle[] r = new Bundle[h.size()];
        int pos = 0;
        for (Iterator i = h.iterator(); i.hasNext();) {
          r[pos++] = ((BundleGeneration)i.next()).bundle;
        }
        return r;
      }
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

    if (b.gen.isFragment() && !b.gen.isExtension()) {
      return BUNDLE_TYPE_FRAGMENT;
    }

    return 0;
  }

}
