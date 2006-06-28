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

import java.io.*;
import java.net.*;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.osgi.framework.*;

/**
 * Classloader for bundle JAR files.
 *
 * @author Jan Stein
 * @author Philippe Laporte
 */
final public class BundleClassLoader extends ClassLoader {

  /**
   * Debug
   */
  final private boolean debug = Debug.classLoader;

  /**
   * Framework class loader
   */
  final static private ClassLoader parent = Framework.class.getClassLoader();

  /**
   * Whether we run in a Java 2 environment.
   */
  private static boolean isJava2;

  /**
   * Handle to secure operations.
   */
  final PermissionOps secure;

  /**
   * Bundle classloader protect domain.
   */
  final ProtectionDomain protectionDomain;

  /**
   * Archive that we load code from.
   */
  private BundleArchive archive;

  /**
   * Fragment archives that we load code from.
   */
  private ArrayList /* BundleArchive */ fragments;

  /**
   * Imported and Exported java packages.
   */
  private BundlePackages bpkgs;
  
  private static ArrayList /* String */ bootDelegationPatterns = new ArrayList(1);
  private static boolean bootDelegationUsed /*= false*/;
  

  static {
    try {
      ClassLoader.class.getDeclaredMethod("findLibrary", new Class [] { String.class });
      isJava2 = true;
    } catch (NoSuchMethodException ignore) {
      isJava2 = false;
    }
   
    buildBootDelegationPatterns();
  }


  static void buildBootDelegationPatterns() { 
    String bootDelegationString = System.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
    bootDelegationUsed = (bootDelegationString != null);
   
    try {
      Iterator i = Util.parseEntries(Constants.FRAMEWORK_BOOTDELEGATION, 
				     bootDelegationString,
				     true, true, false);

      while (i.hasNext()) {
	Map e = (Map)i.next();
	String key = (String)e.get("key");
	if (key.equals("*")) {
	  bootDelegationPatterns = null;
	  //in case funny person puts a * amongst other things
	  break;
	} 
	else if (key.endsWith(".*")) {
	  bootDelegationPatterns.add(key.substring(0, key.length() - 1));
	} 
	else if (key.endsWith(".")) {
	  Main.framework.listeners.frameworkError(Main.framework.systemBundle, new IllegalArgumentException(
													    Constants.FRAMEWORK_BOOTDELEGATION + " entry ends with '.': " + key));
	} 
	else if (key.indexOf("*") != - 1) {
	  Main.framework.listeners.frameworkError(Main.framework.systemBundle, new IllegalArgumentException(
													    Constants.FRAMEWORK_BOOTDELEGATION + " entry contains a '*': " + key));
	} 
	else {
	  bootDelegationPatterns.add(key);
	}
      }
    }
    catch (IllegalArgumentException e) {
      Main.framework.listeners.frameworkError(Main.framework.systemBundle, e);
    }
    //TODO remove when finished up. This currently insures R3 compliant behavior
    //for now removing this will break. finish up when when working on other new classLoading aspects 
    //I could have done more but felt like leaving this to the one who will do the other 
    //new classLoading aspects was more appropriate
    bootDelegationPatterns = null;
    bootDelegationUsed = true;
  }
  
  static boolean isBootDelegated(String className){ 
    if(!bootDelegationUsed){
      return false;
    }
    int pos = className.lastIndexOf('.');
    if (pos != -1) {
      String classPackage = className.substring(0, pos);  
      if (bootDelegationPatterns == null) {
        return true;
      } 
      else {
        for (Iterator i = bootDelegationPatterns.iterator(); i.hasNext(); ) {
          String ps = (String)i.next();
          if ((ps.endsWith(".") && 
               classPackage.startsWith(ps)) || 
              classPackage.equals(ps)) {
            return true;
          }
        }
      }
    }
    return false;
  }
  

