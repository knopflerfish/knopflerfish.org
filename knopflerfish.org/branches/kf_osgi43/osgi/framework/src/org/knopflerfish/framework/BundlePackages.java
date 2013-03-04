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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;

import org.knopflerfish.framework.Util.Comparator;
import org.knopflerfish.framework.Util.HeaderEntry;

/**
 * Class representing all packages imported and exported.
 *
 * @author Jan Stein
 * @author Mats-Ola Persson
 * @author Gunnar Ekolin
 */
class BundlePackages {

  final BundleGeneration bg;

  /* Sorted list of exports */
  private final ArrayList<ExportPkg> exports = new ArrayList<ExportPkg>(1);

  /* Sorted list of declared imports */
  private final ArrayList<ImportPkg> imports = new ArrayList<ImportPkg>(1);

  /* Sorted list of declared dynamic imports */
  private final ArrayList<ImportPkg> dImportPatterns = new ArrayList<ImportPkg>(1);

  private final Map<String, List<BundleCapabilityImpl>> capabilities;

  private TreeMap<BundleGeneration, BundlePackages> fragments = null;

  private ArrayList<RequireBundle> require;

  private ArrayList<BundlePackages> requiredBy = null;

  /* Sorted list of active imports */
  private ArrayList<ImportPkg> okImports = null;

  /* Is our packages registered */
  private boolean registered = false;

  /* Reason we failed to resolve */
  private String failReason = null;

  final static String EMPTY_STRING = "";


  /**
   * Create package entry.
   */
  BundlePackages(BundleGeneration bg) {
    this.bg = bg;
    final BundleArchive ba = bg.archive;

    for (final HeaderEntry he : Util.parseManifestHeader(Constants.IMPORT_PACKAGE, ba
        .getAttribute(Constants.IMPORT_PACKAGE), false, true, false)) {
      final Iterator<String> pi = he.getKeys().iterator();
      ImportPkg ip = new ImportPkg(pi.next(), he, this, false);
      for (;;) {
        final int ii = Util.binarySearch(imports, ipComp, ip);
        if (ii < 0) {
          imports.add(-ii - 1, ip);
        } else {
          throw new IllegalArgumentException("Duplicate import definitions for - " + ip.name);
        }
        if (pi.hasNext()) {
          ip = new ImportPkg(ip, pi.next());
        } else {
          break;
        }
      }
    }

    for (final HeaderEntry he : Util
        .parseManifestHeader(Constants.EXPORT_PACKAGE,
                             ba.getAttribute(Constants.EXPORT_PACKAGE), false,
                             true, false)) {
      final List<String> keys = he.getKeys();
      final Iterator<String> pi = keys.iterator();
      ExportPkg ep = new ExportPkg(pi.next(), he, this);
      for (;;) {
        final int ei = Math.abs(Util.binarySearch(exports, epComp, ep) + 1);
        exports.add(ei, ep);
        if (!bg.v2Manifest) {
          final ImportPkg ip = new ImportPkg(ep);
          final int ii = Util.binarySearch(imports, ipComp, ip);
          if (ii < 0) {
            imports.add(-ii - 1, ip);
          }
        }
        if (pi.hasNext()) {
          ep = new ExportPkg(ep, pi.next());
        } else {
          break;
        }
      }
    }

    parseDynamicImports(ba.getAttribute(Constants.DYNAMICIMPORT_PACKAGE));

    final List<HeaderEntry> hes = Util
        .parseManifestHeader(Constants.REQUIRE_BUNDLE,
                             ba.getAttribute(Constants.REQUIRE_BUNDLE), true,
                             true, false);
    if (!hes.isEmpty()) {
      require = new ArrayList<RequireBundle>();
      for (final HeaderEntry he : hes) {
        require.add(new RequireBundle(this, he));
      }
    } else {
      require = null;
    }
    capabilities = bg.getDeclaredCapabilities();
  }


