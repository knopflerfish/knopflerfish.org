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
package org.knopflerfish.bundle.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;



public class Backdoor {
  

  /**
   * Try to get the BundleContext from a Bundle instance using
   * various known backdoors (we don't really rely on R4.1 yet)
   */
  public static BundleContext getBundleContext(Bundle b) {
    Class clazz = b.getClass();
    try {      
      // getBundleContext() is an R4.1 method, but try to grab it 
      // using reflection and punch a hole in the method modifiers. 
      // Should work on recent KF and recent Felix.
      Method m =  clazz.getMethod("getBundleContext", new Class[] { });
      
      m.setAccessible(true);
      return (BundleContext)m.invoke(b, new Object[] { });
    } catch (Exception e) {
      Activator.log.debug("Failed to call Bundle.getBundleContext()", e);

      // Try some known private fields. 
      String[] fieldNames = new String[] {
        "bundleContext", // available in KF
        "context",       // available in Equinox and Concierge
      };
      for(int i = 0; i < fieldNames.length; i++) {
        try {
          Activator.log.debug("Try field " + clazz.getName() + "." + fieldNames[i]);
          
          Field field = clazz.getDeclaredField(fieldNames[i]);
          field.setAccessible(true);
          return (BundleContext)field.get(b);
        } catch (Exception e2) {
          Activator.log.info("Failed: field " + clazz.getName() + "." + fieldNames[i], e2);
        }
      }
    }
    Activator.log.warn("Failed to get BundleContext from bundle #" + b.getBundleId() + ", class=" + clazz.getName());
    return null;
  }

}
