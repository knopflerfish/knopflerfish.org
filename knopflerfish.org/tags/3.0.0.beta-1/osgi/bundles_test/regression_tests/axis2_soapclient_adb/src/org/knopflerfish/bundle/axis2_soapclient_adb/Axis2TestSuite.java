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

import org.knopflerfish.client.axis2_soapobject.MySoapTestServiceStub.MyJavaBean;

import org.osgi.framework.*;
import junit.framework.*;

public class Axis2TestSuite extends TestSuite {
  Bundle bu;
  BundleContext bc;
  MySoapTestServiceClient_ADB client;

  // Get port of the default HttpService.
  private static String getHttpServerPort(BundleContext bc)
  {
    String port = "8080";

    final ServiceReference httpSR
      = bc.getServiceReference("org.osgi.service.http.HttpService");
    if (null!=httpSR) {
      Object obj = httpSR.getProperty("port.http");
      if(obj == null) {
        obj = httpSR.getProperty("openPort");
      }
      if (null!=obj) {
        port = obj.toString();
      }
    }
    return port;
  }

  public Axis2TestSuite(BundleContext bc) {
    super("SOAP-Axis2TestSuite");
    this.bc     = bc;
    this.bu     = bc.getBundle();
    this.client = new MySoapTestServiceClient_ADB(getHttpServerPort(bc));

    addTest(new GetName());
    addTest(new Add());
    addTest(new Mul());
    addTest(new AddToAll());
    addTest(new Bean1());
    addTest(new Bean2());
  }



  class GetName extends TestCase
  {
    public String getName(){
      return "MySoapTestService:GetName";
    }
    public String getDescription()
    {
      return "Calls an argumentless Web Service method returning a String.";
    }
    public void runTest()
      throws Throwable
    {
      assertEquals("MySoapTestService", client.getName() );
    }
  }


  class Add extends TestCase
  {
    public String getName(){
      return "MySoapTestService:add(1,2)";
    }
    public String getDescription()
    {
      return "Calls a Web Service operation with two int params "
        +"that returns an int.";
    }
    public void runTest()
      throws Throwable
    {
      assertEquals(3, client.add(1,2) );
    }
  }

  class Mul extends TestCase
  {
    public String getName(){
      return "MySoapTestService:mul(1.0,2.0)";
    }
    public String getDescription()
    {
      return "Calls a Web Service operation with two double params "
        +"that returns a double.";
    }
    public void runTest()
      throws Throwable
    {
      double res = 2.0 - client.mul(1.0,2.0);

      assertTrue("-1e-8 <=" +res, -1e-8 <= res );
      assertTrue(""+ res+" <= 1e-8",  res <= 1e-8 );
    }
  }

  class AddToAll extends TestCase
  {
    public String getName(){
      return "MySoapTestService:addToAll(1,[2,3,4])";
    }
    public String getDescription()
    {
      return "Calls a Web Service operation with one int param "
        +"and an int-array param that returns an int-array.";
    }
    public void runTest()
      throws Throwable
    {
      int[] res = client.addToAll(1,new int[]{2,3,4});
      assertNotNull(res);

      int[] answer = new int[]{3,4,5};
      assertEquals(res.length,answer.length);

      for (int i=0; i<res.length; i++) {
        assertEquals("res["+i+"]", answer[i], res[i] );
      }
    }
  }

  class Bean1 extends TestCase
  {
    public String getName(){
      return "MySoapTestService:setBean(null)";
    }
    public String getDescription()
    {
      return "Calls a Web Service operation with one Java Bean param "
        +"that returns nothing.";
    }
    public void runTest()
      throws Throwable
    {
      client.setBean(null);
      assertNull(client.getBean());
    }
  }


  class Bean2 extends TestCase
  {
    public String getName(){
      return "MySoapTestService:setBean(MyJavaBean)";
    }
    public String getDescription()
    {
      return "Calls a Web Service operation with one Java Bean param "
        +"that returns nothing. Then calls another method that should "
        +"return an equivalent (identical) object.";
    }
    public void runTest()
      throws Throwable
    {
      MyJavaBean bean1 = new MyJavaBean();
      bean1.setName("Jim");
      bean1.setValue(33333333);
      client.setBean(bean1);

      MyJavaBean bean2 = client.getBean();
      assertNotNull(bean2);
      assertEquals(bean1.getName(),bean2.getName());
      assertEquals(bean1.getValue(),bean2.getValue());

      client.setBean(null);
    }
  }

}
