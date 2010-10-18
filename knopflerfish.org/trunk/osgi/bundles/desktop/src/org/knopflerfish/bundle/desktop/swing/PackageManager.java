/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project All rights reserved.
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
package org.knopflerfish.bundle.desktop.swing;


import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import org.osgi.framework.*;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.knopflerfish.bundle.desktop.swing.Activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.util.tracker.ServiceTracker;
import org.knopflerfish.util.Text;

public class PackageManager  {
  final ServiceTracker pkgTracker;

  public PackageManager(ServiceTracker pkgTracker) {
    this.pkgTracker = pkgTracker;
    refresh();
  }

  final Map bundleExports = new HashMap();
  final Map bundleImports = new HashMap();
  final Map bundleReqs    = new HashMap();
  final Map manifestImports = new HashMap();
  final Map missingImports = new HashMap();


  // Bundle -> RequiredBundle
  final Map requiredBundleMap = new HashMap();

  // RequiredBundle -> Boolean
  final Map isRequiringMap = new HashMap();

  // int maxSize = 0;

  final Object lock = new Object();

  /*
  public int getMaxSize() {
    synchronized(lock) {
      return maxSize;
    }
  }
  */

  public PackageAdmin getPackageAdmin() {
    return (PackageAdmin) pkgTracker.getService();
  }

  static final Bundle[] EMPTY_BUNDLES = new Bundle[0];

  public Bundle[] getHosts(final Bundle b) {
    final PackageAdmin pkgAdmin = getPackageAdmin();
    final Bundle[] bl = null!=pkgAdmin ? pkgAdmin.getHosts(b) : null;
    return bl != null ? bl : EMPTY_BUNDLES;
  }

  public Bundle[] getFragments(final Bundle b) {
    final PackageAdmin pkgAdmin = getPackageAdmin();
    final Bundle[] bl = null!=pkgAdmin ? pkgAdmin.getFragments(b) : null;
    return bl != null ? bl : EMPTY_BUNDLES;
  }

  public Collection getImportedPackages(final Bundle b) {
    synchronized(lock) {
      final Collection s = (Collection) bundleImports.get(b);
      return s != null ? s : Collections.EMPTY_SET;
    }
  }

  public Collection getRequiredPackages(final Bundle b) {
    synchronized(lock) {
      final Collection s = (Collection) bundleReqs.get(b);
      return s != null ? s : Collections.EMPTY_SET;
    }
  }


  public Collection getManifestImports(final Bundle b) {
    synchronized(lock) {
      Collection c = (Collection) manifestImports.get(b);
      if(c == null) {
        c = getPackageNames(b, "Import-Package");
        manifestImports.put(b, c);
      }

      return c;
    }
  }

  public Collection getMissingImports(final Bundle b) {
    synchronized(lock) {
      Collection missing = (Collection) missingImports.get(b);
      if(missing == null) {
        missing = new TreeSet();
        missing.addAll(getManifestImports(b));

        Collection okSet  = getImportedPackages(b);

        // Remove wired imports
        for(Iterator it = okSet.iterator(); it.hasNext(); ) {
          final ExportedPackage pkg = (ExportedPackage) it.next();
          missing.remove(pkg.getName());
        }

        // Remove imports of packages exported by the bundle (self
        // imports from the current classloader will not have any
        // wires)
        okSet = getExportedPackages(b);
        for(Iterator it = okSet.iterator(); it.hasNext(); ) {
          final ExportedPackage pkg = (ExportedPackage) it.next();
          missing.remove(pkg.getName());
        }

        missingImports.put(b, missing);
      }

      return missing;
    }
  }

  /**
   * Get the specified header name from a bundle and parse the value
   * as package names (ignoring all parameters and directives).
   *
   * @return a collection of strings
   */
  protected Collection getPackageNames(final Bundle b,
                                       final String headerName)
  {
    final Set res = new TreeSet();
    final String v = (String) b.getHeaders().get(headerName);

    if(v != null && v.length() > 0) {
      // Uses the manifest entry parser from the KF-framework
      try {
        final Iterator it = org.knopflerfish.framework.Util
          .parseEntries(headerName, v, false, false, false);
        while (it.hasNext()) {
          final Map entry = (Map) it.next();
          final List pkgs = (List) entry.get("$keys");
          res.addAll(pkgs);
        }
      } catch (IllegalArgumentException iae) {
      }
    }
    return res;
  }


  public Collection getExportedPackages(final Bundle b) {
    synchronized(lock) {
      Collection r = (Collection) bundleExports.get(b);
      if(r == null) {
        r = new TreeSet(pkgComparator);
        final PackageAdmin pkgAdmin = getPackageAdmin();
        ExportedPackage[] pkgs = null!=pkgAdmin
          ? pkgAdmin.getExportedPackages(b) : null;
        for(int i = 0; pkgs != null && i < pkgs.length; i++) {
          if(accept(pkgs[i])) {
            r.add(pkgs[i]);
          }
        }
        bundleExports.put(b, r);
      }
      return r;
    }
  }

  public boolean isWired(final String pkgName, final Bundle b)
  {
    final Collection pkgs = getImportedPackages(b);
    for (Iterator it = pkgs.iterator(); it.hasNext(); ) {
      final ExportedPackage epkg = (ExportedPackage) it.next();
      if (pkgName.equals(epkg.getName())) {
        return true;
      }
    }
    return false;
  }


  PackageFilter packageFilter = null;
  // new NoCommonPackagesFilter();

  public void setPackageFilter(PackageFilter filter) {
    this.packageFilter = filter;
    refresh();
  }


