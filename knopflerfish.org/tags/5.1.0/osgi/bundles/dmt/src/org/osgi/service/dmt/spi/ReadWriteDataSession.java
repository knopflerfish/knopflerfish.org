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

import org.osgi.service.dmt.DmtData;
import org.osgi.service.dmt.DmtException;
import org.osgi.service.dmt.DmtSession;
import org.osgi.service.dmt.MetaNode;

/**
 * Provides non-atomic read-write access to the part of the tree handled by the
 * plugin that created this session.
 * <p>
 * The {@code nodePath} parameters appearing in this interface always contain an
 * array of path segments identifying a node in the subtree of this plugin. This
 * parameter contains an absolute path, so the first segment is always
 * &quot;.&quot;. Special characters appear escaped in the segments.
 * <p>
 * <strong>Error handling</strong>
 * <p>
 * When a tree manipulation command is called on the DmtAdmin service, it must
 * perform an extensive set of checks on the parameters and the authority of the
 * caller before delegating the call to a plugin. Therefore plugins can take
 * certain circumstances for granted: that the path is valid and is within the
 * subtree of the plugin and the session, the command can be applied to the
 * given node (e.g. the target of {@code setNodeValue} is a leaf node), etc. All
 * errors described by the error codes {@link DmtException#INVALID_URI},
 * {@link DmtException#URI_TOO_LONG}, {@link DmtException#PERMISSION_DENIED},
 * {@link DmtException#COMMAND_NOT_ALLOWED} and
 * {@link DmtException#TRANSACTION_ERROR} are fully filtered out before control
 * reaches the plugin.
 * <p>
 * If the plugin provides meta-data for a node, the DmtAdmin service must also
 * check the constraints specified by it, as described in {@link MetaNode}. If
 * the plugin does not provide meta-data, it must perform the necessary checks
 * for itself and use the {@link DmtException#METADATA_MISMATCH} error code to
 * indicate such discrepancies.
 * <p>
 * The DmtAdmin does not check that the targeted node exists (or that it does
 * not exist, in case of a node creation) before calling the plugin. It is the
 * responsibility of the plugin to perform this check and to throw a
 * {@link DmtException#NODE_NOT_FOUND} or
 * {@link DmtException#NODE_ALREADY_EXISTS} if needed. In this case the DmtAdmin
 * must pass through this exception to the caller of the corresponding
 * DmtSession method.
 * <p>
 * The plugin can use the remaining error codes as needed. If an error does not
 * fit into any other category, the {@link DmtException#COMMAND_FAILED} code
 * should be used.
 * 
 * @author $Id: 8237e8ee3cf4e7e5156de813f614986b808bd0f3 $
 */
public interface ReadWriteDataSession extends ReadableDataSession {

	/**
	 * Create a copy of a node or a whole subtree. Beside the structure and
	 * values of the nodes, most properties managed by the plugin must also be
	 * copied, with the exception of the Timestamp and Version properties.
	 * 
	 * @param nodePath an absolute path specifying the node or the root of a
	 *        subtree to be copied
	 * @param newNodePath the absolute path of the new node or root of a subtree
	 * @param recursive {@code false} if only a single node is copied,
	 *        {@code true} if the whole subtree is copied
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a
	 *         non-existing node, or if {@code newNodePath} points to a node
	 *         that cannot exist in the tree</li>
	 *         <li>{@code NODE_ALREADY_EXISTS} if {@code newNodePath} points to
	 *         a node that already exists</li>
	 *         <li>{@code METADATA_MISMATCH} if the node could not be copied
	 *         because of meta-data restrictions</li>
	 *         <li>{@code FEATURE_NOT_SUPPORTED} if the copy operation is not
	 *         supported by the plugin</li>
	 *         <li>{@code DATA_STORE_FAILURE} if an error occurred while
	 *         accessing the data store</li>
	 *         <li>{@code COMMAND_FAILED} if some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#copy(String, String, boolean)
	 */
	void copy(String[] nodePath, String[] newNodePath, boolean recursive) throws DmtException;

	/**
	 * Create an interior node with a given type. The type of interior node, if
	 * specified, is a URI identifying a DDF document.
	 * 
	 * @param nodePath the absolute path of the node to create
	 * @param type the type URI of the interior node, can be {@code null} if no
	 *        node type is defined
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a node
	 *         that cannot exist in the tree</li>
	 *         <li>{@code NODE_ALREADY_EXISTS} if {@code nodeUri} points to a
	 *         node that already exists</li>
	 *         <li>{@code METADATA_MISMATCH} if the node could not be created
	 *         because of meta-data restrictions</li>
	 *         <li>{@code DATA_STORE_FAILURE} if an error occurred while
	 *         accessing the data store</li>
	 *         <li>{@code COMMAND_FAILED} if some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#createInteriorNode(String)
	 * @see DmtSession#createInteriorNode(String, String)
	 */
	void createInteriorNode(String[] nodePath, String type) throws DmtException;

	/**
	 * Create a leaf node with a given value and MIME type. If the specified
	 * value or MIME type is {@code null}, their default values must be taken.
	 * 
	 * @param nodePath the absolute path of the node to create
	 * @param value the value to be given to the new node, can be {@code null}
	 * @param mimeType the MIME type to be given to the new node, can be
	 *        {@code null}
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a node
	 *         that cannot exist in the tree</li>
	 *         <li>{@code NODE_ALREADY_EXISTS} if {@code nodePath} points to a
	 *         node that already exists</li>
	 *         <li>{@code METADATA_MISMATCH} if the node could not be created
	 *         because of meta-data restrictions</li>
	 *         <li>{@code DATA_STORE_FAILURE} if an error occurred while
	 *         accessing the data store</li>
	 *         <li>{@code COMMAND_FAILED} if some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#createLeafNode(String)
	 * @see DmtSession#createLeafNode(String, DmtData)
	 * @see DmtSession#createLeafNode(String, DmtData, String)
	 */
	void createLeafNode(String[] nodePath, DmtData value, String mimeType) throws DmtException;

