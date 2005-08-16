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

import java.util.Dictionary;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class Activator implements BundleActivator{
   /* System component runtime variable */
	SystemComponentRuntimeImpl systemComponentRuntime;
   /* the bundle context for this bundle */
	BundleContext bundleContext;
	
  /**
   * this is the main entry for the SCR it will check if any bundles declares 
   * declarative components and create a ComponentDeclaration of the declaring
   * XML file. Having all already ACTIVE bundles with components the method will
   * create an instance of SystemComponentRuntimeImpl and pass the bundle context
   * and a Vector containing already active components. 
   */
  public void start(BundleContext context) throws Exception {
  	/* assign the context */
  	bundleContext = context;  	
  	/* vector holding already active components */
  	Vector activeComponents=new Vector();
  	/* get all the bundles */
  	Bundle[] bundles = bundleContext.getBundles();
  	
  	for(int i=0;i<bundles.length;i++){
  		/* get the dictionary */
  		Dictionary header = (Dictionary)bundles[i].getHeaders();
  		/* get the entry if any */
  		String entry = (String)header.get("Service-Component");
  		
  		if(entry!=null && bundles[i].getState()== Bundle.ACTIVE){
  			/* split the string if many entries are declared */
  			String[] entries = entry.split(",");
  			/* get the location of this bundle */
  			String bundleLocation = bundles[i].getLocation();
			
			/* format the string, i.e, remove the 'file:' entry from it */
			String formattedLocation = bundleLocation.substring(5,
					bundleLocation.length());
			
			/* get the jar file use the formatted location */
			JarFile jarFile = new JarFile(formattedLocation);
  			
  			/* go through the entries */
  			for(int j=0;j<entries.length;j++){
  				/* get the zip entry */
  				ZipEntry zipEntry = jarFile.getEntry(entries[j]);
  				
  				if(zipEntry!=null){
  					CustomParser customParser = new CustomParser();
  					
  					/* parse the document and retrieve a component declaration */
					ComponentDeclaration componentDeclaration = customParser
							.readXML(zipEntry,jarFile);
					
					/* set the declaring bundle */
					componentDeclaration.setDeclaraingBundle(bundles[i]);
					/* set the xml file */
					componentDeclaration.setXmlFile(entries[j]);
					/* add this declaration to the vector */
					activeComponents.add(componentDeclaration);
  				}
  				
  				
  			}
  		}
  			
  	}
  	
  	/* create a new SCR instance pass the already active components to it
  	 * they will be evaluated and started if they are satisfied  
  	 */
  	systemComponentRuntime= new SystemComponentRuntimeImpl(context,activeComponents);
  	
  	
  }

 /**
  * Stop the SCR
  */
  public void stop(BundleContext context) throws Exception {
  	
  	
  	systemComponentRuntime=null;
  }


}