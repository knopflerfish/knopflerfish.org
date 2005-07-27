/*
 * @(#)SystemComponentRuntimeImpl.java        1.0 2005/06/28
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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;



/**
 * This class is the implementation of the declarative service feature. It will
 * locate and bind diffrent types of declared components on demand. It will also
 * listen to BundleEvents rasied within the framework and
 */
public class SystemComponentRuntimeImpl implements BundleListener{
	/* variable holding the bundlecontext */
	private BundleContext bundleContext;
	
	static final String JAXP_SCHEMA_LANGUAGE =
	    "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	static final String W3C_XML_SCHEMA =
	    "http://www.w3.org/2001/XMLSchema"; 
	
	public SystemComponentRuntimeImpl(BundleContext context){
		/* assign the bundlecontext */
		bundleContext=context;
		/* add this as a bundle listener */
		bundleContext.addBundleListener(this);
	}
	
	/**
	 * Listen for BundleEvents from the framework
	 * 
	 * @throws IOException
	 */
	public void bundleChanged(BundleEvent event)  {
		/* try to get the XML file */
		String manifestEntry= (String) event.getBundle().getHeaders().get("Service-Component");
		String bundleLocation = event.getBundle().getLocation();
		/* check if null */
		if(manifestEntry!=null){
			System.out.println("\n**************************** START ********************************");
			/* print that a service component is found */
			System.out.println("Found service component");
			/* print the bundle location */
			System.out.println("The bundle location: " + bundleLocation);
			/* format the location string */
			String formattedLocation = bundleLocation.substring(5,bundleLocation.length());
			/* print the bundle formatted location */
			System.out.println("The bundle formatted location: " + formattedLocation);
			/* print the xml file location */
			System.out.println("The XML file location: "+manifestEntry);
			
			try{
				/* get the jar file use the formatted location */
				JarFile jarFile = new JarFile(formattedLocation);
				/* get the xmlfile located by the manifestEntry */
				ZipEntry zipEntry= jarFile.getEntry(manifestEntry);
				
				/* check if null */
				if(zipEntry!=null){
					
					/* get the input stream */
					InputStream inputStream= 	jarFile.getInputStream(zipEntry);
	
					/* create the pareser */
					try{
						XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
						factory.setNamespaceAware(true);
						
						XmlPullParser parser = factory.newPullParser();
						
						parser.setInput(inputStream,null);
						parser.nextTag();
				
						
						parser.require(XmlPullParser.START_TAG,"http://www.osgi.org/xmlns/scr/v1.0.0","component");
						//parser.require(XmlPullParser.START_TAG,null,null);
						
						while(parser.nextTag()!= XmlPullParser.END_TAG) {
							
							if(parser.getName().equals("implementation")){
								readXmlAdhocData(parser);
							}
							
							if(parser.getName().equals("service")){
								readXmlStrict(parser);
							}
							
							if(parser.getName().equals("reference")){
								readXmlAdhocData(parser);
							}
							
							if(parser.getName().equals("property")){
								readXmlAdhocData(parser);
							}
							
							if(parser.getName().equals("properties")){
								readXmlAdhocData(parser);
							}
							
						
						}
					
						parser.require(XmlPullParser.END_TAG,"http://www.osgi.org/xmlns/scr/v1.0.0","component");
						
						//parser.require(XmlPullParser.END_DOCUMENT, null, null);
						
					}catch(Exception e){
						System.out.println("ParseException:" +e);
					}
					
					System.out.println("\n**************************** END *********************************");
				}else{
					
				}
				
				
				
			}catch(IOException e){
				System.out.println("Error getting xml file" + e) ;
			}catch(Exception e){
				System.out.println("Error reading zipentry" + e);
			}
			
		}
	}
	
	private void readXmlAdhocData(XmlPullParser parser){
		System.out.println("Reading:" + parser.getName());
		if(parser.getName().equals("implementation") || parser.getName().equals("provide") ||
				parser.getName().equals("reference")){
			
			try{
			
				for(int i=0;i<parser.getAttributeCount();i++){
					System.out.println("Set " + parser.getName() +" "+ parser.getAttributeName(i) +" to: " +  parser.getAttributeValue(i));
				}
			
			parser.next();
			
			}catch(Exception e){
				System.out.println("Error Parsing implementation tag:" + e);
			}
		}
	}
	
