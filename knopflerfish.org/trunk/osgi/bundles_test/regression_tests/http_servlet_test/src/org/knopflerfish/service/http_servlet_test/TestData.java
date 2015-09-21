package org.knopflerfish.service.http_servlet_test;

public class TestData {

  // Test data, set by caller, verified in server tests as well
  // when receiving response
  
  public static String queryString;
  public static String method;
  public static boolean isSecure;
  public static String requestURL;
  
  public final static String getResponseBody = "<html><body><h1>HTTP Server Servlet Test Suite</h1></body></html>";

  public final static String postRequestBody = "THE POSTMAN ALWAYS RINGS TWICE\n\tNOVEL AND MOVIE";
  public final static String postResponseBody = "1946;\n\tLANA TURNER";
    
}
