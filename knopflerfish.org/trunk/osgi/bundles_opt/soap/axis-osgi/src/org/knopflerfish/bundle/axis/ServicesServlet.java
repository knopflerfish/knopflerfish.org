
 
package org.knopflerfish.bundle.axis;

import java.net.URL;
import org.knopflerfish.util.WebApp;



import org.osgi.framework.*;
import org.apache.axis.transport.http.AxisServlet;
import org.apache.axis.server.AxisServer;
import org.apache.axis.AxisFault;

import javax.servlet.*;
import javax.servlet.http.*;

public class ServicesServlet extends AxisServlet implements ServiceListener
{

      public void serviceChanged(ServiceEvent event) {
System.err.println("ServiceServlet.serviceChanged()");
        try {
          if (event.getType() == ServiceEvent.REGISTERED) {
            ServiceReference sr = event.getServiceReference();
            URL url = (URL) sr.getProperty("AXIS_CONFIG");
            if (url != null) {
 	      ConfigurationProvider config = (ConfigurationProvider) Activator.getAxisServer().getConfig();
              config.addWSDD(url.openStream());
	    }
          }
        } catch (Exception e) {e.printStackTrace();}
      }
      
     public AxisServer getEngine() throws AxisFault {
System.err.println("ServiceServlet.getEngine()");
        return Activator.getAxisServer();
    }


    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException,java.io.IOException {
System.err.println("ServiceServlet.doGet()");
      super.doGet(req,res);
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
System.err.println("ServiceServlet.getWebappBase() "+baseURL);
        return baseURL.toString();
    }

}


