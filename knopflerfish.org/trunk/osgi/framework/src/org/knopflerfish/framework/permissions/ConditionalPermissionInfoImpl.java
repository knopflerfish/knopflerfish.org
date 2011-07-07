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

import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.PermissionInfo;

import org.knopflerfish.framework.Debug;
import org.knopflerfish.framework.FrameworkContext;


/**
 * A binding of a set of Conditions to a set of Permissions.
 * 
 */
class ConditionalPermissionInfoImpl implements ConditionalPermissionInfo
{
  private ConditionalPermissionInfoStorage cpis;

  final private ConditionInfo [] conditionInfos;
  final private PermissionInfo [] permissionInfos;
  final private String access;
  final private FrameworkContext framework;
  final private Debug debug;

  private String name;
  private PermissionCollection permissions;


  /**
   */
  ConditionalPermissionInfoImpl(ConditionalPermissionInfoStorage cpis, String name,
                                ConditionInfo [] conds, PermissionInfo [] perms,
                                String access, FrameworkContext fw) {
    this.cpis = cpis;
    this.name = name;
    conditionInfos = conds;
    permissionInfos = perms;
    this.access = access;
    framework = fw;
    debug = fw.debug;
    permissions = null;
  }


  /**
   */
  ConditionalPermissionInfoImpl(ConditionalPermissionInfoStorage cpis,
                                String encoded, FrameworkContext fw) {
    this.cpis = cpis;
    framework = fw;
    debug = fw.debug;
    try {
      char [] eca = encoded.toCharArray();
      int pos = PermUtil.skipWhite(eca, 0);
      if ((eca[pos] == 'A' || eca[pos] == 'a') &&
          (eca[pos+1] == 'L' || eca[pos+1] == 'l') &&
          (eca[pos+2] == 'L' || eca[pos+2] == 'l') &&
          (eca[pos+3] == 'O' || eca[pos+3] == 'o') &&
          (eca[pos+4] == 'W' || eca[pos+4] == 'w') && eca[pos+5] == ' ') {
        pos += 6;
        access = ConditionalPermissionInfo.ALLOW;
      } else if ((eca[pos] == 'D' || eca[pos] == 'd') &&
                 (eca[pos+1] == 'E' || eca[pos+1] == 'e') &&
                 (eca[pos+2] == 'N' || eca[pos+2] == 'n') &&
                 (eca[pos+3] == 'Y' || eca[pos+3] == 'y') && eca[pos+4] == ' ') {
        pos += 5;
        access = ConditionalPermissionInfo.DENY;
      } else {
        throw new IllegalArgumentException("Access must be allow or deny");
      }
      pos = PermUtil.skipWhite(eca, pos);
      if (eca[pos++] != '{') {
        throw new IllegalArgumentException("Missing open brace");
      }
      ArrayList cal = new ArrayList();
      ArrayList pal = new ArrayList();
      boolean seenPermInfo = false;
      while (true) {
        pos = PermUtil.skipWhite(eca, pos);
        char c = eca[pos];
        char ec;
        if (!seenPermInfo && c == '[') {
          ec = ']';
        } else if (c == '(') {
          ec = ')';
          seenPermInfo = true;
        } else if (c == '}') {
          pos++;
          break;
        } else {
          throw new IllegalArgumentException("Unexpected char '" + c + "' at pos " + pos);
        }
        int start_pos = pos++;
        do {
          c = eca[pos];
          if (c == '"') {
            pos = PermUtil.unquote(eca, pos, null);
          } else {
            pos++;
          }
        } while(c != ec);
        String info = new String(eca, start_pos, pos - start_pos);
        if (c == ']') {
          cal.add(new ConditionInfo(info));
        } else {
          pal.add(new PermissionInfo(info));
        }
      }
      if (!seenPermInfo) {
        throw new IllegalArgumentException("Permissions must contain atleast one element");
      }
      pos = PermUtil.endOfString(eca, pos, eca.length);
      if (pos != -1) {
        StringBuffer buf = new StringBuffer();
        pos = PermUtil.unquote(eca, pos, buf);
        name = buf.toString();
        if ((pos = PermUtil.endOfString(eca, pos, eca.length)) != -1) {
          throw new IllegalArgumentException("Unexpected characters at end of string: " +
                                             new String(eca, pos, eca.length - pos));
        }
      } else {
        name = null;
      }
      conditionInfos = (ConditionInfo [])cal.toArray(new ConditionInfo [cal.size()]);
      permissionInfos = (PermissionInfo [])pal.toArray(new PermissionInfo [pal.size()]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Unexpected end of string");
    }
  }

  // Interface ConditionalPermissionInfo

  /**
   * Returns the Condition Infos for the Conditions that must be satisfied to
   * enable the Permissions.
   * 
   * @return The Condition Infos for the Conditions in this Conditional
   *         Permission Info.
   */
  public ConditionInfo[] getConditionInfos() {
    return conditionInfos;
  }


  /**
   * Returns the Permission Infos for the Permission in this Conditional
   * Permission Info.
   * 
   * @return The Permission Infos for the Permission in this Conditional
   *         Permission Info.
   */
  public PermissionInfo[] getPermissionInfos() {
    return permissionInfos;
  }


  /**
   * Removes this Conditional Permission Info from the Conditional Permission
   * Admin.
   * 
   * @throws SecurityException If the caller does not have
   *         <code>AllPermission</code>.
   */
  public void delete() {
    if (cpis == null) {
      throw new UnsupportedOperationException("Not in use");
    }
    cpis.remove(this);
  }


  /**
   * Returns the name of this Conditional Permission Info.
   * 
   * @return The name of this Conditional Permission Info.
   */
  public String getName() {
    return name;
  }


  public String getAccessDecision() {
    return access;
  }


  public String getEncoded() {
    StringBuffer res = new StringBuffer(access);
    res.append(" { ");
    if (conditionInfos != null) {
      for (int i = 0; i < conditionInfos.length; i++) {
        res.append(conditionInfos[i].getEncoded());
        res.append(' ');
      }
    }
    if (permissionInfos != null) {
      for (int i = 0; i < permissionInfos.length; i++) {
        res.append(permissionInfos[i].getEncoded());
        res.append(' ');
      }
    }
    res.append('}');
    if (name != null) {
      res.append(' ');
      PermUtil.quote(name, res);
    }
    return res.toString();
  }


  /**
   * Returns a string representation of this object.
   * 
   */
  public String toString() {
    return getEncoded();
  }


  /**
   *
   */
  public final boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    ConditionalPermissionInfo cpi = (ConditionalPermissionInfo)obj;
    if (name == null ? cpi.getName() != null : !name.equals(cpi.getName())) {
      return false;
    }
    // NYI, we should allow permuted arrays, also affects hashCode.
    if (!Arrays.equals(permissionInfos, cpi.getPermissionInfos())) {
      return false;
    }
    if (!Arrays.equals(conditionInfos, cpi.getConditionInfos())) {
      return false;
    }
    return access == cpi.getAccessDecision();
  }

 
  /**
   *
   */
  public final int hashCode() {
    if (name != null) {
      return name.hashCode();
    }
    int res = conditionInfos != null && conditionInfos.length > 0
      ? conditionInfos[0].hashCode()
      : 0;
    return res + permissionInfos[0].hashCode();
  }

