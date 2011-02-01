/*
 * Copyright (c) 2010-2011, KNOPFLERFISH project
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
import java.util.*;

import org.osgi.framework.*;

/**
 * Bundle generation specific data.
 * 
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 */

public class BundleGeneration implements Comparable {

  /**
   * Bundle
   */
  final BundleImpl bundle;

  /**
   * Packages that the bundle wants to export and import.
   */
  final BundlePackages bpkgs;

  /**
   * Bundle JAR data.
   */
  final BundleArchive archive;

  /**
   * Generation of BundlePackages.
   */
  final int generation;

  /**
   * Does bundle have a version 2 manifest.
   */
  final boolean v2Manifest;

  /**
   * Bundle symbolic name.
   */
  final String symbolicName;

  /**
   * Bundle is a singleton.
   */
  final boolean singleton;

  /**
   * Bundle version.
   */
  final Version version;

  /**
   * This bundle's fragment attach policy.
   */
  final String attachPolicy;

  /**
   * Fragment description. This is null when the bundle isn't a fragment bundle.
   */
  final Fragment fragment;

  /**
   * True when this bundle has its activation policy set to "lazy"
   */
  final boolean lazyActivation;
  final private HashSet lazyIncludes;
  final private HashSet lazyExcludes;

  /**
   * Time when bundle was last created.
   * 
   */
  final long timeStamp;

  /**
   * All fragment bundles this bundle hosts.
   */
  Vector /* BundleGeneration */fragments = null;

  /**
   * Stores the raw manifest headers.
   */
  private volatile HeaderDictionary cachedRawHeaders = null;

  /**
   * Classloader for bundle.
   */
  private volatile ClassLoader classLoader;

  /**
   * Bundle protect domain. Will allways be <tt>null</tt> for the system bundle,
   * methods requireing access to it must be overridden in the SystemBundle
   * class.
   */
  private ProtectionDomain protectionDomain;


  /**
   * Construct a new BundleGeneration for the System Bundle.
   * 
   * @param b BundleImpl this bundle data.
   */
  BundleGeneration(BundleImpl b, String exportStr) {
    bundle = b;
    archive = null;
    generation = 0;
    v2Manifest = true;
    symbolicName = Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
    singleton = false;
    version = new Version(Main.readRelease());
    attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
    fragment = null;
    protectionDomain = null;
    lazyActivation = false;
    lazyIncludes = null;
    lazyExcludes = null;
    timeStamp = System.currentTimeMillis();
    bpkgs = new BundlePackages(this, exportStr);
    classLoader = b.getClassLoader();
  }


