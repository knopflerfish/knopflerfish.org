/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
import java.util.Hashtable;

import org.osgi.framework.*;

/**
 * Here we handle all the bundles that are installed in framework.
 * Also handles load and save of bundle states to file, so that we
 * can restart the platform.
 *
 * @author Jan Stein
 */
class Bundles {

  /**
   * Table of all installed bundles in this framework.
   * Key is bundle location.
   */
  private Hashtable /* String -> BundleImpl */ bundles = new Hashtable();

  /**
   * Link to framework object.
   */
  private Framework framework;


  /**
   * Create a container for all bundles in this framework.
   */
  Bundles(Framework fw) {
    framework = fw;
    bundles.put(fw.systemBundle.getLocation(), fw.systemBundle);
  }


  /**
   * Install a new bundle.
   *
   * @param location The location to be installed
   */
  BundleImpl install(final String location, final InputStream in) throws BundleException {
    BundleImpl b;
    synchronized (this) {
      b = (BundleImpl) bundles.get(location);
      if (b != null) {
	return b;
      }
      try {
	b = (BundleImpl) AccessController.doPrivileged( new PrivilegedExceptionAction() {
	    public Object run() throws Exception {
	      InputStream bin;
	      if (in == null) {
		// Do it the manual way to have a chance to 
		// set request properties
		URL           url  = new URL(location);
		URLConnection conn = url.openConnection(); 

		// Support for http proxy authentication
		String auth = System.getProperty("http.proxyAuth");
		if(auth != null && !"".equals(auth)) {
		  if("http".equals(url.getProtocol()) ||
		     "https".equals(url.getProtocol())) {
		    String base64 = Util.base64Encode(auth);
		    conn.setRequestProperty("Proxy-Authorization", 
					    "Basic " + base64);
		  }
		}
		bin = conn.getInputStream();
	      } else {
		bin = in;
	      }
	      BundleImpl res = null;
	      try {
		BundleArchive ba = framework.storage.insertBundleJar(location, bin);

		String ee = ba.getAttribute(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if(ee != null) {
		  if(Debug.packages) {
		    Debug.println("bundle #" + ba.getBundleId() + " has EE=" + ee);
		  }
		  if(!framework.isValidEE(ee)) {
		    throw new RuntimeException("Execution environment '" + ee + "' is not supported");
		  }
		}

		res = new BundleImpl(framework, ba);
	      } finally {
		bin.close();
	      }
	      bundles.put(location, res);
	      return res;
	    }
	  });
      } catch (PrivilegedActionException e) {
	//	e.printStackTrace();
	throw new BundleException("Failed to install bundle: " + e.getException(), e.getException());
      } catch (Exception e) {
	//	e.printStackTrace();
	throw new BundleException("Failed to install bundle: " + e, e);
      }
    }
    framework.listeners.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, b));
    return b;
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
  BundleImpl getBundle(long id) {
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
  BundleImpl getBundle(String location) {
    synchronized (bundles) {
      return (BundleImpl) bundles.get(location);
    }
  }


  /**
   * Get all installed bundles.
   *
   * @return A Bundle array with bundles.
   */
  BundleImpl[] getBundles() {
    synchronized (bundles) {
      BundleImpl [] result = new BundleImpl[bundles.size()];
      int i = 0;
      for (Enumeration e = bundles.elements(); e.hasMoreElements();) {
	result[i++] = (BundleImpl)e.nextElement();
      }
      return result;
    }
  }


  /**
   * Get all bundles currently in bundle state ACTIVE.
   *
   * @return A Bundle array with bundles.
   */
  List getActiveBundles() {
    ArrayList slist = new ArrayList();
    synchronized (bundles) {
      int i = 0;
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
   * This is done by installing all saved bundle from the local archive
   * copy. And restoring the saved state for each bundle. This is only
   * intended to be executed during start of framework.
   *
   * @return True if we found a saved state.
   */
  synchronized void load() {
    BundleArchive [] bas = framework.storage.getAllBundleArchives();
    for (int i = 0; i < bas.length; i++) {
      BundleImpl b = new BundleImpl(framework, bas[i]);
      bundles.put(b.location, b);
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
	  rb.framework.listeners.frameworkError(rb, be);
	}
      }
    }    
  }

}
