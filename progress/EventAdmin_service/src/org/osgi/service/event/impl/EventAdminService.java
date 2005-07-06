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

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.TopicPermission;

/**
 * Default implementation of the EventAdmin interface this is a singleton class
 * and should always be active. The implementation is responsible for track
 * eventhandlers and deliver events. If an eventhandler is subscibed to the
 * published event the EventAdmin service will call the eventhandlers
 * handleEvent() method.
 * 
 * @author Magnus Klack
 */
public class EventAdminService implements EventAdmin {
    /** the local representation of the bundle context */
    private BundleContext bundleContext;
    /** the log **/
    private LogRef log;
    
    
    /**
     * the constructor use this to create a new Event admin service.
     * 
     * @param context
     *            the BundleContext
     */
    public EventAdminService(BundleContext context) {
        /* assign the context to the local variable */
        bundleContext = context;
        /* create the log */
        log = new LogRef(context);
    }
    
    /**
     * This method should be used when an asynchronus events are to be
     * published.
     * 
     * @param event
     *            the event to publish
     */
    public void postEvent(Event event) {
        try {
            /* get the service references do this now to 
             * ensure that only handlers registered now 
             * will recive the event
             */
            
            /* method variable holding all service references */
            ServiceReference[] serviceReferences;
            
            /* lock 'this' and get the references when lock is aquired */
            synchronized(this){
                /* get the references */
               serviceReferences = getReferences();
            }
            
            /* get the securityManager */
            SecurityManager securityManager = getSecurityManager();
            /* variable indicates if the handler is allowed to publish */
            boolean canPublish;
            /* variable indicates if handlers are granted access to topic */
            boolean canSubscribe;
            
            /* check if security is applied */
            if (securityManager != null) {
                /* check if there are any security limitation */
                canPublish = checkPermissionToPublish(event, securityManager);
            } else {
                /* no security here */
                canPublish = true;
            }
            
            if (securityManager != null) {
                /* check if there are any security limitation */
                canSubscribe = checkPermissionToSubscribe(event,
                        securityManager);
            } else {
                /* no security here */
                canSubscribe = true;
            }
            
            if (canPublish && canSubscribe) {
                /* create deliver session to deliver events in thread */
                DeliverSession deliverSession = new DeliverSession(event,
                        bundleContext, serviceReferences,log);
                /* call the deliver function asynchronus */
                deliverSession.start();
            } else {
                /* no permissions at all are set */
                if (!canPublish && !canSubscribe) {
                    /* this will happen if an error occures in getReferences() */
                    if(log!=null){
                        /* log the error */
	                    log.error("No permission to publish and subscribe top topic:" 
	                            + event.getTopic() );
                    }
                }
                
                /* no publish permission */
                if (!canPublish && canSubscribe) {
                    /* this will happen if an error occures in getReferences() */
                    if(log!=null){
                        /* log the error */
	                    log.error("No permission to publish top topic:" 
	                            + event.getTopic() );
                    }
                }
                
                /* no subscribe permission */
                if (canPublish && !canSubscribe) {
                    /* this will happen if an error occures in getReferences() */
                    if(log!=null){
                        /* log the error */
	                    log.error("No permission granted to subscribe to topic:" 
	                            + event.getTopic() );
                    }
                }
            }
            
        } catch (InvalidSyntaxException e) {
            /* this will happen if an error occures in getReferences() */
            if(log!=null){
                /* log the error */
                log.error("Can't get any service references");
            }
        }
        
    }//Todo Implement Asynchron
    
    private void getException() throws Exception {
        throw new Exception();
        
    }
    
    /**
     * This method should be used when synchronous events are to be published
     * 
     * @author Martin Berg, Magnus Klack
     * @param event
     *            the event to publish
     */
    public void sendEvent(Event event) {
        try {
            /* get the service references do this now to 
             * ensure that only handlers registered now 
             * will recive the event
             */
            
            /* method variable holding all service references */
            ServiceReference[] serviceReferences;
            
            /* lock 'this' and get the references when lock is aquired */
            synchronized(this){
                /* get the references */
               serviceReferences = getReferences();
            }
            
            /* get the securityManager */
            SecurityManager securityManager = getSecurityManager();
            /* variable indicates if the handler is allowed to publish */
            boolean canPublish;
            /* variable indicates if handlers are granted access to topic */
            boolean canSubscribe;
            
            /* check if security is applied */
            if (securityManager != null) {
                /* check if there are any security limitation */
                canPublish = checkPermissionToPublish(event, securityManager);
            } else {
                /* no security here */
                canPublish = true;
            }
            
            if (securityManager != null) {
                /* check if there are any security limitation */
                canSubscribe = checkPermissionToSubscribe(event,
                        securityManager);
            } else {
                /* no security here */
                canSubscribe = true;
            }
            
           
            if (canPublish && canSubscribe) {
                /* create an instance of the deliver session to deliver events */
                DeliverSession deliverSession = new DeliverSession(event,
                        bundleContext, serviceReferences,log);
                synchronized(deliverSession){
                    /* call the deliver function synchronous */
                    deliverSession.startDeliver();
                }
            } else {
                /* no permissions at all are set */
                if (!canPublish && !canSubscribe) {
                    /* this will happen if an error occures in getReferences() */
                    if(log!=null){
                        /* log the error */
	                    log.error("No permission to publish and subscribe top topic:" 
	                            + event.getTopic() );
                    }
                }
                
                /* no publish permission */
                if (!canPublish && canSubscribe) {
                    /* this will happen if an error occures in getReferences() */
                    if(log!=null){
                        /* log the error */
	                    log.error("No permission to publishto topic:" 
	                            + event.getTopic() );
                    }
                }
                
                /* no subscribe permission */
                if (canPublish && !canSubscribe) {
                    /* this will happen if an error occures in getReferences() */
                    if(log!=null){
                        /* log the error */
	                    log.error("No permission to granted for subscription to topic:" 
	                            + event.getTopic() );
                    }
                }
            }
            
        } catch (InvalidSyntaxException e) {
            /* this will happen if an error occures in getReferences() */
            if(log!=null){
                /* log the error */
                log.error("Can't get any service references");         
            }
            
        }
        
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
    private ServiceReference[] getReferences() throws InvalidSyntaxException {
        try {
            return bundleContext.getServiceReferences(
                    "org.osgi.service.event.EventHandler", null);
        } catch (InvalidSyntaxException e) {
            /* print the error */
            throw e;
        }
        
    }
    
}
