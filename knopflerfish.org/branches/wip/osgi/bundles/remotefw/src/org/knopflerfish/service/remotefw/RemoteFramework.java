package org.knopflerfish.service.remotefw;

import org.osgi.framework.*;

import java.util.Map;

public interface RemoteFramework  {

  public BundleContext connect(String host);
  public void disconnect(BundleContext bc);

  public Map getSystemProperties(BundleContext bc);
}
