/*
 * Copyright (c) 2008-2010, KNOPFLERFISH project
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

import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.*;
import org.knopflerfish.framework.Util;
import org.knopflerfish.framework.Debug;


class ConditionalPermissionInfoStorage {

  private File condPermDir;

  private long lastFile;

  private ArrayList /* ConditionalPermissionInfoImpl */ cpiTable = new ArrayList();

  private PermissionsHandle ph;

  private int generation = -1;

  final private Debug debug;

  /**
   *
   */
  ConditionalPermissionInfoStorage(PermissionsHandle ph) {
    this.ph = ph;
    debug = ph.framework.debug;
    condPermDir = Util.getFileStorage(ph.framework, "condperm");
    if (condPermDir == null) {
      System.err.println("Property org.osgi.framework.dir not set," +
                         "conditional permission info will not be saved between sessions");
    } else {
      load();
    }
  }


  /**
   * Get the specified conditional permissions.
   * 
   * @param name The name of the Conditional Permission Info to be returned.
   * @return The Conditional Permission Info with the specified name.
   */
  synchronized ConditionalPermissionInfo get(String name) {
    int i = find(name);
    if (i >= 0) {
      return (ConditionalPermissionInfo)cpiTable.get(i);
    }
    return null;
  }


  /**
   * Get an enumeration of copied conditional permission info table.
   *
   * @return Enumeration of Conditional Permission Info objects.
   */
  synchronized Enumeration getAllEnumeration() {
    return (new Vector(cpiTable)).elements();
  }


  /**
   * Get conditional permission info table.
   * Should be called from synchronized code.
   *
   * @return ArrayList of Conditional Permission Info objects.
   */
  ArrayList getAll() {
    return cpiTable;
  }


  /**
   * Get a copy conditional permission info table.
   *
   * @return Enumeration of Conditional Permission Info objects.
   */
  synchronized ConditionalPermissionUpdate getUpdate() {
    return new ConditionalPermissionUpdateImpl(this, cpiTable, generation);
  }


  /**
   * Add a new or updated an old specified conditional permission.
   * 
   * @param name The name of the Conditional Permission Info to be changed.
   * @param conds The Conditions that need to be satisfied to enable the
   *        corresponding Permissions.
   * @param perms The Permissions that are enable when the corresponding
   *        Conditions are satisfied.
   * @return The ConditionalPermissionInfo object for the added
   *         conditional permission.
   */
  synchronized ConditionalPermissionInfo
  put(String name, ConditionInfo conds[], PermissionInfo perms[]) {
    int oldIx;
    if (name == null) {
      name = uniqueName();
      oldIx = -1;
    } else if (name.equals("")) {
      throw new IllegalArgumentException("Name can not be an empty string");
    } else {
      oldIx = find(name);
    }
    ConditionalPermissionInfoImpl old;
    ConditionalPermissionInfoImpl res = new ConditionalPermissionInfoImpl(this, name, conds, perms, ConditionalPermissionInfo.ALLOW, ph.framework);
    if (oldIx >= 0) {
      old = (ConditionalPermissionInfoImpl)cpiTable.set(oldIx, res);
      updateChangedConditionalPermission(res, oldIx, oldIx);
    } else {
      old = null;
      cpiTable.add(0, res);
      updateChangedConditionalPermission(res, 0, -1);
    }
    generation++;
    save();
    if (debug.permissions) {
      debug.println("CondPermStorage set " + res);
      if (old != null) {
        debug.println("CondPermStorage replaced " + old);
      }
    }
    return res;
  }


  /**
   * Remove any specified conditional permission with the specified name.
   * 
   * @param name The name of the Conditional Permission Info to be changed.
   */
  synchronized void remove(ConditionalPermissionInfoImpl obj) {
    int pos = cpiTable.indexOf(obj);
    if (debug.permissions) {
      debug.println("CondPermStorage remove " + obj + ", pos=" + pos);
    }
    if (pos >= 0) {
      cpiTable.remove(pos);
      updateChangedConditionalPermission(null, -1, pos);
      generation++;
      save();
      if (debug.permissions) {
        debug.println("CondPermStorage removed " + obj);
      }
    }
  }


  /**
   * Commit a new cpi table.
   * 
   * @param name The name of the Conditional Permission Info to be changed.
   */
  synchronized boolean commitUpdate(List updatedTable, int parentGen) {
    if (parentGen != generation) {
      return false;
    }
    ArrayList checkTable = new ArrayList(updatedTable);
    HashSet /* String */ names = new HashSet();
    int oi = 0;
    ConditionalPermissionInfoImpl ocpi = (ConditionalPermissionInfoImpl)(oi < cpiTable.size() ? cpiTable.get(oi) : null);
    int [] update = new int[cpiTable.size() + checkTable.size()];
    int ui = 0;
    String uniqueNameBase = Integer.toString(generation, Character.MAX_RADIX) + "_";
    int i = 0;
    for ( ; i < checkTable.size(); i++) {
      ConditionalPermissionInfoImpl cpi;
      try {
        cpi = (ConditionalPermissionInfoImpl)checkTable.get(i);
      } catch (ClassCastException _) {
        throw new IllegalStateException("Illegal class of element in updated table, index=" + i);
      }
      if (cpi == null) {
        throw new IllegalStateException("Updated table contains null element, index=" + i);
      }
      String name = cpi.getName();
      if (name != null) {
        if (!names.add(name)) {
          throw new IllegalStateException("Updated table contains elements with same name, name=" + name);
        }
        while (name.startsWith(uniqueNameBase)) {
          uniqueNameBase += "_";
        }
      }
      if (cpi == ocpi) {
        // Update doesn't differ check next
        ocpi = (ConditionalPermissionInfoImpl)(++oi < cpiTable.size() ? cpiTable.get(oi) : null);
      } else {
        int removed = 0;
        for (int j = oi + 1; j < cpiTable.size(); j++) {
          if (cpiTable.get(j) == cpi) {
            // Found updated, further down the table, removed or moved intermediate.
            removed = j;
            break;
          }
        }
        if (removed != 0) {
          // remove intermediate objects
          while (oi++ < removed) {
            update[ui++] = -i - 1;
          }
          ocpi = (ConditionalPermissionInfoImpl)(oi < cpiTable.size() ? cpiTable.get(oi) : null);
        } else {
          // New element add it
          update[ui++] = i;
        }
      }
    }
    // remove trailing objects
    while (oi++ < cpiTable.size()) {
      update[ui++] = -i - 1;
    }

    // If no updates just return
    if (ui == 0) {
      return true;
    }

    // Perform updateds on caches and set null names
    final int NOP = Integer.MIN_VALUE;
    int uniqueCounter = 0;
    int rememberLastInsert = 0;
    for (int pui = 0; pui < ui; pui++) {
      int u = update[pui];
      if (u >= 0) {
        // We have an insert, see if we find a matching remove to avoid array shuffling
        int remove = -1;
        int rMatch;
        int ipui;
        if (pui < rememberLastInsert) {
          rMatch = u + rememberLastInsert - pui;
          ipui = rememberLastInsert;
        } else {
          rMatch = u + rememberLastInsert - pui;
          ipui = pui;
        }
        while (++ipui < ui) {
          int iu = update[ipui];
          if (iu == -rMatch) {
            remove = u;
            update[ipui] = NOP;
          } else if (iu != rMatch) {
            rememberLastInsert = iu;
            rMatch++;
            continue;
          }
          break;
        }
        ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl)checkTable.get(u);
        if (cpi.getName() == null) {
          cpi.setName(uniqueNameBase + uniqueCounter++);
        }
        cpi.setPermissionInfoStorage(this);
        if (u == remove) {
          cpiTable.set(u, cpi);
        } else if (remove == -1) {
          cpiTable.add(u, cpi);
        } else {
          // Case with different remove & insert position not used, yet
          throw new RuntimeException("NYI");
        }
        updateChangedConditionalPermission(cpi, u, remove);
      } else if (u != NOP) {
        u = -1 - u;
        cpiTable.remove(u);
        updateChangedConditionalPermission(null, -1, u);
      }
    }
    generation++;
    save();
    if (debug.permissions) {
      debug.println("CondPermStorage commited update, " + ui + " changes");
    }
    return true;
  }


  /**
   * Get number of ConditionPermissionInfos stored.
   * 
   * @return Number of ConditionPermissionInfos objects as an int.
   */
  synchronized int size() {
    return cpiTable.size();
  }


  //
  // Private methods
  //


  private int find(String name) {
    for (int i = 0; i < cpiTable.size(); i++) {
      if (((ConditionalPermissionInfo)cpiTable.get(i)).getName().equals(name)) {
        return i;
      }
    }
    return -1;
  }


  /**
   * Find a unique name.
   */
  private String uniqueName() {
    String uniqueNameBase = Integer.toString(generation, Character.MAX_RADIX) + "_";
    for (Iterator i = cpiTable.iterator(); i.hasNext(); ) {
      String name = ((ConditionalPermissionInfoImpl)i.next()).getName();
      while (name.startsWith(uniqueNameBase)) {
        uniqueNameBase += "_";
      }
    }
    return uniqueNameBase + "0";
  }


  /**
   * Update cached information.
   */
  private void updateChangedConditionalPermission(ConditionalPermissionInfoImpl cpi,
                                                  int pos,
                                                  int removePos) {
    for (Iterator i = ph.getPermissionWrappers(); i.hasNext();) {
      ((PermissionsWrapper)i.next()).updateChangedConditionalPermission(cpi, pos, removePos, cpiTable.size());
    }
  }


  final static String END_MARKER = "END";

  /**
   * Save a permission array.
   */
  private void save() {
    if (debug.permissions) {
      debug.println("CondPermStorage save " + size() + " cpis, gen=" + generation);
    }
    if (condPermDir != null) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            purge();
            File f = new File(condPermDir, Long.toString(++lastFile));
            BufferedWriter out = null;
            try {
              out = new BufferedWriter(new FileWriter(f));
              out.write("# Save generation " + generation + " at: " +  System.currentTimeMillis());
              out.newLine();
              for (Iterator i = cpiTable.iterator(); i.hasNext(); ) {
                out.write(((ConditionalPermissionInfoImpl)i.next()).toString());
                out.newLine();
              }
              out.write(END_MARKER);
              out.newLine();
              out.close();
            } catch (IOException e) {
              if (out != null) {
                try {
                  out.close();
                } catch (IOException ignore) { }
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
   * Load all saved conditional permission data.
   */
  private void load() {
    File[] files = PermUtil.getSortedFiles(condPermDir);
    lastFile = -1;
    for (int i = files.length - 1; i >= 0; i--) {
      try {
        long l = Long.parseLong(files[i].getName());
        if (l > lastFile) {
          lastFile = l;
        }
      } catch (Exception ignore) {
        // Ignore file that isn't a number
        continue;
      }
      if (load(files[i])) {
        break;
      } else {
        // Load failed, purge file
        files[i].delete();
      }
    }
  }


  /**
   * Load saved conditional permission data from specified file.
   */
  private boolean load(File fh) {
    BufferedReader in = null;
    ArrayList loadTable = new ArrayList();
    try {
      in = new BufferedReader(new FileReader(fh));
      for (String l = in.readLine(); l != null; l = in.readLine()) {
        l = l.trim();
        if (l.equals("") || l.startsWith("#")) {
          continue;
        } else if (l.equals(END_MARKER)) {
          in.close();
          cpiTable = loadTable;
          return true;
        } else {
          ConditionalPermissionInfo res = new ConditionalPermissionInfoImpl(this, l, ph.framework);
          loadTable.add(res);
        }
      }
      in.close();
    } catch (Exception e) {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) { }
      }
      debug.printStackTrace("NYI! Report error", e);
    }
    return false;
  }


  /**
   * Prune unused data files in conditional permission directory.
   */
  private void purge() {
    File[] files = PermUtil.getSortedFiles(condPermDir);
    int okName = 0;
    for (int i = files.length - 1; i >= 0; i--) {
      try {
        Long.parseLong(files[i].getName());
        if (++okName > 2) {
          files[i].delete();
        }
      } catch (Exception ignore) {
        // Ignore files which aren't a number.
      }
    }
  }

}
