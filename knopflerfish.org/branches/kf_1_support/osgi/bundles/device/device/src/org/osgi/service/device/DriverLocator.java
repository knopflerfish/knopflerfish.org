/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/bundles/device/device/src/org/osgi/service/device/DriverLocator.java,v 1.1.1.1 2004/03/05 20:35:06 wistrand Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2000).
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

package org.osgi.service.device;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * A Driver Locator service can find and load device driver bundles given a
 * property set. Each driver is represented by a unique <tt>DRIVER_ID</tt>.
 * <p>
 * Driver Locator services provide the mechanism for dynamically downloading new
 * device driver bundles into an OSGi environment. They are supplied by
 * providers and encapsulate all provider-specific details related to the
 * location and acquisition of driver bundles.
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 * @see Driver
 */

public abstract interface DriverLocator {
    /**
     * Returns an array of <tt>DRIVER_ID</tt> strings of drivers capable of
     * attaching to a device with the given properties.
     * 
     * <p>
     * The property keys in the specified <tt>Dictionary</tt> objects are
     * case-insensitive.
     * 
     * @param props
     *            the properties of the device for which a driver is sought
     * @return array of driver <tt>DRIVER_ID</tt> strings of drivers capable
     *         of attaching to a Device service with the given properties, or
     *         <tt>null</tt> if this Driver Locator service does not know of
     *         any such drivers
     */
    public abstract String[] findDrivers(Dictionary props);

    /**
     * Get an <tt>InputStream</tt> from which the driver bundle providing a
     * driver with the giving <tt>DRIVER_ID</tt> can be installed.
     * 
     * @param id
     *            the <tt>DRIVER_ID</tt> of the driver that needs to be
     *            installed.
     * @return a <tt>InputStream</tt> object from which the driver bundle can
     *         be installed
     * @throws java.io.IOException
     *             the input stream for the bundle cannot be created
     */
    public abstract InputStream loadDriver(String id) throws IOException;
}
