package org.knopflerfish.bundle.axis;


import org.apache.axis.server.AxisServer;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;


import org.osgi.framework.*;

import java.io.InputStream;
import java.net.URL;


public class Activator implements BundleActivator {
  public static BundleContext axisBundle = null;
      // Axis server (shared between instances)
  private static AxisServer axisServer = null;
  private ConfigurationProvider axisConfiguration = null;

  private Server server = null;
  
  public static AxisServer getAxisServer() {return axisServer;}


  public void start(BundleContext bc) throws BundleException {
    try {
      axisBundle = bc;
      URL url = this.getClass().getResource("/axis/server-config.wsdd");
      InputStream is = url.openStream();
      EngineConfiguration fromBundleResource = new FileProvider(is);
      axisConfiguration = new ConfigurationProvider(fromBundleResource);
      axisServer = new AxisServer(axisConfiguration);
      server = new Server();
      server.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new BundleException("Failed to start server");
    }
  }
  
  public void stop(BundleContext bc) throws BundleException {
    try {
        server.stop();
        axisBundle = null;
    } catch (Exception e) {
      e.printStackTrace();
      throw new BundleException("Failed to stop server", e);
    }
  }
  
}

