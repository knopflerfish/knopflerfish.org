/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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
package org.knopflerfish.bundle.componentF_test;

import java.util.Map;

import org.knopflerfish.service.componentF_test.ComponentX;
import org.knopflerfish.service.componentF_test.ComponentY;
import org.knopflerfish.service.component_test.TestService;
import org.osgi.service.component.ComponentContext;

public class ComponentXImpl
  implements ComponentX
{
  private int yStatus = 0;
  private ComponentY y;
  private int base = 0;

  void activate(Map props)
  {
    base = ((Integer)props.get("base")).intValue();
    System.out.println("XImpl: activate");
  }

  void deactivate(ComponentContext cc)
  {
    System.out.println("XImpl: deactivate");
  }

  public void setY(ComponentY y)
  {
    this.y = y;
    yStatus += 1;
    System.out.println("XImpl: binding Y, " +y);
  }
  public void unsetY(ComponentY y)
  {
    this.y = null;
    yStatus += 1000;
    System.out.println("XImpl: unbinding Y, " +y);
  }

  public void setTest(TestService t)
  {
    y.testCall(base);
    System.out.println("XImpl: binding TestService, " +t);
  }
  public void unsetTest(TestService t)
  {
    y.testCall(1000 * base);
    System.out.println("XImpl: unbinding TestService, " +t);
  }

  public int getBindYStatus()
  {
    return this.yStatus;
  }

}
