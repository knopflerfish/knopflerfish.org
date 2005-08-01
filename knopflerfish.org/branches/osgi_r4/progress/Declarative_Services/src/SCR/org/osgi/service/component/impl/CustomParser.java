/*
 * @(#)CustomParser.java        1.0 2005/06/29
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
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.BundleEvent;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * @author Martin Berg, Magnus Klack
 * 
 * This is an example of the xml file
 * 
 * <!-- Required, Allways at least one attribute --> 
 * <scr:component name="component name"
 * 		autoenable="boolean" 
 * 		factory="component.factory property value"
 * 		xmlns:scr="http://www.osgi.org/xmlns/scr/v1.0.0">
 * 
 * <!-- Required, Allways one attribute --> 
 * <implementation class="Java implementation class"/>
 * 
 * <!-- Optional allways an attribute but if no value attribute then these are 
 * specified in between start and end tag-->
 * <property 
 * 		name="property name" 
 * 		value="property value" 
 * 		type="property type">
 * <!-- Att least one row if no value attribute "value" --> 
 * property value
 * </property>
 * 
 * <!-- Optional, Allways an attribute --> 
 * <properties entry="bundle entry name"/>
 * 
 * <!-- Optional, not allways an attribute --> 
 * <service servicefactory="boolean"> 
 * 		<!-- Required if service pressent, Allways an attribute --> 
 * 		<provide interface="Java interface type"/>
 * </service>
 * 
 * <!-- Optional, Allways an attribute --> 
 * <reference name="reference name"
 * 		interface="Java interface type" 
 * 		cardinality="reference cardinality"
 * 		policy="reference policy" 
 * 		target="target filter" 
 * 		bind="bind method"
 * 		unbind="unbind method" 
 * /> 
 * </scr:component>
 */

public class CustomParser {

	private ComponentDeclaration compConf;

	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public CustomParser() {
		/* Store the values from the xml-file in this */
		compConf = new ComponentDeclaration();
	}

