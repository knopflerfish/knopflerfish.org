/*
 * @(#)Scenario6TestSuite.Java        1.0 2005/08/15
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
package com.gstm.test.scr.scenarios.scenario6.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.scr.scenarios.scenario6.Scenario6;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Martin Berg
 * 
 * Check that a component that changes properties is restarted and that the
 * service ranking works correctly.
 */
public class Scenario6TestSuite extends TestSuite implements Scenario6 {

	/** bundle context variable */
	private BundleContext bundleContext;

	/**
	 * Constuructor for the TestSuite class.
	 * 
	 * @param context
	 *            the handle to the frame work
	 */
	public Scenario6TestSuite(BundleContext context) {
		/* call super class */
		super("Scenario 6");
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
		Bundle[] desiredBundles = new Bundle[3];

		/** Event counter */
		int eventCounter = 1;

		/**
		 * Consturctor to initialize a dummy bundle
		 * 
		 * @param context
		 */
		public TestStarter(BundleContext context) {
			super("Scenario6");
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
					System.out.println("Scenario 6: Waiting......");
					wait(40);
				} catch (Exception e) {
					System.out.println("Scenario 6: Couldn't wait:" + e);
				}
			}

			// Find expected bundles and start them in predetermined order
			findBundles();

			/* Start Scenario6Component1 */
			try {
				desiredBundles[0].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}
			
			/* Start Scenario6Component2 */
			try {
				desiredBundles[1].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}
			
			/* Start Scenario6CounterFactory */
			try {
				desiredBundles[2].start();
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
				if (name.equals("Scenario6Component1")) {
					desiredBundles[0] = bundle;
					System.out.println("Adding: " + name);
				}else if (name.equals("Scenario6Component2")){
					desiredBundles[1] = bundle;
					System.out.println("Adding: " + name);					
				}else if (name.equals("Scenario6CounterFactory")){
					desiredBundles[2] = bundle;
					System.out.println("Adding: " + name);					
				}
			}
		}

		public void handleEvent(Event event) {
			
			String component1Name = "com.gstm.test.scr.scenarios.scenario6.component1.Component1";
			String component2Name = "com.gstm.test.scr.scenarios.scenario6.component2.Component2";
			
			int component1Value;
			int component2Value;
			
			try{
				component1Value = ((Integer) event.getProperty(component1Name)).intValue();
			}catch (NullPointerException e){
				component1Value = 0;
			}
			
			try{
				component2Value = ((Integer) event.getProperty(component2Name)).intValue();
			}catch (NullPointerException e){
				component2Value = 0;
			}
			
			/*assert that the values isn't higher than 50 on each */ 
			if(component1Value == 0){
				if(component2Value == 0){
					/* Something is wrong */
					assertTrue("Neither component1 or component2 has a value", false);
				}else{
					/* assert that the value in component2 isn't higher than 50 */
					assertTrue("The value of component2 is higher than 50", component2Value < 51);
				}
			} else {
				/* assert that the value in component1 isn't higher than 50 */
				assertTrue("The value of component1 is higher than 50", component1Value < 51);
			}
		}
	}
}
