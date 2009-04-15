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
import org.osgi.framework.AdminPermission;


class PostponementCheck implements PrivilegedAction {

  private AccessControlContext acc;
  private Permission perm;
  private ArrayList checkedClasses;
  private ArrayList ppList;

  private int [] perms;
  private int [] curr;


  PostponementCheck(AccessControlContext acc, Permission perm, PostponementCheck previous) {
    this.acc = acc;
    this.perm = perm;
    checkedClasses = previous != null ? previous.getCheckedClasses() : null;
    ppList = null;
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
  public void savePostponement(List postponement) {
    if (ppList == null) {
      ppList = new ArrayList(2);
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
  private void initPermutations() {
    int depth = ppList.size();
    perms = new int[depth];
    curr = new int[depth];
    for (int i = 0; i < depth; i++) {
      perms[i] = ((List)ppList.get(i)).size();
      curr[i] = 0;
    }
  }


  /**
   * This code became ugly and needs to be rewritten and optimized.
   */
  private Map getNextPermutation() {
    if (curr != null) {
      HashMap res = new HashMap();
      boolean increment = true;
      for (int i = 0; i < curr.length; i++) {
        ConditionalPermission cp = (ConditionalPermission)((List)ppList.get(i)).get(curr[i]);
        for (Iterator pi = cp.getPostponed(); pi.hasNext();) {
          Object o = pi.next();
          Class co = o.getClass();
          ArrayList x = (ArrayList)res.get(co);
          if (x == null) {
            x = new ArrayList(2);
          }
          x.add(0, o);
          x.add(cp);
          res.put(co, x);
        }
        if (increment) {
          if (++curr[i] == perms[i]) {
            curr[i] = 0;
          } else {
            increment = false;
          }
        }
      }
      if (increment) {
        curr = null;
      }
      return res;
    }
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
      initPermutations();
      int permcount = 1;
      while ((cm = getNextPermutation()) != null) {
        boolean ok = true;
        for (Iterator i = cm.entrySet().iterator(); i.hasNext();) {
          Map.Entry me = (Map.Entry)i.next();
          Class cc = (Class)me.getKey();
          if (checkedClasses.contains(cc)) {
            ok = false;
            break;
          }
          ArrayList cs = (ArrayList)me.getValue();
          Dictionary d = (Dictionary)condDict.get(cc);
          if (d == null) {
            d = new Hashtable();
            condDict.put(cc, d);
          }
          checkedClasses.add(cc);
          try {
            int size = cs.size() / 2;
            Condition [] conditions = new Condition[size];
            ConditionalPermission [] immutables = new ConditionalPermission[size];
            for (int j = 0; j < size; j++) {
              Condition ce = (Condition)cs.get(size - j - 1);
              conditions[j] = ce;
              immutables[j] = ce.isMutable() ? null : (ConditionalPermission)cs.get(size + j);
            }
            ok = conditions[0].isSatisfied(conditions, d);
            for (int j = 0; j < size; j++) {
              if (immutables[j] != null) {
                immutables[j].setImmutable(conditions[j], ok);
              }
            }
          } catch (Throwable t) {
            // NYI, Log this
            ok = false;
          }
          checkedClasses.remove(checkedClasses.size() - 1);
          if (!ok) {
            break;
          }
        }
        if (ok) {
          if (Debug.permissions) {
            Debug.println("CHECK_POSTPONE: postponement ok");
          }
          return;
        }
      }
      if (Debug.permissions) {
        Debug.println("CHECK_POSTPONE: postponement failed");
      }
      throw new SecurityException("Postponed conditions failed");
    }
  }

}
