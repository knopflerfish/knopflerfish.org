/*
 * Copyright (c) 2010-2013, KNOPFLERFISH project
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Bundle generation specific data.
 *
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 */

public class BundleGeneration implements Comparable<BundleGeneration> {

  /**
   * Symbolic name system bundle.
   */
  final static String KNOPFLERFISH_SYMBOLICNAME = "org.knopflerfish.framework";

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
   * Symbolic name parameters
   */
  final HeaderEntry symbolicNameParameters;

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
   * The bundle requirements from the Require-Capability header.
   */
  final Map<String, List<BundleRequirementImpl>> requirements
    = new HashMap<String, List<BundleRequirementImpl>>();

  /**
   * The bundle capabilities from the Provide-Capability header.
   */
  final Map<String, List<BundleCapabilityImpl>> capabilities
    = new HashMap<String, List<BundleCapabilityImpl>>();

  /**
   * True when this bundle has its activation policy set to "lazy"
   */
  final boolean lazyActivation;
  final private Set<String> lazyIncludes;
  final private Set<String> lazyExcludes;

  /**
   * Time when bundle was last created.
   *
   */
  final long timeStamp;

  /**
   * All fragment bundles this bundle hosts.
   */
  Vector<BundleGeneration> fragments = null;

  /**
   * Stores the raw manifest headers.
   */
  private volatile HeaderDictionary cachedRawHeaders = null;

  /**
   * Classloader for bundle.
   */
  private volatile ClassLoader classLoader;

  /**
   * Bundle protect domain. Will always be <tt>null</tt> for the system bundle,
   * methods requiring access to it must be overridden in the SystemBundle
   * class.
   */
  private ProtectionDomain protectionDomain;

  private BundleClassPath unresolvedBundleClassPath = null;

  final BundleRevisionImpl bundleRevision;

  final private BundleCapability identity;

  /**
   * Construct a new BundleGeneration for the System Bundle.
   *
   * @param b BundleImpl this bundle data.
   * @param exportStr The value of the Export-Package header.
   * @param capabilityStr The value of the Provide-Capability header.
   */
  BundleGeneration(BundleImpl b, String exportStr, String capabilityStr) {
    bundle = b;
    archive = null;
    generation = 0;
    v2Manifest = true;
    symbolicName = KNOPFLERFISH_SYMBOLICNAME;
    symbolicNameParameters = null;
    singleton = false;
    version = new Version(Util.readFrameworkVersion());
    attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
    fragment = null;
    protectionDomain = null;
    lazyActivation = false;
    lazyIncludes = null;
    lazyExcludes = null;
    timeStamp = System.currentTimeMillis();
    bpkgs = new BundlePackages(this, exportStr);
    bundleRevision = new BundleRevisionImpl(this);
    classLoader = b.getClassLoader();
    processCapabilities(capabilityStr);
    identity = new BundleCapabilityImpl(this);
  }


