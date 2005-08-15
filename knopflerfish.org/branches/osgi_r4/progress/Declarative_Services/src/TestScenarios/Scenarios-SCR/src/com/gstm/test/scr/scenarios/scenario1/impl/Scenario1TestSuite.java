/*
 * Created on Aug 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario1.impl;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.gstm.test.scr.scenarios.scenario1.Scenario1;
import com.gstm.test.scr.scenarios.util.Util;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Martin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Scenario1TestSuite extends TestSuite implements Scenario1 {

    /** bundle context variable */
    private BundleContext bundleContext;
    
    /**
     * Constuructor for the TestSuite class.
     * 
     * @param context
     *            the handle to the frame work
     */
    public Scenario1TestSuite(BundleContext context) {
        /* call super class */
        super("Scenario 1");
        /* assign the bundle context to glocal variable */
        bundleContext = context;
        /* add the setup */
        addTest(new Setup());
        
        addTest(new Scenario1(context));
        
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
   
    class Scenario1 extends TestCase {
        /** local variable holding the bundleContext */
        private BundleContext bundleContext;

        /** the dummy bundle */
        private Bundle bundle1;
        private Bundle bundle2;
        private Bundle bundle3;

        /**
         * Consturctor to initialize a dummy bundle 
         * @param context
         */
        public Scenario1(BundleContext context) {
            super("Scenario1");
            /* assign the context */
            bundleContext = context;
        }

        
        public void runTest() throws Throwable {
        	Bundle[] desiredBundles = new Bundle[3];
        	int amoutOfServices;
					
        	Bundle[] bundles = bundleContext.getBundles();
        	
        	for(int i=0; i<bundles.length ; i++){
        		Bundle bundle = bundles[i];
        		Dictionary headers = bundle.getHeaders();
        		String name = (String) headers.get("Bundle-Name");
        		System.out.println("Found: "+ name);
        		if(name.equals("Scenario1Bundle1")){
        			desiredBundles[0] = bundle;
        			System.out.println("Adding: Scenario1Bundle1");
        		}else if(name.equals("Scenario1Bundle2")){
        			desiredBundles[0] = bundle;
        			System.out.println("Adding: Scenario1Bundle2");
        		} else if(name.equals("Scenario1Bundle3")){
        			desiredBundles[0] = bundle;
        			System.out.println("Adding: Scenario1Bundle3");
        		}
        	}
        	
        	/* Start the first bundle */
        	System.out.println("Starting Scenario1Bundle1");
        	desiredBundles[0].start();
        	amoutOfServices = bundleContext.getServiceReferences(null, null).length;
        	System.out.println("The number of services currently avalible is: " + amoutOfServices);
        	System.out.println("Starting the old style bundles");
        	desiredBundles[1].start();
        	desiredBundles[2].start();
        	System.out.println("Now the number of services is: " + bundleContext.getServiceReferences(null, null).length);
        	assertTrue("There wasn't 2 more services avalible", amoutOfServices+2 == bundleContext.getServiceReferences(null, null).length);
        }      
    }
}
