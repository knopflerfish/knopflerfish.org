/*
 * Copyright (c) OSGi Alliance (2004, 2013). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.dmt.notification;

import org.osgi.service.dmt.DmtException;
import org.osgi.service.dmt.DmtSession;

/**
 * NotificationService enables sending aynchronous notifications to a management
 * server. The implementation of {@code NotificationService} should register
 * itself in the OSGi service registry as a service.
 * 
 * @author $Id: 26ffdcfc2533479d2d48a1c570be3e67f43073eb $
 */
public interface NotificationService {

	/**
	 * Sends a notification to a named principal. It is the responsibility of
	 * the {@code NotificationService} to route the notification to the given
	 * principal using the registered
	 * {@link org.osgi.service.dmt.notification.spi.RemoteAlertSender} services.
	 * <p>
	 * In remotely initiated sessions the principal name identifies the remote
	 * server that created the session, this can be obtained using the session's
	 * {@link DmtSession#getPrincipal() getPrincipal} call.
	 * <p>
	 * The principal name may be omitted if the client does not know the
	 * principal name. Even in this case the routing might be possible if the
	 * Notification Service finds an appropriate default destination (for
	 * example if it is only connected to one protocol adapter, which is only
	 * connected to one management server).
	 * <p>
	 * Since sending the notification and receiving acknowledgment for it is
	 * potentially a very time-consuming operation, notifications are sent
	 * asynchronously. This method should attempt to ensure that the
	 * notification can be sent successfully, and should throw an exception if
	 * it detects any problems. If the method returns without error, the
	 * notification is accepted for sending and the implementation must make a
	 * best-effort attempt to deliver it.
	 * <p>
	 * In case the notification is an asynchronous response to a previous
	 * {@link DmtSession#execute(String, String, String) execute} command, a
	 * correlation identifier can be specified to provide the association
	 * between the execute and the notification.
	 * <p>
	 * In order to send a notification using this method, the caller must have
	 * an {@code AlertPermission} with a target string matching the specified
	 * principal name. If the {@code principal} parameter is {@code null} (the
	 * principal name is not known), the target of the {@code AlertPermission}
	 * must be &quot;*&quot;.
	 * <p>
	 * When this method is called with null correlator, null or empty AlertItem
	 * array, and a 0 code as values, it should send a protocol specific default
	 * notification to initiate a management session. For example, in case of
	 * OMA DM this is alert 1201 "Client Initiated Session". The
	 * {@code principal} parameter can be used to determine the recipient of the
	 * session initiation request.
	 * 
	 * @param principal the principal name which is the recipient of this
	 *        notification, can be {@code null}
	 * @param code the alert code, can be 0 if not needed
	 * @param correlator optional field that contains the correlation identifier
	 *        of an associated exec command, can be {@code null} if not needed
	 * @param items the data of the alert items carried in this alert, can be
	 *        {@code null} or empty if not needed
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code UNAUTHORIZED} when the remote server rejected the
	 *         request due to insufficient authorization</li><li>
	 *         {@code ALERT_NOT_ROUTED} when the alert can not be routed to the
	 *         given principal</li><li>{@code REMOTE_ERROR} in case of
	 *         communication problems between the device and the destination
	 *         </li><li> {@code COMMAND_FAILED} for unspecified errors
	 *         encountered while attempting to complete the command</li><li>
	 *         {@code FEATURE_NOT_SUPPORTED} if the underlying management
	 *         protocol doesn't support asynchronous notifications</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the required
	 *         {@code AlertPermission} with a target matching the
	 *         {@code principal} parameter, as described above
	 */
	void sendNotification(String principal, int code, String correlator, AlertItem[] items) throws DmtException;
}
