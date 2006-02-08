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

import java.util.*;

import org.osgi.framework.*;


/**
 * Here we handle all the java packages that are imported and exported
 * within the framework.
 *
 * @author Jan Stein, Erik Wistrand
 */
class Packages {

  /**
   * Framework for bundle.
   */
  final Framework framework;

  /**
   * All exported and imported packages.
   */
  private HashMap /* String->Pkg */ packages = new HashMap();

  /**
   * Temporary set of resolved bundles during a resolve operation.
   */
  private HashSet /* BundleImpl */ tempResolved = null;

  /**
   * Temporary map of package providers during a resolve operation.
   */
  private HashMap /* String->ExportPkg */ tempProvider = null;

  /**
   * Temporary map of required bundle connections done during a resolve operation.
   */
  private HashMap /* RequireBundle->BundlePackages */ tempRequired = null;

  /**
   * Temporary set of package providers that are black listed in the resolve operation.
   */
  private HashSet /* ExportPkg */ tempBlackList = null;

  /**
   * Temporary set of bundle checked package uses back track.
   */
  private HashSet /* BundleImpl */ tempBackTracked = null;

  /* Statistics to check need for tempBlackList */
  int tempBlackListChecks = 0;
  int tempBlackListHits = 0;

  /**
   * Union of flags allowing bundle package access.
   * <p>
   * Value is <tt>Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING</tt>
   * </p>
   */
  public static int RESOLVED_FLAGS = 
    Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING;


  /**
   * Construct Packages object.
   */
  Packages(Framework fw) {
    framework = fw;
  }


  /**
   * Register all packages a bundle needs to export and import.
   * If it is registered by the system bundle, export it immediately.
   *
   * @param exports Exported packages.
   * @param imports Imported packages.
   */
  synchronized void registerPackages(Iterator exports, Iterator imports) {
    while (exports.hasNext()) {
      ExportPkg pe = (ExportPkg)exports.next();
      Pkg p = (Pkg)packages.get(pe.name);
      if (p == null) {
        p = new Pkg(pe.name);
        packages.put(pe.name, p);
      }
      p.addExporter(pe);
      if (Debug.packages) {
        Debug.println("registerPackages: export, " + pe);
      }
    }
    while (imports.hasNext()) {
      ImportPkg pe = (ImportPkg)imports.next();
      Pkg p = (Pkg)packages.get(pe.name);
      if (p == null) {
        p = new Pkg(pe.name);
        packages.put(pe.name, p);
      }
      p.addImporter(pe);
      if (Debug.packages) {
        Debug.println("registerPackages: import, " + pe);
      }
    }
  }



  /**
   * Dynamically check and register a dynamic package import.
   *
   * @param pe ImportPkg import to add.
   * @return ExportPkg for package provider.
   */
  synchronized ExportPkg registerDynamicImport(ImportPkg ip) {
    if (Debug.packages) {
      Debug.println("registerDynamicImport: try " + ip);
    }
    ExportPkg res = null;
    Pkg p = (Pkg)packages.get(ip.name);
    if (p != null) {
      tempResolved = new HashSet();
      tempProvider = new HashMap();
      tempRequired = new HashMap();
      tempBlackList = new HashSet();
      tempBackTracked = new HashSet();
      backTrackUses(ip);
      tempBackTracked = null;
      ArrayList pkgs = new ArrayList(1);
      pkgs.add(ip);
      p.addImporter(ip);
      List r = resolvePackages(pkgs.iterator());
      tempBlackList = null;
      if (r.size() == 0) {
	registerNewProviders(ip.bpkgs.bundle);
	res = (ExportPkg)tempProvider.get(ip.name);
      } else {
	p.removeImporter(ip);
      }
      tempProvider = null;
      tempRequired = null;
      tempResolved = null;
    }
    if (Debug.packages) {
      Debug.println("registerDynamicImport: Done for " + ip.name + ", res = " + res);
    }
    return res;
  }


