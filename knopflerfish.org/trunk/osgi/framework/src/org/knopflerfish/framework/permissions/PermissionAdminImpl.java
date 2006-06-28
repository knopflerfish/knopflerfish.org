/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

import java.security.AccessController;
import java.security.AllPermission;

import org.osgi.service.permissionadmin.*;



/**
 * Implementation of the PermissionAdmin service.
 *
 * @see org.osgi.service.permissionadmin.PermissionAdmin
 * @author Jan Stein
 * @author Philippe Laporte
 */

public class PermissionAdminImpl implements PermissionAdmin {

  public final static String SPEC_VERSION = "1.2";

  /**
   * AllPermission used for permission check.
   */
  final private AllPermission ALL_PERMISSION = new AllPermission();
  
  final private PermissionInfoStorage pinfos;
  

  public PermissionAdminImpl(PermissionInfoStorage pis) {
    pinfos = pis;
  }
  
  
  //
  // Interface PermissionAdmin
  //

  /**
   * Gets the permissions assigned to the bundle with the specified
   * location.
   *
   * @param location The location of the bundle whose permissions are to
   * be returned.
   *
   * @return The permissions assigned to the bundle with the specified
   * location, or <tt>null</tt> if that bundle has not been assigned any
   * permissions.
   */
  public PermissionInfo[] getPermissions(String location) {
    PermissionInfo[] res = pinfos.get(location, null);
    return res != null ? (PermissionInfo[])res.clone() : null;
  }


  /**
   * Assigns the specified permissions to the bundle with the specified
   * location.
   * 
   * @param location The location of the bundle that will be assigned the
   *        permissions.
   * @param permissions The permissions to be assigned, or <code>null</code> if
   *        the specified location is to be removed from the permission table.
   * @throws SecurityException If the caller does not have
   *            <code>AllPermission</code>.
   */
  public synchronized void setPermissions(String location, PermissionInfo[] perms) {
    AccessController.checkPermission(ALL_PERMISSION);
    if (perms != null) {
      pinfos.put(location, (PermissionInfo[])perms.clone());
    } else {
      pinfos.remove(location);
    }
  }


  /**
   * Returns the bundle locations that have permissions assigned to them, that
   * is, bundle locations for which an entry exists in the permission table.
   * 
   * @return The locations of bundles that have been assigned any permissions,
   *         or <tt>null</tt> if the permission table is empty.
   */
  public String[] getLocations() {
    return pinfos.getKeys();
  }

  
  /**
   * Gets the default permissions.
   *
   * <p>These are the permissions granted to any bundle that does not
   * have permissions assigned to its location.
   *
   * @return The default permissions, or <tt>null</tt> if default 
   * permissions have not been defined.
   */
  public synchronized PermissionInfo[] getDefaultPermissions() {
    PermissionInfo[] res = pinfos.getDefault(null);
    return res != null ? (PermissionInfo[])res.clone() : null;
  }


  /**
   * Sets the default permissions.
   * 
   * <p>
   * These are the permissions granted to any bundle that does not have
   * permissions assigned to its location.
   * 
   * @param permissions The default permissions, or <code>null</code> if the
   *        default permissions are to be removed from the permission table.
   * @throws SecurityException If the caller does not have
   *            <code>AllPermission</code>.
   */
  public synchronized void setDefaultPermissions(PermissionInfo[] perms) {
    AccessController.checkPermission(ALL_PERMISSION);
    if (perms != null) {
      perms = (PermissionInfo[])perms.clone();
    }
    pinfos.putDefault(perms);
  }

}
