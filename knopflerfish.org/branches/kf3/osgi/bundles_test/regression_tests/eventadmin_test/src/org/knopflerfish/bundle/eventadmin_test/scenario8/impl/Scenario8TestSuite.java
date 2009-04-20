/*
 * @(#)Scenario1TestSuite.java        1.0 2005/06/28
 *
 * Copyright (c) 2003-2005 Gatespace telematics AB
 * Otterhallegatan 2, 41670,Gothenburgh, Sweden.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Gatespace telematics AB. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Gatespace telematics AB.
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
