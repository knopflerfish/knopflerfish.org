/*
 * Copyright (c) 2003-2005, KNOPFLERFISH project
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

import java.net.*;
import org.osgi.service.url.*;
import java.util.Map;
import java.util.HashMap;
import org.osgi.framework.*;

/**
 * Factory creating ContentHandlers from both built-in
 * handlers and OSGi-registered ContentHandlers
 */
public class ServiceContentHandlerFactory 
  implements ContentHandlerFactory 
{
  Framework framework;

  // JVM classpath handlers. Initialized once at startup
  String[] jvmPkgs = null;

  // String (mimetype) -> ContentHandlerWrapper
  Map wrapMap   = new HashMap();

  ServiceContentHandlerFactory(Framework fw) {
    this.framework = fw;

    // Initialize JVM classpath handlers
    String s = System.getProperty("java.content.handler.pkgs", "");
    
    jvmPkgs = Util.splitwords(s, "|");
    for(int i = 0; i < jvmPkgs.length; i++) {
      jvmPkgs[i] = jvmPkgs[i].trim();
      if(Debug.url) {
	Debug.println("JVMClassPathCH - jvmPkgs[" + i + "]=" + jvmPkgs[i]);
      }
    }
  }
  
  public ContentHandler createContentHandler(String mimetype) {
    
    if(Debug.url) {
      Debug.println("createContentHandler protocol=" + mimetype);
    }
    
    ContentHandler handler = getJVMClassPathHandler(mimetype);
    
    if(handler != null) {
      if(Debug.url) {
	Debug.println("using JVMClassPath handler for " + mimetype);
      }
      return handler;
    }
    
        
    handler = getServiceHandler(mimetype);

    if(handler != null) {
      if(Debug.url) {
	Debug.println("Using service ContentHandler for " + mimetype + ", handler=" + handler);
      }
      return handler;
    }
    
    if(Debug.url) {
      Debug.println("Using default ContentHandler for " + mimetype);
    }

    // delegate to system handler
    return null;
  }

  ContentHandler getServiceHandler(String mimetype) {
    try {
      String filter = "(" + URLConstants.URL_CONTENT_MIMETYPE + "=" + mimetype + ")";
      //TODO true or false?
      ServiceReference[] srl = framework.services
	.get(ContentHandler.class.getName(), filter, null, false);
      
      if(srl != null && srl.length > 0) {
	ContentHandlerWrapper wrapper = 
	  (ContentHandlerWrapper)wrapMap.get(mimetype);
	
	if(wrapper == null) {
	  wrapper =  new ContentHandlerWrapper(framework, mimetype);
	  wrapMap.put(mimetype, wrapper);
	}
	return wrapper;
      }
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("Failed to get service: " + e);
    }

    return null;
  }

  

  ContentHandler getJVMClassPathHandler(String mimetype) {
    for(int i = 0; i < jvmPkgs.length; i++) {
      String converted = convertMimetype(mimetype);

      String className = jvmPkgs[i] + "." + converted + ".Handler";
      try { 
	if(Debug.url) {
	  Debug.println("JVMClassPathCH - trying ContentHandler class=" + className);
	}
	Class clazz = Class.forName(className);
	ContentHandler handler = (ContentHandler)clazz.newInstance();
	
	if(Debug.url) {
	  Debug.println("JVMClassPathCH - created ContentHandler class=" + className);
	}

	return handler;
      } catch (Throwable t) {
	if(Debug.url) {
	  Debug.println("JVMClassPathCH - no ContentHandler class " + className);
	}
      }
    }
    
    if(Debug.url) {
      Debug.println("JVMClassPath - no ContentHandler for " + mimetype);
    }
    
    return null;
  }

  // please check this one for correctness
  static String convertMimetype(String s) {

    String bad = ".,:;*-";
    for(int i = 0; i < bad.length(); i++) {
      s = s.replace(bad.charAt(i), '_');
    }

    s = s.replace('/', '.');

    return s;
  }
}