	public ComponentDeclaration readXML(BundleEvent event) {

		/* try to get the XML file */
		String manifestEntry = (String) event.getBundle().getHeaders().get(
				"Service-Component");
		String bundleLocation = event.getBundle().getLocation();

		/* check if null */
		if (manifestEntry != null) {
			System.out
					.println("\n**************************** START ********************************");
			/* print that a service component is found */
			System.out.println("Found service component");
			/* print the bundle location */
			System.out.println("The bundle location: " + bundleLocation);
			/* format the location string */
			String formattedLocation = bundleLocation.substring(5,
					bundleLocation.length());
			/* print the bundle formatted location */
			System.out.println("The bundle formatted location: "
					+ formattedLocation);
			/* print the xml file location */
			System.out.println("The XML file location: " + manifestEntry);

			try {
				/* get the jar file use the formatted location */
				JarFile jarFile = new JarFile(formattedLocation);
				/* get the xmlfile located by the manifestEntry */
				ZipEntry zipEntry = jarFile.getEntry(manifestEntry);

				/* check if null *///TODO This mabey should be done in the SCR
				if (zipEntry != null) {

					/* get the input stream */
					InputStream inputStream = jarFile.getInputStream(zipEntry);

					/* create the parser */
					try {
						XmlPullParserFactory factory = XmlPullParserFactory
								.newInstance();
						factory.setNamespaceAware(true);

						XmlPullParser parser = factory.newPullParser();

						parser.setInput(inputStream, null);
						parser.nextTag();

						parser.require(XmlPullParser.START_TAG,
								"http://www.osgi.org/xmlns/scr/v1.0.0",
								"component");

						setComponentInfo(parser);
						while (parser.nextTag() != XmlPullParser.END_TAG) { 
							//TODO The parser does not skip commented parts ie <!--  comment  -->
							System.out.println("Current tag is:"
									+ parser.getName());
							if (parser.getName().equals("implementation")) {
								setImplementationInfo(parser);
							} else if (parser.getName().equals("property")) {
								setPropertyInfo(parser);
							} else if (parser.getName().equals("properties")) {
								setPropertiesInfo(parser);
							} else if (parser.getName().equals("service")) {
								setServiceInfo(parser);
							} else if (parser.getName().equals("reference")) {
								setReferenceInfo(parser);
							} else {
								System.out.println("Unsupported tag");
							}
						}
						parser.require(XmlPullParser.END_TAG,
								"http://www.osgi.org/xmlns/scr/v1.0.0",
								"component");
					} catch (IllegalXMLException e){
						System.out.println("IllegalXMLException:" + e);
					} catch (Exception e) {
						System.out.println("ParseException:" + e);
					}
					System.out
							.println("\n**************************** END *********************************");
					/* print everything that has been found in the xml */
					printComponentConfiguration();
				} else {
					/*
					 * TODO If we keep the check of the xml file here we need
					 * this to return null.
					 */
				}
			} catch (IOException e) {
				System.out.println("Error getting xml file" + e);
			} catch (Exception e) {
				System.out.println("Error reading zipentry" + e);
			}
		}
		
		//TODO Before returning the component check if all required tags are pressent
		return compConf;
	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -name = property
	 * name (mandatory) -value = property value (otional) -type = property type
	 * (optional, default "String")
	 */
	private void setPropertyInfo (XmlPullParser parser)
			throws IllegalXMLException {
		/* Required attributes in the component tag*/
		boolean nameFound = false;
		boolean typeFound = false;
		
		/* Supported types in the type attribute*/
		String[] supportedTypes = {"String", "Long", "Double", "Float", "Integer", "Byte", "Char", "Boolean", "Short"};
		
		/*Declare an instanse to store the values in */
		ComponentPropertyInfo compProp = compConf.getNewPropertyIntance();
		/* test print */
		System.out.println("*-*-*-*-*-* Parsing Property *-*-*-*-*-*");

		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("name")) {
					if (parser.getAttributeValue(i) == null) {
						throw new IllegalXMLException(
								"No value in mandatory attribute:"
										+ parser.getAttributeName(i)
										+ " in property tag");
					} else {
						compProp.setName(parser.getAttributeValue(i));
						nameFound = true;
						/* test print */
						System.out.println("Adding the name attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("value")) {
					if (parser.getAttributeValue(i) == null) {
						/* optional value */
					} else {
						compProp.setValue(parser.getAttributeValue(i));
						/* test print */
						System.out.println("Adding the value attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("type")) {
					//TODO Fix so that only supported types are accepted
					if (parser.getAttributeValue(i) == null) {
						/* default value */
						compProp.setType("String"); //TODO fix so that this default sets even if the type attribute isnt pressent
					} else {
						for(int j = 0; j < supportedTypes.length ; j++){
							/* If the found attribute value equals one of the supported types*/
							if(parser.getAttributeValue(i).toLowerCase().equals(supportedTypes[j].toLowerCase())){
								compProp.setType(supportedTypes[j]);
								typeFound = true;
								/* test print */
								System.out.println("Adding the type attribute:"
										+ parser.getAttributeValue(i));
							}
						}
					}
				} else {
					throw new IllegalXMLException("Unsupported Attribute name:"
							+ parser.getAttributeName(i));
				}
			}
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag:" + e);
		}
		
		/*
		 * need to do this because: 1: If no value was found in the attribute,
		 * they should be here 2: If a value was found we needs to traverse
		 * through anyway
		 */
		try {
			/* if no values was found read them between start and endtag */
			if (compProp.getValues().size() < 1) {
				String text = parser.nextText();
				String[] values = text.split("\n");
				for (int i = 0; i < values.length; i++) {
					compProp.setValue(values[i]);
					/* test print */
					System.out.println("Adding the value attribute:"
							+ values[i]);
				}
			} else {
				/* test print */
				System.out.println("Not adding " + parser.nextText());
			}
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag2:" + e);
		}

		/* Setting default values if no other has been sett*/
		if(compProp.getType() == null){
			compProp.setType(supportedTypes[0]);
		}

		/* check if required attributes has been set */
		if(nameFound == false){
			throw new IllegalXMLException("A required attribute in the tag:property was not pressent");
		}

		/* check if required type has been set */
		if( typeFound == false){
			throw new IllegalXMLException("A illegal type in the attribute:type in the tag:property");
		}
		
		/* Test print */
		System.out.println("*-*-*-*-*-* Finished Parsing Property *-*-*-*-*-*");
		
		/* add the ComponentPropertyInfo to the ComponentConfiguration */
		compConf.addPropertyInfo(compProp);
	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -servicefactory =
	 * bundle entry name (optional) -interface = Java interface type
	 */
	private void setServiceInfo(XmlPullParser parser)
			throws IllegalXMLException {
		/* Required attributes in the component tag*/
		boolean interfaceFound = false;
		
		/* Test print*/
		System.out.println("*-*-*-*-*-* Parsing Services *-*-*-*-*-*");
		ComponentServiceInfo compServ = compConf.getNewServiceIntance();

		/* test print */
		System.out.println("Current tag is:" + parser.getName());
		/* If there is an attribute in the service tag */
		if (parser.getAttributeCount() > 0) {
			System.out.println("There is an attribute in the service tag");
			try {
				for (int i = 0; i < parser.getAttributeCount(); i++) {
					if (parser.getAttributeName(i).equals("servicefactory")) {
						if (parser.getAttributeValue(i).equals("true")) {
							compServ.setServiceFactory(true);
						} else if (parser.getAttributeValue(i).equals("false")) {
							compServ.setServiceFactory(false);
						} else {
							throw new IllegalXMLException(
									"Unsupported value tag service, attribute servicefactory:"
											+ parser.getAttributeValue(i));
						}
						/* test print */
						System.out
								.println("Adding the servicefactory attribute:"
										+ parser.getAttributeValue(i));
					} else {
						throw new IllegalXMLException(
								"Unsupported Attribute name:"
										+ parser.getAttributeName(i));
					}
				}
			} catch (Exception e) {
				System.out.println("Error Parsing reference tag:" + e);
			}
		}

		/* Get the interfaces */
		try {
			parser.require(XmlPullParser.START_TAG, "", "service");
			while (parser.nextTag() != XmlPullParser.END_TAG) {
				parser.require(XmlPullParser.START_TAG, null, null);
				String name = parser.getName();
				if (name.equals("provide")) {
					/* test print */
					System.out.println("Reading provide");
					parser.require(XmlPullParser.START_TAG, null, "provide");
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						if (parser.getAttributeName(i).equals("interface")) {
							compServ.instertInterface(parser
									.getAttributeValue(i));
							interfaceFound = true;
							/* test print */
							System.out
									.println("Adding the interface attribute:"
											+ parser.getAttributeValue(i));
						} else {
							throw new IllegalXMLException(
									"Unsupported Attribute name:"
											+ parser.getAttributeName(i));
						}
					}
					parser.next();
				}
				parser.require(XmlPullParser.END_TAG, null, name);
			}
			parser.require(XmlPullParser.END_TAG, null, "service");
		} catch (Exception e) {
			System.out.println("Error Parsing Service:" + e);
		}

		/* check if required attributes has been set */
		if(interfaceFound == false){
			throw new IllegalXMLException("A required attribute in the tag:provide was not pressent");
		}
		
		/* add the ComponentPropertyInfo to the ComponentConfiguration */
		compConf.addServiceInfo(compServ);
	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -entry = bundle
	 * entry name (mandatory)
	 */
	private void setComponentInfo(XmlPullParser parser)
			throws IllegalXMLException {
		/* Required attributes in the component tag*/
		boolean nameFound = false;
		
		/* test print */
		System.out.println("*-*-*-*-*-* Parsing Component *-*-*-*-*-*");
		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("name")) {
					if (parser.getAttributeValue(i) == null) {
						throw new IllegalXMLException(
								"No value in mandatory attribute:"
										+ parser.getAttributeName(i)
										+ " in component tag");
					} else {
						compConf.setComponentName(parser.getAttributeValue(i));
						nameFound = true;
						/* test print */
						System.out.println("Adding the name attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("autoenable")) {
					if (parser.getAttributeValue(i) == null) {
						/* optional attribute */
					} else {
						if (parser.getAttributeValue(i).equals("true")) {
							compConf.setAutoEnable(true);
						} else {
							compConf.setAutoEnable(false);
						}
						/* test print */
						System.out.println("Adding the autoenable attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("factory")) {
					if (parser.getAttributeValue(i) == null) {
						/*optional attribute */
					} else {
						compConf.setFactory(parser.getAttributeValue(i));
						/* test print */
						System.out.println("Adding the factory attribute:"
								+ parser.getAttributeValue(i));
					}
				} else {
					throw new IllegalXMLException("Unsupported Attribute name:"
							+ parser.getAttributeName(i));
				}
			}
			parser.next();
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag:" + e);
		}
		
		/* check if required attributes has been set */
		if(nameFound == false){
			throw new IllegalXMLException("A required attribute in the tag:component was not pressent");
		}
	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -entry = bundle
	 * entry name (mandatory)
	 */
	private void setPropertiesInfo(XmlPullParser parser)
			throws IllegalXMLException {
		/* Required attributes in the component tag*/
		boolean entryFound = false;
		
		ComponentPropertiesInfo compProps = compConf.getNewPropertiesIntance();
		/* test print */
		System.out.println("*-*-*-*-*-* Parsing Properties *-*-*-*-*-*");
		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("entry")) {
					if (parser.getAttributeValue(i) == null) {
						throw new IllegalXMLException(
								"No value in mandatory attribute:"
										+ parser.getAttributeName(i)
										+ " in properties tag");
					} else {
						compProps.setEntry(parser.getAttributeValue(i));
						entryFound = true;
						/* test print */
						System.out.println("Adding the autoenable entry:"
								+ parser.getAttributeValue(i));
					}
				} else {
					throw new IllegalXMLException("Unsupported Attribute name:"
							+ parser.getAttributeName(i));
				}
			}
			parser.next();
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag:" + e);
		}

		/* check if required attributes has been set */
		if(entryFound == false){
			throw new IllegalXMLException("A required attribute in the tag:properties was not pressent");
		}
		
		/* test print */
		System.out
				.println("*-*-*-*-*-* Finished parsing Properties *-*-*-*-*-*");

		/* add the ComponentPropertyInfo to the ComponentConfiguration */
		compConf.addPropertiesInfo(compProps);
	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -class = Java
	 * implementation class (mandatory)
	 */
	private void setImplementationInfo(XmlPullParser parser)
			throws IllegalXMLException {
		/* Required attributes in the component tag*/
		boolean classFound = false;
		
		/* test print */
		System.out.println("*-*-*-*-*-* Parsing Implementation *-*-*-*-*-*");
		if (compConf.getImplementation() == null) {
			try {
				for (int i = 0; i < parser.getAttributeCount(); i++) {
					if (parser.getAttributeName(i).equals("class")) {
						if (parser.getAttributeValue(i) == null) {
							throw new IllegalXMLException(
									"No value in mandatory attribute:"
											+ parser.getAttributeName(i)
											+ " in implementation tag");
						} else {
							compConf.setImplementation(parser
									.getAttributeValue(i));
							classFound = true;
							/* test print */
							System.out.println("Adding the class attribute:"
									+ parser.getAttributeValue(i));
						}
					} else {
						throw new IllegalXMLException(
								"Unsupported Attribute name:"
										+ parser.getAttributeName(i));
					}
				}
				parser.next();
			} catch (Exception e) {
				System.out.println("Error Parsing implementation tag:" + e);
			}
		} else {
			throw new IllegalXMLException("Only one implementation tag allowed");
		}

		/* check if required attributes has been set */
		if(classFound == false){
			throw new IllegalXMLException("A required attribute in the tag:implementation was not pressent");
		}
		
		/* test print */
		System.out
				.println("*-*-*-*-*-* Finished parsing Implementation *-*-*-*-*-*");
	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -name = reference
	 * name (mandatory) -interface = Java interface name (mandatory)
	 * -cardinality = reference cadinality (if not specified then "1..1")
	 * -policy = reference policy (if not specified then "static") -target =
	 * target filter (if not specified "(objectClass="+ <interface-name>+")"
	 * -bind = bind method (optional) -unbund = unbind method (optional)
	 */
	private void setReferenceInfo(XmlPullParser parser)
			throws IllegalXMLException {
		/* Required attributes in the component tag*/
		boolean nameFound = false;
		boolean interfaceFound = false;
		boolean cardinalityFound = false;
		
		String[] supportedCardniality = {"0..1", "0..n", "1..1", "1..n"};
		
		ComponentReferenceInfo compRef = compConf.getNewReferenceIntance();
		/* test print */
		System.out.println("*-*-*-*-*-* Parsing References *-*-*-*-*-*");
		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("name")) {
					if (parser.getAttributeValue(i) == null) {
						throw new IllegalXMLException(
								"No value in mandatory attribute:"
										+ parser.getAttributeName(i)
										+ " in reference tag");
					} else {
						compRef.setReferenceName(parser.getAttributeValue(i));
						nameFound = true;
						/* test print */
						System.out.println("Adding the name attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("interface")) {
					if (parser.getAttributeValue(i) == null) {
						throw new IllegalXMLException(
								"No value in mandatory attribute:"
										+ parser.getAttributeName(i)
										+ " in reference tag");
					} else {
						compRef.setInterfaceType(parser.getAttributeValue(i));
						interfaceFound = true;
						/* test print */
						System.out.println("Adding the interface attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("cardinality")) { 
					//TODO Fix so that there is a check of the cardinality types so only supported are pressent
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setCardinality("1..1"); 
						//TODO fix so that this is set even if no attribute was found
					} else {
						for(int j = 0; j < supportedCardniality.length ; j++){
							/* If the found attribute value equals one of the supported types*/
							if(parser.getAttributeValue(i).toLowerCase().equals(supportedCardniality[j].toLowerCase())){
								compRef.setCardinality(supportedCardniality[j]);
								cardinalityFound = true;
								/* test print */
								System.out.println("Adding the type attribute:"
										+ parser.getAttributeValue(i));
							}
						}
						compRef.setCardinality(parser.getAttributeValue(i));
						/* test print */
						System.out.println("Adding the cardinality attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("policy")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setPolicy("static");
					} else {
						compRef.setPolicy(parser.getAttributeValue(i));
						/* test print */
						System.out.println("Adding the policy attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("target")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setTarget("(objectClass="
								+ compRef.getInterfaceType() + ")");
					} else {
						compRef.setTarget(parser.getAttributeValue(i));
						/* test print */
						System.out.println("Adding the target attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("bind")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setBind(null);
					} else {
						compRef.setBind(parser.getAttributeValue(i));
						/* test print */
						System.out.println("Adding the bind attribute:"
								+ parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("unbind")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setUnbind(null);
					} else {
						compRef.setUnbind(parser.getAttributeValue(i));
						/* test print */
						System.out.println("Adding the unbind attribute:"
								+ parser.getAttributeValue(i));
					}
				} else {
					throw new IllegalXMLException("Unsupported Attribute name:"
							+ parser.getAttributeName(i));
				}
			}
			parser.next();
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag:" + e);
		}
		
		/* Setting default values if no other has been sett*/
		if(compRef.getCardinality() == null){
			compRef.setCardinality(supportedCardniality[2]);
		}
		
		/* check if required attributes has been set */
		if(nameFound == false && interfaceFound == false){
			throw new IllegalXMLException("A required attribute in the tag:reference was not pressent");
		}
		
		/* test print */
		System.out
				.println("*-*-*-*-*-* Finished Parsing References *-*-*-*-*-*");
		/* add the ComponentReferenceInfo to the ComponentConfiguration */
		compConf.addReferenceInfo(compRef);
	}

	/* Test function that prints the contents of a component */
	private void printComponentConfiguration() {
		printComponent();
		printImplementation();
		printProperty();
		printProperties();
		printService();
		printReference();
	}

	/* Test function that prints a part of a component */
	private void printComponent() {
		/* <component> */
		System.out
				.println("------------Everything in the <component> tag------------");
		System.out.println("Name:" + compConf.getComponentName());
		System.out.println("Autoenable:" + compConf.isAutoEnable());
		System.out.println("Factory:" + compConf.getFactory());
	}

	/* Test function that prints a part of a component */
	private void printImplementation() {
		/* <implementation> */
		System.out
				.println("------------Everything in the <implementation> tag------------");
		System.out.println("Class:" + compConf.getClass());
	}

	/* Test function that prints a part of a component */
	private void printProperty() {
		/* <property> */
		System.out
				.println("------------Everything in the <property> tag------------");
		ArrayList propertyInfo = compConf.getPropertyInfo();
		System.out.println("The number of property is:" + propertyInfo.size());
		for (int i = 0; i < propertyInfo.size(); i++) {
			ComponentPropertyInfo compProp = (ComponentPropertyInfo) propertyInfo
					.get(i);
			System.out.println("Name:" + compProp.getName());
			ArrayList values = compProp.getValues();
			for (int j = 0; j < values.size(); j++) {
				System.out.println("Value:" + values.get(j));
			}
			System.out.println("Type:" + compProp.getType());
			/*
			 * TODO This part will not work later on fix so that it traverse an
			 * array
			 */
		}
	}

	/* Test function that prints a part of a component */
	private void printProperties() {
		/* <properties> */
		System.out
				.println("------------Everything in the <properties> tag------------");
		ArrayList propertiesInfo = compConf.getPropertiesInfo();
		System.out.println("The number of properties is:"
				+ propertiesInfo.size());
		for (int i = 0; i < propertiesInfo.size(); i++) {
			ComponentPropertiesInfo compProps = (ComponentPropertiesInfo) propertiesInfo
					.get(i);
			System.out.println("Entry:" + compProps.getEntry());
		}
	}

	/* Test function that prints a part of a component */
	private void printService() {
		/* <service> */
		System.out
				.println("------------Everything in the <service> tag------------");
		ArrayList serviceInfo = compConf.getServiceInfo();
		System.out.println("The number of services is:" + serviceInfo.size());
		for (int i = 0; i < serviceInfo.size(); i++) {
			ComponentServiceInfo compServ = (ComponentServiceInfo) serviceInfo
					.get(i);
			System.out.println("ServiceFactory:" + compServ.isServiceFactory());
			ArrayList interfaces = compServ.getComponentInterfaces();
			System.out.println("The number of interfaces in this service is:"
					+ serviceInfo.size());
			for (int j = 0; j < interfaces.size(); j++) {
				System.out.println("Interface:" + interfaces.get(i));
			}
		}
	}

	/* Test function that prints a part of a component */
	private void printReference() {
		/* <reference> */
		System.out
				.println("------------Everything in the <reference> tag------------");
		ArrayList referenceInfo = compConf.getReferenceInfo();
		System.out.println("The number of references is:"
				+ referenceInfo.size());
		for (int i = 0; i < referenceInfo.size(); i++) {
			ComponentReferenceInfo compRefs = (ComponentReferenceInfo) referenceInfo
					.get(i);
			System.out.println("Name:" + compRefs.getReferenceName());
			System.out.println("Interfaces:" + compRefs.getInterfaceType());
			System.out.println("Cardniality:" + compRefs.getCardinality());
			System.out.println("Policy:" + compRefs.getPolicy());
			System.out.println("Target:" + compRefs.getTarget());
			System.out.println("Bind:" + compRefs.getBind());
			System.out.println("Unbind:" + compRefs.getUnbind());
		}
	}
}