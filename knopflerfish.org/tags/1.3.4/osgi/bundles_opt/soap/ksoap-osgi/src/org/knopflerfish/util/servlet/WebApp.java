/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
package org.knopflerfish.util.servlet;

import java.net.URL;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;

import org.osgi.service.http.*;


/** A <code>WebApp</code> attempts to somewhat mimic a Web application (servlet 2.3)
 * by introducing a descriptor that defines the servlets to be instantiated.
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class WebApp
   implements HttpContext {
   public static WebAppDescriptor webAppDescriptor = null;
   private HttpService httpService;
   private ServiceReference sRef;

   public WebApp(WebAppDescriptor descriptor) {
      webAppDescriptor = descriptor;
   }

   // Return null and let the HTTP determine the type
   public String getMimeType(String reqEntry) {
      return null;
   }

   // Get the resource from the jar file, use the class loader to do it
   public URL getResource(String name) {
      URL url = getClass().getResource(name);

      return url;
   }

   public boolean handleSecurity(HttpServletRequest request, 
                                 HttpServletResponse response)
                          throws java.io.IOException {
      return true;
   }

   public void start(BundleContext bc)
              throws BundleException {
      if ((sRef = bc.getServiceReference("org.osgi.service.http.HttpService")) == null)
         throw new BundleException("Failed to get HttpServiceReference");
      if ((httpService = (HttpService) bc.getService(sRef)) == null)
         throw new BundleException("Failed to get HttpService");
      try {
         WebAppDescriptor wad = webAppDescriptor;

         for (int i = 0; i < wad.servlet.length; i++) {
            ServletDescriptor servlet = wad.servlet[i];

            httpService.registerServlet(wad.context + servlet.subContext, 
                                        servlet.servlet, 
                                        servlet.initParameters, this);
         }
      } catch (Exception e) {
         throw new BundleException("Failed to register servlets");
      }
   }

   public void stop(BundleContext bc)
             throws BundleException {
      try {
         for (int i = 0; i < webAppDescriptor.servlet.length; i++) {
            ServletDescriptor servlet = webAppDescriptor.servlet[i];

            httpService.unregister(
                   webAppDescriptor.context + servlet.subContext);
         }
         bc.ungetService(sRef);
         httpService = null;
         webAppDescriptor = null;
      } catch (Exception e) {
         throw new BundleException("Failed to unregister resources", e);
      }
   }
}