  //
  // Package methods
  //

  static final Class[] argClasses = new Class[] {Bundle.class, ConditionInfo.class};

  /**
   *
   */
  ConditionalPermission getConditionalPermission(Bundle bundle) {
    String me = "ConditionalPermissionInfoImpl.getConditionalPermission: ";

    ArrayList conds = new ArrayList(conditionInfos.length);
    for (int i = 0; i < conditionInfos.length; i++) {
      Class clazz;
      Condition c;
      try {
        clazz = Class.forName(conditionInfos[i].getType(),
                              true, framework.getClassLoader(null));
        Constructor cons = null;
        Method method = null;
        try {
          method = clazz.getMethod("getCondition", argClasses);
          if ((method.getModifiers() & Modifier.STATIC) == 0) {
            method = null;
          }
        } catch (NoSuchMethodException ignore) { }
        if (method != null) {
          if (debug.permissions) {
            debug.println(me + "Invoke, " + method + " for bundle " + bundle);
          }
          c = (Condition) method.invoke(null, new Object [] {bundle, conditionInfos[i]});
        } else {
          try {
            cons = clazz.getConstructor(argClasses);
          } catch (NoSuchMethodException ignore) { }
          if (cons != null) {
            if (debug.permissions) {
              debug.println(me + "Construct, " + cons + " for bundle " + bundle);
            }
            c = (Condition) cons.newInstance(new Object [] {bundle, conditionInfos[i]});
          } else {
            debug.println("NYI! Log faulty ConditionInfo object!?");
            continue;
          }
        }
        if (!c.isMutable()) {
          if (!c.isPostponed() /* || debug.tck401compat */ ) {
            if (c.isSatisfied()) {
              if (debug.permissions) {
                debug.println(me + "Immutable condition ok, continue");
              }
              continue;
            } else {
              if (debug.permissions) {
                debug.println(me + "Immutable condition NOT ok, abort");
              }
              return null;
            }
          }
        }
        conds.add(c);
      } catch (Throwable t) {
        debug.printStackTrace("NYI! Log failed Condition creation", t);
        return null;
      }
    }
    return new ConditionalPermission((Condition [])conds.toArray(new Condition[conds.size()]),
                                     getPermissions(), access, this);
  }


  /**
   *
   */
  PermissionCollection getPermissions() {
    if (permissions == null) {
      permissions = new PermissionInfoPermissions(framework, null, permissionInfos);
    }
    return permissions;
  }


  /**
   * Set storage for this Conditional Permission Info.
   * 
   */
  void setPermissionInfoStorage(ConditionalPermissionInfoStorage storage) {
    cpis = storage;
  }


  /**
   * Set the name of this Conditional Permission Info.
   * 
   */
  void setName(String newName) {
    name = newName;
  }

}
