/*
 * Copyright (c) 2012-2022, KNOPFLERFISH project
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

import java.util.Dictionary;

import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;


class ComponentFactoryImpl<S> implements ComponentFactory<S>
{
  private FactoryComponent factoryComponent;

  ComponentFactoryImpl(FactoryComponent fc) {
    factoryComponent = fc;
  }

  void deactivate() {
    factoryComponent = null;
  }

  /**
   *
   */
  public ComponentInstance<S> newInstance(@SuppressWarnings("rawtypes") Dictionary instanceProps) {
    final FactoryComponent fc = factoryComponent;
    if (fc != null) {
      @SuppressWarnings("unchecked")
      final Dictionary<String, Object> ip = instanceProps;
      //noinspection unchecked
      return (ComponentInstance<S>) fc.newInstance(ip);
    }
    throw new ComponentException("Component factory has been disposed");
  }

}