	private void readXmlStrict(XmlPullParser parser){
		if(parser.getName().equals("service")){
			System.out.println("Reading service");
			try{
				parser.require(XmlPullParser.START_TAG,"","service");
				while (parser.nextTag() != XmlPullParser.END_TAG) {

					parser.require(XmlPullParser.START_TAG, null, null);
					String name = parser.getName();

					//String text = parser.nextText();
					//System.out.println ("<"+name+">"+text);

					if(name.equals("provide")){
						System.out.println("Reading provide");
						parser.require(XmlPullParser.START_TAG, null, "provide");
						for(int i=0;i<parser.getAttributeCount();i++){
							System.out.println("Set "+ name + " "+ parser.getAttributeName(i) +" to: " +  parser.getAttributeValue(0));
						}
						parser.next();
						
					}
					
					
					parser.require(XmlPullParser.END_TAG, null, name);
				}
				
				parser.require(XmlPullParser.END_TAG, null, "service");
			
			}catch(Exception e){
				System.out.println("Error Parsing Service:" +e);
			}
		}
	}

	/**
	 * This class is used to store information of a specific component
	 * It has four helper classes that supply additional information
	 * these are:
	 * PropertiesInformation
	 * PropertyInformation
	 * ServiceInformation
	 * ReferenceInformation
	 */
	class ComponentInformation{
		
		/* The name of the component*/
		private String componentName;
		/* to use autoenable on the component or not*/
		private boolean autoEnable;
		/* to use a factory with the component or not */
		private boolean factory;
		/* the class that implements the interfaces that is listed in serviceInfo*/
		private String implementation;
		
		/* A list containing all properties for the component*/
		private ArrayList propertiesInfo;
		/* A list containing all property for the component*/
		private ArrayList propertyInfo;
		/* A list containing all services for the component*/
		private ArrayList serviceInfo;
		/* A list containing all references for the component*/
		private ArrayList referenceInfo;
		
		/*The constructor*/
		ComponentInformation(){
			propertiesInfo = new ArrayList();
			propertyInfo = new ArrayList();
			serviceInfo = new ArrayList();
			referenceInfo = new ArrayList();			
		}
		
		/**
		 * @return Returns the autoEnable.
		 */
		public boolean isAutoEnable() {
			return autoEnable;
		}
		/**
		 * @param autoEnable The autoEnable to set.
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
		 * @param componentName The componentName to set.
		 */
		public void setComponentName(String componentName) {
			this.componentName = componentName;
		}
		/**
		 * @return Returns the factory.
		 */
		public boolean isFactory() {
			return factory;
		}
		/**
		 * @param factory The factory to set.
		 */
		public void setFactory(boolean factory) {
			this.factory = factory;
		}
		/**
		 * @return Returns the implementation.
		 */
		public String getImplementation() {
			return implementation;
		}
		/**
		 * @param implementation The implementation to set.
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
		 * @param propertiesInfo The propertiesInfo to set.
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
		 * @param propertyInfo The propertyInfo to set.
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
		 * @param referenceInfo The referenceInfo to set.
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
		 * @param serviceInfo The serviceInfo to set.
		 */
		public void setServiceInfo(ArrayList serviceInfo) {
			this.serviceInfo = serviceInfo;
		}
		
		/**
		 * @return Returns an instance of PropertiesInformation.
		 */
		public PropertiesInformation getNewPropertiesIntance(){ 
			PropertiesInformation props = new PropertiesInformation();
			return props;
		}
		
		/**
		 * @return Returns an instance of PropertyInformation.
		 */
		public PropertyInformation getNewPropertyIntance(){ 
			PropertyInformation prop = new PropertyInformation();
			return prop;
		}
		
		/**
		 * @return Returns an instance of ServiceInformation.
		 */
		public ServiceInformation getNewServiceIntance(){ 
			ServiceInformation serv = new ServiceInformation();
			return serv;
		}

