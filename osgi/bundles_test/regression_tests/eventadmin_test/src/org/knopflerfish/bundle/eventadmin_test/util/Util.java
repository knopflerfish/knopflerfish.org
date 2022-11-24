/*
 * Copyright (c) 2006-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.eventadmin_test.util;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class Util{

  private static final String LOC_PROT = "internal:";

  public static Bundle installBundle(BundleContext context, String resource){
    try{
      /* get the class loader */
      //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      /* get the resource */
      //URL url=classLoader.getResource(resource);
      URL url = context.getBundle().getResource(resource);
      /* print the url */
      System.out.println("URL:" + url);
      /* install the bundle */
      return context.installBundle(LOC_PROT + resource, url.openStream());
      //Bundle bundle = context.installBundle(url.toString());
      //return bundle;

    }catch(Exception e){
      /* print the error */
      System.out.println("ERROR in Util installBundle()" + e);
      e.printStackTrace();
      return null;
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
    }catch(Exception e){
      /* print the error */
      System.out.println("ERROR in Util installBundle()" + e);
      e.printStackTrace();
    }
  }

}
