/*
 * @(#)EventAdminService.java        1.0 2005/06/28
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
package org.osgi.service.event.impl;

import java.security.AccessControlException;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;

import org.knopflerfish.service.log.LogRef;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.TopicPermission;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * Default implementation of the EventAdmin interface this is a singleton class
 * and should always be active. The implementation is responsible for track
 * eventhandlers and check their permissions. It will also
 * host two threads sending diffrent types of data. If an eventhandler is subscribed to the
 * published event the EventAdmin service will put the event on one of the two internal sendstacks
 * depending on what type of deliverance the event requires.  
 * 
 * @author Magnus Klack
 */
public class EventAdminService implements EventAdmin, 
BundleListener, LogListener, ServiceListener, FrameworkListener{
    /** the local representation of the bundle context */
    private BundleContext bundleContext;

    /** the log * */
    private LogRef log;

    /** variable holding the synchronus send procedure */
    private QueueHandler queueHandlerSynch;

    /** variable holding the asynchronus send procedure */
    private QueueHandler queueHandlerAsynch;
    
    /** A hasbtable of eventhandlers and timestamps */
    private static Hashtable eventHandlers = new Hashtable();

    /**
     * the constructor use this to create a new Event admin service.
     * 
     * @param context
     *            the BundleContext
     */
    public EventAdminService(BundleContext context){
        /* assign the context to the local variable */
        bundleContext = context;
        /* create the log */
        log = new LogRef(context);
        /* Adds this class as a listener of bundle events */
        bundleContext.addBundleListener(this);
        /* Gets the service reference of the log reader service*/
        ServiceReference sr = bundleContext.getServiceReference(LogReaderService.class.getName());
        /* Claims the log reader service */
        LogReaderService logReader = (LogReaderService)bundleContext.getService(sr);
        /* Adds this class as a listener of log events */
        logReader.addLogListener(this);
        /* Adds this class as a listener of service events */
        bundleContext.addServiceListener(this);
        /* Adds this class as a listener of framework events */
        bundleContext.addFrameworkListener(this);
        /* create the synchronus sender process */
        queueHandlerSynch = new QueueHandler();
        /* start the asynchronus sender */
        queueHandlerSynch.start();
        /* create the asynchronus queue handler */
        queueHandlerAsynch = new QueueHandler();
        /* start the handler */
        queueHandlerAsynch.start();

    }

    /**
     * This method should be used when an asynchronus events are to be
     * published.
     * 
     * @param event
     *            the event to publish
     */
    public void postEvent(Event event) {
        /* create a calendar */
        Calendar time = Calendar.getInstance();
        /* create an internal admin event */
        InternalAdminEvent adminEvent = new InternalAdminEvent(event, time,
                this);
        /* add the admin event to the queueHandlers send queue */
        queueHandlerAsynch.addEvent(adminEvent);
        /* print that we are finsished */
        System.out.println("Event has been sent to the send queue");

    }

    /**
     * This method should be used when synchronous events are to be published
     * 
     * @author Martin Berg, Magnus Klack
     * @param event
     *            the event to publish
     */
    public void sendEvent(Event event) {
        System.out.println("INCOMMING SYNCHRONOUS EVENT");
        /* create a calendar */
        Calendar time = Calendar.getInstance();
        /* create an internal admin event */
        InternalAdminEvent adminEvent = new InternalAdminEvent(event, time,
                this);
        /* add the admin event to the queueHandlers send queue */
        queueHandlerSynch.addEvent(adminEvent);
        /* wait untill the object have been finished */

        try {
            /* lock this object */
            synchronized (this) {
                /* wait until delivered */
                wait();
            }

        } catch (InterruptedException e) {
            /* write the exception */
            System.out.println(e);
        }

        /* print that we are finsished */
        System.out.println("FINISHED");

    }

    /**
     * checks the permission to subscribe to this subject. OBS! this one will
     * only se if there are any permissions granted for all objects to
     * subscribe.
     * 
     * @param event
     *            the subscription event
     * @param securityManager
     *            the system securitymanager
     * @return true if the object is permitted else false
     */
    private boolean checkPermissionToSubscribe(Event event,
            SecurityManager securityManager) {

        try {
            /* create a topic */
            TopicPermission subscribePermission = new TopicPermission(event
                    .getTopic(), "subscribe");
            /* check the permission */
            securityManager.checkPermission(subscribePermission);
            /* return true */
            return true;
        } catch (AccessControlException e) {
            /* return false */
            return false;
        }

    }

    /**
     * This function checks if the publisher is permitted to publish the event.
     * 
     * @param event
     *            the event to be published
     * @param securityManager
     *            the system security manager
     * @return true if the publisher can publish the subject else false
     */
    private boolean checkPermissionToPublish(Event event,
            SecurityManager securityManager) {

        try {
            /* create a topic */
            TopicPermission publishPermission = new TopicPermission(event
                    .getTopic(), "publish");
            /* check the permission */
            securityManager.checkPermission(publishPermission);
            /* return true */
            return true;

        } catch (AccessControlException e) {
            /* return false */
            return false;
        }

    }

    /**
     * returns the security manager
     * 
     * @return the security manager if any else null
     */
    private SecurityManager getSecurityManager() {
        /* return the security manager */
        return System.getSecurityManager();
    }

    /**
     * returns the servicereferences
     * 
     * @return ServiceReferences[] array if any else null
     * @throws InvalidSyntaxException
     *             if syntax error
     */
    private synchronized ServiceReference[] getReferences()
            throws InvalidSyntaxException {
        try {
            return bundleContext.getServiceReferences(
                    "org.osgi.service.event.EventHandler", null);
        } catch (InvalidSyntaxException e) {
            /* print the error */
            throw e;
        }

    }
    /**
     * A listener for events sent by bundles
     * @author Johnny Bäverås
     */
    public void bundleChanged(BundleEvent bundleEvent){
    	/* A dictionary to store properties in */
    	Dictionary props = new Hashtable();
    	/* The bundle included in the bundleEvent */
    	Bundle bundle = bundleEvent.getBundle();
    	/* The prefix of the topic of the event to be posted*/
    	String topic = "org/osgi/framework/BundleEvent/";
    	/* boolean determining whether the bundleEvent is of a known topic */
    	boolean knownMessageType = true;
    	/* Determining the suffix of the topic of the event to be posted */
    	switch(bundleEvent.getType()){
			case BundleEvent.INSTALLED:
				topic += "INSTALLED";
    		break;
			case BundleEvent.STARTED:
				topic += "STARTED";
    		break;
			case BundleEvent.STOPPED:
				topic += "STOPPED";
    		break;
			case BundleEvent.UPDATED:
				topic += "UPDATED";
    		break;
			case BundleEvent.UNINSTALLED:
				topic += "UNINSTALLED";
    		break;
    		default:
    			/* Setting the boolean to false if an unknown event arrives */
    			knownMessageType = false;
    		break;
    /*		case BundleEvent.RESOLVED:
				topic += "RESOLVED";
    		break;
			case BundleEvent.UNRESOLVED:
				topic += "UNRESOLVED";
    		break;
    */
    	}
    	
    	/* Stores the properties of the event in the dictionary, if the event is known */
    	if(knownMessageType){
    		props.put("event", bundleEvent);
    		props.put("bundle.id", new Long(bundle.getBundleId()));
    		props.put("bundle.symbolicName", bundle.getLocation());//osäker på denna
    		props.put("bundle", bundle);
    		/* Tries posting the event once the properties are set */
    		try{
    			postEvent(new Event(topic, props));
    		}
    		catch(Exception e){
    		}
    	}
    	else{
    		/* Logs an error if the event, which arrived, were of an unknown type */
    		log.error("Recieved unknown message, discarding");
    	}
    }
    /**
     * A listener for entries in the log
     * @author Johnny Bäverås
     */
    public void logged(LogEntry logEntry){
    	/* A dictionary to store properties in */
    	Dictionary props = new Hashtable();
    	/* The bundle included in the bundleEvent */
    	Bundle bundle = logEntry.getBundle();
    	/* The prefix of the topic of the event to be posted*/
    	String topic = "org/osgi/service/log/LogEntry/";
    	/* Determining the suffix of the topic of the event to be posted */
    	switch(logEntry.getLevel()){
			case LogRef.LOG_ERROR:
				topic += "LOG_ERROR";
    		break;
			case LogRef.LOG_WARNING:
				topic += "LOG_WARNING";
    		break;
			case LogRef.LOG_INFO:
				topic += "LOG_INFO";
    		break;
			case LogRef.LOG_DEBUG:
				topic += "LOG_DEBUG";
    		break;
    		default:
    			/* if an unknown event arrives, it should be logged as a LOG_OTHER event */
    			topic += "LOG_OTHER";
    		break;
    	}
    	
    	/* Stores the properties of the event in the dictionary */
		props.put("bundle.id", new Long(bundle.getBundleId()));
		props.put("bundle.symbolicName", bundle.getLocation());//osäker på denna, ska bara sättas om den inte är null
		props.put("bundle", bundle);
		props.put("log.level", new Integer(logEntry.getLevel()));
		props.put("message", logEntry.getMessage());
		props.put("timestamp", new Long(logEntry.getTime()));
		props.put("log.entry", logEntry);
		
		/* If the event contains an exception, further properties shall be set */
		if(logEntry.getException() != null){
			Throwable e = logEntry.getException();
			props.put("exception.class", Throwable.class.getName());
			props.put("exception.message", e.getMessage());
			props.put("exception", e);
		}
		
		/* If the event contains a service reference, further properties shall be set */
		if(logEntry.getServiceReference() != null){
			props.put("service", logEntry.getServiceReference());
			props.put("service.id", Constants.SERVICE_ID);
			props.put("service.objectClass", Constants.OBJECTCLASS);
			
			/* If service_pid returns a non-null value, further properties shall be set*/
			if(Constants.SERVICE_PID != null){
				props.put("service.pid", Constants.SERVICE_PID);
			}

		/* Tries posting the event once the properties are set */
			try{
				postEvent(new Event(topic, props));
			}catch(Exception e){
				System.err.println("Exception:" + e.getMessage());
			}
		}
    }
    /**
     * TODO Fråga Pelle om objectClass, fel typ.. string vs string[]
     * TODO Fråga pelle om konstanterna i bundleevent
     */

	/**
	 * A listener for service events
	 * @author Johnny Bäverås
	 */
	public void serviceChanged(ServiceEvent serviceEvent) {
		
		
    	/* A dictionary to store properties in */
    	Dictionary props = new Hashtable();
    	/* The prefix of the topic of the event to be posted*/
    	String topic = "org/osgi/framework/ServiceEvent/";
    	/* boolean determining whether the bundleEvent is of a known topic */
    	boolean knownMessageType = true;
    	/* Determining the suffix of the topic gof the event to be posted */
    	switch(serviceEvent.getType()){
			case ServiceEvent.REGISTERED:
				topic += "REGISTERED";
				/* Fetches the service sending the event and determines whether its an eventHandler */
				if(bundleContext.getService(serviceEvent.getServiceReference()) instanceof EventHandler){
					/* Adds the service reference as a key in the hashtable of handlers, and the timestamp as value */
					eventHandlers.put(serviceEvent.getServiceReference(), new Long(System.currentTimeMillis()));
				}
    		break;
			case ServiceEvent.MODIFIED:
				topic += "MODIFIED";
    		break;
			case ServiceEvent.UNREGISTERING:
				topic += "UNREGISTERING";
				/* Tries to removes the entry in the hashtable */
				try{
					eventHandlers.remove(serviceEvent.getServiceReference());
				}catch(NullPointerException e){
					log.error("Tried to unregister a service which was not registered");
				}
    		break;
    		default:
    			/* Setting the boolean to false if an unknown event arrives */
    			knownMessageType = false;
    		break;
    	}
    	System.out.println("EventADMIN: current listneners: " + eventHandlers.size());
    	/* Stores the properties of the event in the dictionary, if the event is known */
    	if(knownMessageType){		    		
    		props.put("event", serviceEvent);
    		props.put("service", serviceEvent.getServiceReference());
    		props.put("service.pid", Constants.SERVICE_PID);
    		props.put("service.id", Constants.SERVICE_ID);
    		props.put("service.objectClass", Constants.OBJECTCLASS);
    		
    		/* Tries posting the event once the properties are set */
    		try{
    			postEvent(new Event(topic, props));
    		}catch(Exception e){
    			System.err.println("Exception:" + e.getMessage());
    		}
    	}
    	else{
    		/* Logs an error if the event, which arrived, were of an unknown type */
    		log.error("Recieved unknown message, discarding");
    	}
		
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
	 */
	public void frameworkEvent(FrameworkEvent frameworkEvent) {
    	/* A dictionary to store properties in */
    	Dictionary props = new Hashtable();
    	/* The bundle included in the bundleEvent */
    	Bundle bundle = frameworkEvent.getBundle();
    	/* The prefix of the topic of the event to be posted*/
    	String topic = "org/osgi/framework/FrameworkEvent/";
    	/* boolean determining whether the bundleEvent is of a known topic */
		boolean knownMessageType = true;
    	switch(frameworkEvent.getType()){
			case FrameworkEvent.STARTED:
				topic += "STARTED";
    		break;
			case FrameworkEvent.ERROR:
				topic += "ERROR";
    		break;
			case FrameworkEvent.PACKAGES_REFRESHED:
				topic += "PACKAGES_REFRESHED";
    		break;
			case FrameworkEvent.STARTLEVEL_CHANGED:
				topic += "STARTLEVEL_CHANGED";
    		break;
    		default:
    			/* Setting the boolean to false if an unknown event arrives */
    			knownMessageType = false;
    		break;
    	}
    	
    	/* Stores the properties of the event in the dictionary, if the event is known */
    	if(knownMessageType){		
    		props.put("event", frameworkEvent);
    		/* If the event contains a bundle, further properties shall be set */
    		if(frameworkEvent.getBundle() != null){  			
    			props.put("bundle.id",  new Long(bundle.getBundleId()));
    			props.put("bundle.symbolicName", bundle.getLocation());
    			props.put("bundle", bundle);      			
    		}
    		
    		/* If the event contains an exception, further properties shall be set */
    		if(frameworkEvent.getThrowable() != null){
    			Throwable e = frameworkEvent.getThrowable();
    			props.put("exception.class", Throwable.class.getName());
    			props.put("exception.message", e.getMessage());
    			props.put("exception", e);
    		}
    		
    		/* Tries posting the event once the properties are set */
    		try{
    			postEvent(new Event(topic, props));
    		}
    		catch(Exception e){
    			System.err.println("Exception:" + e.getMessage());
    		}
    	}
    	else{
    		log.error("Recieved unknown message, discarding");
    	}
	}

    /**
     * this class will send the events synchronus to the event handlers it will
     * pick the first element in the queue and assure that the events will
     * arrive in order.
     * 
     * @author Magnus Klack
     */
    private class QueueHandler extends Thread {
        /** the queue */
        private LinkedList syncQueue = new LinkedList();

        /** variable indicating that this is running */
        private boolean running;

        /** class variable indicating deliverance * */
        private boolean delivering;

        /**
         * Constructor for the QueueHandler
         */
        public QueueHandler() {
            running = true;
            delivering = false;
        }

        /**
         * This adds a new InternalAdminEvent to the que
         * 
         * @param event
         *            the new InternalAdminEvent
         */
        public void addEvent(Object event) {
            /* add the event */
            syncQueue.add(event);
        }

        public void run() {
            /* as long as the thread is running */
            while (running) {
                /*
                 * if the queue has events and the Admin class is not receiving
                 * anything
                 */
                if (!syncQueue.isEmpty()) {
                    /* lock the object */
                    synchronized (this) {
                        /* set state to delivering */
                        delivering = true;

                        try {
                            /*
                             * get the service references do this now to ensure
                             * that only handlers registered now will recive the
                             * event
                             */

                            /* method variable holding all service references */
                            ServiceReference[] serviceReferences;

                            /* get the references */
                            serviceReferences = getReferences();
                            /* get the 'Event' not the InternalAdminEvent */
                            InternalAdminEvent event = (InternalAdminEvent) syncQueue
                                    .getFirst();
                            /* remove it from the list */
                            syncQueue.removeFirst();

                            /* get the securityManager */
                            SecurityManager securityManager = getSecurityManager();
                            /*
                             * variable indicates if the handler is allowed to
                             * publish
                             */
                            boolean canPublish;

                            /*
                             * variable indicates if handlers are granted access
                             * to topic
                             */
                            boolean canSubscribe;

                            /* check if security is applied */
                            if (securityManager != null) {
                                /* check if there are any security limitation */
                                canPublish = checkPermissionToPublish(
                                        (Event) event.getElement(),
                                        securityManager);
                            } else {
                                /* no security here */
                                canPublish = true;
                            }

                            if (securityManager != null) {
                                /* check if there are any security limitation */
                                canSubscribe = checkPermissionToSubscribe(
                                        (Event) event.getElement(),
                                        securityManager);
                            } else {
                                /* no security here */
                                canSubscribe = true;
                            }

                            if (canPublish && canSubscribe) {

                                /*
                                 * create an instance of the deliver session to
                                 * deliver events
                                 */
                                DeliverSession deliverSession = new DeliverSession(
                                        event, bundleContext,
                                        serviceReferences, log, this);

                                /* start deliver events */
                                deliverSession.start();

                                try {
                                    /* wait for notification */
                                    wait();
                                } catch (InterruptedException e) {
                                    /* print the error message */
                                    System.out
                                            .println("Exception in SynchDeliverThread:"
                                                    + e);
                                }

                            } else {
                                /* no permissions at all are set */
                                if (!canPublish && !canSubscribe) {
                                    /*
                                     * this will happen if an error occurres in
                                     * getReferences()
                                     */
                                    if (log != null) {
                                        /* log the error */
                                        log
                                                .error("No permission to publish and subscribe top topic:"
                                                        + ((Event) event
                                                                .getElement())
                                                                .getTopic());
                                    }
                                }

                                /* no publish permission */
                                if (!canPublish && canSubscribe) {
                                    /*
                                     * this will happen if an error occures in
                                     * getReferences()
                                     */
                                    if (log != null) {
                                        /* log the error */
                                        log
                                                .error("No permission to publishto topic:"
                                                        + ((Event) event
                                                                .getElement())
                                                                .getTopic());
                                    }
                                }

                                /* no subscribe permission */
                                if (canPublish && !canSubscribe) {
                                    /*
                                     * this will happen if an error occures in
                                     * getReferences()
                                     */
                                    if (log != null) {
                                        /* log the error */
                                        log
                                                .error("No permission to granted for subscription to topic:"
                                                        + ((Event) event
                                                                .getElement())
                                                                .getTopic());
                                    }
                                }
                            }

                        } catch (InvalidSyntaxException e) {
                            /*
                             * this will happen if an error occures in
                             * getReferences()
                             */
                            if (log != null) {
                                /* log the error */
                                log.error("Can't get any service references");
                            }

                        }
                        synchronized (this) {
                            delivering = false;
                        }
                    }// end synchronized
                }// end if(!asyncQueue.isEmpty....

            }// end while ...
        }// end run()

    }// end class QueueHandler

}
