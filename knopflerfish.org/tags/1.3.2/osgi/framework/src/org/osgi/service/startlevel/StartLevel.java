/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/framework/src/org/osgi/service/startlevel/StartLevel.java,v 1.1.1.1 2004/03/05 20:35:29 wistrand Exp $
 *
 * Copyright (c) 2002 - IBM Corporation
 * All Rights Reserved.
 * 	
 * These materials have been contributed to the Open Services Gateway
 * Initiative (OSGi) as "MEMBER LICENSED MATERIALS" as defined in, and
 * subject to the terms of, the OSGi Member Agreement by and between OSGi and
 * IBM, specifically including but not limited to, the license
 * rights and warranty disclaimers as set forth in Sections 3.2 and 12.1
 * thereof.
 *
 * All company, brand and product names contained within this document may be
 * trademarks that are the sole property of the respective owners.
 *
 * The above notice must be included on all copies of this document that are
 * made.
 */

package org.osgi.service.startlevel;

import org.osgi.framework.Bundle;

/**
 * The StartLevel service allows management agents to manage a start level
 * assigned to each bundle and the active start level of the Framework.
 * There is at most one StartLevel service present in the OSGi environment.
 *
 * <p>
 * A start level is defined to be a state of execution in which the Framework
 * exists.  StartLevel values are defined as unsigned integers with 0 (zero)
 * being the state where the Framework is not launched.
 * Progressively higher integral values represent
 * progressively higher start levels. e.g. 2 is a higher start level than 1.
 * <p>
 * Access to the StartLevel service is protected by
 * corresponding
 * <tt>ServicePermission</tt>. In addition the <tt>AdminPermission</tt>
 * that is required to actually modify start level information.
 * <p>
 * Start Level support in the Framework includes the ability to
 * control the beginning start level of the Framework, to modify the active
 * start level of the Framework and to assign a specific start level to a bundle.
 * How the beginning start level of a
 * Framework is specified is implementation dependent. It may be a command line
 * argument when invoking the Framework implementation.
 * <p>
 * When the Framework is first started it must be at start level zero.
 * In this state, no bundles are running. This is the initial state of the
 * Framework before it is launched.
 *
 * When the Framework is launched, the Framework will enter start level one
 * and all bundles which are assigned to start level one and are
 * persistently marked to be started are started as described in the
 * <tt>Bundle.start</tt> method.
 * Within a start level, bundles are started in ascending order by <tt>Bundle.getBundleId</tt>.
 * The Framework will continue to increase
 * the start level, starting bundles at each start level, until the Framework has
 * reached a beginning start level. At this point the Framework has completed
 * starting bundles and will then
 * broadcast a Framework event of type <tt>FrameworkEvent.STARTED</tt>
 * to announce it has completed its launch.
 *
 * <p>
 * The StartLevel service can be used by management bundles to alter the active start level
 * of the framework.
 *
 * @version $Revision: 1.1.1.1 $
 * @author BJ Hargrave, IBM Corporation (hargrave@us.ibm.com)
 */

public interface StartLevel
{
    /**
	 * Return the active start level value of the Framework.
	 *
	 * If the Framework is in the process of changing the start level
	 * this method must return the active start level if this
	 * differs from the requested start level.
	 *
	 * @return The active start level value of the Framework.
	 */
    public abstract int getStartLevel();

    /**
	 * Modify the active start level of the Framework.
	 *
	 * <p>The Framework will move to the requested start level. This method
	 * will return immediately to the caller and the start level
	 * change will occur asynchronously on another thread.
	 *
	 * <p>If the specified start level is
	 * higher than the active start level, the
	 * Framework will continue to increase the start level
	 * until the Framework has reached the specified start level,
	 * starting bundles at each
	 * start level which are persistently marked to be started as described in the
	 * <tt>Bundle.start</tt> method.
	 *
	 * At each intermediate start level value on the
	 * way to and including the target start level, the framework must:
	 * <ol>
	 * <li>Change the active start level to the intermediate start level value.
	 * <li>Start bundles at the intermediate start level in
	 * ascending order by <tt>Bundle.getBundleId</tt>.
	 * </ol>
	 * When this process completes after the specified start level is reached,
	 * the Framework will broadcast a Framework event of
	 * type <tt>FrameworkEvent.STARTLEVEL_CHANGED</tt> to announce it has moved to the specified
	 * start level.
	 *
	 * <p>If the specified start level is lower than the active start level, the
	 * Framework will continue to decrease the start level
	 * until the Framework has reached the specified start level
	 * stopping bundles at each
	 * start level as described in the <tt>Bundle.stop</tt> method except that their
	 * persistently recorded state indicates that they must be restarted in the
	 * future.
	 *
	 * At each intermediate start level value on the
	 * way to and including the specified start level, the framework must:
	 * <ol>
	 * <li>Stop bundles at the intermediate start level in
	 * descending order by <tt>Bundle.getBundleId</tt>.
	 * <li>Change the active start level to the intermediate start level value.
	 * </ol>
	 * When this process completes after the specified start level is reached,
	 * the Framework will broadcast a Framework event of
	 * type <tt>FrameworkEvent.STARTLEVEL_CHANGED</tt> to announce it has moved to the specified
	 * start level.
	 *
	 * <p>If the specified start level is equal to the active start level, then
	 * no bundles are started or stopped, however, the Framework must broadcast
	 * a Framework event of type <tt>FrameworkEvent.STARTLEVEL_CHANGED</tt> to
	 * announce it has finished moving to the specified start level. This
	 * event may arrive before the this method return.
	 *
	 * @param startlevel The requested start level for the Framework.
	 * @throws IllegalArgumentException If the specified start level is less than or
	 * equal to zero.
	 * @throws SecurityException If the caller does not have the
	 * <tt>AdminPermission</tt> and the Java runtime environment supports
	 * permissions.
	 */
    public abstract void setStartLevel(int startlevel);