  /**
   * Construct a new BundleGeneration.
   * 
   * Construct a new Bundle based on a BundleArchive.
   * 
   * @param b BundleImpl this bundle data.
   * @param ba Bundle archive with holding the contents of the bundle.
   * @param checkContext AccessConrolContext to do permission checks against.
   * 
   * @exception IOException If we fail to read and store our JAR bundle or if
   *              the input data is corrupted.
   * @exception SecurityException If we don't have permission to install
   *              extension.
   * @exception IllegalArgumentException Faulty manifest for bundle
   */
  BundleGeneration(BundleImpl b, BundleArchive ba, BundleGeneration prev) {
    bundle = b;
    generation = (prev != null ? prev.generation : -1) + 1;
    archive = ba;
    checkCertificates();
    // TBD, v2Manifest unnecessary to cache?
    String mv = archive.getAttribute(Constants.BUNDLE_MANIFESTVERSION);
    v2Manifest = mv != null && mv.trim().equals("2");
    Iterator i = Util.parseEntries(Constants.BUNDLE_SYMBOLICNAME,
        archive.getAttribute(Constants.BUNDLE_SYMBOLICNAME), true, true, true);
    Map e = null;
    if (i.hasNext()) {
      e = (Map)i.next();
      symbolicName = (String)e.get("$key");
    } else {
      if (v2Manifest) {
        throw new IllegalArgumentException("Bundle has no symbolic name, location="
            + bundle.location);
      } else {
        symbolicName = null;
      }
    }
    String mbv = archive.getAttribute(Constants.BUNDLE_VERSION);
    Version newVer = Version.emptyVersion;
    if (mbv != null) {
      try {
        newVer = new Version(mbv);
      } catch (Throwable ee) {
        if (v2Manifest) {
          throw new IllegalArgumentException("Bundle does not specify a valid "
              + Constants.BUNDLE_VERSION + " header. Got exception: " + ee.getMessage());
        }
      }
    }
    version = newVer;

    if (e != null) {
      singleton = "true".equals((String)e.get(Constants.SINGLETON_DIRECTIVE));
      BundleImpl snb = b.fwCtx.bundles.getBundle(symbolicName, version);
      String tmp = (String)e.get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
      attachPolicy = tmp == null ? Constants.FRAGMENT_ATTACHMENT_ALWAYS : tmp;
      // TBD! Should we allow update to same version?
      if (snb != null && snb != bundle) {
        throw new IllegalArgumentException("Bundle with same symbolic name and version "
            + "is already installed (" + symbolicName + ", " + version);
      }
    } else {
      attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
      singleton = false;
    }

    i = Util.parseEntries(Constants.FRAGMENT_HOST,
        archive.getAttribute(Constants.FRAGMENT_HOST), true, true, true);
    if (i.hasNext()) {
      if (archive.getAttribute(Constants.BUNDLE_ACTIVATOR) != null) {
        throw new IllegalArgumentException("A fragment bundle can not have a Bundle-Activator.");
      }

      e = (Map)i.next();
      String extension = (String)e.get(Constants.EXTENSION_DIRECTIVE);
      String key = (String)e.get("$key");

      if (Constants.EXTENSION_FRAMEWORK.equals(extension)
          || Constants.EXTENSION_BOOTCLASSPATH.equals(extension)) {
        // an extension bundle must target the system bundle.
        if (!Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(key)
            && !"org.knopflerfish.framework".equals(key)) {
          throw new IllegalArgumentException("An extension bundle must target "
              + "the system bundle(=" + Constants.SYSTEM_BUNDLE_SYMBOLICNAME + ")");
        }

        if (archive.getAttribute(Constants.IMPORT_PACKAGE) != null
            || archive.getAttribute(Constants.REQUIRE_BUNDLE) != null
            || archive.getAttribute(Constants.BUNDLE_NATIVECODE) != null
            || archive.getAttribute(Constants.DYNAMICIMPORT_PACKAGE) != null
            || archive.getAttribute(Constants.BUNDLE_ACTIVATOR) != null) {
          throw new IllegalArgumentException("An extension bundle cannot specify: "
              + Constants.IMPORT_PACKAGE + ", " + Constants.REQUIRE_BUNDLE + ", "
              + Constants.BUNDLE_NATIVECODE + ", " + Constants.DYNAMICIMPORT_PACKAGE + " or "
              + Constants.BUNDLE_ACTIVATOR);
        }

        if (!bundle.fwCtx.props.getBooleanProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION)
            && Constants.EXTENSION_FRAMEWORK.equals(extension)) {
          throw new UnsupportedOperationException(
              "Framework extension bundles are not supported "
                  + "by this framework. Consult the documentation");
        }
        if (!bundle.fwCtx.props.getBooleanProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION)
            && Constants.EXTENSION_BOOTCLASSPATH.equals(extension)) {
          throw new UnsupportedOperationException(
              "Bootclasspath extension bundles are not supported "
                  + "by this framework. Consult the documentation");
        }
      } else {
        if (extension != null) {
          throw new IllegalArgumentException("Did not recognize directive "
              + Constants.EXTENSION_DIRECTIVE + ":=" + extension + ".");
        }
      }

      fragment = new Fragment(key, extension,
          (String)e.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
    } else {
      fragment = null;
    }

