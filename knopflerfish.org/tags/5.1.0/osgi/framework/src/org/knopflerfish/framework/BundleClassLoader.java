/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;

/**
 * Classloader for bundle JAR files.
 * 
 * @author Jan Stein, Philippe Laporte, Mats-Ola Persson, Gunna Ekolin
 * @author Vilmos Nebehaj (Android application support)
 */
final public class BundleClassLoader extends ClassLoader implements BundleReference {

  final static int ONLY_FIRST = 1;
  final static int LIST = 2;
  final static int ONLY_RECURSE = 4;
  final static int RECURSE = 256;
  final static int LOCAL = 512;

  /**
   * Framework class loader
   */
  final FrameworkContext fwCtx;

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
   * Imported and Exported java packages.
   */
  BundlePackages bpkgs;

  /**
   * Bundle class path for this classloader.
   */
  final private BundleClassPath classPath;

  // Array of bundles for which a classload is triggering activation.
  private static ThreadLocal<ArrayList<BundleImpl>> tlBundlesToActivate = new ThreadLocal<ArrayList<BundleImpl>>();

  // android/dalvik VM stuff
  private static Method dexFileClassLoadDexMethod;
  private static Method dexFileClassLoadClassMethod;

  // bDalvik will be set to true if we're running on the android
  // dalvik VM.
  // package protected to enable other parts of framework to check
  // for dalvik VM
  static boolean bDalvik = false;

  private ArrayList<Object> dexFile = null;

  static {
    try {
      Class<?> dexFileClass = null;
      try {
        dexFileClass = Class.forName("android.dalvik.DexFile");
      } catch (final Exception ex) {
        dexFileClass = Class.forName("dalvik.system.DexFile");
      }

      dexFileClassLoadDexMethod = dexFileClass.getMethod("loadDex", new Class[] { String.class,
                                                                                 String.class,
                                                                                 Integer.TYPE });

      dexFileClassLoadClassMethod = dexFileClass.getMethod("loadClass",
                                                           new Class[] { String.class,
                                                                        ClassLoader.class });

      bDalvik = true;
      // if(debug.classLoader) {
      // debug.println("running on dalvik VM");
      // }
    } catch (final Exception e) {
      dexFileClassLoadDexMethod = null;
      dexFileClassLoadClassMethod = null;
    }
  }

  Debug debug;

  /**
   * Create class loader for specified bundle.
   */
  BundleClassLoader(final BundleGeneration gen) throws BundleException {
    // otherwise getResource will bypass OUR parent
    super(gen.bundle.fwCtx.parentClassLoader);

    fwCtx = gen.bundle.fwCtx;
    debug = fwCtx.debug;
    secure = fwCtx.perm;
    protectionDomain = gen.getProtectionDomain();
    bpkgs = gen.bpkgs;
    archive = gen.archive;
    classPath = new BundleClassPath(archive, gen.fragments, fwCtx);
    fwCtx.bundleClassLoaderCreated(this);
    if (debug.classLoader) {
      debug.println(this + " Created new classloader");
    }
  }

  /**
   * Find bundle class to load. First check if this load comes from an imported
   * package. Otherwise load class from our bundle.
   * 
   * @see java.lang.ClassLoader#findClass
   */
  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    if (name.startsWith("java.")) {
      return fwCtx.parentClassLoader.loadClass(name);
    }
    if (fwCtx.isBootDelegated(name)) {
      try {
        final Class<?> bootDelegationCls = fwCtx.parentClassLoader.loadClass(name);
        if (debug.classLoader && bootDelegationCls != null) {
          debug.println(this + " findClass: " + name + " boot delegation: " + bootDelegationCls);
        }
        return bootDelegationCls;
      } catch (final ClassNotFoundException e) {
      }
    }
    String path;
    String pkg;
    final int pos = name.lastIndexOf('.');
    if (pos != -1) {
      path = name.replace('.', '/');
      pkg = name.substring(0, pos);
    } else {
      path = name;
      pkg = null;
    }
    Class<?> res = (Class<?>) secure.callSearchFor(this, name, pkg, path + ".class",
                                                   classSearch, ONLY_FIRST, this, null);
    if (res != null) {
      return res;
    }

