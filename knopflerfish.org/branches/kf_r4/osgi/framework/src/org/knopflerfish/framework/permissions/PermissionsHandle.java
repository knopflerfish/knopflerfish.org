/*
 * Copyright (c) 2006, KNOPFLERFISH project
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

import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.knopflerfish.framework.Framework;
import org.knopflerfish.framework.Util;


public class PermissionsHandle {

  PermissionCollection runtimePermissions;

  Framework framework;

  private PermissionInfoStorage pinfos;

  private Hashtable /* Long -> PermissionCollection */ pcCache = new Hashtable();

  private PermissionAdminImpl pa;


  public PermissionsHandle(Framework fw) {
     framework = fw;

     // Get system default permissions
     PermissionCollection pc = 
       Policy.getPolicy().getPermissions(new CodeSource((URL)null, (Certificate[])null));
     // Remove AllPermission
     if (pc != null && pc.implies(new AllPermission())) {
       runtimePermissions = new Permissions();
       for (Enumeration e = pc.elements(); e.hasMoreElements();) {
        Permission p = (Permission) e.nextElement();
        if (!(p instanceof AllPermission)) {
System.out.println("ADD RUNTIME: " + p);
          runtimePermissions.add(p);
        }
       }
     } else {
       runtimePermissions = pc;
     }
     
     pinfos = new PermissionInfoStorage();

     pa = new PermissionAdminImpl(pinfos);
     
     Policy.setPolicy(new FrameworkPolicy(this));
   }


   public PermissionAdminImpl getPermissionAdminService() {
     return pa;
   }

  
  /**
   * Gets the permissionCollection assigned to the bundle with the specified id.
   * The collection contains the configured permissions for the bundle location
   * plus implict gratnet permissions (i.e FilePermission for the data area).
   *
   * @param bid The bundle id whose permissions are to be returned.
   *
   * @return The permissions assigned to the bundle with the specified
   * location, or the default permissions if that bundle has not been assigned
   * any permissions or does not yet exist.
   */
  public PermissionCollection getPermissionCollection(Long bid) {
    return (PermissionCollection)pcCache.get(bid);
  }

  
  /**
   * Gets the permissionCollection assigned to the bundle.
   * We return a permission wrapper so that we can change it dynamically.
   *
   * @param bundle The bundle whose permissions are to be returned.
   *
   * @return The permissions assigned to the bundle with the specified
   * location
   */
  public  PermissionCollection createPermissionCollection(String loc,
                                                          Bundle b,
                                                          InputStream localPerms) {
    Long bid = new Long(b.getBundleId());
    if (pcCache.get(bid) != null) {
      throw new RuntimeException("NYI! update!?");
    }
    PermissionCollection pc = new PermissionsWrapper(framework, pinfos, runtimePermissions,
                                                     loc, b, localPerms);
    pcCache.put(bid, pc);
    return pc;
  }


  public void purgePermissionCollection(Long bid) {
    pcCache.remove(bid);
  }

  

}
