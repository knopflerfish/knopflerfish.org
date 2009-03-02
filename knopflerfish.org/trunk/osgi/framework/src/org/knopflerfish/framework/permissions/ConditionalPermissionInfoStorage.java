/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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


class ConditionalPermissionInfoStorage {

  private File condPermDir;

  private long lastFile;

  private HashMap /* String -> ConditionalPermissionInfoImpl */ cpiMap = new HashMap();

  private long unique_id = 0;

  private PermissionsHandle ph;


  /**
   *
   */
  ConditionalPermissionInfoStorage(PermissionsHandle ph) {
    this.ph = ph;
    condPermDir = Util.getFileStorage("condperm");
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
    return (ConditionalPermissionInfo)cpiMap.get(name);
  }


  /**
   * Get enumeration of all stored conditional permissions.
   *
   * @return Enumeration of Conditional Permission Info objects.
   */
  synchronized Enumeration getAll() {
    return (new Vector(cpiMap.values())).elements();
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
    if (name == null) {
      name = uniqueName();
    } else if (name.equals("")) {
      throw new IllegalArgumentException("Name can not be an empty string");
    }
    ConditionalPermissionInfoImpl res = new ConditionalPermissionInfoImpl(this, name, conds, perms);
    ConditionalPermissionInfoImpl old = (ConditionalPermissionInfoImpl)cpiMap.put(name, res);
    save(name, res);
    if (Debug.permissions) {
      Debug.println("PERMISSIONADMIN set " + res);
      if (old != null) {
	Debug.println("PERMISSIONADMIN replaced " + old);
      }
    }
    updateChangedConditionalPermission(res, old);
    return res;
  }


  /**
   * Remove any specified conditional permission with the specified name.
   * 
   * @param name The name of the Conditional Permission Info to be changed.
   */
  synchronized void remove(String name) {
    ConditionalPermissionInfoImpl old = (ConditionalPermissionInfoImpl)cpiMap.remove(name);
    save(name, null);
    if (Debug.permissions) {
      Debug.println("PERMISSIONADMIN removed " + old);
    }
    updateChangedConditionalPermission(null, old);
  }


  /**
   * Get number of ConditionPermissionInfos stored.
   * 
   * @return Number of ConditionPermissionInfos objects as an int.
   */
  synchronized int size() {
    return cpiMap.size();
  }


  //
  // Private methods
  //

  /**
   * Find a unique name.
   */
  private String uniqueName() {
    String res;
    do {
      res = "ucpi" + Long.toString(unique_id++);
    } while (cpiMap.containsKey(res));
    return res;
  }


  /**
   * Update cached information.
   */
  private void updateChangedConditionalPermission(ConditionalPermissionInfoImpl cpi,
						  ConditionalPermissionInfoImpl old) {
    for (Iterator i = ph.getPermissionWrappers(); i.hasNext();) {
      ((PermissionsWrapper)i.next()).updateChangedConditionalPermission(cpi, old);
    }
  }


  /**
   * Save a permission array.
   */
  private void save(final String name, final ConditionalPermissionInfo cpi) {
    if (condPermDir != null) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
	    if (lastFile % 20 == 0) {
	      purge();
	    }
            File f = new File(condPermDir, Long.toString(++lastFile));
	    StringBuffer buf = new StringBuffer();
	    if (cpi != null) {
	      buf.append(cpi.toString());
	    } else {
	      buf.append('!');
	      PermUtil.quote(name, buf);
	    }
	    buf.append('\n');
            BufferedWriter out = null;
            try {
              out = new BufferedWriter(new FileWriter(f));
	      out.write(buf.toString());
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
   * Load all saved conditional permission data.
   */
  private void load() {
    File[] files = PermUtil.getSortedFiles(condPermDir);
    for (int i = 0; i < files.length; i++) {
      load(files[i]);
    }
    try {
      lastFile = Long.parseLong(files[files.length - 1].getName());
    } catch (Exception e) {
      lastFile = -1;
    }
  }


  /**
   * Load saved conditional permission data from specified file.
   */
  private void load(File fh) {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(fh));
      for (String l = in.readLine(); l != null; l = in.readLine()) {
	l = l.trim();
	if (l.equals("") || l.startsWith("#")) {
	  continue;
	} else if (l.startsWith("!")) {
	  StringBuffer buf = new StringBuffer();
	  PermUtil.unquote(l.toCharArray(), 1, buf);
	  cpiMap.remove(buf.toString());
        } else {
	  ConditionalPermissionInfo res = new ConditionalPermissionInfoImpl(this, l);
	  cpiMap.put(res.getName(), res);
	}
      }
      in.close();
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
   * Prune unused data files in conditional permission directory.
   */
  private void purge() {
    HashSet found = new HashSet();
    File[] files = PermUtil.getSortedFiles(condPermDir);
    ArrayList remove = new ArrayList();
    StringBuffer buf = new StringBuffer();
    boolean empty = false;
    for (int i = files.length - 1; i >= 0; i--) {
      BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(files[i]));
	for (String l = in.readLine(); l != null; l = in.readLine()) {
	  l = l.trim();
	  if (l.equals("") || l.startsWith("#")) {
	    continue;
	  } else {
	    empty = l.startsWith("!");
	    PermUtil.unquote(l.toCharArray(), empty ? 1 : 0, buf);
	    break;
	  }
	}
      } catch (IOException ignore) {
	// Remove faulty file, should we log?
        files[i].delete();
        continue;
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException ignore) { }
        }
      }
      if (buf.length() > 0) {
	if (found.add(buf.toString())) {
	  if (empty) {
	    remove.add(files[i]);
	  }
	} else {
	  // Already found entry for this name remove old file
	  if (!files[i].delete()) {
	    // Don't remove active empty entry if we failed too remove old.
	    remove.remove(files[i]);
	  }
	}
	buf.setLength(0);
      }
    }
    for (Iterator i = remove.iterator(); i.hasNext(); ) {
      ((File)i.next()).delete();
    }
  }

}
