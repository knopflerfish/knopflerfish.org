/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

import java.util.Set;
import java.util.Dictionary;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;


import org.osgi.framework.*;


/**
 * Class representing all packages imported and exported.
 */
class BundlePackages {

  final BundleImpl bundle;

  private ArrayList /* PkgEntry */ exports = new ArrayList(1);

  private ArrayList /* PkgEntry */ imports = new ArrayList(1);

  private ArrayList /* String */ dImportPatterns = new ArrayList(1);

  private HashMap /* String -> PkgEntry */ okImports = null;

  private String failReason = null;

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
      Iterator i = Util.parseEntries(Constants.EXPORT_PACKAGE, exportStr, true);
      while (i.hasNext()) {
	Map e = (Map)i.next();
	PkgEntry p = new PkgEntry((String)e.get("key"), 
				  (String)e.get(Constants.PACKAGE_SPECIFICATION_VERSION),
				  bundle);
	exports.add(p);
	imports.add(new PkgEntry(p));
      }
    } catch (IllegalArgumentException e) {
      b.framework.listeners.frameworkError(b, e);
    }
    try {
      Iterator i = Util.parseEntries(Constants.IMPORT_PACKAGE, importStr, true);
    wloop:
      while (i.hasNext()) {
	Map e = (Map)i.next();
	PkgEntry pe = new PkgEntry((String)e.get("key"), 
				  (String)e.get(Constants.PACKAGE_SPECIFICATION_VERSION),
				  bundle);
	for (int x = imports.size() - 1; x >= 0; x--) {
	  PkgEntry ip = (PkgEntry)imports.get(x);
	  if (pe.packageEqual(ip)) {
	    if (pe.compareVersion(ip) < 0) {
	      imports.set(x, pe);
	    }
	    continue wloop;
	  }
	}
	imports.add(pe);
      }
    } catch (IllegalArgumentException e) {
      b.framework.listeners.frameworkError(b, e);
    }
    dImportPatterns.add("java.");
    try {
      Iterator i = Util.parseEntries(Constants.DYNAMICIMPORT_PACKAGE, dimportStr, true);
      while (i.hasNext()) {
	Map e = (Map)i.next();
	String key = (String)e.get("key");
	if (key.equals("*")) {
	  dImportPatterns = null;
	} else if (key.endsWith(".*")) {
	  dImportPatterns.add(key.substring(0, key.length() - 1));
	} else if (key.endsWith(".")) {
	  b.framework.listeners.frameworkError(b, new IllegalArgumentException(
            Constants.DYNAMICIMPORT_PACKAGE + " entry ends with '.': " + key));
	} else if (key.indexOf("*") != - 1) {
	  b.framework.listeners.frameworkError(b, new IllegalArgumentException(
            Constants.DYNAMICIMPORT_PACKAGE + " entry contains a '*': " + key));
	} else {
	  dImportPatterns.add(key);
	}
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
   * Register bundle packages in framework.
   *
   */
  synchronized boolean unregisterPackages(boolean force) {
    Iterator i = (okImports != null ? okImports.values() : imports).iterator();
    if (bundle.framework.packages.unregisterPackages(exports.iterator(), i, force)) {
      okImports = null;
      return true;
    } else {
      return false;
    }
  }


  /**
   * Resolve all the bundles packages.
   *
   * @return true if we resolved all packages. If we failed
   *         return false. Reason for fail can be fetched with
   *         getResolveFailReason().
   */
  boolean resolvePackages() {
    if (bundle.framework.bPermissions) {
      String e = checkPackagePermission(exports, PackagePermission.EXPORT);
      if (e != null) {
	failReason = "missing export permission for package(s): " + e;
	return false;
      }
      String i = checkPackagePermission(imports, PackagePermission.IMPORT);
      if (i != null) {
	failReason = "missing import permission for package(s): " + i;
	return false;
      }
    }
    List m = bundle.framework.packages.checkResolve(bundle, imports.iterator());
    if (m != null) {
      StringBuffer r = new StringBuffer("missing package(s) or can not resolve all of the them: ");
      Iterator mi = m.iterator();
      r.append(((PkgEntry)mi.next()).pkgString());
      while (mi.hasNext()) {
	r.append(", ");
	r.append(((PkgEntry)mi.next()).pkgString());
      }
      failReason = r.toString();
      return false;
    } else {
      failReason = null;
      okImports = new HashMap();
      for (Iterator i = imports.iterator(); i.hasNext(); ) {
	PkgEntry pe = (PkgEntry)i.next();
	okImports.put(pe.name, pe);
      }
      return true;
    }
  }


  /**
   * If bundle packages has been resolved look for a bundle
   * that provides the requested package.
   * If found, check we import it. If not imported, check
   * if we can dynamicly import the package.
   *
   * @param pkg Package name
   * @return Bundle exporting
   */
  BundleImpl getProviderBundle(String pkg) {
    synchronized (this) {
      if (okImports == null) {
	return null;
      }
      PkgEntry pe = (PkgEntry)okImports.get(pkg);
      if (pe != null) {
	Pkg p = pe.pkg;
	if (p != null) {
	  PkgEntry ppe = p.provider;
	  if (ppe != null) {
	    return ppe.bundle;
	  }
	}
	return null;
      }
    }
    boolean match = false;
    if (dImportPatterns == null) {
      match = true;
    } else {
      for (Iterator i = dImportPatterns.iterator(); i.hasNext(); ) {
	String ps = (String)i.next();
	if ((ps.endsWith(".") && pkg.startsWith(ps)) || pkg.equals(ps)) {
	  match = true;
	  break;
	}
      }
    }
    if (match && checkPackagePermission(pkg, PackagePermission.IMPORT)) {
      synchronized (bundle.framework.packages) {
	synchronized (this) {
	  PkgEntry pe = new PkgEntry(pkg, (String)null, bundle);
	  PkgEntry ppe = bundle.framework.packages.registerDynamicImport(pe);
	  if (ppe != null) {
	    okImports.put(pkg, pe);
	    return ppe.bundle;
	  }
	}
      }
    }
    return null;
  }


  /**
   * Get an iterator over all exported packages.
   *
   * @return An Iterator over PkgEntry.
   */
  Iterator getExports() {
    return exports.iterator();
  }


  /**
   * Get an iterator over all static imported packages.
   *
   * @return An Iterator over PkgEntry.
   */
  Iterator getImports() {
    return imports.iterator();
  }


  /**
   * Return a string with a reason for why resolve failed.
   *
   * @return A error message string.
   */
  String getResolveFailReason() {
    return failReason;
  }

  //
  // Private methods
  //

  /**
   * Check that we have right package permission for a list of packages.
   *
   * @param pkgs List over all packages to check. Data is
   *             Map.Entry with a List of package names as key
   *             and a Map of parameters as value.
   * @param perm Package permission action to check against.
   * @return Returns null if we have correct permission for listed package.
   *         Otherwise a string of failed entries.
   */
  private String checkPackagePermission(List pkgs, String perm) {
    String res = null;
    if (bundle.framework.bPermissions) {
      for (Iterator i = pkgs.iterator(); i.hasNext();) {
	PkgEntry p = (PkgEntry)i.next();
	if (!checkPackagePermission(p.name, perm)) {
	  if (res != null) {
	    res = res + ", " + p.name;
	  } else {
	    res = p.name;
	  }
	}
      }
    }
    return res;
  }


  /**
   * Check that we have right package permission for a package.
   *
   * @param pkg Packages to check. 
   * @param perm Package permission action to check against.
   * @return Returns true if we have permission.
   */
  private boolean checkPackagePermission(String pkg, String perm) {
    if (bundle.framework.bPermissions) {
      return bundle.framework.permissions.getPermissionCollection(bundle)
	.implies(new PackagePermission(pkg, perm));
    } else {
      return true;
    }
  }

}
