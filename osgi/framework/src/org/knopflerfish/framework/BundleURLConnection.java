/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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


/**
 * Bundle URL handling.
 *
 * @author Jan Stein
 */
class BundleURLConnection extends URLConnection {

  private InputStream is = null;

  private Bundles bundles;

  BundleURLConnection(URL u, Bundles b) {
    super(u);
    bundles = b;
  }

  public void connect() throws IOException {
    if (!connected) {
      BundleImpl b = null;
      long ai = -1;
      long fi = -1;
      try {
        String s = url.getHost();
        int i = s.indexOf('_');
        if (i >= 0) {
          fi = Long.parseLong(s.substring(i+1));
          s = s.substring(0,i);
        }
        i = s.indexOf('.');
        if (i >= 0) {
          ai = Long.parseLong(s.substring(i+1));
          s = s.substring(0,i);
        }
        b = (BundleImpl)bundles.getBundle(Long.parseLong(s));
      } catch (NumberFormatException _ignore) { }
      if (b != null) {
        BundleArchive a = b.getBundleArchive(ai, fi);
        if (a != null) {
          is = a.getInputStream(url.getFile(), url.getPort());
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
    } catch (IOException ignore) {
    }
    return is;
  }
}
