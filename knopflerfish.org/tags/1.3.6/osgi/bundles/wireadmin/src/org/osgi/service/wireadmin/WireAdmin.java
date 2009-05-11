/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/bundles/wireadmin/src/org/osgi/service/wireadmin/WireAdmin.java,v 1.1.1.1 2004/03/05 20:35:18 wistrand Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2002).
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

package org.osgi.service.wireadmin;

import java.util.Dictionary;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Wire Administration service.
 *
 * <p>This service can be used to create <tt>Wire</tt> objects connecting
 * a Producer service and a Consumer service.
 * <tt>Wire</tt> objects also have wire properties that may be specified
 * when a <tt>Wire</tt> object is created. The Producer and Consumer
 * services may use the <tt>Wire</tt> object's properties to manage or control their
 * interaction.
 * The use of <tt>Wire</tt> object's properties by a Producer or Consumer
 * services is optional.
 *
 * <p>Security Considerations.
 * A bundle must have <tt>ServicePermission[GET,WireAdmin]</tt> to get the Wire Admin service to
 * create, modify, find, and delete <tt>Wire</tt> objects.
 *
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */
public interface WireAdmin
{
    /**
     * Create a new <tt>Wire</tt> object that connects a Producer
     * service to a Consumer service.
     *
     * The Producer service and Consumer service do not
     * have to be registered when the <tt>Wire</tt> object is created.
     *
     * <p>The <tt>Wire</tt> configuration data must be persistently stored.
     * All <tt>Wire</tt> connections are reestablished when the
     * <tt>WireAdmin</tt> service is registered.
     * A <tt>Wire</tt> can be permanently removed by using the
     * {@link #deleteWire} method.
     *
     * <p>The <tt>Wire</tt> object's properties must have case
     * insensitive <tt>String</tt> objects as keys (like the Framework).
     * However, the case of the key must be preserved.
     * The type of the value of the property must be one of the following:
     *
     * <pre>
     * type        = basetype
     *  | vector | arrays
     *
     * basetype = String | Integer | Long
     *  | Float | Double | Byte
     *  | Short | Character
     *  | Boolean
     *
     * primitive   = long | int | short
     *  | char | byte | double | float
     *
     * arrays   = primitive '[]' | basetype '[]'
     *
     * vector   = Vector of basetype
     * </pre>
     *
     * <p>The <tt>WireAdmin</tt> service must automatically add the
     * following <tt>Wire</tt> properties:
     * <ul>
     * <li>
     * {@link WireConstants#WIREADMIN_PID} set to the value of the <tt>Wire</tt> object's
     * persistent identity (PID). This value is generated by the
     * Wire Admin service when a <tt>Wire</tt> object is created.
     * </li>
     * <li>
     * {@link WireConstants#WIREADMIN_PRODUCER_PID} set to the value of
     * Producer service's PID.
     * </li>
     * <li>
     * {@link WireConstants#WIREADMIN_CONSUMER_PID} set to the value of
     * Consumer service's PID.
     * </li>
     * </ul>
     * If the <tt>properties</tt> argument
     * already contains any of these keys, then the supplied values
     * are replaced with the values assigned by the Wire Admin service.
     *
     * <p>The Wire Admin service must broadcast a <tt>WireAdminEvent</tt> of type
     * {@link WireAdminEvent#WIRE_CREATED}
     * after the new <tt>Wire</tt> object becomes available from {@link #getWires}.
     *
     * @param producerPID The <tt>service.pid</tt> of the Producer service
     * to be connected to the <tt>Wire</tt> object.
     * @param consumerPID The <tt>service.pid</tt> of the Consumer service
     * to be connected to the <tt>Wire</tt> object.
     * @param properties The <tt>Wire</tt> object's properties. This argument may be <tt>null</tt>
     * if the caller does not wish to define any <tt>Wire</tt> object's properties.
     * @return The <tt>Wire</tt> object for this connection.
     * @throws java.lang.IllegalArgumentException If
     * <tt>properties</tt> contains case variants of the same key name.
     */
    public Wire createWire(String producerPID, String consumerPID, Dictionary properties);

    /**
     * Delete a <tt>Wire</tt> object.
     *
     * <p>The <tt>Wire</tt> object representing a connection between
     * a Producer service and a Consumer service must be
     * removed.
     * The persistently stored configuration data for the <tt>Wire</tt> object
     * must destroyed. The <tt>Wire</tt> object's method {@link Wire#isValid} will return <tt>false</tt>
     * after it is deleted.
     *
     * <p>The Wire Admin service must broadcast a <tt>WireAdminEvent</tt> of type
     * {@link WireAdminEvent#WIRE_DELETED}
     * after the <tt>Wire</tt> object becomes invalid.
     *
     * @param wire The <tt>Wire</tt> object which is to be deleted.
     */
    public void deleteWire(Wire wire);

    /**
     * Update the properties of a <tt>Wire</tt> object.
     *
     * The persistently stored configuration data for the <tt>Wire</tt> object
     * is updated with the new properties and then the Consumer and Producer
     * services will be called at the respective {@link Consumer#producersConnected}
     * and {@link Producer#consumersConnected} methods.
     *
     * <p>The Wire Admin service must broadcast a <tt>WireAdminEvent</tt> of type
     * {@link WireAdminEvent#WIRE_UPDATED}
     * after the updated properties are available from the <tt>Wire</tt> object.
     *
     * @param wire The <tt>Wire</tt> object which is to be updated.
     * @param properties The new <tt>Wire</tt> object's properties or <tt>null</tt> if no properties are required.
     */
    public void updateWire(Wire wire, Dictionary properties );

    /**
     * Return the <tt>Wire</tt> objects that match the given <tt>filter</tt>.
     *
     * <p>The list of available <tt>Wire</tt> objects is matched against the
     * specified <tt>filter</tt>. <tt>Wire</tt> objects which match the
     * <tt>filter</tt> must be returned. These <tt>Wire</tt> objects are not necessarily
     * connected. The Wire Admin service should not return
     * invalid <tt>Wire</tt> objects, but it is possible that a <tt>Wire</tt>
     * object is deleted after it was placed in the list.
     *
     * <p>The filter matches against the <tt>Wire</tt> object's properties including
     * {@link WireConstants#WIREADMIN_PRODUCER_PID}, {@link WireConstants#WIREADMIN_CONSUMER_PID}
     * and {@link WireConstants#WIREADMIN_PID}.
     *
     * @param filter Filter string to select <tt>Wire</tt> objects
     * or <tt>null</tt> to select all <tt>Wire</tt> objects.
     * @return An array of <tt>Wire</tt> objects which match the <tt>filter</tt>
     * or <tt>null</tt> if no <tt>Wire</tt> objects match the <tt>filter</tt>.
     * @throws org.osgi.framework.InvalidSyntaxException If the specified <tt>filter</tt>
     * has an invalid syntax.
     * @see org.osgi.framework.Filter
     */
    public Wire[] getWires(String filter) throws InvalidSyntaxException;
}

