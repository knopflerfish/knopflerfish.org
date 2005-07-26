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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.xml.sax.Attributes;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This class is the implementation of the declarative service
 * feature. It will locate and bind diffrent types of declared
 * components on demand. It will also listen to BundleEvents 
 * rasied within the framework and 
 */
public class SystemComponentRuntimeImpl implements BundleListener{
	/* variable holding the bundlecontext */
	BundleContext bundleContext;  
	
	public SystemComponentRuntimeImpl(BundleContext context){
		/* assign the bundlecontext */
		bundleContext=context;
		/* add this as a bundle listener */
		bundleContext.addBundleListener(this);
	}
	
	/**
	 *  Listen for BundleEvents from the framework
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
			/* print the bundle  location */
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
					/* variable holding EOF */
					int c;
					/* buffer size */
					Long bufferSize = new Long(zipEntry.getSize());
					/* byte buffer with size of bufferSize */
					byte buffer[] = new byte[bufferSize.intValue()];
					/* string */
					String XMLContents="";
					/* byte array outputstream using with the same size as the buffer */
					OutputStream outputStream = new ByteArrayOutputStream(bufferSize.intValue()); 
					
					/* read the inputstream */
					while ((c = inputStream.read(buffer)) != -1){
						/* write it to the ByteArray */
						 outputStream.write(buffer,0,c);
						
					}
					
					/* assign the string variable */
					XMLContents=outputStream.toString();
					/* write the XML File Contents */
					System.out.println("\nXML-file contents:\n" +XMLContents );
				
					
					
				    // specify this in system properties
		            // or use one of the next 2 lines
		            //XMLReader reader = XMLReaderFactory.createXMLReader();
		            //XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
		            XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.crimson.parser.XMLReaderImpl");
		            
		            reader.setFeature("http://xml.org/sax/features/validation", true);
		            reader.setFeature("http://apache.org/xml/features/validation/schema", true);
		            reader.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", "T.xsd");
	
	
		            CustomHandler handler = new CustomHandler();
		            
		            reader.setContentHandler(handler);
		            reader.setErrorHandler(handler);
		            reader.parse("T.xml");
					
					
					
				}else{
					
				}
				
				
				
			}catch(IOException e){
				System.out.println("Error getting xml file" + e) ;
			}catch(Exception e){
				System.out.println("Error reading zipentry" + e);
			}
			
		}
	}
	
	
	
	class CustomHandler extends DefaultHandler {
	    public void startElement(String uri,   String localName,
	                             String qName, Attributes attributes)  
	                             throws SAXException {

	        System.out.println("tag: " + qName);
	    }
	    public void warning(SAXParseException e) throws SAXException {
	        // you can choose not to handle it
	        throw new SAXException(getMessage("Warning", e));
	    }
	    public void error(SAXParseException e) throws SAXException {
	        throw new SAXException(getMessage("Error", e));
	    }
	    public void fatalError(SAXParseException e) throws SAXException 
	    {
	        throw new SAXException(getMessage("Fatal Error", e));
	    }
	    private String getMessage(String level, SAXParseException e) {
	        return ( "Parsing " + level + "\n" +
	                 "Line:    " + e.getLineNumber() + "\n" +
	                 "URI:     " + e.getSystemId() + "\n" +
	                 "Message: " + e.getMessage()             );
	    }
	}

}
