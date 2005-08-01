/*
 * @(#)TracingIH.java        1.0 2005/06/28
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

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class creates a proxy object to every instance created in
 * SystemComponentRuntime.
 * 
 * @author Magnus Klack
 */
public  class TracingIH implements InvocationHandler{
	/** printer writer object */
	private PrintWriter out;
	/** the target object */
	private Object target;
	
	/**
	 * Constructor for the Tracing class 
	 * 
	 * @param object
	 * @param printer
	 */
	public TracingIH(Object object,PrintWriter printer){
		/* assign the target object */
		target=object;
		/* assign the printer object */
		out=printer;
	}
	
	/** 
	 * This is the proxy creation. It will create a proxy class to every single
	 * instance created in SystemComponentRuntime.
	 * 
	 * @param object the object which should be proxy
	 * @param printer the printer object
	 * @return the new instance of the proxy object
	 */
	public static Object createProxy(Object object,PrintWriter printer){
		
		try{
			/* return the proxy Object */ 
			return Proxy.newProxyInstance(object.getClass().getClassLoader(),
					object.getClass().getInterfaces(),
					new TracingIH(object,printer));
		}catch(Exception e){
			/* print the error */
			System.err.println("Error int TraceingIH.createProxy:" + e.getCause());
			/* return null */
			return null;
		}
	}
	
	/**
	 * invokes the given method wich is declared by a service 
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try{
			/* declare a return object */
			Object toReturn;
			/* print that this method was called */
			System.out.println(method.getName() +"(...) is called");
			/* assign the return object */
			toReturn = method.invoke(target,args);
			/* return the object */
			return toReturn;
		}catch(InvocationTargetException e){
			/* print that an error occurred */
			System.err.println("Error int TracinIH.invoke()" + e.getCause());
			/* throw the exception*/
			throw e.getCause();
		}
		
	
	}
	

}