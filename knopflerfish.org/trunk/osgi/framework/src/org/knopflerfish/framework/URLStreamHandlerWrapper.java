/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.service.url.*;
import org.osgi.framework.*;

import org.osgi.framework.Constants;

/**
 * Wrapper which delegates an URL protocol to 
 * OSGi URLStreamHandlerServices.
 *
 * <p>
 * Each instance of URLStreamHandlerWrapper tracks URLStreamHandlerServices
 * for a named protocol and selects the best from all available services.
 * </p>
 */
public class URLStreamHandlerWrapper
  extends    URLStreamHandler 
  implements URLStreamHandlerSetter
{
  ArrayList /* FrameworkContext */ framework = new ArrayList(2);
  final String protocol;
  final String filter;
  final ServiceListener serviceListener;
  
  private ServiceReference best;
  private URLStreamHandlerService bestService;
  private FrameworkContext currentFw;

  URLStreamHandlerWrapper(final FrameworkContext  fw,
			  final String proto)
  {
    protocol  = proto;
    filter = "(&(" + Constants.OBJECTCLASS + "=" + 
      URLStreamHandlerService.class.getName() + ")" + 
      "(" + URLConstants.URL_HANDLER_PROTOCOL + "=" + protocol + 
      "))";

    serviceListener = new ServiceListener() {
        synchronized public void serviceChanged(ServiceEvent evt) {
          ServiceReference ref = 
            evt.getServiceReference();
          FrameworkContext fw = ((BundleImpl)ref.getBundle()).fwCtx;
          if (fw == currentFw) {
            switch (evt.getType()) {
            case ServiceEvent.MODIFIED:
              // fall through
            case ServiceEvent.REGISTERED:
              if (best != null && best.compareTo(ref) < 0) {
                best = ref;
                bestService = null;
              }
              break;
            case ServiceEvent.MODIFIED_ENDMATCH:
              // fall through
            case ServiceEvent.UNREGISTERING:
              if (best != null && best.equals(ref)) {
                best = null;
                bestService = null;
              }
            }
          }
        }
      };

    framework.add(fw);
    try {
      fw.systemBundle.bundleContext.addServiceListener(serviceListener, filter);
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Protocol name contains illegal characters: " + proto);
    }

    if (fw.debug.url) {
      fw.debug.println("created wrapper for " + protocol + ", filter=" + filter
                       + ", " + toString());
    }
  }


  /**
   *
   */
  void addFramework(FrameworkContext fw) {
    try {
      fw.systemBundle.bundleContext.addServiceListener(serviceListener, filter);
      framework.add(fw);
      if (fw.debug.url) {
        fw.debug.println("created wrapper for " + protocol + ", filter=" + filter
                         + ", " + toString());
      }
    } catch (InvalidSyntaxException _no) { }
  }


  /**
   *
   */
  void removeFramework(FrameworkContext fw) {
    framework.remove(fw);
  }


  /**
   *
   */
  private URLStreamHandlerService getService() {
    FrameworkContext fw;
    if (framework.size() == 1) {
      fw = (FrameworkContext)framework.get(0);
    } else {
      // Get current FrameworkContext
      throw new RuntimeException("NYI - walk stack to get framework");
    }
    synchronized (serviceListener) {
      if (best == null) {
        try {
          ServiceReference[] refs =
            fw.systemBundle.bundleContext.getServiceReferences(URLStreamHandlerService.class.getName(), filter);
          if (refs != null) {
            // KF gives us highest ranked first.
            best = refs[0];
          }
        } catch (InvalidSyntaxException _no) { }
      }
      if (best == null) {
        throw new IllegalStateException("null: Lost service for protocol="+ protocol);
      }
      if (bestService == null) {
        bestService = (URLStreamHandlerService)fw.systemBundle.bundleContext.getService(best);
      }
      if (bestService == null) {
        throw new IllegalStateException("null: Lost service for protocol=" + protocol);
      }
      currentFw = fw;
      return bestService;
    }
  }


  /**
   *
   */
  public boolean equals(URL u1, URL u2) {
    return getService().equals(u1, u2);
  }


  /**
   *
   */
  protected int getDefaultPort() {
    return getService().getDefaultPort();
  }


  /**
   *
   */
  protected InetAddress getHostAddress(URL u) {
    return getService().getHostAddress(u);
  }


  /**
   *
   */
  protected int hashCode(URL u) {
    return getService().hashCode(u);
  }


  /**
   *
   */
  protected boolean hostsEqual(URL u1, URL u2) {
    return getService().hostsEqual(u1, u2);
  }


  /**
   *
   */
  protected URLConnection openConnection(URL u) throws IOException {
    try {
      return getService().openConnection(u);
    } catch(IllegalStateException e) {
      throw new MalformedURLException(e.getMessage());
    }
  }


  /**
   *
   */
  protected  void parseURL(URL u, String spec, int start, int limit) {
    getService().parseURL(this, u, spec, start, limit);
  }
    

  /**
   *
   */
  protected  boolean sameFile(URL u1, URL u2) {
    return getService().sameFile(u1, u2);
  }

  
  /**
   * This method is deprecated, but wrap it in the same
   * way as JSDK1.4 wraps it.
   */
  public  void setURL(URL u, String protocol, String host, int port, String file, String ref) {
    
    // parse host as "user:passwd@host"

    String authority = null;
    String userInfo = null;

    if (host != null && host.length() != 0) {

      authority = (port == -1) ? host : host + ":" + port;

      int ix = host.lastIndexOf('@');
      if (ix != -1) {
        userInfo = host.substring(0, ix);
        host     = host.substring(ix+1);
      }
    }
        

    // Parse query part from file ending with '?'
    String path  = null;
    String query = null;

    if (file != null) {
      int ix = file.lastIndexOf('?');
      if (ix != -1) {
        query = file.substring(ix + 1);
        path  = file.substring(0, ix);
      } else {
        path = file;
      }
    }
    setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
  }    

  public  void setURL(URL    u, 
		      String protocol,
		      String host, 
		      int    port, 
		      String authority, 
		      String userInfo, 
		      String path, 
		      String query, 
		      String ref) {
    super.setURL(u, protocol, host, port, 
		 authority, userInfo, 
		 path, query, 
		 ref);
  }

  protected  String toExternalForm(URL u) {
    return getService().toExternalForm(u);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("URLStreamHandlerWrapper[");

    ServiceReference ref = best;
    sb.append("protocol=" + protocol);
    //    sb.append(", size=" + tracker.size());
    if(ref != null) {
      sb.append(", id=" + ref.getProperty(Constants.SERVICE_ID));
      sb.append(", rank=" + ref.getProperty(Constants.SERVICE_RANKING));

//       ServiceReference[] srl = tracker.getServiceReferences();
//       for(int i = 0; srl != null && i < srl.length; i++) {
// 	sb.append(", {");
// 	sb.append("id=" + srl[i].getProperty(Constants.SERVICE_ID));
// 	sb.append(", rank=" + srl[i].getProperty(Constants.SERVICE_RANKING));

// 	String[] sa = (String[])srl[i].getProperty(URLConstants.URL_HANDLER_PROTOCOL);
// 	sb.append(", proto=");
	
// 	for(int j = 0; j < sa.length; j++) {
// 	  sb.append(sa[j]);
// 	  if(j < sa.length - 1) {
// 	    sb.append(", ");
// 	  }
// 	}
// 	sb.append("}");
//       }
      
    } else {
      sb.append(" no service tracked");
    }

    sb.append("]");

    return sb.toString();
  }
}
  
