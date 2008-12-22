/*
 * Copyright (c) 2006-2008, KNOPFLERFISH project
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

import org.osgi.service.permissionadmin.PermissionInfo;


class PermUtil {

  /**
   */
  public static StringBuffer quote(String str, StringBuffer out) {
    if (out == null) {
      out = new StringBuffer();
    }
    int len = str.length();
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      switch (c) {
      case '"' : case '\\' :
	out.append('\\');
	out.append(c);
	break;
      case '\r' :
	out.append("\\r");
	break;
      case '\n' :
	out.append("\\n");
	break;
      default :
	out.append(c);
	break;
      }
    }
    return out;
  }


  /**
   */
  public static int skipWhite(char [] ca, int pos) {
    while (ca[pos] == ' ' || ca[pos] == '\t') {
      pos++;
    }
    return pos;
  }


  /**
   *
   */
  public static int unquote(char [] ca, int pos, StringBuffer out) {
    if (ca[pos++] != '"') {
      throw new IllegalArgumentException("Input not a quoted string");
    }
    while (ca[pos] != '"') {
      char c = ca[pos++];
      if (c == '\\') {
	switch (ca[pos++]) {
	case '"' :
	  c = '"';
	  break;
	case '\\' :
	  c = '\\';
	  break;
	case 'n' :
	  c = '\n';
	  break;
	case 'r' :
	  c = '\r';
	  break;
	default :
	  throw new IllegalArgumentException("Illegal escape char in quoted string: \\" + ca[pos-1]);
	}
      }
      if (out != null) {
	out.append(c);
      }
    }
    return pos + 1;
  }


  /**
   * Get data files sorted, first all nonnumeric
   * and then the numeric in ascending order.
   */
  public static File[] getSortedFiles(File dir) {
    String[] files = dir.list();
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
        res[pos++] = new File(dir, files[i]);
      }
    }
    for (int i = 0; i <= lf; i++) {
      res[pos++] = new File(dir, Long.toString(lfiles[i]));
    }
    return res;
  }


  /**
   */
  static PermissionCollection makePermissionCollection(PermissionInfo[] pi, File dataRoot) {
    Permissions res = new Permissions();
    for (int i = pi.length - 1; i >= 0; i--) {
      Permission p = makePermission(pi[i], dataRoot);
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
  static Permission makePermission(PermissionInfo pi, File dataRoot) {
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
          if (dataRoot == null) {
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
