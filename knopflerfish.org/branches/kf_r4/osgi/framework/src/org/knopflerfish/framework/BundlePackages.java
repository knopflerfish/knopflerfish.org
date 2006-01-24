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

  /* Sorted list of exports */
  private ArrayList /* ExportPkg */ exports = new ArrayList(1);

  /* Sorted list of imports */
  private ArrayList /* ImportPkg */ imports = new ArrayList(1);

  private ArrayList /* String */ dImportPatterns = new ArrayList(1);

  /* Sorted list of active imports */
  private ArrayList /* ImportPkg */ okImports = null;

  private String failReason = null;

  final static String EMPTY_STRING = "";

  /**
   * Create package entry.
   */
  BundlePackages(BundleImpl b, 
                 String exportStr, 
                 String importStr, 
                 String dimportStr) {
    this.bundle = b;

    if(b.getBundleArchive() != null) {
      String fakeString = b.getBundleArchive().getAttribute("fakeheader");
      if(fakeString != null) {
        if(Debug.packages) {
          Debug.println(("Fake bundle #" + b.getBundleId() + ": " + fakeString));
        }
      }
    }
    
    try {
      Iterator i = Util.parseEntries(Constants.IMPORT_PACKAGE, importStr, true, false);
      while (i.hasNext()) {
	ImportPkg p = new ImportPkg((Map)i.next(), bundle);
	int ii = Util.binarySearch(imports, ipComp, p);
	if (ii < 0) {
	  imports.add(-ii - 1, p);
	} else {
	  throw new IllegalArgumentException("Duplicate import definitions for - " + p.name);
	}
      }
    } catch (IllegalArgumentException e) {
      b.framework.listeners.frameworkError(b, e);
    }

    try {
      Iterator i = Util.parseEntries(Constants.EXPORT_PACKAGE, exportStr, true, false);
      while (i.hasNext()) {
	ExportPkg p = new ExportPkg((Map)i.next(), bundle);
	int ei = Util.binarySearch(exports, epComp, p);
	if (ei < 0) {
	  exports.add(-ei - 1, p);
	} else {
	  throw new IllegalArgumentException("Duplicate export definitions for - " + p.name);
	}
	if (!b.v2Manifest) {
	  ImportPkg ip = new ImportPkg(p);
	  int ii = Util.binarySearch(imports, ipComp, ip);
	  if (ii < 0) {
	    imports.add(-ii - 1, ip);
	  }
	}
      }
    } catch (IllegalArgumentException e) {
      b.framework.listeners.frameworkError(b, e);
    }

    try {
      Iterator i = Util.parseEntries(Constants.DYNAMICIMPORT_PACKAGE, dimportStr, true, false);
      while (i.hasNext()) {
	Map e = (Map)i.next();
	String key = (String)e.get("key");
	if (key.equals("*")) {
	  e.put("key", EMPTY_STRING);
	} else if (key.endsWith(".*")) {
	  e.put("key", key.substring(0, key.length() - 1));
	} else if (key.endsWith(".")) {
	  throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
					     " entry ends with '.': " + key);
	} else if (key.indexOf("*") != - 1) {
	  throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
					     " entry contains a '*': " + key);
	}
	if (e.containsKey(Constants.RESOLUTION_DIRECTIVE)) {
	  throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
					     " entry illegal contains a " +
					     Constants.RESOLUTION_DIRECTIVE +
					     " directive.");
	}
	if (e.containsKey(Constants.PACKAGE_SPECIFICATION_VERSION)) {
	  throw new IllegalArgumentException(Constants.DYNAMICIMPORT_PACKAGE +
					     " entry illegal contains a " +
					     Constants.PACKAGE_SPECIFICATION_VERSION +
					     " directive.");
	}
	dImportPatterns.add(new ImportPkg(e, bundle));
      }
    } catch (IllegalArgumentException e) {
      b.framework.listeners.frameworkError(b, e);
    }
  }


  /**
   * Register bundle packages in framework.
   *
   */
  void registerPackages() {
    bundle.framework.packages.registerPackages(exports.iterator(), imports.iterator());
  }


  /**
   * Unregister bundle packages in framework.
   *
   */
  synchronized boolean unregisterPackages(boolean force) {
    Iterator i = (okImports != null ? okImports : imports).iterator();
    if (bundle.framework.packages.unregisterPackages(exports.iterator(), i, force)) {
      okImports = null;
      return true;
    } else {
      return false;
    }
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
    List m = bundle.framework.packages.checkResolve(bundle, imports.iterator());
    if (m != null) {
      StringBuffer r = new StringBuffer("missing package(s) or can not resolve all of the them: ");
      Iterator mi = m.iterator();
      r.append(((ImportPkg)mi.next()).pkgString());
      while (mi.hasNext()) {
        r.append(", ");
        r.append(((ImportPkg)mi.next()).pkgString());
      }
      failReason = r.toString();
      return false;
    } else {
      failReason = null;
      okImports = (ArrayList)imports.clone();
      return true;
    }
  }


  /**
   * If bundle package has been resolved look for a bundle
   * that provides the requested package.
   *
   * @param pkg Package name
   * @return Bundle exporting
   */
  synchronized BundleImpl getProviderBundle(String pkg) {
    if (okImports == null) {
      return null;
    }
    int ii = Util.binarySearch(okImports, ipFind, pkg);
    if (ii >= 0) {
      return ((ImportPkg)okImports.get(ii)).provider.bundle;
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
  synchronized BundleImpl getDynamicProviderBundle(String pkg) {
    if (okImports == null) {
      return null;
    }
    int ii = Util.binarySearch(okImports, ipFind, pkg);
    if (ii >= 0) {
      return ((ImportPkg)okImports.get(ii)).provider.bundle;
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
	    return ep.bundle;
	  }
	}
      }
    }
    return null;
  }


  /**
   * Get an iterator over all exported packages.
   *
   * @return An Iterator over ExportPkg.
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
   * Return a string with a reason for why resolve failed.
   *
   * @return A error message string.
   */
  synchronized String getResolveFailReason() {
    return failReason;
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
