/*
 * Copyright (c) 2018, KNOPFLERFISH project
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
package org.knopflerfish.bundle.componentFilter_test;

import org.knopflerfish.service.componentFilter_test.GeneralComponent;

import org.osgi.service.component.ComponentContext;

public class GeneralComponentImplB
  implements GeneralComponent
{

  private ComponentContext cc;
  private GeneralComponent genComp;
  
  public void activate(ComponentContext cc)
  {
    this.cc = cc;
    System.out.println("GeneralComponentImplB: activate");
  }

  public void deactivate(ComponentContext cc)
  {
    this.cc = null;
    System.out.println("GeneralComponentImplB: deactivate");
  }

  public void action() {
    System.out.println("GeneralComponentImplB: action()");
  }

  public void setGeneralComponentImplA(GeneralComponent genComp) {
    System.out.println("GeneralComponentImplB: setting GeneralCompoennt");
    this.genComp = genComp;
    System.out.println("GeneralComponentImplB: calling action() on generalComponent");
    genComp.action();
  }
  
}