  /**
   * Create package entry used by system bundle.
   */
  BundlePackages(BundleGeneration bg, String exportString) {
    this.bg = bg;

    for (final HeaderEntry he : Util.parseManifestHeader(Constants.EXPORT_PACKAGE,
                                                   exportString, false, true,
                                                   false)) {
      final List<String> keys = he.getKeys();
      final Iterator<String> pi = keys.iterator();
      ExportPkg ep = new ExportPkg(pi.next(), he, this);
      for (;;) {
        final int ei = Math.abs(Util.binarySearch(exports, epComp, ep) + 1);
        exports.add(ei, ep);
        if (pi.hasNext()) {
          ep = new ExportPkg(ep, pi.next());
        } else {
          break;
        }
      }
    }
    require = null;
    capabilities = bg.getDeclaredCapabilities();
  }


  /**
   * Create package entry used to clone fragment bundle.
   */
  BundlePackages(BundlePackages host, BundlePackages frag, boolean noNew) {
    this.bg = host.bg;
    /*
     * make sure that the fragment's bundle does not conflict with this bundle's
     * (see 3.1.4 r4-core)
     */
    for (final Iterator<ImportPkg> iiter = frag.getImports(); iiter.hasNext();) {
      final ImportPkg fip = iiter.next();
      final ImportPkg ip = host.getImport(fip.name);

      if (ip != null) {
        if (!fip.intersect(ip)) {
          throw new IllegalStateException(
              "Host bundle import package and fragment bundle " +
                  "import package doesn't intersect so a resolve isn't possible.");
        }
      } else if (noNew){
        throw new IllegalStateException("Resolve host bundle package would " +
                                        "be shadow by new fragment import.");
      }
      imports.add(new ImportPkg(fip, this));
    }

    if (frag.require != null) {
      require = new ArrayList<RequireBundle>();
      for (final RequireBundle fragReq : frag.require) {
        boolean match = false;

        if (require != null) {
          // check for conflicts
          for (final RequireBundle req : host.require) {
            if (fragReq.name.equals(req.name)) {
              if (fragReq.overlap(req)) {
                match = true;
              } else {
                throw new IllegalStateException(
                    "Fragment bundle required bundle doesn't completely " +
                        "overlap required bundle in host bundle.");
              }
            }
          }
        }
        if (!match) {
          if (bg.bundle.state != Bundle.INSTALLED) {
            throw new IllegalStateException("Can not attach a fragment with new required " +
                                            "bundles to a resolved host");
          }
          require.add(new RequireBundle(fragReq, this));
        }
      }
    } else {
      require = null;
    }
    for (final Iterator<ExportPkg> eiter = frag.getExports(); eiter.hasNext();) {
      final ExportPkg fep = eiter.next();
      final ExportPkg hep = getExport(fep.name);
      if (fep.pkgEquals(hep)) {
        continue;
      }
      exports.add(new ExportPkg(fep, this));
    }
    capabilities = new HashMap<String, List<BundleCapabilityImpl>>();

    for (Entry<String, List<BundleCapabilityImpl>> e : frag.bg.getDeclaredCapabilities().entrySet()) {
      List<BundleCapabilityImpl> l = new ArrayList<BundleCapabilityImpl>();
      for (BundleCapabilityImpl bc : e.getValue()) {
        l.add(new BundleCapabilityImpl(bc, bg));
      }
      capabilities.put(e.getKey(), l);
    }
  }


   List<BundleCapability> getBundleCapabilities() {
    // TODO Auto-generated method stub
    return null;
  }


  /**
   * Register bundle packages in framework.
   *
   */
  void registerPackages() {
    bg.bundle.fwCtx.packages.registerCapabilities(capabilities, exports.iterator(), imports.iterator());
    registered = true;
  }


