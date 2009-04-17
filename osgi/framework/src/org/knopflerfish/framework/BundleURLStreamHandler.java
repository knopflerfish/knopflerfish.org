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

import org.osgi.framework.AdminPermission;

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
import java.util.zip.*;

/**
 * Bundle URL handling.
 *
 * @author Jan Stein
 */
public class BundleURLStreamHandler extends URLStreamHandler {

  final public static String PROTOCOL = "bundle";

  final static Permission ADMIN_PERMISSION = new AdminPermission();

  private Bundles bundles;

  BundleURLStreamHandler(Bundles b) {
    bundles = b;
  }

  public URLConnection openConnection(URL u) {
    return new BundleURLConnection(u);
  }

  class BundleURLConnection extends URLConnection {
    private InputStream is = null;

    BundleURLConnection(URL u) {
      super(u);
    }

    public void connect() throws IOException {
      if (!connected) {
        /*
         * AdminPermission is checked for when asking for a resource
         * URL, but it should also enfource it when actually
         * connection to that URL...
         * Not done right now to be backwards compatible.
        // AdminPermission is required to access Bundle URLs.
        SecurityManager sm = System.getSecurityManager();
        if (null!=sm) {
          sm.checkPermission(ADMIN_PERMISSION);
        }
        */
        BundleImpl b = null;
        try {
          b = bundles.getBundle(Long.parseLong(url.getHost()));
        } catch (NumberFormatException ignore) { }
        if (b != null) {
          final BundleArchive a = b.getBundleArchive();
          if (a != null) {
            // Some storage kinds (e.g., expanded storage of sub-JARs)
            // requieres the Framework's permisisons to allow access
            // thus we must call bundleArchive.getInputStream(path)
            // via doPrivileged().
            is = (InputStream)
              AccessController.doPrivileged(new PrivilegedAction() {
                  public Object run() {
                    return a.getInputStream(url.getFile(), url.getPort());
                  }
                });
          }
        }
        if (is != null) {
          connected = true;
        } else {
          throw new IOException("URL not found");
        }
      }
    }

    public InputStream getInputStream() {
      try {
        connect();
      } catch (IOException ignore) { }
      return is;
    }

    public Permission getPermission() throws IOException {
      return ADMIN_PERMISSION;
    }
  }

  /**
   * Equals calculation for bundle URLs.
   * @return <tt>true</tt> if the two urls are
   * considered equal, ie. they refer to the same
   * fragment in the same file.
   *
   * NYI! a complete check!
   */
  protected boolean equals(URL u1, URL u2) {
    String ref1 = u1.getRef();
    String ref2 = u2.getRef();
    return sameFile(u1, u2) &&
      (ref1 == ref2 ||
       (ref1 != null && ref1.equals(ref2)));
  }

  /**
   * Provides the hash calculation
   * @return an <tt>int</tt> suitable for hash table indexing
   */
  protected int hashCode(URL u) {
    int h = 0;

    if (PROTOCOL.equals(u.getProtocol())) {
      String host = u.getHost();
      if (host != null)
        h = host.hashCode();

      String file = u.getFile();
      if (file != null)
        h += file.hashCode();

      String ref = u.getRef();
      if (ref != null)
        h += ref.hashCode();

    } else {
      h = u.hashCode();
    }
    return h;
  }

  /**
   * Compare two urls to see whether they refer to the same file,
   * i.e., having the same protocol, host, port, and path.
   * @return true if u1 and u2 refer to the same file
   */
  protected boolean sameFile(URL u1, URL u2) {
    String p1 = u1.getProtocol();
    if (PROTOCOL.equals(p1)) {
      if (!p1.equals(u2.getProtocol()))
        return false;

      if (!hostsEqual(u1, u2))
        return false;

      if (!(u1.getFile() == u2.getFile() ||
            (u1.getFile() != null && u1.getFile().equals(u2.getFile()))))
        return false;

      if (u1.getPort() != u2.getPort())
        return false;

      return true;
    } else {
      return u1.equals(u2);
    }
  }


  /**
   * Compares the host components of two URLs.
   * @param u1 the URL of the first host to compare
   * @param u2 the URL of the second host to compare
   * @return    <tt>true</tt> if and only if they
   * are equal, <tt>false</tt> otherwise.
   */
  protected boolean hostsEqual(URL u1, URL u2) {
    String s1 = u1.getHost();
    String s2 = u2.getHost();
    return (s1 == s2) || (s1 != null && s1.equals(s2));
  }

}