  /**
   * Unregister bundle packages in framework. If we find exported packages
   * that has been selected as providers don't unregister them unless the
   * parameter force is true. If not all exporters were removed, the don't
   * remove any importers
   *
   * @param exports Exported packages.
   * @param imports Imported packages.
   * @param force If true force unregistration of package providers.
   * @return True if all packages were succesfully unregistered,
   *         otherwise false.
   */
  synchronized boolean unregisterPackages(Iterator exports, Iterator imports, boolean force) {
    boolean allRemoved = true;
    while (exports.hasNext()) {
      ExportPkg pe = (ExportPkg)exports.next();
      Pkg p = pe.pkg;
      if (p != null) {
        if (Debug.packages) {
          Debug.println("unregisterPackages: unregister export - " + pe);
        }
        if (!p.removeExporter(pe, force)) {
          allRemoved = false;
          pe.zombie = true;
          if (Debug.packages) {
              Debug.println("unregisterPackages: failed to unregister - " + pe);
          }
          continue;
        }
        if (p.isEmpty()) {
          packages.remove(pe.name);
        }
      }
    }
    if (allRemoved) {
      while (imports.hasNext()) {
        ImportPkg pe = (ImportPkg)imports.next();
        Pkg p = pe.pkg;
        if (p != null) {
          if (Debug.packages) {
            Debug.println("unregisterPackages: unregister import - " + pe.pkgString());
          }
          p.removeImporter(pe);
          if (p.isEmpty()) {
            packages.remove(pe.name);
          }
        }
      }
    }
    return allRemoved;
  }


  /**
   * Try to resolve all packages for a bundle.
   *
   * @param bundle Bundle owning packages.
   * @param pkgs List of packages to be resolved.
   * @return String with reason for failure or null if all were resolved.
   */
  synchronized String resolve(BundleImpl bundle, Iterator pkgs) {
    String res;
    if (Debug.packages) {
      Debug.println("resolve: " + bundle);
    }
    // If we entry with tempResolved set, it means that we already have
    // resolved bundles. Check that it is true!
    if (tempResolved != null) {
      if (!tempResolved.contains(bundle)) {
        framework.listeners.frameworkError(bundle,
                                           new Exception("resolve: InternalError1!"));
      }
      return null;
    }

    tempResolved = new HashSet();
    BundleImpl sb = checkBundleSingleton(bundle);
    if (sb != null) {
      tempResolved = null;
      return "Singleton bundle failed to resolve because " + sb + " is already resolved";
    }

    tempProvider = new HashMap();
    tempRequired = new HashMap();
    tempBlackList = new HashSet();
    tempResolved.add(bundle);
    RequireBundle br = checkRequireBundle(bundle);
    if (br == null) {
      List failed = resolvePackages(pkgs);
      if (failed.size() == 0) {
        registerNewProviders(bundle);
        res = null;
      } else {
        StringBuffer r = new StringBuffer("missing package(s) or can not resolve all of the them: ");
        Iterator mi = failed.iterator();
        r.append(((ImportPkg)mi.next()).pkgString());
        while (mi.hasNext()) {
          r.append(", ");
          r.append(((ImportPkg)mi.next()).pkgString());
        }
        res = r.toString();
      }
    } else {
      res = "Failed to find required bundle: " + br.name;
    } 
    tempResolved = null;
    tempProvider = null;
    tempRequired = null;
    tempBlackList = null;
    if (Debug.packages) {
      Debug.println("resolve: Done for " + bundle);
    }
    return res;
  }


  /**
   * Get Pkg object for named package.
   *
   * @param pkg Package name.
   * @return Pkg that represents the package, null if no such package.
   */
  synchronized Pkg getPkg(String pkg) {
    return (Pkg)packages.get(pkg);
  }
    
    
  /**
   * Get provider of a package. If several, get the one with the highest version.
   *
   * @param pkg Exported package.
   * @return ExportPkg that exports the package, null if no provider.
   */
  synchronized ExportPkg getPackageProvider(String pkg) {
    Pkg p = (Pkg)packages.get(pkg);
    if (p != null && !p.providers.isEmpty()) {
      // Get highest version
      return (ExportPkg)p.providers.get(0);
    } else {
      return null;
    }
  }
    
    
  /**
   * Check if ExportPkg is provider of a package.
   *
   * @param pe Exported package.
   * @return True if pkg exports the package.
   */
  synchronized boolean isProvider(ExportPkg pe) {
    return pe.pkg != null && pe.pkg.providers.contains(pe);
  }
    
    
  /**
   * Get all packages exported by the system.
   *
   * @return Export-package string for system bundle.
   */
  synchronized String systemPackages() {
    StringBuffer res = new StringBuffer();
    for (Iterator i = packages.values().iterator(); i.hasNext();) {
      Pkg p = (Pkg)i.next();
      for (Iterator pi = p.providers.iterator(); pi.hasNext(); ) {
        ExportPkg ep = (ExportPkg)pi.next();
        if (framework.systemBundle == ep.bpkgs.bundle) {
          if (res.length() > 0) {
            res.append(", ");
          }
          res.append(ep.pkgString());
        }
      }
    }
    return res.toString();
  }