  /**
   * Unregister bundle packages in framework.
   *
   */
  synchronized boolean unregisterPackages(boolean force) {
    if (registered) {
      if (bg.bundle.fwCtx.packages.unregisterCapabilities(capabilities, getExports(), getImports(), force)) {
        okImports = null;
        registered = false;
        unRequireBundles();
        detachFragments();
      } else {
        return false;
      }
    }
    return true;
  }


  /**
   * Resolve all the bundles' packages.
   *
   * @return true if we resolved all packages. If we failed return false. Reason
   *         for fail can be fetched with getResolveFailReason().
   */
  boolean resolvePackages() {
    failReason = bg.bundle.fwCtx.packages.resolve(bg, getImports());
    if (failReason == null) {
      // TBD, Perhaps we should use complete size here
      okImports = new ArrayList<ImportPkg>(imports.size());
      for (final Iterator<ImportPkg> i = getImports(); i.hasNext();) {
        final ImportPkg ip = i.next();
        if (ip.provider != null) { // <=> optional import with unresolved
                                   // provider
          okImports.add(ip);
        }
      }
      return true;
    } else {
      return false;
    }
  }


  /**
   * Return a string with a reason for why resolve failed.
   *
   * @return A error message string.
   */
  String getResolveFailReason() {
    return failReason;
  }


  /**
   * If bundle package has been resolved look for a BundlePackages that provides
   * the requested package.
   *
   * @param pkg Package name
   * @return BundlePackages exporting the pkg.
   */
  synchronized BundlePackages getProviderBundlePackages(String pkg) {
    if (bg.bundle instanceof SystemBundle) {
      return isExported(pkg) ? this : null;
    }
    if (okImports == null) {
      return null;
    }
    final int ii = Util.binarySearch(okImports, ipFind, pkg);
    if (ii >= 0) {
      return okImports.get(ii).provider.bpkgs;
    }
    return null;
  }


  /**
   * Check if we can dynamically import a package. Re-check that we haven't
   * gotten a provider. (Do we need to do that?)
   *
   * @param pkg Package name
   * @return Bundle exporting
   */
  synchronized BundlePackages getDynamicProviderBundlePackages(String pkg) {
    if (okImports == null) {
      return null;
    }
    final int ii = Util.binarySearch(okImports, ipFind, pkg);
    if (ii >= 0) {
      return okImports.get(ii).provider.bpkgs;
    }
    for (final ImportPkg ip : dImportPatterns) {
      if (ip.name == EMPTY_STRING ||
          (ip.name.endsWith(".") && pkg.startsWith(ip.name)) ||
          pkg.equals(ip.name)) {
        final ImportPkg nip = new ImportPkg(ip, pkg);
        final ExportPkg ep = bg.bundle.fwCtx.packages.registerDynamicImport(nip);
        if (ep != null) {
          nip.provider = ep;
          okImports.add(-ii - 1, nip);
          return ep.bpkgs;
        }
      }
    }
    return null;
  }


  /**
   * Get all RequiredBundle for this BundlePackages.
   *
   * @return Iterator of RequireBundle.
   */
  Iterator<RequireBundle> getRequire() {
    if (fragments != null) {
      synchronized (fragments) {
        final ArrayList<Iterator<RequireBundle>> iters = new ArrayList<Iterator<RequireBundle>>(fragments.size() + 1);
        if (require != null) {
          iters.add(require.iterator());
        }
        for (final BundlePackages bundlePackages : fragments.values()) {
          iters.add(bundlePackages.getRequire());
        }
        return new IteratorIterator<RequireBundle>(iters);
      }
    } else if (require != null) {
      return require.iterator();
    } else {
      @SuppressWarnings("unchecked")
      final
      Iterator<RequireBundle>res = Collections.EMPTY_LIST.iterator();
      return res;
    }
  }