    /**
	 * Return the assigned start level value for the specified Bundle.
	 *
	 * @param bundle The target bundle.
	 * @return The start level value of the specified Bundle.
	 * @exception java.lang.IllegalArgumentException If the specified bundle has been uninstalled.
	 */
    public abstract int getBundleStartLevel(Bundle bundle);

    /**
	 * Assign a start level value to the specified Bundle.
	 *
	 * <p>The specified bundle will be assigned the specified start level. The
	 * start level value assigned to the bundle will be persistently recorded
	 * by the Framework.
	 *
	 * If the new start level for the bundle is lower than or equal to the active start level of
	 * the Framework, the Framework will start the specified bundle as described
	 * in the <tt>Bundle.start</tt> method if the bundle is persistently marked
	 * to be started. The actual starting of this bundle must occur asynchronously.
	 *
	 * If the new start level for the bundle is higher than the active start level of
	 * the Framework, the Framework will stop the specified bundle as described
	 * in the <tt>Bundle.stop</tt> method except that the persistently recorded
	 * state for the bundle indicates that the bundle must be restarted in the
	 * future. The actual stopping of this bundle must occur asynchronously.
	 *
	 * @param bundle The target bundle.
	 * @param startlevel The new start level for the specified Bundle.
	 * @throws IllegalArgumentException
	 * If the specified bundle has been uninstalled or
	 * if the specified start level is less than or equal to zero, or the  specified bundle is
	 * the system bundle.
	 * @throws SecurityException if the caller does not have the
	 * <tt>AdminPermission</tt> and the Java runtime environment supports
	 * permissions.
	 */
    public abstract void setBundleStartLevel(Bundle bundle, int startlevel);

    /**
	 * Return the initial start level value that is assigned
	 * to a Bundle when it is first installed.
	 *
	 * @return The initial start level value for Bundles.
	 * @see #setInitialBundleStartLevel
	 */
    public abstract int getInitialBundleStartLevel();

    /**
     * Set the initial start level value that is assigned
     * to a Bundle when it is first installed.
     *
     * <p>The initial bundle start level will be set to the specified start level. The
     * initial bundle start level value will be persistently recorded
     * by the Framework.
     *
     * <p>When a Bundle is installed via <tt>BundleContext.installBundle</tt>,
     * it is assigned the initial bundle start level value.
     *
     * <p>The default initial bundle start level value is 1
     * unless this method has been
     * called to assign a different initial bundle
     * start level value.
     *
     * <p>Thie method does not change the start level values of installed
     * bundles.
     *
     * @param startlevel The initial start level for newly installed bundles.
     * @throws IllegalArgumentException If the specified start level is less than or
     * equal to zero.
     * @throws SecurityException if the caller does not have the
     * <tt>AdminPermission</tt> and the Java runtime environment supports
     * permissions.
     */
  public abstract void setInitialBundleStartLevel(int startlevel);
  
  /**
   * Return the persistent state of the specified bundle.
   *
   * <p>This method returns the persistent state of a bundle.
   * The persistent state of a bundle indicates whether a bundle
   * is persistently marked to be started when it's start level is
   * reached.
   *
   * @return <tt>true</tt> if the bundle is persistently marked to be started,
   * <tt>false</tt> if the bundle is not persistently marked to be started.
   * @exception java.lang.IllegalArgumentException If the specified bundle has been uninstalled.
   */
  public abstract boolean isBundlePersistentlyStarted(Bundle bundle);
}

