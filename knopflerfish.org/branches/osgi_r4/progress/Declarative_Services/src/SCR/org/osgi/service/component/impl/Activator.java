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
package org.osgi.service.component.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class Activator implements BundleActivator{
   /* System component runtime variable */
	SystemComponentRuntimeImpl systemComponentRuntime;
   /* the bundle context for this bundle */
	BundleContext bundleContext;
	
  /**
   * Main Entry for the SCR
   * 
   */
  public void start(BundleContext context) throws Exception {
  	systemComponentRuntime= new SystemComponentRuntimeImpl(context);
  	bundleContext = context;
  }

 /**
  * Stop the SCR
  */
  public void stop(BundleContext context) throws Exception {
  }


}