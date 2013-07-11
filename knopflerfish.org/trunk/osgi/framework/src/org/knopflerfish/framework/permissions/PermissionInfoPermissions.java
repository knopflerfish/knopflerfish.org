/*
 * Copyright (c) 2009-2013, KNOPFLERFISH project
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.UnresolvedPermission;
import java.util.ArrayList;
import java.util.Enumeration;

import org.knopflerfish.framework.Debug;
import org.knopflerfish.framework.FrameworkContext;
import org.osgi.service.permissionadmin.PermissionInfo;


/**
 * Local Permission handling, with lazy loading of permissions
 *
 * @author Jan Stein
 */


class PermissionInfoPermissions extends PermissionCollection {
  private static final long serialVersionUID = 1L;

  private Permissions pc ;
  volatile private PermissionInfo [] pinfo;
  private int unresolved;

  final private File dataRoot;
  final private FrameworkContext framework;
  final private Debug debug;

  static final Permission allPermission = new AllPermission();
  /**
   *
   */
  PermissionInfoPermissions(FrameworkContext fw,
                            File root,
                            InputStream ps) {
    framework = fw;
    debug = fw.debug;
    dataRoot = root;
    try {
      final BufferedReader dis = new BufferedReader(new InputStreamReader(ps));
      String l;
      final ArrayList<PermissionInfo> tmp = new ArrayList<PermissionInfo>();
      while ((l = dis.readLine()) != null) {
        l = l.trim();
        if (l.startsWith("#") || l.startsWith("//") || l.length() == 0) {
          continue;
        }
        try {
          tmp.add(new PermissionInfo(l));
        } catch (final Exception e) {
          // TODO, handle this error
        }
      }
      if (!tmp.isEmpty()) {
        pinfo = tmp.toArray(new PermissionInfo[tmp.size()]);
      }
    } catch (final IOException e) {
      // TODO, handle this error
    } finally {
      try {
        ps.close();
      } catch (final IOException _ignore) { }
    }
    if (pinfo != null) {
      unresolved = pinfo.length;
    } else {
      unresolved = 0;
      pc = new Permissions();
    }
  }


  /**
   *
   */
  PermissionInfoPermissions(FrameworkContext fw,
                            File root,
                            PermissionInfo [] pinfo) {
    framework = fw;
    dataRoot = root;
    debug = fw.debug;
    if (pinfo != null) {
      this.pinfo = pinfo.clone();
      unresolved = pinfo.length;
    } else {
      unresolved = 0;
    }
    if (unresolved == 0) {
      pc = new Permissions();
    }
  }


  /**
   *
   */
  @Override
  public void add(Permission permission) {
    throw new UnsupportedOperationException("Readonly");
  }


  /**
   *
   */
  @Override
  public Enumeration<Permission> elements() {
    if (unresolved != 0) {
      resolve();
    }
    return pc.elements();
  }


  /**
   *
   */
  @Override
  public boolean implies(final Permission permission) {
    if (unresolved != 0) {
      resolve();
    }
    return pc.implies(permission);
  }


  /**
   *
   */
  @Override
  public boolean isReadOnly() {
    return true;
  }


  /**
   *
   */
  @Override
  public void setReadOnly() {
  }


  /**
   *
   */
  synchronized private void resolve() {
    if (pinfo == null) {
      return;
    }
    if (pc == null) {
      pc = new Permissions();
    }
    for (int i = 0; i < pinfo.length; i++) {
      if (pinfo[i] != null) {
        final Permission p = makePermission(pinfo[i]);
        if (p != null) {
          pc.add(p);
          unresolved--;
          pinfo[i] = null;
        }
      } else if (debug.permissions) {
        debug.println("makePermission: Failed to create permission " + pinfo[i]);
      }
    }
    if (unresolved == 0) {
      pinfo = null;
    }
  }




  /**
   *
   * @param pi PermissionInfo to enter into the PermissionCollection.
   *
   * @return
   */
  private Permission makePermission(PermissionInfo pi) {
    final String t = pi.getType();
    if ("java.security.AllPermission".equals(t)) {
      return allPermission;
    }
    final ClassLoader cl = framework.getClassLoader(t);
    if (cl == null) {
      return null;
    }
    final String a = pi.getActions();
    String n = pi.getName();
    try {
      final Class<?> pc = Class.forName(t, true, cl);
      final Constructor<?> c = pc.getConstructor(new Class[] { String.class, String.class });
      if (FilePermission.class.equals(pc) && !"<<ALL FILES>>".equals(n)) {
        File f = new File(n);
        // NYI! How should we handle different separator chars.
        if (!f.isAbsolute()) {
          if (dataRoot == null) {
            return null;
          }
          f = new File(dataRoot, n);
        }
        n = f.getPath();
      }
      return (Permission) c.newInstance(new Object[] { n, a });
    } catch (final ClassNotFoundException ignore) {
      return new UnresolvedPermission(t, n, a, null);
    } catch (final NoSuchMethodException ignore) {
    } catch (final InstantiationException ignore) {
    } catch (final IllegalAccessException ignore) {
    } catch (final InvocationTargetException ignore) {
    }
    return null;
  }

}
