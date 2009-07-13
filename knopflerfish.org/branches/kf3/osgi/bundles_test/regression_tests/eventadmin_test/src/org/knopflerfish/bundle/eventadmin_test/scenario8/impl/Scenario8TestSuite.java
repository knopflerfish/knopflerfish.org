/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
package org.knopflerfish.bundle.eventadmin_test.scenario8.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import org.knopflerfish.bundle.eventadmin_test.scenario8.Scenario8;
import org.knopflerfish.bundle.eventadmin_test.util.Util;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite for testing the requirements specified in the test specification
 * for the EventAdmin service.
 *
 * This test check the ServicePermission and the TopicPermission
 * functionality
 *
 * Resources needed:
 * The following jar files needs to be placed in the framework.jar file
 * under testlibs/
 * Also a policy file need to be constructed that follow the pattern
 * below.
 *
 * EventConsumer1.jar - no ServicePermission and no TopicPermission
 * EventConsumer2.jar - no TopicPermission
 * EventConsumer3.jar - no ServicePermission
 * EventConsumer4.jar
 *
 * EventProducer1.jar - no ServicePermission and no TopicPermission
 * EventProducer2.jar - no TopicPermission
 * EventProducer3.jar - no ServicePermission
 * EventProducer4.jar
 *
 * @author Martin Berg
 *
 */
public class Scenario8TestSuite extends TestSuite implements Scenario8 {
  /** bundle context variable */
  BundleContext bundleContext;

  /* A bundle variable*/
  //Bundle buU;

  /**
   * Constructor for the TestSuite class.
   *
   * @param context
   *            the handle to the frame work
   */
  public Scenario8TestSuite(BundleContext context) {
    super("Scenario 8");
    /* assign the bundelContext variable */
    bundleContext = context;

    /* add the setup */
    addTest(new Setup());

    ExternalJAR[] externalJAR = new ExternalJAR[] {
      new ExternalJAR(bundleContext, "Scenario 8 EventConsumer1", 8, "testlibs/EventConsumer1.jar"),
      new ExternalJAR(bundleContext, "Scenario 8 EventConsumer2", 8, "testlibs/EventConsumer2.jar"),
      new ExternalJAR(bundleContext, "Scenario 8 EventConsumer3", 8, "testlibs/EventConsumer3.jar"),
      new ExternalJAR(bundleContext, "Scenario 8 EventConsumer4", 8, "testlibs/EventConsumer4.jar"),
      new ExternalJAR(bundleContext, "Scenario 8 EventPublisher1", 8, "testlibs/EventProducer1.jar"),
      new ExternalJAR(bundleContext, "Scenario 8 EventPublisher2", 8, "testlibs/EventProducer2.jar"),
      new ExternalJAR(bundleContext, "Scenario 8 EventPublisher3", 8, "testlibs/EventProducer3.jar"),
      new ExternalJAR(bundleContext, "Scenario 8 EventPublisher4", 8, "testlibs/EventProducer4.jar") };

    /* add the external Event consumers */

    addTest(externalJAR[0]);
    addTest(externalJAR[1]);
    addTest(externalJAR[2]);
    addTest(externalJAR[3]);

    /* add the external Event Publishers */
    addTest(externalJAR[4]);
    addTest(externalJAR[5]);
    addTest(externalJAR[6]);
    addTest(externalJAR[7]);
    /* add the clean up */
    addTest(new Cleanup(externalJAR));
  }

  /**
   * Sets up neccessary environment
   *
   *@author Magnus Klack
   */
  class Setup extends TestCase {
    public Setup(){

    }
    public void runTest() throws Throwable {

    }
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

  /**
   * Clean up the test suite
   *
   * @author Magnus Klack
   */
  class Cleanup extends TestCase {
    private ExternalJAR[] externalJAR;

    public Cleanup(ExternalJAR[] externalJAR) {
      this.externalJAR = externalJAR;
    }
    public void runTest() throws Throwable {
      for (int i=0; i<externalJAR.length; i++) {
        externalJAR[i].cleanup();
      }
    }
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

  class ExternalJAR extends TestCase{
    /* The bundle xontext */
    BundleContext bundleContext;
    /* The path to the jar file to be installed*/
    String pathToJAR;
    Bundle buU;

    public ExternalJAR(BundleContext bc, String name, int id, String path) {
      /* call super class */
      super(name + ":" + id);

      /* assign the instance bc */
      bundleContext = bc;

      /* assign the instance path */
      pathToJAR = path;
    }

    public void runTest() throws Throwable {
      /* install the bundle */

      buU = Util.installBundle(bundleContext, pathToJAR);
      buU.start();
    }

    public void cleanup() {
      try {
        buU.uninstall();
      } catch (BundleException ignore) {}
    }
  }
}
