package org.knopflerfish.service.remotefw;

import org.osgi.framework.*;

import java.util.Map;


/**
 * Service interface for remote framework connections.
 *
 * <p>
 * By connecting to a remote framework, a client gets access
 * to a <tt>BundleContext</tt> representing the entire remote
 * platform. This BundleContext can be used as a normal context, 
 * installing/starting/stopping bundles, adding listeners, getting 
 * service references etc. 
 * </p>
 *
 * <p>
 * Some service instances
 * as StartLevel and PackageAdmin may also be remotely accessible
 * via the context. For services not remotely accessible, properties
 * are still available (such as service id, object class array etc)
 * </p>
 *
 * <p>
 * Registering service instances are not 
 * likely to be supported. Some differences in bundle and service 
 * event sematics may also be present, since eventing may be handled
 * with some delay.
 * </p>
 *
 * <p>
 * Currently, the only known implementation is by using SOAP access
 * to the remote framework. See 
 * <pre>
 *  osgi/bundles_opt/soap/readme.txt
 * </pre>
 * for more information.
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
   *
   * @return Context representing the remote framework.
   *
   * @throws RuntimeException if the connection fails
   */
  public BundleContext connect(String host);

  /**
   * Disconnect from a previously connected framework.
   *
   * @param bc Context representing the remote framework. If the
   *           context is not previously returned from <tt>connect</tt>,
   *           do nothing.
   */
  public void disconnect(BundleContext bc);

  /**
   * Get system properties from a remote framework.
   *
   * @param bc Context representing the remote framework. Must be
   *           a context previously returned from <tt>connect</tt>
   *
   * @return Map of key/value (String/String) pairs representing 
   *          all system properties on the remote framework.
   *
   * @throws IllegalArgumentException if bc is not a context returned 
   *                                  from <tt>connect</tt>
   */
  public Map getSystemProperties(BundleContext bc);
}
