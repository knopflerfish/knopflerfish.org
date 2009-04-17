/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
 * within framework.
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
  private Hashtable /* String->Pkg */ packages = new Hashtable();

  /**
   * List of temporary resolved bundles.
   */
  private ArrayList /* BundleImpl */ tempResolved = null;

  /**
   * Map of temporary package providers.
   */
  private HashMap tempProvider = null;

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
      PkgEntry pe = (PkgEntry)exports.next();
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
      PkgEntry pe = (PkgEntry)imports.next();
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
   * @param pe PkgEntry import to add.
   * @return PkgEntry for package provider.
   */
  synchronized PkgEntry registerDynamicImport(PkgEntry pe) {
    if (Debug.packages) {
      Debug.println("dynamicImportPackage: try " + pe);
    }
    Pkg p = (Pkg)packages.get(pe.name);
    if (p != null) {
      PkgEntry provider = p.getProvider();
      if(provider != null) {
	if((pe.bundle.getState() & RESOLVED_FLAGS) != 0) {
	  p.addImporter(pe);
	  if (Debug.packages) {
	    Debug.println("dynamicImportPackage: added " + pe);
	  }
	} else {
	  if (Debug.packages) {
	    Debug.println("dynamicImportPackage: skip add since bundle is not resolved " + pe);
	  }
	}
	return provider;
      } else {
	// If the bundle trying to use dynamic import is resolved but 
	// potential providers are yet not resolved check for providers 
	// and resolve them as necessary

	// List of bundles we try to resolve but fail
	List /* BundleImpl */ failedBundles = new ArrayList();	

	if((pe.bundle.getState() & RESOLVED_FLAGS) != 0) {
	  for(Iterator it = p.exporters.iterator(); it.hasNext();) {
	    PkgEntry pe2 = (PkgEntry)it.next();
	    int state = pe2.bundle.getUpdatedState();
	    if((state & RESOLVED_FLAGS) != 0) {
	      p.addImporter(pe);
	      return p.getProvider();
	    } else {
	      // add to set, to be able to give informative debug info in 
	      // the case of all exporters fail to resolve
	      failedBundles.add(pe2.bundle);
	    }
	  }
	  // If we reach this, not potential exporter has been
	  // possible to resolve.
	  framework.listeners.frameworkError(pe.bundle,
					     new Exception("dynamicResolve: failed to resolve " + pe +", unresolved exportes: " + failedBundles));
	  return null;
	} else {
	  if (Debug.packages) {
	    Debug.println("dynamicImportPackage: failed because importing bundle is not resolved: " + pe.bundle.toString(2));
	  }
	}
      }
    }
    
    return null;
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
      PkgEntry pe = (PkgEntry)exports.next();
      Pkg p = pe.getPkg();
      if (p != null) {
	if (Debug.packages) {
	  Debug.println("unregisterPackages: unregister export - " + pe);
	}
	if (!p.removeExporter(pe)) {
	  if (force) {
	    p.setProvider(null);
	    p.removeExporter(pe);
	    if (Debug.packages) {
	      Debug.println("unregisterPackages: forced unregister - " + pe);
	    }
	  } else {
	    allRemoved = false;
	    p.setZombie();
	    if (Debug.packages) {
	      Debug.println("unregisterPackages: failed to unregister - " + pe);
	    }
	    continue;
	  }
	}
	if (p.isEmpty()) {
	  packages.remove(pe.name);
	}
      }
    }
    if (allRemoved) {
      while (imports.hasNext()) {
	PkgEntry pe = (PkgEntry)imports.next();
	Pkg p = pe.getPkg();
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
   * Check if a list packages can be resolved.
   *
   * @param exports Exported packages.
   * @param imports Imported packages.
   * @param bundle Bundle owning packages.
   * @param pkgs List of packages to be resolved.
   * @return List of packages not resolvable or null if all were resolved.
   */
  synchronized List checkResolve(Bundle bundle, Iterator pkgs) {
    if (Debug.packages) {
      Debug.println("checkResolve: " + bundle);
    }
    if (tempResolved != null) {
      // If we entry with tempResolved set, it means that we already have
      // resolved bundles. Check that it is true!
      if (!tempResolved.contains(bundle)) {
	framework.listeners.frameworkError(bundle,
					   new Exception("checkResolve: InternalError1!"));
      }
      return null;
    }
    tempProvider = new HashMap();
    tempResolved = new ArrayList();
    tempResolved.add(bundle);
    List res = resolvePackages(pkgs);
    if (res.size() == 0) {
      for (Iterator i = tempProvider.values().iterator(); i.hasNext();) {
	PkgEntry pe = (PkgEntry)i.next();
	pe.getPkg().setProvider(pe);
      }
      tempResolved.remove(0);
      for (Iterator i = tempResolved.iterator(); i.hasNext();) {
	BundleImpl bs = (BundleImpl)i.next();
	if (bs.getUpdatedState() == Bundle.INSTALLED) {
	  framework.listeners.frameworkError(bs,
					     new Exception("checkResolve: InternalError2!"));
	}
      }
      res = null;
    }
    tempProvider = null;
    tempResolved = null;
    return res;
  }


  /**
   * Get selected provider of a package.
   *
   * @param pkg Exported package.
   * @return PkgEntry that exports the package, null if no provider.
   */
  PkgEntry getProvider(String pkg) {
    Pkg p = (Pkg)packages.get(pkg);
    if (p != null) {
      return p.getProvider();
    } else {
      return null;
    }
  }
    
    
  /**
   * Get all packages exported by the system.
   *
   * @return Export-package string for system bundle.
   */
  String systemPackages() {
    StringBuffer res = new StringBuffer();
    synchronized (packages) {
      for (Iterator i = packages.values().iterator(); i.hasNext();) {
	Pkg p = (Pkg)i.next();
	PkgEntry pe = p.getProvider();
	if (pe != null && framework.systemBundle == pe.bundle) {
	  if (res.length() > 0) {
	    res.append(", ");
	  }
	  res.append(pe.pkgString());
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
   */
  String getPackageVersion(String pkg) {
    Pkg p = (Pkg)packages.get(pkg);
    PkgEntry pe = p.getProvider();
    return pe != null && pe.version.isSpecified() ? pe.version.toString() : null;
  }


  /**
   * Get packages provide by bundle. If bundle is null, get all.
   *
   * @param b Bundle exporting packages.
   * @return List of packages exported by bundle.
   */
  Collection getPackagesProvidedBy(Bundle b) {
    ArrayList res = new ArrayList();
    synchronized (packages) {
      for (Iterator i = packages.values().iterator(); i.hasNext();) {
	Pkg p = (Pkg) i.next();
	PkgEntry provider = p.getProvider();
	if (provider != null && (b == null || b == provider.bundle)) {
	  res.add(provider);
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
  Collection getPackageImporters(String pkg) {
    Pkg p = (Pkg)packages.get(pkg);
    Set res = new HashSet();
    if (p != null) {
      synchronized (p) {
	if (p.getProvider() != null) {
	  List i = p.importers;
	  for (int x =  0; x < i.size(); x++ ) {
	    PkgEntry pe = (PkgEntry)i.get(x);
	    if (pe.bundle.state != Bundle.INSTALLED) {
	      res.add(pe.bundle);
	    }
	  }
	}
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

    if (bundles == null) {
      if (Debug.packages) {
	Debug.println("getZombieAffected: check - null");
      }
      for (Iterator i = packages.values().iterator(); i.hasNext();) {
	Pkg p = (Pkg)i.next();
	PkgEntry pe = p.getProvider();
	if (pe != null && p.isZombie()) {
	  if (Debug.packages) {
	    Debug.println("getZombieAffected: found zombie - " + pe.bundle);
	  }
	  affected.add(pe.bundle);
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
	PkgEntry pe = (PkgEntry)j.next();
	if (pe.isProvider()) {
	  for (Iterator k = getPackageImporters(pe.name).iterator(); k.hasNext(); ) {
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
   * Check if a bundle has all its package dependencies resolved.
   *
   * @param pkgs List of packages to be resolved.
   * @return List of packages not resolvable.
   */
  private List resolvePackages(Iterator pkgs) {
    ArrayList res = new ArrayList();
    while (pkgs.hasNext()) {
      PkgEntry pe = (PkgEntry)pkgs.next();
      if (Debug.packages) {
	Debug.println("resolvePackages: check - " + pe.pkgString());
      }
      Pkg pkg = pe.getPkg();
      PkgEntry provider = pkg.getProvider();
      if (provider == null) {
	provider = (PkgEntry)tempProvider.get(pe.name);
	if (provider == null) {
	  provider = pickProvider(pkg);
	} else if (Debug.packages) {
	  Debug.println("resolvePackages: " + pe.name + " - has temporay provider - "
			+ provider);
	}
      } else if (Debug.packages) {
	Debug.println("resolvePackages: " + pe.name + " - has provider - " + provider);
      }
      if (provider == null) {
	if (Debug.packages) {
	  Debug.println("resolvePackages: " + pe.name + " - has no provider");
	}
	res.add(pe);
      } else if (provider.compareVersion(pe) < 0) {
	if (Debug.packages) {
	  Debug.println("resolvePackages: " + pe.name + " - provider has wrong version - " + provider + ", need " + pe.version + ", has " + provider.version);
	}
	res.add(pe);
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
  private PkgEntry pickProvider(Pkg p) {
    if (Debug.packages) {
      Debug.println("pickProvider: for - " + p.pkg);
    }
    PkgEntry provider = null;
    for (Iterator i = p.exporters.iterator(); i.hasNext(); ) {
      PkgEntry pe = (PkgEntry)i.next();
      if ((pe.bundle.state & RESOLVED_FLAGS) != 0) {
	provider = pe;
	break;
      }
      if (pe.bundle.state == Bundle.INSTALLED) {
	if (tempResolved.contains(pe.bundle)) {
	  provider = pe;
	  break;
	}
	int oldTempStartSize = tempResolved.size();
	HashMap oldTempProvider = (HashMap)tempProvider.clone();
	tempResolved.add(pe.bundle);
	List r = resolvePackages(pe.bundle.getImports());
	if (r.size() == 0) {
	  provider = pe;
	  break;
	} else {
	  tempProvider = oldTempProvider;
	  for (int x = tempResolved.size() - 1; x >= oldTempStartSize; x--) {
	    tempResolved.remove(x);
	  }
	}
      }
    }
    if (provider != null) {
      if (Debug.packages) {
	Debug.println("pickProvider: " + p.pkg + " - got provider - " + provider);
      }
      tempProvider.put(p.pkg, provider);
    }
    return provider;
  }

}
