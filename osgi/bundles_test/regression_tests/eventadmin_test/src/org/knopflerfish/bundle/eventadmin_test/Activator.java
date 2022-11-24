/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.eventadmin_test;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

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
 * Entry class for the Scenario 1 test case specified in test specification for
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
    public void start(BundleContext context) {
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
        Hashtable<String, Object> props = new Hashtable<>();
        /* put service.pid property in hashtable */
        props.put("service.pid", testSuiteAllTests.getName());
        /* register service with the suite for all tests */
        context.registerService(
                TestSuite.class.getName(), testSuiteAllTests, props);

    }

    /**
     * Start the bundle
     *
     * @param context
     *            the bundle context
     */
    public void stop(BundleContext context) {
    }
}
