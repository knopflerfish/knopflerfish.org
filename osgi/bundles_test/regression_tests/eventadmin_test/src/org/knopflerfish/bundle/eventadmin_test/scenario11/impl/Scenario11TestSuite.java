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
package org.knopflerfish.bundle.eventadmin_test.scenario11.impl;

import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.knopflerfish.bundle.eventadmin_test.util.Util;

/**
 * This class will assert that the event admin delivers service events
 * it will create an event generator which installs a bundle that registers
 * a service and performs several actions on it, i.e,
 * REGISTERED, MODIFIED, UNREGISTERING
 *
 * @author Magnus Klack, Martin Berg
 *
 */
public class Scenario11TestSuite extends TestSuite {
    /** class variable holding the bundle context */
    private BundleContext bundleContext;

    /**
     * Constructor for the Scenario11 TestSuite
     *
     * @param context the bundlecontext, i.e, the handle
     *         to the framework
     */
    public Scenario11TestSuite(BundleContext context) {
        /* call super class */
        super("Scenario 11");
        /* assign the bundle context to local variable */
        bundleContext = context;
        /* create a topic string array */
        String[] topics = { "org/osgi/framework/ServiceEvent/*" };
        /* add the setup */
        addTest(new Setup());
        /* add the event consumer */
        EventConsumer eventConsumer = new EventConsumer(topics, "Scenario 11 Consumer", 1);
        addTest(eventConsumer);
        /*  add the event generator */
        EventGenerator eventGenerator = new EventGenerator(bundleContext);
        addTest(eventGenerator);
        /* add the clean up */
        addTest(new Cleanup(eventConsumer, eventGenerator));

    }

    /**
     * the set up class add listeners and stuff here
     *
     * @author Magnus Klack
     */
    static class Setup extends TestCase {
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
     * the Clean up class
     *
     * @author Magnus Klack
     */
    class Cleanup extends TestCase {
        private EventConsumer eventConsumer;
        private EventGenerator eventGenerator;
        public Cleanup(EventConsumer eventConsumer, EventGenerator eventGenerator) {
            this.eventConsumer = eventConsumer;
            this.eventGenerator = eventGenerator;
        }
        public void runTest() throws Throwable {
            eventConsumer.cleanup();
            eventGenerator.cleanup();
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

    private static class EventGenerator extends TestCase {
        /** local variable holding the bundleContext */
        private BundleContext bundleContext;

        /** the dummy bundle */
        private Bundle bundle;

        /**
         * Consturctor to initialize a dummy bundle
         */
        public EventGenerator(BundleContext context) {
            super("Event generator");
            /* assign the context */
            bundleContext = context;
        }

        public void runTest() throws Throwable {

          /*Wait to make sure that the consumer are pressent*/
          synchronized(this){
            try{
              wait(5000);
            } catch (Exception e){
              fail("Can't wait:" + e.getMessage());
            }

            try {
                /* install the bundle **/
              bundle = Util.installBundle(bundleContext, "testlibs/TestServiceBundle.jar");
              //assertNotNull("Install bundle is shall not be null",bundle);
              //bundle = Util.installBundle(bundleContext, "testlibs/EventConsumer1.jar");
            } catch (Exception e) {
                fail("Can't install bundle:" + e.getMessage());
            }

            /* start the bundle */
            System.out.println("Starting the TestServiceBundle");
            try {
              bundle.start();
            } catch (Exception e){
              fail("Can't start bundle:" + e.getMessage());
            }
            /* stop the bundle */
            System.out.println("Stopping the TestServiceBundle");
            try {
              bundle.stop();
            } catch (Exception e){
              fail("Can't stop bundle:" + e.getMessage());
            }
          }
          try {
            Thread.sleep(500); // allow for delivery
          } catch (Exception ignore) {}
        }

        public void cleanup() {
          try {
            bundle.uninstall();
          } catch (BundleException ignore) {}
        }

    }

    /**
     * Class consumes events
     *
     * @author Magnus Klack, Martin Berg
     */
    private class EventConsumer extends TestCase implements EventHandler {
        /** variable holding the topics to consume */
        private String[] topicsToConsume;

        /** variable holding the instance name */
        private String displayName;

        /** variable counting  events */
        private int eventCounter = 0;

        /** variable counting number of REGISTERED Events */
        private int registeredCounter = 0;

        /** variable counting number of MODIFIED Events */
        private int modifiedCounter = 0;

        /** variable counting number of UNREGISTERING Events */
        private int unregisteringCounter = 0;

        private ServiceRegistration<EventHandler> serviceRegistration;

        private Throwable error;

        /* The topics that the consumer should receive*/
        String registered = "org/osgi/framework/ServiceEvent/REGISTERED";
        String modified = "org/osgi/framework/ServiceEvent/MODIFIED";
        String unregistering = "org/osgi/framework/ServiceEvent/UNREGISTERING";

        public EventConsumer(String[] topics, String name, int id) {
            /* call the super class */
            super(name + " " + id);
            /* create a display name */
            displayName = name + ":" + id;
            /* assign the topicsToConsume variable */
            topicsToConsume = topics;
        }

        public void runTest() throws Throwable {
            eventCounter = 0;
            registeredCounter = 0;
            modifiedCounter = 0;
            unregisteringCounter = 0;
            /* create the hashtable to put properties in */
            Hashtable<String, Object> props = new Hashtable<>();
            /* put service.pid property in hashtable */
            props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
            /* register the service */
            serviceRegistration = bundleContext.registerService(
                EventHandler.class, this, props);
            /* assert not null */
            assertNotNull(displayName + " Can't get service", serviceRegistration);
        }

        public void cleanup() throws Throwable {
          try {
            serviceRegistration.unregister();
          } catch (IllegalStateException ignore) {}
          if (error != null) {
            throw error;
          }
          assertTrue("No " + registered + " received", registeredCounter > 0);
          assertTrue("No " + modified + " received", modifiedCounter > 0);
          assertTrue("No " + unregistering + " received", unregisteringCounter > 0);
        }

        public void handleEvent(Event event) {
          try {
            System.out.println(displayName + " received topic:"
                    + event.getTopic());

            eventCounter++;

            if(event.getTopic().equals(registered)){
              registeredCounter++;
              System.out.println("The number of " + registered + " events received is:" + registeredCounter);
            } else if(event.getTopic().equals(modified)){
              modifiedCounter++;
              System.out.println("The number of " + modified + " events received is:" + modifiedCounter);
            } else if(event.getTopic().equals(unregistering)){
              unregisteringCounter++;
              System.out.println("The number of " + unregistering + " events received is:" + unregisteringCounter);
            } else {
              fail("Only REGISTERED, MODIFIED or UNREGISTERING events are to be received");
            }

            System.out.println("Number of received events:\t" + eventCounter + "\nNumber of received REGISTERED events:\t" +
                registeredCounter + "\nNumber of received MODIFIED events:\t" + modifiedCounter +
                "\nNumber of received UNREGISTERING events:\t" + unregisteringCounter);

          } catch (RuntimeException e) {
            error = e;
            throw e;
          } catch (Throwable e) {
            error = e;
          }
        }
    }
}
