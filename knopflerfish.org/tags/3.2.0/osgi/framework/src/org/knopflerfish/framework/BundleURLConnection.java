/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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
import java.security.Permission;

import org.osgi.framework.AdminPermission;


/**
 * Bundle URL handling.
 *
 * @author Jan Stein, Gunnar Ekolin
 */
class BundleURLConnection extends URLConnection {
  // Should maybe only allow the bundle that fetched the URL to connect?
  final static Permission ADMIN_PERMISSION
    = new AdminPermission( (String)null, AdminPermission.RESOURCE);

  private BundleResourceStream is = null;
  /**
   * Handle to the current framework instance used in the conversion
   * from bundle id on string form in the URL to the actual bundle
   * instance. */
  private FrameworkContext fwCtx;
  /** The bundle that provides the data for this URL. */
  private BundleImpl bundle;
  private int contentLength;
  private String contentType;
  private long lastModified;

  BundleURLConnection(URL u, FrameworkContext fwCtx) {
    super(u);
    this.fwCtx = fwCtx;
  }

  /**
   * Analyzes the URL to determine the bundle and which of its related
   * bundle archives that actually provides the contents of this
   * URL. The bundle is stored in the private member field
   * <tt>bundle</tt> for later use, the bundle archive is returned.
   *
   * @return The bundle archive that provides the contents of this
   *         bundle URL.
   */
  private BundleArchive getBundleArchive()
  {
    bundle = null;
    long gen = 0;
    try {
      String s = url.getHost();
      int i = s.indexOf('!');
      if (i >= 0) {
        s = s.substring(0,i);
      }
      i = s.indexOf('.');
      if (i >= 0) {
        gen = Long.parseLong(s.substring(i+1));
        s = s.substring(0,i);
      }
      bundle = (BundleImpl) fwCtx.bundles.getBundle(Long.parseLong(s));
    } catch (NumberFormatException _ignore) { }
    if (bundle != null) {
      return bundle.getBundleArchive(gen);
    }
    return null;
  }

  public void connect() throws IOException {
    if (!connected) {
      final BundleArchive a = getBundleArchive();
      if (a != null) {
        // Some storage kinds (e.g., expanded storage of sub-JARs)
        // requieres the Framework's permisisons to allow access
        // thus we must call bundleArchive.getInputStream()
        // via doPrivileged().
        int port = url.getPort();
        is = bundle.secure.callGetBundleResourceStream(a, url.getFile(), port != -1 ? port : 0);
      }
      if (is != null) {
        connected = true;
        if(BundleClassLoader.bDalvik) {
          contentLength = -1;
        } else {
          contentLength = (int)is.getContentLength();
        }
        contentType = URLConnection.guessContentTypeFromName(url.getFile());
        lastModified = a.getLastModified();
      } else {
        throw new IOException("URL not found");
      }
    }
  }

  public InputStream getInputStream() {
    try {
      connect();
    } catch (IOException ignore) {
    }
    return is;
  }

  public String getContentType() {
    try {
      connect();
      return contentType;
    } catch (IOException e) {
      return null;
    }
  }

  public int getContentLength() {
    try {
      connect();
      return contentLength;
    } catch (IOException e) {
      return -1;
    }
  }

  public long getLastModified() {
    try {
      connect();
      return lastModified;
    } catch (IOException e) {
      return 0;
    }
  }

  public Permission getPermission() throws IOException {
    return ADMIN_PERMISSION;
  }

}
