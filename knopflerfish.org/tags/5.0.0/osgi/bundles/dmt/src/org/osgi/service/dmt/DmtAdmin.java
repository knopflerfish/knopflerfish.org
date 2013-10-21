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

package org.osgi.service.dmt;

/**
 * An interface providing methods to open sessions and register listeners. The
 * implementation of {@code DmtAdmin} should register itself in the OSGi service
 * registry as a service. {@code DmtAdmin} is the entry point for applications
 * to use the DMT API.
 * <p>
 * The {@code getSession} methods are used to open a session on a specified
 * subtree of the DMT. A typical way of usage:
 * 
 * <pre>
 * serviceRef = context.getServiceReference(DmtAdmin.class.getName());
 * DmtAdmin admin = (DmtAdmin) context.getService(serviceRef);
 * DmtSession session = admin.getSession(&quot;./OSGi/Configuration&quot;);
 * session.createInteriorNode(&quot;./OSGi/Configuration/my.table&quot;);
 * </pre>
 * <p>
 * The methods for opening a session take a node URI (the session root) as a
 * parameter. All segments of the given URI must be within the segment length
 * limit of the implementation, and the special characters '/' and '\' must be
 * escaped (preceded by a '\').
 * <p>
 * See the {@link Uri#encode(String)} method for support on escaping invalid
 * characters in a URI.
 * <p>
 * It is possible to specify a lock mode when opening the session (see lock type
 * constants in {@link DmtSession}). This determines whether the session can run
 * in parallel with other sessions, and the kinds of operations that can be
 * performed in the session. All Management Objects constituting the device
 * management tree must support read operations on their nodes, while support
 * for write operations depends on the Management Object. Management Objects
 * supporting write access may support transactional write, non-transactional
 * write or both. Users of {@code DmtAdmin} should consult the Management Object
 * specification and implementation for the supported update modes. If
 * Management Object definition permits, implementations are encouraged to
 * support both update modes.
 * 
 * @author $Id: 193fb8ed85cf47cdc98b31f0fb1fb11d3bad57eb $
 */
public interface DmtAdmin {

	/**
	 * Opens a {@code DmtSession} for local usage on a given subtree of the DMT
	 * with non transactional write lock. This call is equivalent to the
	 * following:
	 * {@code getSession(null, subtreeUri, DmtSession.LOCK_TYPE_EXCLUSIVE)}
	 * <p>
	 * The {@code subtreeUri} parameter must contain an absolute URI. It can
	 * also be {@code null}, in this case the session is opened with the default
	 * session root, &quot;.&quot;, that gives access to the whole tree.
	 * <p>
	 * To perform this operation the caller must have {@code DmtPermission} for
	 * the {@code subtreeUri} node with the Get action present.
	 * 
	 * @param subtreeUri the subtree on which DMT manipulations can be performed
	 *        within the returned session
	 * @return a {@code DmtSession} object for the requested subtree
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code INVALID_URI} if {@code subtreeUri} is syntactically
	 *         invalid</li><li>{@code URI_TOO_LONG} if {@code subtreeUri} is
	 *         longer than accepted by the {@code DmtAdmin} implementation
	 *         (especially on systems with limited resources)</li><li>
	 *         {@code NODE_NOT_FOUND} if {@code subtreeUri} specifies a
	 *         non-existing node</li><li> {@code SESSION_CREATION_TIMEOUT} if
	 *         the operation timed out because of another ongoing session</li>
	 *         <li>{@code COMMAND_FAILED} if {@code subtreeUri} specifies a
	 *         relative URI, or some unspecified error is encountered while
	 *         attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have
	 *         {@code DmtPermission} for the given root node with the Get action
	 *         present
	 */
	DmtSession getSession(String subtreeUri) throws DmtException;