  /**
   * Get specification version of an exported package.
   *
   * @param pkg Exported package.
   * @param bundle Exporting bundle.
   * @return Version of package or null if unspecified.
  synchronized String getPackageVersion(String pkg) {
    Pkg p = (Pkg)packages.get(pkg);
    ExportPkg pe = p.provider;
    return pe != null && pe.version.isSpecified() ? pe.version.toString() : null;
  }
   */


  /**
   * Get packages exported by bundle. If bundle is null, get all.
   *
   * @param b Bundle exporting packages.
   * @return List of packages exported by bundle.
   */
  synchronized Collection getPackagesExportedBy(Bundle b) {
    ArrayList res = new ArrayList();
    // NYI Improve the speed here!
    for (Iterator i = packages.values().iterator(); i.hasNext();) {
      Pkg p = (Pkg) i.next();
      if (!p.exporters.isEmpty()) {
        if (b == null) {
          res.addAll(p.exporters);
        } else {
          for (Iterator eps = p.exporters.iterator(); eps.hasNext();) {
            ExportPkg ep = (ExportPkg)eps.next();
            if (b == ep.bpkgs.bundle) {
              res.add(ep);
            }
          }
        }
      }
    }
    return res;
  }


  /**
   * Get active importers of a package.
   *
   * @param pkg Package.
   * @return List of bundles importering.
   */
  synchronized Collection getPackageImporters(ExportPkg ep) {
    Set res = new HashSet();
    for (Iterator i = ep.pkg.importers.iterator(); i.hasNext(); ) {
      ImportPkg ip = (ImportPkg)i.next();
      if (ip.provider == ep) {
        res.add(ip.bpkgs.bundle);
      }
    }
    return res;
  }


  /**
   * Get bundles affected by zombie packages.
   * Compute a graph of bundles starting with the specified bundles.
   * If no bundles are specified, compute a graph of bundles starting
   * with all exporting a zombie package.
   * Any bundle that imports a package that is currently exported
   * by a bundle in the graph is added to the graph. The graph is fully
   * constructed when there is no bundle outside the graph that imports a
   * package from a bundle in the graph. The graph may contain
   * <tt>UNINSTALLED</tt> bundles that are currently still
   * exporting packages.
   *
   * @param bundles Initial bundle set.
   * @return List of bundles affected.
   */
  synchronized Collection getZombieAffected(Bundle [] bundles) {
//XXX - begin L-3 modification
    // set of affected bundles will be in start-level/bundle-id order  
    TreeSet affected = new TreeSet(new Comparator() {
      public int compare(Object o1, Object o2) {
        BundleImpl b1 = (BundleImpl)o1; 
        BundleImpl b2 = (BundleImpl)o2;
        int dif  = b1.getStartLevel() - b2.getStartLevel();
        if (dif == 0) {
            dif  = (int)(b1.getBundleId() - b2.getBundleId());
        }
        return dif;
      }
      public boolean equals(Object o) {
        return ((o != null) && getClass().equals(o.getClass()));
      }
    });
//XXX - end L-3 modification
    if (bundles == null) {
      if (Debug.packages) {
        Debug.println("getZombieAffected: check - null");
      }
      for (Iterator i = packages.values().iterator(); i.hasNext();) {
        Pkg p = (Pkg)i.next();
        for (Iterator ps = p.providers.iterator(); ps.hasNext(); ) {
          ExportPkg ep = (ExportPkg)ps.next();
          if (ep.zombie) {
            if (Debug.packages) {
              Debug.println("getZombieAffected: found zombie - " + ep.bpkgs.bundle);
            }
            affected.add(ep.bpkgs.bundle);
          }
        }
      }
    } else {
      for (int i = 0; i < bundles.length; i++) {
        if (bundles[i] != null) {
          if (Debug.packages) {
            Debug.println("getZombieAffected: check - " + bundles[i]);
          }
          affected.add(bundles[i]);
        }
      }
    }
    ArrayList moreBundles = new ArrayList(affected);
    for (int i = 0; i < moreBundles.size(); i++) {
      BundleImpl b = (BundleImpl)moreBundles.get(i);
      for (Iterator j = b.getExports(); j.hasNext(); ) {
        ExportPkg ep = (ExportPkg)j.next();
        if (ep.pkg != null && ep.pkg.providers.contains(ep)) {
          for (Iterator k = getPackageImporters(ep).iterator(); k.hasNext(); ) {
            Bundle ib = (Bundle)k.next();
            if (!affected.contains(ib)) {
              moreBundles.add(ib);
              if (Debug.packages) {
                Debug.println("getZombieAffected: added - " + ib);
              }
              affected.add(ib);
            }
          }
        }
      }
    }
    return affected;
  }

