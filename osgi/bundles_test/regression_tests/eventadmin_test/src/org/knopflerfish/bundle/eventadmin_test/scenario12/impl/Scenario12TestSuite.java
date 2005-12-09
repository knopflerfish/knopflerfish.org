/*
 * @(#)Scenario10TestSuite.java        1.0 2005/06/28
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
package org.knopflerfish.bundle.eventadmin_test.scenario12.impl;

import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class will assert that the event admin delivers bundle events
 * it will create an event generator which installs a bundle and performs
 * several actions on it, i.e, INSTALL,START,STOP,UPDATED,UNINSTALLED
 *
 * @author Magnus Klack
 *
 */
public class Scenario12TestSuite extends TestSuite {
    private static final String ERROR_MSG = "Testing LOG_ERROR";
    private static final String WARN_MSG = "Testing LOG_WARNING";
    private static final String INFO_MSG = "Testing LOG_INFO";
    private static final String DEBUG_MSG = "Testing LOG_DEBUG";

    /** class variable holding the bundle context */
    private BundleContext bundleContext;

    /** variable indicating that consumers are present */
    private boolean consumerIsPresent = false;

    /** variable representing a sempahore for the consumer is present variable */
    public Object semaphore = new Object();

    EventGenerator eventGenerator;

    /**
     * Constructor for the Scenario9 TestSuite
     *
     * @param context the bundlecontext, i.e, the handle
     *         to the framework
     */
    public Scenario12TestSuite(BundleContext context) {
        /* call super class */
        super("Scenario 12");
        /* assign the bundle context to local variable */
        bundleContext = context;
        /* create a topic string array */
        String[] topics = new String[] { "org/osgi/service/log/LogEntry/*" };

        /* add the setup */
        addTest(new Setup());
        /* add the event consumer */
        EventConsumer[] eventConsumer = new EventConsumer[] {
          new EventConsumer(topics, "Scenario 12 Consumer", 1) };
        addTest(eventConsumer[0]);
        /*  add the event generator */
        addTest(new EventGenerator(bundleContext));
        /* add the clean up */
        addTest(new Cleanup(eventConsumer));

    }

    /**
     * the set up class add listeners and stuff here
     *
     * @author Magnus Klack
     */
    class Setup extends TestCase {
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
          for (int i=0; i<eventConsumer.length; i++) {
            try {
              eventConsumer[i].cleanup();
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
        /** the log */
        private LogRef log;

        /**
         * Consturctor to initialize a dummy bundle
         * @param context
         */
        public EventGenerator(BundleContext context) {
          /* call the super class */
            super("Event generator");
            /* assign the log */
            log = new LogRef(context);
            assertNotNull(getName()+ " can't get the log service",log);
            bundleContext=context;
        }

        public void runTest() throws Throwable {
          try{
            //while(bundleContext.getServiceReference(EventHandler.class.getName()) == null){
              Thread.sleep(300);
            //}
            log.error(ERROR_MSG);
            Thread.sleep(200);
            log.warn(WARN_MSG);
            Thread.sleep(200);
            log.info(INFO_MSG);
            Thread.sleep(200);
            log.debug(DEBUG_MSG);
            Thread.sleep(200);
          }catch(InterruptedException e){
            System.out.println(getName()+ " was interrupted during the test of scenario 12");
            fail(getName()+ " was interrupted during the test of scenario 12");
          }
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

        private ServiceRegistration serviceRegistration;
        private Hashtable msgs;

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
            msgs = new Hashtable();
            msgs.put(ERROR_MSG, Boolean.FALSE);
            msgs.put(WARN_MSG, Boolean.FALSE);
            msgs.put(INFO_MSG, Boolean.FALSE);
            msgs.put(DEBUG_MSG, Boolean.FALSE);
            /* create the hashtable to put properties in */
            Hashtable props = new Hashtable();
            /* put service.pid property in hashtable */
            props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
            /* register the service */
            serviceRegistration = bundleContext.registerService(EventHandler.class
                    .getName(), this, props);
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
          assertTrue("Not all messages received",
                     (((Boolean) msgs.get(ERROR_MSG)).booleanValue() &&
                      ((Boolean) msgs.get(WARN_MSG)).booleanValue() &&
                      ((Boolean) msgs.get(INFO_MSG)).booleanValue() &&
                      ((Boolean) msgs.get(DEBUG_MSG)).booleanValue()));
        }

        public void handleEvent(Event event) {
          try {
            System.out.println(displayName + " received topic:"
                      + event.getTopic());
            msgs.put(event.getProperty(EventConstants.MESSAGE), Boolean.TRUE);
          } catch (RuntimeException e) {
            error = e;
            throw e;
          } catch (Throwable e) {
            error = e;
          }
        }// end handleEvent(....

    }
}
