/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.AdminPermission;
import org.osgi.service.permissionadmin.*;

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
  private FrameworkContext framework;

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
    pinfos = pis;
    cpinfos = cpis;
    location = loc;
    bundle = b;
    dataRoot = fw.getDataStorage(b.getBundleId());
    if (localPerms != null) {
      localPermissions = makeLocalPermissionCollection(localPerms);
    } else {
      localPermissions = null;
    }
    implicitPermissions = makeImplicitPermissionCollection(fw, b);
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
  public boolean implies(Permission permission) {
    String me = "PermissionWrapper.implies: ";
    if (implicitPermissions.implies(permission)) {
      if (Debug.permissions) {
        Debug.println(me + "Implicitly OK for, " + permission);
      }
      return true;
    } else if (localPermissions != null && !localPermissions.implies(permission)) {
      if (Debug.permissions) {
        Debug.println(me + "No localpermissions for, " + permission);
      }
      return false;
    } else {
      PermissionCollection p = getPerms();
      boolean res;
      if (p != null) {
        res = p.implies(permission);
        if (Debug.permissions) {
          Debug.println(me + (res ? "OK" : "No") +  " framework permission for," + permission);
        }
      } else {
        res = conditionalPermissionImplies(permission);
        if (Debug.permissions) {
          Debug.println(me + (res ? "OK" : "No") +  " conditional permission for," + permission);
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
        // NYI! What to do
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
  synchronized void updateLocalPermissions(InputStream localPerms) {
    if (localPerms != null) {
      localPermissions = makeLocalPermissionCollection(localPerms);
    } else {
      localPermissions = null;
    }
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
  private PermissionCollection makeLocalPermissionCollection(InputStream localPerms) {
    try {
      DataInputStream dis = new DataInputStream(localPerms);
      String l;
      Permissions res = new Permissions();
      while ((l = dis.readLine()) != null) {
        l = l.trim();
        if (l.startsWith("#") || l.startsWith("//") || l.length() == 0) {
          continue;
        }
        try {
          Permission p = PermUtil.makePermission(new PermissionInfo(l), null);
          if (p != null) {
            res.add(p);
          }
        } catch (Exception e) {
          // TODO, handle this error
        }
      }
      return res;
    } catch (IOException e) {
      // TODO, handle this error
      return null;
    } finally {
      try {
        localPerms.close();
      } catch (IOException _ignore) { }
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
    PermissionInfo[] pi = pinfos.get(location, this);
    final boolean useDefault = (pi == null);
    Permissions res = new Permissions();
    if (useDefault) {
      if (Debug.tck401compat) {
        if (condPermList.size() > 0) {
          // If we are using CPA with rules added do not use default.
          // If we have CPA without rules, use default. This isn't correct
          // way according to the standard. But it helps bootstrapping the
          // system and is required for passing OSGi test suite.
          return null;
        }
      } else {
        if (cpinfos != null && cpinfos.size() > 0) {
          return null;
        }
      }
      pi = pinfos.getDefault(this);
    }
    for (int i = pi.length - 1; i >= 0; i--) {
      Permission p = PermUtil.makePermission(pi[i], useDefault ? null : dataRoot);
      if (p != null) {
        res.add(p);
      }
    }
    return res;
  }


  /**
   *
   */
  private boolean conditionalPermissionImplies(Permission permission) {
    List postponement = null;
    SecurityManager sm = System.getSecurityManager();
    ConditionalPermissionSecurityManager cpsm =
      (sm instanceof ConditionalPermissionSecurityManager) ?
      (ConditionalPermissionSecurityManager)sm :
      null;

    for (Iterator i = condPermList.iterator(); i.hasNext(); ) {
      ConditionalPermission cp = (ConditionalPermission)i.next();
      if (Debug.permissions) {
        Debug.println("conditionalPermissionImplies: Check if " + cp + " implies " + permission + " for " + bundle);
      }
      if (cp.checkImmediateOk(permission, cpsm == null)) {
        if (cp.hasPostponed()) {
          if (Debug.permissions) {
            Debug.println("conditionalPermissionImplies: " + cp + " with postponement implies " + permission + " for " + bundle);
          }
          if (postponement == null) {
            postponement = new ArrayList();
          }
          postponement.add(cp);
        } else {
          if (Debug.permissions) {
            Debug.println("conditionalPermissionImplies: " + cp + " implies " + permission + " for " + bundle);
          }
          return true;
        }
      } else {
        if (Debug.permissions) {
          Debug.println("conditionalPermissionImplies: " + cp + " does NOT imply " + permission + " for " + bundle);
        }
      }  
    }
    if (postponement != null) {
      cpsm.savePostponement(postponement);
      return true;
    }
    return false;
  }


  /**
   *
   */
  synchronized void updateChangedConditionalPermission(ConditionalPermissionInfoImpl cpi,
                                                       ConditionalPermissionInfoImpl old) {
    ConditionalPermission new_cp = cpi != null ? cpi.getConditionalPermission(bundle) : null;
    if (old != null) {
      for (int i = condPermList.size() - 1; i >= 0; i--) {
        ConditionalPermission cp = (ConditionalPermission)condPermList.get(i);
        if (cp.isParent(old)) {
          if (new_cp != null) {
            condPermList.set(i, new_cp);
          } else {
            condPermList.remove(i);
          }
          return;
        }
      }
    }
    if (new_cp != null) {
      condPermList.add(new_cp);
    }
    invalidate();
  }


  /**
   *
   */
  private void initCondPermList() {
    condPermList = new ArrayList();
    for (Enumeration e = cpinfos.getAll(); e.hasMoreElements(); ) {
      ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl) e.nextElement();
      if (Debug.permissions) {
        Debug.println("conditionalPermissionImplies: " + cpi + " Bundle#" + bundle.getBundleId());
      }
      updateChangedConditionalPermission(cpi, null);
    }
  }

}
