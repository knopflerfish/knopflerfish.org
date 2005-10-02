/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/bundles/useradmin/src/org/osgi/service/useradmin/UserAdminListener.java,v 1.1.1.1 2004/03/05 20:35:16 wistrand Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2001).
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

package org.osgi.service.useradmin;

/**
 * Listener for UserAdminEvents.
 * 
 * <p>
 * <tt>UserAdminListener</tt> objects are registered with the Framework
 * service registry and notified with a <tt>UserAdminEvent</tt> object when a
 * <tt>Role</tt> object has been created, removed, or modified.
 * <p>
 * <tt>UserAdminListener</tt> objects can further inspect the received
 * <tt>UserAdminEvent</tt> object to determine its type, the <tt>Role</tt>
 * object it occurred on, and the User Admin service that generated it.
 * 
 * @see UserAdmin
 * @see UserAdminEvent
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */
public interface UserAdminListener {

    /**
     * Receives notification that a <tt>Role</tt> object has been created,
     * removed, or modified.
     * 
     * @param event
     *            The <tt>UserAdminEvent</tt> object.
     */
    public void roleChanged(UserAdminEvent event);
}
