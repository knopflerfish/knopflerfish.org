package org.knopflerfish.bundle.axis;


import org.apache.axis.server.AxisServer;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;


import org.osgi.framework.*;

import java.io.InputStream;
import java.net.URL;


public class Activator extends org.knopflerfish.util.servlet.WebApp {
  public static BundleContext axisBundle = null;
  private static AxisServer axisServer = null;

  
  public static AxisServer getAxisServer() {return axisServer;}
 
  public WebAppDescriptor getWebAppDescriptor() {
    WebAppDescriptor webApp = new WebAppDescriptor();
    webApp.servlet = new ServletDescriptor[1];    
    webApp.context = "/axis";
    webApp.servlet[0] = new ServletDescriptor("/services",new ServicesServlet());

    return webApp;
  }
  
  public void start(BundleContext bc) throws BundleException {
    try {
      axisBundle = bc;
      URL url = this.getClass().getResource("/axis/server-config.wsdd");
      InputStream is = url.openStream();
      EngineConfiguration fromBundleResource = new FileProvider(is);
      axisServer = new AxisServer(fromBundleResource);
      super.start(bc);
    } catch (Exception e) {
      throw new BundleException("Failed to start server");
    }
  }
  
  public void stop(BundleContext bc) throws BundleException {
    try {
        super.stop(bc);
        axisBundle = null;
        axisServer = null;
    } catch (Exception e) {
      e.printStackTrace();
      throw new BundleException("Failed to stop server", e);
    }
  }
  
}

