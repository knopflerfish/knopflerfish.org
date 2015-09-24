package org.knopflerfish.service.http_servlet_test;

import javax.servlet.http.HttpServletResponse;

import org.knopflerfish.bundle.http_servlet_test.HttpTestServlet;

import junit.framework.TestCase;

/**
 * Test suite for HttpServletResponse API - testing default values
 * This suite must run before the HttpServletResponseTests otherwise
 * several tests will fail
 * 
 */
public class HttpServletResponseTestsDefaultValues extends TestCase {
  
  private HttpServletResponse response;
  boolean isCommitted = false;
  int bufferSize;
  
  public HttpServletResponseTestsDefaultValues() {
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
  }

  // TODO add test for type without charset. There may be a bug in the HTTP server
  public void testContentType() {
    assertNull(response.getContentType());
   }
    
  public void testCharacterEncoding() {
    assertEquals("ISO-8859-1", response.getCharacterEncoding());
  }
  
  public void testLocale() {
    assertNotNull(response.getLocale());
  }
  
}
