/*
 * Copyright (c) 2015-2017 KNOPFLERFISH project
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

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.knopflerfish.bundle.http_servlet_test.HttpTestServlet;

/**
 * Test suite for HttpServletResponse API
 * 
 */
public class HttpServletResponseTests extends TestCase
{
  private HttpServletResponse response;
  boolean isCommitted = false;
  int bufferSize;
  
  static final String TEXT_HTML_UTF8 = "text/html;charset=UTF-8";
  
  public HttpServletResponseTests() {
    super();
  }
  
  @Override
  public void setUp() {
    System.out.println(getName() + " - setUp() called");
    response = HttpTestServlet.activeResponse;
  }
  
  public void testIsCommitted() {
    assertEquals(isCommitted, response.isCommitted());
  }
  
  public void testBufferSize() {
    bufferSize = response.getBufferSize();
    
    assertTrue("Buffer Size can not be negative", bufferSize > 0);
    
    // Make buffer twice as big, reducing may have no effect
    bufferSize *= 2;
    response.setBufferSize(bufferSize);
    assertEquals(bufferSize, response.getBufferSize());
    
  }
   // TODO add test for type without charset. There may be a bug in the HTTP server
  public void testContentTypeAndCharacterEncoding() {
    String basetype = "text/html";
    String type = basetype;

    // The server seem to append the default charset, we assume this is OK
    assertNull("Expected null ContentType", response.getContentType());
    response.setContentType(type);
    assertEquals(type, response.getContentType());
 
    assertEquals(TestData.ISO_ENCODING, response.getCharacterEncoding());

    response.setCharacterEncoding(TestData.UTF_16_ENCODING);
    assertEquals(TestData.UTF_16_ENCODING, response.getCharacterEncoding());
    assertEquals(type + ";charset=" + TestData.UTF_16_ENCODING, response.getContentType());
    
    type = basetype + ";charset=" + TestData.UTF_8_ENCODING;
    response.setContentType(type);
    assertEquals(type, response.getContentType());
    assertEquals(TestData.UTF_8_ENCODING, response.getCharacterEncoding());
  }
  
  public void testContentTypeViaSetHeader() {
    String basetype = "text/html";
    
    response.setHeader("Content-Type", basetype);
    assertEquals(basetype, response.getContentType());
    assertEquals(TestData.ISO_ENCODING, response.getCharacterEncoding());
    
    response.setHeader("Content-Type", TEXT_HTML_UTF8);
    assertEquals(TEXT_HTML_UTF8, response.getContentType());
    assertEquals(TestData.UTF_8_ENCODING, response.getCharacterEncoding());
  }

  public void testResettingContentType() {
    String type = "text/html";
    
    response.setContentType(type);
    response.setCharacterEncoding(null);
    assertEquals("Expected content type as set: " + type, type, response.getContentType());
    response.setContentType(null);
    assertNull("Expected null ContentType", response.getContentType());
    assertEquals("Expected the default Character Encoding", TestData.ISO_ENCODING, response.getCharacterEncoding());
  }
    
}