  /**
   * Create class loader for specified bundle.
   */
  BundleClassLoader(BundlePackages bpkgs, BundleArchive ba, ArrayList frags,
                    ProtectionDomain pd, PermissionOps secure) {
    super(parent); //otherwise getResource will bypass OUR parent
    this.secure = secure;
    protectionDomain = pd;
    this.bpkgs = bpkgs;
    archive = ba;
    fragments = frags;
    if (debug) {
      Debug.println(this + " Created new classloader");
    }
  }

  //
  // ClassLoader classes
  //

  /**
   * Find bundle class to load.
   * First check if this load comes from an imported package.
   * Otherwise load class from our bundle.
   *
   * @see java.lang.ClassLoader#findClass
   */
  protected Class findClass(String name) throws ClassNotFoundException {
    if (name.startsWith("java.")) {
      return parent.loadClass(name);
    }
    if (isBootDelegated(name)) {
      try {
        return parent.loadClass(name);
      } catch (ClassNotFoundException e) { }
    }
    if (secure.okClassAdminPerm(bpkgs.bundle)) {
      String path;
      String pkg;
      int pos = name.lastIndexOf('.');
      if (pos != -1) {
        path = name.replace('.', '/');
        pkg = name.substring(0, pos);
      } else {
        path = name;
        pkg = null;
      }
      Class res = (Class)secure.callSearchFor(this, name, pkg, path + ".class",
                                              classSearch, true, this, null);
      if (res != null) {
        return res;
      }
    }
    throw new ClassNotFoundException(name);
  }


  /**
   * Find native library code to load.
   *
   * @see java.lang.ClassLoader#findLibrary
   */
  protected String findLibrary(String name) {
    String res = archive.getNativeLibrary(name);
    if (res == null && fragments != null) {
      for (Iterator i = fragments.iterator(); i.hasNext(); ) {
        res = ((BundleArchive)i.next()).getNativeLibrary(name);
        if (res != null) {
          break;
        }
      }
    }
    if (debug) {
      Debug.println(this + " Find library: " + name + (res != null ? " OK" : " FAIL"));
    }
    return res;
  }


  /**
   * Returns an Enumeration of all the resources with the given name.
   *
   * @see java.lang.ClassLoader#findResources
   */
  protected Enumeration findResources(String name) {
    // Step 1 and 2 are done by getResources?
    return getBundleResources(name, false);
  }


  /**
   * Finds the resource with the given name.
   *
   * @see java.lang.ClassLoader#findResource
   */
  protected URL findResource(String name) {
    Enumeration res = getBundleResources(name, true);
    if (res != null) {
      return (URL)res.nextElement();
    } else {
      return null;
    }
  }
  