  //
  // Private methods.
  //

  /**
   * Backtrack package "uses" so that we can initialize
   * tempProvider with relevent packages.
   * This perhaps to ambitious.
   *
   * @param ip Imported package to back-track from.
   * @return True if we found bundles "using" this package,
   *         otherwise we return false.
   */
  private boolean backTrackUses(ImportPkg ip) {
    if (Debug.packages) {
      Debug.println("backTrackUses: check - " + ip.pkgString());
    }
    if (tempBackTracked.contains(ip.bpkgs)) {
      return false;
    }
    tempBackTracked.add(ip.bpkgs);
    Iterator i = getPackagesProvidedBy(ip.bpkgs).iterator();
    if (i.hasNext()) {
      do {
	ExportPkg ep = (ExportPkg)i.next();
	boolean foundUses = false;
	for (Iterator ii = ep.pkg.importers.iterator(); ii.hasNext(); ) {
	  ImportPkg iip = (ImportPkg)ii.next();
	  if (iip.provider == ep) {
	    if (backTrackUses(iip)) {
	      foundUses = true;
	    }
	  }
	}
	if (!foundUses) {
	  checkUses(ep);
	}
      } while (i.hasNext());
      return true;
    } else {
      return false;
    }
  }


  /**
   * Get packages provide by specified BundlePackages.
   *
   * @param b BundlePackages exporting packages.
   * @return List of packages exported by BundlePackages.
   */
  private Collection getPackagesProvidedBy(BundlePackages bpkgs) {
    ArrayList res = new ArrayList();
    // NYI Improve the speed here!
    for (Iterator i = bpkgs.getExports(); i.hasNext();) {
      ExportPkg ep = (ExportPkg) i.next();
      if (ep.pkg.providers.contains(ep)) {
        res.add(ep);
      }
    }
    return res;
  }


