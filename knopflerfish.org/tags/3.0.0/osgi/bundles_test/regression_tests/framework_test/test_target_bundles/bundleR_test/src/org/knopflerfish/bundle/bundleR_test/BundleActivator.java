/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.bundleR_test;

import java.io.*;
import java.net.*;

import org.osgi.framework.*;

/*
   Test that class loader delegation works for Bundle.getResource().
   Requires that bundleR1 is installed.
*/

public class BundleActivator implements org.osgi.framework.BundleActivator {

  public void start(BundleContext bc) throws Exception {
    // Try a local package file
    URL u = getClass().getResource("BundleActivator.class");
    if (u != null) {
      InputStream i = u.openStream();
      i.read();
      i.close();
    } else {
      throw new Exception("Did not find BundleActivator.class");
    }
    // Try a local absolute file
    u = bc.getBundle().getResource("META-INF/MANIFEST.MF");
    if (u != null) {
      InputStream i = u.openStream();
      i.read();
      i.close();
    } else {
      throw new Exception("Did not find MANIFEST.MF");
    }
    // Try a non-existent file
    u = bc.getBundle().getResource("apabepa");
    if (u != null) {
      throw new Exception("Got url to non-existent file " + u);
    }
    // Try an imported file
    u = bc.getBundle().getResource("/org/knopflerfish/bundle/bundleR1_test/public/info");
    if (u != null) {
      InputStream i = u.openStream();
      char c = (char)i.read();
      if (c != 'r') {
	throw new Exception("Got wrong first character '" + c + "' from " + u);
      }
      i.close();
    } else {
      throw new Exception("Did not find " + u);
    }
    // Try a private file from imported bundle
    u = bc.getBundle().getResource("/org/knopflerfish/bundle/bundleR1_test/private/info");
    if (u != null) {
      throw new Exception("Got url to private file " + u);
    }
  }

  public void stop(BundleContext bc) {
  }

}