  /**
   * Get a list of all BundleGenerations that exports package
   * <code>pkg</code> that comes from bundles that we have required,
   * in correct order. Correct order is a depth first search order.
   *
   * @param pkg String with package name we are searching for, if null get all.
   * @return List of required BundleGenerations or null if we don't
   *         require any bundles.
   */
  ArrayList<BundleGeneration> getRequiredBundleGenerations(String pkg) {
    ArrayList<BundleGeneration> res = null;
    for (final Iterator<RequireBundle> i = getRequire(); i.hasNext(); ) {
      final RequireBundle rb = i.next();
      if (rb.bpkgs != null && rb.bpkgs.isExported(pkg)) {
        if (res == null) {
          res = new ArrayList<BundleGeneration>(2);
        }
        res.add(rb.bpkgs.bg);
      }
    }
    return res;
  }


  /**
   * Check if this BundlePackages is required by another Bundle.
   *
   * @return True if is required
   */
  void addRequiredBy(BundlePackages r) {
    if (requiredBy == null) {
      requiredBy = new ArrayList<BundlePackages>();
    }
    requiredBy.add(r);
  }


  /**
   * Check if this BundlePackages is required by another Bundle.
   *
   * @return True if is required
   */
  boolean isRequired() {
    return requiredBy != null && !requiredBy.isEmpty();
  }


  /**
   * Check if this BundlePackages is required by another Bundle.
   *
   * @return True if is required
   */
  boolean isRequiredBy(BundlePackages cbp) {
    return requiredBy != null && requiredBy.contains(cbp);
  }


  /**
   * Get a list of all BundlePackages that requires the exported packages that
   * comes from the bundle owning this object.
   *
   * @return List of required BundlePackages
   */
  List<BundlePackages> getRequiredBy() {
    final List<BundlePackages> res = new ArrayList<BundlePackages>();
    if (requiredBy != null) {
      synchronized (requiredBy) {
        res.addAll(requiredBy);
      }
      if (fragments != null) {
        synchronized (fragments) {
          for (final BundlePackages bundlePackages : fragments.values()) {
            final List<BundlePackages> fl = bundlePackages.getRequiredBy();
            if (fl != null) {
              res.addAll(fl);
            }
          }
        }
      }
    }
    return res;
  }


  /**
   * Check if package needs to be added as re-exported package.
   *
   * @param ep ExportPkg to re-export.
   */
  void checkReExport(ExportPkg ep) {
    // NYI. Rework this solution and include fragments
    final int i = Util.binarySearch(exports, epFind, ep.name);
    if (i < 0) {
      final ExportPkg nep = new ExportPkg(ep, this);
      exports.add(-i - 1, nep);
      // Perhaps we should avoid this shortcut and go through Packages.
      ep.pkg.addExporter(nep);
    }
  }


  /**
   * Get ExportPkg for exported package.
   *
   * @return ExportPkg entry or null if package is not exported.
   */
  private ExportPkg getExport(String pkg) {
    final int i = Util.binarySearch(exports, epFind, pkg);
    if (i >= 0) {
      return exports.get(i);
    } else {
      return null;
    }
  }


  /**
   * Get an iterator over all exported packages sorted
   * according to epComp.
   *
   * @return An Iterator over ExportPkg.
   */
  Iterator<ExportPkg> getExports() {
    if (fragments != null) {
      synchronized (fragments) {
        final ArrayList<Iterator<ExportPkg>> iters
          = new ArrayList<Iterator<ExportPkg>>(fragments.size() + 1);
        iters.add(exports.iterator());
        for (final BundlePackages bundlePackages : fragments.values()) {
          iters.add(bundlePackages.getExports());
        }
        return new IteratorIteratorSorted<ExportPkg>(iters, epComp);
      }
    } else {
      return exports.iterator();
    }
  }


