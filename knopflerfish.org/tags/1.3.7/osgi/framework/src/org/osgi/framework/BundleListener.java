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

import java.util.EventListener;

/**
 * A <tt>BundleEvent</tt> listener.
 *
 * <p><tt>BundleListener</tt> is a listener interface that may be implemented by a bundle developer.
 * <p>A <tt>BundleListener</tt> object is registered with the Framework using the
 * {@link BundleContext#addBundleListener}method.
 * <tt>BundleListener</tt>s are called with a <tt>BundleEvent</tt> object when a bundle has been
 * installed, started, stopped, updated, or uninstalled.
 *
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 * @see BundleEvent
 */

public abstract interface BundleListener extends EventListener
{
    /**
     * Receives notification that a bundle has had a lifecycle change.
     *
     * @param event The <tt>BundleEvent</tt>.
     */
    public abstract void bundleChanged(BundleEvent event);
}


