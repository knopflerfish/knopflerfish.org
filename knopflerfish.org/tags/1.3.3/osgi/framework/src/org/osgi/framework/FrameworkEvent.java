/*
 * Copyright (c) The Open Services Gateway Initiative (2000, 2002).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.framework;

import java.util.EventObject;

/**
 * A general Framework event.
 *
 * <p><tt>FrameworkEvent</tt> is the event class used when notifying listeners of general events occuring
 * within the OSGI environment. A type code is used to identify the event type for future extendability.
 *
 * <p>OSGi reserves the right to extend the set of event types.
 *
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */

public class FrameworkEvent extends EventObject
{
    /**
     * Bundle related to the event.
     */
    private transient Bundle bundle;

    /**
     * Exception related to the event.
     */
    private transient Throwable throwable;

    /**
     * Type of event.
     */
    private transient int type;

    /**
     * The Framework has started.
     *
     * <p>This event is broadcast when the Framework has started after all installed bundles that
     * are marked to be started have been started
     * and the Framework has reached the intitial start level.
     *
     * <p>The value of <tt>STARTED</tt> is 0x00000001.
     * @see org.osgi.service.startlevel.StartLevel
     */
    public final static int STARTED = 0x00000001;

    /**
     * An error has occurred.
     *
     * <p>There was an error associated with a bundle.
     *
     * <p>The value of <tt>ERROR</tt> is 0x00000002.
     */
    public final static int ERROR = 0x00000002;

    /**
     * A PackageAdmin.refreshPackage operation has completed.
     *
     * <p>This event is broadcast when the Framework has completed
     * the refresh packages operation initiated by a call to the
     * PackageAdmin.refreshPackages method.
     *
     * <p>The value of <tt>PACKAGES_REFRESHED</tt> is 0x00000004.
     * @since 1.2
     * @see org.osgi.service.packageadmin.PackageAdmin#refreshPackages
     */
    public final static int PACKAGES_REFRESHED = 0x00000004;

    /**
     * A StartLevel.setStartLevel operation has completed.
     *
     * <p>This event is broadcast when the Framework has completed
     * changing the active start level initiated by a call to the
     * StartLevel.setStartLevel method.
     *
     * <p>The value of <tt>STARTLEVEL_CHANGED</tt> is 0x00000008.
     * @since 1.2
     * @see org.osgi.service.startlevel.StartLevel
     */
    public final static int STARTLEVEL_CHANGED = 0x00000008;

    /**
     * Creates a Framework event.
     *
     * @param type The event type.
     * @param source The event source object. This may not be <tt>null</tt>.
     * @deprecated Since 1.2. This constructor is deprecated in favor of using
     * the other constructor with the System Bundle as the event source.
     */
    public FrameworkEvent(int type, Object source)
    {
        super(source);
        this.type = type;
        this.bundle = null;
        this.throwable = null;
    }

    /**
     * Creates a Framework event regarding the specified bundle.
     *
     * @param type The event type.
     * @param bundle The event source.
     * @param throwable The related exception. This argument may be <tt>null</tt>
     * if there is no related exception.
     */
    public FrameworkEvent(int type, Bundle bundle, Throwable throwable )
    {
        super(bundle);
        this.type = type;
        this.bundle = bundle;
        this.throwable = throwable;
    }

    /**
     * Returns the exception associated with the event.
     * <p>If the event type is <tt>ERROR</tt>, this method returns the exception related to the error.
     *
     * @return An exception if an event of type <tt>ERROR</tt> or <tt>null</tt>.
     */
    public Throwable getThrowable()
    {
        return(throwable);
    }

    /**
     * Returns the bundle associated with the event.
     * This bundle is also the source of the event.
     *
     * @return The bundle associated with the event.
     */
    public Bundle getBundle()
    {
        return(bundle);
    }

    /**
     * Returns the type of bundle state change.
     * <p>The type values are:
     * <ul>
     * <li>{@link #STARTED}
     * <li>{@link #ERROR}
     * <li>{@link #PACKAGES_REFRESHED}
     * <li>{@link #STARTLEVEL_CHANGED}
     * </ul>
     * @return The type of state change.
     */

    public int getType()
    {
        return(type);
    }
}


