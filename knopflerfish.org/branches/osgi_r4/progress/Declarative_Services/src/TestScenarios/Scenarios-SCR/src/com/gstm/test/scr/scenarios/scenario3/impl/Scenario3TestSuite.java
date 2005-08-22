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
package com.gstm.test.scr.scenarios.scenario3.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.scr.scenarios.scenario3.Scenario3;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Martin Berg
 * 
 * Check that a component that changes properties is restarted and that 
 * the service ranking works correctly.
 */
public class Scenario3TestSuite extends TestSuite implements Scenario3 {

	/** bundle context variable */
	private BundleContext bundleContext;

	/**
	 * Constuructor for the TestSuite class.
	 * 
	 * @param context
	 *            the handle to the frame work
	 */
	public Scenario3TestSuite(BundleContext context) {
		/* call super class */
		super("Scenario 3");
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
		Bundle[] desiredBundles = new Bundle[4];

		/** Event counter */
		int eventCounter = 1;

		/** the dummy bundle */
		private Bundle provider1;

		private Bundle provider2;

		private Bundle userEvent;

		private Bundle userLook;

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
					System.out.println("Waiting");
					wait(40);
				} catch (Exception e) {
					System.out.println("Couldn't wait:" + e);
				}
			}

			// Find expected bundles and start them in predetermined order
			findBundles();

			/* Start Scenario3UserEvent */
			try {
				desiredBundles[0].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}

			/* Start Scenario3UserLook */
			try {
				desiredBundles[1].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}

			/* Start Scenario3Provider1 */
			try {
				desiredBundles[2].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}

			/* Start Scenario3Provider2 */
			try {
				desiredBundles[3].start();
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
				if (name.equals("Scenario3Provider1")) {
					desiredBundles[2] = bundle;
					System.out.println("Adding: Scenario3Provider1");
				} else if (name.equals("Scenario3Provider2")) {
					desiredBundles[3] = bundle;
					System.out.println("Adding: Scenario1Provider2");
				} else if (name.equals("Scenario3UserLook")) {
					desiredBundles[1] = bundle;
					System.out.println("Adding: Scenario3UserLook");
				} else if (name.equals("Scenario3UserEvent")) {
					desiredBundles[0] = bundle;
					System.out.println("Adding: Scenario3UserEvent");
				}
			}
		}

		public void handleEvent(Event event) {
			System.out.println("\nThis event was received: " + event.getTopic()
					+ "\n");

			int userEventValue = ((Integer) event.getProperty("com.gstm.test.scr.scenarios.scenario3.UserEventImpl")).intValue();
			int userLookValue = ((Integer) event.getProperty("com.gstm.test.scr.scenarios.scenario3.UserLookImpl")).intValue();

			//assertTrue("Only one value for each event, and no value should be 0",(userEventValue == 0 && userLookValue > 0) || (userEventValue > 0 && userLookValue == 0));
			
			switch (eventCounter) {
			case 2: //
				System.out.println("First or second message");
				if (userEventValue == 0) {
					if (userLookValue == 0) {
						/* no value in neither userEventValue or userLookValue */ 
						assertTrue("There needs to be a value in the event",
								false);
					}
					/* check that the value received is the expected value */
					assertEquals("Not the expected event value", userLookValue,
							1);
				} else {
					/* check that the value received is the expected value */
					assertEquals("Not the expected event value",
							userEventValue, 1);
				}
				eventCounter++;
				break;
			case 4:
				/* After the change of a property in ServiceProvider1 */
				System.out.println("Third or forth message");
				if (userEventValue == 0) {
					if (userLookValue == 0) {
						assertTrue("There needs to be a value in the event",
								false);
					}
					/* check that the value received is the expected value */
					assertEquals("Not the expected event value", userLookValue,
							2);
				} else {
					/* check that the value received is the expected value */
					assertEquals("Not the expected event value",
							userEventValue, 2);
				}
				eventCounter++;
				break;
			case 6:
				/* After the stop of the ServiceProvider2 */
				System.out.println("Fifth or sixth message");
				if (userEventValue == 0) {
					if (userLookValue == 0) {
						assertTrue("There needs to be a value in the event",
								false);
					}
					/* check that the value received is the expected value */
					assertEquals("Not the expected event value", userLookValue,
							3);
				} else {
					/* check that the value received is the expected value */
					assertEquals("Not the expected event value",
							userEventValue, 3);
				}
				eventCounter++;
				break;
			}
		}
	}
}
