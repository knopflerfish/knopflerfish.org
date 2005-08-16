/*
 * @(#)ComponentController.java        1.0 2005/07/28
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;

/**
 * @author Martin Berg
 *
 * This class is used to store information of a specific component
 * It has four helper classes that supply additional information
 * these are:
 * PropertiesInformation
 * PropertyInformation
 * ServiceInformation
 * ReferenceInformation
 */
public class ComponentDeclaration {
	/** The name of the component */
	private String componentName;
	/** to use autoenable on the component or not */
	private boolean autoEnable;
	/** the factory to use */
	private String factory;
	/** to use a factory with the component or not */
	private boolean serviceFactory;
	/** variable holding the declaring bundle */	
	private Bundle declaringBundle;
	/** String variable holding the path to the XML **/
	private String xmlFile;
	
	/**
	 * the class that implements the interfaces that is listed in
	 * serviceInfo
	 */
	private String implementation;
	/** A list containing all properties for the component */
	private ArrayList propertiesInfo;
	/** A list containing all property for the component */
	private ArrayList propertyInfo;
	/** A list containing all services for the component */
	private ArrayList serviceInfo;
	/** A list containing all references for the component */
	private ArrayList referenceInfo;
	/** A list containing all interfaces for the component */
	private ArrayList provideInfo;
	
	
	/* The constructor */
	ComponentDeclaration(){
		propertiesInfo = new ArrayList();
		propertyInfo = new ArrayList();
		serviceInfo = new ArrayList();
		referenceInfo = new ArrayList();
		provideInfo = new ArrayList();
	}
	
	
	public Object getClone()  {
		
			ComponentDeclaration clone = new ComponentDeclaration();
			clone.setAutoEnable(isAutoEnable());
			clone.setComponentName(getComponentName());
			clone.setDeclaraingBundle(getDeclaringBundle());
			clone.setFactory(getFactory());
			clone.setImplementation(getImplementation());
			clone.setPropertiesInfo(getPropertiesInfo());
			clone.setReferenceInfo(getReferenceInfo());
			clone.setServiceFactory(isServiceFactory());
			clone.setServiceInfo(getServiceInfo());
			
			return clone;
		
	}
	
	/**
	 * sets the bundle which declares the component
	 * 
	 * @param bundle the declaring bundle
	 */
	public void setDeclaraingBundle(Bundle bundle){
		declaringBundle=bundle;
	}
	
	/**
	 * returns the declaring bundle,i.e,
	 * the bundle holding this component.
	 * 
	 * @return bundle if set else null
	 */
	public Bundle getDeclaringBundle(){
		return declaringBundle;
	}
	
	/**
	 * @return Returns the autoEnable.
	 */
	public boolean isAutoEnable() {
		return autoEnable;
	}
	/**
	 * @return Returns the serviceFactory.
	 */
	public boolean isServiceFactory() {
		return serviceFactory;
	}
	/**
	 * @param serviceFactory The serviceFactory to set.
	 */
	public void setServiceFactory(boolean serviceFactory) {
		this.serviceFactory = serviceFactory;
	}
	/**
	 * @param autoEnable
	 *            The autoEnable to set.
	 */
	public void setAutoEnable(boolean autoEnable) {
		this.autoEnable = autoEnable;
	}
	/**
	 * @return Returns the componentName.
	 */
	public String getComponentName() {
		return componentName;
	}
	/**
	 * @param componentName
	 *            The componentName to set.
	 */
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	/**
	 * @return Returns the factory.
	 */
	public String getFactory() {
		return factory;
	}
	/**
	 * @param factory The factory to set.
	 */
	public void setFactory(String factory) {
		this.factory = factory;
	}