  /**
   * Find Class and load it. This function is abstract in PJava 1.2
   * so we define it here to work as closely as it can to Java 2.
   * Should work okey if we don't use the Java 2 stuff.
   *
   * @param name the name of the class
   * @param resolve if <code>true</code> then resolve the class
   * @return the resulting <code>Class</code> object
   * @exception ClassNotFoundException if the class could not be found
   * @see java.lang.ClassLoader#loadClass
   */
  protected Class loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
    Class c = findLoadedClass(name);
    if (c == null) {
      c = findClass(name);
    }
    if (resolve) {
        resolveClass(c);
    }
    return c;
  }


  /**
   * Finds the resource with the given name. This is defined a little
   * different in PJava 1.2 versus Java 2. So we first try to use
   * the super() version and if it fails we try to find it in the
   * local bundle.
   *
   * @param  name resource name
   * @return an URL to resource, or <code>null</code> if
   *         the resource could not be found or the caller doesn't have
   *         adequate privileges to get the resource.
   * @see java.lang.ClassLoader#getResource
   */
  public URL getResource(String name) {
    if (debug) {
      Debug.println(this + " getResource: " + name);
    }
    URL res = super.getResource(name);
    if (res == null && !isJava2) {
      return findResource(name);
    }
    return res;
  }


  /**
   * Finds the resource with the given name and returns the InputStream.
   * The method is overridden to make sure it does the right thing.
   *
   * @param  name resource name
   * @return an InputStream to resource, or <code>null</code> if
   *         the resource could not be found or the caller doesn't have
   *         adequate privileges to get the resource.
   * @see java.lang.ClassLoader#getResourceAsStream
   */
  public InputStream getResourceAsStream(String name) {
    try {
      URL url = getResource(name);
      if (url != null) {
        return url.openStream();
      }
    } catch (IOException ignore) { }
    return null;
  }


  /**
   * Return a string representing this objet
   *
   * @return A message string.
   */
  public String toString() {
    return "BundleClassLoader(id=" + bpkgs.bundle.id + ",gen=" + bpkgs.generation + ")";
  }

  //
  // BundleClassLoader specific
  //

  /**
   * Get bundle owning this class loader.
   */
  BundleImpl getBundle() {
    return bpkgs.bundle;
  }


  /**
   * Get bundle archive belonging to this class loader.
   */
  BundleArchive getBundleArchive(long frag) {
    if (frag >= 0) {
      if (fragments != null) {
        for (Iterator i = fragments.iterator(); i.hasNext(); ) {
          // NYI improve this solution
          BundleArchive ba = (BundleArchive)i.next();
          if (ba.getBundleId() == frag) {
            return ba;
          }
        }
      }
      return null;
    } else {
      return archive;
    }
  }
      

  /**
   * Close down this classloader.
   * We don't give out any new classes. Perhaps we should
   * block all classloads.
   */
  void close() {
    archive = null;
    bpkgs.invalidateClassLoader();
    if (debug) {
      Debug.println(this + " Cleared archives");
    }
  }


  /**
   * Close down this classloader and all its archives.
   * Purge all archives.
   *
   */
  void purge() {
    bpkgs.unregisterPackages(true);
    if (protectionDomain != null) {
      bpkgs.bundle.framework.perm.purge(bpkgs.bundle, protectionDomain);
    }
    if (archive != null) {
      archive.purge();
    }
    if (fragments != null) {
      for (Iterator i = fragments.iterator(); i.hasNext(); ) {
        // NYI improve this solution
        BundleArchive ba = (BundleArchive)i.next();
        BundleImpl b = (BundleImpl)bpkgs.bundle.framework.bundles.getBundle(ba.getBundleLocation());
        if (b == null || b.archive != ba) {
          ba.purge();
        }
      }
    }
    close();
  }


  /**
   * Get all the resources with the given name in this bundle.
   *
   */
  Enumeration getBundleResources(String name, boolean onlyFirst) {
    if (secure.okResourceAdminPerm(bpkgs.bundle)) {
      if (debug) {
        Debug.println(this + " Find bundle resource" + (onlyFirst ? "" : "s") + ": " + name);
      }
      String pkg = null;
      int pos = name.lastIndexOf('/');
      if (pos > 0) {
        int start = name.startsWith("/") ? 1 : 0;
        pkg = name.substring(start, pos).replace('/', '.');
      } else {
        pkg = null;
      }
      return (Enumeration)secure.callSearchFor(this, null, pkg, name, resourceSearch,
                                               onlyFirst, this, null);
    } else {
      return null;
    }
  }


  /**
   * Find localization files and load.
   *
   */
  Hashtable getLocalizationEntries(String name) {
    Hashtable localization_entries = null;
    if (fragments != null) {
      for (int i = fragments.size() - 1; i >= 0; i--) {
        BundleArchive ba = (BundleArchive)fragments.get(i);
        Hashtable tmp = ba.getLocalizationEntries(name);
        if (tmp != null) {
          if (localization_entries != null) {
            localization_entries.putAll(tmp);
          } else {
            localization_entries = tmp;
          }
        }
      }
    }
    Hashtable tmp = archive.getLocalizationEntries(name);
    if (tmp != null) {
      if (localization_entries != null) {
        localization_entries.putAll(tmp);
      } else {
        localization_entries = tmp;
      }
    }
    return localization_entries;
  }


  /**
   * Get bundle package handler.
   *
   */
  BundlePackages getBpkgs() {
    return bpkgs;
  }

  //
  // Private
  //

  /**
   * Search for classloader to use according to OSGi search order.
   *
   * 3 If the class or resource is in a package that is imported using
   *   Import-Package or was imported dynamically in a previous load,
   *   then the request is delegated to the exporting bundles class
   *   loader; otherwise the search continues with the next step.
   *   If the request is delegated to an exporting class loader and
   *   the class or resource is not found, then the search terminates
   *    and the request fails.
   *
   * 4 If the class or resource is in a package that is imported from
   *   one or more other bundles using Require-Bundle, the request is
   *   delegated to the class loaders of the other bundles, in the
   *   order in which they are specified in this bundles manifest.
   *   If the class or resource is not found, then the search
   *   continues with the next step.
   *
   * 5 The bundles own internal bundle class path is searched. If the
   *   class or resource is not found, then the search continues with
   *   the next step.
   *
   * 6 Each attached fragment's internal bundle class path is searched.
   *   The fragments are searched in ascending bundle ID order. If the
   *   class or resource is not found, then the search continues with
   *    the next step.
   *
   * 7 If the class or resource is in a package that is exported by
   *   the bundle or the package is imported by the bundle (using
   *   Import-Package or Require-Bundle), then the search ends and
   *   the class or resource is not found.
   *
   * 8 Otherwise, if the class or resource is in a package that is
   *   imported using DynamicImport-Package, then a dynamic import
   *   of the package is now attempted. An exporter must conform to
   *   any implied package constraints. If an appropriate exporter
   *   is found, a wire is established so that future loads of the
   *   package are handled in Step 3. If a dynamic wire is not
   *   established, then the request fails.
   *
   * 9 If the dynamic import of the package is established, the
   *   request is delegated to the exporting bundle's class loader.
   *   If the request is delegated to an exporting class loader and
   *   the class or resource is not found, then the search
   *   terminates and the request fails.
   *
   * @param name Name of class or null if we look for a resource
   * @param pkg Package name for item
   * @param path File path to item searched ("/" seperated)
   * @param action Action to be taken when item is found
   * @param onlyFirst Stop search when first matching item is found.
   *
   * @return Object returned from action class.
   */
  Object searchFor(String name, String pkg, String path, SearchAction action,
                   boolean onlyFirst, BundleClassLoader requestor, HashSet visited) {
    BundlePackages pbp;
    ExportPkg ep;

    // TBD! Should this be an action method
    if (action == classSearch && requestor != this) {
      Class c = findLoadedClass(name);
      if (c != null) {
        return c;
      }
    }

    if (debug) {
      Debug.println(this + " Search for: " + path);
    }
    /* 3 */
    if (pkg != null) {
      pbp = bpkgs.getProviderBundlePackages(pkg);
      if (pbp != null) {
        BundleClassLoader cl = (BundleClassLoader)pbp.getClassLoader();
        if (cl != this) {
          if (cl != null) {
            if (debug) {
              Debug.println(this + " Import search: " + path +
                            " from #" + pbp.bundle.id);
            }
            return secure.callSearchFor(cl, name, pkg, path, action,
                                        onlyFirst, requestor, visited);
          }
          if (debug) {
            Debug.println(this + " No import found: " + path);
          }
          return null;
        }
      } else {
        /* 4 */
        ArrayList pl = bpkgs.getRequiredBundlePackages(pkg);
        if (pl != null) {
          if (visited == null) {
            visited = new HashSet();
          }
          visited.add(this);
          for (Iterator pi = pl.iterator(); pi.hasNext(); ) {
            pbp = (BundlePackages)pi.next();
            if (pbp != null) {
              BundleClassLoader cl = (BundleClassLoader)pbp.getClassLoader();
              if (cl != null && !visited.contains(cl)) {
                if (debug) {
                  Debug.println(this + " Required bundle search: " +
                                path + " from #" + pbp.bundle.id);
                }
                Object res = secure.callSearchFor(cl, name, pkg, path, action,
                                                  onlyFirst, requestor, visited);
                if (res != null) {
                  return res;
                }
              }
            }
          }
        }
      }
      ep = bpkgs.getExport(pkg);
    } else {
      ep = null;
    }
    /* 5 */
    if (this != requestor && ep != null && !ep.checkFilter(name)) {
      return null;
    }
    Vector av = archive.componentExists(path, onlyFirst);
    if (av != null) {
      try {
        return action.get(av, path, name, pkg, this, archive);
      } catch (IOException ioe) {
        bpkgs.bundle.framework.listeners.frameworkError(bpkgs.bundle, ioe);
        return null;
      }
    }
    /* 6 */
    if (fragments != null) {
      for (Iterator i = fragments.iterator(); i.hasNext(); ) {
        BundleArchive ba = (BundleArchive)i.next();
        if (debug) {
          Debug.println(this + " Fragment bundle search: " +
                        path + " from #" + ba.getBundleId());
        }
        Vector vec = ba.componentExists(path, onlyFirst);
        if (vec != null) {
          try {
            return action.get(vec, path, name, pkg, this, ba);
          } catch (IOException ioe) {
            bpkgs.bundle.framework.listeners.frameworkError(bpkgs.bundle, ioe);
            return null;
          }
        }
      }
    }
    /* 7 */
    if (ep != null) {
      return null;
    }
    /* 8 */
    if (pkg != null) {
      pbp = bpkgs.getDynamicProviderBundlePackages(pkg);
      if (pbp != null) {
        /* 9 */
        BundleClassLoader cl = (BundleClassLoader)pbp.getClassLoader();
        if (cl != null) {
          if (debug) {
            Debug.println(this + " Dynamic import search: " +
                          path + " from #" + pbp.bundle.id);
          }
          return secure.callSearchFor(cl, name, pkg, path, action,
                                      onlyFirst, requestor, visited);
        }
      }
      if (debug) {
        Debug.println(this + " No dynamic import: " + path);
      }
    }
    return null;
  }


  /**
   *  Search action
   */
  interface SearchAction {
    public Object get(Vector items, String path, String name, String pkg,
                      BundleClassLoader cl, BundleArchive ba) throws IOException ;
  }


  /**
   *  Search action for class searching
   */
  static final SearchAction classSearch = new SearchAction() {
      public Object get(Vector items, String path, String name, String pkg,
                        BundleClassLoader cl, BundleArchive ba) throws IOException {
        byte[] bytes = ba.getClassBytes((Integer)items.get(0), path);
        if (bytes != null) {
          if (Debug.classLoader) {
            Debug.println("classLoader(#" + cl.bpkgs.bundle.id + ") - load class: " + name);
          }
          synchronized (cl) {
            Class c = cl.findLoadedClass(name);
            if (c == null) {
              if (pkg != null) {
                if (cl.getPackage(pkg) == null) {
                  cl.definePackage(pkg, null, null, null, null, null, null, null);
                }
              }
              if (cl.protectionDomain == null) {
                // Kaffe can't handle null protectiondomain
                c = cl.defineClass(name, bytes, 0, bytes.length);
              } else {
                c = cl.defineClass(name, bytes, 0, bytes.length, cl.protectionDomain);
              }
            }
            return c;
          }
        }
        return null;
      }
    };


  /**
   *  Search action for resource searching
   */
  static final SearchAction resourceSearch = new SearchAction() {
      public Object get(Vector items, String path, String name, String _pkg,
                        BundleClassLoader cl, BundleArchive ba) {
        Vector answer = new Vector(items.size());
        BundlePackages bp = cl.bpkgs;
        for(int i = 0; i < items.size(); i++) {
          int subId = ((Integer)items.elementAt(i)).intValue();
          URL url = cl.bpkgs.bundle.getURL(cl.bpkgs.generation,
                                           ba.getBundleId(),
                                           subId,
                                           path);
          if (url != null) {
            if (Debug.classLoader) {
              Debug.println("classLoader(#" + cl.bpkgs.bundle.id + ") - found: " +
                            path + " -> " + url);
            }
            answer.addElement(url);
          } else {
            return null;   
          }
        }
        return answer.elements();
      }
    };

} //class
