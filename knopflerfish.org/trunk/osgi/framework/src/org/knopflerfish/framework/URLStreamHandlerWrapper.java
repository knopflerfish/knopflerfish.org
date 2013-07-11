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

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;

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
  ArrayList<FrameworkContext> framework = new ArrayList<FrameworkContext>(2);
  final String protocol;
  final String filter;
  final ServiceListener serviceListener;

  private ServiceReference<URLStreamHandlerService> best;
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
          @SuppressWarnings("unchecked")
          final ServiceReference<URLStreamHandlerService> ref =
            (ServiceReference<URLStreamHandlerService>) evt.getServiceReference();
          final FrameworkContext fw = ((BundleImpl)ref.getBundle()).fwCtx;
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
    } catch (final InvalidSyntaxException e) {
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
    } catch (final InvalidSyntaxException _no) { }
  }


  /**
   *
   */
  boolean removeFramework(FrameworkContext fw) {
    framework.remove(fw);
    return framework.isEmpty();
  }


  /**
   *
   */
  private URLStreamHandlerService getService() {
    FrameworkContext fw;
    if (framework.size() == 1) {
      fw = framework.get(0);
    } else {
      // Get current FrameworkContext
      throw new RuntimeException("NYI - walk stack to get framework");
    }
    synchronized (serviceListener) {
      if (best == null) {
        try {
          @SuppressWarnings("unchecked")
          final ServiceReference<URLStreamHandlerService>[] refs =
            (ServiceReference<URLStreamHandlerService>[])
              fw.systemBundle.bundleContext.getServiceReferences(URLStreamHandlerService.class.getName(), filter);
          if (refs != null) {
            // KF gives us highest ranked first.
            best = refs[0];
          }
        } catch (final InvalidSyntaxException _no) { }
      }
      if (best == null) {
        throw new IllegalStateException("null: Lost service for protocol="+ protocol);
      }
      if (bestService == null) {
        bestService = fw.systemBundle.bundleContext.getService(best);
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
  @Override
  public boolean equals(URL u1, URL u2) {
    return getService().equals(u1, u2);
  }


  /**
   *
   */
  @Override
  protected int getDefaultPort() {
    return getService().getDefaultPort();
  }


  /**
   *
   */
  @Override
  protected InetAddress getHostAddress(URL u) {
    return getService().getHostAddress(u);
  }


  /**
   *
   */
  @Override
  protected int hashCode(URL u) {
    return getService().hashCode(u);
  }


  /**
   *
   */
  @Override
  protected boolean hostsEqual(URL u1, URL u2) {
    return getService().hostsEqual(u1, u2);
  }


  /**
   *
   */
  @Override
  protected URLConnection openConnection(URL u) throws IOException {
    try {
      return getService().openConnection(u);
    } catch(final IllegalStateException e) {
      throw new MalformedURLException(e.getMessage());
    }
  }


  /**
   *
   */
  @Override
  protected  void parseURL(URL u, String spec, int start, int limit) {
    getService().parseURL(this, u, spec, start, limit);
  }


  /**
   *
   */
  @Override
  protected  boolean sameFile(URL u1, URL u2) {
    return getService().sameFile(u1, u2);
  }


  /**
   * This method is deprecated, but wrap it in the same
   * way as JSDK1.4 wraps it.
   */
  @Override
  public  void setURL(URL u, String protocol, String host, int port, String file, String ref) {

    // parse host as "user:passwd@host"

    String authority = null;
    String userInfo = null;

    if (host != null && host.length() != 0) {

      authority = (port == -1) ? host : host + ":" + port;

      final int ix = host.lastIndexOf('@');
      if (ix != -1) {
        userInfo = host.substring(0, ix);
        host     = host.substring(ix+1);
      }
    }


    // Parse query part from file ending with '?'
    String path  = null;
    String query = null;

    if (file != null) {
      final int ix = file.lastIndexOf('?');
      if (ix != -1) {
        query = file.substring(ix + 1);
        path  = file.substring(0, ix);
      } else {
        path = file;
      }
    }
    setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
  }

  @Override
  public void setURL(URL u,
                     String protocol,
                     String host,
                     int port,
                     String authority,
                     String userInfo,
                     String path,
                     String query,
                     String ref)
  {
    super
        .setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
  }

  @Override
  protected  String toExternalForm(URL u) {
    return getService().toExternalForm(u);
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();

    sb.append("URLStreamHandlerWrapper[");

    final ServiceReference<URLStreamHandlerService> ref = best;
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

