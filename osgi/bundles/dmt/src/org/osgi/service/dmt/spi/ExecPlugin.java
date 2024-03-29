/*
 * Copyright (c) OSGi Alliance (2004, 2015). All Rights Reserved.
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

package org.osgi.service.dmt.spi;

import org.osgi.service.dmt.DmtException;
import org.osgi.service.dmt.DmtSession;

/**
 * An implementation of this interface takes the responsibility of handling node
 * execute requests in a subtree of the DMT.
 * <p>
 * In an OSGi environment such implementations should be registered at the OSGi
 * service registry specifying the list of root node URIs in a {@code String}
 * array or in case of a single value as {@code String} in the
 * {@code execRootURIs} registration parameter.
 * 
 * @author $Id: 959b9f9e10f172edcb159af99344f8a8b391d0c3 $
 */
public interface ExecPlugin {
	/**
	 * The string to be used as key for the mount points property when an Exec
	 * Plugin is registered with mount points.
	 */
	String	MOUNT_POINTS	= "mountPoints";

	/**
	 * The string to be used as key for the "execRootURIs" property when an
	 * ExecPlugin is registered.
	 * 
	 * @since 2.0
	 */
	String	EXEC_ROOT_URIS	= "execRootURIs";

	/**
	 * Execute the given node with the given data. This operation corresponds to
	 * the EXEC command in OMA DM.
	 * <p>
	 * The semantics of an execute operation and the data parameter it takes
	 * depends on the definition of the managed object on which the command is
	 * issued. Session information is given as it is needed for sending alerts
	 * back from the plugin. If a correlation ID is specified, it should be used
	 * as the {@code correlator} parameter for alerts sent in response to this
	 * execute operation.
	 * <p>
	 * The {@code nodePath} parameter contains an array of path segments
	 * identifying the node to be executed in the subtree of this plugin. This
	 * is an absolute path, so the first segment is always &quot;.&quot;.
	 * Special characters appear escaped in the segments.
	 * 
	 * @param session a reference to the session in which the operation was
	 *        issued, must not be {@code null}
	 * @param nodePath the absolute path of the node to be executed, must not be
	 *        {@code null}
	 * @param correlator an identifier to associate this operation with any
	 *        alerts sent in response to it, can be {@code null}
	 * @param data the parameter of the execute operation, can be {@code null}
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if the node does not exist</li><li>
	 *         {@code  METADATA_MISMATCH} if the command failed because of
	 *         meta-data restrictions</li><li>{@code DATA_STORE_FAILURE} if an
	 *         error occurred while accessing the data store</li><li>
	 *         {@code  COMMAND_FAILED} if some unspecified error is encountered
	 *         while attempting to complete the command</li>
	 *         </ul>
	 * @see DmtSession#execute(String, String)
	 * @see DmtSession#execute(String, String, String)
	 */
	void execute(DmtSession session, String[] nodePath, String correlator, String data) throws DmtException;
}
