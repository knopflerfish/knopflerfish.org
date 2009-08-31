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
  ServiceTracker pkgTracker;

  public PackageManager(ServiceTracker pkgTracker) {
    this.pkgTracker = pkgTracker;
    refresh();
  }

  Map bundleExports = new HashMap();
  Map bundleImports = new HashMap();
  Map bundleReqs    = new HashMap();
  Map manifestImports = new HashMap();
  Map missingImports = new HashMap();


  // Bundle -> RequiredBundle
  Map requiredBundleMap = new HashMap();

  // RequiredBundle -> Boolean
  Map isRequiringMap = new HashMap();

  // int maxSize = 0;

  Object lock = new Object();

  /*
  public int getMaxSize() {
    synchronized(lock) {
      return maxSize;
    }
  }
  */

  public PackageAdmin getPackageAdmin() {
    return (PackageAdmin)pkgTracker.getService();
  }

  static Bundle[] EMPTY_BUNDLES = new Bundle[0];

  public Bundle[] getHosts(Bundle b) {
    PackageAdmin pkgAdmin = getPackageAdmin();
    Bundle[] bl = pkgAdmin.getHosts(b);
    return bl != null ? bl : EMPTY_BUNDLES;
  }

 public Bundle[] getFragments(Bundle b) {
    PackageAdmin pkgAdmin = getPackageAdmin();
    Bundle[] bl = pkgAdmin.getFragments(b);
    return bl != null ? bl : EMPTY_BUNDLES;
  }

  public Collection getImportedPackages(Bundle b) {
    synchronized(lock) {
      Collection s = (Collection)bundleImports.get(b);
      return s != null ? s : Collections.EMPTY_SET;
    }
  }

  public Collection getRequiredPackages(Bundle b) {
    synchronized(lock) {
      Collection s = (Collection)bundleReqs.get(b);
      return s != null ? s : Collections.EMPTY_SET;
    }
  }


  public Collection getManifestImports(Bundle b) {
    synchronized(lock) {
      Collection c = (Collection)manifestImports.get(b);
      if(c == null) {
        c = getPackageNames(b, "Import-Package");
        manifestImports.put(b, c);
      }

      return c;
    }
  }

  public Collection getMissingImports(Bundle b) {
    synchronized(lock) {
      Collection missing = (Collection)missingImports.get(b);
      if(missing == null) {
        missing = new TreeSet();
        missing.addAll(getManifestImports(b));

        Collection okSet  = getImportedPackages(b);

        for(Iterator it = okSet.iterator(); it.hasNext(); ) {
          ExportedPackage pkg = (ExportedPackage)it.next();
          missing.remove(pkg.getName());
        }

        missingImports.put(b, missing);
      }

      return missing;
    }
  }

  /**
   * Get the specified header name from a bundle and parse
   * the value as package names (ignoring any version info)
   *
   * @return a collection of strings
   */
  protected Collection getPackageNames(Bundle b, String headerName) {
    Set set = new TreeSet();
    String v = (String)b.getHeaders().get(headerName);
    if(v != null && v.length() > 0) {
      String[] packages = Text.splitwords(v, ",", '\0');
      for(int i = 0; i < packages.length; i++) {
        String pkgName = packages[i];
        int ix = pkgName.indexOf(";");
        if(ix != -1) {
          pkgName = pkgName.substring(0, ix);
        }
        set.add(pkgName);
      }
    }
    return set;
  }


  public Collection getExportedPackages(Bundle b) {
    synchronized(lock) {
      Collection r = (Collection)bundleExports.get(b);
      if(r == null) {
        r = new TreeSet(pkgComparator);
        PackageAdmin pkgAdmin = getPackageAdmin();
        ExportedPackage[] pkgs = pkgAdmin.getExportedPackages(b);
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

  PackageFilter packageFilter = null;
  // new NoCommonPackagesFilter();

  public void setPackageFilter(PackageFilter filter) {
    this.packageFilter = filter;
    refresh();
  }


  protected boolean accept(ExportedPackage pkg) {
    if(packageFilter != null) {
      boolean b = packageFilter.accept(pkg);
      return b;
    } else {
      return true;
    }
  }

  public static interface PackageFilter {
    boolean accept(ExportedPackage pkg);
  }

  public static class NoCommonPackagesFilter implements PackageFilter {
    public boolean accept(ExportedPackage pkg) {
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

      PackageAdmin      pkgAdmin = getPackageAdmin();
      ExportedPackage[] pkgs     = pkgAdmin.getExportedPackages((Bundle)null);
      RequiredBundle[]  rbl      = pkgAdmin.getRequiredBundles(null);

      for(int i = 0; pkgs != null && i < pkgs.length; i++) {

        if(accept(pkgs[i])) {
          Bundle   fromB = pkgs[i].getExportingBundle();
          Bundle[] bl    = pkgs[i].getImportingBundles();

          for(int j = 0; bl != null && j < bl.length; j++) {
            /*
            set(fromB, bl[j], EXPORT);
            set(bl[j], fromB, IMPORT);
            */

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
  public boolean isBundleRequiredBy(RequiredBundle[] rbl,
                                    Bundle requiredBundle,
                                    Bundle requiringBundle)
  {
    RequiredBundle rb = getRequiredBundle(rbl, requiredBundle);

    Bundle[] requiringBundles = rb!=null ? rb.getRequiringBundles() : null;
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
  public RequiredBundle getRequiredBundle(RequiredBundle[] rbl,
                                          Bundle b)  {
    RequiredBundle rb = (RequiredBundle)requiredBundleMap.get(b);
    if(rb != null) {
      return rb;
    }
    for (int i=0; rbl!=null && i<rbl.length; i++) {
      Bundle rbb = rbl[i].getBundle();
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

      if (ep1.getName().equals(ep2.getName())) {
        ep1.getVersion().compareTo(ep2.getVersion());
      }
      return ep1.getName().compareTo(ep2.getName());
    }
    public boolean equals(Object o)
    {
      return o instanceof ExportedPackageComparator;
    }

  }

}
