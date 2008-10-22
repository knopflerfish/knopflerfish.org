/*
 * Copyright (c) 2006-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.component_test;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.component.*;

import junit.framework.*;

import org.knopflerfish.service.component_test.*;

public class ComponentTestSuite extends TestSuite implements ComponentATest
{
  private BundleContext bc;

  private int counter = 0;

  public ComponentTestSuite(BundleContext bc) {
    super("ComponentTestSuite");
    this.bc = bc;
    addTest(new Test1());
    addTest(new Test2());
    addTest(new Test3());
  }

  public void bump() {counter++;}

  /* from http_test */
  class FWTestCase extends TestCase {
    public String getName() {
      String name = getClass().getName();
      int ix = name.lastIndexOf("$");
      if(ix == -1) {
        ix = name.lastIndexOf(".");
      }
      if(ix != -1) {
        name = name.substring(ix + 1);
      }
      return name;
    }
  }

  private class Test1 extends FWTestCase {
    /**
     * This checks that componenents are lazily instanciated, and
     * eagerily removed.
     * How it works:
     * - test case is started: the bundles componentA is installed.
     * - ComponentA1 is registered the service ComponentServiceA1
     * - The test then gets the service reference (this should not
     *   activate the component)
     * - The test then gets the serivce object (this should activate
     *   the component)
     * - When the component is activate the bump method is called.
     * - Then the test ungets the service, this should deactivate the component.
     * - When the component is deactivated the bump method is called.
     */

    public void runTest() {
      try {
        counter = 0;

         Bundle c1 = Util.installBundle(bc, "componentA_test-1.0.1.jar");
         c1.start();

         ServiceReference ref = bc.getServiceReference
           ("org.knopflerfish.bundle.componentA_test.ComponentA1");

         assertNotNull("Could not get service reference for A1", ref);
         assertEquals("Should not have been bumped", 0, counter);

         // Check that the component name is available as service property
         String compName
           = (String) ref.getProperty(ComponentConstants.COMPONENT_NAME);
         System.out.println("component.name= " +compName);
         assertEquals("component name valid", "componentA1.test", compName);

         // Check that the component id is available as service property
         Long compId = (Long) ref.getProperty(ComponentConstants.COMPONENT_ID);
         System.out.println("component.id= " +compId);
         assertNotNull("component.id null", compId);
         assertTrue("component.id > 0", compId.longValue()>0);

         Object obj = bc.getService(ref);

         try {
           Thread.sleep(1000);
         } catch (Exception e) {}

         assertNotNull("Could not get service object for A1", obj);
         assertEquals("Should have been bumped", counter, 1);

         bc.ungetService(ref);

         try {
           Thread.sleep(1000);
         } catch (Exception e) {}

         assertEquals("Should have been bumped again", counter, 2);

         counter = 0;
         c1.uninstall();

      } catch (Exception e ) {
        fail("Got exception: Test1: " + e);
      }
    }
  }


  private class Test2 extends FWTestCase {

    /**
     * Test setup: ComponentA references ComponentB,
     *             ComponentB references ComponentC
     *             ComponentC references TestService
     * before: no components are started.
     * action: TestService is registered
     * after: all components are activated
     *
     * then:
     *
     * before: all components are activated
     * action: unregister TestService
     * after: all components are deactivated
     *
     * (the components call bump when they are (de-)actived)
     */

    public void runTest() {
      try {

        counter = 0;

        Bundle c1 = Util.installBundle(bc, "componentA_test-1.0.1.jar");
        c1.start();

        assertNull("Should be null (1)", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentA"));
        assertNull("Should be null (2)", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB"));
        assertNull("Should be null (3)", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should not have been bumped", 0, counter);

        ServiceRegistration reg = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable());

        Thread.sleep(1000);

        ServiceReference ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentA");
        bc.getService(ref);

        ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB");
        bc.getService(ref);

        ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC");
        bc.getService(ref);

        assertEquals("Should have been bumped", 3, counter);
        reg.unregister();

        Thread.sleep(1000);
        assertNull("Should be null (1(2))", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentA"));
        assertNull("Should be null (2(2))", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB"));
        assertNull("Should be null (3(2))", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should have been bumped", counter, 6);

        c1.uninstall();
        counter = 0;
      } catch (Exception e) {
        fail("Test2: got unexpected exception " + e);
      }
    }
  }

  private class Test3 extends FWTestCase {
    /**
     * This test case checks that properties declared in the XML
     * definition of a componenents are correctly parsed and
     * propagated to the service published for the component.
     * <p>
     * How it works:
     * - test case is started: the bundles componentP is installed.
     * - ComponentP is registers the service ComponentP
     * - The test then gets the service reference (this should not
     *   activate the component)
     * - The propperties of the service reference are checked.
     * - When the component is deactivated.
     */

    public void runTest() {
      try {
         Bundle c1 = Util.installBundle(bc, "componentA_test-1.0.1.jar");
         c1.start();

         ServiceReference ref = bc.getServiceReference
           ("org.knopflerfish.bundle.componentA_test.ComponentP");
         assertNotNull("Could not get service reference for component P", ref);

         // Check that the component name is available as service property
         String compName
           = (String) ref.getProperty(ComponentConstants.COMPONENT_NAME);
         System.out.println("component.name= " +compName);
         assertEquals("component name valid", "componentP.test", compName);

         // Check that the component id is available as service property
         Long compId = (Long) ref.getProperty(ComponentConstants.COMPONENT_ID);
         System.out.println("component.id= " +compId);
         assertNotNull("component.id null", compId);
         assertTrue("component.id > 0", compId.longValue()>0);

         String s1 = (String) ref.getProperty("String1");
         System.out.println("String1= " +s1);
         assertNotNull("String1 not null", s1);
         assertEquals("String1 == string1", "string1", s1);

         String[] sa = (String[]) ref.getProperty("Strings");
         System.out.println("Strings= " +Arrays.asList(sa));
         assertNotNull("Strings not null", sa);
         assertTrue("String array property",
                    Arrays.equals(sa,new String[]{"apa","bepa","cepa"}));


         Long l1 = (Long) ref.getProperty("Long1");
         System.out.println("Long1= " +l1);
         assertNotNull("Long1 not null", l1);
         assertEquals("Long1 == 1", new Long(1), l1);

         long[] la = (long[]) ref.getProperty("Longs");
         printArray("Longs= ", la);
         assertNotNull("Longs not null", la);
         assertTrue("Long array property",
                    Arrays.equals(la,new long[]{1,2,3}));


         Double d1 = (Double) ref.getProperty("Double1");
         System.out.println("Double1= " +d1);
         assertNotNull("Double1 not null", d1);
         assertEquals("Double1 == 1", new Double(2.7182818284590), d1);

         double[] da = (double[]) ref.getProperty("Doubles");
         printArray("Doubles= ", da);
         assertNotNull("Doubles not null", da);
         assertTrue("Double array property",
                    Arrays.equals(da,new double[]{1,2,3,4}));


         Float f1 = (Float) ref.getProperty("Float1");
         System.out.println("Float1= " +f1);
         assertNotNull("Float1 not null", f1);
         assertEquals("Float1 == 1", new Float(4.0), f1);

         float[] fa = (float[]) ref.getProperty("Floats");
         printArray("Floats= ", fa);
         assertNotNull("Floats not null", fa);
         assertTrue("Float array property",
                    Arrays.equals(fa,new float[]{1,2,3,4,5}));


         Integer i1 = (Integer) ref.getProperty("Integer1");
         System.out.println("Integer1= " +i1);
         assertNotNull("Integer1 not null", i1);
         assertEquals("Integer1 == 1", new Integer(1), i1);

         int[] ia = (int[]) ref.getProperty("Integers");
         printArray("Integers= ", ia);
         assertNotNull("Integers not null", ia);
         assertTrue("Integer array property",
                    Arrays.equals(ia,new int[]{1,2,3,4,5,6}));


         Byte b1 = (Byte) ref.getProperty("Byte1");
         System.out.println("Byte1= " +b1);
         assertNotNull("Byte1 not null", b1);
         assertEquals("Byte1 == 1", new Byte( (byte)1), b1);

         byte[] ba = (byte[]) ref.getProperty("Bytes");
         printArray("Bytes= ", ba);
         assertNotNull("Bytes not null", ba);
         assertTrue("Byte array property",
                    Arrays.equals(ba,new byte[]{1,2,3,4,5,6,7}));


         Short sh1 = (Short) ref.getProperty("Short1");
         System.out.println("Short1= " +sh1);
         assertNotNull("Short1 not null", sh1);
         assertEquals("Short1 == 1", new Short((short)1), sh1);

         short[] sha = (short[]) ref.getProperty("Shorts");
         printArray("Shorts= ", sha);
         assertNotNull("Shorts not null", sha);
         assertTrue("Short array property",
                    Arrays.equals(sha,new short[]{1,2,3,4,5,6,7,8,9,0}));


         Character ch1 = (Character) ref.getProperty("Character1");
         System.out.println("Character1= " +ch1);
         assertNotNull("Character1 not null", ch1);
         assertEquals("Character1 == '1'", new Character('1'), ch1);

         char[] cha = (char[]) ref.getProperty("Characters");
         printArray("Characters= ", cha);
         assertNotNull("Characters not null", cha);
         assertTrue("Characters array property",
                    Arrays.equals(cha,new char[]{'1','2','3','4','5','6','7','8'}));


         Character ch2 = (Character) ref.getProperty("Character2");
         System.out.println("Character2= " +ch2);
         assertNotNull("Character2 not null", ch2);
         assertEquals("Character2 == '2'", new Character('2'), ch2);

         char[] ch2a = (char[]) ref.getProperty("Characters2");
         printArray("Characters2= ", ch2a);
         assertNotNull("Characters2 not null", ch2a);
         assertTrue("Characters2 array property",
                    Arrays.equals(ch2a,new char[]{'3','4','5','6','7','8'}));


         Boolean bo1 = (Boolean) ref.getProperty("Boolean1");
         System.out.println("Boolean1= " +bo1);
         assertNotNull("Boolean1 not null", bo1);
         assertEquals("Boolean1 == true", new Boolean(true), bo1);

         boolean[] boa = (boolean[]) ref.getProperty("Booleans");
         printArray("Booleans= ", boa);
         assertNotNull("Booleans not null", boa);
         assertTrue("Boolean array property",
                    Arrays.equals(boa,new boolean[]{false,true,false}));


         c1.uninstall();

      } catch (Exception e ) {
        fail("Got exception: Test3: " + e);
      }
    }

    void printArray(String caption, Object a) throws IOException {
      StringBuffer sb = new StringBuffer(80);

      sb.append(caption);
      sb.append(" [");
      int length = Array.getLength(a);
      for(int i = 0; i < length; i++) {
        sb.append(Array.get(a,i));
        if(i < length - 1) {
          sb.append(", ");
        }
      }
      sb.append("]");
      System.out.println(sb);
    }

  }


}
