/*
 * Copyright (c) The Open Services Gateway Initiative (2000-2001).
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
 * A Framework event describing a bundle lifecycle change.
 * <p><tt>BundleEvent</tt> objects are delivered to <tt>BundleListener</tt> objects when a change
 * occurs in a bundle's lifecycle. A type code is used to identify the event type
 * for future extendability.
 *
 * <p>OSGi reserves the right to extend the set of types.
 *
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */

public class BundleEvent extends EventObject
{
    /**
     * Bundle that had a change occur in its lifecycle.
     */
    private transient Bundle bundle;

    /**
     * Type of bundle lifecycle change.
     */
    private transient int type;

    /**
     * This bundle has been installed.
     * <p>The value of <tt>INSTALLED</tt> is 0x00000001.
     *
     * @see BundleContext#installBundle
     */
    public final static int INSTALLED = 0x00000001;

    /**
     * This bundle has been started.
     * <p>The value of <tt>STARTED</tt> is 0x00000002.
     *
     * @see Bundle#start
     */
    public final static int STARTED = 0x00000002;

    /**
     * This bundle has been stopped.
     * <p>The value of <tt>STOPPED</tt> is 0x00000004.
     *
     * @see Bundle#stop
     */
    public final static int STOPPED = 0x00000004;

    /**
     * This bundle has been updated.
     * <p>The value of <tt>UPDATED</tt> is 0x00000008.
     *
     * @see Bundle#update
     */
    public final static int UPDATED = 0x00000008;

    /**
     * This bundle has been uninstalled.
     * <p>The value of <tt>UNINSTALLED</tt> is 0x00000010.
     *
     * @see Bundle#uninstall
     */
    public final static int UNINSTALLED = 0x00000010;

    /**
     * Creates a bundle event of the specified type.
     *
     * @param type The event type.
     * @param bundle The bundle which had a lifecycle change.
     */

    public BundleEvent(int type, Bundle bundle)
    {
        super(bundle);
        this.bundle = bundle;
        this.type = type;
    }

    /**
     * Returns the bundle which had a lifecycle change.
     * This bundle is the source of the event.
     *
     * @return A bundle that had a change occur in its lifecycle.
     */
    public Bundle getBundle()
    {
        return bundle;
    }

    /**
     * Returns the type of lifecyle event.
     * The type values are:
     * <ul>
     * <li>{@link #INSTALLED}
     * <li>{@link #STARTED}
     * <li>{@link #STOPPED}
     * <li>{@link #UPDATED}
     * <li>{@link #UNINSTALLED}
     * </ul>
     *
     * @return The type of lifecycle event.
     */

    public int getType()
    {
        return type;
    }
}


