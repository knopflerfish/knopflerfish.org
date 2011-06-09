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

/**
 * Here we handle all the java packages that are imported and exported within
 * the framework.
 * 
 * @author Jan Stein, Erik Wistrand
 */
class Packages {

  /**
   * Framework for bundle.
   */
  final FrameworkContext framework;

  /**
   * All exported and imported packages.
   */
  private Hashtable /* String->Pkg */packages = new Hashtable();

  /**
   * Temporary set of resolved bundles during a resolve operation.
   */
  private HashSet /* BundleImpl */tempResolved = null;

  /**
   * Temporary map of package providers during a resolve operation.
   */
  private HashMap /* String->ExportPkg */tempProvider = null;

  /**
   * Temporary map of required bundle connections done during a resolve
   * operation.
   */
  private HashMap /* RequireBundle->BundlePackages */tempRequired = null;

  /**
   * Temporary set of package providers that are black listed in the resolve
   * operation.
   */
  private HashSet /* ExportPkg */tempBlackList = null;

  /**
   * Temporary set of bundle checked package uses back track.
   */
  private HashSet /* BundleImpl */tempBackTracked = null;

  /* Statistics to check need for tempBlackList */
  int tempBlackListChecks = 0;
  int tempBlackListHits = 0;


  /**
   * Construct Packages object.
   */
  Packages(FrameworkContext fw) {
    framework = fw;
  }


  /**
   * Clear all datastructures in this object.
   */
  void clear() {
    packages.clear();
    if (null != tempResolved)
      tempResolved.clear();
    if (null != tempProvider)
      tempProvider.clear();
    if (null != tempRequired)
      tempRequired.clear();
    if (null != tempBlackList)
      tempBlackList.clear();
    if (null != tempBackTracked)
      tempBackTracked.clear();
  }


  /**
   * Register all packages a bundle needs to export and import. If it is
   * registered by the system bundle, export it immediately.
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
      if (framework.debug.packages) {
        framework.debug.println("registerPackages: export, " + pe);
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
      if (framework.debug.packages) {
        framework.debug.println("registerPackages: import, " + pe);
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
    if (framework.debug.packages) {
      framework.debug.println("registerDynamicImport: try " + ip);
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
        registerNewProviders(ip.bpkgs.bg.bundle);
        res = (ExportPkg)tempProvider.get(ip.name);
        ip.provider = res;
      } else {
        p.removeImporter(ip);
      }
      tempProvider = null;
      tempRequired = null;
      tempResolved = null;
    }
    if (framework.debug.packages) {
      framework.debug.println("registerDynamicImport: Done for " + ip.name + ", res = " + res);
    }
    return res;
  }


  /**
   * Unregister bundle packages in framework. If we find exported packages that
   * has been selected as providers don't unregister them unless the parameter
   * force is true. If not all exporters were removed, the don't remove any
   * importers
   * 
   * @param exports Exported packages.
   * @param imports Imported packages.
   * @param force If true force unregistration of package providers.
   * @return True if all packages were succesfully unregistered, otherwise
   *         false.
   */
  synchronized boolean unregisterPackages(Iterator exports,
                                          Iterator imports,
                                          boolean force) {
    // Check if somebody other than ourselves use our exports
    if (!force) {
      ArrayList saved = new ArrayList();
      for (Iterator i = exports; i.hasNext();) {
        ExportPkg ep = (ExportPkg)i.next();
        saved.add(ep);
        // Is the exporting bundle wired to any bundle via Require-Bundle
        if (ep.bpkgs.isRequired()) {
          if (framework.debug.packages) {
            framework.debug.println("unregisterPackages: Failed to unregister, "
                                    + ep + " is still in use via Require-Bundle.");
          }
          markAsZombies(saved, exports);
          return false;
        }
        Pkg p = ep.pkg;
        if (p.providers.contains(ep)) {
          for (Iterator ii = p.importers.iterator(); ii.hasNext();) {
            ImportPkg ip = (ImportPkg)ii.next();
            if (ep == ip.provider && ep.bpkgs != ip.bpkgs) {
              if (framework.debug.packages) {
                framework.debug.println("unregisterPackages: Failed to unregister, "
                                        + ep + " is still in use via import-package.");
              }
              markAsZombies(saved, exports);
              return false;
            }
          }
        }
      }
      exports = saved.iterator();
    }

    while (exports.hasNext()) {
      ExportPkg ep = (ExportPkg)exports.next();
      Pkg p = ep.pkg;
      if (framework.debug.packages) {
        framework.debug.println("unregisterPackages: unregister export - " + ep);
      }
      p.removeExporter(ep);
      if (p.isEmpty()) {
        packages.remove(ep.name);
      }
    }

    while (imports.hasNext()) {
      ImportPkg ip = (ImportPkg)imports.next();
      Pkg p = ip.pkg;
      if (framework.debug.packages) {
        framework.debug.println("unregisterPackages: unregister import - "
                                + ip.pkgString());
      }
      p.removeImporter(ip);
      if (p.isEmpty()) {
        packages.remove(ip.name);
      }
    }
    return true;
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
    if (framework.debug.packages) {
      framework.debug.println("resolve: " + bundle);
    }
    // If we enter with tempResolved set, it means that we already have
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
    String br = checkRequireBundle(bundle);
    if (br == null) {
      List failed = resolvePackages(pkgs);
      if (failed.size() == 0) {
        registerNewProviders(bundle);
        res = null;
      } else {
        StringBuffer r = new StringBuffer(
            "missing package(s) or can not resolve all of the them: ");
        Iterator mi = failed.iterator();
        r.append(((ImportPkg)mi.next()).pkgString());
        while (mi.hasNext()) {
          r.append(", ");
          r.append(((ImportPkg)mi.next()).pkgString());
        }
        res = r.toString();
      }
    } else {
      res = "Failed to resolve required bundle or host: " + br;
    }
    tempResolved = null;
    tempProvider = null;
    tempRequired = null;
    tempBlackList = null;
    if (framework.debug.packages) {
      framework.debug.println("resolve: Done for " + bundle);
    }
    return res;
  }