	/**
	 * Opens a {@code DmtSession} for local usage on a specific DMT subtree with
	 * a given lock mode. This call is equivalent to the following:
	 * {@code getSession(null, subtreeUri, lockMode)}
	 * <p>
	 * The {@code subtreeUri} parameter must contain an absolute URI. It can
	 * also be {@code null}, in this case the session is opened with the default
	 * session root, &quot;.&quot;, that gives access to the whole tree.
	 * <p>
	 * To perform this operation the caller must have {@code DmtPermission} for
	 * the {@code subtreeUri} node with the Get action present.
	 * 
	 * @param subtreeUri the subtree on which DMT manipulations can be performed
	 *        within the returned session
	 * @param lockMode one of the lock modes specified in {@code DmtSession}
	 * @return a {@code DmtSession} object for the requested subtree
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code INVALID_URI} if {@code subtreeUri} is syntactically
	 *         invalid</li><li>{@code URI_TOO_LONG} if {@code subtreeUri} is
	 *         longer than accepted by the {@code DmtAdmin} implementation
	 *         (especially on systems with limited resources)</li><li>
	 *         {@code NODE_NOT_FOUND} if {@code subtreeUri} specifies a
	 *         non-existing node</li><li> {@code FEATURE_NOT_SUPPORTED} if
	 *         atomic sessions are not supported by the implementation and
	 *         {@code lockMode} requests an atomic session</li><li>
	 *         {@code SESSION_CREATION_TIMEOUT} if the operation timed out
	 *         because of another ongoing session</li><li>
	 *         {@code COMMAND_FAILED} if {@code subtreeUri} specifies a relative
	 *         URI, if {@code lockMode} is unknown, or some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have
	 *         {@code DmtPermission} for the given root node with the Get action
	 *         present
	 */
	DmtSession getSession(String subtreeUri, int lockMode) throws DmtException;

	/**
	 * Opens a {@code DmtSession} on a specific DMT subtree using a specific
	 * lock mode on behalf of a remote principal. If local management
	 * applications are using this method then they should provide {@code null}
	 * as the first parameter. Alternatively they can use other forms of this
	 * method without providing a principal string.
	 * <p>
	 * The {@code subtreeUri} parameter must contain an absolute URI. It can
	 * also be {@code null}, in this case the session is opened with the default
	 * session root, &quot;.&quot;, that gives access to the whole tree.
	 * <p>
	 * This method is guarded by {@code DmtPrincipalPermission} in case of
	 * remote sessions. In addition, the caller must have Get access rights (ACL
	 * in case of remote sessions, {@code DmtPermission} in case of local
	 * sessions) on the {@code subtreeUri} node to perform this operation.
	 * 
	 * @param principal the identifier of the remote server on whose behalf the
	 *        data manipulation is performed, or {@code null} for local sessions
	 * @param subtreeUri the subtree on which DMT manipulations can be performed
	 *        within the returned session
	 * @param lockMode one of the lock modes specified in {@code DmtSession}
	 * @return a {@code DmtSession} object for the requested subtree
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code INVALID_URI} if {@code subtreeUri} is syntactically
	 *         invalid</li><li>{@code URI_TOO_LONG} if {@code subtreeUri} is
	 *         longer than accepted by the {@code DmtAdmin} implementation
	 *         (especially on systems with limited resources)</li><li>
	 *         {@code NODE_NOT_FOUND} if {@code subtreeUri} specifies a
	 *         non-existing node</li><li> {@code PERMISSION_DENIED} if
	 *         {@code principal} is not {@code null} and the ACL of the node
	 *         does not allow the {@code Get} operation for the principal on the
	 *         given root node </li><li>{@code FEATURE_NOT_SUPPORTED} if atomic
	 *         sessions are not supported by the implementation and
	 *         {@code lockMode} requests an atomic session</li><li>
	 *         {@code SESSION_CREATION_TIMEOUT} if the operation timed out
	 *         because of another ongoing session</li><li>
	 *         {@code COMMAND_FAILED} if {@code subtreeUri} specifies a relative
	 *         URI, if {@code lockMode} is unknown, or some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException in case of remote sessions, if the caller does
	 *         not have the required {@code DmtPrincipalPermission} with a
	 *         target matching the {@code principal} parameter, or in case of
	 *         local sessions, if the caller does not have {@code DmtPermission}
	 *         for the given root node with the Get action present
	 */
	DmtSession getSession(String principal, String subtreeUri, int lockMode) throws DmtException;

}
