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
import java.util.ArrayList;
import java.util.Enumeration;
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
  //TODO have option to remove all such from production builds
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
   * Archive that we load code from.
   */
  private BundleArchive archive;

  /**
   * Imported and Exported java packages.
   */
  private BundlePackages bpkgs /*= null*/;
  
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
  BundleClassLoader(BundlePackages bpkgs, BundleArchive ba) {
        super(parent); //otherwise getResource will bypass OUR parent
    this.bpkgs = bpkgs;
    archive = ba;
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - created new classloader");
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
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - find class: " + name);
    }
    Class c = null;
    int pos = name.lastIndexOf('.');
    String pkg = null;
    if (pos != -1) {
      pkg = name.substring(0, pos);
      BundleImpl p = bpkgs.getProviderBundle(pkg);
      if (p != null) {
        if (p.getBundleId() != 0) {
          BundleClassLoader cl = p.getExporterClassLoader(pkg);
          if (cl != null) {
            c = cl.loadOwnClass(name, pkg, pos, cl != this);
            if (debug) {
              Debug.println("classLoader(#" + bpkgs.bundle.id + ") - imported: " + name +
                            " from #" + p.getBundleId());
            }
            return c;
          }
        }
        if (debug) {
          Debug.println("classLoader(#" + bpkgs.bundle.id + ") - no import found: " + name);
        }
        throw new ClassNotFoundException(name);
      }
    }
    try {
      c = loadOwnClass(name, pkg, pos, false);
      if (debug) {
        Debug.println("classLoader(#" + bpkgs.bundle.id + ") - loaded: " + name);
      }
    } catch (ClassNotFoundException cnf) {
      // NYI! Improve this so we do not have to throw an exception.
      if (pkg != null) {
        BundleImpl p = bpkgs.getDynamicProviderBundle(pkg);
        if (p != null) {
          if (p.getBundleId() != 0) {
            BundleClassLoader cl = p.getExporterClassLoader(pkg);
            if (cl != null) {
              c = cl.loadOwnClass(name, pkg, pos, cl != this);
              if (debug) {
                Debug.println("classLoader(#" + bpkgs.bundle.id + ") - dynamicly imported: " +
                              name + " from #" + p.getBundleId());
              }
              return c;
            }
          }
          if (debug) {
            Debug.println("classLoader(#" + bpkgs.bundle.id +
                          ") - no dynamic import found: " + name);
          }
        }
      }
      throw cnf;
    }
    return c;
  }


  /**
   * Find native library code to load.
   *
   * @see java.lang.ClassLoader#findLibrary
   */
  protected String findLibrary(String name) {
    String res = archive.getNativeLibrary(name);
    /*if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - find library: " + name
                    + (res != null ? " OK" : " FAIL"));
    }*/
    return res;
  }


  /**
   * Returns an Enumeration of all the resources with the given name.
   *
   * @see java.lang.ClassLoader#findResources
   */
  protected Enumeration findResources(String name) {
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - find resources: " + name);
    }
    BundleClassLoader cl = this;
    String pkg = null;
    int pos = name.lastIndexOf('/');
    if (pos > 0) {
      int start = name.startsWith("/") ? 1 : 0;
      pkg = name.substring(start, pos).replace('/', '.');
      BundleImpl p = bpkgs.getProviderBundle(pkg);
      if (p != null && p.getBundleId() != 0) {
          cl = p.getExporterClassLoader(pkg);
          if (debug) {
                  Debug.println("classLoader(#" + bpkgs.bundle.id + ") - imported resource: " + name +
                                        " from #" + p.getBundleId());
          }
      }
    }
    Enumeration res = cl.findBundleResources(name);
    if (res == null) {
      if (pkg != null) {
        BundleImpl p = bpkgs.getDynamicProviderBundle(pkg);
        if (p != null && p.getBundleId() != 0) {
          cl = p.getExporterClassLoader(pkg);
          res = cl.findBundleResources(name);
          if (debug) {
            Debug.println("classLoader(#" + bpkgs.bundle.id +
                          ") - dynamicly imported resource: " +
                          name + " from #" + p.getBundleId());
          }
        }
      }
    }
    return res;
  }


  /**
   * Finds the resource with the given name.
   *
   * @see java.lang.ClassLoader#findResource
   */
  protected URL findResource(String name) {
    Enumeration e = findResources(name);
    if (e != null && e.hasMoreElements()) {
      return (URL)e.nextElement();
    }
    return null;
  }
  

  /**
   * Find Class and load it. This function is abstract in PJava 1.2
   * so we define it here to work as closely as it can to Java 2.
   * Should work okey if we don't use the Java 2 stuff.
   * TODO: Make sure this is true!
   *
   * @param name the name of the class
   * @param resolve if <code>true</code> then resolve the class
   * @return the resulting <code>Class</code> object
   * @exception ClassNotFoundException if the class could not be found
   * @see java.lang.ClassLoader#loadClass
   */
  protected synchronized Class loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
    Class c = findLoadedClass(name);
    if (c == null) {
        //assert: parent != null
        if(name.startsWith("java.")){
                c = parent.loadClass(name);
        }
        else{
                if(isBootDelegated(name)){
                        try{
                                c = parent.loadClass(name);
                } 
                        catch (ClassNotFoundException e) {
                                c = findClass(name);
                        }
                }
                else{
                        c = findClass(name);
                }
        }
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
        //TODO same delegation logic as for loadClass  
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

  //
  // BundleClassLoader specific
  //

  /**
   * Load of class from our bundle.
   * First check if it is already loaded. Then try all archives in this
   * bundles classpath.
   */
  synchronized Class loadOwnClass(String name, String pkg, int pos, boolean filter)
    throws ClassNotFoundException
  {
    if (filter) {
      ExportPkg ep = bpkgs.getExport(pkg);
      String clazz = null;
      boolean ok = true;
      if (ep.include != null) {
        clazz = name.substring(pos + 1);
        for (Iterator i = ep.include.iterator(); i.hasNext(); ) {
          if (filterMatch((String)i.next(), clazz)) {
            break;
          }
          if (!i.hasNext()) {
            ok = false;
          }
        }
      }
      if (ok && ep.exclude != null) {
        if (clazz == null) {
          clazz = name.substring(pos + 1);
        }
        for (Iterator i = ep.exclude.iterator(); i.hasNext(); ) {
          if (filterMatch((String)i.next(), clazz)) {
            ok = false;
            break;
          }
        }
      }
      if (!ok) {
        throw new ClassNotFoundException(name);
      }
    }
    Class c = findLoadedClass(name);
    if (c == null) {
      if (debug) {
        Debug.println("classLoader(#" + bpkgs.bundle.id + ") - try to find: " + name);
      }
      try {
        byte[] bytes = archive.getClassBytes(name);
        if (bytes != null) {
          if (debug) {
            Debug.println("classLoader(#" + bpkgs.bundle.id + ") - load own class: " + name);
          }
          if (pkg != null) {
            if (getPackage(pkg) == null) {
              definePackage(pkg, null, null, null, null, null, null, null);
            }
          }
          return defineClass(name, bytes, 0, bytes.length);
        }
      } catch (IOException ioe) {
        bpkgs.bundle.framework.listeners.frameworkError(bpkgs.bundle, ioe);
      }
      throw new ClassNotFoundException(name);
    }
    else {
      if (debug) {
        Debug.println("classLoader(#" + bpkgs.bundle.id + ") - load own class: " +
                      name + ", already loaded by " + this);
      }
      return c;
    }
  }


  /**
   * Get bundle owning this class loader.
   */
  BundleImpl getBundle() {
    return bpkgs.bundle;
  }


  /**
   * Close down this classloader.
   * We don't give out any new classes. Perhaps we should
   * block all classloads.
   */
  void close() {
    archive = null;
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - cleared archives");
    }
  }


  /**
   * Close down this classloader and all its archives.
   * Purge all archives.
   *
   */
  void purge() {
    bpkgs.unregisterPackages(true);
    if(archive != null) {
      archive.purge();
    }
    close();
  }

  /**
   * Get the resource with the given name in this bundle.
   *
   */
  URL getBundleResource(String name) {
    return findResource(name);
  }
  
  /**
   * Get all the resources with the given name in this bundle.
   *
   */
  Enumeration getBundleResources(String name) {
    return findResources(name);
  }

  /**
   * Get bundle package handler.
   *
   */
  BundlePackages getBpkgs() {
    return bpkgs;
  }


  /**
   * Find resources within bundle.
   *
   * @return Enumeration of resources
   */
  Enumeration findBundleResources(String name) {
    Vector answer = new Vector(1);
    Vector items = archive.componentExists(name);
    if (items != null) {
      for(int i = 0; i < items.size(); i++) {
        int jarId = (items.size() == 1) ? -1
                                        : ((Integer)items.elementAt(i)).intValue();
        
        try {
          /*
           * Fix for Java profiles which do not support 
           * URL(String, String,int,String,URLStreamHandler).
           *  
           * These profiles must set the 
           * org.knopflerfish.osgi.registerbundleurlhandler property 
           * to 'true' so the BundleURLStreamHandler is added
           * to the Framework urlStreamHandlerFactory
           */
          URL url = null;
          if(Framework.REGISTERBUNDLEURLHANDLER) {
            url = new URL(BundleURLStreamHandler.PROTOCOL, 
                    Long.toString(bpkgs.bundle.id),
                    jarId,
                    name.startsWith("/") ? name : ("/" + name));
          } else {
                URLStreamHandler handler 
                    = bpkgs.bundle.framework.bundleURLStreamhandler;
                url = new URL(BundleURLStreamHandler.PROTOCOL, 
                    Long.toString(bpkgs.bundle.id),
                    jarId,
                    name.startsWith("/") ? name : ("/" + name),
                    handler);
          }
          if (debug) {
            Debug.println("classLoader(#" + bpkgs.bundle.id + ") - found: " + name + " -> " + url);
          }
          answer.addElement(url);
        } catch (MalformedURLException ignore) {
          ignore.printStackTrace();
          //Return null since we couldn't construct a valid url.
          return null;   
          // TODO: Rewrite URL if we have special characters.
        }
      }
    }
    else{
        return null;
    }
    return answer.elements();
  }

  private  boolean filterMatch(String filter, String s) {
    return patSubstr(s.toCharArray(), 0, filter.toCharArray(), 0);
  }

  private boolean patSubstr(char[] s, int si, char[] pat, int pi) {
    if (pat.length-pi == 0) 
      return s.length-si == 0;
    if (pat[pi] == '*') {
      pi++;
      for (;;) {
        if (patSubstr( s, si, pat, pi))
          return true;
        if (s.length-si == 0)
          return false;
        si++;
      }
    } else {
      if (s.length-si==0){
        return false;
      }
      if(s[si]!=pat[pi]){
        return false;
      }
      return patSubstr( s, ++si, pat, ++pi);
    }
  }

} //class