    i = Util.parseEntries(Constants.BUNDLE_ACTIVATIONPOLICY,
        archive.getAttribute(Constants.BUNDLE_ACTIVATIONPOLICY), true, true, true);
    if (i.hasNext()) {
      e = (Map)i.next();
      lazyActivation = Constants.ACTIVATION_LAZY.equals(e.get("$key"));
      if (lazyActivation) {
        if (e.containsKey(Constants.INCLUDE_DIRECTIVE)) {
          lazyIncludes = Util.parseEnumeration(Constants.INCLUDE_DIRECTIVE,
              (String)e.get(Constants.INCLUDE_DIRECTIVE));
        } else {
          lazyIncludes = null;
        }
        if (e.containsKey(Constants.EXCLUDE_DIRECTIVE)) {
          lazyExcludes = Util.parseEnumeration(Constants.EXCLUDE_DIRECTIVE,
              (String)e.get(Constants.EXCLUDE_DIRECTIVE));

          if (lazyIncludes != null) {
            for (Iterator excsIter = lazyExcludes.iterator(); excsIter.hasNext();) {
              String entry = (String)excsIter.next();
              if (lazyIncludes.contains(entry)) {
                throw new IllegalArgumentException("Conflicting " + Constants.INCLUDE_DIRECTIVE
                    + "/" + Constants.EXCLUDE_DIRECTIVE + " entries in "
                    + Constants.BUNDLE_ACTIVATIONPOLICY + ": '" + entry
                    + "' both included and excluded");
              }
            }
          }
        } else {
          lazyExcludes = null;
        }
      } else {
        lazyIncludes = null;
        lazyExcludes = null;
      }
    } else {
      lazyActivation = false;
      lazyIncludes = null;
      lazyExcludes = null;
    }
    bpkgs = new BundlePackages(this);
    try {
      if (b.fwCtx.startLevelController == null) {
        archive.setStartLevel(0);
      } else {
        if (archive.getStartLevel() == -1) {
          archive.setStartLevel(b.fwCtx.startLevelController.getInitialBundleStartLevel());
        }
      }
    } catch (Exception exc) {
      b.fwCtx.listeners.frameworkError(b, exc);
    }
    archive.setBundleGeneration(this);
    long lastModified = prev != null ? 0 : archive.getLastModified();
    if (lastModified == 0) {
      lastModified = System.currentTimeMillis();
    }
    timeStamp = lastModified;
  }


  /**
   * Construct a new BundleGeneration for an uninstalled bundle.
   * 
   * @param prev Previous BundleGeneration.
   */
  BundleGeneration(BundleGeneration prev) {
    bundle = prev.bundle;
    archive = null;
    generation = prev.generation + 1;
    v2Manifest = prev.v2Manifest;
    symbolicName = prev.symbolicName;
    singleton = prev.singleton;
    version = prev.version;
    attachPolicy = Constants.FRAGMENT_ATTACHMENT_NEVER;
    fragment = null;
    protectionDomain = null;
    lazyActivation = false;
    lazyIncludes = null;
    lazyExcludes = null;
    timeStamp = System.currentTimeMillis();
    bpkgs = null;
    cachedRawHeaders = prev.cachedRawHeaders;
    classLoader = null;
  }


  /**
   * Compares this <code>BundleGeneration</code> object to another object. It
   * compares the bundle identifier value.
   * 
   */
  public int compareTo(Object obj) {
    long diff = bundle.id - ((BundleGeneration)obj).bundle.id;
    if (diff < 0) {
      return -1;
    } else if (diff == 0) {
      return 0;
    } else {
      return 1;
    }
  }


  //
  // Package methods
  //

  /**
   * Finnish construction by doing protectDomain creation and permission checks.
   * 
   * @return Bundles classloader.
   */
  void checkPermissions(Object checkContext) {
    protectionDomain = bundle.secure.getProtectionDomain(this);
    try {
      bundle.secure.checkExtensionLifecycleAdminPerm(bundle, checkContext);
      if (isExtension() && !bundle.secure.okAllPerm(bundle)) {
        throw new IllegalArgumentException("An extension bundle must have AllPermission");
      }
    } catch (RuntimeException re) {
      purgeProtectionDomain();
      throw re;
    }
    // Bundle ok save timeStamp
    if (archive.getLastModified() != timeStamp) {
      try {
        archive.setLastModified(timeStamp);
      } catch (IOException ioe) {
        bundle.fwCtx.listeners.frameworkError(bundle, ioe);
      }
    }
  }


  /**
   *
   */
  boolean resolvePackages() throws BundleException {
    ArrayList detached = null;
    attachFragments();
    while (true) {
      if (bpkgs.resolvePackages()) {
        if (detached != null) {
          // TBD should we report fragment that failed to attach
          for (int i = detached.size() - 2; i >= 0; i--) {
            BundleGeneration bg = (BundleGeneration)detached.get(i);
            if (bg.bundle.attachToFragmentHost(this)) {
              fragments.add(bg);
            }
          }
        }
        classLoader = bundle.secure.newBundleClassLoader(this);
        return true;
      }
      if (fragments != null) {
        if (bundle.fwCtx.debug.packages) {
          bundle.fwCtx.debug.println("Resolve failed, remove last fragment and retry");
        }
        if (detached == null) {
          detached = new ArrayList();
        }
        detached.add(detachLastFragment());
        if (fragments.isEmpty()) {
          fragments = null;
        }
      } else {
        break;
      }
    }
    return false;
  }


  /**
   * @param packageName
   * @return true if this package name should trigger activation of a lazyBundle
   */
  boolean isPkgActivationTrigger(String packageName) {
    return lazyActivation
        && ((lazyIncludes == null && lazyExcludes == null)
            || (lazyIncludes != null && lazyIncludes.contains(packageName)) || (lazyExcludes != null && !lazyExcludes
            .contains(packageName)));
  }


  /**
   * Get class loader for this bundle generation.
   * 
   * @return Bundles classloader.
   */
  ClassLoader getClassLoader() {
    return classLoader;
  }


  /**
   * Get protection domain for this bundle generation.
   * 
   * @return ProtectionDomain
   */
  ProtectionDomain getProtectionDomain() {
    return protectionDomain;
  }


  /**
   * Attaches all relevant fragments to this bundle.
   */
  private void attachFragments() {
    if (!attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
      Collection hosting = bundle.fwCtx.bundles.getFragmentBundles(bundle);
      if (hosting.size() > 0 && bundle.secure.okHostBundlePerm(bundle)) {
        // retrieve all fragments this bundle host
        for (Iterator iter = hosting.iterator(); iter.hasNext();) {
          BundleGeneration fbg = (BundleGeneration)iter.next();
          fbg.bundle.attachToFragmentHost(this);
        }
      }
    }
  }


  /**
   * Attaches a fragment to this bundle generation.
   */
  void attachFragment(BundleGeneration fragmentBundle) {
    if (attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
      throw new IllegalStateException("Bundle does not allow fragments to attach");
    }
    if (attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_RESOLVETIME)
        && (bundle.state & BundleImpl.RESOLVED_FLAGS) != 0) {
      throw new IllegalStateException("Bundle does not allow fragments to attach dynamicly");
    }

    String failReason = bpkgs.attachFragment(fragmentBundle.bpkgs);
    if (failReason != null) {
      throw new IllegalStateException("Failed to attach: " + failReason);
    }
    if (classLoader != null && classLoader instanceof BundleClassLoader) {
      try {
        ((BundleClassLoader)classLoader).attachFragment(fragmentBundle);
      } catch (BundleException be) {
        throw new IllegalStateException(be.getMessage());
      }
    }
    if (bundle.fwCtx.debug.packages) {
      bundle.fwCtx.debug.println("Fragment(" + fragmentBundle.bundle + ") attached to host(id="
          + bundle.id + ",gen=" + generation + ")");
    }
    if (fragments == null) {
      fragments = new Vector();
    }
    int i = 0;
    // TODO: use sorted list!?
    for (; i < fragments.size(); i++) {
      BundleGeneration b = (BundleGeneration)fragments.get(i);
      if (b.bundle.id > fragmentBundle.bundle.id) {
        break;
      }
    }
    fragments.add(i, fragmentBundle);
  }


  /**
   * Detach last fragment from this bundle.
   * 
   * @return BundleGeneration for fragment removed, otherwise null.
   */
  private BundleGeneration detachLastFragment() {
    // NYI! extensions
    int last = fragments.size() - 1;
    if (last >= 0) {
      BundleGeneration fbg = (BundleGeneration)fragments.remove(last);
      bpkgs.detachFragment(fbg);
      if (bundle.fwCtx.debug.packages) {
        bundle.fwCtx.debug.println("Fragment(id=" + fbg.bundle.id + ") detached from host(id="
            + bundle.id + ",gen=" + generation + ")");
      }
      if (fbg.bundle.state != Bundle.UNINSTALLED) {
        fbg.fragment.removeHost(this);
        if (!fbg.fragment.hasHosts()) {
          if (fbg == fbg.bundle.gen) {
            fbg.bundle.setStateInstalled(true);
          } else {
            // ... NYI zombie detach
          }
        }
      }
      return fbg;
    }
    return null;
  }


  void updateStateFragments() {
    if (fragments != null) {
      for (Iterator i = fragments.iterator(); i.hasNext();) {
        BundleImpl fb = ((BundleGeneration)i.next()).bundle;
        fb.getUpdatedState();
      }
    }
  }


  /**
   * Checks if this bundle is a fragment
   */
  Vector getHosts() {
    return fragment != null ? fragment.getHosts() : null;
  }


  /**
   * Checks if this bundle is a fragment
   */
  boolean isFragment() {
    return fragment != null;
  }


  /**
   * Determines whether this bundle is a fragment host or not.
   */
  boolean isFragmentHost() {
    return fragments != null && fragments.size() > 0;
  }


  /**
   * Checks if this bundle is a boot class path extension bundle
   */
  boolean isBootClassPathExtension() {
    return fragment != null && fragment.extension != null
        && fragment.extension.equals(Constants.EXTENSION_BOOTCLASSPATH);
  }


  /**
   * Checks if this bundle is an extension bundle
   */
  boolean isExtension() {
    return fragment != null && fragment.extension != null;
  }


  /**
   * Get locale dictionary for this bundle.
   */
  private Dictionary getLocaleDictionary(String locale, String baseName) {
    final String defaultLocale = Locale.getDefault().toString();

    if (locale == null) {
      locale = defaultLocale;
    } else if (locale.equals("")) {
      return null;
    }
    Hashtable localization_entries = new Hashtable();
    // TBD, should we do like this and allow mixed locales?
    if (baseName == null) {
      baseName = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
    }
    Vector h = getHosts();
    if (h != null) {
      BundleGeneration best;
      while (true) {
        try {
          best = null;
          for (Iterator i = h.iterator(); i.hasNext();) {
            BundleGeneration bg = (BundleGeneration)i.next();
            if (best == null || bg.version.compareTo(best.version) > 0) {
              best = bg;
            }
          }
          break;
        } catch (ConcurrentModificationException ignore) {
        }
      }
      if (best == bundle.fwCtx.systemBundle.gen) {
        bundle.fwCtx.systemBundle.readLocalization("", localization_entries, baseName);
        bundle.fwCtx.systemBundle.readLocalization(defaultLocale, localization_entries,
            baseName);
        if (!locale.equals(defaultLocale)) {
          bundle.fwCtx.systemBundle.readLocalization(locale, localization_entries, baseName);
        }
        return localization_entries;
      } else if (best != null) {
        return best.getLocaleDictionary(locale, baseName);
      }
      // Didn't find a host, fall through.
    }

    readLocalization("", localization_entries, baseName);
    readLocalization(defaultLocale, localization_entries, baseName);
    if (!locale.equals(defaultLocale)) {
      readLocalization(locale, localization_entries, baseName);
    }
    return localization_entries;
  }


  /**
   *
   */
  HeaderDictionary getHeaders0(String locale) {
    if (cachedRawHeaders == null) {
      cachedRawHeaders = archive.getUnlocalizedAttributes();
    }
    if ("".equals(locale)) {
      return (HeaderDictionary)cachedRawHeaders.clone();
    }
    if (bundle.state != Bundle.UNINSTALLED) {
      String base = (String)cachedRawHeaders.get(Constants.BUNDLE_LOCALIZATION);
      try {
        return localize(getLocaleDictionary(locale, base));
      } catch (RuntimeException e) {
        // We assume that we got an exception because we got
        // state change during the operation. Check!
        // NYI
        if (true) {
          throw e;
        }
      }
    }
    return null;
  }


  /**
   *
   */
  void addResourceEntries(Vector res, String path, String pattern, boolean recurse) {
    if (archive == null) {
      // We are not called as systembundle
      throw new IllegalStateException("Bundle is in UNINSTALLED state");
    }
    Enumeration e = archive.findResourcesPath(path);
    if (e != null) {
      while (e.hasMoreElements()) {
        String fp = (String)e.nextElement();
        boolean isDirectory = fp.endsWith("/");
        int searchBackwardFrom = fp.length() - 1;
        if (isDirectory) {
          // Skip last / in case of directories
          searchBackwardFrom = searchBackwardFrom - 1;
        }
        int l = fp.lastIndexOf('/', searchBackwardFrom);
        String lastComponentOfPath = fp.substring(l + 1, searchBackwardFrom + 1);
        if (pattern == null || Util.filterMatch(pattern, lastComponentOfPath)) {
          URL url = getURL(0, fp);
          if (url != null) {
            res.add(url);
          }
        }
        if (isDirectory && recurse) {
          addResourceEntries(res, fp, pattern, recurse);
        }
      }
    }
  }


  /**
   *
   */
  Enumeration findEntries(String path, String filePattern, boolean recurse) {
    Vector res = new Vector();
    addResourceEntries(res, path, filePattern, recurse);
    if (isFragmentHost()) {
      for (Iterator i = fragments.iterator(); i.hasNext();) {
        BundleGeneration fbg = (BundleGeneration)i.next();
        fbg.addResourceEntries(res, path, filePattern, recurse);
      }
    }
    return res.size() != 0 ? res.elements() : null;
  }


  /**
   * Construct URL to bundle resource
   */
  URL getURL(int subId, String path) {
    try {
      StringBuffer u = new StringBuffer(BundleURLStreamHandler.PROTOCOL);
      u.append("://");
      u.append(bundle.id);
      if (generation > 0) {
        u.append('.').append(generation);
      }
      if (bundle.fwCtx.id > 0) {
        u.append('!').append(bundle.fwCtx.id);
      }
      if (subId > 0) {
        u.append(':').append(subId);
      }
      if (!path.startsWith("/")) {
        u.append('/');
      }
      u.append(path);
      return bundle.secure.getBundleURL(bundle.fwCtx, u.toString());
    } catch (MalformedURLException e) {
      return null;
    }
  }


  /**
   * Purge classloader resources connected to object.
   * 
   */
  void closeClassLoader() {
    if (bundle.fwCtx.debug.classLoader) {
      bundle.fwCtx.debug.println("closeClassLoader: " + bundle + " gen = " + generation);
    }
    BundleClassLoader tmp = (BundleClassLoader)classLoader;
    if (tmp != null) {
      classLoader = null;
      tmp.close();
    }
  }


  /**
   * Purge classloader resources connected to object.
   * 
   */
  void purge(boolean unregister) {
    if (bundle.fwCtx.debug.classLoader) {
      bundle.fwCtx.debug.println("BundleGeneration.purge: " + bundle + " gen = " + generation);
    }
    if (unregister) {
      unregisterPackages(true);
    }
    closeClassLoader();
    purgeProtectionDomain();
    if (archive != null) {
      archive.purge();
    }
  }


  /**
   * 
   *
   */
  boolean unregisterPackages(boolean force) {
    boolean res = bpkgs.unregisterPackages(force);
    if (res && isFragmentHost()) {
      while (detachLastFragment() != null)
        ;
      fragments = null;
    }
    return res;
  }


  /**
   * Purge permission resources connected to object.
   * 
   */
  void purgeProtectionDomain() {
    bundle.secure.purge(bundle, protectionDomain);
  }


  //
  // Private methods
  //

  /**
   * Check bundle certificates
   */
  private void checkCertificates() {
    ArrayList cs = archive.getCertificateChains(false);
    if (cs != null) {
      if (bundle.fwCtx.validator != null) {
        if (bundle.fwCtx.debug.certificates) {
          bundle.fwCtx.debug.println("Validate certs for bundle #" + archive.getBundleId());
        }
        cs = (ArrayList)cs.clone();
        for (Iterator vi = bundle.fwCtx.validator.iterator(); !cs.isEmpty() && vi.hasNext();) {
          Validator v = (Validator)vi.next();
          for (Iterator ci = cs.iterator(); ci.hasNext();) {
            List c = (List)ci.next();
            if (v.validateCertificateChain(c)) {
              archive.trustCertificateChain(c);
              ci.remove();
              if (bundle.fwCtx.debug.certificates) {
                bundle.fwCtx.debug.println("Validated cert: " + c.get(0));
              }
            } else {
              if (bundle.fwCtx.debug.certificates) {
                bundle.fwCtx.debug.println("Failed to validate cert: " + c.get(0));
              }
            }
          }
        }
        if (cs.isEmpty()) {
          // Ok, bundle is signed and validated!
          return;
        }
      }
    }
    if (bundle.fwCtx.props.getBooleanProperty(FWProps.ALL_SIGNED_PROP)) {
      throw new IllegalArgumentException("All installed bundles must be signed!");
    }
  }


  /**
   * "Localizes" this bundle's headers
   * 
   * @param localization_entries A mapping of localization variables to values.
   * @returns a new localized dictionary.
   */
  private HeaderDictionary localize(final Dictionary localization_entries) {
    final HeaderDictionary localized = (HeaderDictionary)cachedRawHeaders.clone();
    if (localization_entries != null) {
      for (Enumeration e = localized.keys(); e.hasMoreElements();) {
        String key = (String)e.nextElement();
        String unlocalizedEntry = (String)localized.get(key);

        if (unlocalizedEntry.startsWith("%")) {
          String k = unlocalizedEntry.substring(1);
          String val = (String)localization_entries.get(k);

          if (val == null) {
            localized.put(key, k);
          } else {
            localized.put(key, val);
          }
        }
      }
    }
    return localized;
  }


  /**
   * Reads all localization entries that affects this bundle (including its
   * host/fragments)
   * 
   * @param locale locale == "" the bundle.properties will be read o/w it will
   *          read the files as described in the spec.
   * @param localization_entries will append the new entries to this dictionary
   * @param baseName the basename for localization properties, <code>null</code>
   *          will choose OSGi default
   */
  private void readLocalization(String locale, Hashtable localization_entries, String baseName) {
    if (!locale.equals("")) {
      locale = "_" + locale;
    }
    while (true) {
      String l = baseName + locale + ".properties";
      Hashtable res = getLocalizationEntries(l);
      if (res != null) {
        localization_entries.putAll(res);
        break;
      }
      int pos = locale.lastIndexOf('_');
      if (pos == -1) {
        break;
      }
      locale = locale.substring(0, pos);
    }
  }


  /**
   * Find localization files and load.
   * 
   */
  private Hashtable getLocalizationEntries(String name) {
    Hashtable res = archive.getLocalizationEntries(name);
    if (res == null && fragments != null) {
      Vector fix = (Vector)fragments.clone();
      for (Iterator i = fix.iterator(); i.hasNext();) {
        BundleGeneration bg = (BundleGeneration)i.next();
        if (bg.archive != null) {
          res = bg.archive.getLocalizationEntries(name);
          if (res != null) {
            break;
          }
        }
      }
    }
    return res;
  }

}
