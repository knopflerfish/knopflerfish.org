/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project All rights reserved.
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


import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.framework.Util.HeaderEntry;

public class PackageManager  {
  final ServiceTracker<PackageAdmin,PackageAdmin> pkgTracker;

  public PackageManager(ServiceTracker<PackageAdmin, PackageAdmin> pkgTracker)
  {
    this.pkgTracker = pkgTracker;
    refresh();
  }

  final Map<Bundle, Collection<ExportedPackage>> bundleExports
    = new HashMap<Bundle, Collection<ExportedPackage>>();
  final Map<Bundle, Set<ExportedPackage>> bundleImports
    = new HashMap<Bundle, Set<ExportedPackage>>();
  final Map<Bundle, Set<ExportedPackage>> bundleReqs
    = new HashMap<Bundle, Set<ExportedPackage>>();
  final Map<Bundle, Collection<String>> manifestImports
    = new HashMap<Bundle, Collection<String>>();
  final Map<Bundle, Collection<String>> missingImports
    = new HashMap<Bundle, Collection<String>>();

  final Map<Bundle, RequiredBundle> requiredBundleMap
    = new HashMap<Bundle, RequiredBundle>();
  final Map<RequiredBundle,Boolean> isRequiringMap
  = new HashMap<RequiredBundle,Boolean>();

  final Object lock = new Object();

