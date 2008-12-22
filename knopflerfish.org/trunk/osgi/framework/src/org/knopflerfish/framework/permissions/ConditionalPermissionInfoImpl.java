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

import java.lang.reflect.*;
import java.security.*;
import java.util.ArrayList;

import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * A binding of a set of Conditions to a set of Permissions. Instances of this
 * interface are obtained from the Conditional Permission Admin service.
 * 
 * @version $Revision: 1.11 $
 */
public class ConditionalPermissionInfoImpl implements ConditionalPermissionInfo
{
  final private ConditionalPermissionInfoStorage cps;
  final private String name;

  final private ConditionInfo [] conditionInfos;
  final private PermissionInfo [] permissionInfos;

  private PermissionCollection permissions;

  /**
   */
  ConditionalPermissionInfoImpl(ConditionalPermissionInfoStorage cps, String name,
				ConditionInfo [] conds, PermissionInfo [] perms) {
    this.cps = cps;
    this.name = name;
    conditionInfos = conds;
    permissionInfos = perms;
    permissions = null;
  }


  /**
   */
  ConditionalPermissionInfoImpl(ConditionalPermissionInfoStorage cps, String encoded) {
    this.cps = cps;
    char [] eca = encoded.toCharArray();
    StringBuffer buf = new StringBuffer();
    int pos = PermUtil.skipWhite(eca, 0);
    pos = PermUtil.unquote(eca, pos, buf);
    name = buf.toString();
    ArrayList cal = new ArrayList();
    ArrayList pal = new ArrayList();
    pos = PermUtil.skipWhite(eca, pos);
    if (eca[pos++] != '{') {
      throw new IllegalArgumentException("Missing open brace");
    }
    while (true) {
      pos = PermUtil.skipWhite(eca, pos);
      char c = eca[pos];
      char ec;
      if (c == '[') {
	ec = ']';
      } else if (c == '(') {
	ec = ')';
      } else if (c == '}') {
	break;
      } else {
	throw new IllegalArgumentException("Unexpected char '" + c + "' at pos " + pos);
      }
      buf.setLength(0);
      do {
	c = eca[pos];
	if (c == '"') {
	  pos = PermUtil.unquote(eca, pos, buf);
	} else {
	  buf.append(c);
	}
	pos++;
      } while(c != ec);
      if (c == ']') {
	cal.add(new ConditionInfo(buf.toString()));
      } else {
	pal.add(new PermissionInfo(buf.toString()));
      }
    }
    conditionInfos = (ConditionInfo [])cal.toArray(new ConditionInfo [cal.size()]);
    permissionInfos = (PermissionInfo [])pal.toArray(new PermissionInfo [pal.size()]);
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
    cps.remove(name);
  }


  /**
   * Returns the name of this Conditional Permission Info.
   * 
   * @return The name of this Conditional Permission Info.
   */
  public String getName() {
    return name;
  }


  /**
   * Returns a string representation of this object.
   * 
   */
  public String toString() {
    StringBuffer res = PermUtil.quote(name, null);
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
    return res.toString();
  }

  //
  // Package methods
  //

  static final Class[] argClasses = new Class[] {Bundle.class, ConditionInfo.class};

  ConditionalPermission getConditionalPermission(Bundle bundle) {
    Condition[] conds = new Condition[conditionInfos.length];
    for (int i = 0; i < conditionInfos.length; i++) {
      Class clazz;
      try {
	clazz = Class.forName(conditionInfos[i].getType());
	Constructor cons = null;
	Method method = null;
	try {
	  method = clazz.getMethod("getCondition", argClasses);
	  if ((method.getModifiers() & Modifier.STATIC) == 0) {
	    method = null;
	  }
	} catch (NoSuchMethodException ignore) { }
	try {
	  cons = clazz.getConstructor(argClasses);
	} catch (NoSuchMethodException ignore) { }
	if (method != null && cons == null) {
	  conds[i] = (Condition) method.invoke(null, new Object [] {bundle, conditionInfos[i]});
	} else if (method == null && cons != null) {
	  conds[i] = (Condition) cons.newInstance(new Object [] {bundle, conditionInfos[i]});
	} else {
	  if (method == null && cons == null) {
	    // NYI! Log this
	  } else {
	    // NYI! Log this
	  }
	  conds[i] = Condition.FALSE;
	}
      } catch (Throwable ignore) {
	// NYI! Log this
	conds[i] = Condition.FALSE;
      }
    }
    return new ConditionalPermission(conds, getPermissions());
  }

  PermissionCollection getPermissions() {
    if (permissions == null) {
      permissions = PermUtil.makePermissionCollection(permissionInfos, null);
    }
    return permissions;
  }

}
