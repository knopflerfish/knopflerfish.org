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

import java.security.*;

import org.osgi.service.condpermadmin.*;

/**
 * A binding of a set of Conditions to a set of Permissions.
 * 
 * @version $Revision: 1.11 $
 */
public class ConditionalPermission
{
  final static int FAILED = 0;
  final static int IMPLIED = 1;
  final static int POSTPONED = 2;

  final private Condition [] conditions;
  final private PermissionCollection permissions;


  /**
   */
  ConditionalPermission(Condition [] conds, PermissionCollection perms) {
    conditions = conds;
    permissions = perms;
  }


  /**
   *
   */
  int check(Permission perm) {
    for (int i = 0; i < conditions.length; i++) {
      Condition c = conditions[i];
      if (c == null) {
	// Immutable condition has been removed
	continue;
      }
      if (!c.isPostponed()) {
	if (c.isSatisfied()) {
	  if (!c.isMutable()) {
	    conditions[i] = null;
	  }
	} else {
	  if (!c.isMutable()) {
	    // NYI! Save failed
	  }
	  return FAILED;
	}
      } else {
	throw new RuntimeException("NYI! Handle postponed");
      }
    }
    return IMPLIED;
  }


  /**
   *
   */
  PermissionCollection getPermissions() {
    return permissions;
  }

}
