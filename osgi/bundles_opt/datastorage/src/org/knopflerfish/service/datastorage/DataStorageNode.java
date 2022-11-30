/*
 * Copyright (c) 2018-2022, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.service.datastorage;

import java.util.Collection;

/**
 * Base class for datastorages
 * @author Makewave AB
 *
 */
public interface DataStorageNode {
  
  /**
   * Returns the name of the node
   * @return the name
   */
  String getName();
  
  /**
   * Returns the absolute path to this node.
   * 
   * @return the absolute path
   */
  String getAbsolutePath();
  
  /**
   * Returns the parent node. For the root node null is returned
   * 
   * @return the parent node.
   * 
   */
  DataStorageNode getParent();
  
  /**
   * Returns the children to the node
   * @return a collection of children.
   */
  Collection<DataStorageNode> children();
  
  /**
   * Returns the names of the children
   * @return a collection of child node names
   */
  Collection<String> childNames();
  
  /**
   * Return the node type
   * @return the node type
   */
  DataStorageNodeType getType();
  
  /**
   * Enumeration indicating the type of the node
   *
   */
  enum DataStorageNodeType {
    JSON,
    JSON_GENERIC
  }
    
}