    if (!fwCtx.props.STRICTBOOTCLASSLOADING) {
      if (isBootClassContext(name)) {
        if (debug.classLoader) {
          debug.println(this + " trying parent loader for class=" + name
                        + ", since it was loaded on the system loader itself");
        }
        res = fwCtx.parentClassLoader.loadClass(name);
        if (res != null) {
          if (debug.classLoader) {
            debug.println(this + " loaded " + name + " from " + fwCtx.parentClassLoader);
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
  @Override
  protected String findLibrary(String name) {
    final String res = secure.callFindLibrary0(this, name);
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
  @Override
  protected Enumeration<URL> findResources(String name) {
    // Step 1 and 2 are done by getResources
    return getBundleResources(name, false);
  }

  /**
   * Finds the resource with the given name.
   * 
   * @see java.lang.ClassLoader#findResource
   */
  @Override
  protected URL findResource(String name) {
    final Enumeration<URL> res = getBundleResources(name, true);
    if (res != null) {
      return res.nextElement();
    } else {
      return null;
    }
  }

  /**
   * Wrapper class around SecurityManager which exposes the getClassLoader()
   * method.
   */
  static class SecurityManagerExposer extends SecurityManager {
    @Override
    public Class<?>[] getClassContext() {
      return super.getClassContext();
    }
  }

  static protected SecurityManagerExposer smex = new SecurityManagerExposer();

  /**
   * @return <code>true</code> if the given class is not loaded by a bundle
   *         class loader, <code>false</false> otherwise.
   */
  private boolean isNonBundleClass(Class<?> cls) {
    return (this.getClass().getClassLoader() != cls.getClassLoader())
           && !ClassLoader.class.isAssignableFrom(cls) && !Class.class.equals(cls)
           && !Proxy.class.equals(cls);
  }

  /**
   * Check if the current call is made from a class loaded on the boot class
   * path (or rather, on a class loaded from something else than a bundle class
   * loader)
   * 
   * @param name
   *          The name of the class to load.
   */
  public boolean isBootClassContext(String name) {
    Class<?>[] classStack = smex.getClassContext();

    if (classStack == null) { // Android 4.0 returns null
      // TODO: Find a cheaper and better solution
      try {
        final StackTraceElement[] classNames = new Throwable().getStackTrace();
        classStack = new Class[classNames.length];
        for (int i = 1; i < classNames.length; i++)
          classStack[i] = Class.forName(classNames[i].getClassName());
      } catch (final ClassNotFoundException e) {
        return false;
      }
    }

    for (int i = 1; i < classStack.length; i++) {
      final Class<?> currentCls = classStack[i];
      if (isNonBundleClass(currentCls)) {
        final ClassLoader currentCL = currentCls.getClassLoader();

        // If any of the classloaders for the caller's class is
        // a BundleClassLoader, we're not in a VM class context
        // ANDROID FIX, android-7/8 unexpectedly returns
        // java.lang.BootClassLoader as the ClassLoader for the
        // BootClassLoader Class other jvm's return null
        for (ClassLoader cl = currentCL; cl != null && cl != cl.getClass().getClassLoader(); cl = cl.getClass()
                                                                                                    .getClassLoader()) {
          if (BundleClassLoader.class.isInstance(cl)) {
            return false;
          }
        }
        return !Bundle.class.isInstance(classStack[i - 1]);
      }
    }
    return false;
  }

  /**
   * Find Class and load it. This function is abstract in PJava 1.2 so we define
   * it here to work as closely as it can to Java 2. Should work okey if we
   * don't use the Java 2 stuff.
   * 
   * @param name
   *          the name of the class
   * @param resolve
   *          if <code>true</code> then resolve the class
   * @return the resulting <code>Class</code> object
   * @exception ClassNotFoundException
   *              if the class could not be found
   * @see java.lang.ClassLoader#loadClass
   */
  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      c = findClass(name);
    } else if (secure.getClassLoaderOf(c) == this) {
      // Handle bundles that are lazely started after having been
      // stopped. In this case the triggering classes will already
      // be loaded. Only consider classes loaded by this classloader
      final BundleImpl b = (BundleImpl) getBundle();
      if (b.triggersActivationCls(name)) {
        if (debug.lazy_activation) {
          debug.println(this + " lazy activation of #" + b.id + " triggered by loadClass("
                        + name + ")");
        }

        final ArrayList<BundleImpl> bundlesToActivate = tlBundlesToActivate.get();
        if (null == bundlesToActivate) {
          // Not part of a load chain; activate bundle here.
          if (debug.lazy_activation) {
            debug.println(this + " requesting lazy activation of #" + b.id);
          }
          try {
            secure.callFinalizeActivation(b);
          } catch (final BundleException e) {
            fwCtx.frameworkError(b, e);
          }
        } else {
          // add bundle to list of bundles to activate when the
          // initiator class has been loaded.
          boolean bundlePresent = false;
          for (int i = 0, size = bundlesToActivate.size(); i < size; i++) {
            final BundleImpl tmp = bundlesToActivate.get(i);
            if (tmp.id == b.id) {
              bundlePresent = true;
              break;
            }
          }
          if (!bundlePresent) {
            bundlesToActivate.add(b);
            if (debug.lazy_activation) {
              debug.println(this + " added #" + b.id + " to list of bundles to be activated.");
            }
          }
        }
      }
    }

    if (resolve) {
      resolveClass(c);
    }
    return c;
  }

  /**
   * Finds the resource with the given name. This is defined a little different
   * in PJava 1.2 versus Java 2. So we first try to use the super() version and
   * if it fails we try to find it in the local bundle.
   * 
   * @param name
   *          resource name
   * @return an URL to resource, or <code>null</code> if the resource could not
   *         be found or the caller doesn't have adequate privileges to get the
   *         resource.
   * @see java.lang.ClassLoader#getResource
   */
  @Override
  public URL getResource(String name) {
    if (debug.classLoader) {
      debug.println(this + " getResource: " + name);
    }
    URL res = null;
    if (name.startsWith("java/")) {
      res = fwCtx.parentClassLoader.getResource(name);
      if (debug.classLoader) {
        debug.println(this + " getResource: " + name + " file in java pkg: " + res);
      }
      return res;
    }

    if (fwCtx.isBootDelegatedResource(name)) {
      res = fwCtx.parentClassLoader.getResource(name);
      if (res != null) {
        if (debug.classLoader) {
          debug.println(this + " getResource: " + name + " boot delegation: " + res);
        }
        return res;
      }
    }

    res = findResource(name);
    if (debug.classLoader) {
      debug.println(this + " getResource: " + name + " bundle space: " + res);
    }
    return res;
  }

  // We would like to use the following implementation of
  // getResources() but that method is final in JDK 1.4
  // thus we can not redefine it here.
  /**
   * Finds all the resources with the given name. A resource is some data
   * (images, audio, text, etc) that can be accessed by class code in a way that
   * is independent of the location of the code.
   * 
   * <p>
   * The name of a resource is a <tt>/</tt>-separated path name that identifies
   * the resource.
   * 
   * @param name
   *          resource name
   * @return An enumeration of {@link java.net.URL <tt>URL</tt>} objects for the
   *         resource. If no resources could be found, the enumeration will be
   *         empty. Resources that the class loader doesn't have access to will
   *         not be in the enumeration.
   * 
   * @see java.lang.ClassLoader#getResources
   * @see org.osgi.framework.Bundle#getResources(String name)
   * 
   */
  public Enumeration<URL> getResourcesOSGi(String name) throws IOException {
    if (debug.classLoader) {
      debug.println(this + " getResources: " + name);
    }
    final int start = name.startsWith("/") ? 1 : 0;
    if (name.substring(start).startsWith("java/")) {
      return fwCtx.parentClassLoader.getResources(name);
    }

    Enumeration<URL> res = null;
    if (fwCtx.isBootDelegatedResource(name)) {
      res = fwCtx.parentClassLoader.getResources(name);
    }

    if (res == null || !res.hasMoreElements()) {
      res = findResources(name);
    }
    return res;
  }

  /**
   * Finds the resource with the given name and returns the InputStream. The
   * method is overridden to make sure it does the right thing.
   * 
   * @param name
   *          resource name
   * @return an InputStream to resource, or <code>null</code> if the resource
   *         could not be found or the caller doesn't have adequate privileges
   *         to get the resource.
   * @see java.lang.ClassLoader#getResourceAsStream
   */
  @Override
  public InputStream getResourceAsStream(String name) {
    try {
      final URL url = getResource(name);
      if (url != null) {
        return url.openStream();
      }
    } catch (final IOException ignore) {
    }
    return null;
  }

  /**
   * Return a string representing this object
   * 
   * @return A message string.
   */
  @Override
  public String toString() {
    return "BundleClassLoader("
    // +"fw=" +bpkgs.bundle.fwCtx.hashCode()
           + "id=" + bpkgs.bg.bundle.id + ",gen=" + bpkgs.bg.generation + ")";
  }

  // Implements BundleReference
  public Bundle getBundle() {
    return bpkgs.bg.bundle;
  }

  //
  // BundleClassLoader specific
  //

  /**
   * Close down this classloader. We don't give out any new classes. Perhaps we
   * should block all classloads.
   */
  void close() {
    archive = null;
    fwCtx.bundleClassLoaderClosed(this);
    if (debug.classLoader) {
      debug.println(this + " Cleared archives");
    }
  }

  /**
   * Get all the resources with the given name in this bundle.
   * 
   */
  Enumeration<URL> getBundleResources(String name, boolean onlyFirst) {
    // Removed this check pending outcome of OSGi bug 1489.
    // if (secure.okResourceAdminPerm(bpkgs.bundle)) {
    if (debug.classLoader) {
      debug.println(this + " Find bundle resource" + (onlyFirst ? "" : "s") + ": " + name);
    }
    String pkg = null;
    final int pos = name.lastIndexOf('/');
    if (pos > 0) {
      final int start = name.startsWith("/") ? 1 : 0;
      pkg = name.substring(start, pos).replace('/', '.');
    } else {
      pkg = null;
    }
    @SuppressWarnings("unchecked")
    final Enumeration<URL> res = (Enumeration<URL>) secure.callSearchFor(this, null, pkg, name,
                                                                         resourceSearch,
                                                                         onlyFirst ? ONLY_FIRST
                                                                                  : 0, this,
                                                                         null);
    return res;
    // } else {
    // return null;
    // }
  }

  /**
   * Get bundle package handler.
   * 
   */
  BundlePackages getBpkgs() {
    return bpkgs;
  }

  /**
   * Attach fragment to classloader.
   * 
   * @throws BundleException
   * 
   */
  void attachFragment(BundleGeneration gen) throws BundleException {
    if (debug.classLoader) {
      debug.println(this + " fragment attached update classpath");
    }
    classPath.attachFragment(gen);
  }

  Collection<String> listResources(String path, String filePattern, int options) {
    if (debug.classLoader) {
      debug.println(this + " List bundle resources: " + path + ", pattern=" + filePattern);
    }
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    // TODO handle . within path
    String pkg = path.replace('/', '.');

    @SuppressWarnings("unchecked")
    final Set<String> res = (Set<String>)
      secure.callSearchFor(this, filePattern, pkg, path, listSearch,
                           (options << 8) | LIST, this, null);
    return res;
  }

  //
  // Private
  //

  /**
   * Searches for and loads classes and resources according to OSGi search
   * order. When lazy activation of bundles are used this method will detect and
   * perform the activation. The actual searching and loading is done in
   * {@link #searchFor0()}
   * 
   * @param name
   *          Name of class or pattern we are looking for, null if we look for a
   *          resource
   * @param pkg
   *          Package name for item
   * @param path
   *          File path to item searched ("/" separated)
   * @param action
   *          Action to be taken when item is found
   * @param options
   *          Options controlling what should be included in search result.
   * 
   * @return Object returned from action class.
   */
  Object searchFor(String name, String pkg, String path, SearchAction action, int options,
                   BundleClassLoader requestor, HashSet<BundleClassLoader> visited) {
    try {
      final BundleImpl b = (BundleImpl) getBundle();
      boolean initiator = false;
      ArrayList<BundleImpl> bundlesToActivate = null;

      if (action == classSearch) {
        boolean bundlePresent = false;

        bundlesToActivate = tlBundlesToActivate.get();
        initiator = bundlesToActivate == null;
        if (initiator) {
          bundlesToActivate = new ArrayList<BundleImpl>();
          tlBundlesToActivate.set(bundlesToActivate);
        } else {
          bundlePresent = bundlesToActivate.contains(b);
        }
        if (!bundlePresent && b.triggersActivationPkg(pkg)) {
          bundlesToActivate.add(b);
          if (debug.lazy_activation) {
            debug.println(this + " lazy activation of #" + b.id + " triggered by searchFor("
                          + name + ")");
          }
        }
      }

      final Object res = searchFor0(name, pkg, path, action, options, requestor, visited);

      if (initiator) {
        tlBundlesToActivate.set(null);
        for (int i = bundlesToActivate.size() - 1; i >= 0; i--) {
          final BundleImpl tmp = bundlesToActivate.get(i);
          if (debug.lazy_activation) {
            debug.println(this + " requesting lazy activation of #" + tmp.id);
          }
          try {
            tmp.finalizeActivation();
          } catch (final BundleException e) {
            fwCtx.frameworkError(tmp, e);
          }
        }
      }
      return res;
    } catch (final Error te) {
      tlBundlesToActivate.set(null);
      throw te;
    }
  }

  /**
   * Search for classloader to use according to OSGi search order.
   * 
   * 3 If the class or resource is in a package that is imported using
   * Import-Package or was imported dynamically in a previous load, then the
   * request is delegated to the exporting bundles class loader; otherwise the
   * search continues with the next step. If the request is delegated to an
   * exporting class loader and the class or resource is not found, then the
   * search terminates and the request fails.
   * 
   * 4 If the class or resource is in a package that is imported from one or
   * more other bundles using Require-Bundle, the request is delegated to the
   * class loaders of the other bundles, in the order in which they are
   * specified in this bundles manifest. If the class or resource is not found,
   * then the search continues with the next step.
   * 
   * 5 The bundles own internal bundle class path is searched. If the class or
   * resource is not found, then the search continues with the next step.
   * 
   * 6 Each attached fragment's internal bundle class path is searched. The
   * fragments are searched in ascending bundle ID order. If the class or
   * resource is not found, then the search continues with the next step.
   * 
   * 7 If the class or resource is in a package that is exported by the bundle
   * or the package is imported by the bundle (using Import-Package or
   * Require-Bundle), then the search ends and the class or resource is not
   * found.
   * 
   * 8 Otherwise, if the class or resource is in a package that is imported
   * using DynamicImport-Package, then a dynamic import of the package is now
   * attempted. An exporter must conform to any implied package constraints. If
   * an appropriate exporter is found, a wire is established so that future
   * loads of the package are handled in Step 3. If a dynamic wire is not
   * established, then the request fails.
   * 
   * 9 If the dynamic import of the package is established, the request is
   * delegated to the exporting bundle's class loader. If the request is
   * delegated to an exporting class loader and the class or resource is not
   * found, then the search terminates and the request fails.
   * 
   * @param name
   *          Name of class or null if we look for a resource
   * @param pkg
   *          Package name for item
   * @param path
   *          File path to item searched ("/" separated)
   * @param action
   *          Action to be taken when item is found
   * @param onlyFirst
   *          Stop search when first matching item is found.
   * 
   * @return Object returned from action class.
   */
  Object searchFor0(String name, String pkg, String path, SearchAction action, int options,
                    BundleClassLoader requestor, HashSet<BundleClassLoader> visited) {
    BundlePackages pbp;
    Iterator<ExportPkg> ep;

    // TODO, Should this be an action method
    if (action == classSearch && requestor != this) {
      final Class<?> c = findLoadedClass(name);
      if (c != null) {
        return c;
      }
    }
    final boolean list = (options & LIST) != 0;
    final boolean local = (options & LOCAL) != 0;
    final boolean recurse = (options & RECURSE) != 0;
    Object answer = null;
    if (debug.classLoader) {
      debug.println(this + " Search for: " + path);
    }
    /* 3 */
    if (pkg != null) {
      pbp = bpkgs.getProviderBundlePackages(pkg);
      if (pbp != null) {
        final ClassLoader cl = pbp.getClassLoader();
        if (!local || cl == this) {
          if (isSystemBundle(pbp.bg.bundle)) {
            answer = frameworkSearchFor(cl, name, path, action);
            if (!recurse) {
              return answer;
            }
          } else {
            final BundleClassLoader bcl = (BundleClassLoader) cl;
            // Second check avoids a loop when a required bundle imports a
            // package from its requiring host that it self should
            // provide contents for to the requiring bundle.
            if (bcl != this && (visited == null || (bcl != null && !visited.contains(bcl)))) {
              if (bcl != null) {
                if (debug.classLoader) {
                  debug.println(this + " Import search: " + path + " from #" + pbp.bg.bundle.id);
                }
                answer = secure.callSearchFor(bcl, name, pkg, path, action, options & ~RECURSE,
                                              requestor, visited);
              } else {
                if (debug.classLoader) {
                  debug.println(this + " No import found: " + path);
                }
              }
              if (!recurse) {
                return answer;
              }
            }
          }
        }
        if (cl != this) {
          // Import checked we don't need to list any more in this directory.
          options |= ONLY_RECURSE;
        }
      } else if (!local) {
        /* 4 */
        final ArrayList<BundleGeneration> pl = bpkgs.getRequiredBundleGenerations(pkg);
        if (pl != null) {
          if (visited == null) {
            visited = new HashSet<BundleClassLoader>();
          }
          visited.add(this);
          for (final BundleGeneration pbg : pl) {
            final ClassLoader cl = pbg.getClassLoader();
            if (cl instanceof BundleClassLoader) {
              final BundleClassLoader bcl = (BundleClassLoader)cl;
              if (bcl != null && !visited.contains(bcl)) {
                if (debug.classLoader) {
                  debug.println(this + " Required bundle search: " + path + " from #"
                                + pbg.bundle.id);
                }
                answer = secure.callSearchFor(bcl, name, pkg, path, action, options,
                                              requestor, visited);
              }
            } else {
              answer = frameworkSearchFor(cl, name, path, action);
            }
            if (answer != null) {
              if (list || recurse) {
                break;
              } else {
                return answer;
              }
            }
          }
          if (debug.classLoader && answer == null) {
            debug.println(this + " Required bundle search: "
                          + "Not found, continuing with local search.");
          }
        }
      }
      ep = bpkgs.getExports(pkg);
    } else {
      ep = null;
    }
    /* 5 + 6 */
    if (this != requestor && ep != null) {
      // TODO should we block resources?
      if (action == classSearch) {
        boolean blocked = true;
        while (ep.hasNext()) {
          if (ep.next().checkFilter(name)) {
            blocked = false;
            break;
          }
        }
        if (blocked) {
          if (debug.classLoader) {
            debug.println(this + " Filter check blocked search for: " + name);
          }
          return null;
        }
      }
    }
    final Vector<FileArchive> av = classPath.componentExists(path, (options & ONLY_FIRST) != 0,
                                                             (options & LIST) != 0);
    if (av != null || recurse) {
      try {
        Object res = action.get(av, path, name, pkg, options, requestor, this);
        if (answer != null) {
          if (res != null) {
            @SuppressWarnings("unchecked")
            Collection<Object> ca = (Collection<Object>) answer;
            @SuppressWarnings("unchecked")
            Collection<Object> cr = (Collection<Object>) res;
            ca.addAll(cr);
          }
        } else {
          answer = res;
        }
        return answer;
      } catch (final ClassFormatError cfe) {
        // TODO: OSGI43 WeavingHook CT has some specific demands that
        // ClassFormatErrors are thrown that doesn't seem to be in the spec
        throw cfe;
      } catch (final IOException ioe) {
        fwCtx.frameworkError(bpkgs.bg.bundle, ioe);
        return null;
      }
    }

    /* 7 */
    if (ep != null || (options & LIST) != 0) {
      return null;
    }
    /* 8 */
    if (pkg != null) {
      pbp = bpkgs.getDynamicProviderBundlePackages(pkg);
      if (pbp != null) {
        /* 9 */
        if (isSystemBundle(pbp.bg.bundle)) {
          try {
            return fwCtx.systemBundle.getClassLoader().loadClass(name);
          } catch (final ClassNotFoundException e) {
            // continue
          }
        } else {
          final BundleClassLoader cl = (BundleClassLoader) pbp.getClassLoader();
          if (cl != null) {
            if (debug.classLoader) {
              debug.println(this + " Dynamic import search: " + path + " from #"
                            + pbp.bg.bundle.id);
            }
            return secure.callSearchFor(cl, name, pkg, path, action, options, requestor,
                                        visited);
          }
        }
      }
      if (debug.classLoader) {
        debug.println(this + " No dynamic import: " + path);
      }
    }
    return null;
  }

  /**
   * Get resources/classes from the framework.
   * Rewrite this since this solution will leak
   * resources that aren't bootdelegated.
   * 
   * @param cl
   * @param name
   * @param path
   * @param action
   * @return
   */
  private Object frameworkSearchFor(final ClassLoader cl, String name, String path,
                                    SearchAction action) {
    if (action == classSearch) {
      try {
        return cl.loadClass(name);
      } catch (final ClassNotFoundException e) {
      }
    } else if (action == resourceSearch) {
      try {
        return cl.getResources(path);
      } catch (IOException e) {
      }
    } else if (action == listSearch) {
      // TODO, listSearch
      throw new UnsupportedOperationException("listResources not available on system bundle");
    }
    return null;
  }

  private static boolean isSystemBundle(BundleImpl bundle) {
    return bundle == bundle.fwCtx.systemBundle;
  }

  /**
   * Search action
   */
  interface SearchAction {
    public abstract Object get(Vector<FileArchive> items, String path, String name, String pkg,
                               int options, BundleClassLoader requestor, BundleClassLoader cl)
                                                                                              throws IOException;
  }

  /**
   * Search action for class searching
   */
  static final SearchAction classSearch = new SearchAction() {
    public Object get(Vector<FileArchive> items, String path, String name, String pkg,
                      int options, BundleClassLoader requestor, BundleClassLoader cl)
                                                                                     throws IOException {
      byte[] bytes = items.get(0).getClassBytes(path);
      if (bytes != null) {
        if (cl.debug.classLoader) {
          cl.debug.println("classLoader(#" + cl.bpkgs.bg.bundle.id + ") - load class: " + name);
        }
        synchronized (cl) {
          Class<?> c = cl.findLoadedClass(name);
          if (c == null) {
            if (pkg != null) {
              if (cl.getPackage(pkg) == null) {
                cl.definePackage(pkg, null, null, null, null, null, null, null);
              }
            }

            // Use dalvik DexFile class loading when running
            // on the dalvik VM
            if (bDalvik) {
              try {
                c = cl.getDexFileClass(name);
              } catch (final Exception e) {
                throw new IOException("Failed to load dex class '" + name + "', " + e);
              }
            }

            if (c == null) {

              WeavingHooks.WovenClassImpl wc = null;
              if (cl != null && cl.bpkgs != null && cl.bpkgs.bg != null
                  && cl.bpkgs.bg.bundle != null) {
                wc = new WeavingHooks.WovenClassImpl(cl.bpkgs.bg.bundle, name, bytes);
                try {
                  cl.fwCtx.weavingHooks.callHooks(wc);
                  if (wc.hasAdditionalDynamicImports()) {
                    cl.bpkgs.parseDynamicImports(wc.getDynamicImportsAsString());
                  }
                  bytes = wc.getBytes();
                } catch (final ClassFormatError cfe) {
                  throw cfe;
                } catch (final Throwable t) {
                  final ClassFormatError cfe = new ClassFormatError(
                                                                    "Failed to call WeavingHooks for "
                                                                        + name);
                  cfe.initCause(t);
                  throw cfe;
                }
              }
              if (cl.protectionDomain == null) {
                // Kaffe can't handle null protectiondomain
                c = cl.defineClass(name, bytes, 0, bytes.length);
              } else {
                c = cl.defineClass(name, bytes, 0, bytes.length, cl.protectionDomain);
              }

              if (wc != null) {
                wc.setDefinedClass(c);
              }
            }
          }
          return c;
        }
      }
      return null;
    }
  };

  private void walkAndAddJars(List<Object> dexlist, String path) throws Exception {
    final File root = new File(path);
    final File[] list = root.listFiles();

    for (final File f : list) {
      if (f.isDirectory()) {
        walkAndAddJars(dexlist, f.getAbsolutePath());
      } else {
        if (f.getAbsolutePath().endsWith(".jar")) {
          if (debug.classLoader) {
            debug.println("creating DexFile from " + f.getAbsolutePath());
          }
          final Object dex = dexFileClassLoadDexMethod.invoke(null,
                                                              new Object[] {
                                                                            f.getAbsolutePath(),
                                                                            f.getAbsolutePath()
                                                                                + ".dexopt",
                                                                            new Integer(0) });
          dexlist.add(dex);
        }
      }
    }
  }

  /**
   * Load a class using the Dalvik DexFile API.
   * <p>
   * This relies in the bundle having a "classes.dex" in its root
   * <p>
   * TODO: We should create a specific bundle storage module for DEX files.
   * <p>
   * 
   * To create such a bundle, do
   * <ol>
   * <li><code>dx --dex --output=classes.dex bundle.jar</code>
   * <li><code>aapt add bundle.jar classes.dex</code>
   * </ol>
   */
  private Class<?> getDexFileClass(String name) throws Exception {
    if (debug.classLoader) {
      debug.println("loading dex class " + name);
    }

    if (dexFile == null) {
      dexFile = new ArrayList<Object>();
      final File f = new File(archive.getJarLocation());
      if (!f.isDirectory()) {
        if (debug.classLoader) {
          debug.println("creating DexFile from " + f);
        }
        final Object dex = dexFileClassLoadDexMethod.invoke(null,
                                                            new Object[] {
                                                                          f.getAbsolutePath(),
                                                                          f.getAbsolutePath()
                                                                              + ".dexopt",
                                                                          new Integer(0) });
        dexFile.add(dex);
        if (debug.classLoader) {
          debug.println("created DexFile from " + f);
        }
      } else {
        // if it has an internal jar file then it was unpacked into a folder
        if (debug.classLoader) {
          System.err.println("creating DexFile from " + f + "/classes.dex");
        }
        final Object dex = dexFileClassLoadDexMethod.invoke(null,
                                                            new Object[] {
                                                                          f.getAbsolutePath()
                                                                              + "/classes.dex",
                                                                          f.getAbsolutePath()
                                                                              + ".dexopt",
                                                                          new Integer(0) });
        dexFile.add(dex);
        if (debug.classLoader) {
          debug.println("created DexFile from " + f + File.pathSeparatorChar + "classes.dex");
        }
        // check for internal jar files
        walkAndAddJars(dexFile, f.getAbsolutePath());
      }
    }

    final String path = name.replace('.', '/');

    final Iterator<Object> i = dexFile.iterator();
    while (i.hasNext()) {
      final Object dex = i.next();
      if (debug.classLoader) {
        debug.println("trying to load " + path + " from " + dex);
      }
      try {
        final Class<?> clz = (Class<?>) dexFileClassLoadClassMethod.invoke(dex,
                                                                           new Object[] { path,
                                                                                         this });
        if (clz != null) {
          if (debug.classLoader) {
            debug.println("loaded " + path + " from " + dex);
          }
          return clz;
        }
      } catch (final Exception e) {
      }
      if (debug.classLoader) {
        debug.println("failed to load " + path + " from " + dex);
      }
    }

    throw new ClassNotFoundException("could not find dex class " + path);
  }

  /**
   * Search action for resource searching
   */
  static final SearchAction resourceSearch = new SearchAction() {
    public Object get(Vector<FileArchive> items, String path, String name, String pkg,
                      int options, BundleClassLoader requestor, BundleClassLoader cl)
                                                                                     throws IOException {
      final Vector<URL> answer = new Vector<URL>();
      for (int i = 0; i < items.size(); i++) {
        final FileArchive fa = items.elementAt(i);
        final URL url = fa.getBundleGeneration().getURL(fa.getSubId(), path);
        if (url != null) {
          if (cl.debug.classLoader) {
            cl.debug.println("classLoader(#" + cl.bpkgs.bg.bundle.id + ") - found: " + path
                             + " -> " + url);
          }
          answer.addElement(url);
        } else {
          return null;
        }
      }
      return answer.elements();
    }
  };

  /**
   * Search action for listResource searching
   */
  static final SearchAction listSearch = new SearchAction() {
    public Object get(Vector<FileArchive> items, String path, String name, String pkg,
                      int options, BundleClassLoader requestor, BundleClassLoader cl)
      throws IOException
    {
      Set<String> answer = new HashSet<String>();
      boolean onlyRecurse = (options & ONLY_RECURSE) != 0;
      HashSet<String> scanned = new HashSet<String>();
      for (String subPkg : cl.bpkgs.getSubProvider(pkg)) {
        if ((options & RECURSE) != 0) {
          String next = path.length() > 0 ? path + "/" + subPkg : subPkg;
          @SuppressWarnings("unchecked")
          Set<String> subAnswer = (Set<String>) cl.searchFor(name, next.replace('/', '.'),
                                                             next, listSearch,
                                                             options & ~ONLY_RECURSE,
                                                             requestor, null);
          if (subAnswer != null) {
            answer.addAll(subAnswer);
          }
        }
        if (!onlyRecurse && (name == null || Util.filterMatch(name, subPkg))) {
          answer.add(path + "/" + subPkg);
        }
        scanned.add(subPkg + "/");
      }
      if (items != null) {
        for (FileArchive fa : items) {
          for (String e : fa.listDir(path)) {
            if (scanned.contains(e)) {
              if (cl.debug.classLoader) {
                cl.debug.println("classLoader(#" + cl.bpkgs.bg.bundle.id + ") - list search skip: " + e);
              }
              continue;
            } else if (cl.debug.classLoader) {
              cl.debug.println("classLoader(#" + cl.bpkgs.bg.bundle.id +
                  ") - list search check: " + e + (onlyRecurse ? " (scan)" : ""));
            }
            if (e.endsWith("/")) {
              e = e.substring(0, e.length() - 1);
              if ((options & RECURSE) != 0) {
                String next = path.length() > 0 ? path + "/" + e : e;
                @SuppressWarnings("unchecked")
                Set<String> subAnswer = (Set<String>) cl.searchFor(name,
                                                                   next.replace('/', '.'),
                                                                   next, listSearch,
                                                                   options & ~ONLY_RECURSE,
                                                                   requestor, null);
                if (subAnswer != null) {
                  answer.addAll(subAnswer);
                }
              }
            }
            if (!onlyRecurse && (name == null || Util.filterMatch(name, e))) {
              answer.add(path + "/" + e);
              if (cl.debug.classLoader) {
                cl.debug.println("classLoader(#" + cl.bpkgs.bg.bundle.id + ") - list search match: " + e);
              }
            }
          }
        }
      }
      return answer;
    }
  };

  /**
   * Find native library code to load. This method is called from
   * findLibrary(name) within a doPriviledged-block via the secure object.
   * 
   */
  String findLibrary0(final String name) {
    return classPath.getNativeLibrary(name);
  }

}
