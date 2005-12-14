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

import org.osgi.framework.BundleEvent;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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

  static String[] supportedTypes = {"String", "Long", "Double", "Float", "Integer", "Byte", "Char", "Boolean", "Short"};
  static String[] supportedCardniality = {"0..1", "0..n", "1..1", "1..n"};

  private ComponentDeclaration compConf;

  int serviceCount;

  public ComponentParser() {
    /* Store the values from the xml-file in this */
    compConf = new ComponentDeclaration();
    serviceCount = 0;
  }

  public ComponentDeclaration readXML(URL url) throws IllegalXMLException {

    boolean foundImplementation = false;

    try {
      if (url == null) {
        return compConf;
      }
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);

      XmlPullParser parser = factory.newPullParser();

      parser.setInput(url.openStream(), null);
      parser.nextTag();
      parser.require(XmlPullParser.START_TAG,
                     "http://www.osgi.org/xmlns/scr/v1.0.0",
                     "component");
      setComponentInfo(parser);
      while (parser.nextTag() != XmlPullParser.END_TAG) {
        if (parser.getName().equals("implementation")) {
          setImplementationInfo(parser);
          foundImplementation = true;
        } else if (parser.getName().equals("property")) {
          setPropertyInfo(parser);
        } else if (parser.getName().equals("properties")) {
          setPropertiesInfo(parser);
        } else if (parser.getName().equals("service")) {
          setServiceInfo(parser);
        } else if (parser.getName().equals("reference")) {
          setReferenceInfo(parser);
        } else {
          ComponentActivator.debug("Unsupported tag");
        }
      }
      parser.require(XmlPullParser.END_TAG,
                     "http://www.osgi.org/xmlns/scr/v1.0.0",
                     "component");
      printComponentConfiguration();
    } catch (IOException e) {
      throw new IllegalXMLException("Error getting xml file" + e,e.getCause());
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalXMLException("Error reading zipentry" + e,e.getCause());
    }

    // Check that required tags has been found
    if (!foundImplementation) {
      throw new IllegalXMLException("No Implementations tag found:");
    }
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
  private void setPropertyInfo (XmlPullParser parser) throws IllegalXMLException {
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
    try {
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
    } catch (XmlPullParserException e) {
      ComponentActivator.error("Error Parsing property tag", e);
    } catch (IOException e){
      ComponentActivator.error("Error Parsing property tag", e);
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
  private void setServiceInfo(XmlPullParser parser)
      throws IllegalXMLException {
    /* Required attributes in the component tag*/
    boolean interfaceFound = false;
    boolean servicefactoryFound = false;

    /* count the number of times the servicetag is found */
    serviceCount++;

    /* There may only be one occurance of the service tag*/
    if(serviceCount > 1){
      throw new IllegalXMLException(
          "To many service tags found in the xml document:");
    }

    ComponentServiceInfo compServ = compConf.getNewServiceIntance();

    /* If there is an attribute in the service tag */
    if (parser.getAttributeCount() > 0) {
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
    }

    /* Get the interfaces */
    try {
      parser.require(XmlPullParser.START_TAG, "", "service");
      while (parser.nextTag() != XmlPullParser.END_TAG) {
        parser.require(XmlPullParser.START_TAG, null, null);
        String name = parser.getName();
        if (name.equals("provide")) {
          parser.require(XmlPullParser.START_TAG, null, "provide");
          for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (parser.getAttributeName(i).equals("interface")) {
              compServ.instertInterface(parser.getAttributeValue(i));
              interfaceFound = true;
            } else {
              throw new IllegalXMLException("Unsupported Attribute name:"
                                            + parser.getAttributeName(i));
            }
          }
          parser.next();
        }
        parser.require(XmlPullParser.END_TAG, null, name);
      }
      parser.require(XmlPullParser.END_TAG, null, "service");
    } catch (XmlPullParserException e) {
      ComponentActivator.error("Error Parsing service tag", e);
    } catch (IOException e){
      ComponentActivator.error("Error Parsing service tag", e);
    }

    /* check if required attributes has been set */
    if (!interfaceFound) {
      throw new IllegalXMLException("A required attribute in the tag:provide was not pressent");
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
  private void setComponentInfo(XmlPullParser parser) throws IllegalXMLException {
    /* Required attributes in the component tag*/
    boolean nameFound = false;

    boolean autoEnableFound = false;

    try {
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
      parser.next();
    } catch (XmlPullParserException e) {
      ComponentActivator.error("Error Parsing component tag", e);
    } catch (IOException e){
      ComponentActivator.error("Error Parsing component tag", e);
    }

    /* Setting default value if no other value was set*/
    if (!autoEnableFound) {
      compConf.setAutoEnable(true);
    }

    /* check if required attributes has been set */
    if (!nameFound) {
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
  private void setPropertiesInfo(XmlPullParser parser) throws IllegalXMLException {
    /* Required attributes in the component tag*/
    boolean entryFound = false;

    ComponentPropertiesInfo compProps = compConf.getNewPropertiesIntance();
    try {
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
      parser.next();
    } catch (XmlPullParserException e) {
      ComponentActivator.error("Error Parsing properties tag", e);
    } catch (IOException e){
      ComponentActivator.error("Error Parsing properties tag", e);
    }

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
  private void setImplementationInfo(XmlPullParser parser) throws IllegalXMLException {
    /* Required attributes in the component tag*/
    boolean classFound = false;

    if (compConf.getImplementation() != null) {
      throw new IllegalXMLException("Only one implementation tag allowed");
    }
    try {
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
      parser.next();
    } catch (XmlPullParserException e) {
      ComponentActivator.error("Error Parsing implementation tag", e);
    } catch (IOException e){
      ComponentActivator.error("Error Parsing implementation tag", e);
    }

    /* check if required attributes has been set */
    if (!classFound) {
      throw new IllegalXMLException("A required attribute in the tag:implementation was not pressent");
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
  private void setReferenceInfo(XmlPullParser parser) throws IllegalXMLException {
    /* Required attributes in the component tag*/
    boolean nameFound = false;
    boolean interfaceFound = false;
    boolean cardinalityFound = false;
    boolean policyFound = false;

    ComponentReferenceInfo compRef = compConf.getNewReferenceIntance();
    try {
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
      parser.next();
    } catch (XmlPullParserException e) {
      ComponentActivator.error("Error Parsing reference tag", e);
    } catch (IOException e){
      ComponentActivator.error("Error Parsing reference tag", e);
    }

    /* Setting default values if no other has been sett*/
    if (compRef.getCardinality() == null) {
      compRef.setCardinality(supportedCardniality[2]);
    }
    if (compRef.getPolicy() == null) {
      compRef.setPolicy("static");
    }

    /* check if required attributes has been set */
    if (!nameFound && !interfaceFound) {
      throw new IllegalXMLException("A required attribute in the tag:reference was not pressent");
    }

    /* add the ComponentReferenceInfo to the ComponentConfiguration */
    compConf.addReferenceInfo(compRef);
  }

  //TODO Check if the string follows the NMTOKEN in XML SCHEMA
  private boolean checkNMToken(String text){
    return checkToken(text);
  }

  /**
   * A Function that test if no Line terminators and whitespaces is used
   * in a string
   */
  private boolean checkToken(String text){
    String[] result = text.split(" |\\n|\\t|\\r|'\u0085'|'\u2028'|'\u2029'");
    return (result.length <= 1);
  }



  /* Test function that prints the contents of a component */
  private void printComponentConfiguration() {
    StringBuffer buf = new StringBuffer();
    buf.append("THE RESULT FROM THE PARSING");
    buf.append(toStringComponent());
    buf.append(toStringImplementation());
    buf.append(toStringProperty());
    buf.append(toStringProperties());
    buf.append(toStringService());
    buf.append(toStringReference());
    ComponentActivator.debug(buf.toString());
  }

  /* Test function that prints a part of a component */
  private String toStringComponent() {
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
  private String toStringImplementation() {
    /* <implementation> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <implementation> tag------------");
    buf.append("Class:" + compConf.getImplementation());
    return buf.toString();
  }

  /* Test function that prints a part of a component */
  private String toStringProperty() {
    /* <property> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <property> tag------------");
    ArrayList propertyInfo = compConf.getPropertyInfo();
    buf.append("The number of property is:" + propertyInfo.size());
    for (int i = 0; i < propertyInfo.size(); i++) {
      ComponentPropertyInfo compProp = (ComponentPropertyInfo) propertyInfo.get(i);
      buf.append("Name:" + compProp.getName());
      ArrayList values = compProp.getValueList();
      for (int j = 0; j < values.size(); j++) {
        buf.append("Value:" + values.get(j));
      }
      buf.append("Type:" + compProp.getType());
    }
    return buf.toString();
  }

  /* Test function that prints a part of a component */
  private String toStringProperties() {
    /* <properties> */
    StringBuffer buf = new StringBuffer();
    buf.append("------------Everything in the <properties> tag------------");
    ArrayList propertiesInfo = compConf.getPropertiesInfo();
    buf.append("The number of properties is:" + propertiesInfo.size());
    for (int i = 0; i < propertiesInfo.size(); i++) {
      ComponentPropertiesInfo compProps = (ComponentPropertiesInfo) propertiesInfo.get(i);
      buf.append("Entry:" + compProps.getEntry());
    }
    return buf.toString();
  }

  /* Test function that prints a part of a component */
  private String toStringService() {
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
  private String toStringReference() {
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