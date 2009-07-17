/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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

import org.knopflerfish.client.axis2_soapobject.*;

public class MySoapTestServiceClient_ADB {

  //  private static final String url
  //    = "http://localhost:8080/axis2/services/MySoapTestService";

  private MySoapTestServiceStub stub = null;

  public MySoapTestServiceClient_ADB()
  {
    this("8080");
  }

  public MySoapTestServiceClient_ADB(final String port)
  {
    try {
      final String targetEndpoint
        = "http://127.0.0.1:" +port +"/axis2/services/MySoapTestService";
      stub = new MySoapTestServiceStub(targetEndpoint);
    } catch (Exception e) {
      System.err.println("stub creation failed: " +e.getMessage());
      e.printStackTrace();
    }
  }

  // This client assumes that the stub was generated using the option
  // "-uw" for unwrapp parameters. The commented out code below is
  // what you need to type if parameter unwrapping is not used.
  public String getName()
  {
    try {
      //MySoapTestServiceStub.GetNameResponse res = stub.getName();
      //return res.get_return();
      return stub.getName();
    } catch (Exception e) {
      System.err.println("getName() failed: " +e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  public int add(int a, int b)
  {
    //MySoapTestServiceStub.Add req = new MySoapTestServiceStub.Add();
    //req.setParam0(a);
    //req.setParam1(b);

    try {
      //MySoapTestServiceStub.AddResponse res = stub.add(req);
      //return res.get_return();
      return stub.add(a,b);
    } catch (Exception e) {
      System.err.println("add("+a+","+b+") failed: " +e.getMessage());
      e.printStackTrace();
    }
    return 0;
  }

  public int[] addToAll(int a, int b[])
  {
    //MySoapTestServiceStub.AddToAll req = new MySoapTestServiceStub.AddToAll();
    //req.setParam0(a);
    //req.setParam1(b);

    try {
      //MySoapTestServiceStub.AddToAllResponse res = stub.addToAll(req);
      //return res.get_return();
      return stub.addToAll(a,b);
    } catch (Exception e) {
      System.err.println("addToAll("+a+","+b+") failed: " +e.getMessage());
      e.printStackTrace();
    }
    return new int[0];
  }

  public double mul(double x, double y)
  {
    //MySoapTestServiceStub.Mul req = new MySoapTestServiceStub.Mul();
    //req.setParam0(x);
    //req.setParam1(y);

    try {
      //MySoapTestServiceStub.MulResponse res = stub.mul(req);
      //return res.get_return();
      return stub.mul(x,y);
    } catch (Exception e) {
      System.err.println("mul("+x+","+y+") failed: " +e.getMessage());
      e.printStackTrace();
    }
    return 0.0;
  }

  public MySoapTestServiceStub.MyJavaBean getBean()
  {
    try {
      //MySoapTestServiceStub.GetBeanResponse res = stub.getBean();
      //return res.get_return();
      return stub.getBean();
    } catch (Exception e) {
      System.err.println("getBean() failed: " +e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  public void setBean(MySoapTestServiceStub.MyJavaBean bean)
  {
    try {
      //MySoapTestServiceStub.SetBean req = new MySoapTestServiceStub.SetBean();
      //req.setParam0(bean);

      //stub.setBean(req);
      stub.setBean(bean);
    } catch (Exception e) {
      System.err.println("setBean() failed: " +e.getMessage());
      e.printStackTrace();
    }
  }

}
