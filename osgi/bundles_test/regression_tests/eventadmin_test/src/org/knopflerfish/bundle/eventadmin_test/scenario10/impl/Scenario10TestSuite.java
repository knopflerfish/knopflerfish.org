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
package org.knopflerfish.bundle.eventadmin_test.scenario10.impl;

import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.knopflerfish.bundle.eventadmin_test.util.Util;

/**
 * This class will assert that the event admin delivers bundle events
 * it will create an event generator which installs a bundle and performs
 * several actions on it, i.e, INSTALL,START,STOP,UPDATED,UNINSTALLED
 *
 * @author Magnus Klack
 *
 */
public class Scenario10TestSuite extends TestSuite {
    /** class variable holding the bundle context */
    private BundleContext bundleContext;

    /** variable indicating that consumers are present */
    private boolean consumerIsPresent = false;

    /** variable representing a sempahore for the consumer is present variable */
    public final Object semaphore = new Object();

    /**
     * Constructor for the Scenario9 TestSuite
     *
     * @param context the bundlecontext, i.e, the handle
     *         to the framework
     */
    public Scenario10TestSuite(BundleContext context) {
        /* call super class */
        super("Scenario 10");
        /* assign the bundle context to local variable */
        bundleContext = context;
        /* create a topic string array */
        String[] topics = { "org/osgi/framework/BundleEvent/*" };

        /* add the setup */
        addTest(new Setup());
        /* add the event consumer */
        EventConsumer[] eventConsumer = new EventConsumer[] {
          new EventConsumer(topics, "Scenario 10 Consumer", 1) };
        addTest(eventConsumer[0]);
        /*  add the event generator */
        addTest( new EventGenerator(bundleContext));
        /* add the clean up */
        addTest(new Cleanup(eventConsumer));

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
        private EventConsumer[] eventConsumer;

        public Cleanup(EventConsumer[] eventConsumer) {
            this.eventConsumer = eventConsumer;
        }
        public void runTest() throws Throwable {
          Throwable error = null;
          for (EventConsumer consumer : eventConsumer) {
            try {
              consumer.cleanup();
            } catch (Throwable e) {
              error = e;
            }
          }
          if (error != null) throw error;
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



    private class EventGenerator extends TestCase {
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

            try {
              bundle=Util.installBundle(bundleContext,"testlibs/ThreadTest.jar");
            } catch (Exception e) {
                fail("Can't install bundle make sure testlibs folder is "
                    + "added to the framework.jar file:" + e.getMessage());
            }

            /* must be conumers */
            Thread surveyThread = new Thread(() -> {
                try {
                    ServiceReference<?>[] references = null;
                    while (references == null) {
                        /* wait until consumers are present */
                        references = bundleContext
                                .getServiceReferences(
                                        "org.osgi.service.event.EventHandler",
                                        null);
                    }

                    /* lock the semaphore */
                    synchronized (semaphore) {
                        /* set the consumer as present */
                        consumerIsPresent = true;
                        semaphore.notifyAll();
                    }

                } catch (InvalidSyntaxException e) {
                    System.out
                            .println("\n************** FATAL ERROR IN SCENARIO 10 ***************\n");

                }
            });
            /* start wait process */
            surveyThread.start();

            /* wait for the semaphore to be true */
            while (!consumerIsPresent) {
                synchronized (semaphore) {
                    try {
                      semaphore.wait();
                    } catch (Exception ignored) {}
                }

            }

            /* start the bundle */
            bundle.start();
            /* stop the bundle */
            bundle.stop();
            /* update the bundle */
            Util.updateBundle(bundleContext, bundle);
            //bundle.update();
            /* uninstall the bundle */
            bundle.uninstall();

            try {
              Thread.sleep(500); // allow for delivery
            } catch (Exception ignore) {}
        }
    }

    /**
     * Class consumes events
     *
     * @author Magnus Klack
     */
    private class EventConsumer extends TestCase implements EventHandler {
        /** variable holding the topics to consume */
        private String[] topicsToConsume;

        /** variable holding the instance name */
        private String displayName;

        /** variable counting  events */
        private int eventCounter = 0;
        private ServiceRegistration<EventHandler> serviceRegistration;

        private Throwable error;

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
          assertTrue("Not all events received (" + eventCounter + "/7)", eventCounter==7);
        }

        public void handleEvent(Event event) {
          try {
            /*
             * use the case statement to determine that the right event has arrived
             * if not an assertment error will occurr.
             */
            System.out.println(displayName + " received topic:" + event.getTopic());

            switch (eventCounter) {
            case 0:
                assertEquals("Wrong topic!",
                             "org/osgi/framework/BundleEvent/INSTALLED",
                             event.getTopic());
                break;
            case 1:
                assertEquals("Wrong topic!",
                             "org/osgi/framework/BundleEvent/RESOLVED",
                             event.getTopic());
                break;
            case 2:
                assertEquals("Wrong topic!",
                             "org/osgi/framework/BundleEvent/STARTED",
                             event.getTopic());
                break;
            case 3:
                assertEquals("Wrong topic!",
                             "org/osgi/framework/BundleEvent/STOPPED",
                             event.getTopic());
                break;
            case 4:
                assertEquals("Wrong topic!",
                             "org/osgi/framework/BundleEvent/UNRESOLVED",
                             event.getTopic());
                break;
            case 5:
                assertEquals("Wrong topic!",
                             "org/osgi/framework/BundleEvent/UPDATED",
                             event.getTopic());
                break;
            case 6:
                assertEquals("Wrong topic!",
                             "org/osgi/framework/BundleEvent/UNINSTALLED",
                             event.getTopic());
                break;
            default:
                fail("Order not granted in event admin service");
            }
            eventCounter++;
          } catch (RuntimeException e) {
            error = e;
            throw e;
          } catch (Throwable e) {
            error = e;
          }

        }

    }
}
