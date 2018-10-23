/*
 * Copyright (c) 2003-2016, KNOPFLERFISH project
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

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;


/**
 * Factory creating URLStreamHandlers from both built-in
 * handlers and OSGi-registered URLStreamHandlerServices.
 * 
 * TODO: Handle separation between different frameworks.
 */
public class ServiceURLStreamHandlerFactory
  implements URLStreamHandlerFactory
{
  Vector<FrameworkContext> framework = new Vector<FrameworkContext>(2);

  //
  // Special framework handlers
  //
  Hashtable<String, URLStreamHandler> handlers = new Hashtable<String, URLStreamHandler>();

  // JVM classpath handlers. Initialized once at startup
  String[] jvmPkgs = null;

  //
  // OSGi URLStreamHandlerService wrappers
  // This map is not really necessary since the JVM
  // caches handlers anyway, but it just seems nice to have.
  //
  HashMap<String, URLStreamHandlerWrapper> wrapMap
    = new HashMap<String, URLStreamHandlerWrapper>();

  //
  BundleURLStreamHandler bundleHandler;

  // Debug handle for fw which created this factory
  private Debug debug = null;


  ServiceURLStreamHandlerFactory() {
    // Initialize JVM classpath packages
    final String s = System.getProperty("java.protocol.handler.pkgs");
    if (s != null) {
      jvmPkgs = Util.splitwords(s, "|");
      for(int i = 0; i < jvmPkgs.length; i++) {
        jvmPkgs[i] = jvmPkgs[i].trim();
      }
    }
    // Add framework protocols
    setURLStreamHandler(FWResourceURLStreamHandler.PROTOCOL, new FWResourceURLStreamHandler());
    setURLStreamHandler(ReferenceURLStreamHandler.PROTOCOL, ReferenceURLStreamHandler.INSTANCE);
    bundleHandler = new BundleURLStreamHandler();
    setURLStreamHandler(BundleURLStreamHandler.PROTOCOL, bundleHandler);
  }

  /**
   *
   */
  public URLStreamHandler createURLStreamHandler(String protocol) {
    Debug doDebug = debug;
    if (doDebug != null && !doDebug.url) {
      doDebug = null;
    }
    if (doDebug != null) {
      doDebug.println("createURLStreamHandler protocol=" + protocol);
    }

    // Check for
    // 1. JVM classpath handlers
    // 2. Framework built-in handlers
    // 2. OSGi-based handlers
    // 3. system handlers
    URLStreamHandler handler = getJVMClassPathHandler(protocol);
    if (handler != null) {
      if (doDebug != null) {
        doDebug.println("using JVMClassPath handler for " + protocol);
      }
      return handler;
    }
    handler = (URLStreamHandler)handlers.get(protocol);
    if (handler != null) {
      if (doDebug != null) {
        doDebug.println("using predefined handler for " + protocol);
      }
      return handler;
    }

    handler = getServiceHandler(protocol);
    if (handler != null) {
      if (doDebug != null) {
        doDebug.println("Using service URLHandler for " + protocol);
      }
      return handler;
    }

    if (doDebug != null) {
      doDebug.println("Using default URLHandler for " + protocol);
    }
    return null;
  }


  /**
   * Sets the handler for a named protocol.
   *
   * <p>
   * Any old handler for the specified protocol will be lost.
   * </p>
   *
   * @param protocol Protocol name.
   * @param handler Handler for the specified protocol name.
   */
  void setURLStreamHandler(String protocol, URLStreamHandler handler) {
    handlers.put(protocol, handler);
  }


  /**
   * Add framework that uses this URLStreamHandlerFactory.
   *
   * @param fw Framework context for framework to add.
   */
  void addFramework(FrameworkContext fw) {
    bundleHandler.addFramework(fw);
    synchronized (wrapMap) {
      if (debug == null) {
        debug = fw.debug;
      }
      framework.add(fw);
    }
  }


  /**
   * Remove framework that uses this URLStreamHandlerFactory.
   *
   * @param fw Framework context for framework to remove.
   */
  void removeFramework(FrameworkContext fw) {
    bundleHandler.removeFramework(fw);
    synchronized (wrapMap) {
      for (Iterator<Map.Entry<String, URLStreamHandlerWrapper>> i = wrapMap.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry<String, URLStreamHandlerWrapper> e = i.next();
        if ((e.getValue()).removeFramework(fw)) {
          i.remove();
        }
      }
      framework.remove(fw);
      if (debug == fw.debug) {
        if (framework.isEmpty()) {
          debug = null;
        } else {
          debug = framework.get(0).debug;
        }
      }
    }
  }


  /**
   *
   */
  private URLStreamHandler getServiceHandler(final String protocol)
  {
    try {
      final String filter =
        "(" +
        URLConstants.URL_HANDLER_PROTOCOL +
        "=" + protocol +
        ")";
      @SuppressWarnings("unchecked")
      final Vector<FrameworkContext> sfws = (Vector<FrameworkContext>)framework.clone();
      for (final FrameworkContext sfw : sfws) {
        @SuppressWarnings("unchecked")
        final ServiceReference<URLStreamHandlerService>[] srl
          = (ServiceReference<URLStreamHandlerService>[]) sfw.services
            .get(URLStreamHandlerService.class.getName(), filter, sfw.systemBundle, false);

        if (srl != null && srl.length > 0) {
          synchronized (wrapMap) {
            URLStreamHandlerWrapper wrapper = wrapMap.get(protocol);
            if (wrapper == null) {
              wrapper = new URLStreamHandlerWrapper(sfw, protocol);
              wrapMap.put(protocol, wrapper);
            } else {
              wrapper.addFramework(sfw);
            }
            return wrapper;
          }
        }
      }
    } catch (final InvalidSyntaxException e) {
      throw new RuntimeException("Failed to get service: " + e);
    }

    // no handler found
    return null;
  }


  /**
   * Check if there exists a JVM classpath handler for a protocol.
   */
  private URLStreamHandler getJVMClassPathHandler(final String protocol)
  {
    Debug doDebug = debug;
    if (doDebug != null && !doDebug.url) {
      doDebug = null;
    }
    if (jvmPkgs != null) {
      for (final String jvmPkg : jvmPkgs) {
        final String className = jvmPkg + "." + protocol + ".Handler";
        try {
          if (doDebug != null) {
            doDebug.println("JVMClassPath - trying URLHandler class=" + className);
          }
          final Class<?> clazz = Class.forName(className);
          final URLStreamHandler handler = (URLStreamHandler)clazz.newInstance();

          if (doDebug != null) {
            doDebug.println("JVMClassPath - created URLHandler class=" + className);
          }

          return handler;
        } catch (final Throwable t) {
          if (doDebug != null) {
            doDebug.println("JVMClassPath - no URLHandler class " + className);
          }
        }
      }
    }

    if (doDebug != null) {
      doDebug.println("JVMClassPath - no URLHandler for " + protocol);
    }

    return null;
  }

}
