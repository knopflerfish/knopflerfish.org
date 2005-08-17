/*
 * Created on Aug 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario3.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.scr.scenarios.scenario3.Scenario3;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Martin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
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
        
        addTest(new Scenario3(context));
        
        /* add the clean up */
        addTest(new Cleanup());
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
   
    class Scenario3 extends TestCase implements EventHandler {
        /** class variable for service registration */
        private ServiceRegistration serviceRegistration;
        
        /** local variable holding the bundleContext */
        private BundleContext bundleContext;
        
        /** the bundles that are needed in the test */
        Bundle[] desiredBundles = new Bundle[4];

        /** the dummy bundle */
        private Bundle provider1;
        private Bundle provider2;
        private Bundle userEvent;
        private Bundle userLook;

        /**
         * Consturctor to initialize a dummy bundle 
         * @param context
         */
        public Scenario3(BundleContext context) {
            super("Scenario3");
            /* assign the context */
            bundleContext = context;
        }
        
        public synchronized void runTest() throws Throwable {
        	/* create the hashtable to put properties in */
            Hashtable props = new Hashtable();
            /* put service.pid property in hashtable */
            props.put(EventConstants.EVENT_TOPIC, "com/gstm/test/scr/scenarios/util/Whiteboard");
            /* register the service */
            serviceRegistration = bundleContext.registerService(
                    EventHandler.class.getName(), this, props);
            
            // Create list containing expected events
            LinkedList expectedEvents = new LinkedList();
            expectedEvents.addLast(new Integer(1));
            expectedEvents.addLast(new Integer(1));
            expectedEvents.addLast(new Integer(2));
            expectedEvents.addLast(new Integer(2));
            expectedEvents.addLast(new Integer(1));
            expectedEvents.addLast(new Integer(1));
            
            // Find expected bundles and start them in predetermined order
            findBundles();
            
            /* Start Scenario3UserEvent */
        	try{
        		desiredBundles[0].start();
        	}catch (Exception e){
        		assertFalse("Couldn't start the bundle:" + e, true);
        	} 
        	
        	/* Start Scenario3UserLook */
        	try{
        		desiredBundles[1].start();
        	}catch (Exception e){
        		assertFalse("Couldn't start the bundle:" + e, true);
        	}
        	
        	/* Start Scenario3Provider1 */
        	try{
        		desiredBundles[2].start();
        	}catch (Exception e){
        		assertFalse("Couldn't start the bundle:" + e, true);
        	}
        	
        	/* Start Scenario3Provider2 */
        	try{
        		desiredBundles[3].start();
        	}catch (Exception e){
        		assertFalse("Couldn't start the bundle:" + e, true);
        	}
        }

        private void findBundles(){
        	Bundle[] bundles = bundleContext.getBundles();
        	
        	for(int i=0; i<bundles.length ; i++){
        		Bundle bundle = bundles[i];
        		Dictionary headers = bundle.getHeaders();
        		String name = (String) headers.get("Bundle-Name");
        		//System.out.println("Found: "+ name);
        		if(name.equals("Scenario3Provider1")){
        			desiredBundles[2] = bundle;
        			System.out.println("Adding: Scenario3Provider1");
        		}else if(name.equals("Scenario3Provider2")){
        			desiredBundles[3] = bundle;
        			System.out.println("Adding: Scenario1Provider2");
        		} else if(name.equals("Scenario3UserLook")){
        			desiredBundles[1] = bundle;
        			System.out.println("Adding: Scenario3UserLook");
        		} else if(name.equals("Scenario3UserEvent")){
        			desiredBundles[0] = bundle;
        			System.out.println("Adding: Scenario3UserEvent");
        		}
        	}
        }
        
        /* Do the state changes here */
        private void stateMachine(int state){
        	switch (state) {
				case 1: 
					// do something
				break;
				case 2: 
					// do something else
				break;
			}
        }
        
		public void handleEvent(Event event) {
			System.out.println("This event was received: " + event.getTopic());
		}      
    }
}
