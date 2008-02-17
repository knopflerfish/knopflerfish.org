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

}