  /**
   * Get an iterator over all exported packages with specific name.
   *
   * @return An Iterator over ExportPkg.
   */
  Iterator<ExportPkg> getExports(String pkg) {
    final ArrayList<ExportPkg> res = new ArrayList<ExportPkg>(2);
    ExportPkg ep = getExport(pkg);
    if (ep != null) {
      res.add(ep);
    }
    if (fragments != null) {
      synchronized (fragments) {
        for (final BundlePackages bundlePackages : fragments.values()) {
          ep = bundlePackages.getExport(pkg);
          if (ep != null) {
            res.add(ep);
          }
        }
      }
    }
    return res.isEmpty() ? null : res.iterator();
  }


  /**
   * Check if this packaged is exported
   */
  boolean isExported(String pkg) {
    if (getExport(pkg) != null) {
      return true;
    }
    if (fragments != null) {
      synchronized (fragments) {
        for (final BundlePackages bundlePackages : fragments.values()) {
          if (bundlePackages.getExport(pkg) != null) {
            return true;
          }
        }
      }
    }
    return false;
  }


  /**
   * Get an iterator over all static imported packages sorted
   * according to ipComp.
   *
   * @return An Iterator over ImportPkg.
   */
  Iterator<ImportPkg> getImports() {
    if (fragments != null) {
      synchronized (fragments) {
        final ArrayList<Iterator<ImportPkg>> iters = new ArrayList<Iterator<ImportPkg>>(fragments.size() + 1);
        iters.add(imports.iterator());
        for (final BundlePackages bundlePackages : fragments.values()) {
          iters.add(bundlePackages.getImports());
        }
        return new IteratorIteratorSorted<ImportPkg>(iters, ipComp);
      }
    } else {
      return imports.iterator();
    }
  }


  /**
   * Get an iterator over all active imported packages.
   *
   * @return An Iterator over ImportPkg.
   */
  Iterator<ImportPkg> getActiveImports() {
    if (okImports != null) {
      return okImports.iterator();
    } else {
      // This is fragment BP, use host
      return bg.bpkgs.getActiveImports();
    }
  }


  /**
   * Get the list package requirements derived from the Import-Package header.
   * The bundle requirement objects for imported packages in the list has the
   * same order as the packages in the Import-Package header.
   *
   * @return all defined import package requirements for this bundle revision.
   */
  List<BundleRequirement> getDeclaredPackageRequirements() {
    final TreeSet<ImportPkg> ipCreationOrder = new TreeSet<ImportPkg>(imports);
    ipCreationOrder.addAll(dImportPatterns);

    return new ArrayList<BundleRequirement>(ipCreationOrder);
  }


  /**
   * Get the list of package capabilities derived from the Export-Package header.
   *
   * The bundle capability objects in the list has the same order as the packages
   * in the Export-Package header.
   *
   * @return ordered list with bundle capabilities for packages.
   */
  public List<BundleCapability> getDeclaredPackageCapabilities()
  {
    final TreeSet<ExportPkg> epCreationOrder = new TreeSet<ExportPkg>(exports);

    return new ArrayList<BundleCapability>(epCreationOrder);
  }

  /**
   * Get the list bundle requirements derived from the Require-Bundle header.
   * The bundle requirement objects for required bundles in the list has the
   * same order as the bundles in the Require-Bundle header.
   *
   * @return all defined require bundle requirements for this bundle revision.
   */
  List<BundleRequirement> getDeclaredBundleRequirements() {
    final List<BundleRequirement> res = new ArrayList<BundleRequirement>();

    if (require!=null) {
      final TreeSet<RequireBundle> rbCreationOrder
        = new TreeSet<RequireBundle>(require);
      res.addAll(rbCreationOrder);
    }
    return res;
  }

  /**
   * Get class loader for these packages.
   *
   * @return ClassLoader handling these packages.
   */
  ClassLoader getClassLoader() {
    return bg.getClassLoader();
  }


  /**
   * Is these packages registered in the Packages object.
   *
   * @return True if packages are registered otherwise false.
   */
  boolean isRegistered() {
    return registered;
  }


