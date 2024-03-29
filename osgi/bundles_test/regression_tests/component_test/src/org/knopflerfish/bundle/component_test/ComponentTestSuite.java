/*
 * Copyright (c) 2006-2022, KNOPFLERFISH project
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

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.knopflerfish.service.component_test.ComponentATest;
import org.knopflerfish.service.component_test.TestService;
import org.knopflerfish.service.component_test.TestService2;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class ComponentTestSuite extends TestSuite implements ComponentATest
{
  public static final long SLEEP_TIME = 1000;

  private BundleContext bc;

  private int counter = 0;

  public ComponentTestSuite(BundleContext bc) {
    super("ComponentTestSuite");
    this.bc = bc;
    addTest(new Test1());
    addTest(new Test2b());
    addTest(new Test3());
    addTest(new TestSfBugs32556558());
    addTest(new TestSfBugs2883959());
    addTest(new Test4());
    addTest(new Test5());
    addTest(new Test6());
    addTest(new Test7());
    addTest(new Test8());
    addTest(new Test9());
    addTest(new Test10());
    addTest(new Test11());
    addTest(new Test12());
    addTest(new Test13());
    addTest(new Test14());
  }

  public void bump(int count) {
    counter += count;
  }

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

         ServiceReference<?> ref = bc.getServiceReference
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
         // NYI, larger than previous, assertTrue("component.id > 0", compId.longValue()>0);

         Object obj = bc.getService(ref);

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (Exception e) {}

         assertNotNull("Could not get service object for A1", obj);
         assertEquals("Should have been activate bumped", 1, counter);

         bc.ungetService(ref);

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (Exception e) {}

         assertEquals("Should have been deactivate bumped", 11, counter);

         counter = 0;
         c1.uninstall();

      } catch (Exception e ) {
        fail("Got exception: Test1: " + e);
      }
    }
  }


  private class Test2b extends FWTestCase implements LogListener {

    /**
     * Test setup: ComponentA references ComponentB,
     *             ComponentB references ComponentC,TestService2
     *             ComponentC references TestService
     *             ComponentD provides TestService and reference ComponentA
     * before: no components are started. Circular condition detected
     * action: TestService and TestService2 is registered
     * after: all components are activated
     *
     * then:
     *
     * before: all components are activated
     * action: unregister TestService
     * after: all components are deactivated
     *        (this because when ComponentC rebinds with TestService
     *         from ComponentD it detects that it is broken and
     *         disposes itself).
     *
     */
    private boolean gotCircularError;

    public void logged(LogEntry le) {
      if (le.getLevel() == LogService.LOG_ERROR &&
          le.getMessage().indexOf("circular") >= 0) {
        gotCircularError = true;
      }
    }


    public void runTest() {
      Bundle c1 = null;
      ServiceReference<?> sr = null;
      ServiceRegistration<?> reg = null;
      ServiceRegistration<?> reg2 = null;
      try {
        counter = 0;
        gotCircularError = false;
        sr = bc.getServiceReference(LogReaderService.class.getName());
        LogReaderService lrs = (LogReaderService)bc.getService(sr);
        lrs.addLogListener(this);
        c1 = Util.installBundle(bc, "componentA_test-1.0.1.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);
        sr = bc.getServiceReference(ServiceComponentRuntime.class.getName());
        ServiceComponentRuntime scr = (ServiceComponentRuntime)bc.getService(sr);
        ComponentDescriptionDTO cdd = new ComponentDescriptionDTO();
        cdd.name = "componentD.test";
        cdd.bundle = c1.adapt(BundleDTO.class);
        scr.enableComponent(cdd);
        Thread.sleep(SLEEP_TIME);

        assertNull("Should be null (1)", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentA"));
        assertNull("Should be null (2)", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB"));
        assertNull("Should be null (3)", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should not have been bumped", 0, counter);
        assertTrue("Should have got circular error message", gotCircularError);
        lrs.removeLogListener(this);

        reg2 = bc.registerService(TestService2.class.getName(), new TestService2(), new Hashtable<String, Object>());
        reg = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable<String, Object>());

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentA");
        assertNotNull("Should get service A", bc.getService(ref));

        ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB");
        assertNotNull("Should get service B", bc.getService(ref));

        ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC");
        assertNotNull("Should get service C", bc.getService(ref));

        assertEquals("Should have been activate/bind bumped", 103, counter);
        reg.unregister();
        reg = null;

        Thread.sleep(SLEEP_TIME);
        assertNull("Should be null (1(2))", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentA"));
        assertNull("Should be null (2(2))", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB"));
        assertNull("Should be null (3(2))", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should have been bind/2*unbind and deactive bumped", 2233, counter);

        counter = 0;
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test2b: got unexpected exception " + e);
      } finally {
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (BundleException be) {
            be.printStackTrace();
            fail("Test2b: got uninstall exception " + be);
          }
        }
        if (sr != null) {
          bc.ungetService(sr);
        }
        if (reg != null) {
          reg.unregister();
        }
        if (reg2 != null) {
          reg2.unregister();
        }
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

         ServiceReference<?> ref = bc.getServiceReference
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
         // NYI, larger than previous, assertTrue("component.id > 0", compId.longValue()>0);

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
         assertEquals("Double1 == 2^24 + 2^-8", new Double(16777216.00390625), d1);

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
      StringBuilder sb = new StringBuilder(80);

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

  private class TestSfBugs32556558 extends FWTestCase {
    /**
     * This test case is a regression test for SF Bugs item 32556558.
     *
     * How it works:
     * - A service publishing component, ComponentE1, with an optional,
     *   dynamic dependency on ComponentE2, is registered by bundleE_test.
     * - A factory component, ComponentE2, that will register a
     *   service is also provided by bundleE_test.
     * - First check that the ComponentE1 serivce is published when
     *   bundleE_test is started and the it have not been given a
     *   ComponentE2-service.
     * - Create an instance of ComponentE2, check that it is bound as
     *   the ComponentE2-service in ComponentE1.
     */

    public void runTest() {
      try {
         Bundle buE = Util.installBundle(bc, "componentE_test-1.0.0.jar");
         buE.start();

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (InterruptedException ie) {}

         ServiceReference<?> e1SR = bc.getServiceReference
           ("org.knopflerfish.service.componentE_test.ComponentE1");

         assertNotNull("Could not get service reference for E1", e1SR);

         // Check that the component name is available as service property
         String compName
           = (String) e1SR.getProperty(ComponentConstants.COMPONENT_NAME);
         System.out.println("component.name= " +compName);
         assertEquals("component name valid", "componentE_test.E1", compName);

         Object e1 = bc.getService(e1SR);
         assertNotNull("Could not get service object for E1", e1);

         // Check that no E2 instance have been injected into e1 yet.
         Method getE2Method = e1.getClass().getMethod("getE2", (Class[])null);
         assertNull("No E2 service yet",
                    getE2Method.invoke(e1, new Object[]{}));
         System.out.println("Initially no E2 service bound to E1.");

         // Prepare to create an E2 component instance
         ServiceReference<?>[] e2FactorySRs = bc.getServiceReferences
           (ComponentFactory.class.getName(),
            "(" +ComponentConstants.COMPONENT_FACTORY
            +"=componentE_test.E2-factory)");
         assertNotNull("No E2 component factory service ref", e2FactorySRs);
         assertEquals("One E2 component factory service ref",
                      1, e2FactorySRs.length);

         ComponentFactory e2Factory
           = (ComponentFactory) bc.getService(e2FactorySRs[0]);
         assertNotNull("No E2 component factory", e2Factory);

         // Create an E2 component instance
         System.out.println("Creating E2 component instance.");
         ComponentInstance e2Inst = e2Factory.newInstance((Dictionary<String,Object>) null);
         assertNotNull("No E2 instance", e2Inst);

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (InterruptedException ie) {}


         // Check that the E2 instance have been bound to e1.
         Object e2 = getE2Method.invoke(e1, new Object[]{});
         System.out.println("E2 service bound to E1: " +e2);
         assertNotNull("E2 service should now be bound to e1", e2);

         // Destroy the E2 component instance
         System.out.println("Disposing E2 component instance.");
         e2Inst.dispose();
         e2Inst = null;

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (InterruptedException ie) {}

         // Check that the E2 instance have been unbound from e1.
         e2 = getE2Method.invoke(e1, new Object[]{});
         System.out.println("E2 service bound to E1 after disposal: " +e2);
         assertNull("E2 service should be unbound from e1 after dispose", e2);

         // Create a second E2 component instance
         System.out.println("Creating second E2 component instance.");
         e2Inst = e2Factory.newInstance((Dictionary<String,Object>) null);
         assertNotNull("No E2 instance (2)", e2Inst);

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (InterruptedException ie) {}


         // Check that the second E2 instance have been bound to e1 now.
         e2 = getE2Method.invoke(e1, new Object[]{});
         System.out.println("Second E2 service bound to e1: " +e2);
         assertNotNull("E2 service should now be bound to e1", e2);

         // Destroy the second E2 component instance
         System.out.println("Disposing second E2 component instance.");
         e2Inst.dispose();
         e2Inst = null;

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (InterruptedException ie) {}

         // Check that the second E2 instance have been unbound from e1.
         e2 = getE2Method.invoke(e1, new Object[]{});
         System.out.println("E2 service bound to E1 after disposal (2): " +e2);
         assertNull("E2 service bound to E1 after disposal (2).", e2);

         // Cleanup
         bc.ungetService(e2FactorySRs[0]);
         bc.ungetService(e1SR);
         buE.uninstall();
      } catch (Exception e ) {
        e.printStackTrace();
        fail("Got unexpected exception: " +e);
      }
    }
  }


  private class TestSfBugs2883959 extends FWTestCase {
    /**
     * This test case is a regression test for SF Bugs item 2883959.
     *
     * How it works:
     * - Install a bundle, Component2_test, with two component
     *   specifing XML-documents. Each of the XML-files specifies a
     *   single component providing a service (same service interface
     *   for both components).
     * - There are extra spaces in between those files in the
     *   Service-Component header.
     * - Check that both services are registered.
     */
    public void runTest() {
      try {
         final Bundle bu2 = Util.installBundle(bc, "component2_test-1.0.0.jar");
         bu2.start();

         try {
           Thread.sleep(SLEEP_TIME);
         } catch (InterruptedException ie) {}

         final ServiceReference<?>[] srs = bc.getServiceReferences
           ("org.knopflerfish.service.component2_test.Component", null);

         assertEquals("Expected 2 service references", 2, srs.length);

         for (int i=0; i<2; i++) {
           // Check that the service is registered by bu2.
           assertEquals("Service registered by component2_test-bundle "+i,
                        srs[i].getBundle(), bu2);

           final Object s = bc.getService(srs[i]);
           assertNotNull("Could not get service object "+i, s);

           // Call the service method to double check.
           final Method getMethod = s.getClass().getMethod("get", (Class[])null);

           final String v = (String) getMethod.invoke(s, new Object[]{});
           assertEquals("s.get(" +i +")", "C"+(i+1), v);
         }

         // Cleanup
         bc.ungetService(srs[0]);
         bc.ungetService(srs[1]);
         bu2.uninstall();
      } catch (Exception e ) {
        e.printStackTrace();
        fail("Got unexpected exception: " +e);
      }
    }
  }

  private class Test4 extends FWTestCase  {

    /**
     * Test setup: ComponentA references ComponentB,
     *             ComponentB references ComponentC,TestService2
     *             ComponentC references TestService
     *             ComponentD provides TestService and reference ComponentA
     * before: no components are started.
     * action: TestService and TestService2 is registered
     * after: all components are activated
     *
     * then:
     *
     * before: all components are activated
     * action: modify TestService2 to block ComponentB
     * after: only ComponentC is active
     *
     * then:
     *
     * before: all components are activated
     * action: unregister TestService and TestService2
     * after: all components are deactivated
     *
     * (the components call bump when they are (de-)actived)
     */

    public void runTest() {
      Bundle c1 = null;
      ServiceRegistration<?> reg = null;
      ServiceRegistration<?> reg2 = null;
      try {
        reg = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable<String,Object>());
        reg2 = bc.registerService(TestService2.class.getName(), new TestService2(), new Hashtable<String,Object>());

        counter = 0;
        c1 = Util.installBundle(bc, "componentA_test-1.0.1.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB");
        assertNotNull("Should get serviceRef B", ref);
        assertNotNull("Should get service B", bc.getService(ref));

        ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC");
        assertNotNull("Should get serviceRef C", ref);
        assertNotNull("Should get service C", bc.getService(ref));

        assertEquals("Should have been activate(B&C)/bind(C) bumped", 102, counter);
        Hashtable<String, Object> p = new Hashtable<String,Object>();
        p.put("block","yes");
        reg2.setProperties(p);

        Thread.sleep(SLEEP_TIME);
        assertNull("Should not get B", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB"));
        assertNotNull("Should still get C", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should have been deactivate B", 112, counter);

        reg.unregister();
        reg = null;

        Thread.sleep(SLEEP_TIME);

        assertNull("Should not get C", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should have been deactivate/unbind C bumped", 1122, counter);

        counter = 0;
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test4: got unexpected exception " + e);
      } finally {
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (BundleException be) {
            be.printStackTrace();
            fail("Test4: got uninstall exception " + be);
          }
        }
        if (reg != null) {
          reg.unregister();
        }
        if (reg2 != null) {
          reg2.unregister();
        }
      }
    }
  }

  private class Test5 extends FWTestCase  {

    /**
     * Test setup: ComponentA references ComponentB,
     *             ComponentB references ComponentC,TestService2
     *             ComponentC references TestService
     *             ComponentD provides TestService and reference ComponentA
     * before: no components are started.
     * action: TestService and TestService2 is registered
     * after: all components are activated
     *
     * then:
     *
     * before: all components are activated
     * action: register second TestService, then set high service ranking and
     *         then unregister first TestService
     * after: all components are still activated
     *
     * then:
     *
     * before: all components are activated
     * action: unregister second TestService
     * after: all components are deactivated
     *
     * (the components call bump when they are (de-)actived)
     */

    public void runTest() {
      Bundle c1 = null;
      ServiceRegistration<?> reg = null;
      ServiceRegistration<?> regSecond = null;
      ServiceRegistration<?> reg2 = null;
      try {
        reg = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable<String,Object>());
        reg2 = bc.registerService(TestService2.class.getName(), new TestService2(), new Hashtable<String,Object>());

        counter = 0;
        c1 = Util.installBundle(bc, "componentA_test-1.0.1.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB");
        assertNotNull("Should get serviceRef B", ref);
        assertNotNull("Should get service B", bc.getService(ref));

        ref = bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC");
        assertNotNull("Should get serviceRef C", ref);
        assertNotNull("Should get service C", bc.getService(ref));

        assertEquals("Should have been activate(B&C)/bind(C) bumped", 102, counter);
        regSecond = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable<String,Object>());
        Hashtable<String,Object> p = new Hashtable<String,Object>();
        p.put(Constants.SERVICE_RANKING, new Integer(7));
        regSecond.setProperties(p);
        reg.unregister();
        reg = null;

        Thread.sleep(SLEEP_TIME);
        assertNotNull("Should still get B", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB"));
        assertNotNull("Should still get C", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should have been deactivate B", 1202, counter);

        regSecond.unregister();
        regSecond = null;

        Thread.sleep(SLEEP_TIME);
        assertNull("Should not get B", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentB"));
        assertNull("Should not get C", bc.getServiceReference("org.knopflerfish.bundle.componentA_test.ComponentC"));
        assertEquals("Should have been deactivate/unbind C bumped", 2222, counter);

        counter = 0;
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test5: got unexpected exception " + e);
      } finally {
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (BundleException be) {
            be.printStackTrace();
            fail("Test5: got uninstall exception " + be);
          }
        }
        if (reg != null) {
          reg.unregister();
        }
        if (regSecond != null) {
          regSecond.unregister();
        }
        if (reg2 != null) {
          reg2.unregister();
        }
      }
    }
  }

  private class Test6 extends FWTestCase  {

    /**
     * Test setup: Dynamic ComponentX optional references ComponentY,
     *             Dynamic ComponentY static reference ComponentZ
     *             Dynamic ComponentZ static reference ComponentX
     * before: no components are started.
     * action: Get ComponentX service
     * after: all components are activated
     *
     * then:
     *
     * before: all components are activated
     * action: disable componentZ
     * after: only X should be active
     *
     * then:
     *
     * before: only X active
     * action: enable componentY
     * after: all components are deactivated
     *
     */

    public void runTest() {
      Bundle c1 = null;
      try {
        c1 = Util.installBundle(bc, "componentX_test-1.0.0.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentX");
        assertNotNull("Should get serviceRef X", ref);
        org.knopflerfish.service.componentX_test.ComponentX x =
          (org.knopflerfish.service.componentX_test.ComponentX)bc.getService(ref);
        assertNotNull("Should get service X", x);

        assertEquals("Should have been bind(Y) bumped", 1, x.getBindStatus().intValue());
        assertEquals("Should have been bind(Z) bumped", 1, x.getBindYStatus().intValue());

        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentZ");
        assertNotNull("Should get serviceRef Z", ref);
        org.knopflerfish.service.componentX_test.ComponentZ z =
          (org.knopflerfish.service.componentX_test.ComponentZ)bc.getService(ref);
        assertNotNull("Should get service Z", z);

        assertEquals("Z should have been bind(X) bumped", 1, z.getBindStatus().intValue());
        assertEquals("X should have been bind(Y) bumped", 1, z.getBindXStatus().intValue());

        z.disableZ();

        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentZ");
        assertNull("Should not get serviceRef Z", ref);
        assertEquals("Z should have been unbind(X) bumped", 101, z.getBindStatus().intValue());

        assertEquals("Should have been unbind(Y) bumped", 101, x.getBindStatus().intValue());
        assertNull("Should have been unbind(Z) bumped, but unaccesible", x.getBindYStatus());

        x.enableZ();

        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentZ");
        assertNotNull("Should get serviceRef Z", ref);
        z = (org.knopflerfish.service.componentX_test.ComponentZ)bc.getService(ref);
        assertEquals("Z should have been bind(X) bumped", 1, z.getBindStatus().intValue());

        assertEquals("Should have been bind(Y) bumped", 102, x.getBindStatus().intValue());
        assertEquals("Should have been bind(Z) bumped", 1, x.getBindYStatus().intValue());

        assertEquals("X should have been bind(Y) bumped", 102, z.getBindXStatus().intValue());
        assertEquals("Z should have been bind(X) bumped", 1, z.getBindStatus().intValue());

        c1.stop();
        // Restart components
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentY");
        assertNotNull("Should get serviceRef Y", ref);
        org.knopflerfish.service.componentX_test.ComponentY y =
          (org.knopflerfish.service.componentX_test.ComponentY)bc.getService(ref);
        assertNotNull("Should get service Y", y);

        assertEquals("Should have been bind(Z) bumped", 1, y.getBindStatus().intValue());

        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentZ");
        assertNotNull("Should get serviceRef Z", ref);
        z = (org.knopflerfish.service.componentX_test.ComponentZ)bc.getService(ref);
        assertNotNull("Should get service Z", z);

        assertEquals("Z should have been bind(X) bumped", 1, z.getBindStatus().intValue());
        assertEquals("X should have been bind(Y) bumped", 1, z.getBindXStatus().intValue());

        z.disableZ();

        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentY");
        assertNull("Should not get serviceRef Y", ref);
        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentY");
        assertNull("Should not get serviceRef Z", ref);
        assertEquals("Y should have been unbind(Z) bumped", 101, y.getBindStatus().intValue());
        assertEquals("Z should have been unbind(X) bumped", 101, z.getBindStatus().intValue());

        ref = bc.getServiceReference("org.knopflerfish.service.componentX_test.ComponentX");
        assertNotNull("Should get serviceRef X", ref);
        x = (org.knopflerfish.service.componentX_test.ComponentX)bc.getService(ref);
        assertNotNull("Should get service X", x);

        assertEquals("X should have been unbind(Y) bumped", 101, x.getBindStatus().intValue());

      } catch (Exception e) {
        e.printStackTrace();
        fail("Test6: got unexpected exception " + e);
      } finally {
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (BundleException be) {
            be.printStackTrace();
            fail("Test6: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test7 extends FWTestCase  {

    /**
     * Test setup: Immediate ComponentX references required ComponentY, ComponentZ, TestService
     *             Dynamic ComponentY
     *             Dynamic ComponentZ dynamic required reference ComponentY
     * before: no components are started.
     * action: Register TestService
     * after: all components are activated
     *
     * then:
     *
     * before: all components are activated
     * action: Unregister TestService
     * after: all components are deactivated
     *
     */

    public void runTest() {
      Bundle c1 = null;
      try {
        c1 = Util.installBundle(bc, "componentU_test-1.0.0.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentU_test.ComponentX");
        assertNull("Should not get serviceRef X", ref);

        
        ServiceRegistration<?> reg = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable<String,Object>());

        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentU_test.ComponentX");
        assertNotNull("Should get serviceRef X", ref);
        org.knopflerfish.service.componentU_test.ComponentX x =
          (org.knopflerfish.service.componentU_test.ComponentX)bc.getService(ref);
        assertNotNull("Should get service X", x);
        assertEquals("Should have been bind(Y) bumped", 1, x.getBindYStatus().intValue());
        assertEquals("Should have been bind(Z) bumped", 1, x.getBindZStatus().intValue());

        reg.unregister();

        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentU_test.ComponentX");
        assertNull("Should not get serviceRef X", ref);
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test7: got unexpected exception " + e);
      } finally {
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test7: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test8 extends FWTestCase  {

    /**
     * Test setup: Factory ComponentX references required ComponentY, TestService
     *             Delayed ComponentY
     *
     * before: All components are satisfied.
     * action: Get ComponentFactory and create 2 instances
     * after: all components are activated and we have 2 factory instances
     *
     * before: all components are activated and we have 2 factory instances
     * action: Register TestService, check that both factory instances gets bound
     * after: all components are activated and we have 2 factory instances
     *
     * before: all components are activated and we have 2 factory instances
     * action: Unregister TestService, check that both factory instances gets unbound
     * after: all components are activated and we have 2 factory instances
     *
     * before: all components are activated and we have 2 factory instances
     * action: Register TestService, check that both factory instances gets bound
     * after: all components are activated and we have 2 factory instances
     *
     * before: all components are activated and we have 2 factory instances
     * action: Dispose of one factory instance, check that only that factory instance gets unbound
     * after: all components are activated and we have 1 factory instances
     *
     * before: all components are activated and we have 1 factory instances
     * action: Dispose of last factory instance, check that only that factory instance gets unbound
     * after: all components are activated and we have no factory instances
     *
     * before: all components are activated and we have no factory instances
     * action: Unregister TestService, check that no factory unbind is called
     * after: all components are activated and we have no factory instances
     */

    public void runTest() {
      Bundle c1 = null;
      try {
        c1 = Util.installBundle(bc, "componentF_test-1.0.0.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentF_test.ComponentX");
        assertNull("Should not get serviceRef X", ref);

        ServiceReference<?> yref = bc.getServiceReference("org.knopflerfish.service.componentF_test.ComponentY");
        assertNotNull("Should get serviceRef Y", yref);
        org.knopflerfish.service.componentF_test.ComponentY y =
            (org.knopflerfish.service.componentF_test.ComponentY)bc.getService(yref);

        assertEquals("No test calls", 0, y.getTestStatus());

        ServiceReference<?> [] refs = bc.getServiceReferences(ComponentFactory.class.getName(),
            "(&(component.name=componentF_test.factory)(component.factory=componentF_test.X))");
        assertTrue("Should get one serviceRef factory", refs != null && refs.length == 1);
        ComponentFactory cf = (ComponentFactory) bc.getService(refs[0]);
        assertNotNull("Should get ComponentFactory", cf);

        Hashtable<String, Integer> dict = new Hashtable<String, Integer>();
        dict.put("base", new Integer(1));
        ComponentInstance ci1 = cf.newInstance(dict);
        dict.put("base", new Integer(10));
        ComponentInstance ci2 = cf.newInstance(dict);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentF_test.ComponentX", null);
        assertNotNull("Should get serviceRef X", refs);
        assertEquals("Should get two serviceRef X", 2, refs.length);
        
        refs = bc.getServiceReferences("org.knopflerfish.service.componentF_test.ComponentX", "(base=1)");
        assertNotNull("Should get serviceRef X", refs);
        assertEquals("Should get one serviceRef X", 1, refs.length);
        org.knopflerfish.service.componentF_test.ComponentX x =
          (org.knopflerfish.service.componentF_test.ComponentX)bc.getService(refs[0]);
        assertEquals("X1 should have been bind(Y) bumped", 1, x.getBindYStatus());

        Collection<ServiceReference<org.knopflerfish.service.componentF_test.ComponentX>> refc =
          bc.getServiceReferences(org.knopflerfish.service.componentF_test.ComponentX.class, "(base=10)");
        assertEquals("Should get one serviceRef X", 1, refc.size());
        x = bc.getService(refc.iterator().next());
        assertEquals("X2 should have been bind(Y) bumped", 1, x.getBindYStatus());

        assertEquals("Still no test calls", 0, y.getTestStatus());

        ServiceRegistration<?> reg = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable<String,String>());
        Thread.sleep(200);

        assertEquals("Should have been 2*bind(Test) bumped", 11, y.getTestStatus());

        reg.unregister();
        Thread.sleep(200);

        assertEquals("Should have been 2*unbind(Test) bumped", 11011, y.getTestStatus());
        
        reg = bc.registerService(TestService.class.getName(), new TestService(), new Hashtable<String,String>());
        Thread.sleep(200);

        assertEquals("Should have another 2*bind(Test) bumped", 11022, y.getTestStatus());

        ci1.dispose();

        assertEquals("Should have first instance unbind(Test) bumped", 12022, y.getTestStatus());

        reg.unregister();
        Thread.sleep(200);

        assertEquals("Should have second instance unbind(Test) bumped", 22022, y.getTestStatus());
        
        ci2.dispose();
        
        assertEquals("Should have nothing bumped", 22022, y.getTestStatus());
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test8: got unexpected exception " + e);
      } finally {
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test8: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test9 extends FWTestCase  {

    /**
     * Test setup: Factory ComponentX references required ComponentY,
     *             Delayed ComponentZ dynamically references optional ComponentXs
     *
     * before: All components are satisfied.
     * action: Get ComponentZ, ComponentFactory and create 1 instances, check that new factory instance gets bound
     * after: all components are activated and we have 1 factory instances
     *
     * before: all components are activated and we have 1 factory instances
     * action: Create one more factory instance, check that new factory instance gets bound
     * after: all components are activated and we have 2 factory instances
     *
     * before: all components are activated and we have 2 factory instances
     * action: Disable ComponentZ, check that ComponentZ is deactivated and factory instances gets unbound
     * after: ComponentX and ComponentZ are activated and we have 2 factory instances
     *
     * before: ComponentX and ComponentZ are activated and we have 2 factory instances
     * action: Enable ComponentZ, check that we get new ComponentZ and that both factory instances gets bound
     * after: all components are activated and we have 2 factory instances
     *
     * before: all components are activated and we have 2 factory instances
     * action: Create one more factory instance, check that new factory instance gets bound
     * after: all components are activated and we have 3 factory instances
     *
     * before: all components are activated and we have 3 factory instances
     * action: Dispose one factory instance, check that only that factory instance gets unbound
     * after: all components are activated and we have 2 factory instances
     *
     * before: all components are activated and we have 2 factory instances
     * action: Dispose last factory instance, check that factory instances gets unbound
     * after: all components are activated and we have no factory instances
     */

    public void runTest() {
      Bundle c1 = null;
      try {
        c1 = Util.installBundle(bc, "componentF_test-1.0.0.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentF_test.ComponentX");
        assertNull("Should not get serviceRef X", ref);

        ServiceReference<?> zref = bc.getServiceReference("org.knopflerfish.service.componentF_test.ComponentZ");
        assertNotNull("Should get serviceRef Z", zref);
        org.knopflerfish.service.componentF_test.ComponentZ z =
            (org.knopflerfish.service.componentF_test.ComponentZ)bc.getService(zref);
        assertNotNull("Should get service Z", z);

        assertEquals("No test calls", 0, z.getXStatus());

        ServiceReference<?> [] refs = bc.getServiceReferences(ComponentFactory.class.getName(),
            "(&(component.name=componentF_test.factory)(component.factory=componentF_test.X))");
        assertTrue("Should get one serviceRef factory", refs != null && refs.length == 1);
        ComponentFactory cf = (ComponentFactory) bc.getService(refs[0]);
        assertNotNull("Should get ComponentFactory", cf);

        Hashtable<String, Integer> dict = new Hashtable<String, Integer>();
        dict.put("base", new Integer(1));
        ComponentInstance ci1 = cf.newInstance(dict);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentF_test.ComponentX", null);
        assertNotNull("Should get serviceRef X", refs);
        assertEquals("Should get one serviceRef X", 1, refs.length);
        org.knopflerfish.service.componentF_test.ComponentX x =
            (org.knopflerfish.service.componentF_test.ComponentX)bc.getService(refs[0]);
        assertNotNull("Should get X service", x);

        assertEquals("One bind call", 1, z.getXStatus());

        dict.put("base", new Integer(10));
        ComponentInstance ci2 = cf.newInstance(dict);

        assertEquals("One more bind call", 11, z.getXStatus());

        x.disableZ();
        
        assertEquals("Two unbind calls", 11011, z.getXStatus());

        zref = bc.getServiceReference("org.knopflerfish.service.componentF_test.ComponentZ");
        assertNull("Should not get serviceRef X", ref);
        
        x.enableZ();

        zref = bc.getServiceReference("org.knopflerfish.service.componentF_test.ComponentZ");
        assertNotNull("Should get serviceRef Z", zref);
        org.knopflerfish.service.componentF_test.ComponentZ znew =
            (org.knopflerfish.service.componentF_test.ComponentZ)bc.getService(zref);

        assertNotSame("Should get a new service", z, znew);

        assertEquals("Two bind calls", 11, znew.getXStatus());
        
        dict.put("base", new Integer(100));
        ComponentInstance ci3 = cf.newInstance(dict);

        assertEquals("One more bind call", 111, znew.getXStatus());

        ci1.dispose();

        assertEquals("One unbind call", 1111, znew.getXStatus());

        ci2.dispose();
        ci3.dispose();

        assertEquals("All unbind calls", 111111, znew.getXStatus());
        assertEquals("No calls to old z", 11011, z.getXStatus());
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test9: got unexpected exception " + e);
      } finally {
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test9: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test10 extends FWTestCase  {

    /**
     * Test that SCR handles targeted CM pids.
     * 
     */

    public void runTest() {
      Bundle b1 = null;
      ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmt = null;
      try {
        b1 = Util.installBundle(bc, "componentC_test-1.0.0.jar");
        b1.start();
        final String b1loc = b1.getLocation();

        cmt = new ServiceTracker<ConfigurationAdmin,ConfigurationAdmin>(bc, ConfigurationAdmin.class.getName(), null);
        cmt.open();
        
        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentX");
        assertNull("Should not get serviceRef X", ref);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentY");
        assertNotNull("Should get serviceRef Y", ref);
        org.knopflerfish.service.componentC_test.ComponentY y =
            (org.knopflerfish.service.componentC_test.ComponentY)bc.getService(ref);
        assertNull("Config pid null", y.getProp(Constants.SERVICE_PID));

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentZ");
        assertNull("Should not get serviceRef Z", ref);

        StringBuilder targetedPid = new StringBuilder("componentC_test.factory");
        ConfigurationAdmin ca = cmt.getService();
        Configuration c = ca.getConfiguration(targetedPid.toString(), b1loc);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("base", new Integer(1));
        c.update(props);
        
        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> [] refs = bc.getServiceReferences(ComponentFactory.class.getName(),
            "(&(component.name=componentC_test.factory)(component.factory=componentC_test.X))");
        assertTrue("Should get one serviceRef factory", refs != null && refs.length == 1);
        ComponentFactory cf = (ComponentFactory) bc.getService(refs[0]);
        assertNotNull("Should get componentCactory", cf);
        Dictionary<String, Object> iprops = new Hashtable<String, Object>();
        iprops.put("iprop", "first");
        cf.newInstance(iprops);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentX");
        assertNotNull("Should get serviceRef X", ref);
        org.knopflerfish.service.componentC_test.ComponentX x =
            (org.knopflerfish.service.componentC_test.ComponentX)bc.getService(ref);
        assertEquals(targetedPid.toString(), x.getProp(Constants.SERVICE_PID));
        assertEquals(new Integer(1), x.getProp("base"));
        assertEquals("first", x.getProp("iprop"));

        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentZ");
        assertNotNull("Should get serviceRef Z", ref);
        org.knopflerfish.service.componentC_test.ComponentZ z =
            (org.knopflerfish.service.componentC_test.ComponentZ)bc.getService(ref);
        assertEquals("Check x ref calls", 1, z.getXStatus());

        String bsnPid  = targetedPid.append('|').append(b1.getSymbolicName()).toString();
        targetedPid.append('|').append(b1.getVersion().toString());
        targetedPid.append('|').append(b1.getLocation().toString());
        Configuration c2 = ca.getConfiguration(targetedPid.toString(), b1loc);
        props.put("base", new Integer(2));
        c2.update(props);

        Thread.sleep(SLEEP_TIME);

        assertEquals(targetedPid.toString(), x.getProp(Constants.SERVICE_PID));
        assertEquals(new Integer(2), x.getProp("base"));
        assertEquals("first", x.getProp("iprop"));

        Configuration c3 = ca.getConfiguration(bsnPid, b1loc);
        props.put("base", new Integer(4));
        c3.update(props);

        Thread.sleep(SLEEP_TIME);

        assertEquals(targetedPid.toString(), x.getProp(Constants.SERVICE_PID));
        assertEquals(new Integer(2), x.getProp("base"));
        assertEquals("first", x.getProp("iprop"));
        
        c2.delete();

        Thread.sleep(SLEEP_TIME);

        assertEquals(bsnPid, x.getProp(Constants.SERVICE_PID));
        assertEquals(new Integer(4), x.getProp("base"));
        assertEquals("first", x.getProp("iprop"));

        c.delete();

        Thread.sleep(SLEEP_TIME);

        assertEquals(bsnPid, x.getProp(Constants.SERVICE_PID));
        assertEquals(new Integer(4), x.getProp("base"));
        assertEquals("first", x.getProp("iprop"));

        assertEquals("Check x ref calls", 6000001, z.getXStatus());
        
        c3.delete();
        
        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentX");
        assertNull("Should not get serviceRef X", ref);
        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentZ");
        assertNull("Should not get serviceRef Z", ref);

        c = ca.getConfiguration("c|test", b1loc);
        props = new Hashtable<String, Object>();
        props.put("factory", "no");
        c.update(props);

        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentY");
        assertNotNull("Should get serviceRef Y", ref);
        y = (org.knopflerfish.service.componentC_test.ComponentY)bc.getService(ref);
        assertEquals("c|test", y.getProp(Constants.SERVICE_PID));
        assertEquals("no", y.getProp("factory"));
        c.delete();
        
        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentY");
        assertNotNull("Should get serviceRef Y", ref);
        assertNull("Config pid null", y.getProp(Constants.SERVICE_PID));
        assertNull(y.getProp("factory"));
        
        targetedPid.setLength(0);
        targetedPid.append("c|test");
        c = ca.createFactoryConfiguration(targetedPid.toString(), b1loc);
        props.put("factory", "first");
        c.update(props);

        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentY");
        assertNotNull("Should get serviceRef Y", ref);
        y = (org.knopflerfish.service.componentC_test.ComponentY)bc.getService(ref);
        assertEquals("c|test", y.getProp(ConfigurationAdmin.SERVICE_FACTORYPID));
        assertEquals("first", y.getProp("factory"));

        targetedPid.append('|').append(b1.getSymbolicName());
        c2 = ca.createFactoryConfiguration(targetedPid.toString(), b1loc);
        props.put("factory", "second");
        c2.update(props);

        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentY", null);
        assertTrue("Should get two serviceRef Y", refs != null && refs.length == 2);
        org.knopflerfish.service.componentC_test.ComponentY y2 =
            (org.knopflerfish.service.componentC_test.ComponentY) bc.getService(refs[0]);
        if (y2 == y) {
          y2 = (org.knopflerfish.service.componentC_test.ComponentY) bc.getService(refs[1]);
          assertNotSame(y, y2);
        } else {
          assertEquals(y, bc.getService(refs[1]));
        }
        assertEquals(targetedPid.toString(), y2.getProp(ConfigurationAdmin.SERVICE_FACTORYPID));
        assertEquals("second", y2.getProp("factory"));

        targetedPid.append('|').append(b1.getVersion());
        c3 = ca.createFactoryConfiguration(targetedPid.toString(), b1loc);
        props.put("factory", "third");
        c3.update(props);

        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentY", null);
        assertTrue("Should get three serviceRef Y", refs != null && refs.length == 3);
        org.knopflerfish.service.componentC_test.ComponentY y3 = null;
        for (ServiceReference<?> yr : refs) {
          if ("third".equals(yr.getProperty("factory"))) {
            y3 = (org.knopflerfish.service.componentC_test.ComponentY) bc.getService(yr);
            break;
          }
        }
        assertNotNull(y3);
        assertEquals(targetedPid.toString(), y3.getProp(ConfigurationAdmin.SERVICE_FACTORYPID));
        assertEquals("third", y3.getProp("factory"));

        Configuration c4 = ca.createFactoryConfiguration("c|test", b1loc);
        props.put("factory", "fourth");
        c4.update(props);

        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentY", null);
        assertTrue("Should get four serviceRef Y", refs != null && refs.length == 4);
        org.knopflerfish.service.componentC_test.ComponentY y4 = null;
        for (ServiceReference<?> yr : refs) {
          if ("fourth".equals(yr.getProperty("factory"))) {
            y4 = (org.knopflerfish.service.componentC_test.ComponentY) bc.getService(yr);
            break;
          }
        }
        assertNotNull(y4);
        assertEquals("c|test", y4.getProp(ConfigurationAdmin.SERVICE_FACTORYPID));
        assertEquals("fourth", y4.getProp("factory"));
        
        c.delete();
        c3.delete();
        c4.delete();
        
        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentY", null);
        assertTrue("Should get one serviceRef Y", refs != null && refs.length == 1);
        y = (org.knopflerfish.service.componentC_test.ComponentY)bc.getService(refs[0]);
        assertEquals(y2, y);

        c2.delete();
        
        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentY", null);
        assertTrue("Should get one serviceRef Y", refs != null && refs.length == 1);
        assertNull("Config pid null", refs[0].getProperty(Constants.SERVICE_PID));
        y = (org.knopflerfish.service.componentC_test.ComponentY)bc.getService(refs[0]);
        assertNull("Config pid null", y.getProp(Constants.SERVICE_PID));

      } catch (Exception e) {
        e.printStackTrace();
        fail("Test10: got unexpected exception " + e);
      } finally {
        if (b1 != null) {
          try {
            if (cmt != null) {
              deleteConfig(cmt.getService(), b1.getLocation());
              cmt.close();
            }
            b1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test10: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test11 extends FWTestCase  {

    /**
     * Test that SCR handles factory CM pids with target filters.
     * 
     */

    public void runTest() {
      Bundle b1 = null;
      ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmt = null;
      try {
        b1 = Util.installBundle(bc, "componentC_test-1.0.0.jar");
        b1.start();
        final String b1loc = b1.getLocation();

        cmt = new ServiceTracker<ConfigurationAdmin,ConfigurationAdmin>(bc, ConfigurationAdmin.class.getName(), null);
        cmt.open();
        
        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentU");
        assertNull("Should not get serviceRef U", ref);

        ConfigurationAdmin ca = cmt.getService();
        Configuration c = ca.createFactoryConfiguration("componentC_test.U", b1loc);

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("vRef.target", "(vnum=v0)");
        c.update(props);
        
        Thread.sleep(SLEEP_TIME);

        ref = bc.getServiceReference("org.knopflerfish.service.componentC_test.ComponentU");
        assertNull("Should not get serviceRef U", ref);

        props.put("vRef.target", "(vnum=v1)");
        c.update(props);
        
        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> [] refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentU", null);
        assertTrue("Should get one serviceRef to ComponentU", refs != null && refs.length == 1);

        Configuration c2 = ca.createFactoryConfiguration("componentC_test.U", b1loc);
        props = new Hashtable<String, Object>();
        props.put("vRef.target", "(vnum=v2)");
        c2.update(props);
        
        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentU", null);
        assertTrue("Should get two serviceRef to ComponentU", refs != null && refs.length == 2);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentU", "(vRef.target=\\(vnum=v1\\))");
        assertTrue("Should get one serviceRef to ComponentU with ref v1", refs != null && refs.length == 1);
        org.knopflerfish.service.componentC_test.ComponentU u =
            (org.knopflerfish.service.componentC_test.ComponentU)bc.getService(refs[0]);
        assertEquals("Should get v1 version", "v1", u.getV());

        refs = bc.getServiceReferences("org.knopflerfish.service.componentC_test.ComponentU", "(vRef.target=\\(vnum=v2\\))");
        assertTrue("Should get one serviceRef to ComponentU with ref v2", refs != null && refs.length == 1);
        org.knopflerfish.service.componentC_test.ComponentU u2 =
            (org.knopflerfish.service.componentC_test.ComponentU)bc.getService(refs[0]);
        assertNotSame("Services should differ", u, u2);
        assertEquals("Should get v2 version", "v2", u2.getV());
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test11: got unexpected exception " + e);
      } finally {
        if (b1 != null) {
          try {
            if (cmt != null) {
              deleteConfig(cmt.getService(), b1.getLocation());
              cmt.close();
            }
            b1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test11: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test12 extends FWTestCase  {

    /**
     * Test that SCR handles factory CM pids with target filters.
     * 
     */

    public void runTest() {
      Bundle b1 = null;
      ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmt = null;
      try {
        b1 = Util.installBundle(bc, "componentM_test-1.0.0.jar");
        b1.start();
        final String b1loc = b1.getLocation();

        cmt = new ServiceTracker<ConfigurationAdmin,ConfigurationAdmin>(bc, ConfigurationAdmin.class.getName(), null);
        cmt.open();
        
        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentM_test.ComponentA");
        assertNull("Should not get serviceRef A", ref);

        ConfigurationAdmin ca = cmt.getService();

        Configuration c = ca.createFactoryConfiguration("componentM_test.A", b1loc);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("anum", "a0");
        props.put("bRef.target", "(bnum=b0)");
        c.update(props);
        Configuration c1 = ca.createFactoryConfiguration("componentM_test.A", b1loc);
        props.put("anum", "a1");
        props.put("bRef.target", "(bnum=b1)");
        c1.update(props);
        c = ca.createFactoryConfiguration("componentM_test.A", b1loc);
        props.put("anum", "a2");
        props.put("bRef.target", "(bnum=b2)");
        c.update(props);

        ref = bc.getServiceReference("org.knopflerfish.service.componentM_test.ComponentB");
        assertNull("Should not get serviceRef B", ref);

        props.clear();
        c = ca.createFactoryConfiguration("componentM_test.B2", b1loc);
        props.put("bnum", "b1");
        props.put("cRef.target", "(cnum=c1)");
        c.update(props);
        c = ca.createFactoryConfiguration("componentM_test.B2", b1loc);
        props.put("bnum", "b2");
        props.put("cRef.target", "(cnum=c2)");
        c.update(props);

        ref = bc.getServiceReference("org.knopflerfish.service.componentM_test.ComponentC");
        assertNull("Should not get serviceRef C", ref);

        props.clear();
        c = ca.createFactoryConfiguration("componentM_test.C", b1loc);
        props.put("cnum", "c1");
        c.update(props);
        c = ca.createFactoryConfiguration("componentM_test.C", b1loc);
        props.put("cnum", "c2");
        c.update(props);
        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> [] refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentC", null);
        assertTrue("Should get two serviceRef to ComponentC", refs != null && refs.length == 2);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentC", "(cnum=c1)");
        assertTrue("Should get one serviceRef to ComponentC with cnum c1", refs != null && refs.length == 1);
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentC", "(cnum=c2)");
        assertTrue("Should get one serviceRef to ComponentC with cnum c2", refs != null && refs.length == 1);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentB", null);
        assertTrue("Should get two serviceRef to ComponentB", refs != null && refs.length == 2);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentB", "(bnum=b1)");
        assertTrue("Should get one serviceRef to ComponentB with bnum b1", refs != null && refs.length == 1);
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentB", "(bnum=b2)");
        assertTrue("Should get one serviceRef to ComponentB with bnum b2", refs != null && refs.length == 1);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", null);
        assertTrue("Should get two serviceRef to ComponentA", refs != null && refs.length == 2);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a1)");
        assertTrue("Should get one serviceRef to ComponentA with anum a1", refs != null && refs.length == 1);
        org.knopflerfish.service.componentM_test.ComponentA a =
            (org.knopflerfish.service.componentM_test.ComponentA)bc.getService(refs[0]);
        assertEquals("Should get b1 version", "b1", a.getProp("bnum"));
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a2)");
        assertTrue("Should get one serviceRef to ComponentA with anum a2", refs != null && refs.length == 1);
        a = (org.knopflerfish.service.componentM_test.ComponentA)bc.getService(refs[0]);
        assertEquals("Should get b2 version", "b2", a.getProp("bnum"));
        
        Dictionary<String, Object> d = c1.getProperties();
        d.put("cRef.target", "(cnum=c0)");
        c1.update(d);
        Thread.sleep(SLEEP_TIME);
        
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", null);
        assertTrue("Should get one serviceRef to ComponentA", refs != null && refs.length == 1);
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a2)");
        assertTrue("Should get one serviceRef to ComponentA with anum a2", refs != null && refs.length == 1);

        d.put("cRef.target", "(cnum=c2)");
        c1.update(d);
        Thread.sleep(SLEEP_TIME);
        
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", null);
        assertTrue("Should get two serviceRef to ComponentA", refs != null && refs.length == 2);
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a1)");
        assertTrue("Should get one serviceRef to ComponentA with anum a1", refs != null && refs.length == 1);
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a2)");
        assertTrue("Should get one serviceRef to ComponentA with anum a2", refs != null && refs.length == 1);
      } catch (Exception e) {
        e.printStackTrace();
        fail("Test12: got unexpected exception " + e);
      } finally {
        if (b1 != null) {
          try {
            if (cmt != null) {
              deleteConfig(cmt.getService(), b1.getLocation());
              cmt.close();
            }
            b1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test12: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test13 extends FWTestCase  {

    /**
     * Test that SCR handles factory CM pids with target filters.
     * 
     */

    public void runTest() {
      Bundle b1 = null;
      ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmt = null;
      try {
        b1 = Util.installBundle(bc, "componentM_test-1.0.0.jar");
        b1.start();
        final String b1loc = b1.getLocation();

        cmt = new ServiceTracker<ConfigurationAdmin,ConfigurationAdmin>(bc, ConfigurationAdmin.class.getName(), null);
        cmt.open();
        
        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> ref = bc.getServiceReference("org.knopflerfish.service.componentM_test.ComponentA");
        assertNull("Should not get serviceRef A", ref);

        ConfigurationAdmin ca = cmt.getService();

        Configuration c = ca.createFactoryConfiguration("componentM_test.A", b1loc);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("anum", "a0");
        props.put("bRef.target", "(bnum=b0)");
        props.put("cRef.target", "(cnum=c1)");
        c.update(props);
        c = ca.createFactoryConfiguration("componentM_test.A", b1loc);
        props.put("anum", "a1");
        props.put("bRef.target", "(bnum=b1)");
        props.put("cRef.target", "(cnum=c2)");
        c.update(props);
        c = ca.createFactoryConfiguration("componentM_test.A", b1loc);
        props.put("anum", "a2");
        props.put("bRef.target", "(bnum=b2)");
        props.put("cRef.target", "(cnum=c0)");
        c.update(props);

        ref = bc.getServiceReference("org.knopflerfish.service.componentM_test.ComponentB");
        assertNull("Should not get serviceRef B", ref);

        props.clear();
        c = ca.createFactoryConfiguration("componentM_test.B2", b1loc);
        props.put("bnum", "b1");
        props.put("cRef.target", "(cnum=c1)");
        c.update(props);
        c = ca.createFactoryConfiguration("componentM_test.B2", b1loc);
        props.put("bnum", "b2");
        props.put("cRef.target", "(cnum=c2)");
        c.update(props);

        ref = bc.getServiceReference("org.knopflerfish.service.componentM_test.ComponentC");
        assertNull("Should not get serviceRef C", ref);

        props.clear();
        Configuration c1 = ca.createFactoryConfiguration("componentM_test.C", b1loc);
        props.put("cnum", "c1");
        c1.update(props);
        Configuration c2 = ca.createFactoryConfiguration("componentM_test.C", b1loc);
        props.put("cnum", "c2");
        c2.update(props);

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?> [] refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentC", null);
        assertTrue("Should get two serviceRef to ComponentC", refs != null && refs.length == 2);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentC", "(cnum=c1)");
        assertTrue("Should get one serviceRef to ComponentC with cnum c1", refs != null && refs.length == 1);
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentC", "(cnum=c2)");
        assertTrue("Should get one serviceRef to ComponentC with cnum c2", refs != null && refs.length == 1);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentB", null);
        assertTrue("Should get two serviceRef to ComponentB", refs != null && refs.length == 2);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentB", "(bnum=b1)");
        assertTrue("Should get one serviceRef to ComponentB with bnum b1", refs != null && refs.length == 1);
        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentB", "(bnum=b2)");
        assertTrue("Should get one serviceRef to ComponentB with bnum b2", refs != null && refs.length == 1);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", null);
        assertTrue("Should get one serviceRef to ComponentA", refs != null && refs.length == 1);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a1)");
        assertTrue("Should get one serviceRef to ComponentA with anum a1", refs != null && refs.length == 1);
        org.knopflerfish.service.componentM_test.ComponentA a =
            (org.knopflerfish.service.componentM_test.ComponentA)bc.getService(refs[0]);
        assertEquals("Should get b1 version", "b1", a.getProp("bnum"));

        c = ca.createFactoryConfiguration("componentM_test.C", b1loc);
        props.put("cnum", "c0");
        c.update(props);
        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", null);
        assertTrue("Should get three serviceRef to ComponentA", refs != null && refs.length == 3);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a1)");
        assertTrue("Should still get one serviceRef to ComponentA with anum a1", refs != null && refs.length == 1);
        a = (org.knopflerfish.service.componentM_test.ComponentA)bc.getService(refs[0]);
        assertEquals("Should still get b1 version", "b1", a.getProp("bnum"));

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a0)");
        assertTrue("Should get one serviceRef to ComponentA with anum a0", refs != null && refs.length == 1);
        a = (org.knopflerfish.service.componentM_test.ComponentA)bc.getService(refs[0]);
        assertEquals("Should get b0 version", "b0", a.getProp("bnum"));

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a2)");
        assertTrue("Should get one serviceRef to ComponentA with anum a2", refs != null && refs.length == 1);
        a = (org.knopflerfish.service.componentM_test.ComponentA)bc.getService(refs[0]);
        assertEquals("Should get b2 version", "b2", a.getProp("bnum"));

        c1.delete();
        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", null);
        assertTrue("Should get one serviceRef to ComponentA", refs != null && refs.length == 1);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", "(anum=a2)");
        assertTrue("Should still get one serviceRef to ComponentA with anum a2", refs != null && refs.length == 1);
        a = (org.knopflerfish.service.componentM_test.ComponentA)bc.getService(refs[0]);
        assertEquals("Should still get b2 version", "b2", a.getProp("bnum"));

        c.delete();
        Thread.sleep(SLEEP_TIME);

        refs = bc.getServiceReferences("org.knopflerfish.service.componentM_test.ComponentA", null);
        assertNull("Shouldn't get serviceRef to ComponentA", refs);
     } catch (Exception e) {
        e.printStackTrace();
        fail("Test13: got unexpected exception " + e);
      } finally {
        if (b1 != null) {
          try {
            if (cmt != null) {
              deleteConfig(cmt.getService(), b1.getLocation());
              cmt.close();
            }
            b1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test13: got uninstall exception " + be);
          }
        }
      }
    }
  }

  private class Test14 extends FWTestCase implements LogListener {

    private boolean gotCircularError;

    public void logged(LogEntry le) {
      if (le.getLevel() == LogService.LOG_ERROR &&
          le.getMessage().indexOf("circular") >= 0) {
        gotCircularError = true;
      }
    }


    public void runTest() {
      Bundle c1 = null;
      ServiceReference<?> sr = null;
      LogReaderService lrs = null;

      try {
        gotCircularError = false;
        sr = bc.getServiceReference(LogReaderService.class.getName());
        lrs = (LogReaderService)bc.getService(sr);
        lrs.addLogListener(this);

        c1 = Util.installBundle(bc, "componentFilter_test-1.0.0.jar");
        c1.start();

        Thread.sleep(SLEEP_TIME);

        ServiceReference<?>[] ref = bc.getServiceReferences("org.knopflerfish.service.componentFilter_test.GeneralComponent", null);
        assertNull("Shouldn't get serviceRef GeneralComponent", ref);
        assertFalse("Shouldn't get circular message", gotCircularError);
      } catch (Exception e ) {
        fail("Got exception: Test1: " + e);
      } finally {
        if (lrs != null) {
          lrs.removeLogListener(this);
        }
        if (c1 != null) {
          try {
            c1.uninstall();
          } catch (Exception be) {
            be.printStackTrace();
            fail("Test14: got uninstall exception " + be);
          }
        }
      }
    }
  }
  


  public void deleteConfig(ConfigurationAdmin ca, String location) throws IOException, InvalidSyntaxException {
    Configuration [] cs = ca.listConfigurations("(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + location + ")");
    if (cs != null) {
      for (Configuration c : cs) {
        c.delete();
      }
    }
  }
}
