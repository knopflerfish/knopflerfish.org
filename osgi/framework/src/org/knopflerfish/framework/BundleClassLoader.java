/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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
import java.security.*;

import java.util.Set;
import java.util.Dictionary;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Vector;

import java.util.jar.*;

import org.osgi.framework.*;

/**
 * Classloader for bundle JAR files.
 *
 * @author Jan Stein
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
   * Do we run in a Java 2 environment.
   */
  private static boolean isJava2;

  /**
   * Archives that we load code from.
   */
  private BundleArchive archive;

  /**
   * Imported java package.
   */
  private BundlePackages bpkgs = null;

  static {
    try {
      ClassLoader.class.getDeclaredMethod("findLibrary",
                                          new Class [] { String.class });
      isJava2 = true;
    } catch (NoSuchMethodException ignore) {
      isJava2 = false;
    }
  }


  /**
   * Create class loader for specified bundle.
   */
  BundleClassLoader(BundlePackages bpkgs, BundleArchive ba) {
    this.bpkgs = bpkgs;
    archive = ba;
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id
                    + ") - created new classloader");
    }
  }

  //
  // ClassLoader classes
  //

  /**
   * Find bundle class to load.
   * First check if this load comes from an imported package.
   * Otherwise load class from out bundle.
   *
   * @see java.lang.ClassLoader#findClass
   */
  protected Class findClass(String name) throws ClassNotFoundException {
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - find class: "
                    + name);
    }
    Class c = null;
    String pkg = null;
    int pos = name.lastIndexOf('.');
    if (pos != -1) {
      pkg = name.substring(0, pos);
      BundleImpl p = bpkgs.getProviderBundle(pkg);
      if (p != null) {
        if (p.getBundleId() != 0) {
          BundleClassLoader cl = p.getExporterClassLoader(pkg);
          if (cl != null) {
            c = cl.loadOwnClass(name, pkg);
            if (debug) {
              Debug.println("classLoader(#" + bpkgs.bundle.id
                            + ") - imported: " + name + " from #"
                            + p.getBundleId());
            }
            return c;
          }
        }
        if (debug) {
          Debug.println("classLoader(#" + bpkgs.bundle.id
                        + ") - no imported found: " + name);
        }
        throw new ClassNotFoundException(name);
      }
    }
    c = loadOwnClass(name, pkg);
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - loaded: " + name);
    }
    return c;
  }


  /**
   * Find native library code to load.
   *
   * @see java.lang.ClassLoader#findLibrary
   */
  protected String findLibrary(final String name) {
    String res = null;
    // Some storage kinds (e.g., expanded storage of sub-JARs)
    // requieres the Framework's permisisons to allow acces thus
    // we must call archive.getNativeLibrary(name) via doPrivileged().
    res = (String) AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return archive.getNativeLibrary(name);
        }
      });

    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - find library: "
                    + name + (res != null ? " OK" : " FAIL"));
    }
    return res;
  }


  /**
   * Returns an Enumeration of all the resources with the given name.
   *
   * @see java.lang.ClassLoader#findResources
   */
  protected Enumeration findResources(String name) {
    if (debug) {
      Debug.println("classLoader(#" + bpkgs.bundle.id + ") - find resources: "
                    + name);
    }
    BundleClassLoader cl = this;
    int pos = name.lastIndexOf('/');
    if (pos > 0) {
      int start = name.startsWith("/") ? 1 : 0;
      String pkg = name.substring(start, pos).replace('/', '.');
      BundleImpl p = bpkgs.getProviderBundle(pkg);
      if (p != null && p.getBundleId() != 0) {
        cl = p.getExporterClassLoader(pkg);
        if (debug) {
          Debug.println("classLoader(#" + bpkgs.bundle.id
                        + ") - imported resource: " + name + " from #"
                        + p.getBundleId());
        }
      }
    }
    return cl.findBundleResources(name);
  }


  /**
   * Finds the resource with the given name.
   *
   * @see java.lang.ClassLoader#findResource
   */
  protected URL findResource(String name) {
    Enumeration e = findResources(name);
    if (e.hasMoreElements()) {
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
      try {
        if (parent != null) {
          c = parent.loadClass(name);
        } else {
          c = findSystemClass(name);
        }
      } catch (ClassNotFoundException e) {
        c = findClass(name);
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
  synchronized Class loadOwnClass(final String name, String pkg)
    throws ClassNotFoundException
  {
    Class c = findLoadedClass(name);
    if (c == null) {
      if (debug) {
        Debug.println("classLoader(#" + bpkgs.bundle.id
                      + ") - try to find: " + name);
      }
      try {
        byte[] bytes = null;
        // Some storage kinds (e.g., expanded storage of sub-JARs)
        // requieres the Framework's permisisons to allow acces thus
        // we must call archive.getBytes() via doPrivileged().
        try {
          bytes = (byte[]) AccessController
            .doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws IOException {
                  return archive.getClassBytes(name);
                }
              });
        } catch (PrivilegedActionException pae) {
          // pae.getException() should be an instance of IOException,
          // as only "checked" exceptions will be "wrapped" in a
          // PrivilegedActionException.
          throw (IOException) pae.getException();
        }
        if (bytes != null) {
          if (debug) {
            Debug.println("classLoader(#" + bpkgs.bundle.id
                          + ") - load own class: " + name);
          }
          if (pkg != null) {
            if (getPackage(pkg) == null) {
              definePackage(pkg, null, null, null, null, null, null, null);
            }
          }
          if(bpkgs.bundle.protectionDomain == null) {
            // Kaffe can't handle null protectiondomain
            return defineClass(name, bytes, 0, bytes.length);
          } else {
            return defineClass(name, bytes, 0, bytes.length,
                               bpkgs.bundle.protectionDomain);
          }
        }
      } catch (IOException ioe) {
        bpkgs.bundle.framework.listeners.frameworkError(bpkgs.bundle, ioe);
      }
      throw new ClassNotFoundException(name);
    } else {
      if (debug) {
        Debug.println("classLoader(#" + bpkgs.bundle.id
                      + ") - load own class: " + name
                      + ", already loaded by " + this);
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
   * Get bundle package handler.
   *
   */
  BundlePackages getBpkgs() {
    return bpkgs;
  }

  //
  // Private methods
  //

  /**
   * Find resources within bundle.
   *
   * @return Enumeration of resources
   */
  private Enumeration findBundleResources(final String name) {
    Vector answer = new Vector(1);
    Vector items = (Vector)
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return archive.componentExists(name);
          }
        });
    if (items != null) {
      for(int i = 0; i < items.size(); i++) {
        final int jarId = items.size() == 1
          ? -1
          : ((Integer)items.elementAt(i)).intValue();

        try {
          /*
           * Fix for Java profiles which does not support
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
            final URLStreamHandler handler
              = bpkgs.bundle.framework.bundleURLStreamhandler;
            try {
              url = (URL) AccessController
                .doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws MalformedURLException {
                      return new URL
                        (BundleURLStreamHandler.PROTOCOL,
                         Long.toString(bpkgs.bundle.id),
                         jarId,
                         name.startsWith("/") ? name : ("/" + name),
                         handler);
                    }
                  });
            } catch (PrivilegedActionException pae) {
              // pae.getException() should be an instance of
              // MalformedURLException, as only "checked" exceptions
              // will be "wrapped" in a PrivilegedActionException.
              throw (MalformedURLException) pae.getException();
            }
          }
          if (debug) {
            Debug.println("classLoader(#" + bpkgs.bundle.id + ") - found: "
                          + name + " -> " + url);
          }
          answer.addElement(url);
        } catch (MalformedURLException ignore) {
          ignore.printStackTrace();
          // Return null since we couldn't construct a valid url.
          // TODO: Rewrite URL if we have special characters.
        }
      }
    }
    return answer.elements();
  }

}
