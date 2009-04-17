/*
 * Copyright (c) 2003-2004, Goeminne Nico
 * All rights reserved.
 *
 * Granted permission to the KNOPFLERFISH Project to use this source code.
 *
 */

import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;
import org.osgi.framework.BundleEvent;

public class CheckerBundleEventListener
    implements BundleListener {

  public static final String DEP_PACKAGE = "Dep-Package";
  public static final String DEP_LOCATION = "location";

  private PackageAdmin pa = null;
  private BundleContext bundleContext = null;

  public CheckerBundleEventListener(BundleContext bc) {
    this.bundleContext = bc;
    ServiceReference ref = bundleContext.
        getServiceReference("org.osgi.service.packageadmin.PackageAdmin");
    this.pa = (PackageAdmin) bundleContext.getService(ref);
  }

  public void bundleChanged(BundleEvent bundleEvent) {
    if (bundleEvent.getType() == bundleEvent.INSTALLED) {

      ArrayList availablePackages = new ArrayList();
      ArrayList neededPackages = new ArrayList();
      ArrayList missingPackages = new ArrayList();
      ArrayList installList = new ArrayList();

// fill of available packages
      ExportedPackage[] ep = pa.getExportedPackages(null);
      if (ep != null) {
        for (int i = 0; i < ep.length; i++) {
          availablePackages.add(ep[i].getName());
        }
      }
// fill of needed packages
      Bundle b = bundleEvent.getBundle();
      String imports = CheckerBundleEventListener.getBundleImports(b);
      try {
        Iterator i = Util.parseEntries(Constants.IMPORT_PACKAGE, imports, true);
        while (i.hasNext()) {
          Map e = (Map) i.next();
          neededPackages.add( (String) e.get("key"));
        }
      }
      catch (IllegalArgumentException e) {}

// fill of missing packages (packages needed that are not available
      for (int i = 0; i < neededPackages.size(); i++) {
        if (!availablePackages.contains(neededPackages.get(i))) {
          missingPackages.add(neededPackages.get(i));
        }
      }

// Getting the Hint URL's out of the header (adding them if not already in list)
      String deps = CheckerBundleEventListener.getBundleImportsHint(b);
      try {
        Iterator i = Util.parseEntries(DEP_PACKAGE, deps, true);
        while (i.hasNext()) {
          Map e = (Map) i.next();
          String toInstall = (String) e.get(DEP_LOCATION);
          if (!installList.contains(toInstall)) {
            installList.add(toInstall);
          }
        }
      }
      catch (IllegalArgumentException e) {}
// if the bundle isn't installed by now
      if (b.getState() != Bundle.ACTIVE &&
          b.getState() != Bundle.STARTING &&
          b.getState() != Bundle.RESOLVED) {

        // Store references to installed bundles
        Bundle [] installedBundles = new Bundle [installList.size()];
        for (int i = 0; i < installList.size(); i++) {
          try {
            installedBundles [i] =
                bundleContext.installBundle( (String) installList.get(i));
          }
          catch (BundleException ex) {}
        }
        // now everything is installed start the bundle
        try {
          b.start();
        }
        catch (BundleException ex) {}
        //roll back cose can't start the bundle anyway
        if(b.getState() != Bundle.ACTIVE) {
          for (int i = 0; i < installedBundles.length; i++){
            if(installedBundles[i] != null){
              try {
                installedBundles[i].uninstall();
              }
              catch (BundleException ex) {}
            }
          }
        }
      }
    }
  }

  public static String getBundleImports(Bundle b) {
    return (String) b.getHeaders().get(new String(Constants.IMPORT_PACKAGE));
  }

  public static String getBundleImportsHint(Bundle b) {
    return (String) b.getHeaders().get(new String(DEP_PACKAGE));
  }

}