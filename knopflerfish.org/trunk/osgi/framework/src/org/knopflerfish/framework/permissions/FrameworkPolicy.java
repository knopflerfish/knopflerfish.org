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

package org.knopflerfish.framework.permissions;

import java.lang.reflect.Method;
import java.net.*;
import java.security.*;

import org.knopflerfish.framework.BundleURLStreamHandler;



/**
 * Implementation of a Permission Policy for Framework.
 *
 * Special handling for all protection domains that corresponds to a
 * bundle, for all other delegate to given default policy (normally
 * the policy file based policy implementation).
 *
 * @see java.security.Policy
 * @author Jan Stein, Philippe Laporte, Gunnar Ekolin
 */

class FrameworkPolicy extends Policy {

  /** The policy to delegate non-bundle permission requests to. */
  private final Policy defaultPolicy;

  private PermissionsHandle ph;

  FrameworkPolicy(Policy policy, PermissionsHandle ph) {
    this.defaultPolicy = policy;
    this.ph = ph;
  }

  //
  // Policy methods
  //
  /** The Policy.implies(ProtectionDomain,Permission) method if running
   *  in JDK 1.4 or above. */
  private static Method impliesPDPermMethod;
  /** The Policy.getPermissions(ProtectionDomain) method if running
   *  in JDK 1.4 or above. */
  private static Method getPermissionsPDMethod;
  static {
    try {
      impliesPDPermMethod = Policy.class
        .getDeclaredMethod("implies",
                           new Class[] {ProtectionDomain.class,
                                        Permission.class});
      getPermissionsPDMethod = Policy.class
        .getDeclaredMethod("getPermissions",
                           new Class[] {ProtectionDomain.class});
    } catch (NoSuchMethodException ignore) {
      impliesPDPermMethod    = null;
      getPermissionsPDMethod = null;
    }
  }

  /**
   * If the method Policy#getPermissions(ProtectionDomain pd) is
   * available use it on the <tt>defaultPolicy</tt> object, otherwise
   * return an empty permission collection.
   *
   * @param policy the policy object to use.
   * @param pd     the protection domain to ask about.
   * @return The permissions for the given protection domain.
   */
  private boolean implies0(Policy policy,
                           ProtectionDomain pd,
                           Permission permission) {
    if (null!=impliesPDPermMethod) {
      try {
        return ((Boolean)
          impliesPDPermMethod.invoke(policy,
                                     new Object[] {pd, permission})
                ).booleanValue();
      } catch (Exception e) {
      }
    }
    return false;
  }

  /**
   * If the method Policy#getPermissions(ProtectionDomain pd) is
   * available use it on the <tt>defaultPolicy</tt> object, otherwise
   * return an empty permission collection.
   *
   * @param policy the policy object to use.
   * @param pd     the protection domain to ask about.
   * @return The permissions for the given protection domain.
   */
  private PermissionCollection getPermissions0(Policy policy,
                                               ProtectionDomain pd) {
    if (null!=getPermissionsPDMethod) {
      try {
        return (PermissionCollection)
          getPermissionsPDMethod.invoke(policy,
                                        new Object[] {pd});
      } catch (Exception e) {
      }
    }
    return new Permissions();
  }


  // Delegate to the wrapped defaultPolicy for all non-bundle domains.
  public PermissionCollection getPermissions(ProtectionDomain pd) {
    if (null==pd)
      return getPermissions0(defaultPolicy, pd);

    CodeSource cs = pd.getCodeSource();
    if (null==cs)
      return getPermissions0(defaultPolicy, pd);

    URL u = cs.getLocation();
    if (u != null && BundleURLStreamHandler.PROTOCOL.equals(u.getProtocol())) {
      return getPermissions(cs);
    } else {
      return getPermissions0(defaultPolicy, pd);
    }
  }


  public PermissionCollection getPermissions(CodeSource cs) {
    if (null==cs) {
      // Not a code source for a bundle, delegate to the default policy
      return defaultPolicy.getPermissions(cs);
    }

    URL u = cs.getLocation();
    if (u != null && BundleURLStreamHandler.PROTOCOL.equals(u.getProtocol())) {
      try {
        Long id = new Long(u.getHost());
        //return getPermissions(id);
        return ph.getPermissionCollection(id);
      } catch (NumberFormatException ignore) {
        return null;
      }
    } else {
      return defaultPolicy.getPermissions(cs);
    }
  }

  public boolean implies(ProtectionDomain pd, Permission p) {
    if (null==pd)
      return implies0(defaultPolicy,pd,p);

    CodeSource cs = pd.getCodeSource();
    if (null==cs)
      return implies0(defaultPolicy,pd,p);

    URL u = cs.getLocation();
    if (u != null && BundleURLStreamHandler.PROTOCOL.equals(u.getProtocol())) {
      PermissionCollection pc = getPermissions(cs);
      return (pc == null) ? false : pc.implies(p);
    } else {
      return implies0(defaultPolicy,pd,p);
    }
  }

  public void refresh() {
    // A bundle permissions is allways up to date, but we must
    // propagate to the wrapped defaultPolicy.
    defaultPolicy.refresh();
  }

}
