/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;


/**
 * Implementation of a Permission Policy for Framework.
 *
 * @see java.security.Policy
 * @author Jan Stein
 * @author Gunnar Ekolin
 */

class FrameworkPolicy extends Policy {

  /** The policy to delegate non-bundle permission requests to. */
  private final Policy defaultPolicy;

  private Hashtable /* Long -> PermissionCollection */
    permissions = new Hashtable();

  private PermissionAdminImpl permissionAdmin;

  FrameworkPolicy(Policy defaultPolicy, PermissionAdminImpl pa ) {
    this.defaultPolicy = defaultPolicy;
    permissionAdmin = pa;
  }

  //
  // Policy methods
  //

  // Delegate to the wrapped defaultPolicy for all non-bundle domains.
  public PermissionCollection getPermissions(ProtectionDomain pd) {
    if (null==pd)
      return defaultPolicy.getPermissions(pd);

    CodeSource cs = pd.getCodeSource();
    if (null==cs)
      return defaultPolicy.getPermissions(pd);

    URL u = cs.getLocation();
    if (u != null && BundleURLStreamHandler.PROTOCOL.equals(u.getProtocol())) {
      return getPermissions(cs);
    } else {
      return defaultPolicy.getPermissions(pd);
    }
  }


  // Delegate to the wrapped defaultPolicy for all non-bundle domains.
  public PermissionCollection getPermissions(CodeSource cs) {
    // The following line causes a loop when running on 1.4
    // System.getSecurityManager().checkPermission(new SecurityPermission("getPermissions"));
    // Also note that there's no "getPermissions" target for SercurityPermission

    if (null==cs) return new Permissions();

    URL u = cs.getLocation();
    if (u != null && BundleURLStreamHandler.PROTOCOL.equals(u.getProtocol())) {
      try {
        Long id = new Long(u.getHost());
        return getPermissions(id);
      } catch (NumberFormatException ignore) {
        return null;
      }
    } else {
      return defaultPolicy.getPermissions(cs);
    }
  }

  public boolean implies(ProtectionDomain pd, Permission p) {
    if (null==pd)
      return defaultPolicy.implies(pd,p);

    CodeSource cs = pd.getCodeSource();
    if (null==cs)
      return defaultPolicy.implies(pd,p);

    URL u = cs.getLocation();
    if (u != null && BundleURLStreamHandler.PROTOCOL.equals(u.getProtocol())) {
      return super.implies(pd,p);
    } else {
      return defaultPolicy.implies(pd,p);
    }
  }

  public void refresh() {
    // A bundle permissions is allways up to date, but we must
    // propagate to the wrapped defaultPolicy.
    defaultPolicy.refresh();
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
