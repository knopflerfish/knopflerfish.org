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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.osgi.framework.*;


/**
 * Class representing all packages imported and exported.
 *
 * @author Jan Stein
 */
class BundlePackages {

  final BundleImpl bundle;

  final int generation;

  private ClassLoader classLoader = null;

  /* Sorted list of exports */
  private ArrayList /* ExportPkg */ exports = new ArrayList(1);

  /* Sorted list of imports */
  private ArrayList /* ImportPkg */ imports = new ArrayList(1);

  private ArrayList /* String */ dImportPatterns = new ArrayList(1);

  /* List of RequireBundle entries. */
  ArrayList /* RequireBundle */ require;

  /* List of BundlePackages that require us. */
  ArrayList /* BundlePackages */ requiredBy = null;

  /* Sorted list of active imports */
  private ArrayList /* ImportPkg */ okImports = null;

  /* Is our packages registered */
  private boolean registered = false;

  /* Reason we failed to resolve */
  private String failReason = null;

  final static String EMPTY_STRING = "";

  /**
   * Create package entry.
   */
  BundlePackages(BundleImpl b, 
                 int gen,
                 String exportStr, 
                 String importStr, 
                 String dimportStr,
                 String requireStr) {
    this.bundle = b;
    this.generation = gen;

    if(b.getBundleArchive() != null) {
      String fakeString = b.getBundleArchive().getAttribute("fakeheader");
      if(fakeString != null) {
        if(Debug.packages) {
          Debug.println(("Fake bundle #" + b.getBundleId() + ": " + fakeString));
        }
      }
    }
    
    Iterator i = Util.parseEntries(Constants.IMPORT_PACKAGE, importStr, false, true, false);
    while (i.hasNext()) {
      Map e = (Map)i.next();
      Iterator pi = ((List)e.remove("keys")).iterator();
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

    i = Util.parseEntries(Constants.EXPORT_PACKAGE, exportStr, false, true, false);
    while (i.hasNext()) {
      Map e = (Map)i.next();
      Iterator pi = ((List)e.remove("keys")).iterator();
      ExportPkg ep = new ExportPkg((String)pi.next(), e, this);
      for (;;) {
        int ei = Math.abs(Util.binarySearch(exports, epComp, ep) + 1);
        exports.add(ei, ep);
        if (!b.v2Manifest) {
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

    i = Util.parseEntries(Constants.DYNAMICIMPORT_PACKAGE, dimportStr, false, true, false);
    while (i.hasNext()) {
      Map e = (Map)i.next();
      if (e.containsKey(Constants.RESOLUTION_DIRECTIVE)) {
        throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
                                           " entry illegal contains a " +
                                           Constants.RESOLUTION_DIRECTIVE +
                                           " directive.");
      }
      ImportPkg tmpl = null;
      for (Iterator pi = ((List)e.remove("keys")).iterator(); pi.hasNext(); ) {
        String key = (String)pi.next();
        if (key.equals("*")) {
          key = EMPTY_STRING;
        } else if (key.endsWith(".*")) {
          key = key.substring(0, key.length() - 1);
        } else if (key.endsWith(".")) {
          throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
                                             " entry ends with '.': " + key);
        } else if (key.indexOf("*") != - 1) {
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
    i = Util.parseEntries(Constants.REQUIRE_BUNDLE, requireStr, true, true, false);
    if (i.hasNext()) {
      require = new ArrayList();
      do {
        Map e = (Map)i.next();
        require.add(new RequireBundle(this,
                                      (String)e.get("key"),
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
   * Register bundle packages in framework.
   *
   */
  void registerPackages() {
    bundle.framework.packages.registerPackages(exports.iterator(), imports.iterator());
    registered = true;
  }


  /**
   * Unregister bundle packages in framework.
   *
   */
  synchronized boolean unregisterPackages(boolean force) {
    if (registered) {
      List i = okImports != null ? okImports : imports;
      if (bundle.framework.packages.unregisterPackages(exports, i, force)) {
        okImports = null;
        registered = false;
      } else {
        return false;
      }
    }
    return true;
  }


  /**
   * Resolve all the bundles' packages. 
   *
   * @return true if we resolved all packages. If we failed
   *         return false. Reason for fail can be fetched with
   *         getResolveFailReason().
   */
  boolean resolvePackages() {

    if (bundle.framework.permissions != null) {
      failReason = checkPackagePermissions();
      if (failReason != null) {
        return false;
      }
    }
   
    if (bundle.id != 0 &&  // skip the system bundle...
        bundle.attachPolicy != null && 
        (bundle.attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_ALWAYS) ||
         bundle.attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_RESOLVETIME))) {
      
      // retrieve all fragments this bundle host
      List hosting = 
        bundle.framework.bundles.getFragmentBundles(bundle);
      for (Iterator iter = hosting.iterator();
           iter.hasNext(); ) {
        
        BundleImpl fb = (BundleImpl)iter.next();
        if (fb.state != Bundle.UNINSTALLED) {
          System.out.println("attaching " + fb + " to " + bundle);
          bundle.attachFragment(fb);
        }	      
      }
    }

    failReason = bundle.framework.packages.resolve(bundle, imports.iterator());
    if (failReason == null) {
      okImports = (ArrayList)imports.clone();
      return true;
    } else {
      return false;
    }
  }


  /**
   * If bundle package has been resolved look for a BundlePackages
   * that provides the requested package.
   *
   * @param pkg Package name
   * @return BundlePackages exporting the pkg.
   */
  synchronized BundlePackages getProviderBundlePackages(String pkg) {
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
   * Check if we can dynamically import a package. Re-check
   * that we haven't gotten a provider. (Do we need to do that?)
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
    if (checkPackagePermission(pkg, PackagePermission.IMPORT)) {
      for (Iterator i = dImportPatterns.iterator(); i.hasNext(); ) {
	ImportPkg ip = (ImportPkg)i.next();
        if (ip.name == EMPTY_STRING ||
	    (ip.name.endsWith(".") && pkg.startsWith(ip.name)) ||
	    pkg.equals(ip.name)) {
	  ImportPkg nip = new ImportPkg(ip, pkg);
	  ExportPkg ep = bundle.framework.packages.registerDynamicImport(nip);
	  if (ep != null) {
	    nip.provider = ep;
	    okImports.add(-ii - 1, nip);
	    return ep.bpkgs;
	  }
	}
      }
    }
    return null;
  }


  /**
   * Get a list of all BundlePackages that exports package <code>pkg</code>
   * that comes from bundles that we have required, in correct order.
   * Correct order is a depth first search order.
   *
   * @param pkg String with package name we are searching for.
   * @return List of required BundlePackages or null we don't require any bundles.
   */
  ArrayList getRequiredBundlePackages(String pkg) {
    if (require != null) {
      ArrayList res = new ArrayList();
      for (Iterator i = require.iterator(); i.hasNext(); ) {
        RequireBundle rb = (RequireBundle)i.next();
        if (rb.bpkgs != null && rb.bpkgs.getExport(pkg) != null) {
          res.add(rb.bpkgs);
        }
      }
      if (!res.isEmpty()) {
        return res;
      }
    }
    return null;
  }


  /**
   * Check if package needs to be added as re-exported package.
   *
   * @param ep ExportPkg to re-export.
   */
  void checkReExport(ExportPkg ep) {
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
  ExportPkg getExport(String pkg) {
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
    return exports.iterator();
  }
  
  /**
   * Adds an export package
   * @param pkg export to be included
   */
  void addExport(ExportPkg pkg) {
    int ei = Math.abs(Util.binarySearch(exports, epComp, pkg) + 1);
    exports.add(ei, pkg);
  }
  

  /**
   * Removes an export package
   * @param pkg export to be removed.
   */
  void removeExport(ExportPkg pkg) {
    int ei = Util.binarySearch(exports, epComp, pkg);
    exports.remove(ei);
  }

  
  /** 
   * Get a specific import
   * @return an import
   */
  ImportPkg getImport(String pkg) {
    int i = Util.binarySearch(imports, ipFind, pkg);
    if (i >= 0) {
      return (ImportPkg)imports.get(i);
    } else {
      return null;
    }
  }

  /**
   * Adds an imported package
   * @param pkg import to be included
   */ 
  void addImport(ImportPkg pkg) {
    int ii = Util.binarySearch(imports, ipComp, pkg);
    if (ii < 0) {
      imports.add(-ii - 1, pkg);
    } else {
      throw new IllegalArgumentException("Duplicate import definitions for - " + pkg.name);
    }
  }

  /**
   * Removes an imported package
   * @param pkg import to be removed
   */ 
  void removeImport(ImportPkg pkg) {
    int ii = Util.binarySearch(imports, ipComp, pkg);
    imports.remove(ii);
  }



  /**
   * Get an iterator over all static imported packages.
   *
   * @return An Iterator over ImportPkg.
   */
  Iterator getImports() {
    return imports.iterator();
  }


  /**
   * Get an iterator over all active imported packages.
   *
   * @return An Iterator over ImportPkg.
   */
  Iterator getActiveImports() {
    return okImports.iterator();
  }


  /**
   * Get class loader for these packages.
   *
   */
  ClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = bundle.getClassLoader(this);
    }
    return classLoader;
  }


  /**
   * Invalidate class loader for these packages.
   *
   */
  void invalidateClassLoader() {
    classLoader = null;
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
   * Return a string representing this objet
   *
   * @return A message string.
   */
  public String toString() {
    return "BundlePackages(id=" + bundle.id + ",gen=" + generation + ")";
  }

  //
  // Private methods
  //

  /**
   * Check that we have right export and import package permission for the bundle.
   *
   * @return Returns null if we have correct permission for listed package.
   *         Otherwise a string of failed entries.
   */
  private String checkPackagePermissions() {
    String e_res = null;
    for (Iterator i = exports.iterator(); i.hasNext();) {
      ExportPkg p = (ExportPkg)i.next();
      if (!checkPackagePermission(p.name, PackagePermission.EXPORT)) {
        if (e_res != null) {
          e_res = e_res + ", " + p.name;
        } else {
          e_res = "missing export permission for package(s): " + p.name;
          e_res = p.name;
        }
      }
    }
    String i_res = null;
    for (Iterator i = imports.iterator(); i.hasNext();) {
      ImportPkg p = (ImportPkg)i.next();
      if (!checkPackagePermission(p.name, PackagePermission.IMPORT)) {
        if (i_res != null) {
          i_res = i_res + ", " + p.name;
        } else {
          i_res = "missing import permission for package(s): " + p.name;
        }
      }
    }
    if (e_res != null) {
      if (i_res != null) {
        return e_res + "; " + i_res;
      } else {
        return e_res;
      }
    } else {
      return i_res;
    }
  }


  /**
   * Check that we have right package permission for a package.
   *
   * @param pkg Packages to check. 
   * @param perm Package permission action to check against.
   * @return Returns true if we have permission.
   */
  private boolean checkPackagePermission(String pkg, String perm) {
    if (bundle.framework.permissions != null) {
      return bundle.framework.permissions.getPermissionCollection(bundle)
        .implies(new PackagePermission(pkg, perm));
    } else {
      return true;
    }
  }

  
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

}
