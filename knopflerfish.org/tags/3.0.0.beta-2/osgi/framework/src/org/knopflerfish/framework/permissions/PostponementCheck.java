/*
 * Copyright (c) 2009, KNOPFLERFISH project
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

import java.security.*;
import java.util.*;

import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.framework.AdminPermission;

import org.knopflerfish.framework.Debug;


class PostponementCheck implements PrivilegedAction {

  private AccessControlContext acc;
  private Permission perm;
  private ArrayList checkedClasses;
  private ArrayList ppList = null;

  private Debug debug = null;

  /**
   *
   */
  PostponementCheck(AccessControlContext acc, Permission perm, PostponementCheck previous) {
    this.acc = acc;
    this.perm = perm;
    checkedClasses = previous != null ? previous.getCheckedClasses() : null;
  }


  /**
   *
   */
  ArrayList getCheckedClasses() {
    if (checkedClasses != null) {
      return (ArrayList)checkedClasses.clone();
    }
    return null;
  }


  /**
   *
   */
  public void savePostponement(List postponement, Object debug) {
    if (ppList == null) {
      ppList = new ArrayList(2);
      this.debug = (Debug)debug;
    }
    ppList.add(postponement);
  }


  /**
   *
   */
  public Object run() {
    acc.checkPermission(perm);
    checkPostponements();
    return null;
  }


  /**
   *
   */
  private void checkPostponements() {
    if (ppList != null) {
      HashMap condDict = new HashMap();
      Map cm;
      if (checkedClasses == null) {
        checkedClasses = new ArrayList();
      }
      // Loop through all bundle protection domains found on stack
    pdloop:
      for (Iterator ppi = ppList.iterator(); ppi.hasNext(); ) {
        // Loop through all matching ConditionalPermissions
      cploop:
        for (Iterator pi = ((List)ppi.next()).iterator(); pi.hasNext(); ) {
          ConditionalPermission cp = (ConditionalPermission)pi.next();
          // Loop through all postponed Conditions for ConditionalPermissions
          // List ends with an immediate conditions, if not assume an implicity
          // DENY ConditionalPermission.
          for (Iterator ci = cp.getPostponed(); ci.hasNext();) {
            Condition c = (Condition)ci.next();
            if (c.isPostponed()) {
              Class cc = c.getClass();
              if (checkedClasses.contains(cc)) {
                if (debug.permissions) {
                  debug.println("CHECK_POSTPONE: recursive postponement, class=" + cc);
                }
                throw new SecurityException("Postponement condition check recursive for class: " + cc);
              }
              Dictionary d = (Dictionary)condDict.get(cc);
              if (d == null) {
                d = new Hashtable();
                condDict.put(cc, d);
              }
              checkedClasses.add(cc);
              try {
                // NYI! Optimize immutables!
                if (!c.isSatisfied(new Condition [] {c}, d)) {
                  // ConditionPermissional didn't match, check next
                  continue cploop;
                }
              } catch (Throwable ignore) {
                // NYI, Log this failure
                // ConditionPermissional couldn't be evaluated, treat as fail
                // TBD should we do this even if we have a DENY CondPerm.
                continue cploop;
              } finally {
                checkedClasses.remove(checkedClasses.size() - 1);
              }
            } else if (cp.access == ConditionalPermissionInfo.ALLOW) {
              // We allow, check next protection domain
              continue pdloop;
            }
          }
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
