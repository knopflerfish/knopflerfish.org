/*
 * Copyright (c) 2009, KNOPFLERFISH project
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

package org.knopflerfish.framework.permissions;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;


/**
 * Framework service to administer Conditional Permissions. Conditional
 * Permissions can be added to, retrieved from, and removed from the framework.
 * 
 */
public class ConditionalPermissionUpdateImpl implements ConditionalPermissionUpdate {

  private final ConditionalPermissionInfoStorage storage;

  private final ArrayList cpiTable;

  private final int parent;

 
  /**
   *
   */
  ConditionalPermissionUpdateImpl(ConditionalPermissionInfoStorage cpis,
                                  ArrayList org, int generation) {
    storage = cpis;
    cpiTable = (ArrayList)org.clone();
    parent = generation;
  }


  /**
   *
   */
  public List /* ConditionalPermissionInfo */ getConditionalPermissionInfos() {
    return cpiTable;
  }


  /**
   *
   */
  public boolean commit() {
    return storage.commitUpdate(cpiTable, parent);
  }
}