  public PackageAdmin getPackageAdmin() {
    return pkgTracker.getService();
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

  public Collection<ExportedPackage> getImportedPackages(final Bundle b) {
    synchronized(lock) {
      final Set<ExportedPackage> s = bundleImports.get(b);
      if (s!=null) {
        return s;
      }
    }
    return Collections.emptySet();
  }

  public Collection<ExportedPackage> getRequiredPackages(final Bundle b) {
    synchronized(lock) {
      final Set<ExportedPackage> s = bundleReqs.get(b);
      if (s!=null) {
        return s;
      }
      return Collections.emptySet();
    }
  }


  public Collection<String> getManifestImports(final Bundle b) {
    synchronized(lock) {
      Collection<String> c = manifestImports.get(b);
      if(c == null) {
        c = getPackageNames(b, "Import-Package");
        manifestImports.put(b, c);
      }

      return c;
    }
  }

  public Collection<String> getMissingImports(final Bundle b) {
    synchronized(lock) {
      Collection<String> missing = missingImports.get(b);
      if(missing == null) {
        missing = new TreeSet<String>();
        missing.addAll(getManifestImports(b));

        Collection<ExportedPackage> okSet  = getImportedPackages(b);

        // Remove wired imports
        for (final ExportedPackage pkg : okSet) {
          missing.remove(pkg.getName());
        }

        // Remove imports of packages exported by the bundle (self
        // imports from the current classloader will not have any
        // wires)
        okSet = getExportedPackages(b);
        for (final ExportedPackage pkg : okSet) {
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
  protected Collection<String> getPackageNames(final Bundle b,
                                               final String headerName)
  {
    final Set<String> res = new TreeSet<String>();
    final String v = b.getHeaders().get(headerName);

    if(v != null && v.length() > 0) {
      // Uses the manifest entry parser from the KF-framework
      try {
        for (final HeaderEntry he : org.knopflerfish.framework.Util
            .parseManifestHeader(headerName, v, false, false, false)) {
          res.addAll(he.getKeys());
        }
      } catch (final IllegalArgumentException iae) {
      }
    }
    return res;
  }


  public Collection<ExportedPackage> getExportedPackages(final Bundle b) {
    synchronized(lock) {
      Collection<ExportedPackage> r = bundleExports.get(b);
      if(r == null) {
        r = new TreeSet<ExportedPackage>(pkgComparator);
        final PackageAdmin pkgAdmin = getPackageAdmin();
        final ExportedPackage[] pkgs = null!=pkgAdmin
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
    final Collection<ExportedPackage> pkgs = getImportedPackages(b);
    for (final ExportedPackage epkg : pkgs) {
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
      final String name = pkg.getName();
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
            final Bundle   fromB = pkgs[i].getExportingBundle();
            if (null==fromB)
             {
              continue; // Ignore STALE epkgs
            }

            Collection<ExportedPackage> r = bundleExports.get(fromB);
            if(r == null) {
              r = new TreeSet<ExportedPackage>(pkgComparator);
              bundleExports.put(fromB, r);
            }
            if(accept(pkgs[i])) {
              r.add(pkgs[i]);
            }

            final Bundle[] bl    = pkgs[i].getImportingBundles();
            for(int j = 0; bl != null && j < bl.length; j++) {
              if (isBundleRequiredBy(rbl, fromB, bl[j])) {
                Set<ExportedPackage> reqs = bundleReqs.get(bl[j]);
                if(reqs == null) {
                  reqs = new TreeSet<ExportedPackage>(pkgComparator);
                  bundleReqs.put(bl[j], reqs);
                }

                reqs.add(pkgs[i]);
              } else {
                Set<ExportedPackage> imports = bundleImports.get(bl[j]);
                if(imports == null) {
                  imports = new TreeSet<ExportedPackage>(pkgComparator);
                  bundleImports.put(bl[j], imports);
                }
                imports.add(pkgs[i]);
              }
            }
          }
        }
      }
    }
  }



  /**
   * Check if one given bundle is required by another specified bundle.
   *
   * @param rbl List of required bundles as returned by package admin.
   * @param requiredBundle The bundle to check if it is required.
   * @param requiringBundle The bundle to check that it requires.
   * @return <tt>true</tt> if <tt>requiredBundle</tt> is required by
   *         requiringBundle, <tt>false</tt> otherwise.
   */
  public boolean isBundleRequiredBy(final RequiredBundle[] rbl,
                                    final Bundle requiredBundle,
                                    final Bundle requiringBundle)
  {
    final RequiredBundle rb = getRequiredBundle(rbl, requiredBundle);

    try {
    final Bundle[] requiringBundles
      = rb!=null ? rb.getRequiringBundles() : null;
    for (int j=0; requiringBundles!=null && j<requiringBundles.length;j++){
      if (requiringBundles[j].getBundleId()==requiringBundle.getBundleId()){
        return true;
      }
    }
    } catch (final NullPointerException npe) {
      // Thrown by equinox 3.8 after an update; ignore it.
      // java.lang.NullPointerException
      //  at org.eclipse.osgi.internal.loader.BundleLoaderProxy.addRequirers(BundleLoaderProxy.java:142)
      //  at org.eclipse.osgi.internal.loader.BundleLoaderProxy.getRequiringBundles(BundleLoaderProxy.java:129)
      //  at org.knopflerfish.bundle.desktop.swing.PackageManager.isBundleRequiredBy(PackageManager.java:346)
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
    final RequiredBundle rb = requiredBundleMap.get(b);
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

  static public Comparator<ExportedPackage> pkgComparator
    = new ExportedPackageComparator();

  static class ExportedPackageComparator implements Comparator<ExportedPackage>
  {
    public int compare(ExportedPackage ep1, ExportedPackage ep2)
    {
      // First package name
      int res = ep1.getName().compareTo(ep2.getName());
      if (0!=res) {
        return res;
      }

      // Then package version
      res = ep1.getVersion().compareTo(ep2.getVersion());
      if (0!=res) {
        return res;
      }

      // Then pending removal
      if (ep1.isRemovalPending() && !ep2.isRemovalPending()) {
        return -1;
      }
      if (!ep1.isRemovalPending() && ep2.isRemovalPending()) {
        return 1;
      }

      // Then number of importing bundles
      res = ep2.getImportingBundles().length - ep1.getImportingBundles().length;
      if (0!=res) {
        return res;
      }

      // Finally object identity (hashCode is the closest approximation)
      res = ep2.hashCode() - ep1.hashCode();
      return res;
    }
    @Override
    public boolean equals(Object o)
    {
      return o instanceof ExportedPackageComparator;
    }

  }

}
