package org.knopflerfish.service.resman;

import org.osgi.framework.Bundle;
import java.util.Collection;

public interface ResourceManager {

  /**
   * Start monitoring a bundle. Fragments can not be monitored since
   * they do not have a class loader.
   *
   * @param b The bundle to start monitoring.
   *
   * @return The bundle monitor for the bundle or <code>null</code> if
   * the given bundle object is a fragment.
   */
  public BundleMonitor monitor(Bundle b);

  /**
   * Stop monitoring a bundle.
   *
   * @param b The bundle to stop monitoring.
   */
  public void unmonitor(Bundle b);


  public Collection getMonitors();

  public BundleMonitor getMonitor(Bundle b);
}
