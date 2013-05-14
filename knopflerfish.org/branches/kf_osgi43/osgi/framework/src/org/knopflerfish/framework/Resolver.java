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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Here we handle all the java packages that are imported and exported within
 * the framework.
 *
 * @author Jan Stein, Erik Wistrand
 */
class Resolver {

  static final String RESOLVER_HOOK_VETO = "ResolverHook Veto";

  /**
   * Framework for bundle.
   */
  final FrameworkContext framework;

  /**
   * All exported and imported packages.
   */
  private final Hashtable<String, Pkg> packages = new Hashtable<String, Pkg>();

  /*
   * All BundleCapabilities that can be or is resolved.
   */
  private final Capabilities capabilities = new Capabilities();

  /**
   * Temporary set of resolved bundles during a resolve operation.
   */
  private volatile HashSet<BundleGeneration> tempResolved = null;

  /**
   * Temporary map of package providers during a resolve operation.
   */
  private HashMap<String, ExportPkg> tempProvider = null;

  /**
   * Temporary map of required bundle connections done during a resolve
   * operation.
   */
  private HashMap<RequireBundle, BundlePackages> tempRequired = null;

  /**
   * Temporary set of package providers that are black listed in the resolve
   * operation.
   */
  private HashSet<ExportPkg> tempBlackList = null;

  /**
   * Temporary set of bundle checked package uses back track.
   */
  private HashSet<BundlePackages> tempBackTracked = null;

  /**
   * Temporary list of bundle wires during a resolve operation.
   */
  private ArrayList<BundleWireImpl> tempWires = null;


  /* Statistics to check need for tempBlackList */
  int tempBlackListChecks = 0;
  int tempBlackListHits = 0;


  /**
   * Construct Packages object.
   */
  Resolver(FrameworkContext fw) {
    framework = fw;
  }


  /**
   * Clear all datastructures in this object.
   */
  void clear() {
    packages.clear();
    if (null != tempResolved) {
      tempResolved.clear();
    }
    if (null != tempProvider) {
      tempProvider.clear();
    }
    if (null != tempRequired) {
      tempRequired.clear();
    }
    if (null != tempBlackList) {
      tempBlackList.clear();
    }
    if (null != tempBackTracked) {
      tempBackTracked.clear();
    }
    if (null != tempWires) {
      tempWires.clear();
    }
  }


  /**
   * Register all packages a bundle needs to export and import. If it is
   * registered by the system bundle, export it immediately.
   *
   * @param exports Exported packages.
   * @param imports Imported packages.
   */
  synchronized void registerCapabilities(Map<String, List<BundleCapabilityImpl>> capabilities,
                                         Iterator<ExportPkg> exports,
                                         Iterator<ImportPkg> imports) {
    this.capabilities.addCapabilities(capabilities);
    while (exports.hasNext()) {
      final ExportPkg pe = exports.next();
      Pkg p = packages.get(pe.name);
      if (p == null) {
        p = new Pkg(pe.name);
        packages.put(pe.name, p);
      }
      p.addExporter(pe);
      if (framework.debug.resolver) {
        framework.debug.println("registerPackages: export, " + pe);
      }
    }
    while (imports.hasNext()) {
      final ImportPkg pe = imports.next();
      Pkg p = packages.get(pe.name);
      if (p == null) {
        p = new Pkg(pe.name);
        packages.put(pe.name, p);
      }
      p.addImporter(pe);
      if (framework.debug.resolver) {
        framework.debug.println("registerPackages: import, " + pe);
      }
    }
  }


