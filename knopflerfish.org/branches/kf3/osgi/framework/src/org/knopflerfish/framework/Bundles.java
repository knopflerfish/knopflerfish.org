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
import java.security.*;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.*;

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
  private Hashtable /* location String -> BundleImpl */ bundles = new Hashtable();

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
  BundleImpl install(final String location, final InputStream in)
    throws BundleException
  {
    if (null==fwCtx) { // This bundles object have been closed!
      throw new IllegalStateException
        ("Bundles.getBundle(id) called on closed bundles object.");
    }
    BundleImpl b;
    synchronized (this) {
      b = (BundleImpl)bundles.get(location);
      if (b != null) {
        return b;
      }
      b = fwCtx.perm.callInstall0(this, location, in);
    }
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, b));
    return b;
  }


  BundleImpl install0(String location, InputStream in, Object checkContext)
    throws BundleException {
    InputStream bin;
    BundleArchive ba = null;
    try {
      if (in == null) {
        // Do it the manual way to have a chance to
        // set request properties
        URL url  = new URL(location);
        URLConnection conn = url.openConnection();

        // Support for http proxy authentication
        //TODO put in update as well
        String auth = fwCtx.props.getProperty("http.proxyAuth");
        if (auth != null && !"".equals(auth)) {
          if ("http".equals(url.getProtocol()) ||
              "https".equals(url.getProtocol())) {
            String base64 = Util.base64Encode(auth);
            conn.setRequestProperty("Proxy-Authorization",
                                    "Basic " + base64);
          }
        }
        // Support for http basic authentication
        String basicAuth = fwCtx.props.getProperty("http.basicAuth");
        if (basicAuth != null && !"".equals(basicAuth)) {
          if ("http".equals(url.getProtocol()) ||
              "https".equals(url.getProtocol())) {
            String base64 = Util.base64Encode(basicAuth);
            conn.setRequestProperty("Authorization", 
                                    "Basic " +base64);
          }
        }
        bin = conn.getInputStream();
      } else {
        bin = in;
      }
      BundleImpl res = null;
      try {
        ba = fwCtx.storage.insertBundleJar(location, bin);
      } finally {
        bin.close();
      }

      res = new BundleImpl(fwCtx, ba);

      fwCtx.perm.checkLifecycleAdminPerm(res, checkContext);
      if (res.isExtension()) {
        fwCtx.perm.checkExtensionLifecycleAdminPerm(res, checkContext);
        if (!res.hasPermission(new AllPermission())) {
          throw new SecurityException();
        }
      }

      /* Commit bundle to storage */
      ba.setLastModified(res.getLastModified());

      bundles.put(location, res);

      return res;
    } catch (Exception e) {
      if (ba != null) {
        ba.purge();
      }
      throw new BundleException("Failed to install bundle: " + e,
                                BundleException.UNSPECIFIED, e);
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


  /**
   * Get bundle that has specified bundle identifier.
   *
   * @param id The identifier of bundle to get.
   * @return BundleImpl representing bundle or null
   *         if bundle was not found.
   */
  public Bundle getBundle(long id) {
    if (null==fwCtx) { // This bundles object have been closed!
      throw new IllegalStateException
        ("Bundles.getBundle(id) called on closed bundles object.");
    }
    synchronized (bundles) {
      for (Enumeration e = bundles.elements(); e.hasMoreElements();) {
        BundleImpl b = (BundleImpl)e.nextElement();
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
  public Bundle getBundle(String location) {
    if (null==fwCtx) { // This bundles object have been closed!
      throw new IllegalStateException
        ("Bundles.getBundle(id) called on closed bundles object.");
    }
    return (Bundle) bundles.get(location);
  }


  /**
   * Get bundle that has specified bundle symbolic name and version.
   *
   * @param name The symbolic name of bundle to get.
   * @param version The bundle version of bundle to get.
   * @return BundleImpl for bundle or null.
   */
  BundleImpl getBundle(String name, Version version) {
    if (null==fwCtx) { // This bundles object have been closed!
      throw new IllegalStateException
        ("Bundles.getBundle(id) called on closed bundles object.");
    }
    synchronized (bundles) {
      for (Enumeration e = bundles.elements(); e.hasMoreElements();) {
        BundleImpl b = (BundleImpl)e.nextElement();
        if (name.equals(b.symbolicName) && version.equals(b.version)) {
          return b;
        }
      }
    }
    return null;
  }


  /**
   * Get all installed bundles.
   *
   * @return A Bundle array with bundles.
   */
  List getBundles() {
    if (null==fwCtx) { // This bundles object have been closed!
      throw new IllegalStateException
        ("Bundles.getBundle(id) called on closed bundles object.");
    }
    ArrayList res = new ArrayList(bundles.size());
    synchronized (bundles) {
      res.addAll(bundles.values());
    }
    return res;
  }


  /**
   * Get all bundles that has specified bundle symbolic name.
   *
   * @param name The symbolic name of bundles to get.
   * @return A List of BundleImpl.
   */
  List getBundles(String name) {
    ArrayList res = new ArrayList();
    synchronized (bundles) {
      for (Enumeration e = bundles.elements(); e.hasMoreElements();) {
        BundleImpl b = (BundleImpl)e.nextElement();
        if (name.equals(b.symbolicName)) {
          res.add(b);
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
   * @return A List of BundleImpl.
   */
  List getBundles(String name, VersionRange range) {
    if (null==fwCtx) { // This bundles object have been closed!
      throw new IllegalStateException
        ("Bundles.getBundle(id) called on closed bundles object.");
    }
    List res = getBundles(name);
    for (int i = 0; i < res.size(); ) {
      BundleImpl b = (BundleImpl)res.remove(i);
      if (range.withinRange(b.version)) {
        int j = i;
        while (--j >= 0) {
          if (b.version.compareTo(((BundleImpl)res.get(j)).version) <= 0) {
            break;
          }
        }
        res.add(j + 1, b);
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
  List getActiveBundles() {
    if (null==fwCtx) { // This bundles object have been closed!
      throw new IllegalStateException
        ("Bundles.getBundle(id) called on closed bundles object.");
    }
    ArrayList slist = new ArrayList();
    synchronized (bundles) {
      for (Enumeration e = bundles.elements(); e.hasMoreElements();) {
        BundleImpl b = (BundleImpl)e.nextElement();
        int s = b.getState();
        if (s == Bundle.ACTIVE || s == Bundle.STARTING) {
          slist.add(b);
        }
      }
    }
    return slist;
  }


  /**
   * Try to load any saved framework state.
   * This is done by installing all saved bundles from the local archive
   * copy, and restoring the saved state for each bundle. This is only
   * intended to be executed during the start of the framework.
   *
   */
  synchronized void load() {
    BundleArchive [] bas = fwCtx.storage.getAllBundleArchives();
    for (int i = 0; i < bas.length; i++) {
      try {
        BundleImpl b = new BundleImpl(fwCtx, bas[i]);
        bundles.put(b.location, b);
      } catch (Exception e) {
        try {
          bas[i].setAutostartSetting(-1); // Do not start on launch
          bas[i].setStartLevel(-2); // Mark as uninstalled
        } catch (IOException _ioe) {
        }
        System.err.println("Error: Failed to load bundle "
                           +bas[i].getBundleId()
                           +" (" +bas[i].getBundleLocation() +")"
                           +" uninstalled it!" );
        e.printStackTrace();
      }
    }
  }


  /**
   * Start a list of bundles in order
   *
   * @param slist Bundles to start.
   */
  void startBundles(List slist) {
    // Sort in start order
    for (Iterator i = slist.iterator(); i.hasNext();) {
      BundleImpl rb = (BundleImpl)i.next();
      if (rb.getUpdatedState() == Bundle.RESOLVED) {
        try {
          rb.start();
        } catch (BundleException be) {
          rb.fwCtx.listeners.frameworkError(rb, be);
        }
      }
    }
  }


  /**
   * Returns all fragment bundles that is
   * already attached and targets given bundle.
   *
   * @param target the targetted bundle
   * @return a list of all matching fragment bundles.
   */
  List getFragmentBundles(BundleImpl target) {
    ArrayList retval = new ArrayList();
    for (Enumeration e = bundles.elements(); e.hasMoreElements();) {
      BundleImpl b = (BundleImpl)e.nextElement();
      if (b.isFragment() &&
          b.state != Bundle.UNINSTALLED &&
          b.getFragmentHost() == target) {
        retval.add(b);
      }
    }
    return retval;
  }
}
