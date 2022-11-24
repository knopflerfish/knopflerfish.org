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

    private static class EventGenerator extends TestCase {
        /** the log */
        private LogRef log;

        /**
         * Consturctor to initialize a dummy bundle
         */
        public EventGenerator(BundleContext context) {
          /* call the super class */
            super("Event generator");
            /* assign the log */
            log = new LogRef(context);
            assertNotNull(getName()+ " can't get the log service",log);
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

        private ServiceRegistration<EventHandler> serviceRegistration;
        private Hashtable<String, Boolean> msgs;

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
            msgs = new Hashtable<>();
            msgs.put(ERROR_MSG, Boolean.FALSE);
            msgs.put(WARN_MSG, Boolean.FALSE);
            msgs.put(INFO_MSG, Boolean.FALSE);
            msgs.put(DEBUG_MSG, Boolean.FALSE);
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
          assertTrue("Not all messages received",
                     (msgs.get(ERROR_MSG) &&
                         msgs.get(WARN_MSG) &&
                         msgs.get(INFO_MSG) &&
                         msgs.get(DEBUG_MSG)));
        }

        public void handleEvent(Event event) {
          try {
            System.out.println(displayName + " received topic:"
                      + event.getTopic());
            msgs.put(event.getProperty(EventConstants.MESSAGE).toString(), Boolean.TRUE);
          } catch (RuntimeException e) {
            error = e;
            throw e;
          } catch (Throwable e) {
            error = e;
          }
        }// end handleEvent(....

    }
}