  protected boolean accept(final ExportedPackage pkg) {
    if(packageFilter != null) {
      return packageFilter.accept(pkg);
    } else {
      return true;
    }
  }

  public static interface PackageFilter {
    boolean accept(ExportedPackage pkg);
  }

  public static class NoCommonPackagesFilter implements PackageFilter {
    public boolean accept(final ExportedPackage pkg) {
      String name = pkg.getName();
      if(name.startsWith("org.eclipse.swt.")) {
        return false;
      }
      if(name.startsWith("org.eclipse.ui.")) {
        return false;
      }
      if(name.startsWith("org.eclipse.")) {
        if(-1 != name.indexOf(".internal")) {
          return false;
        }
      }
      if(name.startsWith("javax.")) {
        return false;
      }
      if(name.startsWith("org.omg.")) {
        return false;
      }
      if(name.startsWith("org.w3c.")) {
        return false;
      }
      return true;
    }
  }


  void refresh() {
    synchronized(lock) {
      // maxSize = 0;
      long t0 = System.currentTimeMillis();

      manifestImports.clear();
      missingImports.clear();
      bundleExports.clear();
      bundleImports.clear();
      bundleReqs.clear();
      requiredBundleMap.clear();

      // makeBundleIndex();

      final PackageAdmin pkgAdmin = getPackageAdmin();
      if (null!=pkgAdmin) {
        final ExportedPackage[] pkgs
          = pkgAdmin.getExportedPackages((Bundle)null);
        final RequiredBundle[] rbl = pkgAdmin.getRequiredBundles(null);

        for(int i = 0; pkgs != null && i < pkgs.length; i++) {

          if(accept(pkgs[i])) {
            Bundle   fromB = pkgs[i].getExportingBundle();
            if (null==fromB) continue; // Ignore STALE epkgs

            Collection r = (Collection)bundleExports.get(fromB);
            if(r == null) {
              r = new TreeSet(pkgComparator);
              bundleExports.put(fromB, r);
            }
            if(accept(pkgs[i])) {
              r.add(pkgs[i]);
            }

            Bundle[] bl    = pkgs[i].getImportingBundles();
            for(int j = 0; bl != null && j < bl.length; j++) {
              if (isBundleRequiredBy(rbl, fromB, bl[j])) {
                Set reqs = (Set)bundleReqs.get(bl[j]);
                if(reqs == null) {
                  reqs = new TreeSet(pkgComparator);
                  bundleReqs.put(bl[j], reqs);
                }

                reqs.add(pkgs[i]);
              } else {
                Set imports = (Set)bundleImports.get(bl[j]);
                if(imports == null) {
                  imports = new TreeSet(pkgComparator);
                  bundleImports.put(bl[j], imports);
                }
                imports.add(pkgs[i]);
              }
            }
          }
        }
      }
      long t1 = System.currentTimeMillis();
    }
  }



  /**
   * Check if one given bundle is required by another specified bundle.
   *
   * @param rbl List of required bundles as returend by package admin.
   * @param requiredBundle The bundle to check if it is required.
   * @param requiringBundle The bundle to check that it requires.
   * @return <tt>true</tt> if requiredbundle is required by
   *         requiringBundle, <tt>false</tt> otherwsie.
   */
  public boolean isBundleRequiredBy(final RequiredBundle[] rbl,
                                    final Bundle requiredBundle,
                                    final Bundle requiringBundle)
  {
    final RequiredBundle rb = getRequiredBundle(rbl, requiredBundle);

    final Bundle[] requiringBundles
      = rb!=null ? rb.getRequiringBundles() : null;
    for (int j=0; requiringBundles!=null && j<requiringBundles.length;j++){
      if (requiringBundles[j].getBundleId()==requiringBundle.getBundleId()){
        return true;
      }
    }
    return false;
  }



  /**
   * Get the RequiredBundle object for this bundle.
   *
   * @param rbl List of required bundles as returend by package admin.
   * @param bundle The bundle to get requiring bundles for.
   * @return The RequiredBundle object for the given bundle or
   *         <tt>null</tt> if the bundle is not required.
   */
  public RequiredBundle getRequiredBundle(final RequiredBundle[] rbl,
                                          final Bundle b)  {
    final RequiredBundle rb = (RequiredBundle) requiredBundleMap.get(b);
    if(rb != null) {
      return rb;
    }
    for (int i=0; rbl!=null && i<rbl.length; i++) {
      final Bundle rbb = rbl[i].getBundle();
      if (rbb != null && rbb.getBundleId()==b.getBundleId()) {
        requiredBundleMap.put(b, rbl[i]);
        return rbl[i];
      }
    }
    requiredBundleMap.put(b, null);
    return null;
  }

  static public Comparator pkgComparator = new ExportedPackageComparator();

  static class ExportedPackageComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      ExportedPackage ep1 = (ExportedPackage) o1;
      ExportedPackage ep2 = (ExportedPackage) o2;

      // First package name
      int res = ep1.getName().compareTo(ep2.getName());
      if (0!=res) return res;

      // Then package version
      res = ep1.getVersion().compareTo(ep2.getVersion());
      if (0!=res) return res;

      // Then pending removal
      if (ep1.isRemovalPending() && !ep2.isRemovalPending()) return -1;
      if (!ep1.isRemovalPending() && ep2.isRemovalPending()) return 1;

      // Then number of importing bundles
      res = ep2.getImportingBundles().length - ep1.getImportingBundles().length;
      if (0!=res) return res;

      // Finally object identity (hashCode is the closest approximation)
      res = ep2.hashCode() - ep1.hashCode();
      return res;
    }
    public boolean equals(Object o)
    {
      return o instanceof ExportedPackageComparator;
    }

  }

}
