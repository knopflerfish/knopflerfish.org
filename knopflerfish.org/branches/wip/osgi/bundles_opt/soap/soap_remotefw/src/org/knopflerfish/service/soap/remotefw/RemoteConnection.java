package org.knopflerfish.service.soap.remotefw;

import org.osgi.framework.*;


public interface RemoteConnection  {

  public BundleContext connect(String host);
  public void disconnect(BundleContext bc);
}
