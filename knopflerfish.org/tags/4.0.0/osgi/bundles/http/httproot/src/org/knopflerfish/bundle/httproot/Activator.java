/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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

package org.knopflerfish.bundle.httproot;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator
  implements BundleActivator
{
  // This is my world
  static BundleContext bc;
  static LogRef        log;

  // Aliases for HttpService registrations
  private final static String ALIAS_ROOT = "/";
  private final static String ALIAS_DOCS = "/docs";
  private final static String ALIAS_SERVLET = "/servlet/knopflerfish-info";

  // Directory inside the bundle that holds the resources to be published
  private final static String  RES_ROOT = "/www";

  private ServiceTracker httpTracker;


  public void start(final BundleContext bc)
    throws BundleException
  {
    Activator.bc  = bc;
    Activator.log = new LogRef(bc);

    httpTracker = new ServiceTracker(bc, HttpService.class.getName(), null) {
        public Object addingService(final ServiceReference sr)
        {
          return register(sr);
        }

        public void removedService(final ServiceReference sr,
                                   final Object service)
        {
          unregister((HttpService) service);
        }
      };
    httpTracker.open();
  }

  public void stop(final BundleContext bc)
    throws BundleException
  {
    httpTracker.close();
  }


  private HttpService register(final ServiceReference sr)
  {
    log.info("Registering with added HttpService");

    final HttpService http = (HttpService) bc.getService(sr);

    try {
      http.registerResources(ALIAS_ROOT, RES_ROOT, new CompletingHttpContext());
      http.registerServlet(ALIAS_SERVLET, new InfoServlet(sr),
                           new Hashtable(), null);
      http.registerResources(ALIAS_DOCS, "", new DirectoryHttpContext());
    } catch (Exception e) {
      log.error("Failed to register in HttpService: " +e.getMessage(), e);
    }

    log.info("Registration done.");
    return http;
  }

  protected void unregister(HttpService http) {
    log.info("Unregistering from removed HttpService");

    http.unregister(ALIAS_ROOT);
    http.unregister(ALIAS_SERVLET);
    http.unregister(ALIAS_DOCS);

    log.info("Unregistration done.");
  }

  /**
   * An HttpContext implementation that adds "index.html" to all
   * requests ending in a '/'. This avoids returning directory
   * listings when the name parameter points to a directory.
   */
  static class CompletingHttpContext
    implements HttpContext
  {
    public boolean handleSecurity(final HttpServletRequest request,
                                  final HttpServletResponse response)
      throws java.io.IOException
    {
      return true;
    }

    public URL getResource(String name)
    {
      // Add "index.html" to all directories
      if (name.endsWith("/")) {
        name += "index.html";
      }
      return getClass().getResource(name);
    }

    public String getMimeType(final String reqEntry) {
      return null;
    }
  }

  /**
   * An HttpContext implementation that serves files from a directory
   * in the local file system (or from another http-server).
   */
  static class DirectoryHttpContext
    implements HttpContext
  {
    // Default location of documentation relative to the
    // osgi-directory in a Knopflerfish distribution.
    private final static String RES_DOC_ROOT = "../docs";

    // Fall-back http-URL for the documentation
    private static final String  RES_DOC_URL
      = "http://www.knopflerfish.org/releases/current/docs";

    private File baseDir = null;
    private URL  baseURL  = null;

    public DirectoryHttpContext() {
      try {
        baseDir = new File(RES_DOC_ROOT);
        if (!baseDir.exists() || !baseDir.isDirectory()
            || !new File(baseDir,"index.html").canRead()) {
          baseDir = null;
          baseURL = new URL(RES_DOC_URL);
        }
        log.info("Documentation from "
                 +(null==baseDir ? baseURL.toString() : baseDir.toString()));
      } catch (Exception e) {
        log.error("Failed to create base URL.", e);
      }
    }

    public boolean handleSecurity(final HttpServletRequest  request,
                                  final HttpServletResponse response)
      throws java.io.IOException
    {
      return true;
    }

    public URL getResource(String name)
    {
      // Default to index.html to avoid publishing directory listings
      if (name.endsWith("/")) {
        name += "index.html";
      }
      try {
        URL url = null;
        if (null!=baseDir) {
          File f = new File(baseDir, name);
          if (f.exists()) {
            if (f.isDirectory()) {
              f = new File(f, "index.html");
            }
            url = f.toURI().toURL();
          }
        } else {
          url = new URL(baseURL, name);
        }
        return url;
      } catch (Exception e) {
        log.error("DirectoryHttpContext: Failed to create resource URL.", e);
      }
      return null;
    }

    public String getMimeType(final String reqEntry)
    {
      return null;
    }
  };
}
