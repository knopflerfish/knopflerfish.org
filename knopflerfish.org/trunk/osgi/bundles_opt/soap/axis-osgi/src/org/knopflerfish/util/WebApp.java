package org.knopflerfish.util;

import java.util.*;
import java.net.URL;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;


public abstract class WebApp implements BundleActivator, HttpContext {
  private ServiceReference sRef;
  private HttpService httpService;


  public class ServletDescriptor {
    String          subContext;
    HttpServlet     servlet;
    Hashtable       initParameters;

    public ServletDescriptor(String subContext, HttpServlet servlet) {
      this.subContext = subContext;
      this.servlet = servlet;
    }
  } 

  public class WebAppDescriptor {
    public String            context;
    public ServletDescriptor servlet[];
  }

  public static WebAppDescriptor webAppDescriptor = null;
  protected abstract WebAppDescriptor getWebAppDescriptor();

  public void start(BundleContext bc) throws BundleException {
    if ((sRef = bc.getServiceReference("org.osgi.service.http.HttpService")) == null)
      throw new BundleException("Failed to get HttpServiceReference");
    if ((httpService = (HttpService)bc.getService(sRef)) == null) 
      throw new BundleException("Failed to get HttpService");

    try {
      WebAppDescriptor wad = getWebAppDescriptor();
      webAppDescriptor = wad;
      for (int i = 0; i < wad.servlet.length; i++) {
        ServletDescriptor servlet = wad.servlet[i];
        httpService.registerServlet(wad.context+servlet.subContext, servlet.servlet, servlet.initParameters, this);
      }
    }
    catch (Exception e) {
      throw new BundleException("Failed to register servlet");
    }

  }
  
  public void stop(BundleContext bc) throws BundleException {
    try {
      for (int i = 0; i < webAppDescriptor.servlet.length; i++) {
        ServletDescriptor servlet = webAppDescriptor.servlet[i];
        httpService.unregister(webAppDescriptor.context+servlet.subContext);
      }
      bc.ungetService(sRef);
      httpService = null;
      webAppDescriptor = null;
    }
    catch (Exception e) {
      throw new BundleException("Failed to unregister resources", e);
    }
  }
  

  public boolean handleSecurity(HttpServletRequest  request,
				HttpServletResponse response) 
    throws java.io.IOException 
  {
    return true;
  }


  // Get the resource from the jar file, use the class loader to do it
  public URL getResource(String name) {
    URL url = getClass().getResource(name);
    return url;
  }

  // Return null and let the HTTP determine the type
  public String getMimeType(String reqEntry) {
    return null;
  }

}