  /**
   * Construct a new BundleGeneration.
   *
   * Construct a new Bundle based on a BundleArchive.
   *
   * @param b BundleImpl this bundle data.
   * @param ba Bundle archive with holding the contents of the bundle.
   * @param prev the previous generation of this bundle.
   *
   * @throws BundleException If we have duplicate symbolicname and version.
   *
   * @exception IOException If we fail to read and store our JAR bundle or if
   *              the input data is corrupted.
   * @exception SecurityException If we don't have permission to install
   *              extension.
   * @exception IllegalArgumentException Faulty manifest for bundle
   */
  BundleGeneration(BundleImpl b, BundleArchive ba, BundleGeneration prev, Bundle caller) throws BundleException {
    bundle = b;
    generation = (prev != null ? prev.generation : -1) + 1;
    archive = ba;
    checkCertificates();
    // TODO, v2Manifest unnecessary to cache?
    final String mv = archive.getAttribute(Constants.BUNDLE_MANIFESTVERSION);
    v2Manifest = mv != null && mv.trim().equals("2");
    
    final String mbv = archive.getAttribute(Constants.BUNDLE_VERSION);
    Version newVer = Version.emptyVersion;
    if (mbv != null) {
      try {
        newVer = new Version(mbv);
      } catch (final Exception ee) {
        if (v2Manifest) {
          throw new IllegalArgumentException("Bundle does not specify a valid "
              + Constants.BUNDLE_VERSION + " header. Got exception: " + ee.getMessage());
        }
      }
    }
    version = newVer;

    List<HeaderEntry> hes = Util.parseManifestHeader(Constants.FRAGMENT_HOST, archive
        .getAttribute(Constants.FRAGMENT_HOST), true, true, true);
    if (!hes.isEmpty()) {
      fragment = new Fragment(this, hes.get(0));
    } else {
      fragment = null;
    }

    hes = Util.parseManifestHeader(Constants.BUNDLE_ACTIVATIONPOLICY, archive
        .getAttribute(Constants.BUNDLE_ACTIVATIONPOLICY), true, true, true);
    if (!hes.isEmpty()) {
      HeaderEntry he = hes.get(0);
      lazyActivation = Constants.ACTIVATION_LAZY.equals(he.getKey());
      if (lazyActivation) {
        if (he.getDirectives().containsKey(Constants.INCLUDE_DIRECTIVE)) {
          lazyIncludes = Util.parseEnumeration(Constants.INCLUDE_DIRECTIVE,
              he.getDirectives().get(Constants.INCLUDE_DIRECTIVE));
        } else {
          lazyIncludes = null;
        }
        if (he.getDirectives().containsKey(Constants.EXCLUDE_DIRECTIVE)) {
          lazyExcludes = Util.parseEnumeration(Constants.EXCLUDE_DIRECTIVE,
              he.getDirectives().get(Constants.EXCLUDE_DIRECTIVE));

          if (lazyIncludes != null) {
            for (final String entry : lazyExcludes) {
              if (lazyIncludes.contains(entry)) {
                throw new IllegalArgumentException("Conflicting "
                    + Constants.INCLUDE_DIRECTIVE
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

    hes = Util.parseManifestHeader(Constants.REQUIRE_CAPABILITY, archive
        .getAttribute(Constants.REQUIRE_CAPABILITY), true, true, false);
    for (final HeaderEntry e : hes) {
      final BundleRequirementImpl bri = new BundleRequirementImpl(this, e);
      final String ns = bri.getNamespace();
      List<BundleRequirementImpl> nsReqs = requirements.get(ns);
      if (null == nsReqs) {
        if (isExtension() && !ns.equals(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)) {
          throw new IllegalArgumentException("An extension bundle can only specify capability requirements in the " +
              ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE + " namespace. Not in " + ns);
        }
        nsReqs = new ArrayList<BundleRequirementImpl>();
        requirements.put(ns, nsReqs);
      }
      nsReqs.add(bri);
    }

    @SuppressWarnings("deprecation")
    String ee = archive.getAttribute(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
    if (ee != null) {
      final BundleRequirementImpl bri = new BundleRequirementImpl(this, ee);
      List<BundleRequirementImpl> nsReqs = requirements.get(bri.getNamespace());
      if (null == nsReqs) {
        nsReqs = new ArrayList<BundleRequirementImpl>();
        requirements.put(bri.getNamespace(), nsReqs);
      }
      nsReqs.add(bri);
    }

    processCapabilities(archive.getAttribute(Constants.PROVIDE_CAPABILITY));

    bpkgs = new BundlePackages(this);
    try {
      if (b.fwCtx.startLevelController == null) {
        archive.setStartLevel(0);
      } else {
        if (archive.getStartLevel() == -1) {
          archive.setStartLevel(b.fwCtx.startLevelController.getInitialBundleStartLevel());
        }
      }
    } catch (final Exception exc) {
      b.fwCtx.frameworkError(b, exc);
    }
    archive.setBundleGeneration(this);
    long lastModified = prev != null ? 0 : archive.getLastModified();
    if (lastModified == 0) {
      lastModified = System.currentTimeMillis();
    }
    bundleRevision = new BundleRevisionImpl(this);
    timeStamp = lastModified;

    hes = Util
        .parseManifestHeader(Constants.BUNDLE_SYMBOLICNAME, archive
            .getAttribute(Constants.BUNDLE_SYMBOLICNAME), true, true, true);
    HeaderEntry he = null;
    if (!hes.isEmpty()) {
      he = hes.get(0);
      symbolicName = he.getKey();
    } else {
      if (v2Manifest) {
        throw new IllegalArgumentException("Bundle has no symbolic name, location="
            + bundle.location);
      } else {
        symbolicName = null;
      }
    }
    if (he != null) {
      singleton = "true".equals(he.getDirectives().get(Constants.SINGLETON_DIRECTIVE));
      final String tmp = he.getDirectives().get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
      attachPolicy = tmp == null ? Constants.FRAGMENT_ATTACHMENT_ALWAYS : tmp;
      symbolicNameParameters = he;
      if (!bundle.fwCtx.bsnversionMode.equals(Constants.FRAMEWORK_BSNVERSION_MULTIPLE)) {
        final Collection<Bundle> snbl = b.fwCtx.bundles.getBundles(symbolicName, version);
        if (!snbl.isEmpty() && (snbl.size() > 1 || !snbl.contains(bundle))) {
          boolean fail;
          if (bundle.fwCtx.bsnversionMode.equals(Constants.FRAMEWORK_BSNVERSION_MANAGED)) {
            bundle.fwCtx.bundleHooks.filterCollisions(prev != null ? CollisionHook.UPDATING : CollisionHook.INSTALLING, caller, snbl);
            fail = !snbl.isEmpty() && (snbl.size() > 1 || !snbl.contains(bundle));
          } else {
            fail = true;
          }
          if (fail) {
            throw new BundleException("Bundle#" + b.id +
                                      ", a bundle with same symbolic name and version "
                                      + "is already installed (" + symbolicName + ", "
                                      + version, BundleException.DUPLICATE_BUNDLE_ERROR);
          }
        }
      }
    } else {
      attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
      singleton = false;
      symbolicNameParameters = null;
    }
    identity = symbolicName != null ? new BundleCapabilityImpl(this) : null;
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
    symbolicNameParameters = prev.symbolicNameParameters;
    singleton = prev.singleton;
    version = prev.version;
    attachPolicy = Constants.FRAGMENT_ATTACHMENT_NEVER;
    fragment = prev.fragment;
    protectionDomain = null;
    lazyActivation = false;
    lazyIncludes = null;
    lazyExcludes = null;
    timeStamp = System.currentTimeMillis();
    bpkgs = null;
    cachedRawHeaders = prev.cachedRawHeaders;
    classLoader = null;
    bundleRevision = null;
    identity = null;
  }


  private void processCapabilities(String capabilityStr)
  {
    if (capabilityStr != null && capabilityStr.length() > 0) {
      for (final HeaderEntry he : Util
          .parseManifestHeader(Constants.PROVIDE_CAPABILITY, capabilityStr,
                               true, true, false)) {
        final BundleCapabilityImpl bci = new BundleCapabilityImpl(this, he);
        List<BundleCapabilityImpl> nsCaps = capabilities.get(bci.getNamespace());
        if (null == nsCaps) {
          nsCaps = new ArrayList<BundleCapabilityImpl>();
          capabilities.put(bci.getNamespace(), nsCaps);
        }
        nsCaps.add(bci);
      }
    }
  }

  /**
   * Compares this {@code BundleGeneration} object to another
   * {@code BundleGeneration}. It compares the bundle identifier value.
   *
   * @param bg the other object to compare this one to.
   */
  public int compareTo(BundleGeneration bg) {
    final long diff = bundle.id - bg.bundle.id;
    if (diff < 0) {
      return -1;
    } else if (diff == 0) {
      return 0;
    } else {
      return 1;
    }
  }


  public String toString() {
    return "BundleGeneration[bid=" + bundle.id + ", gen=" + generation + "]";
  }

  //
  // Package methods
  //

  /**
   * Finish construction by doing protectDomain creation and permission checks.
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
    } catch (final RuntimeException re) {
      purgeProtectionDomain();
      throw re;
    }
    // Bundle ok save timeStamp
    if (archive.getLastModified() != timeStamp) {
      try {
        archive.setLastModified(timeStamp);
      } catch (final IOException ioe) {
        bundle.fwCtx.frameworkError(bundle, ioe);
      }
    }
  }


  /**
   * Get capabilities specified by this bundle generation.
   *
   * Returns capabilities declared in the Bundle-Capability manifest header.
   * <p/>
   * The key in the map is the {@code name-space} of the capability.
   */
  Map<String, List<BundleCapabilityImpl>> getDeclaredCapabilities()
  {
    return capabilities;
  }


  /**
   * Get requirements specified by this bundle generation.
   *
   * Returns all requirements declared in the Bundle-Requirement manifest header.
   * <p/>
   * The key in the map is the {@code name-space} of the bundle requirement.
   */
  Map<String, List<BundleRequirementImpl>> getDeclaredRequirements()
  {
    return requirements;
  }


  /**
   *
   */
  boolean resolvePackages(BundleImpl[] triggers) throws BundleException {
    ArrayList<BundleGeneration> detached = null;
    attachFragments();
    while (true) {
      if (bpkgs.resolvePackages(triggers)) {
        if (detached != null) {
          // TODO should we report fragment that failed to attach
          for (int i = detached.size() - 2; i >= 0; i--) {
            final BundleGeneration bg = detached.get(i);
            if (bg.bundle.attachToFragmentHost(this)) {
              fragments.add(bg);
            }
          }
        }
        classLoader = bundle.secure.newBundleClassLoader(this);

        return true;
      }
      if (isFragmentHost()) {
        if (bundle.fwCtx.debug.resolver) {
          bundle.fwCtx.debug.println("Resolve failed, remove last fragment and retry");
        }
        if (detached == null) {
          detached = new ArrayList<BundleGeneration>();
        }
        detached.add(detachLastFragment(true));
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
   * @throws BundleException Resolve hook throw an exception.
   */
  private void attachFragments() throws BundleException {
    if (!attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
      final Collection<BundleGeneration> hosting = bundle.fwCtx.bundles.getFragmentBundles(this);
      if (hosting.size() > 0 && bundle.secure.okHostBundlePerm(bundle)) {
        // retrieve all fragments this bundle host
        for (final BundleGeneration fbg : hosting) {
          // TODO, do we need to clean in case of BundleException?
          fbg.bundle.attachToFragmentHost(this);
        }
      }
    }
  }


  /**
   * Attaches a fragment to this bundle generation.
   * @throws BundleException 
   */
  void attachFragment(BundleGeneration fragmentBundle) throws BundleException {
    if (attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
      throw new IllegalStateException("Bundle does not allow fragments to attach");
    }
    if (attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_RESOLVETIME)
        && bundle.isResolved()) {
      throw new IllegalStateException("Bundle does not allow fragments to attach dynamically");
    }
    try {
      BundleClassPath.checkNativeCode(fragmentBundle.archive, bundle.fwCtx.props, true);
    } catch (BundleException e) {
      throw new IllegalStateException(e.getMessage());
    }
    if (!bundle.fwCtx.resolverHooks.filterMatches(fragmentBundle.fragment,
                                                  new BundleNameVersionCapability(this, BundleRevision.HOST_NAMESPACE))) {
      throw new IllegalStateException("Resolver hooks vetoed attach to: " + this);      
    }
    final String failReason = bpkgs.attachFragment(fragmentBundle.bpkgs);
    if (failReason != null) {
      throw new IllegalStateException("Failed to attach: " + failReason);
    }
    if (classLoader != null && classLoader instanceof BundleClassLoader) {
      try {
        ((BundleClassLoader)classLoader).attachFragment(fragmentBundle);
      } catch (final BundleException be) {
        // TODO, should we unregister fragment packaaes
        throw new IllegalStateException(be.getMessage());
      }
    } else {
      
    }
    if (bundle.fwCtx.debug.resolver) {
      bundle.fwCtx.debug.println("Fragment(" + fragmentBundle.bundle + ") attached to host(id="
          + bundle.id + ",gen=" + generation + ")");
    }
    if (fragments == null) {
      fragments = new Vector<BundleGeneration>();
    }
    int i = 0;
    // TODO: use sorted list!?
    for (; i < fragments.size(); i++) {
      final BundleGeneration b = fragments.get(i);
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
  private BundleGeneration detachLastFragment(boolean unregister) {
    // TODO, handle extensions
    final int last = fragments.size() - 1;
    if (last >= 0) {
      final BundleGeneration fbg = fragments.remove(last);
      bpkgs.detachFragmentSynchronized(fbg, unregister);
      if (bundle.fwCtx.debug.resolver) {
        bundle.fwCtx.debug.println("Fragment(id=" + fbg.bundle.id + ") detached from host(id="
            + bundle.id + ",gen=" + generation + ")");
      }
      if (fbg.bundle.state != Bundle.UNINSTALLED) {
        fbg.fragment.removeHost(this);
        if (!fbg.fragment.hasHosts()) {
          if (fbg.isCurrent()) {
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
      @SuppressWarnings("unchecked")
      final Vector<BundleGeneration> fix = (Vector<BundleGeneration>)fragments.clone();
      for (final BundleGeneration bundleGeneration : fix) {
        final BundleImpl fb = bundleGeneration.bundle;
        fb.getUpdatedState(null);
      }
    }
  }


  /**
   * Get hosts if this bundle is a fragment.
   * Return null if not a fragment.
   */
  Vector<BundleGeneration> getHosts() {
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
    return fragments != null && !fragments.isEmpty();
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
  private Dictionary<String, String> getLocaleDictionary(String locale, String baseName) {
    final String defaultLocale = Locale.getDefault().toString();

    if (locale == null) {
      locale = defaultLocale;
    } else if (locale.equals("")) {
      return null;
    }
    final Hashtable<String, String> localization_entries = new Hashtable<String, String>();
    // TODO, should we do like this and allow mixed locales?
    if (baseName == null) {
      baseName = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
    }
    final Vector<BundleGeneration> h = getHosts();
    if (h != null) {
      BundleGeneration best;
      while (true) {
        try {
          best = null;
          for (final BundleGeneration bundleGeneration : h) {
            final BundleGeneration bg = bundleGeneration;
            if (best == null || bg.version.compareTo(best.version) > 0) {
              best = bg;
            }
          }
          break;
        } catch (final ConcurrentModificationException ignore) {
        }
      }
      if (best == bundle.fwCtx.systemBundle.current()) {
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
      final String base = cachedRawHeaders.get(Constants.BUNDLE_LOCALIZATION);
      try {
        return localize(getLocaleDictionary(locale, base));
      } catch (final RuntimeException e) {
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
  void addResourceEntries(Vector<URL> res, String path, String pattern, boolean recurse) {
    if (archive == null) {
      // We are not called as systembundle
      throw new IllegalStateException("Bundle is in UNINSTALLED state");
    }
    final Enumeration<String> e = archive.findResourcesPath(path);
    if (e != null) {
      while (e.hasMoreElements()) {
        final String fp = e.nextElement();
        final boolean isDirectory = fp.endsWith("/");
        int searchBackwardFrom = fp.length() - 1;
        if (isDirectory) {
          // Skip last / in case of directories
          searchBackwardFrom = searchBackwardFrom - 1;
        }
        final int l = fp.lastIndexOf('/', searchBackwardFrom);
        final String lastComponentOfPath = fp.substring(l + 1, searchBackwardFrom + 1);
        if (pattern == null || Util.filterMatch(pattern, lastComponentOfPath)) {
          final URL url = getURL(0, fp);
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
  Vector<URL> findEntries(String path, String filePattern, boolean recurse) {
    final Vector<URL> res = new Vector<URL>();
    addResourceEntries(res, path, filePattern, recurse);
    if (isFragmentHost()) {
      @SuppressWarnings("unchecked")
      final Vector<BundleGeneration> fix = (Vector<BundleGeneration>)fragments.clone();
      for (final BundleGeneration fbg : fix) {
        fbg.addResourceEntries(res, path, filePattern, recurse);
      }
    }
    return res;
  }


  /**
   * Construct URL to bundle resource
   */
  URL getURL(int subId, String path) {
    try {
      final StringBuffer u = new StringBuffer(BundleURLStreamHandler.PROTOCOL);
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
    } catch (final MalformedURLException e) {
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
    final BundleClassLoader tmp = (BundleClassLoader)classLoader;
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
    clearWiring();
  }


  /**
   *
   *
   */
  boolean unregisterPackages(boolean force) {
    final boolean res = bpkgs.unregisterPackages(force);
    if (res && isFragmentHost()) {
      while (detachLastFragment(false) != null)
        ;
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
    ArrayList<List<X509Certificate>> cs = archive.getCertificateChains(false);
    if (cs != null) {
      if (bundle.fwCtx.validator != null) {
        if (bundle.fwCtx.debug.certificates) {
          bundle.fwCtx.debug.println("Validate certs for bundle #" + archive.getBundleId());
        }
        cs = new ArrayList<List<X509Certificate>>(cs);
        for (final Iterator<Validator> vi = bundle.fwCtx.validator.iterator(); !cs.isEmpty() && vi.hasNext();) {
          final Validator v = vi.next();
          for (final Iterator<List<X509Certificate>> ci = cs.iterator(); ci.hasNext();) {
            final List<X509Certificate> c = ci.next();
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
  private HeaderDictionary localize(final Dictionary<String, String> localization_entries) {
    final HeaderDictionary localized = (HeaderDictionary)cachedRawHeaders.clone();
    if (localization_entries != null) {
      for (final Enumeration<String> e = localized.keys(); e.hasMoreElements();) {
        final String key = e.nextElement();
        final String unlocalizedEntry = localized.get(key);

        if (unlocalizedEntry.startsWith("%")) {
          final String k = unlocalizedEntry.substring(1);
          final String val = localization_entries.get(k);

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
  private void readLocalization(String locale, Hashtable<String, String> localization_entries, String baseName) {
    if (!locale.equals("")) {
      locale = "_" + locale;
    }
    while (true) {
      final String l = baseName + locale + ".properties";
      final Hashtable<String, String> res = getLocalizationEntries(l);
      if (res != null) {
        localization_entries.putAll(res);
        break;
      }
      final int pos = locale.lastIndexOf('_');
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
  private Hashtable<String, String> getLocalizationEntries(String name) {
    Hashtable<String, String> res = archive.getLocalizationEntries(name);
    if (res == null && isFragmentHost()) {
      @SuppressWarnings("unchecked")
      final Vector<BundleGeneration> fix = (Vector<BundleGeneration>)fragments.clone();
      for (final BundleGeneration bg : fix) {
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


  boolean isUninstalled() {
    return bpkgs == null;
  }


  BundleCapability getHostCapability()
  {
    if (v2Manifest
        && !attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER) && fragment == null) {
      return new BundleNameVersionCapability(this,
                                             BundleRevision.HOST_NAMESPACE);
    }
    return null;
  }

  BundleNameVersionCapability getBundleCapability() {
    if (v2Manifest && fragment==null) {
      return new BundleNameVersionCapability(this, BundleRevision.BUNDLE_NAMESPACE);
    }
    return null;
  }


  Vector<URL> getBundleClassPathEntries(final String name, final boolean onlyFirst) {
    BundleClassPath bcp = unresolvedBundleClassPath;
    if (bcp == null) {
      bcp = new BundleClassPath(archive, bundle.fwCtx);
      unresolvedBundleClassPath = bcp;
    }
    final Vector<FileArchive> fas = bcp.componentExists(name, onlyFirst, false);
    if (fas != null) {
      final Vector<URL> res = new Vector<URL>();
      for (final FileArchive fa : fas)  {
        final URL url = fa.getBundleGeneration().getURL(fa.getSubId(), name);
        if (url != null) {
          res.addElement(url);
        } else {
          // Internal error
          return null;
        }
      }
      return res;
    }
    return null;
  }


  List<BundleWireImpl> getCapabilityWires() {
    if (bpkgs != null) {
      List<BundleWireImpl> res = new ArrayList<BundleWireImpl>();
      for (List<BundleCapabilityImpl> lbc : bpkgs.getOtherCapabilities().values()) {
        for (BundleCapabilityImpl bc : lbc) {
          bc.getWires(res);
        }
      }
      return res;
    } else {
      return null;
    }
  }


  List<BundleWireImpl> getRequirementWires() {
    List<BundleWireImpl> res = new ArrayList<BundleWireImpl>();
    for (List<BundleRequirementImpl> lbr : getOtherRequirements().values()) {
      for (BundleRequirementImpl br : lbr) {
        final BundleWireImpl wire = br.getWire();
        if (wire != null) {
          res.add(wire);
        }
      }
    }
    return res;
  }


  Map<String, List<BundleRequirementImpl>> getOtherRequirements() {
    Map<String, List<BundleRequirementImpl>> res = getDeclaredRequirements();
    if (isFragmentHost()) {
      boolean copied = false;
      @SuppressWarnings("unchecked")
      final Vector<BundleGeneration> fix = (Vector<BundleGeneration>)fragments.clone();
      for (final BundleGeneration fbg : fix) {
        Map<String, List<BundleRequirementImpl>> frm = fbg.getDeclaredRequirements();
        if (!frm.isEmpty()) {
          if (!copied) {
            res = new HashMap<String, List<BundleRequirementImpl>>(res);
            copied = true;
          }
          for (Entry<String, List<BundleRequirementImpl>> e : frm.entrySet()) {
            String ns = e.getKey();
            List<BundleRequirementImpl> p = res.get(ns);
            if (p != null) {
              p = new ArrayList<BundleRequirementImpl>(p);
              p.addAll(e.getValue());
            } else {
              p = e.getValue();
            }
            res.put(ns, p);
          }
        }
      }
    }
    // TODO filter optional
    return res;
  }


  boolean isCurrent() {
    return this == bundle.current();
  }


  boolean bsnAttrMatch(Map<String, Object> attributes) {
    if (symbolicNameParameters != null) {
      final Map<String, Object> attr = symbolicNameParameters.getAttributes();
      final String mandatory = symbolicNameParameters.getDirectives().get(Constants.MANDATORY_DIRECTIVE);
      HashSet<String> mAttr = new HashSet<String>();
      if (mandatory != null) {
        String [] split = Util.splitwords(mandatory, ",");
        for (String e : split) {
          mAttr.add(e.trim());
        }
      }
      for (Entry<String, Object> e : attributes.entrySet()) {
        final String key = e.getKey();
        if (e.getValue().equals(attr.get(key))) {
          mAttr.remove(key);
        } else {
          return false;
        }
      }
      return mAttr.isEmpty();
    }
    return true;
  }


  void setWired() {
    bundleRevision.setWired();
  }


  void clearWiring() {
    bundleRevision.clearWiring();
  }


  Set<BundleImpl> getResolvedHosts() {
    Set<BundleImpl> res = new HashSet<BundleImpl>();
    List<BundleGeneration> tgts = fragment.targets();
    for (BundleGeneration t : tgts) {
      if (t.bundle.isResolved()) {
        res.add(t.bundle);
      }
    }
    return res;
  }


  BundleCapability getIdentityCapability() {
    return identity;
  }

}
