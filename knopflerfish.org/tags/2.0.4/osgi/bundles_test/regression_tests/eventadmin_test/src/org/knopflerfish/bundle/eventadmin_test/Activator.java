/*
 * @(#)Activator.java        1.0 2005/06/28
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
package org.knopflerfish.bundle.eventadmin_test;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.knopflerfish.bundle.eventadmin_test.scenario1.impl.Scenario1TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario2.impl.Scenario2TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario3.impl.Scenario3TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario4.impl.Scenario4TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario5.impl.Scenario5TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario6.impl.Scenario6TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario7.impl.Scenario7TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario8.impl.Scenario8TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario10.impl.Scenario10TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario11.impl.Scenario11TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario12.impl.Scenario12TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario13.impl.Scenario13TestSuite;
import org.knopflerfish.bundle.eventadmin_test.scenario14.impl.Scenario14TestSuite;

import junit.framework.*;

/**
 * Entry class for the Scenario 1 test case specified in test specifiacation for
 * EventAdmin service feature.
 *
 * IMPORTANT! This test require the eventadmin service to be installed and
 * active within the framework.
 *
 * @author Magnus Klack
 */
public class Activator implements BundleActivator {

    /**
     * Start the bundle
     *
     * @param context
     *            the bundle context, i.e, the handle to framework
     */
    public void start(BundleContext context) throws Exception {
        /* create the All tests suite */
         TestSuite testSuiteAllTests = new TestSuite("EventAdminTestSuite");

        TestSuite testSuiteScenario1 = new Scenario1TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario1);
        TestSuite testSuiteScenario2 = new Scenario2TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario2);
        TestSuite testSuiteScenario3 = new Scenario3TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario3);
        TestSuite testSuiteScenario4 = new Scenario4TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario4);
        TestSuite testSuiteScenario5 = new Scenario5TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario5);
        TestSuite testSuiteScenario6 = new Scenario6TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario6);
        TestSuite testSuiteScenario7 = new Scenario7TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario7);
        TestSuite testSuiteScenario8 = new Scenario8TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario8);
        TestSuite testSuiteScenario10 = new Scenario10TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario10);
        TestSuite testSuiteScenario11 = new Scenario11TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario11);
        TestSuite testSuiteScenario12 = new Scenario12TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario12);
        TestSuite testSuiteScenario13 = new Scenario13TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario13);
        TestSuite testSuiteScenario14 = new Scenario14TestSuite(context);
        testSuiteAllTests.addTest(testSuiteScenario14);

        /* create the hashtable to put properties in */
        Hashtable props = new Hashtable();
        /* put service.pid property in hashtable */
        props.put("service.pid", testSuiteAllTests.getName());
        /* register service with the suite for all tests */
        ServiceRegistration serviceRegistration = context.registerService(
                TestSuite.class.getName(), testSuiteAllTests, props);

    }

    /**
     * Start the bundle
     *
     * @param context
     *            the bundle context
     */
    public void stop(BundleContext context) throws Exception {
    }
}
