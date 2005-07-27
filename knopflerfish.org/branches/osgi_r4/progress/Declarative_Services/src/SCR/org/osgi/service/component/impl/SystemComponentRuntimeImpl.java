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

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.xml.sax.Parser;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
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
							
						
						}
					
						parser.require(XmlPullParser.END_TAG,"http://www.osgi.org/xmlns/scr/v1.0.0","component");
						
						//parser.require(XmlPullParser.END_DOCUMENT, null, null);
						
					}catch(Exception e){
						System.out.println("ParseException:" +e);
					}
					
					
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
		System.out.println("Parsing:" + parser.getName());
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
			System.out.println("Parsing service");
			try{
				parser.require(XmlPullParser.START_TAG,"","service");
				while (parser.nextTag() != XmlPullParser.END_TAG) {

					parser.require(XmlPullParser.START_TAG, null, null);
					String name = parser.getName();

					//String text = parser.nextText();
					//System.out.println ("<"+name+">"+text);

					if(name.equals("provide")){
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

}