  /**
   * Dynamically check and register a dynamic package import.
   *
   * @param pe ImportPkg import to add.
   * @return ExportPkg for package provider.
   * @throws BundleException Resolver hook complaint.
   */
  synchronized ExportPkg registerDynamicImport(ImportPkg ip) throws BundleException {
    if (framework.debug.resolver) {
      framework.debug.println("registerDynamicImport: try " + ip);
    }
    ExportPkg res = null;
    final Pkg p = packages.get(ip.name);
    if (p != null) {
      // Wait for other resolve operations to
      while (tempResolved != null) {
        checkThread();
        try {
          wait();
        } catch (final InterruptedException _ignore) { }
      }
      tempResolved = new HashSet<BundleGeneration>();
      tempProvider = new HashMap<String, ExportPkg>();
      tempRequired = new HashMap<RequireBundle, BundlePackages>();
      tempBlackList = new HashSet<ExportPkg>();
      tempBackTracked = new HashSet<BundlePackages>();
      backTrackUses(ip);
      tempBackTracked = null;
      final List<ImportPkg> pkgs = Collections.singletonList(ip);
      p.addImporter(ip);
      try {
        final List<ImportPkg> r = resolvePackages(pkgs.iterator());
        tempBlackList = null;
        if (r.size() == 0) {
          registerNewProviders(ip.bpkgs.bg.bundle);
          res = tempProvider.get(ip.name);
          ip.provider = res;
        } else {
          p.removeImporter(ip);
        }
      } catch (BundleException be) {
        p.removeImporter(ip);
        throw be;
      } finally {
        tempProvider = null;
        tempRequired = null;
        tempResolved = null;
        notifyAll();
      }
    }
    if (framework.debug.resolver) {
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
   * @return True if all packages were successfully unregistered, otherwise
   *         false.
   */
  synchronized boolean unregisterCapabilities(Map<String, List<BundleCapabilityImpl>> capabilities,
                                              Iterator<ExportPkg> exports,
                                              Iterator<ImportPkg> imports,
                                              boolean force) {
    // Check if somebody other than ourselves use our exports
    if (!force) {
      final ArrayList<ExportPkg> saved = new ArrayList<ExportPkg>();
      for (final Iterator<ExportPkg> i = exports; i.hasNext();) {
        final ExportPkg ep = i.next();
        saved.add(ep);
        // Is the exporting bundle wired to any bundle via Require-Bundle
        if (ep.bpkgs.isRequired()) {
          if (framework.debug.resolver) {
            framework.debug.println("unregisterPackages: Failed to unregister, "
                                    + ep + " is still in use via Require-Bundle.");
          }
          markAsZombies(saved, exports);
          return false;
        }
        final Pkg p = ep.pkg;
        if (p.providers.contains(ep)) {
          for (final Object element : p.importers) {
            final ImportPkg ip = (ImportPkg)element;
            if (ep == ip.provider && ep.bpkgs != ip.bpkgs) {
              if (framework.debug.resolver) {
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
      for (List<BundleCapabilityImpl> lbc : capabilities.values()) {
        for (BundleCapabilityImpl bc : lbc) {
          if (bc.isWired()) {
            return false;
          }
        }
      }
    }

    while (exports.hasNext()) {
      final ExportPkg ep = exports.next();
      final Pkg p = ep.pkg;
      if (framework.debug.resolver) {
        framework.debug.println("unregisterPackages: unregister export - " + ep);
      }
      p.removeExporter(ep);
      if (p.isEmpty()) {
        packages.remove(ep.name);
      }
    }

    while (imports.hasNext()) {
      final ImportPkg ip = imports.next();
      final Pkg p = ip.pkg;
      if (framework.debug.resolver) {
        framework.debug.println("unregisterPackages: unregister import - "
                                + ip.pkgString());
      }
      p.removeImporter(ip);
      if (p.isEmpty()) {
        packages.remove(ip.name);
      }
    }
    this.capabilities.removeCapabilities(capabilities);
    for (List<BundleCapabilityImpl> lbc : capabilities.values()) {
      for (BundleCapabilityImpl bc : lbc) {
        bc.removeWires();
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
   * @throws BundleException Resolver hook complaint.
   */
  synchronized String resolve(BundleGeneration bg, Iterator<ImportPkg> pkgs) throws BundleException {
    String res = null;
    if (framework.debug.resolver) {
      framework.debug.println("resolve: " + bg);
    }
    // If we enter with tempResolved set, it means that we already have
    // resolved bundles. Check that it is true!
    if (tempResolved != null) {
      if (tempResolved.remove(bg)) {
        return null;
      }
      // Not true, wait before starting new resolve process.
      checkThread();
      do {
 	     try {
  	      wait();
    	  } catch (final InterruptedException _ignore) { }
      } while (tempResolved != null);
   	}

    tempResolved = new HashSet<BundleGeneration>();
    try {
      if (!addTempResolved(bg)) {
        res = RESOLVER_HOOK_VETO;
      }
    } catch (BundleException be) {
      tempResolved = null;
      notifyAll();
      throw be;
    }
    if (res == null) {
      final BundleGeneration sbg = checkBundleSingleton(bg);
      if (sbg != null) {
        res = "Singleton bundle failed to resolve because " +
              sbg.bundle + " is already resolved";
      }
    }
    if (res != null) {
      tempResolved = null;
      notifyAll();
      return res;
    }
    
    tempProvider = new HashMap<String, ExportPkg>();
    tempRequired = new HashMap<RequireBundle, BundlePackages>();
    tempBlackList = new HashSet<ExportPkg>();
    tempWires = new ArrayList<BundleWireImpl>();
    try {
      final String breq = checkBundleRequirements(bg);
      if (breq == null) {
        String br = checkRequireBundle(bg);
        if (br == null) {
          List<ImportPkg> failed = resolvePackages(pkgs);
          if (failed.size() == 0) {
            registerNewWires();
            registerNewProviders(bg.bundle);
            res = null;
          } else {
            StringBuffer r = new StringBuffer(
                "missing package(s) or can not resolve all of the them: ");
            final Iterator<ImportPkg> mi = failed.iterator();
            r.append(mi.next().pkgString());
            while (mi.hasNext()) {
              r.append(", ");
              r.append(mi.next().pkgString());
            }
            res = r.toString();
          }
        } else {
          res = "Failed to resolve required bundle or host: " + br;
        }
      } else {
        res = "Failed to fulfill requirement: " + breq;
      }
    } finally {
      tempResolved = null;
      tempProvider = null;
      tempRequired = null;
      tempBlackList = null;
      tempWires = null;
      notifyAll();
    }
    if (framework.debug.resolver) {
      framework.debug.println("resolve: Done for " + bg);
    }
    return res;
  }


  private boolean addTempResolved(BundleGeneration bg) throws BundleException {
    if (framework.resolverHooks.filterResolvable(bg)) {
      tempResolved.add(bg);
      return true;
    }
    return false;
  }


  /**
   * Get Pkg object for named package.
   *
   * @param pkg Package name.
   * @return Pkg that represents the package, null if no such package.
   */
  Pkg getPkg(String pkg) {
    return packages.get(pkg);
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
  synchronized TreeSet<Bundle> getZombieAffected(Bundle[] bundles) {
    // set of affected bundles will be in start-level/bundle-id order
    final TreeSet<Bundle> affected = new TreeSet<Bundle>(new Comparator<Bundle>() {
      public int compare(Bundle b1, Bundle b2) {
        int dif = ((BundleImpl) b1).getStartLevel() - ((BundleImpl) b2).getStartLevel();
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
      if (framework.debug.resolver) {
        framework.debug.println("getZombieAffected: check - null");
      }
      framework.bundles.getRemovalPendingBundles(affected);
      framework.bundles.getUnattachedBundles(affected);
    } else {
      for (final Bundle bundle : bundles) {
        final BundleImpl tmp = (BundleImpl)bundle;
        if (tmp != null) {
          if (framework.debug.resolver) {
            framework.debug.println("getZombieAffected: check - " + bundle);
          }
          affected.add(tmp);
        }
      }
    }
    closure(affected);
    return affected;
  }


  synchronized void closure(Set<Bundle> bundles) {
    final ArrayList<Bundle> moreBundles = new ArrayList<Bundle>(bundles);
    for (int i = 0; i < moreBundles.size(); i++) {
      final BundleImpl b = (BundleImpl) moreBundles.get(i);
      for (final Iterator<ExportPkg> j = b.getExports(); j.hasNext();) {
        final ExportPkg ep = j.next();
        if (ep.pkg != null && ep.pkg.providers.contains(ep)) {
          for (final ImportPkg ip : ep.getPackageImporters()) {
            final BundleImpl ib = ip.bpkgs.bg.bundle;
            if (!bundles.contains(ib)) {
              moreBundles.add(ib);
              if (framework.debug.resolver) {
                framework.debug.println("closure: added importing bundle - "
                                        + ib);
              }
              bundles.add(ib);
            }
          }
        }
        for (final Object element : ep.bpkgs.getRequiredBy()) {
          final BundlePackages rbpkgs = (BundlePackages)element;
          final BundleImpl rb = rbpkgs.bg.bundle;
          if (!bundles.contains(rb)) {
            moreBundles.add(rb);
            if (framework.debug.resolver) {
              framework.debug.println("closure: added requiring bundle - "
                                      + rb);
            }
            bundles.add(rb);
          }
        }
      }
      for (BundleGeneration bbg : b.generations) {
        List<BundleWireImpl> bwl = bbg.getCapabilityWires();
        if (bwl != null) {
          for (final BundleWireImpl bcw : bwl) {
            BundleImpl bbr = bcw.getRequirer().bundle;
            if (!bundles.contains(bbr)) {
              moreBundles.add(bbr);
              if (framework.debug.resolver) {
                framework.debug.println("closure: added wired bundle - "
                                        + bbr);
              }
              bundles.add(bbr);
            }
          }
        }
        if (bbg.isFragmentHost()) {
          @SuppressWarnings("unchecked")
          final Vector<BundleGeneration> fix = (Vector<BundleGeneration>)bbg.fragments.clone();
          for (BundleGeneration fbg : fix) {
            if (!bundles.contains(fbg.bundle)) {
              moreBundles.add(fbg.bundle);
              if (framework.debug.resolver) {
                framework.debug.println("closure: added fragment bundle - "
                                        + fbg.bundle);
              }
              bundles.add(fbg.bundle);
            }
          }
        }
        if (bbg.isFragment()) {
          final Set<BundleImpl> hosts = bbg.getResolvedHosts();
          for (BundleImpl hb : hosts) {
            if (!bundles.contains(hb)) {
              moreBundles.add(hb);
              if (framework.debug.resolver) {
                framework.debug.println("closure: added fragment host bundle - " + hb);
              }
              bundles.add(hb);
            }
          }
        }
      }
    }
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
    if (framework.debug.resolver) {
      framework.debug.println("backTrackUses: check - " + ip.pkgString());
    }
    if (tempBackTracked.contains(ip.bpkgs)) {
      return false;
    }
    tempBackTracked.add(ip.bpkgs);
    final Iterator<ExportPkg> i = getPackagesProvidedBy(ip.bpkgs).iterator();
    if (i.hasNext()) {
      do {
        final ExportPkg ep = i.next();
        boolean foundUses = false;
        for (final Object element : ep.pkg.importers) {
          final ImportPkg iip = (ImportPkg)element;
          if (iip.provider == ep) {
            if (backTrackUses(iip)) {
              foundUses = true;
            }
          }
        }
        if (!foundUses) {
          checkUses(ep.uses, ep, ep.bpkgs);
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
  private void markAsZombies(List<ExportPkg> e1, Iterator<ExportPkg> e2) {
    for (final ExportPkg exportPkg : e1) {
      exportPkg.zombie = true;
    }
    while (e2.hasNext()) {
      e2.next().zombie = true;
    }
  }


  /**
   * Get packages provide by specified BundlePackages.
   *
   * @param bpkgs BundlePackages exporting packages.
   * @return List of packages exported by BundlePackages.
   */
  private Collection<ExportPkg> getPackagesProvidedBy(BundlePackages bpkgs) {
    final ArrayList<ExportPkg> res = new ArrayList<ExportPkg>();
    for (final Iterator<ExportPkg> i = bpkgs.getExports(); i.hasNext();) {
      final ExportPkg ep = i.next();
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
   * @throws BundleException Resolver hook throw an exception.
   */
  private List<ImportPkg> resolvePackages(Iterator<ImportPkg> pkgs) throws BundleException {
    final ArrayList<ImportPkg> res = new ArrayList<ImportPkg>();

    while (pkgs.hasNext()) {
      ExportPkg provider = null;
      final ImportPkg ip = pkgs.next();
      if (ip.provider != null) {
        framework.listeners.frameworkError(ip.bpkgs.bg.bundle,
                                           new Exception("resolvePackages: InternalError1!"));
      }
      if (framework.debug.resolver) {
        framework.debug.println("resolvePackages: check - " + ip.pkgString());
      }
      List<ExportPkg> possibleProvider = new LinkedList<ExportPkg>();
      for (ExportPkg ep : ip.pkg.exporters) {
        if (ip.checkAttributes(ep)) {
          if (ip.bpkgs == ep.bpkgs || ip.checkPermission(ep)) {
            possibleProvider.add(ep);
          }
        }
      }
      framework.resolverHooks.filterMatches((BundleRequirement)ip,
                                            (Collection<? extends BundleCapability>) possibleProvider);
      provider = tempProvider.get(ip.name);
      if (provider != null) {
        if (framework.debug.resolver) {
          framework.debug.println("resolvePackages: " + ip.name
              + " - has temporary provider - "
                                  + provider);
        }
        if (!possibleProvider.contains(provider)) {
          if (framework.debug.resolver) {
            framework.debug.println("resolvePackages: " + ip.name
                                    + " - provider not used, rejected by constraints or resolver hooks - "
                                    + provider);
          }
          provider = null;
        } else if (provider.zombie && provider.bpkgs.bg.bundle.state == Bundle.UNINSTALLED) {
          if (framework.debug.resolver) {
            framework.debug.println("resolvePackages: " + ip.name +
                                    " - provider not used since it is an uninstalled zombie - "
                                    + provider);
          }
          provider = null;
        }
      } else {
        for (ExportPkg ep : ip.pkg.providers) {
          if (!possibleProvider.contains(ep)) {
            continue;
          }
          tempBlackListChecks++;
          if (tempBlackList.contains(ep)) {
            possibleProvider.remove(ep);
            tempBlackListHits++;
            continue;
          }
          if (ep.zombie) {
            continue;
          }
          final HashMap<String, ExportPkg> oldTempProvider = tempProviderClone();
          if (checkUses(ep.uses, ep, ep.bpkgs)) {
            provider = ep;
            break;
          } else {
            tempProvider = oldTempProvider;
            tempBlackList.add(ep);
            possibleProvider.remove(ep);
          }
        }
        if (provider == null) {
          provider = pickProvider(ip, possibleProvider);
        }
        if (provider != null) {
          tempProvider.put(ip.pkg.pkg, provider);
        }
      }
      if (provider == null) {
        if (ip.mustBeResolved()) {
          res.add(ip);
        } else {
          if (framework.debug.resolver) {
            framework.debug.println("resolvePackages: Ok, no provider for optional " + ip.name);
          }
        }
      }
    }
    return res;
  }


  @SuppressWarnings("unchecked")
  private HashMap<String, ExportPkg> tempProviderClone() {
    return (HashMap<String, ExportPkg>)tempProvider.clone();
  }


  /**
   * Find a provider for specified package.
   *
   * @param pkg Package to find provider for.
   * @return Package entry that can provide.
   * @throws BundleException Resolver hook throw an exception.
   */
  private ExportPkg pickProvider(ImportPkg ip, List<ExportPkg> possibleProvider)
      throws BundleException {
    if (framework.debug.resolver) {
      framework.debug.println("pickProvider: for - " + ip);
    }
    boolean zombieExists = false;
    for (Iterator<ExportPkg> i = possibleProvider.iterator(); i.hasNext();) {
      ExportPkg ep = i.next();
      tempBlackListChecks++;
      if (tempBlackList.contains(ep)) {
        tempBlackListHits++;
        i.remove();
        continue;
      }
      // TODO optimize this, check could already be checked.
      if (!ip.checkPermission(ep)) {
        if (ip.bpkgs == ep.bpkgs) {
          if (framework.debug.resolver) {
            framework.debug.println("pickProvider: internal wire ok for - " + ep);
          }
          ip.internalOk = ep;
        }
        if (framework.debug.resolver) {
          framework.debug.println("pickProvider: no import permission for - " + ep);
        }
        i.remove();
        continue;
      }
      if (!ep.checkPermission()) {
        if (framework.debug.resolver) {
          framework.debug.println("pickProvider: no export permission for - " + ep);
        }
        i.remove();
        continue;
      }
      if (ep.bpkgs.bg.bundle.state != Bundle.INSTALLED) {
        final HashMap<String, ExportPkg> oldTempProvider = tempProviderClone();
        if (checkUses(ep.uses, ep, ep.bpkgs)) {
          if (framework.debug.resolver) {
            framework.debug.println("pickProvider: " + ip +
                                    " - got resolved provider - " + ep);
          }
          return ep;
        } else {
          tempProvider = oldTempProvider;
          tempBlackList.add(ep);
          i.remove();
          continue;
        }
      }
      if (ep.zombie) {
        zombieExists  = true;
      }
    }
    if (zombieExists) {
      for (Iterator<ExportPkg> iep = possibleProvider.iterator(); iep.hasNext();) {
        final ExportPkg ep = iep.next();
        if (tempResolved.contains(ep.bpkgs.bg)) {
          if (framework.debug.resolver) {
            framework.debug.println("pickProvider: " + ip + " - got temp provider - " + ep);
          }
          return ep;
        } else if (ep.zombie) {
          final HashMap<String, ExportPkg> oldTempProvider = tempProviderClone();
          if (checkUses(ep.uses, ep, ep.bpkgs)) {
            if (framework.debug.resolver) {
              framework.debug.println("pickProvider: " + ip +
                                      " - got zombie provider - " + ep);
            }
            return ep;
          }
          tempProvider = oldTempProvider;
          tempBlackList.add(ep);
          iep.remove();
        }
      }
    }
    for (final ExportPkg ep : possibleProvider) {
      if (framework.debug.resolver) {
        framework.debug.println("pickProvider: check possible provider - " + ep);
      }
      if (checkResolve(ep.bpkgs.bg, ep)) {
        if (framework.debug.resolver) {
          framework.debug.println("pickProvider: " + ip + " - got provider - " + ep);
        }
        return ep;
      }
    }
    if (framework.debug.resolver) {
      framework.debug.println("pickProvider: " + ip + " - found no provider");
    }
    return null;
  }


  /**
   * Check if a bundle can be resolved. If resolvable, then the objects
   * tempResolved, tempProvider and tempBlackList are updated. Bundle must be in
   * installed state.
   *
   * @param bg BundleGeneration to be checked.
   * @param ep ExportPkg that must be exported by bundle.
   * @return true if resolvable otherwise false.
   * @throws BundleException Resolver hook throw an exception.
   */
  private boolean checkResolve(BundleGeneration bg, ExportPkg ep) throws BundleException {
    if (tempResolved.contains(bg)) {
      return true;
    }
    if (checkBundleSingleton(bg) == null) {
      @SuppressWarnings("unchecked")
      final HashSet<BundleGeneration> oldTempResolved = (HashSet<BundleGeneration>)tempResolved.clone();
      if (!addTempResolved(bg)) {
        return false;
      }
      final HashMap<String, ExportPkg> oldTempProvider = tempProviderClone();
      @SuppressWarnings("unchecked")
      final HashMap<RequireBundle, BundlePackages> oldTempRequired = (HashMap<RequireBundle, BundlePackages>)tempRequired.clone();
      @SuppressWarnings("unchecked")
      final HashSet<ExportPkg> oldTempBlackList = (HashSet<ExportPkg>)tempBlackList.clone();
      final int oldTempWiresSize = tempWires.size();
      if (ep != null) {
        tempProvider.put(ep.pkg.pkg, ep);
      }
      final String breq = checkBundleRequirements(bg);
      if (breq == null) {
        if (checkRequireBundle(bg) == null) {
          final List<ImportPkg> r = resolvePackages(bg.bpkgs.getImports());
          if (r.size() == 0) {
            return true;
          }
        }
      }
      tempResolved = oldTempResolved;
      tempProvider = oldTempProvider;
      tempRequired = oldTempRequired;
      tempBlackList = oldTempBlackList;
      tempWires.subList(oldTempWiresSize, tempWires.size()).clear();
    }
    return false;
  }


  /**
   * Check that the packages that this provider uses do not collied with
   * previous selections. If a bundle doesn't have a uses directive we check all
   * currently imported packages. This is then applied recursively.
   *
   * @param pkg Exported package to check
   * @return True if we checked all packages without collision.
   */
  private boolean checkUses(Set<String> uses, BundleCapability bc, BundlePackages bpkgs) {
    if (framework.debug.resolver) {
      framework.debug.println("checkUses: check if packages used by " + bc + " is okay.");
    }
    if (framework.debug.resolver) {
      framework.debug.println("checkUses: provider with bpkgs=" + bpkgs);
    }
    final Iterator<ImportPkg> i = bpkgs.getActiveImports();
    if (i != null) {
      final ArrayList<ExportPkg> checkList = new ArrayList<ExportPkg>();
      while (i.hasNext()) {
        final ImportPkg ip = i.next();
        if (uses != null && !uses.contains(ip.pkg.pkg)) {
          continue;
        }
        final ExportPkg ep = tempProvider.get(ip.pkg.pkg);
        if (framework.debug.resolver) {
          framework.debug.println("checkUses: check import, " + ip +
                                  " with provider, " + ip.provider);
        }
        if (ep == null) {
          tempProvider.put(ip.pkg.pkg, ip.provider);
          checkList.add(ip.provider);
        } else if (ep != ip.provider) {
          if (framework.debug.resolver) {
            framework.debug.println("checkUses: mismatch in providers for, " + ip.pkg.pkg);
          }
          return false;
        }
      }
      for (final ExportPkg exportPkg : checkList) {
        if (!checkUses(exportPkg.uses, exportPkg, exportPkg.bpkgs)) {
          return false;
        }
      }
    }
    if (framework.debug.resolver) {
      framework.debug.println("checkUses: " + bc + " is okay.");
    }
    return true;
  }


  /**
   * Check that the bundle specified can be resolved without violating any
   * singleton requirements.
   *
   * @param b Bundle to check, must be in INSTALLED state
   * @return Bundle blocking resolve, otherwise null.
   * @throws BundleException Resolver hook throw an exception.
   */
  private BundleGeneration checkBundleSingleton(BundleGeneration bg) throws BundleException {
    if (bg.symbolicName != null && bg.singleton) {
      if (framework.debug.resolver) {
        framework.debug.println("checkBundleSingleton: check singleton bundle " + bg);
      }
      final List<BundleGeneration> bl = framework.bundles.getBundleGenerations(bg.symbolicName);
      if (bl.size() > 1) {
        if (framework.resolverHooks.hasHooks()) {
          final BundleCapability bc = bg.getBundleCapability();
          Collection<BundleCapability> candidates = new LinkedList<BundleCapability>();
          List<BundleNameVersionCapability> active = new ArrayList<BundleNameVersionCapability>(bl.size());
          for (final BundleGeneration bg2 : bl) {
            if (bg2.singleton) {
              if (bg2 != bg) {
                BundleNameVersionCapability bc2 = bg2.getBundleCapability();
                if (bc2 != null) {
                  if (bg2.bpkgs.isActive()) {
                    active.add(bc2);
                  } else {
                    candidates.add(bc2);
                  }
                }
              }
            }
          }
          if (!active.isEmpty()) {
            for (BundleNameVersionCapability abc : active) {
              Collection<BundleCapability> c = new LinkedList<BundleCapability>(candidates);
              c.add(bc);
              framework.resolverHooks.filterSingletonCollisions(abc, c);
              if (c.contains(bc)) {
                return abc.gen;
              } else {
                candidates.removeAll(c);
              }
            }
          }
          if (!candidates.isEmpty()) {
            framework.resolverHooks.filterSingletonCollisions(bc, candidates);
            for (final BundleCapability bc2 : candidates) {
              BundleGeneration bg2 = ((BundleRevisionImpl)bc2.getRevision()).gen;
              if (tempResolved.contains(bg2)) {
                // TODO add to blacklist to avoid resolve tries?!
                if (framework.debug.resolver) {
                  framework.debug.println("checkBundleSingleton: Reject because of bundle: "
                                          + bg2.bundle);
               }
                return bg2;
              }
            }
          }
        } else {
          for (final BundleGeneration bg2 : bl) {
            if (bg != bg2 && bg2.singleton && (bg2.bpkgs.isActive() || tempResolved.contains(bg2))) {
              if (framework.debug.resolver) {
                framework.debug.println("checkBundleSingleton: Reject because of bundle: "
                                        + bg2.bundle);
              }
              return bg2;
            }
          }
        }
      }
    }
    return null;
  }


  /**
   * Check that the bundle specified can resolve all its Require-Capability
   * constraints.
   * 
   * @param b Bundle to check, must be in INSTALLED state
   * @return Capability not full name of bundle blocking resolve, otherwise null.
   * @throws BundleException Resolver hook throw an exception.
   */
  private String checkBundleRequirements(BundleGeneration bg) throws BundleException {
    for (Entry<String, List<BundleRequirementImpl>> e : bg.getOtherRequirements().entrySet()) {
      String namespace = e.getKey(); 
      for (BundleRequirementImpl br : e.getValue()) {
        if (!br.shouldResolve()) {
          continue;
        }
        if (framework.debug.resolver) {
          framework.debug.println("checkBundleRequirements: Check requirement: " + br);
        }
        final boolean reqPerm = framework.perm.hasRequirePermission(br);
        List<BundleCapabilityImpl> bcs = capabilities.getCapabilities(namespace);
        BundleWireImpl found = null;
        if (bcs != null) {
          List<BundleCapabilityImpl> mbcs = new LinkedList<BundleCapabilityImpl>();
          for (BundleCapabilityImpl bc : bcs) {
            if (br.matches(bc) && bc.checkPermission() && (reqPerm || framework.perm.hasRequirePermission(br, bc))) {
              mbcs.add(bc);
            }
          }
          if (framework.resolverHooks.hasHooks()) {
            framework.resolverHooks.filterMatches(br, (Collection<? extends BundleCapability>) mbcs);
          }
          List<BundleCapabilityImpl> matches = new ArrayList<BundleCapabilityImpl>();
          int n_active = 0;
          for (BundleCapabilityImpl bc : mbcs) {
            BundleGeneration bcbg = bc.getBundleGeneration();
            if (bcbg.isCurrent()) {
              // Select active or soon active first
              if (bcbg.bpkgs.isActive() || tempResolved.contains(bcbg)) {
                matches.add(0, bc);
                n_active++;
                if (framework.debug.resolver) {
                  framework.debug.println("checkBundleRequirements: Found active capability: " + bc);
                }
              } else {
                matches.add(n_active, bc);
                if (framework.debug.resolver) {
                  framework.debug.println("checkBundleRequirements: Found unresolved capability: " + bc);
                }
              }
            } else {
              // Select zombies last
              matches.add(bc);
              if (framework.debug.resolver) {
                framework.debug.println("checkBundleRequirements: Found zombie capability: " + bc);
              }
            }
          }
          for (BundleCapabilityImpl bc : matches) {
            BundleGeneration bcbg = bc.getBundleGeneration();
            if (!tempResolved.contains(bcbg)) {
              if (bcbg.bundle.state == Bundle.INSTALLED) {
                if (!checkResolve(bcbg, null)) {
                  continue;
                }
              } else {
                // Check uses
                Set<String> uses = bc.getUses();
                if (uses != null && !checkUses(uses, bc, bc.getBundleGeneration().bpkgs)) {
                  continue;
                }
              }
            }
            found = new BundleWireImpl(bc, bcbg, br, bg);
            break;
          }
        }
        if (found != null) {
          tempWires.add(found);
        } else if (!br.isOptional()) {
          return "Failed to satisfy: " + br;
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
   * @throws BundleException Resolver hook throw an exception.
   */
  private String checkRequireBundle(BundleGeneration bg) throws BundleException {
    // NYI! More speed?
    final Iterator<RequireBundle> i = bg.bpkgs.getRequire();
    if (i.hasNext()) {
      if (framework.debug.resolver) {
        framework.debug.println("checkRequireBundle: check requiring bundle " + bg);
      }
      if (!framework.perm.okRequireBundlePerm(bg.bundle)) {
        return bg.symbolicName;
      }
      final HashMap<RequireBundle, BundlePackages> res = new HashMap<RequireBundle, BundlePackages>();
      do {
        final RequireBundle br = i.next();
        BundleGeneration ok = null;
        List<BundleNameVersionCapability> possibleProvider = new LinkedList<BundleNameVersionCapability>();
        for (BundleGeneration bg2 : framework.bundles.getBundles(br.name, br.bundleRange)) {
          possibleProvider.add(new BundleNameVersionCapability(bg2, BundleRevision.BUNDLE_NAMESPACE));
        }
        framework.resolverHooks.filterMatches((BundleRequirement)br,
                                              (Collection<? extends BundleCapability>) possibleProvider);
        for (final BundleNameVersionCapability bc : possibleProvider) {
          final BundleGeneration bg2 = bc.gen;
          if (!bg2.bsnAttrMatch(br.attributes)) {
            continue;
          }
          if (tempResolved.contains(bg2)) {
            ok = bg2;
            break;
          } else if (bg2.bpkgs.isActive()) {
            final HashMap<String, ExportPkg> oldTempProvider = tempProviderClone();
            ok = bg2;
            for (final Iterator<ExportPkg> epi = bg2.bpkgs.getExports(); epi.hasNext();) {
              final ExportPkg ep = epi.next();
              if (!checkUses(ep.uses, ep, ep.bpkgs)) {
                tempProvider = oldTempProvider;
                tempBlackList.add(ep);
                ok = null;
                break;
              }
            }
            if (ok != null) {
              break;
            }
          } else if (bg2.bundle.state == Bundle.INSTALLED &&
                     framework.perm.okProvideBundlePerm(bg2.bundle) &&
                     checkResolve(bg2, null)) {
            ok = bg2;
            break;
          }
        }
        if (ok != null) {
          if (framework.debug.resolver) {
            framework.debug.println("checkRequireBundle: added required bundle " + ok);
          }
          res.put(br, ok.bpkgs);
        } else if (br.resolution == Constants.RESOLUTION_MANDATORY) {
          if (framework.debug.resolver) {
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
    for (final ExportPkg ep : tempProvider.values()) {
      ep.pkg.addProvider(ep);
    }
    for (final Entry<RequireBundle, BundlePackages> entry : tempRequired.entrySet()) {
      final BundlePackages bpkgs = entry.getValue();
      final RequireBundle br = entry.getKey();
      br.bpkgs = bpkgs;
      bpkgs.addRequiredBy(br.requestor);
      if (framework.debug.resolver) {
        framework.debug.println("registerNewProviders: '"
                                + Constants.REQUIRE_BUNDLE + ": " + br.name
                                + "' for " + br.requestor.bg.bundle.getBundleId()
                                + " bound to (id=" + bpkgs.bg.bundle.getBundleId()
                                + ",gen=" + bpkgs.bg.generation + ")");
      }
      if (br.visibility == Constants.VISIBILITY_REEXPORT) {
        // Create necessary re-export entries
        for (final Iterator<ExportPkg> be = bpkgs.getExports(); be.hasNext();) {
          final ExportPkg ep = be.next();
          br.requestor.checkReExport(ep);
          if (framework.debug.resolver) {
            framework.debug.println("registerNewProviders: "
                                    + br.requestor.bg.bundle.getBundleId()
                                    + " reexports package " + ep.name);
          }
        }
      }
    }
    List<BundleImpl> resolve = new ArrayList<BundleImpl>(tempResolved.size());
    for (final BundleGeneration bg : tempResolved) {
      if (!bg.bpkgs.isActive()) {
        for (final Iterator<ImportPkg> bi = bg.bpkgs.getImports(); bi.hasNext();) {
          final ImportPkg ip = bi.next();
          final ExportPkg ep = tempProvider.get(ip.name);
          if (ep == null) {
            // There could be an internal connection, that should export a
            // package
            if (ip.internalOk != null) {
              if (ip.internalOk.pkg.providers.isEmpty() &&
                  ip.internalOk.checkPermission()) {
                ip.internalOk.pkg.addProvider(ip.internalOk);
                if (framework.debug.resolver) {
                  framework.debug.println("registerNewProviders: exported internal wire, "
                                          + ip + " -> " + ip.internalOk);
                }
              } else {
                if (framework.debug.resolver) {
                  framework.debug.println("registerNewProviders: internal wire, "
                                          + ip + " -> " + ip.internalOk);
                }
              }
            }
          } else {
            // TODO! This check is already done, we should cache result
            if (ip.checkAttributes(ep) && ip.checkPermission(ep)) {
              ip.provider = ep;
            } else {
              // Check if got a missmatching internal wire.
              if (ip.internalOk != null) {
                if (ip.internalOk == ep) {
                  if (framework.debug.resolver) {
                    framework.debug.println("registerNewProviders: internal wire, " + ip + ", "
                        + ep);
                  }
                } else {
                  // TODO, should we resolve when this happens!?
                  framework.listeners.frameworkError(bg.bundle,
                       new Exception("registerNewProviders: Warning! Internal wire for, " + ip +
                                     ", does not match exported. " + ep));
                }
              }
            }
          }
        }
        if (bg.bundle != bundle) {
          resolve.add(bg.bundle);
        }
      }
    }
    for (final BundleImpl b : resolve) {
      if (b.getUpdatedState(null) == Bundle.INSTALLED) {
        framework.listeners.frameworkError(b,
          new Exception("registerNewProviders: InternalError!"));
      }
    }
  }

  /**
   *
   */
  private void registerNewWires() {
    for (final BundleWireImpl bw : tempWires) {
      ((BundleRequirementImpl)bw.getRequirement()).setWire(bw);
    }
  }

  /**
   * Check if current thread is a bundle thread involved in
   * resolve operation, if so abort to avoid deadlock.
   * @throws BundleException 
   *  
   */
  private void checkThread() throws BundleException {
    Thread t = Thread.currentThread();
    for (BundleGeneration bg : tempResolved) {
      if (bg.bundle.isBundleThread(t)) {
		    throw new BundleException("Can not resolve a bundle inside current BundleListener." +
                                  "Will cause a dead-lock. BID=" + bg.bundle.id);
      }
    }
  }

}
