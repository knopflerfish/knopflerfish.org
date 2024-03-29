/*
 * Copyright (c) 2006-2022, KNOPFLERFISH project
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;

import org.osgi.service.permissionadmin.PermissionInfo;
import org.knopflerfish.framework.Debug;
import org.knopflerfish.framework.FWProps;
import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.Util;


class PermissionInfoStorage {

  final static String DEFAULTPERM = "(java.security.AllPermission)";

  private PermissionInfo[] initialDefault = null;

  private final File permDir;

  private long lastPermFile;

  private final HashMap<String, Element> permissions
    = new HashMap<String, Element>();

  private PermissionInfo[] defaultPermissions;

  private final HashMap<String, ArrayList<PermissionsWrapper>>
    defaultInvalidateCallbacks
      = new HashMap<String, ArrayList<PermissionsWrapper>>();

  final private Debug debug;

  final boolean readOnly;

  /**
   *
   */
  public PermissionInfoStorage(FrameworkContext ctx) {
    debug = ctx.debug;
    initialDefault = new PermissionInfo[] { new PermissionInfo(DEFAULTPERM) };
    defaultPermissions = initialDefault;

    readOnly = ctx.props.getBooleanProperty(FWProps.READ_ONLY_PROP);
    permDir = Util.getFileStorage(ctx, "perms", !readOnly);
    if (permDir == null) {
      System.err.println("Property org.osgi.framework.dir not set," +
                         "permission data will not be saved between sessions");
    } else if (permDir.isDirectory()) {
      load();
    }
  }


  /**
   * Get the specified permissions to the bundle with the specified
   * location.
   *
   * @param location The location of the bundle.
   */
  synchronized PermissionInfo[] get(String location, PermissionsWrapper callInvalidate) {
    final Element res = permissions.get(location);
    if (res != null) {
      if (callInvalidate != null) {
        if (res.invalidateCallback == null) {
          res.invalidateCallback = new ArrayList<PermissionsWrapper>(2);
        }
        res.invalidateCallback.add(callInvalidate);
      }
      return res.pi;
    }
    return null;
  }


  /**
   * Get the default permissions.
   * <p>
   * These are the permissions granted to any bundle that does not have
   * permissions assigned to its location.
   *
   * @return the default permissions.
   */
  synchronized PermissionInfo[] getDefault(PermissionsWrapper callInvalidate) {
    if (callInvalidate != null && callInvalidate.location != null) {
      ArrayList<PermissionsWrapper> cil = defaultInvalidateCallbacks.get(callInvalidate.location);
      if (cil == null) {
        cil = new ArrayList<PermissionsWrapper>(2);
        defaultInvalidateCallbacks.put(callInvalidate.location, cil);
      }
      cil.add(callInvalidate);
    }
    return defaultPermissions;
  }


  /**
   * Returns the bundle locations that have permissions assigned to them, that
   * is, bundle locations for which an entry exists in the permission table.
   *
   * @return The locations of bundles that have been assigned any permissions,
   *         or <tt>null</tt> if the permission table is empty.
   */
  synchronized String [] getKeys() {
    final int size = permissions.size();
    if (size == 0) {
      return null;
    } else {
      final String [] res = new String [size];
      int ix = 0;
      for (final String string : permissions.keySet()) {
        res[ix++] = string;
      }
      return res;
    }
  }


  /**
   * Assigns the specified permissions to the bundle with the specified
   * location.
   *
   * @param location The location of the bundle that will be assigned the
   *        permissions.
   * @param permissions The permissions to be assigned, or {@code null} if
   *        the specified location is to be removed from the permission table.
   */
  synchronized void put(String location, PermissionInfo[] perms)
  {
    final Element old = permissions.put(location, new Element(perms));
    save(location, perms);
    final ArrayList<PermissionsWrapper> vpw = old != null ? old.invalidateCallback
        : defaultInvalidateCallbacks.remove(location);
    if (vpw != null) {
      for (final PermissionsWrapper permissionsWrapper : vpw) {
        permissionsWrapper.invalidate();
      }
    }
  }

  /**
   * Sets the default permissions.
   *
   * <p>
   * These are the permissions granted to any bundle that does not have
   * permissions assigned to its location.
   *
   * @param permissions The default permissions, or {@code null} if the
   *        default permissions are to be removed from the permission table.
   * @throws SecurityException If the caller does not have
   *            {@code AllPermission}.
   */
  synchronized void putDefault(PermissionInfo[] permissions) {
    if (permissions != null) {
      defaultPermissions = permissions;
    } else {
      defaultPermissions = initialDefault;
    }
    save(null, defaultPermissions);
    for (final ArrayList<PermissionsWrapper> pws : defaultInvalidateCallbacks.values()) {
      for (final PermissionsWrapper pw : pws) {
        pw.invalidate();
      }
    }
    defaultInvalidateCallbacks.clear();
  }


  /**
   * Remove any specified permissions to the bundle with the specified
   * location.
   *
   * @param location The location of the bundle.
   */
  synchronized void remove(String location) {
    final Element old = permissions.remove(location);
    save(location, null);
    if (old != null && old.invalidateCallback != null) {
      for (final PermissionsWrapper permissionsWrapper : old.invalidateCallback) {
        permissionsWrapper.invalidate();
      }
    }
  }


  /**
   * Remove any reference to specified permission collection in the
   * callback tables.
   *
   * @param pc The permission collection to purge.
   */
  synchronized void purgeCallback(PermissionCollection pc) {
    final PermissionsWrapper pw = (PermissionsWrapper)pc;
    final Element e = permissions.get(pw.location);
    if (e != null && e.invalidateCallback != null && e.invalidateCallback.remove(pw)) {
      if (e.invalidateCallback.isEmpty()) {
        e.invalidateCallback = null;
      }
    } else {
      final ArrayList<PermissionsWrapper> cil
        = defaultInvalidateCallbacks.get(pw.location);
      if (cil != null && cil.remove(pw)) {
        if (cil.isEmpty()) {
          defaultInvalidateCallbacks.remove(pw);
        }
      }
    }
  }

  //
  // Private methods
  //

  /**
   * Save a permission array.
   */
  private void save(final String location, final PermissionInfo [] perms) {
    if (permDir != null && !readOnly) {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
          public Object run() {
            File f;
            String loc;
            if (location != null) {
              if (lastPermFile % 20 == 0) {
                purge();
              }
              f = new File(permDir, Long.toString(++lastPermFile));
              loc = location;
            } else {
              // NYI! keep backups
              f = new File(permDir, "default");
              loc = "defaultPermissions";
            }
            BufferedWriter out = null;
            try {
              out = new BufferedWriter(new FileWriter(f));
              int p;
              while ((p = loc.indexOf('\n')) != -1) {
                out.write(loc.substring(0, ++p) + " ");
                loc = loc.substring(p);
              }
              out.write(loc + "\n\n");
              if (perms != null) {
                for (final PermissionInfo perm : perms) {
                  out.write(perm.getEncoded() + "\n");
                }
              } else {
                out.write("NULL\n");
              }
              out.write("\n");
              out.close();
            } catch (final IOException e) {
              if (out != null) {
                try {
                  out.close();
                } catch (final IOException ignore) { }
                f.delete();
              }
              debug.printStackTrace("NYI! Report error", e);
            }
            return null;
          }
        });
    }
  }


  /**
   * Load all saved permission data.
   */
  private void load() {
    final File[] files = PermUtil.getSortedFiles(permDir);
    for (final File file : files) {
      load(file);
    }
    try {
      lastPermFile = Long.parseLong(files[files.length - 1].getName());
    } catch (final Exception e) {
      lastPermFile = -1;
    }
  }

  /**
   * Load saved permission data from named file.
   */
  private void load(File fh) {
    BufferedReader in = null;
    final boolean isDefault = "default".equals(fh.getName());
    try {
      in = new BufferedReader(new FileReader(fh));
      final String loc = parseLocation(in);
      final ArrayList<PermissionInfo> piv = new ArrayList<PermissionInfo>();
      // Read permissions
      int c = in.read();
      while (c != -1) {
        final StringBuilder pe = new StringBuilder();
        while (c != -1 && c != '\n') {
          pe.append((char) c);
          c = in.read();
        }
        final String line = pe.toString();
        if ("NULL".equals(line)) {
          // Clear any previous entries
          if (isDefault) {
            defaultPermissions = null;
          } else {
            permissions.remove(loc);
          }
          try {
            in.close();
          } catch (final IOException ignore) { }
          return;
        } else if ("".equals(line)) {
          // Correct end with double NL
          break;
        }
        piv.add(new PermissionInfo(line));
        c = in.read();
      }
      if (c == -1) {
        throw new IOException("Premature EOF when parsing permission file: " + fh.getName());
      }
      if (in.read() != -1) {
        throw new IOException("Garbage at end of file when parsing permission file: " + fh.getName());
      }
      in.close();
      final PermissionInfo[] pi = new PermissionInfo[piv.size()];
      piv.toArray(pi);
      if (isDefault) {
        defaultPermissions = pi;
      } else {
        permissions.put(loc, new Element(pi));
      }
    } catch (final IOException e) {
      if (in != null) {
        try {
          in.close();
        } catch (final IOException ignore) { }
      }
      debug.printStackTrace("NYI! Report error", e);
    }
  }

  /**
   * Parse location data.
   */
  private String parseLocation(Reader in) throws IOException {
    final StringBuilder loc = new StringBuilder();
    int c;
    // Read location
    while ((c = in.read()) != -1) {
      final char cc = (char)c;
      if (cc == '\n') {
        c = in.read();
        if (c != ' ') {
          break;
        }
      }
      loc.append(cc);
    }
    return loc.toString();
  }

  /**
   * Purge old saved permission data.
   * Keep two latest version of permission data.
   */
  private void purge() {
    final HashMap<String, Boolean> foundTwo = new HashMap<String, Boolean>();
    final File[] files = PermUtil.getSortedFiles(permDir);
    for (int i = files.length - 1; i >= 0; i--) {
      String loc;
      BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(files[i]));
        loc = parseLocation(in);
      } catch (final IOException ignore) {
        files[i].delete();
        continue;
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (final IOException ignore) { }
        }
      }
      final Boolean v = foundTwo.get(loc);
      if (v != null) {
        if (v.booleanValue()) {
          files[i].delete();
        } else {
          foundTwo.put(loc, new Boolean(true));
        }
      } else {
        foundTwo.put(loc, new Boolean(false));
      }
    }
  }


  static class Element {
    PermissionInfo[] pi;
    ArrayList /* PermissionsWrapper */<PermissionsWrapper> invalidateCallback = null;

    Element(PermissionInfo[] pi) {
      this.pi = pi;
    }
  }
}
