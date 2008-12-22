/*
 * Copyright (c) 2008, KNOPFLERFISH project
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

  private HashMap /* String -> ConditionalPermissionInfo */ cpiMap = new HashMap();

  private long unique_id = 0;

  /**
   *
   */
  ConditionalPermissionInfoStorage() {
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
   */
  synchronized Enumeration getAll() {
    return (new Vector(cpiMap.values())).elements();
  }


  /**
   */
  synchronized ConditionalPermissionInfo
  put(String name, ConditionInfo conds[], PermissionInfo perms[]) {
    if (name == null) {
      name = uniqueName();
    } else if (name.equals("")) {
      throw new IllegalArgumentException("Name can not be an empty string");
    }
    ConditionalPermissionInfo res = new ConditionalPermissionInfoImpl(this, name, conds, perms);
    ConditionalPermissionInfo old = (ConditionalPermissionInfo)cpiMap.put(name, res);
    save(name, res);
    return res;
  }


  /**
   * Remove any specified permissions to the bundle with the specified
   * location.
   * 
   * @param location The location of the bundle.
   */
  synchronized void remove(String name) {
    cpiMap.remove(name);
    save(name, null);
  }


  /**
   * Get number of ConditionPermissionInfos.
   * 
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
   * Save a permission array.
   */
  private void save(final String name, final ConditionalPermissionInfo cpi) {
    if (condPermDir != null) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
	    if (lastFile % 20 == 0) {
	      compact();
	    }
            File f = new File(condPermDir, Long.toString(++lastFile));
	    StringBuffer buf = new StringBuffer("!");
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
   * Load all saved permission data.
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
   * Load saved permission data from named file.
   */
  private void load(File fh) {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(fh));
      for (String l = in.readLine(); l != null; l = in.readLine()) {
	if (l.equals("")) {
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
   * Compact data files in conditional permission directory.
   */
  private void compact() {
    // NYI!
  }

}
