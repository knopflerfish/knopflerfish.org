/*
 * @(#)Scenario8TestSuite.Java        1.0 2005/08/23
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
package com.gstm.test.scr.scenarios.scenario8.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.scr.scenarios.scenario8.Scenario8;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Martin Berg
 * 
 * Check that a component that changes properties is restarted and that the
 * service ranking works correctly.
 */
public class Scenario8TestSuite extends TestSuite implements Scenario8 {

	/** bundle context variable */
	private BundleContext bundleContext;

	/**
	 * Constuructor for the TestSuite class.
	 * 
	 * @param context
	 *            the handle to the frame work
	 */
	public Scenario8TestSuite(BundleContext context) {
		/* call super class */
		super("Scenario 8");
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
			super("Scenario8");
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
					System.out.println("Scenario 8: Waiting......");
					wait(40);
				} catch (Exception e) {
					System.out.println("Scenario 8: Couldn't wait:" + e);
				}
			}

			// Find expected bundles and start them in predetermined order
			findBundles();
			/* Make sure that all bundles needed were found */
			for(int i=0 ; i<desiredBundles.length ; i++){
				assertNotSame("Not all bundles needed in the test were found", null, desiredBundles[i]);
			}

			/* Start Scenario6Bundle1 */
			try {
				desiredBundles[0].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}
			/* Start Scenario6Bundle2 */
			try {
				desiredBundles[1].start();
			} catch (Exception e) {
				assertFalse("Couldn't start the bundle:" + e, true);
			}
			/* Start Scenario6FaultyProvider */
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
				if (name.equals("Scenario8Bundle1")) {
					desiredBundles[0] = bundle;
					System.out.println("Adding: " + name);
				}else if (name.equals("Scenario8Bundle2")){
					desiredBundles[1] = bundle;
					System.out.println("Adding: " + name);					
				}else if (name.equals("Scenario8FaultyProvider")){
					desiredBundles[2] = bundle;
					System.out.println("Adding: " + name);					
				}
			}
		}

		public void handleEvent(Event event) {

			synchronized (this){
				/* The key to be used to get a value from the event */
				String component1Name = "com.gstm.test.scr.scenarios.scenario8.component1.Component1";
				String component2Name = "com.gstm.test.scr.scenarios.scenario8.component2.Component2";
				
				int component1Value;
				int component2Value;
				
				/* Getting the value from the event, if no value is found the variable value is set to 0. */
				try{
					component1Value = ((Integer) event.getProperty(component1Name)).intValue();
				}catch (Exception e){
					component1Value = 0;
				}
				
				try{
					component2Value = ((Integer) event.getProperty(component2Name)).intValue();
				}catch (Exception e){
					component2Value = 0;
				}
				
				/* Testing the values against the expected values, see scenario8 in the STS */
				switch (eventCounter) {
					case 1: 
						/* assert that that component1 is started */
						System.out.println("Event" + eventCounter + " -- Component1:"+ component1Value + " Component2:" + component2Value);
						assertEquals("Component2 should be the first thing to generate and event", 10, component2Value);
					break;
					case 2:
						System.out.println("Event" + eventCounter + " -- Component1:"+ component1Value + " Component2:" + component2Value);
						/* assert that that component2 is started */
						assertEquals("Component1 should be the second thing to generate and event", 10, component1Value);
					break;
					default:
						/* assert that that no more events are generated */
						System.out.println("Event" + eventCounter + " -- Component1:"+ component1Value + " Component2:" + component2Value);
						if(component1Value != 0 || component2Value != 0){
							assertTrue("Component1 or Component2 has generated an event that it should not have.", false);
						}
					break;
				}
				eventCounter++;
			}
			
		}
	}
}