	/**
	 * @return Returns the implementation.
	 */
	public String getImplementation() {
		return implementation;
	}
	/**
	 * @param implementation
	 *            The implementation to set.
	 */
	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}
	/**
	 * @return Returns the propertiesInfo.
	 */
	public ArrayList getPropertiesInfo() {
		return propertiesInfo;
	}
	/**
	 * @param propertiesInfo
	 *            The propertiesInfo to set.
	 */
	public void setPropertiesInfo(ArrayList propertiesInfo) {
		this.propertiesInfo = propertiesInfo;
	}
	/**
	 * @return Returns the propertyInfo.
	 */
	public ArrayList getPropertyInfo() {
		return propertyInfo;
	}
	/**
	 * @param propertyInfo
	 *            The propertyInfo to set.
	 */
	public void setPropertyInfo(ArrayList propertyInfo) {
		this.propertyInfo = propertyInfo;
	}
	/**
	 * @return Returns the referenceInfo.
	 */
	public ArrayList getReferenceInfo() {
		return referenceInfo;
	}
	/**
	 * @param referenceInfo
	 *            The referenceInfo to set.
	 */
	public void setReferenceInfo(ArrayList referenceInfo) {
		this.referenceInfo = referenceInfo;
	}
	/**
	 * @return Returns the serviceInfo.
	 */
	public ArrayList getServiceInfo() {
		return serviceInfo;
	}
	/**
	 * @param serviceInfo
	 *            The serviceInfo to set.
	 */
	public void setServiceInfo(ArrayList serviceInfo) {
		this.serviceInfo = serviceInfo;
	}
	
	/**
	 * @return Returns an instance of PropertiesInformation.
	 */
	public ComponentPropertiesInfo getNewPropertiesIntance(){ 
		ComponentPropertiesInfo props = new ComponentPropertiesInfo();
		return props;
	}
	
	/**
	 * @return Returns an instance of PropertyInformation.
	 */
	public ComponentPropertyInfo getNewPropertyIntance(){ 
		ComponentPropertyInfo prop = new ComponentPropertyInfo();
		return prop;
	}
	
	/**
	 * @return Returns an instance of ServiceInformation.
	 */
	public ComponentServiceInfo getNewServiceIntance(){ 
		ComponentServiceInfo serv = new ComponentServiceInfo();
		return serv;
	}
		/**
	 * @return Returns an instance of ReferenceInformation.
	 */
	public ComponentReferenceInfo getNewReferenceIntance(){ 
		ComponentReferenceInfo ref = new ComponentReferenceInfo();
		return ref;
	}
	
	/**
	 * @param ComponentPropertiesInfo
	 *            The ComponentPropertiesInfo to add.
	 */
	public void addPropertiesInfo(ComponentPropertiesInfo propsInfo){
		propertiesInfo.add(propsInfo);
	}

	/**
	 * @param ComponentPropertyInfo
	 *            The ComponentPropertyInfo to add.
	 */
	public void addPropertyInfo(ComponentPropertyInfo propInfo){
		propertyInfo.add(propInfo);
	}
	
	/**
	 * @param ComponentServiceInfo
	 *            The ComponentServiceInfo to add.
	 */
	public void addServiceInfo(ComponentServiceInfo servInfo){
		serviceInfo.add(servInfo);
	}
	
	/**
	 * @param ComponentReferenceInfo
	 *            The ComponentReferenceInfo to add.
	 */
	public void addReferenceInfo(ComponentReferenceInfo refInfo){
		referenceInfo.add(refInfo);
	}
	
	/**
	 * this method converts properties located in a component 
	 * declaration and returns a dictionary. Those properties 
	 * are declared within the property element in the
	 * xml file.
	 * 
	 * @return	Dictionary with properties 
	 */
	public Dictionary getDeclaredProperties(){
		/* hashtable holding properties */
		Dictionary properties = new Hashtable();
				
		/* create an iterator */
		Iterator propsIterator = propertyInfo.iterator();
		
		/* iterate through them */
		while(propsIterator.hasNext()){
			/* get the property */
			ComponentPropertyInfo propertyInfo = (ComponentPropertyInfo)
													propsIterator.next();
			/* get the type of the property */
			String type= propertyInfo.getType();
			/* get the name of the propery */
			String name = propertyInfo.getName();
			
			if(type==null){
				type="String";
			}
			
			/* get the values */
			ArrayList values = propertyInfo.getValues();
			/* create an iterator */
			Iterator valuesIterator = values.iterator();
			
			/* if it is a string type */
			if(type.equals("String")){
				/* value array */
				String[] valueArray = new String[values.size()];
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					valueArray[valueCounter]= (String)valuesIterator.next();
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property to the property */
				properties.put(name,valueArray);
			}
			
			/* if it is a long type */
			if(type.equals("Long")){
				/* value array */
				long[] valueArray = new long[values.size()];
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					valueArray[valueCounter]= Long.parseLong((String)valuesIterator.next());
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property */
				properties.put(name,valueArray);
			}
			
			
			
			/* if it is a double type */
			if(type.equals("Double")){
				/* value array */
				double[] valueArray = new double[values.size()];
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					valueArray[valueCounter]= Double.parseDouble((String)valuesIterator.next());
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property */
				properties.put(name,valueArray);
			}
			
			
			/* if it is a float type */
			if(type.equals("Float")){
				/* value array */
				float[] valueArray = new float[values.size()];
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					valueArray[valueCounter]= Float.parseFloat((String)valuesIterator.next());
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property */
				properties.put(name,valueArray);
			}
			
			/* if it is a integer type */
			if(type.equals("Integer")){
				/* value array */
				int[] valueArray = new int[values.size()];
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					valueArray[valueCounter]= Integer.parseInt((String)valuesIterator.next());
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property */
				properties.put(name,valueArray);
			}
			
			/* if it is a byte type */
			if(type.equals("Byte")){
				/* value array */
				byte[] valueArray = new byte[values.size()];
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					valueArray[valueCounter]= Byte.parseByte((String)valuesIterator.next());
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property */
				properties.put(name,valueArray);
			}
			
			/* if it is a char type */
			if(type.equals("Char")){
				/* new string array */
				String chars = new String();
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					chars = chars + (String)valuesIterator.next();
					/* increase the value counter */
					
				}
				/* convert the string to char array */
				char[] valueArray =chars.toCharArray(); 
				/* add the property */
				properties.put(name,valueArray);
			}
			
			/* if it is a integer type */
			if(type.equals("Boolean")){
				/* value array */
				boolean[] valueArray = new boolean[values.size()];
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the string value */
					String value = (String)valuesIterator.next();
					/* assign the the value */
					valueArray[valueCounter]= Boolean.valueOf(value).booleanValue(); 
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property */
				properties.put(name,valueArray);
			}
			
			
			/* if it is a integer type */
			if(type.equals("Short")){
				/* value array */
				short[] valueArray = new short[values.size()];
			
				/* array counter */
				int valueCounter=0;
				
				while(valuesIterator.hasNext()){
					/* assign the the value */
					valueArray[valueCounter]= Short.parseShort((String)valuesIterator.next());
					/* increase the value counter */
					valueCounter++;
				}
				/* add the property */
				properties.put(name,valueArray);
			}
			
			
		}
		
		
		return mergeProperties(readPropertyFile(), properties);
	}
	
	/**
	 * this method overides values in one dictionary with values from
	 * another dictionary.
	 *  
	 * @param overideTable the dictionary which overides other simular values 
	 * @param mergeTable the dictionary to be modified
	 * @return Dictionary a modified table
	 */
	public Dictionary mergeProperties(Dictionary overideTable,Dictionary mergeTable){
		Enumeration enumeration = overideTable.keys();
		
		while(enumeration.hasMoreElements()){
			/* get the key */
			String key = (String)enumeration.nextElement();
			/* add the value to the merge table */
			mergeTable.put(key,overideTable.get(key));
		}
		
		return mergeTable;
	}
	
	/**
	 * @return Returns the xmlFile.
	 */
	public String getXmlFile() {
		return xmlFile;
	}
	
	/**
	 * @param xmlFile The xmlFile to set.
	 */
	public void setXmlFile(String xml) {
		this.xmlFile = xml;
	}
	
	private Dictionary readPropertyFile(){
		Iterator propsIterator = propertiesInfo.iterator();
		
		Dictionary returnDictionary = new Hashtable();
		while(propsIterator.hasNext()){
			try {
				/* get the information class */
				ComponentPropertiesInfo propertyEntry = (ComponentPropertiesInfo)propsIterator.next();
				/* get the string representing the file */
				String propertyFile =propertyEntry.getEntry();
				
				System.out.println("****************** Reading property file:" + propertyFile +
				" ********************");
				
				/* get the bundle location */
				String bundleLocation = declaringBundle.getLocation();
				/* get the formatted location */
				String formattedLocation = bundleLocation.substring(5,
						bundleLocation.length());

				/* get the jar file use the formatted location */
				JarFile jarFile = new JarFile(formattedLocation);
				ZipEntry zipEntry = jarFile.getEntry(propertyFile);
				
				
				Properties properties = new Properties();
				properties.load(jarFile.getInputStream(zipEntry));
				
				Enumeration enumeration = properties.keys();
				
				while(enumeration.hasMoreElements()){
					/* assign the key */
					String key = (String)enumeration.nextElement();
					/* put the key into the return dictonary element */
					returnDictionary.put(key,properties.get(key));
				}
								
			} catch (IOException e) {
				System.err.println("error when reading property file:" + e);
				e.printStackTrace();
			} catch(Exception e){
				System.err.println("error when reading property file:" + e);
				e.printStackTrace();
			}

		}
		
		/* return the dictionary */
		return returnDictionary;
		
	}
}
