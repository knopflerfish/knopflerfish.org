/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.httptest;

import java.util.*;
import java.net.URL;
import java.io.InputStream;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;

//  ********************     HttpTest     ********************
/**
 ** A simple bundle using the HttpService. 
 ** Registers a resource and a servlet.
 */

public class HttpTest implements BundleActivator, HttpContext {
  private ServiceReference httpSRef;
  // new ServiceDescription("javax.osg.services.http.HttpService");
  /** resource alias */
  private static final String RES_ALIAS = "/httptest";
  /** servlet alias */
  private static final String SERVLET_ALIAS = "/httptest/servlet";
  private static final String SERVLET_ALIAS_2 = "/httptest/servlet2";
  static final String TOP_URI = "/foppa";
  private HttpService httpService;
  
  public String bgcolor = "#ffffff";
  
  //----------------------------------------------------------------------------
  //	IMPLEMENTS - BundleActivator	
  //----------------------------------------------------------------------------

  /**
   ** Gets the HttpServive and register the resouce and the servlet
   */
  public void start(BundleContext bc) throws BundleException {
    System.err.println("Starting httptest");
    if ((httpSRef = bc.getServiceReference("org.osgi.service.http.HttpService")) == null)
      throw new BundleException("Failed to get HttpServiceReference");
    if ((httpService = (HttpService)bc.getService(httpSRef)) == null) 
      throw new BundleException("Failed to get HttpService");
    try {
      Hashtable ht = new Hashtable();
      ht.put("in1", "inval1");
      ht.put("in2", "inval2");
      // Register web page, point to /www in the jar file.
      httpService.registerResources(RES_ALIAS, "/www", this);
      httpService.registerResources("/index.html", "/www/index.html", this);
      httpService.registerServlet(SERVLET_ALIAS, new HttpTestServlet(this), ht, this);

      System.err.println("registered resources at " + RES_ALIAS + " and " + "/index.html");
      httpService.registerServlet(SERVLET_ALIAS_2, new HttpTestServlet2(this), ht, this);

      System.err.println("registered servlets at " + SERVLET_ALIAS + " and " + SERVLET_ALIAS_2);
    }
    catch (Exception e) {
      throw new BundleException("Failed to register at HTTP service");
    }


    // Register Publisher
    // Hashtable ht = new Hashtable();
    //ht.put(Publisher.PROP_PAGES_HTTP, new String[] { "/foppa" });
    //bc.registerService(Publisher.PUBLISHER_SERVICE, this, ht);
  }
  
  /**
   ** Stops the bundle, unregister everything at HTTP, then ungets service.
   */
  public void stop(BundleContext bc) throws BundleException {
    try {
      httpService.unregister(RES_ALIAS);
      httpService.unregister(SERVLET_ALIAS);
      bc.ungetService(httpSRef);
      httpService = null;
    }
    catch (Exception e) {
      throw new BundleException("Failed to unregister HTTP resource", e);
    }
  }
  
  //----------------------------------------------------------------------------
  //	IMPLEMENTS - Publisher
  //----------------------------------------------------------------------------
  //public Page getPage(String url) {
  //return new PageImpl(url);
  //  }

  //----------------------------------------------------------------------------
  //	IMPLEMENTS - HttpContext
  //----------------------------------------------------------------------------
  
  // We don't bother about security rigth now
  public boolean handleSecurity(HttpServletRequest  request,
				HttpServletResponse response) 
    throws java.io.IOException 
  {
    return true;
  }
  
  // Get the resource from the jar file, use the class loader to do it
  public URL getResource(String name) {
    //System.out.println("LOOKING FOR - " + name);
    //if ("/index.html".equals(name))
    //name = "/www/index.html";
    URL url = getClass().getResource(name);
    //if (url != null) 
    // System.out.println("LOOKING GOOD - exists");
    return url;
  }

  // Return null and let the HTTP determine the type
  public String getMimeType(String reqEntry) {
    //throw new RuntimeException("Hejsan");
    return null;
  }

}

/*
final class PageImpl implements Page, Content {
  String uri;
  
  PageImpl(String uri) {
    this.uri = uri;
  }

  public long getModified() {
    return -1;
  }

  public String[] getKeywords() {
    return null;
  }

  public Content getContent(Dictionary props, InputStream data) {
    return this;
  }

  public String getContentType() {
    return "text/html";
  }

  public Dictionary getProperties() {
    return null;
  }
  
  public InputStream getInputStream() {
    try {
      System.err.println("Looking for URI: " + uri);
      String s = uri.substring(HttpTest.TOP_URI.length());
      System.err.println("Translates to URI: " + s);
    
      return getClass().getResourceAsStream(s);
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
}
*/