  /**
   * Get Pkg object for named package.
   * 
   * @param pkg Package name.
   * @return Pkg that represents the package, null if no such package.
   */
  Pkg getPkg(String pkg) {
    return (Pkg)packages.get(pkg);
  }


  /**
   * Get bundles affected by zombie packages.
   * 
   * Compute a graph of bundles starting with the specified bundles. If no
   * bundles are specified, compute a graph of bundles starting with all
   * exporting a zombie package. Any bundle that imports a package that is
   * currently exported by a bundle in the graph (or requires a bundle that is
   * in the graph) is added to the graph. The graph is fully constructed when
   * there is no bundle outside the graph that imports a package from a bundle
   * in the graph (and there is no bundle outside the graph that requires a
   * bundle in the graph). The graph may contain <tt>UNINSTALLED</tt> bundles
   * that are currently still exporting packages.
   * 
   * @param bundles Initial bundle set.
   * @return List of bundles affected.
   */
  synchronized TreeSet getZombieAffected(Bundle[] bundles) {
    // set of affected bundles will be in start-level/bundle-id order
    TreeSet affected = new TreeSet(new Comparator() {
      public int compare(Object o1, Object o2) {
        BundleImpl b1 = (BundleImpl)o1;
        BundleImpl b2 = (BundleImpl)o2;
        int dif = b1.getStartLevel() - b2.getStartLevel();
        if (dif == 0) {
          dif = (int)(b1.getBundleId() - b2.getBundleId());
        }
        return dif;
      }


      public boolean equals(Object o) {
        return ((o != null) && getClass().equals(o.getClass()));
      }
    });
    if (bundles == null) {
      if (framework.debug.packages) {
        framework.debug.println("getZombieAffected: check - null");
      }
      for (Iterator i = packages.values().iterator(); i.hasNext();) {
        Pkg p = (Pkg)i.next();
        // Search all exporters to catch both provided and required pkgs
        for (Iterator ps = p.exporters.iterator(); ps.hasNext();) {
          ExportPkg ep = (ExportPkg)ps.next();
          if (ep.zombie) {
            if (framework.debug.packages) {
              framework.debug.println("getZombieAffected: found zombie - " + ep);
            }
            affected.add(ep.bpkgs.bg.bundle);
          }
        }
      }
    } else {
      for (int i = 0; i < bundles.length; i++) {
        BundleImpl tmp = (BundleImpl)bundles[i];
        if (tmp != null) {
          if (framework.debug.packages) {
            framework.debug.println("getZombieAffected: check - " + bundles[i]);
          }
          affected.add(tmp);
          Vector h = tmp.getHosts(true);
          if (h != null) {
            for (Iterator hi = h.iterator(); hi.hasNext();) {
              affected.add(((BundleGeneration)hi.next()).bundle);
            }
          }
        }
      }
    }
    ArrayList moreBundles = new ArrayList(affected);
    for (int i = 0; i < moreBundles.size(); i++) {
      BundleImpl b = (BundleImpl)moreBundles.get(i);
      for (Iterator j = b.getExports(); j.hasNext();) {
        ExportPkg ep = (ExportPkg)j.next();
        if (ep.pkg != null && ep.pkg.providers.contains(ep)) {
          for (Iterator k = ep.getPackageImporters().iterator(); k.hasNext();) {
            Bundle ib = (Bundle)k.next();
            if (!affected.contains(ib)) {
              moreBundles.add(ib);
              if (framework.debug.packages) {
                framework.debug.println("getZombieAffected: added importing bundle - "
                                        + ib);
              }
              affected.add(ib);
            }
          }
        }
        for (Iterator rbi = ep.bpkgs.getRequiredBy().iterator(); rbi.hasNext();) {
          BundlePackages rbpkgs = (BundlePackages)rbi.next();
          Bundle rb = rbpkgs.bg.bundle;
          if (!affected.contains(rb)) {
            moreBundles.add(rb);
            if (framework.debug.packages) {
              framework.debug.println("getZombieAffected: added requiring bundle - "
                                      + rb);
            }
            affected.add(rb);
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
   * Backtrack package "uses" so that we can initialize tempProvider with
   * relevent packages. This perhaps to ambitious.
   * 
   * @param ip Imported package to back-track from.
   * @return True if we found bundles "using" this package, otherwise we return
   *         false.
   */
  private boolean backTrackUses(ImportPkg ip) {
    if (framework.debug.packages) {
      framework.debug.println("backTrackUses: check - " + ip.pkgString());
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
        for (Iterator ii = ep.pkg.importers.iterator(); ii.hasNext();) {
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
   * Mark list of exporters as zombie packages.
   * 
   * @param exporters List of ExportPkg.
   */
  private void markAsZombies(List e1, Iterator e2) {
    for (Iterator i = e1.iterator(); i.hasNext();) {
      ((ExportPkg)i.next()).zombie = true;
    }
    while (e2.hasNext()) {
      ((ExportPkg)e2.next()).zombie = true;
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
      ExportPkg ep = (ExportPkg)i.next();
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
        framework.listeners.frameworkError(ip.bpkgs.bg.bundle,
                                           new Exception("resolvePackages: InternalError1!"));
      }
      if (framework.debug.packages) {
        framework.debug.println("resolvePackages: check - " + ip.pkgString());
      }
      provider = (ExportPkg)tempProvider.get(ip.name);
      if (provider != null) {
        if (framework.debug.packages) {
          framework.debug.println("resolvePackages: " + ip.name
              + " - has temporary provider - "
                                  + provider);
        }
        if (provider.zombie && provider.bpkgs.bg.bundle.state == Bundle.UNINSTALLED) {
          if (framework.debug.packages) {
            framework.debug.println("resolvePackages: " + ip.name +
                                    " - provider not used since it is an uninstalled zombie - "
                                    + provider);
          }
          provider = null;
        } else if (!ip.checkAttributes(provider)) {
          if (framework.debug.packages) {
            framework.debug.println("resolvePackages: " + ip.name +
                                    " - provider has wrong attributes - " + provider);
            // NYI, print what is missing
          }
          provider = null;
        } else if (ip.bpkgs != provider.bpkgs && !ip.checkPermission(provider)) {
          // We can not make internal wire if it collide with the
          // provided version.
          if (framework.debug.packages) {
            framework.debug.println("resolvePackages: " + ip.name +
                                    " -  has no permission to use - " + provider);
          }
          provider = null;
        }
      } else {
        for (Iterator i = ip.pkg.providers.iterator(); i.hasNext();) {
          ExportPkg ep = (ExportPkg)i.next();
          tempBlackListChecks++;
          if (tempBlackList.contains(ep)) {
            tempBlackListHits++;
            continue;
          }
          if (ep.zombie) {
            // TBD! Should we refrain from using a zombie package and try a new
            // provider instead?
            continue;
          }
          if (ip.checkAttributes(ep)) {
            if (framework.debug.packages) {
              framework.debug.println("resolvePackages: " + ip.name +
                                      " - has provider - " + ep);
            }
            if (ip.checkPermission(ep)) {
              HashMap oldTempProvider = (HashMap)tempProvider.clone();
              if (!checkUses(ep)) {
                tempProvider = oldTempProvider;
                tempBlackList.add(ep);
                continue;
              }
              provider = ep;
              break;
            } else {
              if (framework.debug.packages) {
                framework.debug.println("resolvePackages: no permission for - " + ep);
              }
            }
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
        if (ip.mustBeResolved()) {
          res.add(ip);
        } else {
          if (framework.debug.packages) {
            framework.debug.println("resolvePackages: Ok, no provider for optional " + ip.name);
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
    if (framework.debug.packages) {
      framework.debug.println("pickProvider: for - " + ip);
    }
    ArrayList possibleProvider = new ArrayList(ip.pkg.exporters.size());
    for (Iterator i = ip.pkg.exporters.iterator(); i.hasNext();) {
      ExportPkg ep = (ExportPkg)i.next();
      tempBlackListChecks++;
      if (tempBlackList.contains(ep)) {
        tempBlackListHits++;
        continue;
      }
      if (!ip.checkAttributes(ep)) {
        if (framework.debug.packages) {
          framework.debug.println("pickProvider: attribute match failed for - " + ep);
        }
        continue;
      }
      if (!ip.checkPermission(ep)) {
        if (ip.bpkgs == ep.bpkgs) {
          if (framework.debug.packages) {
            framework.debug.println("pickProvider: internal wire ok for - " + ep);
          }
          ip.internalOk = ep;
        }
        if (framework.debug.packages) {
          framework.debug.println("pickProvider: no import permission for - " + ep);
        }
        continue;
      }
      if (!ep.checkPermission()) {
        if (framework.debug.packages) {
          framework.debug.println("pickProvider: no export permission for - " + ep);
        }
        continue;
      }
      if ((ep.bpkgs.bg.bundle.state & BundleImpl.RESOLVED_FLAGS) != 0) {
        HashMap oldTempProvider = (HashMap)tempProvider.clone();
        if (checkUses(ep)) {
          if (framework.debug.packages) {
            framework.debug.println("pickProvider: " + ip +
                                    " - got resolved provider - " + ep);
          }
          return ep;
        } else {
          tempProvider = oldTempProvider;
          tempBlackList.add(ep);
          continue;
        }
      }
      if (ep.bpkgs.bg.bundle.state == Bundle.INSTALLED) {
        possibleProvider.add(ep);
      }
    }
    for (Iterator i = possibleProvider.iterator(); i.hasNext();) {
      ExportPkg ep = (ExportPkg)i.next();
      if (framework.debug.packages) {
        framework.debug.println("pickProvider: check possible provider - " + ep);
      }
      if (tempResolved.contains(ep.bpkgs.bg.bundle) || checkResolve(ep.bpkgs.bg.bundle, ep)) {
        if (framework.debug.packages) {
          framework.debug.println("pickProvider: " + ip + " - got provider - " + ep);
        }
        return ep;
      }
    }
    if (framework.debug.packages) {
      framework.debug.println("pickProvider: " + ip + " - found no provider");
    }
    return null;
  }


  /**
   * Check if a bundle can be resolved. If resolvable, then the objects
   * tempResolved, tempProvider and tempBlackList are updated. Bundle must be in
   * installed state.
   * 
   * @param b Bundle to be checked.
   * @param ep ExportPkg that must be exported by bundle.
   * @return true if resolvable otherwise false.
   */
  private boolean checkResolve(BundleImpl b, ExportPkg ep) {
    if (checkBundleSingleton(b) == null) {
      HashSet oldTempResolved = (HashSet)tempResolved.clone();
      HashMap oldTempProvider = (HashMap)tempProvider.clone();
      HashMap oldTempRequired = (HashMap)tempRequired.clone();
      HashSet oldTempBlackList = (HashSet)tempBlackList.clone();
      if (ep != null) {
        tempProvider.put(ep.pkg.pkg, ep);
      }
      tempResolved.add(b);
      if (checkRequireBundle(b) == null) {
        List r = resolvePackages(b.gen.bpkgs.getImports());
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
   * Check that the packages that this provider uses do not collied with
   * previous selections. If a bundle doesn't have a uses directive we check all
   * currently imported packages. This is then applied recursivly.
   * 
   * @param pkg Exported package to check
   * @return True if we checked all packages without collision.
   */
  private boolean checkUses(ExportPkg pkg) {
    Iterator ui = null;
    String next_uses = null;
    if (framework.debug.packages) {
      framework.debug.println("checkUses: check if packages used by " + pkg + " is okay.");
    }
    if (pkg.uses != null) {
      ui = pkg.uses.iterator();
      if (ui.hasNext()) {
        next_uses = (String)ui.next();
      }
    }
    if (framework.debug.packages) {
      framework.debug.println("checkUses: provider with bpkgs=" + pkg.bpkgs);
    }
    ArrayList checkList = new ArrayList();
    for (Iterator i = pkg.bpkgs.getActiveImports(); i.hasNext();) {
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
      if (framework.debug.packages) {
        framework.debug.println("checkUses: check import, " + ip +
                                " with provider, " + ip.provider);
      }
      if (ep == null) {
        tempProvider.put(ip.pkg.pkg, ip.provider);
        checkList.add(ip.provider);
      } else if (ep != ip.provider) {
        if (framework.debug.packages) {
          framework.debug.println("checkUses: mismatch in providers for, " + ip.pkg.pkg);
        }
        return false;
      }
    }
    for (Iterator i = checkList.iterator(); i.hasNext();) {
      if (!checkUses((ExportPkg)i.next())) {
        return false;
      }
    }
    if (framework.debug.packages) {
      framework.debug.println("checkUses: package " + pkg + " is okay.");
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
    if (b.gen.symbolicName != null && b.gen.singleton) {
      if (framework.debug.packages) {
        framework.debug.println("checkBundleSingleton: check singleton bundle " + b);
      }
      List bl = framework.bundles.getBundles(b.gen.symbolicName);
      if (bl.size() > 1) {
        for (Iterator i = bl.iterator(); i.hasNext();) {
          BundleImpl b2 = (BundleImpl)i.next();
          if (b2.gen.singleton && ((b2.state & BundleImpl.RESOLVED_FLAGS) != 0 ||
                               tempResolved.contains(b2))) {
            if (framework.debug.packages) {
              framework.debug.println("checkBundleSingleton: Reject because of bundle: " + b2);
            }
            return b2;
          }
        }
      }
    }
    return null;
  }


  /**
   * Check that the bundle specified can resolve all its Require-Bundle
   * constraints.
   * 
   * @param b Bundle to check, must be in INSTALLED state
   * @return Symbolic name of bundle blocking resolve, otherwise null.
   */
  private String checkRequireBundle(BundleImpl b) {
    // NYI! More speed?
    Iterator i = b.gen.bpkgs.getRequire();
    if (i != null) {
      if (framework.debug.packages) {
        framework.debug.println("checkRequireBundle: check requiring bundle " + b);
      }
      if (!framework.perm.okRequireBundlePerm(b)) {
        return b.gen.symbolicName;
      }
      HashMap res = new HashMap();
      do {
        RequireBundle br = (RequireBundle)i.next();
        List bl = framework.bundles.getBundles(br.name, br.bundleRange);
        BundleImpl ok = null;
        for (Iterator bci = bl.iterator(); bci.hasNext() && ok == null;) {
          BundleImpl b2 = (BundleImpl)bci.next();
          if (tempResolved.contains(b2)) {
            ok = b2;
          } else if ((b2.state & BundleImpl.RESOLVED_FLAGS) != 0) {
            HashMap oldTempProvider = (HashMap)tempProvider.clone();
            ok = b2;
            for (Iterator epi = b2.gen.bpkgs.getExports(); epi.hasNext();) {
              ExportPkg ep = (ExportPkg)epi.next();
              if (!checkUses(ep)) {
                tempProvider = oldTempProvider;
                tempBlackList.add(ep);
                ok = null;
              }
            }
          } else if (b2.state == Bundle.INSTALLED &&
                     framework.perm.okProvideBundlePerm(b2) &&
                     checkResolve(b2, null)) {
            ok = b2;
          }
        }
        if (ok != null) {
          if (framework.debug.packages) {
            framework.debug.println("checkRequireBundle: added required bundle " + ok);
          }
          res.put(br, ok.gen.bpkgs);
        } else if (br.resolution == Constants.RESOLUTION_MANDATORY) {
          if (framework.debug.packages) {
            framework.debug.println("checkRequireBundle: failed to satisfy: " + br.name);
          }
          return br.name;
        }
      } while (i.hasNext());
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
      bpkgs.addRequiredBy(br.requestor);
      if (framework.debug.packages) {
        framework.debug.println("registerNewProviders: '"
                                + Constants.REQUIRE_BUNDLE + ": " + br.name
                                + "' for " + br.requestor.bg.bundle.getBundleId()
                                + " bound to (id=" + bpkgs.bg.bundle.getBundleId()
                                + ",gen=" + bpkgs.bg.generation + ")");
      }
      if (br.visibility == Constants.VISIBILITY_REEXPORT) {
        // Create necessary re-export entries
        for (Iterator be = bpkgs.getExports(); be.hasNext();) {
          ExportPkg ep = (ExportPkg)be.next();
          br.requestor.checkReExport(ep);
          if (framework.debug.packages) {
            framework.debug.println("registerNewProviders: "
                                    + br.requestor.bg.bundle.getBundleId()
                                    + " reexports package " + ep.name);
          }
        }
      }
    }
    for (Iterator i = tempResolved.iterator(); i.hasNext();) {
      BundleImpl bs = (BundleImpl)i.next();
      if (bs.getState() == Bundle.INSTALLED) {
        for (Iterator bi = bs.gen.bpkgs.getImports(); bi.hasNext();) {
          ImportPkg ip = (ImportPkg)bi.next();
          ExportPkg ep = (ExportPkg)tempProvider.get(ip.name);
          if (ep == null) {
            // There could be an internal connection, that should export a
            // package
            if (ip.internalOk != null) {
              if (ip.internalOk.pkg.providers.isEmpty() &&
                  ip.internalOk.checkPermission()) {
                ip.internalOk.pkg.addProvider(ip.internalOk);
                if (framework.debug.packages) {
                  framework.debug.println("registerNewProviders: exported internal wire, "
                                          + ip + " -> " + ip.internalOk);
                }
              } else {
                if (framework.debug.packages) {
                  framework.debug.println("registerNewProviders: internal wire, "
                                          + ip + " -> " + ip.internalOk);
                }
              }
            }
          } else {
            // NYI! This check is already done, we should cache result
            if (ip.checkAttributes(ep) && ip.checkPermission(ep)) {
              ip.provider = ep;
            } else {
              // Check if got a missmatching internal wire.
              if (ip.internalOk != null) {
                if (ip.internalOk == ep) {
                  if (framework.debug.packages) {
                    framework.debug.println("registerNewProviders: internal wire, " + ip + ", "
                        + ep);
                  }
                } else {
                  // TBD, should we resolve when this happens!?
                  framework.listeners.frameworkError(bs,
                       new Exception("registerNewProviders: Warning! Internal wire for, " + ip +
                                     ", does not match exported. " + ep));
                }
              }
            }
          }
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
