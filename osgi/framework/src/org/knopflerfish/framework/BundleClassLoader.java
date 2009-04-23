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

import java.io.*;
import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.osgi.framework.*;

/**
 * Classloader for bundle JAR files.
 *
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */
final public class BundleClassLoader extends ClassLoader {

  /**
   * Framework class loader
   */
  final private ClassLoader parent;

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
  BundleArchive archive;

  /**
   * Fragment archives that we load code from.
   */
  private ArrayList /* BundleArchive */ fragments;

  /**
   * Imported and Exported java packages.
   */
  BundlePackages bpkgs;

  // android/dalvik VM stuff
  private static Constructor dexFileClassCons;
  private static Method      dexFileClassLoadClassMethod;

  // bDalvik will be set to true if we're running on the android
  // dalvik VM. 
  // package protected to enable other parts of framework to check 
  // for dalvik VM
  static boolean bDalvik = false;
  
  private Object dexFile = null;
  
  static {
    try {
      Class dexFileClass;
      try {
        dexFileClass = Class.forName("android.dalvik.DexFile");
      } catch( Exception ex ) {
        dexFileClass = Class.forName("dalvik.system.DexFile");
      }
      
      dexFileClassCons = 
        dexFileClass.getConstructor( new Class[] { File.class });
      
      dexFileClassLoadClassMethod = 
        dexFileClass.getMethod("loadClass", 
                               new Class[] { String.class, 
                                             ClassLoader.class 
                               });
      
      bDalvik = true;
      // if(debug.classLoader) {
      // debug.println("running on dalvik VM");
      // }
    } catch(Exception e) {
      dexFileClassCons            = null;
      dexFileClassLoadClassMethod = null;
    }
  }


  Debug debug;

  /**
   * Create class loader for specified bundle.
   */
  BundleClassLoader(BundlePackages bpkgs, BundleArchive ba, ArrayList frags,
                    ProtectionDomain pd, PermissionOps secure) {
    super(bpkgs.bundle.framework.getClass().getClassLoader()); //otherwise getResource will bypass OUR parent
    this.debug = bpkgs.bundle.framework.props.debug;
    this.parent = bpkgs.bundle.framework.getClass().getClassLoader();
    this.secure = secure;
    protectionDomain = pd;
    this.bpkgs = bpkgs;
    archive = ba;
    fragments = frags;
    if (debug.classLoader) {
      debug.println(this + " Created new classloader");
    }
  }

  //
  // ClassLoader classes
  //


  static boolean bHasASM        = false;
  static boolean bHasCheckedASM = false;

  /**
   * Check if this bundle is to be byte code patched
   */
  boolean isBundlePatch() {
    if(!bHasCheckedASM) {
      try {
        Class clazz = Class.forName("org.objectweb.asm.ClassReader");
        bHasASM = true;
      } catch (Exception no_asm_class) {
        bHasASM = false;
      }
      bHasCheckedASM = true;

      if(debug.patch) {
        debug.println("ASM library: " + bHasASM);
      }
    }

    return bHasASM &&
      "true".equals(bpkgs.bundle.framework.props.getProperty("org.knopflerfish.framework.patch",
                                                             "false"));
  }

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
    if (bpkgs.bundle.framework.isBootDelegated(name)) {
      try {
        Class bootDelegationCls = parent.loadClass(name);
        if (debug.classLoader && bootDelegationCls!=null) {
          debug.println(this +" findClass: " +name +" boot delegation: "
                        +bootDelegationCls);
        }
        return bootDelegationCls;
      } catch (ClassNotFoundException e) { }
    }
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

