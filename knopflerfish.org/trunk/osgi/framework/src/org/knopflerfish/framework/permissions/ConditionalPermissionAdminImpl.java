/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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

import java.security.*;
import java.util.Enumeration;

import org.osgi.service.permissionadmin.PermissionInfo;

import org.osgi.service.condpermadmin.*;

/**
 * Framework service to administer Conditional Permissions. Conditional
 * Permissions can be added to, retrieved from, and removed from the framework.
 * 
 */
public class ConditionalPermissionAdminImpl implements ConditionalPermissionAdmin {

  public final static String SPEC_VERSION = "1.0";

  private ConditionalPermissionInfoStorage cpis;


  /**
   *
   */
  public ConditionalPermissionAdminImpl(ConditionalPermissionInfoStorage cpis) {
    this.cpis = cpis;
  }


  //
  // Interface ConditionalPermissionAdmin
  //

  /**
   * Create a new Conditional Permission Info.
   * 
   * The Conditional Permission Info will be given a unique, never reused
   * name.
   * 
   * @param conds The Conditions that need to be satisfied to enable the
   *        corresponding Permissions.
   * @param perms The Permissions that are enable when the corresponding
   *        Conditions are satisfied.
   * @return The ConditionalPermissionInfo for the specified Conditions and
   *         Permissions.
   * @throws SecurityException If the caller does not have
   *         <code>AllPermission</code>.
   */
  public ConditionalPermissionInfo
  addConditionalPermissionInfo(ConditionInfo conds[], PermissionInfo perms[]) {
    return cpis.put(null, conds, perms);
  }

  
  /**
   * Set or create a Conditional Permission Info with a specified name.
   * 
   * If the specified name is <code>null</code>, a new Conditional
   * Permission Info must be created and will be given a unique, never reused
   * name. If there is currently no Conditional Permission Info with the
   * specified name, a new Conditional Permission Info must be created with
   * the specified name. Otherwise, the Conditional Permission Info with the
   * specified name must be updated with the specified Conditions and
   * Permissions.
   * 
   * @param name The name of the Conditional Permission Info, or
   *        <code>null</code>.
   * @param conds The Conditions that need to be satisfied to enable the
   *        corresponding Permissions.
   * @param perms The Permissions that are enable when the corresponding
   *        Conditions are satisfied.
   * @return The ConditionalPermissionInfo that for the specified name,
   *         Conditions and Permissions.
   * @throws SecurityException If the caller does not have
   *         <code>AllPermission</code>.
   */
  public ConditionalPermissionInfo
  setConditionalPermissionInfo(String name, ConditionInfo conds[], PermissionInfo perms[]) {
    return cpis.put(name, conds, perms);
  }


  /**
   * Returns the Conditional Permission Infos that are currently managed by
   * Conditional Permission Admin. Calling
   * {@link ConditionalPermissionInfo#delete()} will remove the Conditional
   * Permission Info from Conditional Permission Admin.
   * 
   * @return An enumeration of the Conditional Permission Infos that are
   *         currently managed by Conditional Permission Admin.
   */
  public Enumeration getConditionalPermissionInfos() {
    return cpis.getAll();
  }


  /**
   * Return the Conditional Permission Info with the specified name.
   * 
   * @param name The name of the Conditional Permission Info to be returned.
   * @return The Conditional Permission Info with the specified name.
   */
  public ConditionalPermissionInfo getConditionalPermissionInfo(String name) {
    return cpis.get(name);
  }


  /**
   * Returns the Access Control Context that corresponds to the specified
   * signers.
   * 
   * @param signers The signers for which to return an Access Control Context.
   * @return An <code>AccessControlContext</code> that has the Permissions
   *         associated with the signer.
   */
  public AccessControlContext getAccessControlContext(String[] signers) {
    Permissions perms = new Permissions();
    if (signers != null && signers.length > 0) {
      for (Enumeration e = cpis.getAll(); e.hasMoreElements(); ) {
	ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl) e.nextElement();
	if (cpi.hasSigners(signers)) {
	  for (Enumeration e2 = cpi.getPermissions().elements(); e2.hasMoreElements(); ) {
	    perms.add((Permission)e2.nextElement());
	  }
	}
      }
    }
    return new AccessControlContext(new ProtectionDomain[] {new ProtectionDomain(null, perms)});
  }

}
