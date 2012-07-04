/*
 * Copyright (c) 2011-2011, KNOPFLERFISH project
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

import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;

import org.apache.felix.scr.Component;

public class ScrServiceImpl implements org.apache.felix.scr.ScrService
{

  final SCR scr;

  ScrServiceImpl(final SCR scr) {
    this.scr = scr;
  }

  // ScrService interface

  /**
   * @see
   */
  public Component[] getComponents() {
    List all = scr.getAllComponents();
    if (all.size() > 0) {
      return (Component[])all.toArray(new Component[all.size()]);
    }
    return null;
  }


  /**
   * @see
   */
  public Component getComponent(final long componentId) {
    List all = scr.getAllComponents();
    for (Iterator i = all.iterator(); i.hasNext(); ) {
      Component c = (Component)i.next();
      if (c.getId() == componentId) {
        return c;
      }
    }
    return null;
  }


  /**
   * @see
   */
  public Component[] getComponents(final String componentName) {
    Component[] res = scr.getComponent(componentName);
    if (res != null) {
      res = (Component[])res.clone();
    }
    return res;
  }


  /**
   * @see
   */
  public Component[] getComponents(final Bundle bundle) {
    Component[] res = scr.getComponents(bundle);
    if (res != null) {
      res = (Component[])res.clone();
    }
    return res;
  }

}
