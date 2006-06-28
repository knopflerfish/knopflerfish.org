/*
 * Copyright (c) 2006, KNOPFLERFISH project
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
package org.knopflerfish.bundle.endurance_test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Stolen from framework test suite / event test.
 */

public class Util {
  
  private static final String LOC_PROT = "internal:";
  /**
   * Install a bundle from a resource file.
   *
   * @param bc context owning both resources and to install bundle from
   * @param resource resource name of bundle jar file
   * @return the installed bundle
   * @throws BundleException if no such resource is found or if
   *                         installation fails.
   */
  public static Bundle installBundle(BundleContext bc, String resource) throws BundleException {
    try {
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
      throw new BundleException("Failed to get input stream for " + resource + ": " + e);
    }
  }
  
  public static void updateBundle(BundleContext context, Bundle bundle) {
    try{
      if (bundle.getLocation().startsWith(LOC_PROT)) {
        String resource = bundle.getLocation().substring(LOC_PROT.length());
        URL url = context.getBundle().getResource(resource);
        bundle.update(url.openStream());
      } else {
        bundle.update();
      }
    } catch(Exception e) {
      /* print the error */
      System.out.println("ERROR in Util installBundle()" + e);
      e.printStackTrace();
    }
  }

  
}