  /**
   * Check if a bundle has all its package dependencies resolved.
   *
   * @param pkgs List of packages to be resolved.
   * @return List of packages not resolvable.
   */
  private List resolvePackages(Iterator pkgs) {
    ArrayList res = new ArrayList();

    while (pkgs.hasNext()) {
      ExportPkg provider = null;
      ImportPkg ip = (ImportPkg)pkgs.next();
      if (ip.provider != null) {
        framework.listeners.frameworkError(ip.bpkgs.bundle,
                                           new Exception("resolvePackages: InternalError1!"));
      }
      if (Debug.packages) {
        Debug.println("resolvePackages: check - " + ip.pkgString());
      }
      provider = (ExportPkg)tempProvider.get(ip.name);
      if (provider != null) {
        if (Debug.packages) {
          Debug.println("resolvePackages: " + ip.name + " - has temporary provider - "
                        + provider);
        }
        if (provider.zombie && provider.bpkgs.bundle.state == Bundle.UNINSTALLED) {
          if (Debug.packages) {
            Debug.println("resolvePackages: " + ip.name +
                          " - provider not used since it is an uninstalled zombie - "
                          + provider);
          }
          provider = null;
        } else if (!ip.okPackageVersion(provider.version)) {
          if (Debug.packages) {
            Debug.println("resolvePackages: " + ip.name +
                          " - provider has wrong version - " + provider +
                          ", need " + ip.packageRange + ", has " + provider.version);
          }
          provider = null;
        }
      } else {
        for (Iterator i = ip.pkg.providers.iterator(); i.hasNext(); ) {
          ExportPkg ep = (ExportPkg)i.next();
          tempBlackListChecks++;
          if (tempBlackList.contains(ep)) {
            tempBlackListHits++;
            System.err.println("BlackList " + tempBlackListHits + "/" + tempBlackListChecks);
            continue;
          }
	  if (ep.zombie) {
            // TBD! Should we refrain from using a zombie package and try a new provider instead?
            continue;
          }
          if (ip.okPackageVersion(ep.version)) {
            if (Debug.packages) {
              Debug.println("resolvePackages: " + ip.name + " - has provider - " + ep);
            }
            HashMap oldTempProvider = (HashMap)tempProvider.clone();
            if (!checkUses(ep)) {
              tempProvider = oldTempProvider;
              tempBlackList.add(ep);
              continue;
            }
            provider = ep;
            break;
          }
        }
        if (provider == null) {
          provider = pickProvider(ip);
        }
        if (provider != null) {
          tempProvider.put(ip.pkg.pkg, provider);
        }
      }
      if (provider == null) {
        if (ip.resolution == Constants.RESOLUTION_MANDATORY) {
          res.add(ip);
        } else {
          if (Debug.packages) {
            Debug.println("resolvePackages: Ok, no provider for optional " + ip.name);
          }
        }
      }
    }
    return res;
  }


  /**
   * Find a provider for specified package.
   *
   * @param pkg Package to find provider for.
   * @return Package entry that can provide.
   */
  private ExportPkg pickProvider(ImportPkg ip) {
    if (Debug.packages) {
      Debug.println("pickProvider: for - " + ip);
    }
    ExportPkg provider = null;
    for (Iterator i = ip.pkg.exporters.iterator(); i.hasNext(); ) {
      // NYI Permission check!?
      ExportPkg ep = (ExportPkg)i.next();
      tempBlackListChecks++;
      if (tempBlackList.contains(ep)) {
        tempBlackListHits++;
        System.err.println("BlackList " + tempBlackListHits + "/" + tempBlackListChecks);
        continue;
      }
      if (!checkAttributes(ep, ip)) {
        if (Debug.packages) {
          Debug.println("pickProvider: attribute match failed for - " + ep);
        }
        continue;
      }
      if (tempResolved.contains(ep.bpkgs.bundle)) {
        provider = ep;
        break;
      }
      if ((ep.bpkgs.bundle.state & RESOLVED_FLAGS) != 0) {
        HashMap oldTempProvider = (HashMap)tempProvider.clone();
        if (checkUses(ep)) {
          provider = ep;
          break;
        } else {
          tempProvider = oldTempProvider;
          tempBlackList.add(ep);
          continue;
        }
      }
      if (ep.bpkgs.bundle.state == Bundle.INSTALLED && checkResolve(ep.bpkgs.bundle)) { 
        provider = ep;
        break;
      }
    }
    if (Debug.packages) {
      if (provider != null) {
        Debug.println("pickProvider: " + ip + " - got provider - " + provider);
      } else {
        Debug.println("pickProvider: " + ip + " - found no provider");
      }
    }
    return provider;
  }


