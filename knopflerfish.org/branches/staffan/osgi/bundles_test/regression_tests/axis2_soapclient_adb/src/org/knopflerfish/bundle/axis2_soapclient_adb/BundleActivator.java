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

package org.knopflerfish.bundle.axis2_soapclient_adb;

import org.knopflerfish.client.axis2_soapobject.MySoapTestServiceStub.MyJavaBean;

import org.osgi.framework.*;
import junit.framework.*;

import java.util.Hashtable;


public class BundleActivator implements org.osgi.framework.BundleActivator {
  BundleContext bc;

  public void start(BundleContext bc) {
    this.bc = bc;

    TestSuite suite = new Axis2TestSuite(bc);
    Hashtable props = new Hashtable();
    props.put("service.pid", suite.getName());
    ServiceRegistration sr
      = bc.registerService(TestSuite.class.getName(), suite, props);
  }

  public void stop(BundleContext bc) {
  }

  public static void main(String[] args)
    throws Exception
  {
    MySoapTestServiceClient_ADB client = new MySoapTestServiceClient_ADB();
    System.out.println("getName():      " +client.getName());
    System.out.println("1   + 2       = " +client.add(1,2));
    System.out.println("1.0 * 2.0     = " +client.mul(1.0,2.0));
    int[] res = client.addToAll(1,new int[]{2,3,4});
    System.out.println("1   + [2,3,4] = " +formatIntArray(res));
    MyJavaBean bean1 = client.getBean();
    System.out.println("getBean():            "+bean1);
    bean1 = new MyJavaBean();
    bean1.setName("Jim");
    bean1.setValue(33333333);
    client.setBean(bean1);
    MyJavaBean bean2 = client.getBean();
    System.out.println("getBean():            "+bean2);
    System.out.println("getBean().getName():  "+bean2.getName());
    System.out.println("getBean().getValue(): "+bean2.getValue());
    client.setBean(null);
  }

  private static String formatIntArray(int[] ints)
  {
    if (null==ints) return "null";

    StringBuffer sb = new StringBuffer("[");
    for (int i=0; i<ints.length; i++) {
      if (i>0) sb.append(",");
      sb.append(String.valueOf(ints[i]));
    }
    sb.append("]");
    return sb.toString();
  }

}
