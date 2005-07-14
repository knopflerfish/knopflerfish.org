/*
 * @(#)Activator.java        1.0 2005/06/28
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

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;

/**
 * The Activator class is the startup class for the EventHandlerService.
 * 
 * @author Magnus Klack
 */
public class Activator implements BundleActivator {
    /** local representation of the bundlecontext */
    private BundleContext bundleContext;

    /** the EventAdminService */
    private EventAdmin eventAdmin;

    /** the service id string */
    final static String SERVICE_ID = "org.osgi.service.event.EventAdmin";

    /**
     * Main entry for the service
     * 
     * @param context
     *            the BundleContext ,ie, the handle to the OSGi framework
     */
    public void start(BundleContext context) throws Exception {
        /* assign the context variable to a local variable */
        bundleContext = context;
        /* create the event admin service */
        eventAdmin = new EventAdminService(bundleContext);
        /* create the hashtable */
        Hashtable propsTable = new Hashtable();
        /* add the Constant variable and the id to the Hashtable */
        propsTable.put(Constants.SERVICE_PID, SERVICE_ID);
        /* register the service to the framework */
        bundleContext.registerService(EventAdmin.class.getName(), eventAdmin,
                propsTable);

        /* print that the service is up */
        System.out.println("EventAdmin Service is up\n");
    }

    /**
     * Stop the service
     * 
     * @param context
     *            the BundleContext ,ie, the handle to the OSGi framework
     */
    public void stop(BundleContext context) throws Exception {
    	eventAdmin = null;
        

    }

}