    if(!bpkgs.bundle.framework.props.STRICTBOOTCLASSLOADING) {
      if(isBootClassContext(name)) {
        if(debug.classLoader) {
          debug.println(this + " trying parent loader for class=" + name + ", since it was loaded on the system loader itself");
        }
	res = parent.loadClass(name);
        if(res != null) {
          if(debug.classLoader) {
            debug.println(this + " loaded " + name + " from " + parent);
          }
        }
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
    String res = secure.callFindLibrary0(this,name);
    if (debug.classLoader) {
      debug.println(this + " Find library: " + name + (res != null ? " OK" : " FAIL"));
    }
    return res;
  }


  /**
   * Returns an Enumeration of all the resources with the given name.
   *
   * @see java.lang.ClassLoader#findResources
   */
  protected Enumeration findResources(String name) {
    // Step 1 and 2 are done by getResources
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
   * Wrapper class around SecurityManager which exposes
   * the getClassLoader() method.
   */
  static class SecurityManagerExposer extends SecurityManager {
    public Class[] getClassContext() {
      return super.getClassContext();
    }
  }
    
  static protected SecurityManagerExposer smex = new SecurityManagerExposer();
  
  /**
   * Check if the current call is made from a class loaded on the 
   * boot class path (or rather, on a class loaded from something else
   * than a bundle class loader)
   */
  public boolean isBootClassContext(String msg) {
    Class[] classStack = smex.getClassContext();

    for (int i = 1; i < classStack.length; i++) {
      if ((this.getClass().getClassLoader() != classStack[i].getClassLoader())
          && !ClassLoader.class.isAssignableFrom(classStack[i])
          && !Class.class.equals(classStack[i])) {
        
        // If any of the classloaders for the caller's class is
        // a BundleClassLoader, we're not in a VM class context
        for (ClassLoader cl = classStack[i].getClassLoader(); 
             cl != null; cl = cl.getClass().getClassLoader()) {
          if (BundleClassLoader.class.isInstance(cl)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
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
    if (debug.classLoader) {
      debug.println(this + " getResource: " + name);
    }
    URL res = null;
    if (name.startsWith("java/")) {
      res = parent.getResource(name);
      if (debug.classLoader) {
        debug.println(this +" getResource: " +name +" file in java pkg: "+res);
      }
      return res;
    }

    if (bpkgs.bundle.framework.isBootDelegatedResource(name)) {
      res = parent.getResource(name);
      if (res!=null) {
        if (debug.classLoader) {
          debug.println(this +" getResource: " +name +" boot delegation: "
                        +res);
        }
        return res;
      }
    }

    res = findResource(name);
    if (debug.classLoader) {
      debug.println(this + " getResource: " + name +" bundle space: "+res);
    }
    return res;
  }


  // We would like to use the following implementation of
  // getResources() but that method is final in JDK 1.4
  // thus we can not redefine it here.
  /**
   * Finds all the resources with the given name. A resource is some data
   * (images, audio, text, etc) that can be accessed by class code in a way
   * that is independent of the location of the code.
   *
   * <p>The name of a resource is a <tt>/</tt>-separated path name that
   * identifies the resource.
   *
   * @param  name resource name
   * @return  An enumeration of {@link java.net.URL <tt>URL</tt>} objects for
   *          the resource.  If no resources could  be found, the enumeration
   *          will be empty.  Resources that the class loader doesn't have
   *          access to will not be in the enumeration.
   *
   * @see java.lang.ClassLoader#getResources
   * @see org.osgi.framework.Bundle#getResources(String name)
   *
   */
  public Enumeration getResourcesOSGi(String name) throws IOException {
    if (debug.classLoader) {
      debug.println(this + " getResources: " + name);
    }
    int start = name.startsWith("/") ? 1 : 0;
    if (name.substring(start).startsWith("java/")) {
      return parent.getResources(name);
    }

    Enumeration res = null;
    if (bpkgs.bundle.framework.isBootDelegatedResource(name)) {
      res = parent.getResources(name);
    }

    if (res==null || !res.hasMoreElements()) {
      res = findResources(name);
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
    if (fragments!=null) {
      fragments.clear();
      fragments = null;
    }
    if (debug.classLoader) {
      debug.println(this + " Cleared archives");
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
      if (debug.classLoader) {
        debug.println(this + " Find bundle resource" + (onlyFirst ? "" : "s") + ": " + name);
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

    if (debug.classLoader) {
      debug.println(this + " Search for: " + path);
    }
    /* 3 */
    if (pkg != null) {
      pbp = bpkgs.getProviderBundlePackages(pkg);
      if (pbp != null) {

        if (isSystemBundle(pbp.bundle)) {
          try {
            return pbp.bundle.framework.systemBundle.getClassLoader().loadClass(name);
          } catch (ClassNotFoundException e) {
            // continue
          }

        } else {
          BundleClassLoader cl = (BundleClassLoader)pbp.getClassLoader();
          // Second check avoids a loop when a required bundle imports a
          // package from its requiring host that it self should
          // provide contents for to the requiring bundle.
          if (cl != this
              && (visited==null || (cl!=null && !visited.contains(cl))) ) {
            if (cl != null) {
              if (debug.classLoader) {
                debug.println(this + " Import search: " + path +
                              " from #" + pbp.bundle.id);
              }
              return secure.callSearchFor(cl, name, pkg, path, action,
                                          onlyFirst, requestor, visited);
            }
            if (debug.classLoader) {
              debug.println(this + " No import found: " + path);
            }
            return null;
          }
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
                if (debug.classLoader) {
                  debug.println(this + " Required bundle search: " +
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
          if (debug.classLoader) {
            debug.println(this + " Required bundle search: "
                          +"Not found, continuing with local search.");
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
    // Must collect and merge all search hits from step 5
    // and 6 to handle the case when onlyFirst is false.
    Vector /* SearchActionItem */ sais = new Vector();

    Vector av = archive.componentExists(path, onlyFirst);
    if (av != null) {
      sais.add( new SearchActionItem( av, archive ) );
    }
    /* 6 */
    if (fragments != null && !(onlyFirst && sais.size()>0) ) {
      for (Iterator i = fragments.iterator(); i.hasNext(); ) {
        BundleArchive ba = (BundleArchive)i.next();
        if (debug.classLoader) {
          debug.println(this + " Fragment bundle search: " +
                        path + " from #" + ba.getBundleId());
        }
        Vector vec = ba.componentExists(path, onlyFirst);
        if (vec != null) {
          sais.add( new SearchActionItem( vec, ba ) );
          if (onlyFirst) break;
        }
      }
    }
    if (sais.size()>0) { /* 5 or 6 found the item */
      try {
        return action.get(sais, path, name, pkg, this );
      } catch (IOException ioe) {
        bpkgs.bundle.framework.listeners.frameworkError(bpkgs.bundle, ioe);
        return null;
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
        if (isSystemBundle(pbp.bundle)) {
          try {
            return pbp.bundle.framework.systemBundle.getClassLoader().loadClass(name);
          } catch (ClassNotFoundException e) {
            // continue
          }
        } else {
          BundleClassLoader cl = (BundleClassLoader)pbp.getClassLoader();
          if (cl != null) {
            if (debug.classLoader) {
              debug.println(this + " Dynamic import search: " +
                  path + " from #" + pbp.bundle.id);
            }
            return secure.callSearchFor(cl, name, pkg, path, action,
                onlyFirst, requestor, visited);
          }
        }
      }
      if (debug.classLoader) {
        debug.println(this + " No dynamic import: " + path);
      }
    }
    return null;
  }


  private static boolean isSystemBundle(BundleImpl bundle) {
    return bundle == bundle.framework.systemBundle;
  }


  static class SearchActionItem {
    public Vector /* Integer(class loader internal class path id) */ items;
    public BundleArchive ba;
    public SearchActionItem(Vector items, BundleArchive ba)
    {
      this.items = items;
      this.ba = ba;
    }
  }

  /**
   *  Search action
   */
  interface SearchAction {
    public abstract Object get(Vector /* SearchActionItem */ items,
                      String path, String name, String pkg,
                      BundleClassLoader cl )
      throws IOException ;
  }


  /**
   *  Search action for class searching
   */
  static final SearchAction classSearch = new SearchAction() {
      public Object get(Vector items, String path, String name, String pkg,
                        BundleClassLoader cl )
        throws IOException {
        SearchActionItem sai = (SearchActionItem) items.get(0);
        byte[] bytes = sai.ba.getClassBytes((Integer)sai.items.get(0), path);
        if (bytes != null) {
          if(cl.isBundlePatch()) {
            bytes = ClassPatcher.getInstance(cl).patch(name, bytes);
          }
          if (cl.debug.classLoader) {
            cl.debug.println("classLoader(#" + cl.bpkgs.bundle.id
                          + ") - load class: " + name);
          }
          synchronized (cl) {
            Class c = cl.findLoadedClass(name);
            if (c == null) {
              if (pkg != null) {
                if (cl.getPackage(pkg) == null) {
                  cl.definePackage(pkg, null, null, null, null,
                                   null, null, null);
                }
              }

	      // Use dalvik DexFile class loading when running
	      // on the dalvik VM
              if(bDalvik) {
		  try {
		      c = cl.getDexFileClass(name);
		  } catch (Exception e) {
		      throw new IOException("Failed to load dex class '" + name + "', " + e);
		  }
	      }
              
              if(c == null) {
                if (cl.protectionDomain == null) {
                  // Kaffe can't handle null protectiondomain
                  c = cl.defineClass(name, bytes, 0, bytes.length);
                } else {
                  c = cl.defineClass(name, bytes, 0, bytes.length,
                                   cl.protectionDomain);
                }
              }
            }
            return c;
          }
        }
        return null;
      }
    };

  
    /**
     * Load a class using the Dalvik DexFile API.
     * <p>
     * This relies in the bundle having a "classes.dex"
     * in its root
     * <p>
     * 
     * To create such a bundle, do
     * <ol>
     *  <li><code>dx --dex --output=classes.dex bundle.jar</code>
     *  <li><code>aapt add bundle.jar classes.dex</code>
     * </ol>
     */
    private Class getDexFileClass(String name) 
	throws Exception {
	
	if (debug.classLoader) {
	    debug.println("loading dex class " + name);
	}
	
	if (dexFile == null) {
	    File f  = new File(archive.getJarLocation());
	    dexFile = dexFileClassCons.newInstance(new Object[] { f });
	    if(debug.classLoader) {
		debug.println("created DexFile from " + f);
	    }
	}
    
	String path = name.replace('.','/');

	return (Class)dexFileClassLoadClassMethod
	    .invoke(dexFile, new Object[] { path, this });
    }



  
  /**
   *  Search action for resource searching
   */
  static final SearchAction resourceSearch = new SearchAction() {
      public Object get(Vector items, String path, String name, String pkg,
                        BundleClassLoader cl )
        throws IOException {

        Vector answer = new Vector();
        for(int i = 0; i < items.size(); i++) {
          SearchActionItem sai = (SearchActionItem) items.elementAt(i);
          for(int j = 0; j < sai.items.size(); j++) {
            int subId = ((Integer)sai.items.elementAt(j)).intValue();
            URL url = cl.bpkgs.bundle.getURL(cl.bpkgs.generation,
                                             sai.ba.getBundleId(),
                                             subId,
                                             path);
            if (url != null) {
              if (cl.debug.classLoader) {
                cl.debug.println("classLoader(#" + cl.bpkgs.bundle.id
                              + ") - found: " +
                              path + " -> " + url);
              }
              answer.addElement(url);
            } else {
              return null;
            }
          }
        }
        return answer.elements();
      }
    };


  /**
   * Find native library code to load. This method is called from
   * findLibrary(name) within a doPriviledged-block via the
   * secure object.
   *
   */
  String findLibrary0(final String name) {
    String res = archive.getNativeLibrary(name);
    if (res == null && fragments != null) {
      for (Iterator i = fragments.iterator(); i.hasNext(); ) {
        res = ((BundleArchive)i.next()).getNativeLibrary(name);
        if (res != null) {
          break;
        }
      }
    }
    return res;
  }

} //class
