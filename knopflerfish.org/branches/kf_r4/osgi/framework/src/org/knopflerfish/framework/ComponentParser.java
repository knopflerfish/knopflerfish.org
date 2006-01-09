/*
 * Copyright (c) 2005, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import org.osgi.service.log.LogService;

/**
 * @author Martin Berg, Magnus Klack (refactoring by Björn Andersson)
 *
 * This is an example of the xml file
 *
 * <!-- Required, Allways at least one attribute -->
 * <scr:component name="component name"
 *    enabled="boolean"
 *    factory="component.factory property value"
 *    xmlns:scr="http://www.osgi.org/xmlns/scr/v1.0.0">
 *
 * <!-- Required, Allways one attribute -->
 * <implementation class="Java implementation class"/>
 *
 * <!-- Optional allways an attribute but if no value attribute then these are
 * specified in between start and end tag-->
 * <property
 *    name="property name"
 *    value="property value"
 *    type="property type">
 * <!-- Att least one row if no value attribute "value" -->
 * property value
 * </property>
 *
 * <!-- Optional, Allways an attribute -->
 * <properties entry="bundle entry name"/>
 *
 * <!-- Optional, not allways an attribute -->
 * <service servicefactory="boolean">
 *    <!-- Required if service pressent, Allways an attribute -->
 *    <provide interface="Java interface type"/>
 * </service>
 *
 * <!-- Optional, Allways an attribute -->
 * <reference name="reference name"
 *    interface="Java interface type"
 *    cardinality="reference cardinality"
 *    policy="reference policy"
 *    target="target filter"
 *    bind="bind method"
 *    unbind="unbind method"
 * />
 * </scr:component>
 */

public class ComponentParser {

  static String[] supportedTypes = {"String", "Long", "Double", 
				    "Float", "Integer", "Byte", 
				    "Char", "Boolean", "Short"};
  
  static String[] supportedCardniality = {"0..1", "0..n", "1..1", "1..n"};
  static private String COMPONENT_NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

