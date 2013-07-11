/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.http_test;

import org.knopflerfish.service.http_test.*;

import java.util.*;
import java.net.*;
import org.osgi.framework.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import javax.servlet.http.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.*;
import org.osgi.service.http.HttpContext;

public class HttpTest2Context implements HttpContext {
  String name;
  boolean security = true;

  public HttpTest2Context (String name) {
    this.name = name;
    System.out.println("HttpTest2Context " + name);
  }

  public String getMimeType(String name) {
    String mime = "text/html";
    // return mime;
    return null;	// allow web server to select type
                        // Changed as Christer assumed that this could cause the 
                        // web server to misinterpret pages.
  }

  public URL getResource(String name) {
    System.out.println("HttpTest2Context.getResource " + name + ", this.name=" + this.name);
    try {
      int p1 = name.indexOf("/");
      String s1 = name.substring(p1);
      String s2;
      if (s1.equals("/")) {
        s2 = this.name;
      } else {
        s2 = this.name + s1;
      }
      
      System.out.println("");
      System.out.println("Initial name = " + name );
      System.out.println("Substring = " + s1 );
      System.out.println("Final name = "+ s2);
      URL url = getClass().getResource(s2);
      if (url != null) {
        System.out.println("URL = " + url.toString());
      } else {
        System.out.println("URL = null");
      }
      return url;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean handleSecurity(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) {
    /* Set response status */
    if (security == false) {
       response.setStatus(403);
    }
    return security;
  }

  public void setSecurity(boolean security) {
    this.security = security;
  }
}
