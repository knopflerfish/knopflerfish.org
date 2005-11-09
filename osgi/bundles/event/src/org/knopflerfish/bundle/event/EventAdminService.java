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
package org.knopflerfish.bundle.event;

import java.util.Calendar;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import org.knopflerfish.service.log.LogRef;

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
public class EventAdminService implements EventAdmin {

  /** the local representation of the bundle context */
  private BundleContext bundleContext;

  /** the log * */
  private LogRef log;

  /** variable holding the synchronus send procedure */
  private QueueHandler queueHandlerSynch;

  /** variable holding the asynchronus send procedure */
  private QueueHandler queueHandlerAsynch;

  private Object semaphore = new Object();

  /**
   * the constructor use this to create a new Event admin service.
   *
   * @param context
   *            the BundleContext
   */
  public EventAdminService(BundleContext context) {
    synchronized(this){
      /* assign the context to the local variable */
      bundleContext = context;
      /* create the log */
      log = new LogRef(context);

      /* create the synchronus sender process */
      queueHandlerSynch  = new QueueHandler(this, context, QueueHandler.SYNCHRONUS_HANDLER);
      /* start the asynchronus sender */
      queueHandlerSynch.start();
      /* create the asynchronus queue handler */
      queueHandlerAsynch = new QueueHandler(this, context, QueueHandler.ASYNCHRONUS_HANDLER);
      /* start the handler */
      queueHandlerAsynch.start();

      new MultiListener(this, context);
    }
  }

  /**
   * This method should be used when an asynchronus events are to be
   * published.
   *
   * @param event the event to publish
   */
  public void postEvent(Event event) {
    /* console text for debugging purpose */
    //System.out.println("INCOMMING ASYNCHRONOUS EVENT");
    /* create a calendar */
    Calendar time = Calendar.getInstance();

    try{
      /* create an internal admin event */
      InternalAdminEvent adminEvent = new InternalAdminEvent(event, time,
          this);

      if (queueHandlerAsynch != null && getReferences()!=null) {
        /* add the admin event to the queueHandlers send queue */
        queueHandlerAsynch.addEvent(adminEvent);
        /* console text for debugging purpose */
        // System.out.println("Event has been sent to the send queue");
      }
    }catch(Exception e){
      System.out.println("Unknown exception in postEvent():" +e);
      e.printStackTrace();
    }
  }

  /**
   * This method should be used when synchronous events are to be published
   *
   * @param event the event to publish
   * @author Magnus Klack
   */
  public void sendEvent(Event event) {
    /* console text for debugging purpose */
    //System.out.println("INCOMMING SYNCHRONOUS EVENT");
    /* create a calendar */

    Calendar time = Calendar.getInstance();
    /* create an internal admin event */
    InternalAdminEvent adminEvent = new InternalAdminEvent(event, time,this);
    try {
      if(queueHandlerSynch!=null && getReferences()!=null){
        /* add the admin event to the queueHandlers send queue */
        queueHandlerSynch.addEvent(adminEvent);
      }
      /* lock this object to make it
       * possible to be notified */
      synchronized (this) {
        /* check not null */
        if(this.getReferences()!=null){
          /* wait until notified*/
          wait();
        }
      }

    } catch (InterruptedException e) {
      /* write the exception */
      System.out.println("sendEvent() was interrupted by external process:" + e);
    } catch(InvalidSyntaxException e){
      /* write the exception */
      System.out.println("invalid syntax in getReferences()" + e);
    }

    /* console text for debugging purpose */
    //System.out.println("FINISHED");
  }

  /**
   * returns the servicereferences
   *
   * @return ServiceReferences[] array if any else null
   * @throws InvalidSyntaxException if syntax error
   */
  ServiceReference[] getReferences() throws InvalidSyntaxException {
    try {
      return bundleContext.getServiceReferences(
          "org.osgi.service.event.EventHandler", null);
    } catch (InvalidSyntaxException e) {
      /* throw the error */
      throw e;
    }
  }

}
