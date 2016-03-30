/*
 * Copyright (c) 2003-2016, KNOPFLERFISH project
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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Here we handle all the bundles that are installed in the framework.
 * Also handles load and save of bundle states to file, so that we
 * can restart the platform.
 *
 * @author Jan Stein, Mats-Ola Persson, Gunnar Ekolin
 */
public class Bundles {

  /**
   * Table of all installed bundles in this framework.
   * Key is bundle location.
   */
  final private Hashtable<String, BundleImpl> bundles = new Hashtable<String, BundleImpl>();

  final private HashSet<BundleImpl> zombies = new HashSet<BundleImpl>();

  /**
   * Link to framework object.
   */
  private FrameworkContext fwCtx;


  /**
   * Create a container for all bundles in this framework.
   */
  Bundles(FrameworkContext fw) {
    fwCtx = fw;
    bundles.put(fw.systemBundle.location, fw.systemBundle);
  }


  void clear()
  {
    bundles.clear();
    fwCtx = null;
  }


  /**
   * Install a new bundle.
   *
   * @param location The location to be installed
   */
  BundleImpl install(final String location, final InputStream in, final Bundle caller)
    throws BundleException
  {
    checkIllegalState();
    BundleImpl b;
    synchronized (this) {
      b = bundles.get(location);
      if (b != null) {
        if (fwCtx.bundleHooks.filterBundle(b.bundleContext, b) == null &&
            caller != fwCtx.systemBundle) {
          throw new BundleException("Rejected by a bundle hook",
                                    BundleException.REJECTED_BY_HOOK);
        } else {
          return b;
        }
      }
      b = fwCtx.perm.callInstall0(this, location, in, caller);
    }
    return b;
  }


