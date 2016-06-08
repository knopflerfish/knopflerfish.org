/*
 * Copyright (c) 2016-2016, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;


public class FactoryComponentConfiguration extends ComponentConfiguration
{
  private final Hashtable<Bundle, ComponentContextImpl> factoryContexts =
      new Hashtable<Bundle, ComponentContextImpl>();

  
  FactoryComponentConfiguration(Component c,
                                String ccId,
                                Map<String, Object> cmDict)
  {
    super(c, ccId, cmDict, null);
    Activator.logDebug("Created factory " + toString());
  }


  @Override
  void addContext(ComponentContextImpl cci) {
    factoryContexts.put(cci.getUsingBundle(), cci);
  }


  @Override
  void removeContext(ComponentContextImpl cci) {
    factoryContexts.remove(cci.getUsingBundle());
  }


  @Override
  boolean containsContext(ComponentContextImpl cci) {
    return factoryContexts.containsValue(cci);
  }


  int noContext() {
    return factoryContexts.size();
  }


  @Override
  ComponentContextImpl getContext(Bundle b, Object instance) {
    return factoryContexts.get(b);
  }


  /**
   */
  ComponentContextImpl [] getAllContexts() {
    synchronized (factoryContexts) {
      ComponentContextImpl [] res = new ComponentContextImpl[factoryContexts.size()];
      factoryContexts.values().toArray(res);
      return res;
    }
  } 

}
