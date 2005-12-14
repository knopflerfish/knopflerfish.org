/*
 * Copyright (c) 2003-2005, KNOPFLERFISH project
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
 */
class BundlePackages {

  final BundleImpl bundle;

  private ArrayList /* ExportPkg */ exports = new ArrayList(1);

  private ArrayList /* ImportPkg */ imports = new ArrayList(1);

  private ArrayList /* String */ dImportPatterns = new ArrayList(1);

  private HashMap /* String -> ImportPkg */ okImports = null;

  private String failReason = null;

  /**
   * Create package entry.
   */
  BundlePackages(BundleImpl b, 
		 String exportStr, 
		 String importStr, 
		 String dimportStr,
		 String manifestVer) {
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
      int manver = manifestVer != null ? Integer.parseInt(manifestVer) : 0;
      Iterator i = Util.parseEntries(Constants.EXPORT_PACKAGE, exportStr, true);
      while (i.hasNext()) {
	ExportPkg p = new ExportPkg((Map)i.next(), bundle);
	exports.add(p);
	if ( manver < 2 ) {
	  imports.add(new ImportPkg(p));
	}
      }
    } catch (IllegalArgumentException e) {
      b.framework.listeners.frameworkError(b, e);
    }
    try {
      Iterator i = Util.parseEntries(Constants.IMPORT_PACKAGE, importStr, true);
    wloop:
      while (i.hasNext()) {
	ImportPkg p = new ImportPkg((Map)i.next(), bundle);
	for (int x = imports.size() - 1; x >= 0; x--) {
	  ImportPkg ip = (ImportPkg)imports.get(x);
	  if (p.packageNameEqual(ip)) {
	    if (p.compareVersion(ip) < 0) {
	      imports.set(x, p);
	    }
	    continue wloop;
	  }
	}
	imports.add(p);
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
      //if funny one puts a * amongst other things
	  break;
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
   * Unregister bundle packages in framework.
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
      okImports = new HashMap();
      for (Iterator i = imports.iterator(); i.hasNext(); ) {
	ImportPkg ip = (ImportPkg)i.next();
	// NYI, fix multiple providers
	ip.provider = bundle.framework.packages.getProvider(ip.name);
	okImports.put(ip.name, ip);
      }
      return true;
    }
  }


  /**
   * If bundle package has been resolved look for a bundle
   * that provides the requested package.
   * If found, check if we import it. If not imported, check
   * if we can dynamically import the package.
   *
   * @param pkg Package name
   * @return Bundle exporting
   */
  synchronized BundleImpl getProviderBundle(String pkg) {
    if (okImports == null) {
      return null;
    }
    ImportPkg ip = (ImportPkg)okImports.get(pkg);
    if (ip != null) {
      return ip.provider.bundle;
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
      ip = new ImportPkg(pkg, bundle);
      ExportPkg ep = bundle.framework.packages.registerDynamicImport(ip);
      if (ep != null) {
	ip.provider = ep;
	okImports.put(pkg, ip);
	return ep.bundle;
      }
    }
    return null;
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

}
