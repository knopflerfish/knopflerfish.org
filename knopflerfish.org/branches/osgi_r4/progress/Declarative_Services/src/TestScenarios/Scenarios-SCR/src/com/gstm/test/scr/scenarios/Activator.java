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
package com.gstm.test.scr.scenarios;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.gstm.test.scr.scenarios.scenario1.impl.Scenario1TestSuite;

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
		TestSuite testSuiteAllTests = new TestSuite("All Tests");


		/* create the test suite for scenario 1 */
		TestSuite testSuiteScenario1 = new Scenario1TestSuite(context);
		/* add scenario 1 to the all tests suite */
		testSuiteAllTests.addTest(testSuiteScenario1);

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