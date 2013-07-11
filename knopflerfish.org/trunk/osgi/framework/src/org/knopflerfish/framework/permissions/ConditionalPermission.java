/*
 * Copyright (c) 2008-2013, KNOPFLERFISH project
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

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.service.condpermadmin.Condition;


/**
 * A binding of a set of Conditions to a set of Permissions.
 *
 */
public class ConditionalPermission
{
  private final ConditionalPermissionInfoImpl parent;
  private Condition [] conditions;
  final private PermissionCollection permissions;
  private List<Condition> postponed = null;

  final String access;

  /**
   */
  ConditionalPermission(Condition [] conds, PermissionCollection perms, String access,
                        ConditionalPermissionInfoImpl cpi) {
    conditions = conds;
    permissions = perms;
    this.access = access;
    parent = cpi;
  }


  /**
   * Check immediate conditions and if asked save postponed conditions.
   * If conditions are fulfilled check permission is ok.
   *
   */
  boolean checkImmediateOk(Permission perm, boolean checkPostponed) {
    if (conditions == null) {
      return false;
    }
    postponed = new ArrayList<Condition>(1);
    for (int i = 0; i < conditions.length; i++) {
      final Condition c = conditions[i];
      if (c == null) {
        // Immutable condition has been removed
        continue;
      }
      if (checkPostponed || !c.isPostponed()) {
        final boolean mutable = c.isMutable();
        if (c.isSatisfied()) {
          if (!mutable) {
            // Mark always ok by clearing condition element.
            conditions[i] = null;
          }
        } else {
          if (!mutable) {
            // Mark always fail by clearing conditions.
            conditions = null;
          }
          postponed = null;
          return false;
        }
      } else {
        postponed.add(c);
      }
    }
    return permissions.implies(perm);
  }


  /**
   * Check postponed conditions are OK.
   *
   */
  boolean checkPostponedOk(Map<Class<? extends Condition>, Dictionary<Object, Object>> condDict,
                           List<Class<? extends Condition>> checkedClasses)
  {
    if (conditions == null) {
      return false;
    }
    // Loop through all postponed Conditions for ConditionalPermissions
    for (final Condition c : postponed) {
      final Class<? extends Condition> cc = c.getClass();
      if (checkedClasses.contains(cc)) {
        throw new SecurityException("Postponement condition check recursive for class: " + cc);
      }
      Dictionary<Object, Object> d = condDict.get(cc);
      if (d == null) {
        d = new Hashtable<Object, Object>();
        condDict.put(cc, d);
      }
      checkedClasses.add(cc);
      try {
        final boolean m = c.isMutable();
        if (c.isSatisfied(new Condition [] {c}, d)) {
          if (!m) {
            for (int i = 0; i < conditions.length; i++ ) {
              if (conditions[i] == c) {
                conditions[i] = null;
                break;
              }
            }
          }
        } else {
          if (!m) {
            conditions = null;
          }
          // ConditionPermissional didn't match
          return false;
        }
      } catch (final Throwable ignore) {
        // NYI, Log this failure
        // ConditionPermissional couldn't be evaluated, treat as fail
        // TBD should we do this even if we have a DENY CondPerm.
        return false;
      } finally {
        checkedClasses.remove(checkedClasses.size() - 1);
      }
    }
    return true;
  }


  /**
   * Check if we have saved any postponements in last
   * checkImmediateOk call.
   */
  boolean hasPostponed() {
    return !postponed.isEmpty();
  }


  /**
   *
   */
  boolean isParent(ConditionalPermissionInfoImpl cpi) {
    return cpi == parent;
  }


  /**
   *
   */
  @Override
  public String toString() {
    return "HASH: " + hashCode() + " INFO: " + parent.toString();
  }

}
