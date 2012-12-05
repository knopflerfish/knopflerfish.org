/*
 * Copyright (c) 2012-2012, KNOPFLERFISH project
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
package org.knopflerfish.bundle.componentX_test;

import org.knopflerfish.service.componentX_test.ComponentX;
import org.knopflerfish.service.componentX_test.ComponentZ;

import org.osgi.service.component.ComponentContext;

public class ComponentZImpl
  implements ComponentZ
{
  private Integer xStatus = new Integer(0);
  private ComponentX x;
  private ComponentContext cc = null;

  void activate(ComponentContext cc)
  {
    this.cc = cc;
    System.out.println("ZImpl: activate");
  }

  void deactivate(ComponentContext cc)
  {
    this.cc = null;
    System.out.println("ZImpl: deactivate");
  }

  void setX(ComponentX x)
  {
    this.x = x;
    xStatus = new Integer(xStatus.intValue() + 1);
    System.out.println("ZImpl: binding X, " +x);
  }

  void unsetX(ComponentX x)
  {
    this.x = null;
    xStatus = new Integer(xStatus.intValue() + 100);
    System.out.println("ZImpl: unbinding X, " +x);
  }

  public Integer getBindStatus()
  {
    return this.xStatus;
  }

  public Integer getBindXStatus()
  {
    return x != null ? x.getBindStatus() : null;
  }

  public void disableZ()
  {
    if (cc != null) {
      System.out.println("ZImpl: disable!");
      cc.disableComponent("componentX_test.Z");
    } else {
      System.out.println("ZImpl: no disable");
    }
  }

}