		/**
		 * @return Returns an instance of ReferenceInformation.
		 */
		public ReferenceInformation getNewReferenceIntance(){ 
			ReferenceInformation ref = new ReferenceInformation();
			return ref;
		}
	}
	
	/**
	 * This class is a helper classes that supply additional information
	 * to the component class
	 */
	class ServiceInformation{
		/* A list containing all interfaces that the component supplys*/
		ArrayList componentInterfaces;
		
		ServiceInformation(){
			componentInterfaces  = new ArrayList();
		}
		
		/* insert a componentInterface in the list*/
		public void instertInterface(String componentInterface){
			componentInterfaces.add(componentInterface);
		}
	}
	
	/**
	 * This class is a helper classes that supply additional information
	 * to the component class
	 */
	class ReferenceInformation{
		/*component reference name*/
		String referenceName;
		/*Java interface*/
		String interfaceType;
		/*The number of service wich will bind up to this component*/
		String cardinality;
		/*Indicate when this service may be activated and deactivated, (either static or dynamic) */
		String policy;
		/* Selection filter to be used when selecting a desired service*/
		String target;
		/* The name of the component method to call when to bind*/
		String bind;
		/* The name of the component method to call when to unbind*/
		String unbind;
				
		/**
		 * @return Returns the bind.
		 */
		public String getBind() {
			return bind;
		}
		/**
		 * @param bind The bind to set.
		 */
		public void setBind(String bind) {
			this.bind = bind;
		}
		/**
		 * @return Returns the cardinality.
		 */
		public String getCardinality() {
			return cardinality;
		}
		/**
		 * @param cardinality The cardinality to set.
		 */
		public void setCardinality(String cardinality) {
			this.cardinality = cardinality;
		}
		/**
		 * @return Returns the interfaceType.
		 */
		public String getInterfaceType() {
			return interfaceType;
		}
		/**
		 * @param interfaceType The interfaceType to set.
		 */
		public void setInterfaceType(String interfaceType) {
			this.interfaceType = interfaceType;
		}
		/**
		 * @return Returns the policy.
		 */
		public String getPolicy() {
			return policy;
		}
		/**
		 * @param policy The policy to set.
		 */
		public void setPolicy(String policy) {
			this.policy = policy;
		}
		/**
		 * @return Returns the referenceName.
		 */
		public String getReferenceName() {
			return referenceName;
		}
		/**
		 * @param referenceName The referenceName to set.
		 */
		public void setReferenceName(String referenceName) {
			this.referenceName = referenceName;
		}
		/**
		 * @return Returns the target.
		 */
		public String getTarget() {
			return target;
		}
		/**
		 * @param target The target to set.
		 */
		public void setTarget(String target) {
			this.target = target;
		}
		/**
		 * @return Returns the unbind.
		 */
		public String getUnbind() {
			return unbind;
		}
		/**
		 * @param unbind The unbind to set.
		 */
		public void setUnbind(String unbind) {
			this.unbind = unbind;
		}
	}
	
	/**
	 * This class is a helper classes that supply additional information
	 * to the component class
	 */
	class PropertiesInformation{
		/* bundle entry name*/
		private String entry;
		
		/**
		 * @return Returns the entry.
		 */
		public String getEntry() {
			return entry;
		}
		/**
		 * @param entry The entry to set.
		 */
		public void setEntry(String entry) {
			this.entry = entry;
		}
	}
	
	/**
	 * This class is a helper classes that supply additional information
	 * to the component class
	 */
	class PropertyInformation{
		/* Property name*/
		private String name;
		/* Property value*/
		private String value;
		/* Property type */
		private String type;
		
		/**
		 * @return Returns the name.
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name The name to set.
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return Returns the type.
		 */
		public String getType() {
			return type;
		}
		/**
		 * @param type The type to set.
		 */
		public void setType(String type) {
			this.type = type;
		}
		/**
		 * @return Returns the value.
		 */
		public String getValue() {
			return value;
		}
		/**
		 * @param value The value to set.
		 */
		public void setValue(String value) {
			this.value = value;
		}
	}

}
