package org.knopflerfish.service.resman;

import org.osgi.framework.Bundle;
import java.util.Collection;

public interface ResourceManager {

  public BundleMonitor monitor(Bundle b);
  public void unmonitor(Bundle b);

  public Collection getMonitors();
  public BundleMonitor getMonitor(Bundle b);
}

  
   
