package org.knopflerfish.service.resman;

import java.util.Iterator;
import org.osgi.framework.Bundle;

/**
 * Bundle resource usage monitoring.
 *
 * The resource usage and limit methods in this interface all applies
 * to the current, i.e., most recent revisions of this bundle.
 *
 * Setting the limit for some resource via this interface will set the
 * default limit used for all new {@link BundleRevisionMonitor}s
 * created for this bundle. It will also apply the new limit to the
 * current {@link BundleReviosionMonitor}, but leave all old {@link
 * BundleRevisionMonitor}s untouched.
 */
public interface BundleMonitor extends BundleRevisionMonitor {

  /**
   * Get an iterator over all the bundle revisions monitors that exist
   * for the bundle that this class is monitoring.
   */
  Iterator /*<BundleRevisionMonitor>*/ getBundleRevisionMonitors();

}
