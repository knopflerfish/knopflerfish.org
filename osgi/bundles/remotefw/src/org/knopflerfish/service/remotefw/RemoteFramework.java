package org.knopflerfish.service.remotefw;

import org.osgi.framework.*;


public interface RemoteFramework  {

  public BundleContext connect(String host);
  public void disconnect(BundleContext bc);
}