  public static ArrayList readXML(Bundle declaringBundle, URL url) throws IllegalXMLException {

    try {
      if (url == null) {
        return null; // TODO: is this safe? was compConf.
      }
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);

      XmlPullParser parser = factory.newPullParser();
      parser.setInput(url.openStream(), null);
      return readDocument(declaringBundle, parser);

    } catch (IOException e) {
      throw new IllegalXMLException("Error getting xml file" + e,e.getCause());
    } catch (Exception e) {
      throw new IllegalXMLException("Error reading zipentry" + e,e.getCause());
    }
  }
  
  private static ArrayList readDocument(Bundle declaringBundle, XmlPullParser parser) {

    ArrayList decls = new ArrayList();
    boolean foundImplementation = false;
    try {
      while (parser.next() != XmlPullParser.END_DOCUMENT) {
	
	if (parser.getEventType() != XmlPullParser.START_TAG &&
	    parser.getEventType() != XmlPullParser.END_TAG) {
	  continue; // nothing of interest to us.
	}
	
	if (parser.getName().equals("component") &&
	    parser.getEventType() == XmlPullParser.START_TAG &&
	    (parser.getDepth() == 1 || // assume scr here. The root is component
	     COMPONENT_NAMESPACE_URI.equals(parser.getNamespace()))) {
	  // we have found a proper component-tag here.
	  
	  try {
	    ComponentDeclaration dec = readComponent(declaringBundle, parser);
	    decls.add(dec);
	    
	  } catch (Exception e) {
	    ComponentRuntimeImpl.log(LogService.LOG_ERROR, "Could not read component-tag. Got exception.", e);
	  }
	}
      }
    } catch (Exception e) {
      ComponentRuntimeImpl.log(LogService.LOG_ERROR, "Could not read component-tag. Got exception.", e);
    }

    return decls;
  }
  
  private static ComponentDeclaration readComponent(Bundle bundle, XmlPullParser parser) 
    throws XmlPullParserException,
	   IOException,
	   IllegalXMLException {
    
    ComponentDeclaration curr = new ComponentDeclaration(bundle);
    setComponentInfo(curr, parser); 

    int event = parser.getEventType();
    
    boolean serviceVisited = false; // only one service-tag allowed.

    while (event != XmlPullParser.END_TAG) {

      if (event != XmlPullParser.START_TAG) { // nothing of interest.
	event = parser.next();
	continue;
      }

      if (parser.getName().equals("implementation")) {
	setImplementationInfo(curr, parser);
	
      } else if (parser.getName().equals("property")) {
	setPropertyInfo(curr, parser);
	
      } else if (parser.getName().equals("properties")) {
	setPropertiesInfo(curr, parser);
	
      } else if (parser.getName().equals("service")) {

	if (!serviceVisited) {
	  setServiceInfo(curr, parser);
	  parser.next();
	} else 
	  throw new 
	    IllegalXMLException("More than one service-tag " +
				"in component: \"" + curr.getComponentName()
				+ "\"");
	
      } else if (parser.getName().equals("reference")) {
	setReferenceInfo(curr, parser);
	
      } else {
	skip(parser);
      }
      
      event = parser.next();
    }
  
    if (curr.getImplementation() == null) 
      throw new IllegalXMLException("Component \"" + curr.getComponentName() + 
				    "\" lacks implementation-tag");
    
    
    return curr;
  }

  /* discard all unrecognized tags (and their children) */
  private static void skip(XmlPullParser parser) throws XmlPullParserException,
							IOException,
							IllegalXMLException {
    int level = 0;
    int event = parser.getEventType();

    while (true) {
      
      if (event == XmlPullParser.START_TAG) { 
	level++;
	
      } else if (event == XmlPullParser.END_TAG) {
	level--;
	
	if (level == 0)
	  break;
      } 
      
      event = parser.next();
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
  private static void setPropertyInfo (ComponentDeclaration compConf, 
				XmlPullParser parser) throws IllegalXMLException,
							     XmlPullParserException,
							     IOException {
    /* Required attributes in the component tag*/
    boolean nameFound = false;

    /*Declare an instanse to store the values in */
    ComponentPropertyInfo compProp = compConf.getNewPropertyIntance();

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("name")) {
        if (parser.getAttributeValue(i) == null) {
          throw new IllegalXMLException("No value in mandatory attribute:"
                                        + parser.getAttributeName(i)
                                        + " in property tag");
        } else {
          compProp.setName(parser.getAttributeValue(i));
          nameFound = true;
        }
      } else if (parser.getAttributeName(i).equals("value")) {
        if (parser.getAttributeValue(i) != null) {
          compProp.addValue(parser.getAttributeValue(i));
        }
      } else if (parser.getAttributeName(i).equals("type")) {
        if (parser.getAttributeValue(i) == null) {
          compProp.setType("String"); // default value
        } else {
          for(int j = 0; j < supportedTypes.length ; j++){
            /* If the found attribute value equals one of the supported types*/
            if (supportedTypes[j].equalsIgnoreCase(parser.getAttributeValue(i))) {
              compProp.setType(supportedTypes[j]);
              break;
            }
          }
        }
      } else {
        throw new IllegalXMLException("Unsupported Attribute name:"
                                      + parser.getAttributeName(i));
      }
    }

    /*
     * need to do this because: 1: If no value was found in the attribute, they
     * should be here 2: If a value was found we needs to traverse through anyway
     */
    /* if no values was found read them between start and endtag */
    if (compProp.getValue() == null) {
      String text = parser.nextText();
      String[] values = text.split("\n");
      for (int i = 0; i < values.length; i++) {
	compProp.addValue(values[i]);
      }
    } else {
      parser.nextText();
    }


    /* Setting default values if no other has been sett*/
    if(compProp.getType() == null){
      compProp.setType(supportedTypes[0]);
    }

    /* check if required attributes has been set */
    if (!nameFound) {
      throw new IllegalXMLException("A required attribute in the tag:property was not pressent");
    }

    /* add the ComponentPropertyInfo to the ComponentConfiguration */
    compConf.addPropertyInfo(compProp);
  }

  /**
   * Parses out the following values from the reference tag -servicefactory =
   * bundle entry name (optional) -interface = Java interface type
   *
   * @param parser
   * @param compConf
   */
  private static void setServiceInfo(ComponentDeclaration compConf,
				     XmlPullParser parser) throws IllegalXMLException,
								  XmlPullParserException,
								  IOException {
    /* Required attributes in the component tag*/
    boolean interfaceFound = false;
    boolean servicefactoryFound = false;

    ComponentServiceInfo compServ = compConf.getNewServiceIntance();

    /* If there is an attribute in the service tag */
    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("servicefactory")) {
	if(compConf.getFactory() == null){
	  if (parser.getAttributeValue(i).equals("true")) {
	    compConf.setServiceFactory(true);
	    servicefactoryFound = true;
	  } else if (parser.getAttributeValue(i).equals("false")) {
	    compConf.setServiceFactory(false);
	    servicefactoryFound = true;
	  } else {
	    throw new IllegalXMLException("Unsupported value, attribute servicefactory:"
					  + parser.getAttributeValue(i));
	  }
	} else {
	  throw new IllegalXMLException("Unsupported Attribute name:"
					+ parser.getAttributeName(i) +
					" The component is a ComponentFactory");
	}
      } else {
	throw new IllegalXMLException("Unsupported Attribute name:"
				      + parser.getAttributeName(i));
      }
    }

    if (!servicefactoryFound) {
      compConf.setServiceFactory(false); // default value
    }

    /* Get the interfaces */
    // parser.require(XmlPullParser.START_TAG, "", "service");
    
    int event = parser.next();

    while (event != XmlPullParser.END_TAG) {

      if (event != XmlPullParser.START_TAG) {
	event = parser.next();
	continue;
      }
      
      if ("provide".equals(parser.getName())) {
	for (int i = 0; i < parser.getAttributeCount(); i++) {
	  if (parser.getAttributeName(i).equals("interface")) {
	    compServ.instertInterface(parser.getAttributeValue(i));
	    interfaceFound = true;
	  } else {
	    throw new IllegalXMLException("Unsupported attribute name:"
					  + parser.getAttributeName(i));
	  }
	}

	skip(parser);
	
      } else {
	skip(parser);
      }
      
      event = parser.next();
    }

    /* check if required attributes has been set */
    if (!interfaceFound) {
      throw new IllegalXMLException("Could not find a correct provide-tag in service-tag");

    }

    /* add the ComponentPropertyInfo to the ComponentConfiguration */
    compConf.addServiceInfo(compServ);
  }

  /**
   *
   * @param parser
   *
   * Parses out the following values from the reference tag -entry = bundle
   * entry name (mandatory)
   */
  private static void setComponentInfo(ComponentDeclaration compConf,
				       XmlPullParser parser) throws IllegalXMLException,
								    XmlPullParserException,
								    IOException {
    /* Required attributes in the component tag*/
    boolean nameFound = false;

    boolean autoEnableFound = false;

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("name")) {
	if (parser.getAttributeValue(i) == null) {
	  throw new IllegalXMLException("No value in mandatory attribute:"
					+ parser.getAttributeName(i)
					+ " in component tag");
	}
	compConf.setComponentName(parser.getAttributeValue(i));
	nameFound = true;
      } else if (parser.getAttributeName(i).equals("enabled")) {
	if (parser.getAttributeValue(i) != null) {
	  /* optional attribute */
	  compConf.setAutoEnable(parser.getAttributeValue(i).equals("true"));
	  autoEnableFound = true;
	}
      } else if (parser.getAttributeName(i).equals("factory")) {
	if (parser.getAttributeValue(i) != null) {
	  /*optional attribute */
	  compConf.setFactory(parser.getAttributeValue(i));
	}
      } else if (parser.getAttributeName(i).equals("immediate")) {
	if (parser.getAttributeValue(i) != null) {
	  /*optional attribute */
	  compConf.setImmediate(parser.getAttributeValue(i).equals("true"));
	}
      } else {
	throw new IllegalXMLException("Unsupported Attribute name:"
				      + parser.getAttributeName(i));
      }
    }
    parser.next(); // can't use skip here since we are going to read the body.

    /* Setting default value if no other value was set*/
    if (!autoEnableFound) {
      compConf.setAutoEnable(true);
    }

    /* check if required attributes has been set */
    if (!nameFound) {
      throw new IllegalXMLException("Could not find \"name\" attribute in component-tag");
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
  private static void setPropertiesInfo(ComponentDeclaration compConf,
					XmlPullParser parser) throws IllegalXMLException,
								     XmlPullParserException,
								     IOException {
    /* Required attributes in the component tag*/
    boolean entryFound = false;

    ComponentPropertiesInfo compProps = compConf.getNewPropertiesIntance();

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("entry")) {
	if (parser.getAttributeValue(i) == null) {
	  throw new IllegalXMLException("No value in mandatory attribute:"
					+ parser.getAttributeName(i)
					+ " in properties tag");
	} else {
	  compProps.setEntry(parser.getAttributeValue(i));
	  entryFound = true;
	}
      } else {
	throw new IllegalXMLException("Unsupported Attribute name:"
				      + parser.getAttributeName(i));
      }
    }
    skip(parser);

    /* check if required attributes has been set */
    if (!entryFound) {
      throw new IllegalXMLException("A required attribute in the tag:properties was not pressent");
    }

    /* add the ComponentPropertyInfo to the ComponentConfiguration */
    compConf.addPropertiesInfo(compProps);
  }

  /**
   * Parses out the following values from the reference tag -class = Java
   * implementation class (mandatory)
   *
   * @param parser
   * @param compConf
   */
  private static void setImplementationInfo(ComponentDeclaration compConf,
					    XmlPullParser parser) throws IllegalXMLException,
									 XmlPullParserException,
									 IOException {
    /* Required attributes in the component tag*/
    boolean classFound = false;

    if (compConf.getImplementation() != null) {
      throw new IllegalXMLException("Only one implementation tag allowed");
    }

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("class")) {
	if (parser.getAttributeValue(i) == null) {
	  throw new IllegalXMLException("No value in mandatory attribute:"
					+ parser.getAttributeName(i)
					+ " in implementation tag");
	} else {
	  compConf.setImplementation(parser.getAttributeValue(i));
	  classFound = true;
	}
      } else {
	throw new IllegalXMLException("Unsupported Attribute name:"
				      + parser.getAttributeName(i));
      }
    }
    skip(parser);

    /* check if required attributes has been set */
    if (!classFound) {
      throw new IllegalXMLException("Could not find \"class\" attribute in implementation-tag");
    }
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
  private static void setReferenceInfo(ComponentDeclaration compConf,
				       XmlPullParser parser) throws IllegalXMLException,
								    XmlPullParserException,
								    IOException {
    /* Required attributes in the component tag*/
    boolean nameFound = false;
    boolean interfaceFound = false;
    boolean cardinalityFound = false;
    boolean policyFound = false;

    ComponentReferenceInfo compRef = compConf.getNewReferenceIntance();
    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("name")) {
	if (parser.getAttributeValue(i) == null) {
	  throw new IllegalXMLException("No value in mandatory attribute:"
					+ parser.getAttributeName(i)
					+ " in reference tag");
	} else {
	  if (checkNMToken(parser.getAttributeValue(i))) {
	    compRef.setReferenceName(parser.getAttributeValue(i));
	    nameFound = true;
	  } else {
	    throw new IllegalXMLException("Invalid value in mandatory attribute:"
					  + parser.getAttributeName(i)
					  + " in reference tag");
	  }
	}
      } else if (parser.getAttributeName(i).equals("interface")) {
	if (parser.getAttributeValue(i) == null) {
	  throw new IllegalXMLException("No value in mandatory attribute:"
					+ parser.getAttributeName(i)
					+ " in reference tag");
	} else {
	  if (checkToken(parser.getAttributeValue(i))) {
	    compRef.setInterfaceType(parser.getAttributeValue(i));
	    interfaceFound = true;
	  } else {
	    throw new IllegalXMLException("Invalid value in mandatory attribute:"
					  + parser.getAttributeName(i)
					  + " in reference tag");
	  }
	}
      } else if (parser.getAttributeName(i).equals("cardinality")) {
	if (parser.getAttributeValue(i) == null) {
	  /* set default value if non pressent */
	  compRef.setCardinality("1..1");
	} else {
	  for(int j = 0; j < supportedCardniality.length ; j++){
	    /* If the found attribute value equals one of the supported types*/
	    if (parser.getAttributeValue(i).equalsIgnoreCase(supportedCardniality[j])) {
	      compRef.setCardinality(supportedCardniality[j]);
	      cardinalityFound = true;
	    }
	  }
	  compRef.setCardinality(parser.getAttributeValue(i));
	}
      } else if (parser.getAttributeName(i).equals("policy")) {
	if (parser.getAttributeValue(i) == null) {
	  compRef.setPolicy("static"); // default value
	} else {
	  compRef.setPolicy(parser.getAttributeValue(i));
	  policyFound = true;
	}
      } else if (parser.getAttributeName(i).equals("target")) {
	if (parser.getAttributeValue(i) == null) {
	  compRef.setTarget("(objectClass=" + compRef.getInterfaceType() + ")"); // default value
	} else {
	  compRef.setTarget(parser.getAttributeValue(i));
	}
      } else if (parser.getAttributeName(i).equals("bind")) {
	if (parser.getAttributeValue(i) == null) {
	  compRef.setBind(null); // default value
	} else {
	  compRef.setBind(parser.getAttributeValue(i));
	}
      } else if (parser.getAttributeName(i).equals("unbind")) {
	if (parser.getAttributeValue(i) == null) {
	  compRef.setUnbind(null); // default value
	} else {
	  compRef.setUnbind(parser.getAttributeValue(i));
	}
      } else {
	throw new IllegalXMLException("Unsupported Attribute name:"
				      + parser.getAttributeName(i));
      }
    }
    
    skip(parser);

    /* Setting default values if no other has been sett*/
    if (compRef.getCardinality() == null) {
      compRef.setCardinality(supportedCardniality[2]);
    }
    if (compRef.getPolicy() == null) {
      compRef.setPolicy("static");
    }

    /* check if required attributes has been set */
    if (!nameFound)
      throw new IllegalXMLException("Could not find \"name\" attribute in implementation-tag");

    if (!interfaceFound) 
      throw new IllegalXMLException("Could not find \"interface\" attribute in implementation-tag");

    /* add the ComponentReferenceInfo to the ComponentConfiguration */
    compConf.addReferenceInfo(compRef);
  }

  //TODO Check if the string follows the NMTOKEN in XML SCHEMA
  private static boolean checkNMToken(String text){
    return checkToken(text);
  }

  /**
   * A Function that test if no Line terminators and whitespaces is used
   * in a string
   */
  private static boolean checkToken(String text){
    String[] result = text.split(" |\\n|\\t|\\r|'\u0085'|'\u2028'|'\u2029'");
    return (result.length <= 1);
  }



  /* Test function that prints the contents of a component */
  private static String printComponentConfiguration(ComponentDeclaration compConf) {
     StringBuffer buf = new StringBuffer();
     buf.append("THE RESULT FROM THE PARSING");
     buf.append(toStringComponent(compConf));
     buf.append(toStringImplementation(compConf));
     buf.append(toStringProperties(compConf));
     buf.append(toStringService(compConf));
     buf.append(toStringReference(compConf));
     return buf.toString();
  }

  /* Test function that prints a part of a component */
  private static String toStringComponent(ComponentDeclaration compConf) {
    /* <component> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <component> tag------------");
    buf.append("Name:" + compConf.getComponentName());
    buf.append("Autoenable:" + compConf.isAutoEnable());
    buf.append("Immediate:" + compConf.isImmediate());
    buf.append("Factory:" + compConf.getFactory());
    return buf.toString();
  }

  /* Test function that prints a part of a component */
  private static String toStringImplementation(ComponentDeclaration compConf) {
    /* <implementation> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <implementation> tag------------");
    buf.append("Class:" + compConf.getImplementation());
    return buf.toString();
  }

  /* Test function that prints a part of a component */
  private static String toStringProperties(ComponentDeclaration compConf) {
    /* <properties> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <properties/property> tag(s)------------\n");
    //buf.append(properties.toString() + "\n");

    return buf.toString();
  }

  /* Test function that prints a part of a component */
  private static String toStringService(ComponentDeclaration compConf) {
    /* <service> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <service> tag------------");
    ArrayList serviceInfo = compConf.getServiceInfo();
    buf.append("The number of services is:" + serviceInfo.size());
    for (int i = 0; i < serviceInfo.size(); i++) {
      ComponentServiceInfo compServ = (ComponentServiceInfo) serviceInfo.get(i);
      buf.append("ServiceFactory:" + compConf.isServiceFactory());
      ArrayList interfaces = compServ.getComponentInterfaces();
      buf.append("The number of interfaces in this service is:" + serviceInfo.size());
      for (int j = 0; j < interfaces.size(); j++) {
        buf.append("Interface:" + interfaces.get(i));
      }
    }
    return buf.toString();
  }

  /* Test function that prints a part of a component */
  private static String toStringReference(ComponentDeclaration compConf) {
    /* <reference> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <reference> tag------------");
    ArrayList referenceInfo = compConf.getReferenceInfo();
    buf.append("The number of references is:" + referenceInfo.size());
    for (int i = 0; i < referenceInfo.size(); i++) {
      ComponentReferenceInfo compRefs = (ComponentReferenceInfo) referenceInfo.get(i);
      buf.append("Name:" + compRefs.getReferenceName());
      buf.append("Interfaces:" + compRefs.getInterfaceType());
      buf.append("Cardniality:" + compRefs.getCardinality());
      buf.append("Policy:" + compRefs.getPolicy());
      buf.append("Target:" + compRefs.getTarget());
      buf.append("Bind:" + compRefs.getBind());
      buf.append("Unbind:" + compRefs.getUnbind());
    }
    return buf.toString();
  }
}