  /**
   * Attach a fragment bundle packages.
   *
   * @param fbpkgs The BundlePackages of the fragment to be attached.
   * @return null if okay, otherwise a String with fail reason.
   */
  String attachFragment(BundlePackages fbpkgs) {
    // TODO, should we lock this?!
    final boolean resolvedHost = okImports != null;
    final BundlePackages nfbpkgs = new BundlePackages(this, fbpkgs, resolvedHost);
    nfbpkgs.registerPackages();
    if (resolvedHost) {
      failReason = bg.bundle.fwCtx.packages.resolve(bg, nfbpkgs.getImports());
      if (failReason == null) {
        for (final Iterator<ImportPkg> i = nfbpkgs.getImports(); i.hasNext();) {
          final ImportPkg ip = i.next();
          if (ip.provider != null) { // <=> optional import with unresolved
                                     // provider
            final int ii = Util.binarySearch(okImports, ipComp, ip);
            if (ii < 0) {
              okImports.add(-ii - 1, ip);
            }
          }
        }
      } else {
        nfbpkgs.unregisterPackages(true);
        return failReason;
      }
    }
    if (fragments == null) {
      fragments = new TreeMap<BundleGeneration, BundlePackages>();
    }
    fragments.put(fbpkgs.bg, nfbpkgs);
    return null;
  }


  /**
   * An attached fragment is now a zombie since it have been updated or
   * uninstalled. Mark all packages exported by this host as zombies, since
   * their contents may have changed.
   *
   * @param fb The fragment bundle that have been updated or uninstalled.
   */
  void fragmentIsZombie(BundleImpl fb) {
    if (null != exports) {
      if (bg.bundle.fwCtx.debug.packages) {
        bg.bundle.fwCtx.debug.println("Marking all packages exported by host bundle(id="
                                      + bg.bundle.id + ",gen=" + bg.generation
                                      + ") as zombies since the attached fragment (id="
                                      + fb.getBundleId() + ") was updated/uninstalled.");
      }
      for (final ExportPkg exportPkg : exports) {
        exportPkg.zombie = true;
      }
    }
  }


  /**
   * Detach a fragment bundle's packages.
   *
   * I.e., unregister and remove the fragments import / exports from the set of
   * packages that are imported / exported by this bundle packages.
   *
   * If this bundle packages is resolved, do nothing since in that case must not
   * change the set of imports and exports.
   *
   * @param fb The fragment bundle to detach.
   */
  void detachFragment(BundleGeneration fbg) {
    synchronized (fragments) {
      detachFragment(fbg, true);
    }
  }


  /**
   * Sets these packages registered in the Packages object.
   *
   * @return True if packages are registered otherwise false.
   */
  void unregister() {
    registered = false;
    unRequireBundles();
  }


  /**
   * Return a string representing this objet
   *
   * @return A message string.
   */
  @Override
  public String toString() {
    return "BundlePackages(id=" + bg.bundle.id + ",gen=" + bg.generation + ")";
  }


  boolean isActive() {
    return okImports != null;
  }

  //
  // Private methods
  //

  /**
   * Get a specific import
   *
   * @return an import
   */
  private ImportPkg getImport(String pkg) {
    final int i = Util.binarySearch(imports, ipFind, pkg);
    if (i >= 0) {
      return imports.get(i);
    } else {
      return null;
    }
  }


  /**
   * Remove this bundle packages from the requiredBy list in the wired required
   * bundle package.
   */
  private void unRequireBundles() {
    if (require != null) {
      for (final RequireBundle req : require) {
        if (null != req.bpkgs && null != req.bpkgs.requiredBy) {
          req.bpkgs.requiredBy.remove(this);
          // TODO check if we can reset bpkgs to make checking easier
        }
      }
    }
  }


  /**
   * Detach all remaining fragments.
   */
  private void detachFragments() {
    if (fragments != null) {
      synchronized (fragments) {
        while (!fragments.isEmpty()) {
          detachFragment(fragments.lastKey(), false);
        }
      }
    }
  }


