/*
 * Copyright (c) 2004-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.framework_test;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Misc static utility methods.
 */
public class Util {

  /**
   * Install a bundle from a resource file.
   *
   * @param bc context owning both resources and to install bundle from
   * @param resource resource name of bundle jar file
   * @return the installed bundle
   * @throws BundleException if no such resource is found or if
   *                         installation fails.
   */
  public static Bundle installBundle(BundleContext bc, String resource)
    throws BundleException
  {
    try {
      System.out.println("installBundle(" + resource + ")");
      URL url = bc.getBundle().getResource(resource);
      if(url == null) {
        throw new BundleException("No resource " + resource);
      }
      InputStream in = url.openStream();
      if(in == null) {
        throw new BundleException("No resource " + resource);
      }
      return bc.installBundle("internal:" + resource, in);
    } catch (IOException e) {
      throw new BundleException
        ("Failed to get input stream for " + resource + ": " + e);
    }
  }

  public static void updateBundle(BundleContext bc,
                                  Bundle bu,
                                  String resource)
    throws BundleException
  {
    try {
      System.out.println("updateBundle(" +bu.getSymbolicName()
                         +", " + resource + ")");
      URL url = bc.getBundle().getResource(resource);
      if(url == null) {
        throw new BundleException("No resource " + resource);
      }
      InputStream in = url.openStream();
      if(in == null) {
        throw new BundleException("No resource " + resource);
      }
      bu.update(in);
    } catch (IOException e) {
      throw new BundleException
        ("Failed to get input stream for " + resource + ": " + e);
    }
  }

  /**
   * Load URL contents int a byte array
   */
  public static byte[] loadURL(URL url) throws Exception {
    byte[]       buf = new byte[1024];
    InputStream  is  = null;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try {
      is = url.openStream();
      int n;
      while(-1 != (n = is.read(buf))) {
        bout.write(buf, 0, n);
      }
      return bout.toByteArray();
    } finally {
      try { is.close(); } catch (Exception e) {}
    }
  }


  /**
   * Calls package admin refresh packages and waits for the operation
   * to complete.
   *
   * @param bc context owning both resources and to install bundle from
   * @param bundles the inital list of bundles to refresh.
   * @return null on sucess, string with error message on failure.
   */
  public static String refreshPackages(BundleContext bc,
                                       Bundle[] bundles)
  {
    System.out.println("PackageAdmin.refreshPackages("
                       +Arrays.asList(bundles) +")");
    ServiceReference paSR
      = bc.getServiceReference(PackageAdmin.class.getName());
    if (null==paSR)
      return "No package admin service reference.";

    PackageAdmin pa = (PackageAdmin) bc.getService(paSR);
    if (null==pa)
      return "No package admin service.";

    final Object lock = new Object();

    FrameworkListener fListen = new FrameworkListener(){
        public void frameworkEvent(FrameworkEvent event)
        {
          System.out.println("Got framework event of type "+event.getType());
          if (event.getType()==FrameworkEvent.PACKAGES_REFRESHED) {
            synchronized(lock) {
              lock.notifyAll();
            }
          }
        }
      };
    bc.addFrameworkListener(fListen);

    try {
      pa.refreshPackages(bundles);
    } catch (Exception e) {
      e.printStackTrace();
      return "Failed to refresh packages, got exception " +e;
    }

    synchronized (lock) {
      try {
        lock.wait(30000L);
      } catch (InterruptedException ie) {
        System.err.println("Waiting or packages refreshed was interrupted.");
      }
    }
    System.out.println("PackageAdmin.refreshPackages("
                       +Arrays.asList(bundles) +") done.");

    bc.removeFrameworkListener(fListen);

    return null;
  }

}
