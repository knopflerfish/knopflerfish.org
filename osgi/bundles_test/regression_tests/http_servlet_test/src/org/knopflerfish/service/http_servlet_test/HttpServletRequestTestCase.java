/*
 * Copyright (c) 2015 KNOPFLERFISH project
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


package org.knopflerfish.service.http_servlet_test;

import javax.servlet.http.HttpServletRequest;

import org.knopflerfish.bundle.http_servlet_test.HttpServletTestSuite;
import org.knopflerfish.bundle.http_servlet_test.HttpTestServlet;

import junit.framework.TestCase;

/**
 * Test suite for HttpServletRequest API
 * 
 */
public class HttpServletRequestTestCase extends TestCase {
    private HttpServletRequest request;
    
    public HttpServletRequestTestCase() {
      super();
    }
    
    public HttpServletRequestTestCase(String name) {
      super(name);
    }
    
    @Override
    public void setUp() {
      System.out.println("setUp() called");
      request = HttpTestServlet.activeRequest;
    }
    
    public void testLocalPort() {
      assertEquals("Ports differ: port=" + HttpServletTestSuite.port + "localPort= " + request.getLocalPort(),
                   HttpServletTestSuite.port, request.getLocalPort());
    }
    
    public void testRemotePort() {
      assertTrue("Remote port value not correct", request.getRemotePort() > 0);
    }

    public void testLocalAddr() {
      assertNotNull(request.getLocalAddr());
    }
    
    public void testRemoteAddr() {
      assertNotNull(request.getRemoteAddr());
    }
    
    public void testProtocol() {
      assertNotNull(request.getProtocol());
    }
    
    public void testScheme() {
      assertNotNull(request.getScheme());
    }
    
    public void testServerName() {
      assertEquals("localhost", request.getServerName());
    }
    
    public void testMethod() {
      assertEquals(TestData.method, request.getMethod());
    }
    
    public void testContentLenght() {
      if ("GET".equals(TestData.method)) 
        assertEquals(-1, request.getContentLength());
    }
    
    public void testContentType() {
      if ("GET".equals(TestData.method)) 
        assertNull(request.getContentType());
    }
    
    public void testIsSecure() {
      assertEquals(TestData.isSecure, request.isSecure());
    }
    
    public void testQueryString() {
      assertEquals(TestData.queryString, request.getQueryString());
    }
    
    public void testRequestURI() {
      assertEquals(HttpServletTestSuite.TEST_SERVLET_ALIAS, request.getRequestURI());
    }
    
    public void testRequestURL() {
      assertEquals(TestData.requestURL, request.getRequestURL().toString());
    }
    
  }