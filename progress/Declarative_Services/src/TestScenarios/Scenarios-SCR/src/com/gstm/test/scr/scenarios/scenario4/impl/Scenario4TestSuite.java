/*
 * @(#)Scenario3TestSuite.Java        1.0 2005/08/15
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
package com.gstm.test.scr.scenarios.scenario4.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.scr.scenarios.scenario4.Scenario4;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Martin Berg
 * 
 * Check that a component that changes properties is restarted and that the
 * service ranking works correctly.
 */
public class Scenario4TestSuite extends TestSuite implements Scenario4 {

	/** bundle context variable */
	private BundleContext bundleContext;

	/**
	 * Constuructor for the TestSuite class.
	 * 
	 * @param context
	 *            the handle to the frame work
	 */
	public Scenario4TestSuite(BundleContext context) {
		/* call super class */
		super("Scenario 4");
		/* assign the bundle context to glocal variable */
		bundleContext = context;
		/* add the setup */
		addTest(new Setup());

		addTest(new TestStarter(context));

		/* add the clean up */
		addTest(new Cleanup());
	}

	/**
	 * Sets up neccessary environment
	 * 
	 * @author Magnus Klack
	 */
	class Setup extends TestCase {
		public Setup() {

		}

		public void runTest() throws Throwable {

		}

		public String getName() {
			String name = getClass().getName();
			int ix = name.lastIndexOf("$");
			if (ix == -1) {
				ix = name.lastIndexOf(".");
			}
			if (ix != -1) {
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
		public void runTest() throws Throwable {

		}

		public String getName() {
			String name = getClass().getName();
			int ix = name.lastIndexOf("$");
			if (ix == -1) {
				ix = name.lastIndexOf(".");
			}
			if (ix != -1) {
				name = name.substring(ix + 1);
			}
			return name;
		}
	}

	class TestStarter extends TestCase implements EventHandler {
		/** class variable for service registration */
		private ServiceRegistration serviceRegistration;

		/** local variable holding the bundleContext */
		private BundleContext bundleContext;

		/** the bundles that are needed in the test */
		Bundle[] desiredBundles = new Bundle[1];

		/** Event counter */
		int eventCounter = 1;

		/**
		 * Consturctor to initialize a dummy bundle
		 * 
		 * @param context
		 */
		public TestStarter(BundleContext context) {
			super("Scenario3");
			/* assign the context */
			bundleContext = context;
		}

		public synchronized void runTest() throws Throwable {
			/* create the hashtable to put properties in */
			Hashtable props = new Hashtable();
			/* put service.pid property in hashtable */
			String[] topic = { "com/gstm/test/scr/scenarios/util/Whiteboard" };
			props.put(EventConstants.EVENT_TOPIC, topic);
			/* register the service */
			serviceRegistration = bundleContext.registerService(
					EventHandler.class.getName(), this, props);

			assertNotNull(getName()
					+ " service registration should not be null",
					serviceRegistration);

			if (serviceRegistration == null) {
				fail("Could not get Service Registration ");
			}

			synchronized (this) {
				try {
					System.out.println("Scenario 4: Waiting......");
					wait(40);
				} catch (Exception e) {
					System.out.println("Scenario 4: Couldn't wait:" + e);
				}
			}

			// Find expected bundles and start them in predetermined order
			findBundles();
			/* Make sure that all bundles needed were found */
			for(int i=0 ; i<desiredBundles.length ; i++){
				assertNotSame("Not all bundles needed in the test were found", null, desiredBundles[i]);
			}

			/* Start Scenario4Bundle1 */
			try {
				desiredBundles[0].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}
		}

		private void findBundles() {
			Bundle[] bundles = bundleContext.getBundles();

			for (int i = 0; i < bundles.length; i++) {
				Bundle bundle = bundles[i];
				Dictionary headers = bundle.getHeaders();
				String name = (String) headers.get("Bundle-Name");
				//System.out.println("Found: "+ name);
				if (name.equals("Scenario4Bundle1")) {
					desiredBundles[0] = bundle;
					System.out.println("Adding: " + name);
				}
			}
		}

		public void handleEvent(Event event) {
			/* The key to be used to get a value from the event */
			String immediateComponent2Name = "com.gstm.test.scr.scenarios.scenario4.immediatecomponent2.ImmediateComponent2";
			String immediateComponent3Name = "com.gstm.test.scr.scenarios.scenario4.immediatecomponent3.ImmediateComponent3";

			int immediate2Value;
			int immediate3Value;
			
			/* Getting the value from the event, if no value is found the variable value is set to 0. */
			try{
				immediate2Value = ((Integer) event.getProperty(immediateComponent2Name)).intValue();
			}catch (NullPointerException e){
				immediate2Value = 0;
			}
			
			try{
				immediate3Value = ((Integer) event.getProperty(immediateComponent3Name)).intValue();
			}catch (NullPointerException e){
				immediate3Value = 0;
			}
			
			/* Testing the values against the expected values, see scenario4 in the STS */
			switch (eventCounter) {
			case 1: //
				System.out.println("First event:immediate2Value:"
						+ immediate2Value);

				assertEquals("The value from Component2 should be 1", 1, immediate2Value);
				eventCounter++;
				break;
			case 2: //
				System.out.println("Second event:immediate3Value:"
						+ immediate3Value);

				assertEquals("The value from Component3 should be 1", 1, immediate3Value);
				eventCounter++;
				break;
			default:
				System.out.println("No matching event");
				assertTrue("No matching event", false);
			}
		}
	}
}
