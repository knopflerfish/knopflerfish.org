/*
 * Copyright (c) 2012, KNOPFLERFISH project
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;


/**
 * Context that extension bundles are given to interact with the
 * Knopflerfish framework implementation.
 *
 * @author Gunnar Ekolin
 */

public class ExtensionContext {

  private final FrameworkContext fwCtx;
  private final BundleGeneration ext;
  private Object activator = null;

  ExtensionContext(final FrameworkContext fwCtx,
                   final BundleGeneration ext) {
    this.fwCtx = fwCtx;
    this.ext   = ext;

    String extActivatorName =
      ext.archive.getAttribute("Extension-Activator");
    extActivatorName = null!=extActivatorName ? extActivatorName.trim() : null;

    try {
      // Extension bundles uses the framework class loader...
      final Class c = getClass().getClassLoader().loadClass(extActivatorName);
      activator = c.newInstance();

      final Method activateMethod
        = c.getMethod("activate", new Class[] { ExtensionContext.class });
      activateMethod.invoke(activator, new Object[] { this } );
    } catch (Exception e) {
      activator = null;
      final String msg = "Failed to activate framework extension "
        +ext.symbolicName + ":" +ext.version;
      fwCtx.log(msg, e);
    }
  }


  /**
   * Register a service posssibly implementing multiple interfaces.
   *
   * @see org.osgi.framework.BundleContext#registerService
   */
  public ServiceRegistration registerService(String[] clazzes,
                                             Object service,
                                             Dictionary properties) {
    return fwCtx.services.register(fwCtx.systemBundle, clazzes,
                                   service, properties);
  }

  /**
   * @return the current bundle class loader for a givne bundle.
   */
  public ClassLoader getClassLoader(Bundle b) {
    BundleImpl bi = (BundleImpl) b;
    return bi.getClassLoader();
  }


  /**
   * The list of bundle class loader listeners.
   */
  private List bclls = new ArrayList();
  /**
   * Register a bundle class loader created listener.
   *
   * @param bcll the bundle class loader listener to register.
   */
  public void addBundleClassLoaderListener(BundleClassLoaderListener bcll)  {
    bclls.add(bcll);
  }

  /**
   * Called by the framework context when a bundle class
   * loader has been created. Will notify all registered listeners.
   *
   * <p>This is a synchronous call.
   *
   * @param bcl the newly created bundle class loader.
   */
  void bundleClassLoaderCreated(final BundleClassLoader bcl) {
    for (Iterator it = bclls.iterator(); it.hasNext(); ) {
      final BundleClassLoaderListener bcll
        = (BundleClassLoaderListener) it.next();
      bcll.bundleClassLoaderCreated(bcl);
    }
  }

}
