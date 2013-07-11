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

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;

import org.knopflerfish.framework.Debug;


class PostponementCheck<T> implements PrivilegedAction<T> {

  private final AccessControlContext acc;
  private final Permission perm;
  private ArrayList<Class<? extends Condition>> checkedClasses;
  private ArrayList<List<ConditionalPermission>> ppList = null;

  private Debug debug = null;

  /**
   *
   */
  PostponementCheck(AccessControlContext acc, Permission perm, PostponementCheck<?> previous) {
    this.acc = acc;
    this.perm = perm;
    checkedClasses = previous != null ? previous.getCheckedClasses() : null;
  }


  /**
   *
   */
  ArrayList<Class<? extends Condition>> getCheckedClasses() {
    if (checkedClasses != null) {
      @SuppressWarnings("unchecked")
      final ArrayList<Class<? extends Condition>> res
        = (ArrayList<Class<? extends Condition>>) checkedClasses.clone();
      return res;
    }
    return null;
  }


  /**
   *
   */
  public void savePostponement(List<ConditionalPermission> postponement, Object debug) {
    if (ppList == null) {
      ppList = new ArrayList<List<ConditionalPermission>>(2);
      this.debug = (Debug)debug;
    }
    ppList.add(postponement);
  }


  /**
   *
   */
  public T run() {
    acc.checkPermission(perm);
    checkPostponements();
    return null;
  }


  /**
   *
   */
  private void checkPostponements() {
    if (ppList != null) {
      final HashMap<Class<? extends Condition>, Dictionary<Object,Object>> condDict
        = new HashMap<Class<? extends Condition>, Dictionary<Object,Object>>();
      if (checkedClasses == null) {
        checkedClasses = new ArrayList<Class<? extends Condition>>();
      }
      // Loop through all bundle protection domains found on stack
      for (final List<?> list : ppList) {
        // Loop through all matching ConditionalPermissions
        boolean deny = true;
        for (final Object name : list) {
          final ConditionalPermission cp = (ConditionalPermission)name;
          if (!cp.checkPostponedOk(condDict, checkedClasses)) {
            // ConditionPermissional didn't match, check next
            continue;
          }
          if (cp.access == ConditionalPermissionInfo.ALLOW) {
            // We allow, check next protection domain
            deny = false;
          }
          break;
        }
        if (deny) {
          // Denied
          if (debug.permissions) {
            debug.println("CHECK_POSTPONE: postponement failed");
          }
          throw new SecurityException("Postponed conditions failed");
        }
      }
      if (debug.permissions) {
        debug.println("CHECK_POSTPONE: postponement ok");
      }
    }
  }

}
