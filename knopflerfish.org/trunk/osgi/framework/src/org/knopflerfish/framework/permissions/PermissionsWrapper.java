/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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
import java.security.*;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.AdminPermission;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.*;

import org.knopflerfish.framework.Debug;
import org.knopflerfish.framework.FrameworkContext;


/**
 * Wrapps Permissions so that we can update it dynamically.
 *
 * @author Jan Stein, Philippe Laporte
 */


public class PermissionsWrapper extends PermissionCollection {
  private static final long serialVersionUID = 1L;

  String location;

  private Bundle bundle;
  private PermissionInfoStorage pinfos;
  private ConditionalPermissionInfoStorage cpinfos;
  private PermissionCollection implicitPermissions;
  private PermissionCollection localPermissions;
  private volatile PermissionCollection systemPermissions;
  private File dataRoot;
  private boolean readOnly = false;
  private ArrayList condPermList = null;
  private ConditionalPermissionSecurityManager cpsm;

  final private FrameworkContext framework;
  final private Debug debug;

  /**
   *
   */
  PermissionsWrapper(FrameworkContext fw,
                     PermissionInfoStorage pis,
                     ConditionalPermissionInfoStorage cpis,
                     String loc,
                     Bundle b,
                     InputStream localPerms) {
    this.framework = fw;
    debug = fw.debug;
    pinfos = pis;
    cpinfos = cpis;
    location = loc;
    bundle = b;
    // If location is null, then we have a dummy bundle.
    // Used for ConditionalPermissionAdmin.getAccessControlerContext()
    if (loc != null) {
      SecurityManager sm = System.getSecurityManager();
      if (sm instanceof ConditionalPermissionSecurityManager) {
        cpsm = (ConditionalPermissionSecurityManager)sm;
      }
      dataRoot = fw.getDataStorage(b.getBundleId());
      if (localPerms != null) {
        localPermissions = new PermissionInfoPermissions(fw, dataRoot, localPerms);
      }
      implicitPermissions = makeImplicitPermissionCollection(fw, b);
    }
    initCondPermList();
    systemPermissions = makePermissionCollection();
  }


  /**
   *
   */
  public void add(Permission permission) {
    PermissionCollection p = getPerms();
    if (p != null) {
      p.add(permission);
    } else {
      throw new RuntimeException("NYI! Using Conditional Permissions");
    }
  }


  /**
   *
   */
  public Enumeration elements() {
    final PermissionCollection p = getPerms();
    if (p == null) {
      throw new RuntimeException("NYI! Using Conditional Permissions 2");
    }

    return new Enumeration() {
        private Enumeration implicitElements = implicitPermissions.elements();
        private Enumeration systemElements = p.elements();

        public boolean hasMoreElements() {
          if (implicitElements != null) {
            if (implicitElements.hasMoreElements()) {
              return true;
            }
            implicitElements = null;
          }
          return systemElements.hasMoreElements();
        }


        public Object nextElement() {
          if (implicitElements != null) {
            try {
              return implicitElements.nextElement();
            } catch (NoSuchElementException _ignore) { }
            implicitElements = null;
          }
          return systemElements.nextElement();
        }
      };
  }


  /**
   *
   */
  public boolean implies(final Permission permission) {
    String me = "PermissionWrapper.implies: ";
    if (implicitPermissions != null && implicitPermissions.implies(permission)) {
      if (debug.permissions) {
        debug.println(me + "Implicitly OK for, " + permission);
      }
      return true;
    } else if (localPermissions != null && !localPermissions.implies(permission)) {
      if (debug.permissions) {
        debug.println(me + "No localpermissions for, " + permission);
      }
      return false;
    } else {
      PermissionCollection p = getPerms();
      boolean res;
      if (p != null) {
        res = p.implies(permission);
        if (debug.permissions) {
          debug.println(me + (res ? "OK" : "No") +  " framework permission for," + permission);
        }
      } else {
        res = conditionalPermissionImplies(permission);
        if (debug.permissions) {
          debug.println(me + (res ? "OK" : "No") +  " conditional permission for," + permission);
        }
      }
      return res;
    }
  }


  /**
   *
   */
  public boolean isReadOnly() {
    return readOnly;
  }


  /**
   *
   */
  public void setReadOnly() {
    if (!readOnly) {
      readOnly = true;
      PermissionCollection p = getPerms();
      if (p != null) {
        p.setReadOnly();
      } else {
        // TBD, Conditional permission already readonly?
      }
    }
  }


  /**
   *
   */
  synchronized void invalidate() {
    systemPermissions = null;
  }


  /**
   *
   */
  private PermissionCollection getPerms0() {
    if (systemPermissions == null) {
      PermissionCollection p = makePermissionCollection();
      if (readOnly && p != null) {
        p.setReadOnly();
      }
      systemPermissions = p;
    }
    return systemPermissions;
  }


  /**
   *
   */
  private PermissionCollection getPerms() {
    if (framework.props.isDoubleCheckedLockingSafe) {
       if (systemPermissions == null) {
        synchronized (this) {
          return getPerms0();
        }
      }
      return systemPermissions;
    } else {
      synchronized(this) {
        return getPerms0();
      }
    }
  }


