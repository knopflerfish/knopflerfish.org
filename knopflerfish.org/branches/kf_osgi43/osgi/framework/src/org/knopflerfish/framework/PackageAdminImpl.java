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

package org.knopflerfish.framework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

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

  private final FrameworkContext fwCtx;

  volatile private Vector<Thread> refreshSync = new Vector<Thread>(1);

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
    final ArrayList<ExportedPackageImpl> pkgs = new ArrayList<ExportedPackageImpl>();
    if (bundle != null) {
      for (final Iterator<ExportPkg> i = ((BundleImpl)bundle).getExports(); i.hasNext();) {
        final ExportPkg ep = i.next();
        if (ep.isExported()) {
          pkgs.add(new ExportedPackageImpl(ep));
        }
      }
    } else {
      for (final Object element : fwCtx.bundles.getBundles()) {
        for (final Iterator<ExportPkg> i = ((BundleImpl)element).getExports(); i.hasNext();) {
          final ExportPkg ep = i.next();
          if (ep.isExported()) {
            pkgs.add(new ExportedPackageImpl(ep));
          }
        }
      }
    }
    final int size = pkgs.size();
    if (size > 0) {
      return pkgs.toArray(new ExportedPackage[size]);
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
    final Pkg pkg = fwCtx.packages.getPkg(name);
    ExportedPackage[] res = null;
    if (pkg != null) {
      synchronized (pkg) {
        final int size = pkg.exporters.size();
        if (size > 0) {
          res = new ExportedPackage[size];
          final Iterator<ExportPkg> i = pkg.exporters.iterator();
          for (int pos = 0; pos < size;) {
            res[pos++] = new ExportedPackageImpl(i.next());
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
    final Pkg p = fwCtx.packages.getPkg(name);
    if (p != null) {
      final ExportPkg ep = p.getBestProvider();
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
    refreshPackages(bundles, null);
  }

  void refreshPackages(final Bundle[] bundles, final FrameworkListener[] fl) {
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
      for (final Object element : fwCtx.bundles.getBundles()) {
        if (((BundleImpl)element).extensionNeedsRestart()) {
          restart = true;
          break;
        }
      }
    }
    if (restart) {
      try {
        // will restart the framework.
        fwCtx.systemBundle.update();
      } catch (final BundleException ignored) {
        /* this can't be happening. */
      }
      return;
    }

    final PackageAdminImpl thisClass = this;
    synchronized (refreshSync) {
      final Thread t = new Thread(fwCtx.threadGroup, "RefreshPackages") {
        @Override
        public void run() {
          fwCtx.perm.callRefreshPackages0(thisClass, bundles, fl);
        }
      };
      t.setDaemon(false);
      refreshSync.add(t);
      t.start();
      // Wait for a refresh thread to start
      try {
        refreshSync.wait(500);
      } catch (final InterruptedException ignore) {
      }
    }
  }

  /**
   *
   */
  void refreshPackages0(final Bundle[] bundles, final FrameworkListener...fl) {
    if (fwCtx.debug.packages) {
      fwCtx.debug.println("PackageAdminImpl.refreshPackages() starting");
    }
    // TODO send framework error events to fl

    final ArrayList<BundleImpl> startList = new ArrayList<BundleImpl>();

    synchronized (fwCtx.packages) {
      final TreeSet<BundleImpl> zombies = fwCtx.packages.getZombieAffected(bundles);
      final BundleImpl bi[] = zombies.toArray(new BundleImpl[zombies.size()]);

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
            final Exception be = bi[bx].stop0();
            if (be != null) {
              fwCtx.listeners.frameworkError(bi[bx], be, fl);
            }
          } catch (final BundleException ignore) {
            // Wait failed, we will try again
          }
        }
      }

      // Update the affected bundle states in normal start order
      int startPos = startList.size() - 1;
      BundleImpl nextStart = startPos >= 0 ? startList.get(startPos) : null;
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
            } catch (final BundleException we) {
              if (fwCtx.debug.packages) {
                fwCtx.debug
                    .println("PackageAdminImpl.refreshPackages() timeout on bundle stop, retry...");
              }
              fwCtx.listeners.frameworkWarning(bi[bx], we, fl);
            }
          }
          be = bi[bx].stop0();
          if (be != null) {
            fwCtx.listeners.frameworkError(bi[bx], be, fl);
          }
          if (nextStart != bi[bx]) {
            startList.add(startPos + 1, bi[bx]);
          }
          // Fall through...
        case Bundle.STOPPING:
        case Bundle.RESOLVED:
          bi[bx].setStateInstalled(true);
          if (bi[bx] == nextStart) {
            nextStart = --startPos >= 0 ? startList.get(startPos) : null;
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
    startBundles(startList);
    final FrameworkEvent fe = new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED,
                                           fwCtx.systemBundle, null);
    fwCtx.listeners.frameworkEvent(fe, fl);
    refreshSync.remove(Thread.currentThread());
    if (fwCtx.debug.packages) {
      fwCtx.debug.println("PackageAdminImpl.refreshPackages() done.");
    }
  }


  /**
   * Start a list of bundles in order
   *
   * @param slist Bundles to start.
   */
  private void startBundles(List<BundleImpl> slist, FrameworkListener...fl) {
    // Sort in start order
    // Resolve first to avoid dead lock
    for (final BundleImpl rb : slist) {
      rb.getUpdatedState();
    }
    for (final BundleImpl rb : slist) {
      if (rb.getUpdatedState() == Bundle.RESOLVED) {
        try {
          rb.start();
        } catch (final BundleException be) {
          rb.fwCtx.listeners.frameworkError(rb, be, fl);
        }
      }
    }
  }



  /**
   *
   */
  public boolean resolveBundles(Bundle[] bundles) {
    fwCtx.perm.checkResolveAdminPerm();
    synchronized (fwCtx.packages) {
      List<BundleImpl> bl;
      if (bundles != null) {
        bl = new ArrayList<BundleImpl>();
        for (final Bundle bundle : bundles) {
          bl.add( (BundleImpl) bundle);
        }
      } else {
        bl = fwCtx.bundles.getBundles();
      }
      boolean res = true;

      for (final Bundle bundle : bl) {
        final BundleImpl b = (BundleImpl)bundle;
        if (b.getUpdatedState() == Bundle.INSTALLED) {
          res = false;
        }
      }
      return res;
    }
  }

  public RequiredBundle[] getRequiredBundles(String symbolicName) {
    List<BundleImpl> bs;
    final ArrayList<RequiredBundleImpl> res = new ArrayList<RequiredBundleImpl>();
    if (symbolicName != null) {
      bs = fwCtx.bundles.getBundles(symbolicName);
    } else {
      bs = fwCtx.bundles.getBundles();
    }
    for (final Object element : bs) {
      final BundleImpl b = (BundleImpl)element;
      if (((b.state & BundleImpl.RESOLVED_FLAGS) != 0
           || b.getRequiredBy().size() > 0)// Required, updated but not
                                           // re-resolved
          && !b.current().isFragment()) {
        res.add(new RequiredBundleImpl(b.current().bpkgs));
      }
    }
    final int s = res.size();
    if (s > 0) {
      return res.toArray(new RequiredBundle[s]);
    } else {
      return null;
    }
  }

  public Bundle[] getBundles(String symbolicName, String versionRange) {
    final VersionRange vr = versionRange != null ? new VersionRange(versionRange.trim()) :
        VersionRange.defaultVersionRange;
    final List<BundleImpl> bs = fwCtx.bundles.getBundles(symbolicName, vr);
    final int size = bs.size();
    if (size > 0) {
      final Bundle[] res = new Bundle[size];
      final Iterator<BundleImpl> i = bs.iterator();
      for (int pos = 0; pos < size;) {
        res[pos++] = i.next();
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

    final BundleGeneration bg = ((BundleImpl)bundle).current();

    if (bg.isFragment()) {
      return null;
    }

    if (bg.isFragmentHost()) {
      @SuppressWarnings("unchecked")
      final Vector<BundleGeneration> fix = (Vector<BundleGeneration>) bg.fragments.clone();
      final Bundle[] r = new Bundle[fix.size()];
      for (int i = fix.size() - 1; i >= 0; i--) {
        r[i] = fix.get(i).bundle;
      }
      return r;
    } else {
      return null;
    }
  }

  public Bundle[] getHosts(Bundle bundle) {
    final BundleImpl b = (BundleImpl) bundle;
    if (b != null) {
      final Vector<BundleGeneration> h = b.getHosts(false);
      if (h != null) {
        final Bundle[] r = new Bundle[h.size()];
        int pos = 0;
        for (final BundleGeneration bg : h) {
          r[pos++] = bg.bundle;
        }
        return r;
      }
    }
    return null;
  }

  public Bundle getBundle(@SuppressWarnings("rawtypes") Class  clazz) {
    final ClassLoader cl = clazz.getClassLoader();
    if (cl instanceof BundleClassLoader) {
      return ((BundleClassLoader)cl).getBundle();
    } else {
      return null;
    }
  }

  public int getBundleType(Bundle bundle) {
    final BundleImpl b = (BundleImpl)bundle;

    if (b.current().isFragment() && !b.current().isExtension()) {
      return BUNDLE_TYPE_FRAGMENT;
    }

    return 0;
  }

}