  /**
   * Check that all package attributes match.
   *
   * @param ep Exported package.
   * @param ip Imported package.
   * @return True if okay, otherwise false.
   */
  private boolean checkAttributes(ExportPkg ep, ImportPkg ip) {
    /* Mandatory attributes */
    if (ep.mandatory != null) {
      for (Iterator i = ep.mandatory.iterator(); i.hasNext(); ) {
        String a = (String)i.next();
        if (Constants.VERSION_ATTRIBUTE.equals(a)) {
          if (!ip.packageRange.isSpecified()) {
            return false;
          }
        } else if (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(a)) {
          if (ip.bundleSymbolicName == null) {
            return false;
          }
        } else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(a)) {
          if (!ip.bundleRange.isSpecified()) {
            return false;
          }
        } else if (!ip.attributes.containsKey(a)) {
          return false;
        }
      }
    }
    /* Predefined attributes */
    if (!ip.okPackageVersion(ep.version) ||
        (ip.bundleSymbolicName != null &&
         !ip.bundleSymbolicName.equals(ep.bpkgs.bundle.symbolicName)) ||
        !ip.bundleRange.withinRange(ep.bpkgs.bundle.version)) {
      return false;
    }
    /* Other attributes */
    for (Iterator i = ip.attributes.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry e = (Map.Entry)i.next();
      String a = (String)ep.attributes.get(e.getKey());
      if (a == null || !a.equals(e.getValue())) {
        return false;
      }
    }
    return true;
  }


  /**
   * Check if a bundle can be resolved. If resolvable, then the
   * objects tempResolved, tempProvider and tempBlackList are updated.
   *
   * @param b Bundle to be checked.
   * @return true
   */
  private boolean checkResolve(BundleImpl b) {
    if (checkBundleSingleton(b) == null) {
      HashSet oldTempResolved = (HashSet)tempResolved.clone();
      HashMap oldTempProvider = (HashMap)tempProvider.clone();
      HashMap oldTempRequired = (HashMap)tempRequired.clone();
      HashSet oldTempBlackList = (HashSet)tempBlackList.clone();
      tempResolved.add(b);
      if (checkRequireBundle(b) == null) { 
        List r = resolvePackages(b.getImports());
        if (r.size() == 0) {
          return true;
        }
      }
      tempResolved = oldTempResolved;
      tempProvider = oldTempProvider;
      tempRequired = oldTempRequired;
      tempBlackList = oldTempBlackList;
    }
    return false;
  }


  /**
   * Check that the packages that this provider uses do not collied with previous
   * selections. If a bundle doesn't have a uses directive we check all currently
   * imported packages. This is then applied recursivly. 
   *
   * @param pkg Exported package to check
   * @return True if we checked all packages without collision.
   */
  private boolean checkUses(ExportPkg pkg) {
    Iterator ui = null;
    String next_uses = null;
    if (Debug.packages) {
      Debug.println("checkUses: check if packages used by " + pkg + " is okay.");
    }
    if (pkg.uses != null) {
      ui = pkg.uses.iterator();
      if (ui.hasNext()) {
        next_uses = (String)ui.next();
      }
    }
    if (Debug.packages) {
      Debug.println("checkUses: provider " + pkg.bpkgs.bundle +
		    " with bpkgs=" + pkg.bpkgs);
    }
    for (Iterator i = pkg.bpkgs.getActiveImports(); i.hasNext(); ) {
      ImportPkg ip = (ImportPkg)i.next();
      if (ui != null) {
        if (next_uses == null || !ip.pkg.pkg.equals(next_uses)) {
          continue;
        }
        if (ui.hasNext()) {
          next_uses = (String)ui.next();
        } else {
          next_uses = null;
        }
      }
      ExportPkg ep = (ExportPkg)tempProvider.get(ip.pkg.pkg);
      if (Debug.packages) {
        Debug.println("checkUses: check import, " + ip +
                      " with provider, " + ip.provider);
      }
      if (ep == null) {
        tempProvider.put(ip.pkg.pkg, ip.provider);
        checkUses(ip.provider);
      } else if (ep != ip.provider) {
        if (Debug.packages) {
          Debug.println("checkUses: mismatch in providers for, " +
                        ip.pkg.pkg);
        }
        return false;
      }
    }
    if (Debug.packages) {
      Debug.println("checkUses: package " + pkg + " is okay.");
    }
    return true;
  }


  /**
   * Check that the bundle specified can be resolved without violating any
   * singleton requirements.
   *
   * @param b Bundle to check, must be in INSTALLED state
   * @return Bundle blocking resolve, otherwise null.
   */
  private BundleImpl checkBundleSingleton(BundleImpl b) {
    // NYI! More speed?
    if (b.symbolicName != null && b.singleton) {
      if (Debug.packages) {
        Debug.println("checkBundleSingleton: check singleton bundle " + b);
      }
      List bl = framework.bundles.getBundles(b.symbolicName);
      if (bl.size() > 1) {
        for (Iterator i = bl.iterator(); i.hasNext(); ) {
          BundleImpl b2 = (BundleImpl)i.next();
          if (b2.singleton && ((b2.state & RESOLVED_FLAGS) != 0 || tempResolved.contains(b2))) {
            if (Debug.packages) {
              Debug.println("checkBundleSingleton: Reject resolve because of bundle: " + b2);
            }
            return b2;
          }
        }
      }
    }
    return null;
  }


  /**
   * Check that the bundle specified can resolve all its Require-Bundle constraints.
   *
   * @param b Bundle to check, must be in INSTALLED state
   * @return RequireBundle blocking resolve, otherwise null.
   */
  private RequireBundle checkRequireBundle(BundleImpl b) {
    // NYI! More speed?
    if (b.bpkgs.require != null) {
      if (Debug.packages) {
        Debug.println("checkRequireBundle: check requiring bundle " + b);
      }
      HashMap res = new HashMap();
      for (Iterator i = b.bpkgs.require.iterator(); i.hasNext(); ) {
        RequireBundle br = (RequireBundle)i.next();
        List bl = framework.bundles.getBundles(br.name, br.bundleRange);
        BundleImpl ok = null;
        for (Iterator bci = bl.iterator(); bci.hasNext() && ok == null; ) {
          BundleImpl b2 = (BundleImpl)bci.next();
          if (tempResolved.contains(b2)) {
            ok = b2;
          } else if ((b2.state & RESOLVED_FLAGS) != 0) {
            HashMap oldTempProvider = (HashMap)tempProvider.clone();
            ok = b2;
            for (Iterator epi = b2.bpkgs.getExports(); epi.hasNext(); ) {
              ExportPkg ep = (ExportPkg)epi.next();
              if (!checkUses(ep)) {
                tempProvider = oldTempProvider;
                tempBlackList.add(ep);
                ok = null;
              }
            }
          } else if (b2.state == Bundle.INSTALLED && checkResolve(b2)) {
            ok = b2;
          }
        }
        if (ok != null) {
          if (Debug.packages) {
            Debug.println("checkRequireBundle: added required bundle " + ok);
          }
          res.put(br, ok.bpkgs);
        } else if (br.resolution == Constants.RESOLUTION_MANDATORY) {
          if (Debug.packages) {
            Debug.println("checkRequireBundle: failed to satisfy: " + br.name);
          }
          return br;
        }
      }
      tempRequired.putAll(res);
    }
    return null;
  }


  /**
   *
   */
  private void registerNewProviders(BundleImpl bundle) {
    for (Iterator i = tempProvider.values().iterator(); i.hasNext();) {
      ExportPkg ep = (ExportPkg)i.next();
      ep.pkg.addProvider(ep);
    }
    for (Iterator i = tempRequired.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry)i.next();
      BundlePackages bpkgs = (BundlePackages)e.getValue();
      RequireBundle br = (RequireBundle)e.getKey();
      br.bpkgs = bpkgs;
      if (bpkgs.requiredBy == null) {
        bpkgs.requiredBy = new ArrayList(1);
      }
      bpkgs.requiredBy.add(br.requestor);
      if (br.visibility == Constants.VISIBILITY_REEXPORT) {
        // Create necessary re-export entries
        for (Iterator be = bpkgs.getExports(); be.hasNext(); ) {
          br.requestor.checkReExport((ExportPkg)be.next());
        }
      }
    }
    for (Iterator i = tempResolved.iterator(); i.hasNext();) {
      BundleImpl bs = (BundleImpl)i.next();
      if (bs.getState() == Bundle.INSTALLED) {
	for (Iterator bi = bs.bpkgs.getImports(); bi.hasNext(); ) {
	  ImportPkg ip = (ImportPkg)bi.next();
	  ip.provider = (ExportPkg)tempProvider.get(ip.name);
	}
	if (bs != bundle) {
	  if (bs.getUpdatedState() == Bundle.INSTALLED) {
	    framework.listeners.frameworkError(bs,
	       new Exception("registerNewProviders: InternalError!"));
	  }
	}
      }
    }
  }

}
