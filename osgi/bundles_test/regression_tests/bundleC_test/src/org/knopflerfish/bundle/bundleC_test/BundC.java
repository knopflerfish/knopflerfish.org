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

package org.knopflerfish.bundle.bundleC_test;

import java.util.*;
import org.knopflerfish.service.bundleC_test.*;
import org.osgi.framework.*;

/*
   This bundle is used to check parts of the functionality of the framework
   It registers itself and its service but does nothing.
*/

public class BundC implements BundleC {
  BundleContext bc;
  // String []serviceDescription = {"org.knopflerfish.service.bundleC_test.BundleC", "java.lang.Object"};
  // ServiceRegistration sreg;
  // ServiceReference servRef;

  public BundC (BundleContext bc) {
    this.bc = bc;
    Hashtable dict = new Hashtable();
    dict.put ("key1","value1");
    dict.put ("key2","value2");
    try {
      // sreg = bc.registerService(serviceDescription, this, dict);
    }

    catch (RuntimeException ru) {
      System.out.println ("Exception " + ru + " in BundleC start"); 
      ru.printStackTrace();
    }
    // servRef = bc.getServiceReference("org.knopflerfish.service.bundleC_test.BundleC");
  }

  // public ServiceReference getServRef() {
  //   System.out.println("Unregister BundleC");
  //   return servRef;
  // }
}
