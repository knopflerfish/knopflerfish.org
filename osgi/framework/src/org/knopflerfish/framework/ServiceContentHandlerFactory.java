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

import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;

/**
 * Factory creating ContentHandlers from both built-in
 * handlers and OSGi-registered ContentHandlers
 */
public class ServiceContentHandlerFactory
  implements ContentHandlerFactory
{
  private static final String AVOID_SYSTEM_HANDLER = "org.knopflerfish.framework.avoid_delegating_to_system_content_handler";

  FrameworkContext framework;

  // JVM classpath handlers. Initialized once at startup
  String[] jvmPkgs = null;

  // String (mimetype) -> ContentHandlerWrapper
  Map<String, ContentHandlerWrapper> wrapMap
    = new HashMap<String, ContentHandlerWrapper>();

  ServiceContentHandlerFactory(FrameworkContext fw) {
    this.framework = fw;

    // Initialize JVM classpath handlers
    final String s = framework.props.getProperty("java.content.handler.pkgs");
    if (s != null && s.length() > 0) {
      jvmPkgs = Util.splitwords(s, "|");
      for(int i = 0; i < jvmPkgs.length; i++) {
        jvmPkgs[i] = jvmPkgs[i].trim();
        if(framework.debug.url) {
          framework.debug.println("JVMClassPathCH - jvmPkgs[" + i + "]=" + jvmPkgs[i]);
        }
      }
    }
  }

  public ContentHandler createContentHandler(String mimetype) {

    if(framework.debug.url) {
      framework.debug.println("createContentHandler protocol=" + mimetype);
    }

    ContentHandler handler = getJVMClassPathHandler(mimetype);

    if(handler != null) {
      if(framework.debug.url) {
	framework.debug.println("using JVMClassPath handler for " + mimetype);
      }
      return handler;
    }


    handler = getServiceHandler(mimetype);

    if(handler != null) {
      if(framework.debug.url) {
	framework.debug.println("Using service ContentHandler for " + mimetype
                                + ", handler=" + handler);
      }
      return handler;
    }

    if(framework.debug.url) {
      framework.debug.println("Using default ContentHandler for " + mimetype);
    }

    if (framework.props.getBooleanProperty(AVOID_SYSTEM_HANDLER)) {
      return createContentHandlerWrapper(mimetype);
    }

    // delegate to system handler
    // if we return null here, we will not get another chance for this mimetype since URLConnection.handlers is never reset.
    return null;
  }

  ContentHandler getServiceHandler(String mimetype) {
    try {
      final String filter = "(" + URLConstants.URL_CONTENT_MIMETYPE + "=" + mimetype + ")";
      //TODO true or false?
      @SuppressWarnings("unchecked")
      final ServiceReference<ContentHandler>[] srl
        = (ServiceReference<ContentHandler>[])
        framework.services.get(ContentHandler.class.getName(), filter, framework.systemBundle, false);

      if (srl != null && srl.length > 0) {
        return createContentHandlerWrapper(mimetype);
      }
    } catch (final InvalidSyntaxException e) {
      throw new RuntimeException("Failed to get service: " + e);
    }

    return null;
  }

  private ContentHandler createContentHandlerWrapper(String mimetype) {
    ContentHandlerWrapper wrapper = wrapMap.get(mimetype);

    if (wrapper == null) {
      wrapper = new ContentHandlerWrapper(framework, mimetype);
      wrapMap.put(mimetype, wrapper);
    }
    return wrapper;
  }


  ContentHandler getJVMClassPathHandler(String mimetype) {
    if (jvmPkgs != null) {
      for (final String jvmPkg : jvmPkgs) {
        final String converted = convertMimetype(mimetype);

        final String className = jvmPkg + "." + converted + ".Handler";
        try {
          if(framework.debug.url) {
            framework.debug.println("JVMClassPathCH - trying ContentHandler class="
                                    + className);
          }
          final Class<?> clazz = Class.forName(className);
          final ContentHandler handler = (ContentHandler)clazz.newInstance();

          if(framework.debug.url) {
            framework.debug.println("JVMClassPathCH - created ContentHandler class="
                                    + className);
          }

          return handler;
        } catch (final Throwable t) {
          if(framework.debug.url) {
            framework.debug.println("JVMClassPathCH - no ContentHandler class " + className);
          }
        }
      }
    }

    if(framework.debug.url) {
      framework.debug.println("JVMClassPath - no ContentHandler for " + mimetype);
    }

    return null;
  }

  // please check this one for correctness
  static String convertMimetype(String s) {

    final String bad = ".,:;*-";
    for(int i = 0; i < bad.length(); i++) {
      s = s.replace(bad.charAt(i), '_');
    }

    s = s.replace('/', '.');

    return s;
  }
}