	/**
	 * Delete the given node. Deleting interior nodes is recursive, the whole
	 * subtree under the given node is deleted.
	 * 
	 * @param nodePath the absolute path of the node to delete
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a
	 *         non-existing node</li>
	 *         <li>{@code METADATA_MISMATCH} if the node could not be deleted
	 *         because of meta-data restrictions</li>
	 *         <li>{@code DATA_STORE_FAILURE} if an error occurred while
	 *         accessing the data store</li>
	 *         <li>{@code COMMAND_FAILED} if some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#deleteNode(String)
	 */
	void deleteNode(String[] nodePath) throws DmtException;

	/**
	 * Rename a node. This operation only changes the name of the node (updating
	 * the timestamp and version properties if they are supported), the value
	 * and the other properties are not changed. The new name of the node must
	 * be provided, the new path is constructed from the base of the old path
	 * and the given name.
	 * 
	 * @param nodePath the absolute path of the node to rename
	 * @param newName the new name property of the node
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a
	 *         non-existing node, or if the new node is not defined in the tree</li>
	 *         <li>{@code NODE_ALREADY_EXISTS} if there already exists a sibling
	 *         of {@code nodePath} with the name {@code newName}</li>
	 *         <li>{@code METADATA_MISMATCH} if the node could not be renamed
	 *         because of meta-data restrictions</li>
	 *         <li>{@code DATA_STORE_FAILURE} if an error occurred while
	 *         accessing the data store</li>
	 *         <li>{@code COMMAND_FAILED} if some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#renameNode(String, String)
	 */
	void renameNode(String[] nodePath, String newName) throws DmtException;

	/**
	 * Set the title property of a node. The length of the title is guaranteed
	 * not to exceed the limit of 255 bytes in UTF-8 encoding.
	 * 
	 * @param nodePath the absolute path of the node
	 * @param title the title text of the node, can be {@code null}
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a
	 *         non-existing node</li>
	 *         <li>{@code METADATA_MISMATCH} if the title could not be set
	 *         because of meta-data restrictions</li>
	 *         <li>{@code FEATURE_NOT_SUPPORTED} if the Title property is not
	 *         supported by the plugin</li>
	 *         <li>{@code DATA_STORE_FAILURE} if an error occurred while
	 *         accessing the data store</li>
	 *         <li>{@code COMMAND_FAILED} if some unspecified error is
	 *         encountered while attempting to complete the command</li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#setNodeTitle(String, String)
	 */
	void setNodeTitle(String[] nodePath, String title) throws DmtException;

	/**
	 * Set the type of a node. The type of leaf node is the MIME type of the
	 * data it contains. The type of an interior node is a URI identifying a DDF
	 * document.
	 * <p>
	 * For interior nodes, the {@code null} type should remove the reference (if
	 * any) to a DDF document overriding the tree structure defined by the
	 * ancestors. For leaf nodes, it requests that the default MIME type is used
	 * for the given node.
	 * 
	 * @param nodePath the absolute path of the node
	 * @param type the type of the node, can be {@code null}
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a
	 *         non-existing node</li><li>{@code METADATA_MISMATCH} if the type
	 *         could not be set because of meta-data restrictions</li><li>
	 *         {@code DATA_STORE_FAILURE} if an error occurred while accessing
	 *         the data store</li><li>{@code COMMAND_FAILED} if some unspecified
	 *         error is encountered while attempting to complete the command
	 *         </li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#setNodeType(String, String)
	 */
	void setNodeType(String[] nodePath, String type) throws DmtException;

	/**
	 * Set the value of a leaf or interior node. The format of the node is
	 * contained in the {@code DmtData} object. For interior nodes, the format
	 * is {@code FORMAT_NODE}, while for leaf nodes this format is never used.
	 * <p>
	 * If the specified value is {@code null}, the default value must be taken;
	 * if there is no default value, a {@code DmtException} with error code
	 * {@code METADATA_MISMATCH} must be thrown.
	 * 
	 * @param nodePath the absolute path of the node
	 * @param data the data to be set, can be {@code null}
	 * @throws DmtException with the following possible error codes:
	 *         <ul>
	 *         <li>{@code NODE_NOT_FOUND} if {@code nodePath} points to a
	 *         non-existing node</li><li>{@code METADATA_MISMATCH} if the value
	 *         could not be set because of meta-data restrictions</li><li>
	 *         {@code FEATURE_NOT_SUPPORTED} if the specified node is an
	 *         interior node and does not support Java object values</li><li>
	 *         {@code DATA_STORE_FAILURE} if an error occurred while accessing
	 *         the data store</li><li>{@code COMMAND_FAILED} if some unspecified
	 *         error is encountered while attempting to complete the command
	 *         </li>
	 *         </ul>
	 * @throws SecurityException if the caller does not have the necessary
	 *         permissions to execute the underlying management operation
	 * @see DmtSession#setNodeValue(String, DmtData)
	 */
	void setNodeValue(String[] nodePath, DmtData data) throws DmtException;
}