  /**
   * 
   */
  private PermissionCollection makeImplicitPermissionCollection(FrameworkContext fw, Bundle b) {
    // NYI, perhaps we should optimize this collection.
    Permissions pc = new Permissions();
    if (dataRoot != null) {
      pc.add(new FilePermission(dataRoot.getPath(), "read,write"));
      pc.add(new FilePermission((new File(dataRoot, "-")).getPath(),
                                "read,write,delete"));
    }
    StringBuffer sb = new StringBuffer("(id=");
    sb.append(b.getBundleId());
    sb.append(")");
    pc.add(new AdminPermission(sb.toString(),
                               AdminPermission.RESOURCE + "," +
                               AdminPermission.METADATA + "," +
                               AdminPermission.CLASS));
    pc.add(new PropertyPermission("org.osgi.framework.*", "read"));
    return pc;
  }


  /**
   * Create the permissionCollection assigned to the bundle.
   * The collection contains the configured permissions for the bundle location.
   * Build the permissionCollection based on a set PermissionInfo objects. All the
   * permissions that are available in the CLASSPATH are constructed and all the
   * bundle based permissions are constructed as UnresolvedPermissions.
   *
   * @return The permissions assigned to the bundle with the specified
   * location, or the default permissions if that bundle has not been assigned
   * any permissions.
   */
  private PermissionCollection makePermissionCollection() {
    PermissionInfo[] pi = pinfos.get(location, this) ;
    final boolean useDefault = (pi == null);
    if (useDefault) {
      if (cpinfos != null && cpinfos.size() > 0) {
        return null;
      }
      pi = pinfos.getDefault(this);
    }
    return new PermissionInfoPermissions(framework, useDefault ? null : dataRoot, pi);
  }


  /**
   *
   */
  private boolean conditionalPermissionImplies(Permission permission) {
    List postponement;
    if (cpsm != null && cpsm.isPostponeAvailable()) {
      postponement = new ArrayList();
    } else {
      postponement = null;
    }
    String immediateAccess = null;
    // TBD, should condPermList by guard from changes!?
    for (Iterator i = condPermList.iterator(); i.hasNext(); ) {
      ConditionalPermission cp = (ConditionalPermission)i.next();
      if (cp == null) {
        // Permission is already checked and is immutable and failed.
        continue;
      }
      if (debug.permissions) {
        debug.println("conditionalPermissionImplies: Check if " + cp + " implies " + permission + " for " + bundle);
      }
      if (cp.checkImmediateOk(permission, postponement == null)) {
        if (postponement != null) {
          postponement.add(cp);
        }
        if (cp.hasPostponed()) {
          if (debug.permissions) {
            debug.println("conditionalPermissionImplies: " + cp + " with postponement implies " + permission + " for " + bundle);
          }
        } else {
          if (debug.permissions) {
            debug.println("conditionalPermissionImplies: " + cp + " implies " + permission + " for " + bundle + ", end search");
          }
          immediateAccess = cp.access;
          break;
        }
      } else {
        if (debug.permissions) {
          debug.println("conditionalPermissionImplies: " + cp + " does NOT imply " + permission + " for " + bundle);
        }
      }  
    }
    if (postponement != null) {
      // Optimize superfluous
      int offset;
      if (immediateAccess == null) {
        immediateAccess = ConditionalPermissionInfo.DENY;
        offset = 1;
      } else {
        offset = 2;
      }
      for (int pos = postponement.size() - offset; pos >= 0; pos--) {
        if (((ConditionalPermission)postponement.get(pos)).access == immediateAccess) {
          Object pruned = postponement.remove(pos);
          if (debug.permissions) {
            debug.println("conditionalPermissionImplies: pruned, " + pruned + " for " + bundle);
          }
        } else {
          break;
        }
      }
      // If we only have deny, do deny
      if (immediateAccess == ConditionalPermissionInfo.DENY && postponement.size() < offset) {
        return false;
      }
      if (debug.permissions) {
        debug.println("conditionalPermissionImplies: postpone check of " + permission + " for " + bundle);
      }
      cpsm.savePostponement(postponement, debug);
      return true;
    } else {
      return immediateAccess == ConditionalPermissionInfo.ALLOW;
    }
  }


  /**
   *
   */
  synchronized void updateChangedConditionalPermission(ConditionalPermissionInfoImpl cpi,
                                                       int cpi_pos,
                                                       int remove_pos,
                                                       int expected_size) {
    ConditionalPermission new_cp = cpi != null ? cpi.getConditionalPermission(bundle) : null;
    Object old_cp;
    if (cpi_pos == remove_pos) {
      old_cp = condPermList.set(cpi_pos, new_cp);
    } else if (remove_pos == -1) {
      condPermList.add(cpi_pos, new_cp);
      old_cp = null;
    } else if (cpi_pos == -1) {
      old_cp = condPermList.remove(remove_pos);
    } else {
      // Case with different remove & insert position not used, yet
      throw new RuntimeException("NYI");
    }
    if (expected_size != condPermList.size()) {
      debug.printStackTrace("ASSERT, table size differ, " + expected_size + " != " + condPermList.size(), new Throwable());
      throw new RuntimeException("ASSERT ERROR");
    }
    // TBD! How to optimize?   if (new_cp != null || old_cp != null) {
      invalidate();
    //    }
  }


  /**
   *
   */
  private void initCondPermList() {
    // cpinfos is locked when we are here.
    ArrayList cpis = cpinfos.getAll();
    condPermList = new ArrayList(cpis.size());
    // TBD, perhaps we should go back to lazy instanciation.
    for (Iterator i = cpis.iterator(); i.hasNext(); ) {
      ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl) i.next();
      if (debug.permissions) {
        debug.println("conditionalPermissionImplies: " + cpi + " Bundle#" + bundle.getBundleId());
      }
      condPermList.add(cpi.getConditionalPermission(bundle));
    }
    invalidate();
  }

}
