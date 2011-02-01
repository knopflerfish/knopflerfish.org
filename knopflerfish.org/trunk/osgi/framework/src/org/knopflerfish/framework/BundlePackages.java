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
 * Class representing all packages imported and exported.
 * 
 * @author Jan Stein
 * @author Mats-Ola Persson
 * @author Gunnar Ekolin
 */
class BundlePackages {

  final BundleGeneration bg;

  /* Sorted list of exports */
  private ArrayList /* ExportPkg */exports = new ArrayList(1);

  /* Sorted list of imports */
  private ArrayList /* ImportPkg */imports = new ArrayList(1);

  private ArrayList /* String */dImportPatterns = new ArrayList(1);

  private TreeMap /* BundleGeneration -> BundlePackages */fragments = null;

  /* List of RequireBundle entries. */
  private ArrayList /* RequireBundle */require;

  /* List of BundlePackages that require us. */
  private ArrayList /* BundlePackages */requiredBy = null;

  /* Sorted list of active imports */
  private ArrayList /* ImportPkg */okImports = null;

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

    Iterator i = Util.parseEntries(Constants.IMPORT_PACKAGE,
                                   ba.getAttribute(Constants.IMPORT_PACKAGE),
                                   false, true, false);
    while (i.hasNext()) {
      Map e = (Map)i.next();
      Iterator pi = ((List)e.remove("$keys")).iterator();
      ImportPkg ip = new ImportPkg((String)pi.next(), e, this);
      for (;;) {
        int ii = Util.binarySearch(imports, ipComp, ip);
        if (ii < 0) {
          imports.add(-ii - 1, ip);
        } else {
          throw new IllegalArgumentException("Duplicate import definitions for - " + ip.name);
        }
        if (pi.hasNext()) {
          ip = new ImportPkg(ip, (String)pi.next());
        } else {
          break;
        }
      }
    }

    i = Util.parseEntries(Constants.EXPORT_PACKAGE,
                          ba.getAttribute(Constants.EXPORT_PACKAGE),
                          false, true, false);
    while (i.hasNext()) {
      Map e = (Map)i.next();
      Iterator pi = ((List)e.remove("$keys")).iterator();
      ExportPkg ep = new ExportPkg((String)pi.next(), e, this);
      for (;;) {
        int ei = Math.abs(Util.binarySearch(exports, epComp, ep) + 1);
        exports.add(ei, ep);
        if (!bg.v2Manifest) {
          ImportPkg ip = new ImportPkg(ep);
          int ii = Util.binarySearch(imports, ipComp, ip);
          if (ii < 0) {
            imports.add(-ii - 1, ip);
          }
        }
        if (pi.hasNext()) {
          ep = new ExportPkg(ep, (String)pi.next());
        } else {
          break;
        }
      }
    }

