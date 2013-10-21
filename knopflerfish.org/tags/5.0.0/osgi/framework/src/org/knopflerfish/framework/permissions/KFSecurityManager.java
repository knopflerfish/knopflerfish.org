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
import java.security.AccessController;
import java.security.Permission;
import java.util.List;

import org.knopflerfish.framework.Debug;
import org.knopflerfish.framework.FrameworkContext;


public class KFSecurityManager extends SecurityManager
  implements ConditionalPermissionSecurityManager {

  private final ThreadLocal<PostponementCheck<?>> postponementCheck
    = new ThreadLocal<PostponementCheck<?>>();

  @SuppressWarnings("unused")
  private final Debug debug;

  public KFSecurityManager(FrameworkContext fwCtx) {
    this.debug = fwCtx.debug;
  }

  /**
   *
   */
  @Override
  public void checkPermission(Permission perm, Object context) {
    if (!(context instanceof AccessControlContext)) {
      throw new SecurityException("Context not an AccessControlContext");
    }
    final PostponementCheck<?> old = postponementCheck.get();
    final PostponementCheck<?> pc
      = new PostponementCheck<Object>((AccessControlContext) context, perm, old);
    postponementCheck.set(pc);
    try {
      AccessController.doPrivileged(pc);
    } finally {
      postponementCheck.set(old);
    }
  }


  /**
   *
   */
  @Override
  public void checkPermission(Permission perm) {
    checkPermission(perm, getSecurityContext());
  }


  /**
   * Is it possible to do postponement checks.
   * This is only possible if a checkPermission via
   * a ConditionalPermissionSecurityManager is in progress.
   *
   * @return true is postponement is available, otherwise false
   */
  public boolean isPostponeAvailable() {
    return postponementCheck.get() != null;
  }


  /**
   * NYI! Think about security here!
   */
  public void savePostponement(List<ConditionalPermission> postponement,
                               Object debug)
  {
    final PostponementCheck<?> pc = postponementCheck.get();
    pc.savePostponement(postponement, debug);
  }

}
