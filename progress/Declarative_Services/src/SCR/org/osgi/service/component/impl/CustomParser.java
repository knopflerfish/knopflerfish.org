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
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class CustomParser {
	
	static final String JAXP_SCHEMA_LANGUAGE =
	    "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	static final String W3C_XML_SCHEMA =
	    "http://www.w3.org/2001/XMLSchema"; 

	public ComponentDeclaration readXML(BundleEvent event) {
		/* try to get the XML file */
		String manifestEntry = (String) event.getBundle().getHeaders().get(
				"Service-Component");
		String bundleLocation = event.getBundle().getLocation();

		/* Stor the values from the xml-file in this */
		ComponentDeclaration compConf = new ComponentDeclaration();

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

				/* check if null */
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
						//parser.require(XmlPullParser.START_TAG,null,null);

						/*
						 * TODO Before moving to the next tag read all the
						 * attributes in the component tag
						 */
						while (parser.nextTag() != XmlPullParser.END_TAG) {

							if (parser.getName().equals("implementation")) {
								readXmlAdhocData(parser, "implementation",
										compConf);
							}

//							if(parser.getName().equals("service")){
//								readXmlStrict(parser);
//							}

							if (parser.getName().equals("service")) {
								readXmlAdhocData(parser, "service", compConf);
							}

							if (parser.getName().equals("reference")) {
								readXmlAdhocData(parser, "reference", compConf);
							}

							if (parser.getName().equals("property")) {
								readXmlAdhocData(parser, "property", compConf);
							}

							if (parser.getName().equals("properties")) {
								readXmlAdhocData(parser, "properties", compConf);
							}
						}

						parser.require(XmlPullParser.END_TAG,
								"http://www.osgi.org/xmlns/scr/v1.0.0",
								"component");

						//parser.require(XmlPullParser.END_DOCUMENT, null,
						// null);

					} catch (Exception e) {
						System.out.println("ParseException:" + e);
					}
					System.out
							.println("\n**************************** END *********************************");
				} else {

				}
			} catch (IOException e) {
				System.out.println("Error getting xml file" + e);
			} catch (Exception e) {
				System.out.println("Error reading zipentry" + e);
			}
		}
		/* print everything that has been found in the xml */
		printComponentConfiguration(compConf);
		
		return compConf;
	}

	private void readXmlAdhocData(XmlPullParser parser, String tag,
			ComponentDeclaration compConf) {
		/* TODO Check that all the tags that are mandatory has been set/read */
		System.out.println("Reading Ad Hoc:" + parser.getName());
		if (parser.getName().equals(tag)) {
			if (parser.getName().equals("implementation")) {
				setImplementationInfo(parser, compConf);
			} else if (parser.getName().equals("property")) {
				setPropertyInfo(parser, compConf);
			} else if (parser.getName().equals("service")) {
				setServiceInfo(parser, compConf);
			} else if (parser.getName().equals("properties")) {
				setPropertiesInfo(parser, compConf);
			} else if (parser.getName().equals("reference")) {
				setReferenceInfo(parser, compConf);
			} else if (parser.getName().equals("service")) {
				setReferenceInfo(parser, compConf);
			} else {
				try {
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						System.out.println("Set " + parser.getName() + " "
								+ parser.getAttributeName(i) + " to: "
								+ parser.getAttributeValue(i));
					}
					parser.next();
				} catch (Exception e) {
					System.out.println("Error Parsing " + tag + " tag:" + e);
				}
			}
		}
	}

	private void readXmlStrict(XmlPullParser parser) {
		if (parser.getName().equals("service")) {
			System.out.println("Reading service Strict");
			try {
				parser.require(XmlPullParser.START_TAG, "", "service");
				while (parser.nextTag() != XmlPullParser.END_TAG) {
					parser.require(XmlPullParser.START_TAG, null, null);
					String name = parser.getName();

					//String text = parser.nextText();
					//System.out.println ("<"+name+">"+text);

					if (name.equals("provide")) {
						System.out.println("Reading provide");
						parser
								.require(XmlPullParser.START_TAG, null,
										"provide");
						for (int i = 0; i < parser.getAttributeCount(); i++) {
							System.out.println("Set " + name + " "
									+ parser.getAttributeName(i) + " to: "
									+ parser.getAttributeValue(0));
						}
						parser.next();
					}
					parser.require(XmlPullParser.END_TAG, null, name);
				}
				parser.require(XmlPullParser.END_TAG, null, "service");
			} catch (Exception e) {
				System.out.println("Error Parsing Service:" + e);
			}
		}
	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -servicefactory =
	 * bundle entry name (mandatory)
	 */
	public void setServiceInfo(XmlPullParser parser,
			ComponentDeclaration compConf) {

	}

	/**
	 * 
	 * @param parser
	 * @param compConf
	 * 
	 * Parses out the following values from the reference tag -entry = bundle
	 * entry name (mandatory)
	 */
	private void setPropertiesInfo(XmlPullParser parser,
			ComponentDeclaration compConf) {
		ComponentPropertiesInfo compProps = compConf.getNewPropertiesIntance();
		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("entry")) {
					if (parser.getAttributeValue(i) == null) {
						/* TODO Fix a better error */
						System.out.println("No value in mandatory attribute:"
								+ parser.getAttributeName(i)
								+ " in properties tag");
					} else {
						compProps.setEntry(parser.getAttributeValue(i));
					}
				} else {
					System.out.println("Unsupported Attribute name:"
							+ parser.getAttributeName(i));
				}
			}
			parser.next();
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag:" + e);
		}
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
	private void setImplementationInfo(XmlPullParser parser,
			ComponentDeclaration compConf) {
		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeValue(i) == null) {
					/* TODO Fix a better error */
					System.out.println("No value in mandatory attribute:"
							+ parser.getAttributeName(i)
							+ " in implementation tag");
				} else {
					compConf.setImplementation(parser.getAttributeValue(i));
				}
			}
			parser.next();
		} catch (Exception e) {
			System.out.println("Error Parsing implementation tag:" + e);
		}
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
	private void setPropertyInfo(XmlPullParser parser,
			ComponentDeclaration compConf) {
		ComponentPropertyInfo compProp = compConf.getNewPropertyIntance();
		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("name")) {
					if (parser.getAttributeValue(i) == null) {
						/* TODO Fix a better error */
						System.out.println("No value in mandatory attribute:"
								+ parser.getAttributeName(i)
								+ " in reference tag");
					} else {
						compProp.setName(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("value")) {
					if (parser.getAttributeValue(i) == null) {
						/* optional value */
						/*
						 * TODO Search the body of the <ptroperty> tag for
						 * multiple values Means that the value variable needs
						 * to be changed to an ArrayList in order to store
						 * multiple value
						 */
					} else {
						compProp.setValue(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("type")) {
					if (parser.getAttributeValue(i) == null) {
						/* default value */
						compProp.setValue("String");
					} else {
						compProp.setValue(parser.getAttributeValue(i));
					}
				} else {
					System.out.println("Unsupported Attribute name:"
							+ parser.getAttributeName(i));
				}
			}
			parser.next();
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag:" + e);
		}
		/* add the ComponentPropertyInfo to the ComponentConfiguration */
		compConf.addPropertyInfo(compProp);
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
	private void setReferenceInfo(XmlPullParser parser,
			ComponentDeclaration compConf) {
		ComponentReferenceInfo compRef = compConf.getNewReferenceIntance();
		try {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("name")) {
					if (parser.getAttributeValue(i) == null) {
						/* TODO Fix a better error */
						System.out.println("No value in mandatory attribute:"
								+ parser.getAttributeName(i)
								+ " in reference tag");
					} else {
						compRef.setReferenceName(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("interface")) {
					if (parser.getAttributeValue(i) == null) {
						/* TODO Fix a better error */
						System.out.println("No value in mandatory attribute:"
								+ parser.getAttributeName(i)
								+ " in reference tag");
					} else {
						compRef.setInterfaceType(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("cardinality")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setCardinality("1..1");
					} else {
						compRef.setCardinality(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("policy")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setPolicy("static");
					} else {
						compRef.setPolicy(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("target")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setTarget("(objectClass="
								+ compRef.getInterfaceType() + ")");
					} else {
						compRef.setTarget(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("bind")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setBind(null);
					} else {
						compRef.setBind(parser.getAttributeValue(i));
					}
				} else if (parser.getAttributeName(i).equals("unbind")) {
					if (parser.getAttributeValue(i) == null) {
						/* set default value if non pressent */
						compRef.setUnbind(null);
					} else {
						compRef.setUnbind(parser.getAttributeValue(i));
					}
				} else {
					System.out.println("Unsupported Attribute name:"
							+ parser.getAttributeName(i));
				}

			}
			parser.next();
		} catch (Exception e) {
			System.out.println("Error Parsing reference tag:" + e);
		}
		/* add the ComponentReferenceInfo to the ComponentConfiguration */
		compConf.addReferenceInfo(compRef);
	}

	private void printComponentConfiguration(ComponentDeclaration compConf) {

		System.out
				.println("------------Everything in the <component> tag------------");
		System.out.println("Name:" + compConf.getComponentName());
		System.out.println("Autoenable:" + compConf.isAutoEnable());
		System.out.println("Factory" + compConf.getFactory());

		System.out
				.println("------------Everything in the <implementation> tag------------");
		System.out.println("Class:" + compConf.getClass());

		System.out
				.println("------------Everything in the <property> tag------------");
		ArrayList propertyInfo = compConf.getPropertyInfo();
		int size = propertyInfo.size();
		for (int i = 0; i < size; i++) {
			ComponentPropertyInfo compProp = (ComponentPropertyInfo) propertyInfo
					.get(i);
			System.out.println("Name:" + compProp.getName());
			System.out.println("Value:" + compProp.getValue());
			System.out.println("Type:" + compProp.getType()); /*
															   * TODO This part
															   * will not work
															   * later on fix so
															   * that is
															   * traverse an
															   * array
															   */
		}

		System.out
				.println("------------Everything in the <properties> tag------------");
		ArrayList propertiesInfo = compConf.getPropertiesInfo();
		size = propertiesInfo.size();
		for (int i = 0; i < size; i++) {
			ComponentPropertiesInfo compProps = (ComponentPropertiesInfo) propertiesInfo
					.get(i);
			System.out.println("Entry:" + compProps.getEntry());
		}

		System.out
				.println("------------Everything in the <service> tag------------");
		ArrayList serviceInfo = compConf.getServiceInfo();
		size = serviceInfo.size();
		for (int i = 0; i < size; i++) {
			ComponentServiceInfo compProps = (ComponentServiceInfo) propertiesInfo
					.get(i);
			System.out.println("Entry:" + compProps.getClass());
			/* TODO get the interfaces out to */
		}

		System.out
				.println("------------Everything in the <provide> tag------------");
		/* TODO print everything that is in the provide tag */
		System.out
				.println("------------Everything in the <reference> tag------------");
		/* TODO print everything that is in the reference tag */
	}
}