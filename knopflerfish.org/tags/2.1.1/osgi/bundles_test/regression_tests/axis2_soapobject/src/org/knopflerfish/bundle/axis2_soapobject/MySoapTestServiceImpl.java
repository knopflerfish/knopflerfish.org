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

package org.knopflerfish.bundle.axis2_soapobject;

import org.knopflerfish.service.axis2_soapobject.*;
import org.knopflerfish.service.axis2.*;
import org.osgi.framework.*;

import java.util.Hashtable;


public class MySoapTestServiceImpl implements MySoapTestService {
  BundleContext bc;
  private static final String NAME = "MySoapTestService";

  public MySoapTestServiceImpl(BundleContext bc)
  {
    this.bc = bc;

    Hashtable ht = new Hashtable();
    ht.put(Axis2Admin.SOAP_SERVICE_NAME, NAME);

    bc.registerService(MySoapTestService.class.getName(), this, ht);
  }

  public String getName()
  {
    return NAME;
  }

  public int add(int a, int b)
  {
    return a+b;
  }

  public int[] addToAll(int a, int[] b)
  {
    int[] res = new int[b.length];
    for (int i=0; i<b.length; i++) res[i] = a +b[i];

    return res;
  }

  public double mul(double x, double y)
  {
    return x*y;
  }

  private MyJavaBean myBean;
  public MyJavaBean getBean()
  {
    return myBean;
  }
  public void setBean(MyJavaBean bean)
  {
    this.myBean = bean;
  }


}
