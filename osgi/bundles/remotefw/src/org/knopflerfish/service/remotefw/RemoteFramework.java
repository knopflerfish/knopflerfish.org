package org.knopflerfish.service.remotefw;

import org.osgi.framework.*;

import java.util.Map;


/**
 * Service interface for remote framework connections.
 *
 * <p>
 * By connecting to a remote framework, a client gets access
 * to a <tt>BundleContext</tt> representing the entire remote
 * platform. This BncleContext can be used as a normal context, 
 * adding listeners, getting service references etc. 
 * </p>
 * <p>
 * Some service instances
 * as StartLevel and PackageAdmin may also be remotely accessible
 * via the context. For services not remotely accessible, properties
 * are still available (such as service id, object class array etc)
 * </p>
 */
public interface RemoteFramework  {

  /**
   * Connect to a remote framework.
   * 
   * <p>
   * The host string is implementation specific, but the SOAP
   * implementation uses the form <tt>http://[host]:[port]</tt>
   * </p>
   * <p>
   * If the connection fails, a RuntimeException is thrown.
   * </p>
   */
  public BundleContext connect(String host);

  /**
   * Disconnect from a previously connected framework.
   *
   * @param bc Context representing the remote framework. Must be
   *           a context previously returned from <tt>connect</tt>
   */
  public void disconnect(BundleContext bc);

  /**
   * Get system properties from a remote framework.
   *
   * @param bc Context representing the remote framework. Must be
   *           a context previously returned from <tt>connect</tt>
   */
  public Map getSystemProperties(BundleContext bc);
}
