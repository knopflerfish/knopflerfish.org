package org.knopflerfish.bundle.axis;

import java.net.URL;
import org.knopflerfish.util.servlet.WebApp;

import org.osgi.framework.*;
import org.apache.axis.transport.http.AxisServlet;
import org.apache.axis.server.AxisServer;
import org.apache.axis.AxisFault;

import javax.servlet.*;
import javax.servlet.http.*;

public class ServicesServlet extends AxisServlet implements ServiceListener
{
    private void addWSDD(java.io.InputStream stream) {
	System.err.println("ServiceServlet.addWSDD()  not yet implemented");    
    }


      public void serviceChanged(ServiceEvent event) {
System.err.println("ServiceServlet.serviceChanged()");
        try {
          if (event.getType() == ServiceEvent.REGISTERED) {
            ServiceReference sr = event.getServiceReference();
            URL url = (URL) sr.getProperty("AXIS_DEPLOY");
            if (url != null) {
              addWSDD(url.openStream());
	    }
          }
        } catch (Exception e) {e.printStackTrace();}
      }
      
     public AxisServer getEngine() throws AxisFault {
        return Activator.getAxisServer();
    }
    
    
   /**
     * extract the base of our webapp from an inbound request
     *
     * @param request request containing http://foobar/axis/services/something
     * @return some URL like http://foobar:8080/axis/
     */
    protected String getWebappBase(HttpServletRequest request) {
        StringBuffer baseURL=new StringBuffer(128);
        baseURL.append(request.getScheme());
        baseURL.append("://");
        baseURL.append(request.getServerName());
        if(request.getServerPort()!=80) {
            baseURL.append(":");
            baseURL.append(request.getServerPort());
        }
        baseURL.append(request.getContextPath());
        baseURL.append(WebApp.webAppDescriptor.context);
        return baseURL.toString();
    }

}