  BundleImpl install0(String location, InputStream in, Object checkContext, Bundle caller)
    throws BundleException {
    InputStream bin;
    BundleArchive ba = null;
    try {
      if (in == null) {
        // Do it the manual way to have a chance to
        // set request properties
        final URL url  = new URL(location);
        final URLConnection conn = url.openConnection();

        // Support for http proxy authentication
        //TODO put in update as well
        final String auth = fwCtx.props.getProperty("http.proxyAuth");
        if (auth != null && !"".equals(auth)) {
          if ("http".equals(url.getProtocol()) ||
              "https".equals(url.getProtocol())) {
            final String base64 = Util.base64Encode(auth);
            conn.setRequestProperty("Proxy-Authorization",
                                    "Basic " + base64);
          }
        }
        // Support for http basic authentication
        final String basicAuth = fwCtx.props.getProperty("http.basicAuth");
        if (basicAuth != null && !"".equals(basicAuth)) {
          if ("http".equals(url.getProtocol()) ||
              "https".equals(url.getProtocol())) {
            final String base64 = Util.base64Encode(basicAuth);
            conn.setRequestProperty("Authorization",
                                    "Basic " +base64);
          }
        }
        bin = conn.getInputStream();
      } else {
        bin = in;
      }
      try {
        ba = fwCtx.storage.insertBundleJar(location, bin);
      } finally {
        bin.close();
      }
      final BundleImpl res = new BundleImpl(fwCtx, ba, checkContext, caller);
      bundles.put(location, res);
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, res, caller));
      return res;
    } catch (final Exception e) {
      if (ba != null) {
        ba.purge();
      }
      if (e instanceof SecurityException) {
        throw (SecurityException)e;
      } else if (e instanceof BundleException) {
        throw (BundleException)e;
      } else if (e instanceof IllegalArgumentException) {
        throw new BundleException("Faulty manifest: " + e,
                                  BundleException.MANIFEST_ERROR, e);
      } else {
        throw new BundleException("Failed to install bundle: " + e,
                                  BundleException.UNSPECIFIED, e);
      }
    }
  }


  /**
   * Remove bundle registration.
   *
   * @param location The location to be removed
   */
  void remove(String location) {
    bundles.remove(location);
  }

  void addZombie(BundleImpl b) {
    synchronized (bundles) {
      zombies.add(b);
    }
  }

  void removeZombie(BundleImpl b) {
    synchronized (bundles) {
      zombies.remove(b);
    }
  }

  /**
   * Get bundle that has specified bundle identifier.
   *
   * @param id The identifier of bundle to get.
   * @return BundleImpl representing bundle or null
   *         if bundle was not found.
   */
  Bundle getBundle(long id) {
    checkIllegalState();
    synchronized (bundles) {
      for (final Enumeration<BundleImpl> e = bundles.elements(); e.hasMoreElements();) {
        final BundleImpl b = e.nextElement();
        if (b.id == id) {
          return b;
        }
      }
    }
    return null;
  }


  /**
   * Get bundle that has specified bundle location.
   *
   * @param location The location of bundle to get.
   * @return BundleImpl representing bundle or null
   *         if bundle was not found.
   */
  Bundle getBundle(String location) {
    checkIllegalState();
    return bundles.get(location);
  }


  /**
   * Get all bundles that has specified bundle symbolic name and version.
   *
   * @param name The symbolic name of bundle to get.
   * @param version The bundle version of bundle to get.
   * @return Collection of BundleImpls.
   */
  Collection<Bundle> getBundles(String name, Version version) {
    checkIllegalState();
    final ArrayList<Bundle> res = new ArrayList<Bundle>(bundles.size());    
    if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(name)
        && version.equals(fwCtx.systemBundle.getVersion())) {
      res.add(fwCtx.systemBundle);
    }
    synchronized (bundles) {
      for (final Enumeration<BundleImpl> e = bundles.elements(); e.hasMoreElements();) {
        final BundleImpl b = e.nextElement();
        if (name.equals(b.getSymbolicName()) && version.equals(b.getVersion())) {
          res.add(b);
        }
      }
    }
    return res;
  }


  /**
   * Get all installed bundles.
   *
   * @return A Bundle array with bundles.
   */
  List<BundleImpl> getBundles() {
    final ArrayList<BundleImpl> res = new ArrayList<BundleImpl>(bundles.size());
    synchronized (bundles) {
      res.addAll(bundles.values());
    }
    return res;
  }


  /**
   * Get all bundles that has specified bundle symbolic name.
   *
   * @param name The symbolic name of bundles to get, if null get all.
   * @return A List of current BundleGenerations.
   */
  List<BundleGeneration> getBundleGenerations(String name) {
    final ArrayList<BundleGeneration> res = new ArrayList<BundleGeneration>();
    if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(name)) {
      res.add(fwCtx.systemBundle.current());
    }
    synchronized (bundles) {
      for (final BundleImpl b : bundles.values()) {
        if (name == null || name.equals(b.getSymbolicName())) {
          res.add(b.current());
        }
      }
    }
    return res;
  }


  /**
   * Get all bundles that has specified bundle symbolic name and
   * version range. Result is sorted in decreasing version order.
   *
   * @param name The symbolic name of bundles to get.
   * @param range Version range of bundles to get.
   * @return A List of current BundleGenerations.
   */
  List<BundleGeneration> getBundles(String name, VersionRange range) {
    checkIllegalState();
    final List<BundleGeneration> res = getBundleGenerations(name);
    for (int i = 0; i < res.size(); ) {
      final BundleGeneration bg = res.remove(i);
      if (range == null || range.includes(bg.version)) {
        int j = i;
        while (--j >= 0) {
          if (bg.version.compareTo(res.get(j).version) <= 0) {
            break;
          }
        }
        res.add(j + 1, bg);
        i++;
      }
    }
    return res;
  }


  /**
   * Get all bundles currently in bundle state ACTIVE.
   *
   * @return A List of BundleImpl.
   */
  List<BundleImpl> getActiveBundles() {
    checkIllegalState();
    final ArrayList<BundleImpl> slist = new ArrayList<BundleImpl>();
    synchronized (bundles) {
      for (final Enumeration<BundleImpl> e = bundles.elements(); e.hasMoreElements();) {
        final BundleImpl b = e.nextElement();
        final int s = b.getState();
        if (s == Bundle.ACTIVE || s == Bundle.STARTING) {
          slist.add(b);
        }
      }
    }
    return slist;
  }


  /**
   * Get removal pending bundles.
   *
   * @return A Bundle array with bundles.
   */
  void getRemovalPendingBundles(Collection<Bundle> res) {
    synchronized (bundles) {
      for (BundleImpl b : bundles.values()) {
        if (b.hasZombies()) {
          res.add(b);
        }
      }
      res.addAll(zombies);
    }
  }


  /**
   * Get unattached fragments that has a resolved host.
   *
   * @return A Bundle array with bundles.
   */
  void getUnattachedBundles(Collection<Bundle> res) {
    synchronized (bundles) {
      for (BundleImpl b : bundles.values()) {
        if (b.getState() == Bundle.INSTALLED) {
          final BundleGeneration curr = b.current();
          if (curr.isFragment() && curr.getResolvedHosts().size() > 0) {
            res.add(b);
          }
        }
      }
    }
  }


  /**
   * Check if any extension bundle needs a framework restart.
   *
   * @return A Bundle array with bundles.
   */
  boolean checkExtensionBundleRestart() {
    synchronized (bundles) {
      for (BundleImpl b : zombies) {
        if (b.extensionNeedsRestart()) {
          return true;
        }
      }
      for (BundleImpl b : bundles.values()) {
        if (b.extensionNeedsRestart()) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Try to load any saved framework state.
   * This is done by installing all saved bundles from the local archive
   * copy, and restoring the saved state for each bundle. This is only
   * intended to be executed during the start of the framework.
   *
   */
  synchronized void load() {
    final BundleArchive [] bas = fwCtx.storage.getAllBundleArchives();
    for (final BundleArchive ba : bas) {
      try {
        final BundleImpl b = new BundleImpl(fwCtx, ba, null, fwCtx.systemBundle);
        bundles.put(b.location, b);
      } catch (final Exception e) {
        try {
          ba.setAutostartSetting(-1); // Do not start on launch
          ba.setStartLevel(-2); // Mark as uninstalled
        } catch (final IOException _ioe) {
        }
        System.err.println("Error: Failed to load bundle "
                           +ba.getBundleId()
                           +" (" +ba.getBundleLocation() +")"
                           +" uninstalled it!" );
        e.printStackTrace();
      }
    }
  }


  /**
   * Returns all fragment bundles that is
   * already attached and targets given bundle.
   *
   * @param target the targeted bundle
   * @return a list of all matching fragment bundle generations.
   */
  Collection<BundleGeneration> getFragmentBundles(BundleGeneration target) {
    final HashMap<String, BundleGeneration> res = new HashMap<String, BundleGeneration>();
    for (final Enumeration<BundleImpl> e = bundles.elements(); e.hasMoreElements();) {
      final BundleImpl b = e.nextElement();
      final BundleGeneration bg = b.current();
      if (bg.isFragment() &&
          b.state != Bundle.UNINSTALLED &&
          bg.fragment.isTarget(target)) {
        final String sym = bg.symbolicName;
        final BundleGeneration old = res.get(sym);
        if (old != null && old.symbolicName.equals(sym)) {
          if (old.version.compareTo(bg.version) > 0) {
            continue;
          }
        }
        res.put(sym, bg);
      }
    }
    return res.values();
  }


  /**
   * Check if this bundles object have been closed!
   */
  private void checkIllegalState() {
    if (null == fwCtx) {
      throw new IllegalStateException("This framework instance is not active.");
    }
  }

}
