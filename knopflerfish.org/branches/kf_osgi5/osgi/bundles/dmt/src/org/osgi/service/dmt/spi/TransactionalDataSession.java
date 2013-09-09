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

package org.osgi.service.dmt.spi;

import org.osgi.service.dmt.DmtException;

/**
 * Provides atomic read-write access to the part of the tree handled by the
 * plugin that created this session.
 * 
 * @author $Id: 14e5b379945c794fbb29c0c369e1f652795cf46a $
 */
public interface TransactionalDataSession extends ReadWriteDataSession {

	/**
	 * Commits a series of DMT operations issued in the current atomic session
	 * since the last transaction boundary. Transaction boundaries are the
	 * creation of this object that starts the session, and all subsequent
	 * {@link #commit()} and {@link #rollback()} calls.
	 * <p>
	 * This method can fail even if all operations were successful. This can
	 * happen due to some multi-node semantic constraints defined by a specific
	 * implementation. For example, node A can be required to always have
	 * children A/B, A/C and A/D. If this condition is broken when
	 * {@code commit()} is executed, the method will fail, and throw a
	 * {@code METADATA_MISMATCH} exception.
	 * <p>
	 * In many cases the tree is not the only way to manage a given part of the
	 * system. It may happen that while modifying some nodes in an atomic
	 * session, the underlying settings are modified parallelly outside the
	 * scope of the DMT. If this is detected during commit, an exception with
	 * the code {@code CONCURRENT_ACCESS} is thrown.
	 * 
	 * @throws DmtException with the following possible error codes
	 *         <ul>
	 *         <li>{@code METADATA_MISMATCH} if the operation failed because of
	 *         meta-data restrictions</li><li>{@code CONCURRENT_ACCESS} if it is
	 *         detected that some modification has been made outside the scope
	 *         of the DMT to the nodes affected in the session's operations</li>
	 *         <li> {@code DATA_STORE_FAILURE} if an error occurred while
	 *         accessing the data store</li><li>{@code COMMAND_FAILED} if some
	 *         unspecified error is encountered while attempting to complete the
	 *         command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 */
	void commit() throws DmtException;

	/**
	 * Rolls back a series of DMT operations issued in the current atomic
	 * session since the last transaction boundary. Transaction boundaries are
	 * the creation of this object that starts the session, and all subsequent
	 * {@link #commit} and {@link #rollback} calls.
	 * 
	 * @throws DmtException with the error code {@code ROLLBACK_FAILED} in case
	 *         the rollback did not succeed
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 */
	void rollback() throws DmtException;
}
