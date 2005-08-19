/*
 * @(#)MyValueClient.java        1.0 2005/06/28
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
package com.gstm.scr;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.isv.process.myvalue.MyValueBind;
import com.isv.process.myvalue.MyValueLocate;

/** 
 * This class wil start a dummy client used 
 * in the Declarative service feature within
 * the OSGi framework.
 */
public class MyValueClient implements BundleActivator{

	/**
	 * Start the bundle 
	 */
	public void start(BundleContext context) throws Exception {
		
		
		ServiceReference reference = context.getServiceReference("com.isv.process.myvalue.MyValueBind");
		//System.out.println("THE REFERENCE IS:" + reference);
		
		
		if(reference!=null){
			MyValueBind myValue = (MyValueBind) context.getService(reference);
			
			if(myValue!=null){
				System.out.println("***************** My value is:" +myValue.getMyValue("WhatEver") +" ******************");
			}else{
				System.err.println("error getting myvalue service");
			}
		}
		
//		
//		MyValueLocate myValue = (MyValueLocate) context.getService(reference);
//		 	//System.out.println("THE INSTANCE IS:" + myValue);
//		
//			if(myValue!=null){
//				System.out.println("***************** My value is:" +myValue.getMyValue("WhatEver") +" ******************");
//			}else{
//				System.err.println("error getting myvalue service");
//			}
//		}else{
//			reference = context.getServiceReference("com.isv.process.myvalue.MyValueBind");
//			
//			if(reference!=null){
//				MyValueBind myValue = (MyValueBind) context.getService(reference);
//				
//				if(myValue!=null){
//					System.out.println("***************** My value is:" +myValue.getMyValue("WhatEver") +" ******************");
//				}else{
//					System.err.println("error getting myvalue service");
//				}
//				
//			}else{
//				System.err.println("error getting myvalue service");
//			}
//			
//		}
		
		
		
		
	}

	/**
	 * Stop the bundle
	 */
	public void stop(BundleContext context) throws Exception {
		
		
	}

}
