/*
 * Copyright (c) 2008-2016, KNOPFLERFISH project
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.knopflerfish.framework.Debug;
import org.knopflerfish.framework.FWProps;
import org.knopflerfish.framework.Util;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;


class ConditionalPermissionInfoStorage {

  private final File condPermDir;

  private long lastFile;

  private ArrayList<ConditionalPermissionInfoImpl> cpiTable
    = new ArrayList<ConditionalPermissionInfoImpl>();

  private final PermissionsHandle ph;

  private int generation = -1;

  final private Debug debug;

  final private boolean readOnly;

  /**
   *
   */
  ConditionalPermissionInfoStorage(PermissionsHandle ph) {
    this.ph = ph;
    debug = ph.framework.debug;
    readOnly = ph.framework.props.getBooleanProperty(FWProps.READ_ONLY_PROP);
    condPermDir = Util.getFileStorage(ph.framework, "condperm", !readOnly);
    if (condPermDir == null) {
      System.err.println("Property org.osgi.framework.dir not set," +
                         "conditional permission info will not be saved between sessions");
    } else if (condPermDir.isDirectory()) {
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
    final int i = find(name);
    if (i >= 0) {
      return cpiTable.get(i);
    }
    return null;
  }


  /**
   * Get an enumeration of copied conditional permission info table.
   *
   * @return Enumeration of Conditional Permission Info objects.
   */
  synchronized Enumeration<ConditionalPermissionInfo> getAllEnumeration() {
    return (new Vector<ConditionalPermissionInfo>(cpiTable)).elements();
  }


  /**
   * Get conditional permission info table.
   * Should be called from synchronized code.
   *
   * @return ArrayList of Conditional Permission Info objects.
   */
  ArrayList<ConditionalPermissionInfoImpl> getAll() {
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
  synchronized ConditionalPermissionInfo put(String name,
                                             ConditionInfo conds[],
                                             PermissionInfo perms[])
  {
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
    final ConditionalPermissionInfoImpl res
      = new ConditionalPermissionInfoImpl(this, name, conds, perms, ConditionalPermissionInfo.ALLOW, ph.framework);
    if (oldIx >= 0) {
      old = cpiTable.set(oldIx, res);
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
    final int pos = cpiTable.indexOf(obj);
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
   * @param updatedTable The Conditional Permission Info to commit.
   * @param parentGen generation number of the active table that update is
   * based on.
   */
  synchronized boolean commitUpdate(List<ConditionalPermissionInfo> updatedTable,
                                    int parentGen)
  {
    if (parentGen != generation) {
      return false;
    }
    final ArrayList<ConditionalPermissionInfo> checkTable
      = new ArrayList<ConditionalPermissionInfo>(updatedTable);
    final HashSet<String> names = new HashSet<String>();
    int oi = 0;
    ConditionalPermissionInfoImpl ocpi = oi < cpiTable.size() ? cpiTable.get(oi) : null;
    final int [] update = new int[cpiTable.size() + checkTable.size()];
    int ui = 0;
    String uniqueNameBase = Integer.toString(generation, Character.MAX_RADIX) + "_";
    int nextRemove = update.length;
    int i = 0;
    for ( ; i < checkTable.size(); i++) {
      ConditionalPermissionInfoImpl cpi;
      try {
        cpi = (ConditionalPermissionInfoImpl)checkTable.get(i);
      } catch (final ClassCastException _ignore) {
        throw new IllegalStateException("Illegal class of element in updated table, index=" + i);
      }
      if (cpi == null) {
        throw new IllegalStateException("Updated table contains null element, index=" + i);
      }
      final String name = cpi.getName();
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
        ocpi = ++oi < cpiTable.size() ? cpiTable.get(oi) : null;
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
            if (nextRemove > ui) {
              nextRemove = ui;
            }
            update[ui++] = -i - 1;
          }
          ocpi = oi < cpiTable.size() ? cpiTable.get(oi) : null;
        } else {
          // New element add it
          update[ui++] = i;
        }
      }
    }
    // remove trailing objects
    if (oi++ < cpiTable.size()) {
      if (nextRemove > ui) {
        nextRemove = ui;
      }
      do {
        update[ui++] = -i - 1;
      } while (oi++ < cpiTable.size());
    }

    // If no updates just return
    if (ui == 0) {
      return true;
    }

    // Perform updates on caches and set null names
    final int NOP = Integer.MIN_VALUE;
    int nops = 0;
    int uniqueCounter = 0;
    for (int pui = 0; pui < ui; pui++) {
      int u = update[pui];
      if (u >= 0) {
        // We have an insert, see if we find a matching remove to avoid array shuffling
        boolean remove = false;
        if (nextRemove < ui) {
          int rMatch = u + nextRemove - pui - nops;
          if (update[nextRemove] + 1 == -rMatch) {
            update[nextRemove] = NOP;
            nops++;
            while (++nextRemove < ui && update[nextRemove] >= 0)
              ;
            remove = true;
          }
        }
        final ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl)checkTable.get(u);
        if (cpi.getName() == null) {
          cpi.setName(uniqueNameBase + uniqueCounter++);
        }
        cpi.setPermissionInfoStorage(this);
        if (remove) {
          cpiTable.set(u, cpi);
          updateChangedConditionalPermission(cpi, u, u);
        } else {
          cpiTable.add(u, cpi);
          updateChangedConditionalPermission(cpi, u, -1);
        }
      } else if (u != NOP) {
        while (++nextRemove < ui && update[nextRemove] >= 0)
          ;
        u = -1 - u;
        cpiTable.remove(u);
        updateChangedConditionalPermission(null, -1, u);
      } else {
        nops--;
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
    for (final Object element : cpiTable) {
      final String name = ((ConditionalPermissionInfoImpl)element).getName();
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
                                                  int removePos)
  {
    for (final Iterator<PermissionsWrapper> i = ph.getPermissionWrappers(); i
        .hasNext();) {
      i.next().updateChangedConditionalPermission(cpi, pos, removePos,
                                                  cpiTable.size());
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
    if (condPermDir != null && !readOnly) {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
          public Object run() {
            purge();
            final File f = new File(condPermDir, Long.toString(++lastFile));
            BufferedWriter out = null;
            try {
              out = new BufferedWriter(new FileWriter(f));
              out.write("# Save generation " + generation + " at: " +  System.currentTimeMillis());
              out.newLine();
              for (final Object element : cpiTable) {
                out.write(((ConditionalPermissionInfoImpl)element).toString());
                out.newLine();
              }
              out.write(END_MARKER);
              out.newLine();
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
   * Load all saved conditional permission data.
   */
  private void load() {
    final File[] files = PermUtil.getSortedFiles(condPermDir);
    lastFile = -1;
    for (int i = files.length - 1; i >= 0; i--) {
      try {
        final long l = Long.parseLong(files[i].getName());
        if (l > lastFile) {
          lastFile = l;
        }
      } catch (final Exception ignore) {
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
    final ArrayList<ConditionalPermissionInfoImpl> loadTable
      = new ArrayList<ConditionalPermissionInfoImpl>();
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
          final ConditionalPermissionInfoImpl res
            = new ConditionalPermissionInfoImpl(this, l, ph.framework);
          loadTable.add(res);
        }
      }
      in.close();
    } catch (final Exception e) {
      if (in != null) {
        try {
          in.close();
        } catch (final IOException ignore) { }
      }
      debug.printStackTrace("NYI! Report error", e);
    }
    return false;
  }


  /**
   * Prune unused data files in conditional permission directory.
   */
  private void purge() {
    final File[] files = PermUtil.getSortedFiles(condPermDir);
    int okName = 0;
    for (int i = files.length - 1; i >= 0; i--) {
      try {
        Long.parseLong(files[i].getName());
        if (++okName > 2) {
          files[i].delete();
        }
      } catch (final Exception ignore) {
        // Ignore files which aren't a number.
      }
    }
  }

}
