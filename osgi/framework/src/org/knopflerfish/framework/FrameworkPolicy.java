/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.net.*;
import java.security.*;
import java.util.*;

/**
 * Implementation of a Permission Policy for Framework.
 *
 * @see java.security.Policy
 * @author Jan Stein
 */

class FrameworkPolicy extends Policy {

  private final PermissionCollection all = new Permissions();

  private Hashtable /* Long -> PermissionCollection */ permissions = new Hashtable();

  private PermissionAdminImpl permissionAdmin;

  FrameworkPolicy(PermissionAdminImpl pa) {
    all.add(new AllPermission());
    all.setReadOnly();
    permissionAdmin = pa;
  }

  //
  // Policy methods
  //

  public PermissionCollection getPermissions(CodeSource cs) {
    // The following line causes a loop when running on 1.4
    // System.getSecurityManager().checkPermission(new SecurityPermission("getPermissions"));
    // Also note that there's no "getPermissions" target for SercurityPermission
    
    URL u = cs.getLocation();
    if (u != null && BundleURLStreamHandler.PROTOCOL.equals(u.getProtocol())) {
      try {
        Long id = new Long(u.getHost());
        return getPermissions(id);
      } catch (NumberFormatException ignore) {
        return null;
      }
    } else {
      return all;
    }
  }

  public void refresh() {
    // Nothing todo since we are always updated
  }

  //
  // Package methods
  //

  PermissionCollection getPermissions(Long id) {
    PermissionCollection pc = (PermissionCollection)permissions.get(id);
    if (pc == null) {
      pc = permissionAdmin.getPermissionCollection(id);
      if (pc != null) {
	permissions.put(id, pc);
      }
    }
    return pc;
  }

  void invalidate(long id) {
    permissions.remove(new Long(id));
  }

}