    i = Util.parseEntries(Constants.DYNAMICIMPORT_PACKAGE,
                          ba.getAttribute(Constants.DYNAMICIMPORT_PACKAGE),
                          false, true, false);
    while (i.hasNext()) {
      Map e = (Map)i.next();
      if (e.containsKey(Constants.RESOLUTION_DIRECTIVE)) {
        throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
                                           " entry illegal contains a " +
                                           Constants.RESOLUTION_DIRECTIVE +
                                           " directive.");
      }
      ImportPkg tmpl = null;
      for (Iterator pi = ((List)e.remove("$keys")).iterator(); pi.hasNext();) {
        String key = (String)pi.next();
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
          tmpl = new ImportPkg(key, e, this);
          dImportPatterns.add(tmpl);
        }
      }
    }
    i = Util.parseEntries(Constants.REQUIRE_BUNDLE,
                          ba.getAttribute(Constants.REQUIRE_BUNDLE),
                          true, true, false);
    if (i.hasNext()) {
      require = new ArrayList();
      do {
        Map e = (Map)i.next();
        require.add(new RequireBundle(this,
                                      (String)e.get("$key"),
                                      (String)e.get(Constants.VISIBILITY_DIRECTIVE),
                                      (String)e.get(Constants.RESOLUTION_DIRECTIVE),
                                      (String)e.get(Constants.BUNDLE_VERSION_ATTRIBUTE)));
        // NYI warn about unknown directives?
      } while (i.hasNext());
    } else {
      require = null;
    }

  }


  /**
   * Create package entry used by system bundle.
   */
  BundlePackages(BundleGeneration bg, String exportString) {
    this.bg = bg;

    Iterator i = Util.parseEntries(Constants.EXPORT_PACKAGE,
                                   exportString, false, true, false);
    while (i.hasNext()) {
      Map e = (Map)i.next();
      Iterator pi = ((List)e.remove("$keys")).iterator();
      ExportPkg ep = new ExportPkg((String)pi.next(), e, this);
      for (;;) {
        int ei = Math.abs(Util.binarySearch(exports, epComp, ep) + 1);
        exports.add(ei, ep);
        if (!bg.v2Manifest) {
          ImportPkg ip = new ImportPkg(ep);
          int ii = Util.binarySearch(imports, ipComp, ip);
          if (ii < 0) {
            imports.add(-ii - 1, ip);
          }
        }
        if (pi.hasNext()) {
          ep = new ExportPkg(ep, (String)pi.next());
        } else {
          break;
        }
      }
    }
    require = null;
  }


  /**
   * Create package entry used to clone fragment bundle.
   */
  BundlePackages(BundlePackages host, BundlePackages frag) {
    this.bg = host.bg;

    /*
     * make sure that the fragment's bundle does not conflict with this bundle's
     * (see 3.1.4 r4-core)
     */
    for (Iterator iiter = frag.getImports(); iiter.hasNext();) {
      ImportPkg fip = (ImportPkg)iiter.next();
      ImportPkg ip = host.getImport(fip.name);

      if (ip != null) {
        if (!fip.intersect(ip)) {
          throw new IllegalStateException(
              "Host bundle import package and fragment bundle " +
                  "import package doesn't intersect so a resolve isn't possible.");
        }
      }
      imports.add(new ImportPkg(fip, this));
    }

    if (frag.require != null) {
      require = new ArrayList();
      for (Iterator iter = frag.require.iterator(); iter.hasNext();) {
        RequireBundle fragReq = (RequireBundle)iter.next();
        boolean match = false;

        if (require != null) {
          // check for conflicts
          for (Iterator iter2 = host.require.iterator(); iter2.hasNext();) {
            RequireBundle req = (RequireBundle)iter2.next();
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
    for (Iterator eiter = frag.getExports(); eiter.hasNext();) {
      ExportPkg fep = (ExportPkg)eiter.next();
      ExportPkg hep = getExport(fep.name);
      if (fep.pkgEquals(hep)) {
        continue;
      }
      exports.add(new ExportPkg(fep, this));
    }
  }


  /**
   * Register bundle packages in framework.
   * 
   */
  void registerPackages() {
    bg.bundle.fwCtx.packages.registerPackages(exports.iterator(), imports.iterator());
    registered = true;
  }


  /**
   * Unregister bundle packages in framework.
   * 
   */
  synchronized boolean unregisterPackages(boolean force) {
    if (registered) {
      if (bg.bundle.fwCtx.packages.unregisterPackages(getExports(), getImports(), force)) {
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
    failReason = bg.bundle.fwCtx.packages.resolve(bg.bundle, getImports());
    if (failReason == null) {
      // TBD, Perhaps we should use complete size here
      okImports = new ArrayList(imports.size());
      for (Iterator i = getImports(); i.hasNext();) {
        ImportPkg ip = (ImportPkg)i.next();
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
    int ii = Util.binarySearch(okImports, ipFind, pkg);
    if (ii >= 0) {
      return ((ImportPkg)okImports.get(ii)).provider.bpkgs;
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
    int ii = Util.binarySearch(okImports, ipFind, pkg);
    if (ii >= 0) {
      return ((ImportPkg)okImports.get(ii)).provider.bpkgs;
    }
    for (Iterator i = dImportPatterns.iterator(); i.hasNext();) {
      ImportPkg ip = (ImportPkg)i.next();
      if (ip.name == EMPTY_STRING ||
          (ip.name.endsWith(".") && pkg.startsWith(ip.name)) ||
          pkg.equals(ip.name)) {
        ImportPkg nip = new ImportPkg(ip, pkg);
        ExportPkg ep = bg.bundle.fwCtx.packages.registerDynamicImport(nip);
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
   * Get a list of all BundlePackages that exports package <code>pkg</code> that
   * comes from bundles that we have required, in correct order. Correct order
   * is a depth first search order.
   * 
   * @param pkg String with package name we are searching for, if null get all.
   * @return List of required BundlePackages or null we don't require any
   *         bundles.
   */
  Iterator getRequire() {
    if (fragments != null) {
      synchronized (fragments) {
        ArrayList iters = new ArrayList(fragments.size() + 1);
        if (require != null) {
          iters.add(require.iterator());
        }
        for (Iterator i = fragments.values().iterator(); i.hasNext();) {
          Iterator fi = ((BundlePackages)i.next()).getRequire();
          if (fi != null) {
            iters.add(fi);
          }
        }
        return iters.isEmpty() ? null : new IteratorIterator(iters);
      }
    } else if (require != null) {
      return require.iterator();
    } else {
      return null;
    }
  }


  /**
   * Get a list of all BundlePackages that exports package <code>pkg</code> that
   * comes from bundles that we have required, in correct order. Correct order
   * is a depth first search order.
   * 
   * @param pkg String with package name we are searching for, if null get all.
   * @return List of required BundlePackages or null we don't require any
   *         bundles.
   */
  ArrayList getRequiredBundlePackages(String pkg) {
    Iterator i = getRequire();
    if (i != null) {
      ArrayList res = new ArrayList(2);
      do {
        RequireBundle rb = (RequireBundle)i.next();
        if (rb.bpkgs != null && rb.bpkgs.isExported(pkg)) {
          res.add(rb.bpkgs);
        }
      } while (i.hasNext());
      return res.isEmpty() ? null : res;
    }
    return null;
  }


  /**
   * Check if this BundlePackages is required by another Bundle.
   * 
   * @return True if is required
   */
  void addRequiredBy(BundlePackages r) {
    if (requiredBy == null) {
      requiredBy = new ArrayList();
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
   * Get a list of all BundlePackages that requires the exported packages that
   * comes from the bundle owning this object.
   * 
   * @return List of required BundlePackages
   */
  List getRequiredBy() {
    if (requiredBy != null) {
      if (fragments != null) {
        ArrayList res = new ArrayList();
        synchronized (requiredBy) {
          res.addAll(requiredBy);
        }
        synchronized (fragments) {
          for (Iterator i = fragments.values().iterator(); i.hasNext();) {
            List fl = ((BundlePackages)i.next()).getRequiredBy();
            if (fl != null) {
              res.addAll(fl);
            }
          }
        }
        return res;
      } else {
        synchronized (requiredBy) {
          return (List)requiredBy.clone();
        }
      }
    }
    return Collections.EMPTY_LIST;
  }


  /**
   * Check if package needs to be added as re-exported package.
   * 
   * @param ep ExportPkg to re-export.
   */
  void checkReExport(ExportPkg ep) {
    // NYI. Rework this solution and include fragments
    int i = Util.binarySearch(exports, epFind, ep.name);
    if (i < 0) {
      ExportPkg nep = new ExportPkg(ep, this);
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
    int i = Util.binarySearch(exports, epFind, pkg);
    if (i >= 0) {
      return (ExportPkg)exports.get(i);
    } else {
      return null;
    }
  }


  /**
   * Get an iterator over all exported packages.
   * 
   * @return An Iterator over ExportPkg.
   */
  Iterator getExports() {
    if (fragments != null) {
      synchronized (fragments) {
        ArrayList iters = new ArrayList(fragments.size() + 1);
        iters.add(exports.iterator());
        for (Iterator i = fragments.values().iterator(); i.hasNext();) {
          iters.add(((BundlePackages)i.next()).getExports());
        }
        return new IteratorIterator(iters);
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
  Iterator getExports(String pkg) {
    ArrayList res = new ArrayList(2);
    ExportPkg ep = getExport(pkg);
    if (ep != null) {
      res.add(ep);
    }
    if (fragments != null) {
      synchronized (fragments) {
        for (Iterator i = fragments.values().iterator(); i.hasNext();) {
          ep = ((BundlePackages)i.next()).getExport(pkg);
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
        for (Iterator i = fragments.values().iterator(); i.hasNext();) {
          if (((BundlePackages)i.next()).getExport(pkg) != null) {
            return true;
          }
        }
      }
    }
    return false;
  }


  /**
   * Get an iterator over all static imported packages.
   * 
   * @return An Iterator over ImportPkg.
   */
  Iterator getImports() {
    if (fragments != null) {
      synchronized (fragments) {
        ArrayList iters = new ArrayList(fragments.size() + 1);
        iters.add(imports.iterator());
        for (Iterator i = fragments.values().iterator(); i.hasNext();) {
          iters.add(((BundlePackages)i.next()).getImports());
        }
        return new IteratorIterator(iters);
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
  Iterator getActiveImports() {
    if (okImports != null) {
      return okImports.iterator();
    } else {
      // This is fragment BP, use host
      return bg.bpkgs.getActiveImports();
    }
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
    // TBD, should we lock this?!
    BundlePackages nfbpkgs = new BundlePackages(this, fbpkgs);
    nfbpkgs.registerPackages();
    if (okImports != null) {
      failReason = bg.bundle.fwCtx.packages.resolve(bg.bundle, nfbpkgs.getImports());
      if (failReason == null) {
        for (Iterator i = nfbpkgs.getImports(); i.hasNext();) {
          ImportPkg ip = (ImportPkg)i.next();
          if (ip.provider != null) { // <=> optional import with unresolved
                                     // provider
            okImports.add(ip);
          }
        }
      } else {
        nfbpkgs.unregisterPackages(true);
        return failReason;
      }
    }
    if (fragments == null) {
      fragments = new TreeMap();
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
      for (Iterator eiter = exports.iterator(); eiter.hasNext();) {
        ((ExportPkg)eiter.next()).zombie = true;
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
  public String toString() {
    return "BundlePackages(id=" + bg.bundle.id + ",gen=" + bg.generation + ")";
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
    int i = Util.binarySearch(imports, ipFind, pkg);
    if (i >= 0) {
      return (ImportPkg)imports.get(i);
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
      for (Iterator iter = require.iterator(); iter.hasNext();) {
        RequireBundle req = (RequireBundle)iter.next();
        if (null != req.bpkgs && null != req.bpkgs.requiredBy) {
          req.bpkgs.requiredBy.remove(this);
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
          detachFragment((BundleGeneration)fragments.lastKey(), false);
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
      BundlePackages fbpkgs = (BundlePackages)fragments.remove(fbg);
      if (fbpkgs != null) {
        if (unregisterPkg) {
          fbpkgs.unregisterPackages(true);
        } else {
          fbpkgs.unregister();
        }
      }
    }
  }

  //
  // Pkg Comparators
  //

  static final Util.Comparator epComp = new Util.Comparator() {
    /**
     * Name compare two ExportPkg objects.
     * 
     * @param oa Object to compare.
     * @param ob Object to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException if object is not a ExportPkg object.
     */
    public int compare(Object oa, Object ob) throws ClassCastException {
      ExportPkg a = (ExportPkg)oa;
      ExportPkg b = (ExportPkg)ob;
      return a.name.compareTo(b.name);
    }
  };

  static final Util.Comparator epFind = new Util.Comparator() {
    /**
     * Name compare ExportPkg object with String object.
     * 
     * @param oa ExportPkg object to compare.
     * @param ob String object to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException if object is not a ExportPkg object.
     */
    public int compare(Object oa, Object ob) throws ClassCastException {
      ExportPkg a = (ExportPkg)oa;
      String b = (String)ob;
      return a.name.compareTo(b);
    }
  };

  static final Util.Comparator ipComp = new Util.Comparator() {
    /**
     * Name compare two ImportPkg objects.
     * 
     * @param oa Object to compare.
     * @param ob Object to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException if object is not a ImportPkg object.
     */
    public int compare(Object oa, Object ob) throws ClassCastException {
      ImportPkg a = (ImportPkg)oa;
      ImportPkg b = (ImportPkg)ob;
      return a.name.compareTo(b.name);
    }
  };

  static final Util.Comparator ipFind = new Util.Comparator() {
    /**
     * Name compare ImportPkg object with String object.
     * 
     * @param oa ImportPkg object to compare.
     * @param ob String object to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException if object is not a ImportPkg object.
     */
    public int compare(Object oa, Object ob) throws ClassCastException {
      ImportPkg a = (ImportPkg)oa;
      String b = (String)ob;
      return a.name.compareTo(b);
    }
  };

}
