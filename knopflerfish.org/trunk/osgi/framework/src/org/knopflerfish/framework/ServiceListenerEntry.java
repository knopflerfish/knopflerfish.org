/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.util.List;
import java.util.EventListener;

import org.osgi.framework.*;
import org.osgi.framework.hooks.service.ListenerHook;

/**
 * Data structure for saving service listener info. Contains
 * the optional service listener filter, in addition to the info 
 * in ListenerEntry.
 */
class ServiceListenerEntry extends ListenerEntry implements ListenerHook.ListenerInfo {
  LDAPExpr ldap;

  /**
   * The elements of "simple" filters are cached, for easy lookup.
   * 
   * The grammar for simple filters is as follows:
   *
   * <pre>
   * Simple = '(' attr '=' value ')'
   *        | '(' '|' Simple+ ')'
   * </pre>
   * where <code>attr</code> is one of {@link Constants#OBJECTCLASS}, 
   * {@link Constants#SERVICE_ID} or {@link Constants#SERVICE_PID}, and
   * <code>value</code> must not contain a wildcard character.
   * <p>
   * The index of the vector determines which key the cache is for
   * (see {@link ServiceListenerState#hashedKeys}). For each key, there is
   * a vector pointing out the values which are accepted by this
   * ServiceListenerEntry's filter. This cache is maintained to make
   * it easy to remove this service listener.
   */
  List[] local_cache;

  ServiceListenerEntry(BundleContextImpl bc, EventListener l, String filter) 
    throws InvalidSyntaxException {
    super(bc, l);
    if (filter != null) {
      ldap = new LDAPExpr(filter);
    } else {
      ldap = null;
    }
  }

  ServiceListenerEntry(BundleContextImpl bc, EventListener l) {
    super(bc, l);
    ldap = null;
  }

  public BundleContext getBundleContext() {
    return bc;
  }
  
  public String getFilter() {
    return ldap != null ? ldap.toString() : null;
  }
}
