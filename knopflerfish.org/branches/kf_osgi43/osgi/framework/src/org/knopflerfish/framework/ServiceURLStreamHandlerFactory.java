/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;


/**
 * Factory creating URLStreamHandlers from both built-in
 * handlers and OSGi-registered URLStreamHandlerServices.
 */
public class ServiceURLStreamHandlerFactory
  implements URLStreamHandlerFactory
{
  /* FrameworkId -> FrameworkContext */
  ArrayList<FrameworkContext> framework = new ArrayList<FrameworkContext>(2);

  //
  // Special framework handlers
  //
  // String protocol -> URLStreamHandler
  Map<String, URLStreamHandler> handlers = new HashMap<String, URLStreamHandler>();

  // JVM classpath handlers. Initialized once at startup
  String[] jvmPkgs = null;

  //
  // OSGi URLStreamHandlerService wrappers
  // This map is not really necessary since the JVM
  // caches handlers anyway, but it just seems nice to have.
  //
  // String (protocol) -> URLStreamHandlerWrapper
  Map<String, URLStreamHandlerWrapper> wrapMap
    = new HashMap<String, URLStreamHandlerWrapper>();

  //
  BundleURLStreamHandler bundleHandler;

  // Debug handle for fw which created this factory
  final private Debug debug;


  ServiceURLStreamHandlerFactory(final FrameworkContext fw) {
    // Initialize JVM classpath packages
    final String s = fw.props.getProperty("java.protocol.handler.pkgs");
    debug = fw.debug;
    if (s != null) {
      jvmPkgs = Util.splitwords(s, "|");
      for(int i = 0; i < jvmPkgs.length; i++) {
        jvmPkgs[i] = jvmPkgs[i].trim();
        if(debug.url) {
          debug.println("JVMClassPath - URLHandler jvmPkgs[" + i + "]=" + jvmPkgs[i]);
        }
      }
    }
    // Add framework protocols
    setURLStreamHandler(ReferenceURLStreamHandler.PROTOCOL, new ReferenceURLStreamHandler());
    bundleHandler = new BundleURLStreamHandler();
    setURLStreamHandler(BundleURLStreamHandler.PROTOCOL, bundleHandler);
  }


  /**
   *
   */
  public URLStreamHandler createURLStreamHandler(String protocol) {
    if (debug.url) {
      debug.println("createURLStreamHandler protocol=" + protocol);
    }

    synchronized (handlers) {

      // Check for
      // 1. JVM classpath handlers
      // 2. Framework built-in handlers
      // 2. OSGi-based handlers
      // 3. system handlers

      URLStreamHandler handler = getJVMClassPathHandler(protocol);
      if(handler != null) {
        if(debug.url) {
          debug.println("using JVMClassPath handler for " + protocol);
        }
        return handler;
      }

      handler = handlers.get(protocol);
      if (handler != null) {
        if (debug.url) {
          debug.println("using predefined handler for " + protocol);
        }
        return handler;
      }

      handler = getServiceHandler(protocol);
      if (handler != null) {
        if (debug.url) {
          debug.println("Using service URLHandler for " + protocol);
        }
        return handler;
      }

      if (debug.url) {
        debug.println("Using default URLHandler for " + protocol);
      }
      return null;
    }
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
    // TBD, should we check that property "java.protocol.handler.pkgs" is equal?
    framework.add(fw);
    bundleHandler.addFramework(fw);
  }


  /**
   * Remove framework that uses this URLStreamHandlerFactory.
   *
   * @param fw Framework context for framework to remove.
   */
  void removeFramework(FrameworkContext fw) {
    framework.remove(fw);
    bundleHandler.removeFramework(fw);
    for (final URLStreamHandlerWrapper urlStreamHandlerWrapper : wrapMap.values()) {
      urlStreamHandlerWrapper.removeFramework(fw);
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
//    TODO true or false?
      for (final FrameworkContext sfw : framework) {
        @SuppressWarnings("unchecked")
        final ServiceReference<URLStreamHandlerService>[] srl
          = (ServiceReference<URLStreamHandlerService>[]) sfw.services
            .get(URLStreamHandlerService.class.getName(), filter, null);

        if (srl != null && srl.length > 0) {
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
    if (jvmPkgs != null) {
      for (final String jvmPkg : jvmPkgs) {
        final String className = jvmPkg + "." + protocol + ".Handler";
        try {
          if (debug.url) {
            debug.println("JVMClassPath - trying URLHandler class=" + className);
          }
          final Class<?> clazz = Class.forName(className);
          final URLStreamHandler handler = (URLStreamHandler)clazz.newInstance();

          if (debug.url) {
            debug.println("JVMClassPath - created URLHandler class=" + className);
          }

          return handler;
        } catch (final Throwable t) {
          if (debug.url) {
            debug.println("JVMClassPath - no URLHandler class " + className);
          }
        }
      }
    }

    if (debug.url) {
      debug.println("JVMClassPath - no URLHandler for " + protocol);
    }

    return null;
  }

}
