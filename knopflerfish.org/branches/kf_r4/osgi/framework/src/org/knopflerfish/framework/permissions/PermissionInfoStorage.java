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
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.osgi.service.permissionadmin.PermissionInfo;
import org.knopflerfish.framework.Util;


class PermissionInfoStorage {

  final static String DEFAULTPERM = "(java.security.AllPermission)";

  private PermissionInfo[] initialDefault = null;

  private File permDir;

  private long lastPermFile;

  private HashMap /* String -> Element */ permissions = new HashMap();

  private PermissionInfo[] defaultPermissions;

  private HashMap defaultInvalidateCallbacks = new HashMap();


  public PermissionInfoStorage() {
     initialDefault = new PermissionInfo[] { new PermissionInfo(DEFAULTPERM) };
     defaultPermissions = initialDefault;

     permDir = Util.getFileStorage("perms");
     if (permDir == null) {
       System.err.println("Property org.osgi.framework.dir not set," +
       "permission data will not be saved between sessions");
     } else {
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
    Element res = (Element) permissions.get(location);
    if (res != null) {
      if (callInvalidate != null) {
        if (res.invalidateCallback == null) {
          res.invalidateCallback = new ArrayList(2);
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
    if (callInvalidate != null) {
      ArrayList cil = (ArrayList)defaultInvalidateCallbacks.get(callInvalidate.location);
      if (cil == null) {
        cil = new ArrayList(2);
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
    int size = permissions.size();
    if (size == 0) {
      return null;
    } else {
      String [] res = new String [size];
      int ix = 0;
      for (Iterator i = permissions.keySet().iterator(); i.hasNext();) {
        res[ix++] = (String)i.next();
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
   * @param permissions The permissions to be assigned, or <code>null</code> if
   *        the specified location is to be removed from the permission table.
   */
  synchronized void put(String location, PermissionInfo[] perms) {
    Element old = (Element)permissions.put(location, new Element(perms));
    save(location, perms);
    ArrayList vpw = old != null ? old.invalidateCallback :
      (ArrayList)defaultInvalidateCallbacks.remove(location);
    if (vpw != null) {
      for (Iterator i = vpw.iterator(); i.hasNext(); ) {
        ((PermissionsWrapper)i.next()).invalidate();
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
   * @param permissions The default permissions, or <code>null</code> if the
   *        default permissions are to be removed from the permission table.
   * @throws SecurityException If the caller does not have
   *            <code>AllPermission</code>.
   */
  synchronized void putDefault(PermissionInfo[] permissions) {
    if (permissions != null) {
      defaultPermissions = permissions;
    } else {
      defaultPermissions = initialDefault;
    }
    save(null, defaultPermissions);
    for (Iterator i = defaultInvalidateCallbacks.values().iterator(); i.hasNext(); ) {
      for (Iterator j = ((ArrayList)i.next()).iterator(); j.hasNext(); ) {
        ((PermissionsWrapper)j.next()).invalidate();
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
    Element old = (Element)permissions.remove(location);
    save(location, null);
    if (old != null && old.invalidateCallback != null) {
      for (Iterator i = old.invalidateCallback.iterator(); i.hasNext(); ) {
        ((PermissionsWrapper)i.next()).invalidate();
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
    PermissionsWrapper pw = (PermissionsWrapper)pc;
    Element e = (Element)permissions.get(pw.location);
    if (e != null && e.invalidateCallback != null && e.invalidateCallback.remove(pw)) {
      if (e.invalidateCallback.isEmpty()) {
        e.invalidateCallback = null;
      }
    } else {
      ArrayList cil = (ArrayList)defaultInvalidateCallbacks.get(pw.location);
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
    if (permDir != null) {
      AccessController.doPrivileged(new PrivilegedAction() {
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
                for (int i = 0; i < perms.length; i++) {
                  out.write(perms[i].getEncoded() + "\n");
                }
              } else {
                out.write("NULL\n");
              }
              out.write("\n");
              out.close();
            } catch (IOException e) {
              if (out != null) {
                try {
                  out.close();
                } catch (IOException ignore) { }
                f.delete();
              }
              // NYI! Report error
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
    File[] files = getSortedFiles();
    for (int i = 0; i < files.length; i++) {
      load(files[i]);
    }
  }

  /**
   * Load saved permission data from named file.
   */
  private void load(File fh) {
    BufferedReader in = null;
    boolean isDefault = "default".equals(fh.getName());
    try {
      in = new BufferedReader(new FileReader(fh));
      String loc = parseLocation(in);
      ArrayList piv = new ArrayList();
      // Read permissions
      int c = in.read();
      while (c != -1) {
        StringBuffer pe = new StringBuffer();
        while (c != -1 && c != (int)'\n') {
          pe.append((char) c);
          c = in.read();
        }
        String line = pe.toString();
        if ("NULL".equals(line)) {
          // Clear any previous entries
          if (isDefault) {
            defaultPermissions = null;
          } else {
            permissions.remove(loc);
          }
          try {
            in.close();
          } catch (IOException ignore) { }
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
      PermissionInfo[] pi = new PermissionInfo[piv.size()];
      piv.toArray(pi);
      if (isDefault) {
        defaultPermissions = pi;
      } else {
        permissions.put(loc, new Element(pi));
      }
    } catch (IOException e) {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) { }
      }
      // NYI! Report error
    }
  }

  /**
   * Parse location data.
   */
  private String parseLocation(Reader in) throws IOException {
    StringBuffer loc = new StringBuffer();
    int c;
    // Read location
    while ((c = in.read()) != -1) {
      char cc = (char)c;
      if (cc == '\n') {
        c = in.read();
        if (c != (int)' ') {
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
    HashMap foundTwo = new HashMap();
    File[] files = getSortedFiles();
    for (int i = files.length - 1; i >= 0; i--) {
      String loc;
      BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(files[i]));
        loc = parseLocation(in);
      } catch (IOException ignore) {
        files[i].delete();
        continue;
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException ignore) { }
        }
      }
      Boolean v = (Boolean)foundTwo.get(loc);
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

  /**
   * Get permission data files sorted, first all nonnumeric
   * and then the numeric in ascending order.
   */
  private File[] getSortedFiles() {
    String[] files = permDir.list();
    File[] res = new File[files.length];
    long[] lfiles = new long[files.length];
    int lf = -1;
    int pos = 0;
    for (int i = 0; i < files.length; i++) {
      try {
        long fval = Long.parseLong(files[i]);
        int j;
        for (j = lf; j >= 0; j--) {
          if (fval > lfiles[j]) {
            break;
          }
        }
        if (j >= lf) {
          lfiles[++lf] = fval;
        } else {
          lf++;
          j++;
          System.arraycopy(lfiles, j, lfiles, j+1, lf-j);
          lfiles[j] = fval;
        }
        files[i] = null;
      } catch (NumberFormatException ignore) {
        res[pos++] = new File(permDir, files[i]);
      }
    }
    for (int i = 0; i <= lf; i++) {
      res[pos++] = new File(permDir, Long.toString(lfiles[i]));
    }
    lastPermFile = (lf >= 0) ? lfiles[lf] : -1;
    return res;
  }

  class Element {
    PermissionInfo[] pi;
    ArrayList /* PermissionsWrapper */ invalidateCallback = null;

    Element(PermissionInfo[] pi) {
      this.pi = pi;
    }
  }
}
