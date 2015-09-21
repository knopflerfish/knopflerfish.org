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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.knopflerfish.bundle.http_servlet_test.HttpTestServlet;

import junit.framework.TestCase;

/**
 * Test suite for verifying correctnes of HTTP POST bodies
 * 
 */
public class PostRequestTests extends TestCase {
  private HttpServletRequest request;
  
  @Override
  public void setUp() {
    request = HttpTestServlet.activeRequest;
  }
  
  public void testPostBody() {
    try {
      InputStream is = request.getInputStream();
      int len = request.getContentLength();
      
      assertTrue("Content Length not specified in POST", len > 0);
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
      
      byte buf[] = new byte[256];
      int n;
      int toRead = len;
      
      while (( n = is.read(buf, 0, Math.min(buf.length, toRead))) >= 0 && toRead > 0) {
        baos.write(buf, 0, n);
        toRead -= n;
      }

      assertEquals("Read length differs from Content-Length", len, baos.size());
      
      String body = baos.toString("UTF-8");
      assertEquals(TestData.postRequestBody, body);
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
