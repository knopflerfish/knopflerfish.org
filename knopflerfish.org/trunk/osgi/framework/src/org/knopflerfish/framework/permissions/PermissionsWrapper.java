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

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.AdminPermission;
import org.osgi.service.permissionadmin.*;

import org.knopflerfish.framework.Framework;


/**
 * Wrapps Permissions so that we can update it dynamically.
 *
 * @author Jan Stein, Philippe Laporte
 */


public class PermissionsWrapper extends PermissionCollection {
  private static final long serialVersionUID = 1L;
  
  String location;

  private PermissionInfoStorage pinfos;
  private PermissionCollection implicitPermissions;
  private PermissionCollection localPermissions;
  private PermissionCollection systemPermissions;
  private File dataRoot;
  private boolean readOnly = false;
  

  PermissionsWrapper(Framework fw,
                     PermissionInfoStorage pis,
                     String loc,
                     Bundle b,
                     InputStream localPerms) {
    pinfos = pis;
    location = loc;
    dataRoot = fw.getDataStorage(b.getBundleId());
    if (localPerms != null) {
      localPermissions = makeLocalPermissionCollection(localPerms);
    } else {
      localPermissions = null;
    }
    implicitPermissions = makeImplicitPermissionCollection(fw, b);
    systemPermissions = makePermissionCollection();
  }


  public void add(Permission permission) {
    getPerms().add(permission);
  }

    
  public Enumeration elements() {
    return new Enumeration() {
        private Enumeration implicitElements = implicitPermissions.elements();
        private Enumeration systemElements = getPerms().elements();

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


  public boolean implies(Permission permission) {
    if (implicitPermissions.implies(permission)) {
      return true;
    } else if (localPermissions != null && !localPermissions.implies(permission)) {
      return false;
    } else {
      return getPerms().implies(permission);
    }
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
    systemPermissions = null;
  }


  synchronized void updateLocalPermissions(InputStream localPerms) {
    if (localPerms != null) {
      localPermissions = makeLocalPermissionCollection(localPerms);
    } else {
      localPermissions = null;
    }
  }


  private PermissionCollection getPerms() {
    PermissionCollection p = systemPermissions;
    if (p == null) {
      synchronized (this) {
        p = systemPermissions;
        if (p == null) {
          p = makePermissionCollection();
          if (readOnly) {
            p.setReadOnly();
          }
          systemPermissions = p;
        }
      }
    }
    return p;
  }


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
          Permission p = makePermission(new PermissionInfo(l), false);
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


  private PermissionCollection makeImplicitPermissionCollection(Framework fw, Bundle b) {
    Permissions pc = new Permissions();
    if (dataRoot != null) {
      pc.add(new FilePermission(dataRoot.getPath(), "read,write"));
      pc.add(new FilePermission((new File(dataRoot, "-")).getPath(),
                                "read,write,delete"));
    }
    pc.add(new AdminPermission(b,
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
   * @param bundle The bundle whose permissions are to be created.
   *
   * @return The permissions assigned to the bundle with the specified
   * location, or the default permissions if that bundle has not been assigned
   * any permissions.
   */
  private PermissionCollection makePermissionCollection() {
    PermissionInfo[] pi = pinfos.get(location, this);
    boolean usingDefault;
    if (pi == null) {
      pi = pinfos.getDefault(this);
      usingDefault = true;
    } else {
      usingDefault = false;
    }
    Permissions res = new Permissions();
    for (int i = pi.length - 1; i >= 0; i--) {
      Permission p = makePermission(pi[i], usingDefault);
      if (p != null) {
        res.add(p);
      }
    }
    return res;
  }


  /**
   * 
   * @param pi PermissionInfo to enter into the PermissionCollection.
   *
   * @return 
   */
  private Permission makePermission(PermissionInfo pi, boolean usingDefault) {
    String a = pi.getActions();
    String n = pi.getName();
    String t = pi.getType();
    try {
      Class pc = Class.forName(t);
      Constructor c = pc.getConstructor(new Class[] { String.class, String.class });
      if (FilePermission.class.equals(pc)) {
        File f = new File(n);
        // NYI! How should we handle different seperator chars.
        if (!f.isAbsolute()) {
          if (usingDefault) {
            return null;
          }
          f = new File(dataRoot, n);
        }
        n = f.getPath();
      }
      return (Permission) c.newInstance(new Object[] { n, a });
    } catch (ClassNotFoundException e) {
      return new UnresolvedPermission(t, n, a, null);
    } catch (NoSuchMethodException ignore) {
    } catch (InstantiationException ignore) {
    } catch (IllegalAccessException ignore) {
    } catch (InvocationTargetException ignore) {
    }
    return null;
  }
}