  /**
   * Detach a fragment bundle's packages.
   *
   * I.e., unregister and remove the fragments import / exports from the set of
   * packages that are imported / exported by this bundle packages.
   *
   * If this bundle packages is resolved, do nothing since in that case must not
   * change the set of imports and exports.
   *
   * Note! Must be called with fragments locked.
   *
   * @param fb The fragment bundle to detach.
   * @param unregisterPkg Unregister the imports and exports of the specified
   *          fragment.
   */
  private void detachFragment(final BundleGeneration fbg, final boolean unregisterPkg) {
    if (null == okImports) {
      final BundlePackages fbpkgs = fragments.remove(fbg);
      if (fbpkgs != null) {
        if (unregisterPkg) {
          fbpkgs.unregisterPackages(true);
        } else {
          fbpkgs.unregister();
        }
      }
    }
  }

  /**
   * Parse the dynamic import attribute
   */

  void parseDynamicImports(final String s) {
    for (final HeaderEntry he : Util
        .parseManifestHeader(Constants.DYNAMICIMPORT_PACKAGE, s, false, true,
                             false)) {
      if (he.getDirectives().containsKey(Constants.RESOLUTION_DIRECTIVE)) {
        throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
                                           " entry illegal contains a " +
                                           Constants.RESOLUTION_DIRECTIVE +
                                           " directive.");
      }
      ImportPkg tmpl = null;
      for (String key : he.getKeys()) {
        if (key.equals("*")) {
          key = EMPTY_STRING;
        } else if (key.endsWith(".*")) {
          key = key.substring(0, key.length() - 1);
        } else if (key.endsWith(".")) {
          throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
                                             " entry ends with '.': " + key);
        } else if (key.indexOf("*") != -1) {
          throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
                                           " entry contains a '*': " + key);
        }
        if (tmpl != null) {
          dImportPatterns.add(new ImportPkg(tmpl, key));
        } else {
          tmpl = new ImportPkg(key, he, this, true);
          dImportPatterns.add(tmpl);
        }
      }
    }
  }

  //
  // Pkg Comparators
  //

  static final Comparator<ExportPkg, ExportPkg> epComp
    = new Util.Comparator<ExportPkg, ExportPkg>() {
    /**
     * Compare two ExportPkg objects on package name.
     *
     * @param a Object to compare.
     * @param b Object to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException if object is not a ExportPkg object.
     */
    public int compare(ExportPkg a, ExportPkg b) throws ClassCastException {
      return a.name.compareTo(b.name);
    }
  };

  static final Util.Comparator<ExportPkg,String> epFind
    = new Util.Comparator<ExportPkg,String>() {
    /**
     * Compare package name of ExportPkg object with String object.
     *
     * @param a ExportPkg object to compare.
     * @param b String object to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException if object is not a ExportPkg object.
     */
    public int compare(ExportPkg a, String b)
    {
      return a.name.compareTo(b);
    }
  };

  static final Util.Comparator<ImportPkg,ImportPkg> ipComp
    = new Util.Comparator<ImportPkg, ImportPkg>() {
    /**
     * Compare two ImportPkg objects by package name.
     *
     * @param a Object to compare.
     * @param b Object to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException if object is not a ImportPkg object.
     */
    public int compare(ImportPkg a, ImportPkg b) throws ClassCastException {
      return a.name.compareTo(b.name);
    }
  };

  static final Util.Comparator<ImportPkg,String> ipFind
    = new Util.Comparator<ImportPkg,String>() {
    /**
     * Compare package name in ImportPkg object with a package name as a String.
     *
     * @param a Candidate ImportPkg object to compare.
     * @param b Package name of the ImportPkg to find.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     */
    public int compare(ImportPkg a, String b) {
      return a.name.compareTo(b);
    }
  };


}
