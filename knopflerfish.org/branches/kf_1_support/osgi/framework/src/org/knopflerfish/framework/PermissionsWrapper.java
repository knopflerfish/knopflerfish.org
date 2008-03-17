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

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.permissionadmin.*;


/**
 * Wrapps Permissions so that we can update it dynamicly.
 *
 * @author Jan Stein
 */


class PermissionsWrapper extends PermissionCollection {

  private BundleImpl bundle;
  private PermissionAdminImpl pa;
  private volatile PermissionCollection permissions;
  private boolean readOnly = false;

  PermissionsWrapper(PermissionAdminImpl p, BundleImpl b) {
    pa = p;
    bundle = b;
    permissions = makePermissionCollection(b);;
  }


  public void add(Permission permission) {
    getPerms().add(permission);
  }


  public Enumeration elements() {
    return getPerms().elements();
  }


  public boolean implies(Permission permission) {
    return getPerms().implies(permission);
  }


  public boolean isReadOnly() {
    return readOnly;
  }


  public void setReadOnly() {
    if (!readOnly) {
      readOnly = true;
      getPerms().setReadOnly();
    }
  }

  synchronized void invalidate() {
    permissions = null;
  }

  private PermissionCollection getPerms0() {
    if (permissions == null) {
      PermissionCollection p = makePermissionCollection(bundle);
      if (readOnly) {
        p.setReadOnly();
      }
      permissions = p;
    }
    return permissions;
  }

  private PermissionCollection getPerms() {
    if (Framework.isDoubleCheckedLockingSafe) {
       if (permissions == null) {
        synchronized (this) {
          return getPerms0();
        }
      }
      return permissions;
    } else {
      synchronized(this) {
        return getPerms0();
      }
    }
  }


  /**
   * Create the permissionCollection assigned to the bundle with the specified id.
   * The collection contains the configured permissions for the bundle location
   * plus implict granted permissions (FilePermission for the data area and
   * java runtime permissions).
   *
   * @param bundle The bundle whose permissions are to be created.
   *
   * @return The permissions assigned to the bundle with the specified
   * location, or the default permissions if that bundle has not been assigned
   * any permissions.
   */
  private PermissionCollection makePermissionCollection(BundleImpl bundle) {
    PermissionCollection pc;
    PermissionInfo[] pi = pa.getPermissions(bundle.location);
    if (pi != null) {
      pc = makePermissionCollection(pi);
    } else {
      pi = pa.getDefaultPermissions();
      if (pi != null) {
        pc = makePermissionCollection(pi);
      } else {
        pc = new Permissions();
      }
    }
    File root = bundle.getDataRoot();
    if (root != null) {
      pc.add(new FilePermission(root.getPath(), "read,write"));
      pc.add(new FilePermission((new File(root, "-")).getPath(),
                                "read,write,execute,delete"));
    }
    if (pa.runtimePermissions != null) {
      for (Enumeration e = pa.runtimePermissions.elements(); e.hasMoreElements();) {
        pc.add((Permission) e.nextElement());
      }
    }
    return pc;
  }

  /**
   * Build a permissionCollection based on a set PermissionInfo objects. All the
   * permissions that are available in the CLASSPATH are constructed and all
   * the bundle based permissions are constructed as UnresolvedPermissions.
   *
   * @param pi Array of PermissionInfo to enter into the PermissionCollection.
   *
   * @return The permissions assigned to the bundle with the specified
   * location, or the default permissions if that bundle has not been assigned
   * any permissions.
   */
  private PermissionCollection makePermissionCollection(PermissionInfo[] pi) {
    Permissions p = new Permissions();
    for (int i = pi.length - 1; i >= 0; i--) {
      String a = pi[i].getActions();
      String n = pi[i].getName();
      String t = pi[i].getType();
      try {
        Class pc = Class.forName(t);
        Constructor c = pc.getConstructor(new Class [] { String.class, String.class });
        p.add((Permission)c.newInstance(new Object[] { n, a }));
      } catch (ClassNotFoundException e) {
        p.add(new UnresolvedPermission(t, n, a, null));
      } catch (NoSuchMethodException ignore) {
      } catch (InstantiationException ignore) {
      } catch (IllegalAccessException ignore) {
      } catch (InvocationTargetException ignore) {
      }
    }
    return p;
  }
}
