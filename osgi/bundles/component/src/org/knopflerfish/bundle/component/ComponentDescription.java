/*
 * Copyright (c) 2010-2017, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;


/* Immutable Java representation of component description found in xml */
public class ComponentDescription {

  final static int POLICY_OPTIONAL = 0;
  final static int POLICY_REQUIRE = 1;
  final static int POLICY_IGNORE = 2;
  private final static String[] supportedPolicies = { "optional", "require", "ignore" };
  private final static String[] supportedTypes =
    { "Boolean", "Byte", "Character", "Double", "Float",
       "Integer", "Long", "Short", "String" };

  private final static String SCR_NAMESPACE_V1_0_0_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";
  private final static String SCR_NAMESPACE_V1_1_0_URI = "http://www.osgi.org/xmlns/scr/v1.1.0";
  private final static String SCR_NAMESPACE_V1_2_0_URI = "http://www.osgi.org/xmlns/scr/v1.2.0";
  private final static String SCR_NAMESPACE_V1_3_0_URI = "http://www.osgi.org/xmlns/scr/v1.3.0";

  final Bundle bundle;
  private String implementation;
  private String componentName;
  private String factory = null;
  private boolean enabled = true;
  private boolean immediate = true;
  private boolean immediateSet = false;
  private boolean isServiceFactory = false;
  private String activateMethod = "activate";
  private boolean activateMethodSet = false;
  private String deactivateMethod = "deactivate";
  private boolean deactivateMethodSet = false;
  private String modifiedMethod = null;
  private int confPolicy = POLICY_OPTIONAL;
  private String [] configurationPid = null;
  private final Properties properties = new Properties();
  private String [] services = null;
  private ArrayList<ReferenceDescription> references = null;
  private String scope = Constants.SCOPE_SINGLETON;
  private int scrNSminor = 0;


  /**
   *
   */
  public static ComponentDescription parseComponent(Bundle b, XmlPullParser p)
    throws IOException, XmlPullParserException
  {
    while (findNextStartTag(p, true)) {
      Activator.logDebug("Check for component description: " +
                         p.getPositionDescription());
      if (p.getEventType() == XmlPullParser.START_TAG &&
          "component".equals(p.getName()) &&
          (p.getDepth() == 1 ||
           SCR_NAMESPACE_V1_3_0_URI.equals(p.getNamespace()) ||
           SCR_NAMESPACE_V1_2_0_URI.equals(p.getNamespace()) ||
           SCR_NAMESPACE_V1_1_0_URI.equals(p.getNamespace()) ||
           SCR_NAMESPACE_V1_0_0_URI.equals(p.getNamespace()))) {
        return new ComponentDescription(b, p);
      }
      p.next();
    }
    return null;
  }


  /**
   *
   */
  private ComponentDescription(Bundle b, XmlPullParser p)
    throws IOException, XmlPullParserException
  {
    this.bundle = b;
    if (SCR_NAMESPACE_V1_3_0_URI.equals(p.getNamespace())) {
      scrNSminor = 3;
    } else if (SCR_NAMESPACE_V1_2_0_URI.equals(p.getNamespace())) {
      scrNSminor = 2;
    } else if (SCR_NAMESPACE_V1_1_0_URI.equals(p.getNamespace())) {
      scrNSminor = 1;
    }
    parseAttributes(p);

    for (int e = p.getEventType(); e != XmlPullParser.END_TAG; e = p.getEventType()) {
      if (!findNextStartTag(p, false)) {
        if (p.getEventType() == XmlPullParser.END_DOCUMENT) {
          throw new IllegalXMLException("Component \"" + componentName +
                                        "\" lacks end-tag", p);
        }
        break;
      }
      String tag = p.getName();
      Activator.logDebug("Found component sub-element: " + tag);
      if ("implementation".equals(tag)) {
        parseImplementation(p);
      } else if ("property".equals(tag)) {
        parseProperty(p);
      } else if ("properties".equals(tag)) {
        parseProperties(p);
      } else if ("service".equals(tag)) {
        parseService(p);
      } else if ("reference".equals(tag)) {
        parseReference(p);
      } else {
        Activator.logDebug("Skip unknown component sub-element: " + tag);
        skip(p);
      }
    }
    if (implementation == null) {
      throw new IllegalXMLException("Component \"" + componentName +
                                    "\" lacks implementation-tag", p);
    }
    if (services == null && immediateSet && !immediate) {
      throw new IllegalXMLException("Attribute immediate in component-tag must "+
                                    "be set to \"true\" when component-factory is "+
                                    "not set and we do not have a service element", p);
    }
  }


  String getActivateMethod() {
    return activateMethod;
  }

  boolean isActivateMethodSet() {
    return activateMethodSet;
  }

  int getConfigPolicy() {
    return confPolicy;
  }

  String getConfigPolicyString() {
    return supportedPolicies[confPolicy];
  }

  String getDeactivateMethod() {
    return deactivateMethod;
  }

  boolean isDeactivateMethodSet() {
    return deactivateMethodSet;
  }

  String [] getConfigurationPid() {
    return configurationPid;
  }

  boolean isEnabled() {
    return enabled;
  }

  String getFactory() {
    return factory;
  }

  boolean isImmediate() {
    return immediate;
  }

  String getImplementation() {
    return implementation;
  }

  String getModifiedMethod() {
    return modifiedMethod;
  }

  String getName() {
    return componentName;
  }

  Properties getProperties() {
    return properties;
  }

  ArrayList<ReferenceDescription> getReferences() {
    return references;
  }

  int getScrNSminor() {
    return scrNSminor;
  }

  boolean isServiceFactory() {
    return isServiceFactory;
  }

  String [] getServices() {
    return services;
  }

  String getScope() {
    return scope;
  }

  //
  // Private methods
  //

  private static boolean findNextStartTag(XmlPullParser p, boolean skipEnd)
    throws IOException, XmlPullParserException
  {
    int e = p.getEventType();
    while (e != XmlPullParser.END_DOCUMENT &&
           (skipEnd || e != XmlPullParser.END_TAG) &&
           e != XmlPullParser.START_TAG) {
      e = p.next();
    }
    return e == XmlPullParser.START_TAG;
  }


  /*
    Parses attributes for
      <component name="<name>" [enabled="<boolean>"] [factory="val"] [immediate="<boolean>"]>
      ....
      </component>
   */
  private void parseAttributes(XmlPullParser p)
    throws IOException, XmlPullParserException
  {
    try {
      for (int i = 0; i < p.getAttributeCount(); i++) {
        if (p.getAttributeName(i).equals("name")) {
          componentName = p.getAttributeValue(i);
        } else if (p.getAttributeName(i).equals("enabled")) {
          enabled = parseBoolean(p, i);
        } else if (p.getAttributeName(i).equals("factory")) {
          factory = p.getAttributeValue(i);
        } else if (p.getAttributeName(i).equals("immediate")) {
          immediate = parseBoolean(p, i);
          immediateSet = true;
        } else if (scrNSminor > 0) {
          if (p.getAttributeName(i).equals("configuration-policy")) {
            String policy = p.getAttributeValue(i);
            if (supportedPolicies[POLICY_OPTIONAL].equals(policy)) {
              confPolicy = POLICY_OPTIONAL;
            } else if (supportedPolicies[POLICY_REQUIRE].equals(policy)) {
              confPolicy = POLICY_REQUIRE;
            } else if (supportedPolicies[POLICY_IGNORE].equals(policy)) {
              confPolicy = POLICY_IGNORE;
            } else {
              invalidValue(p, supportedPolicies, i);
            }
          } else if (p.getAttributeName(i).equals("activate")) {
            activateMethod = p.getAttributeValue(i);
            activateMethodSet = true;
          } else if (p.getAttributeName(i).equals("deactivate")) {
            deactivateMethod = p.getAttributeValue(i);
            deactivateMethodSet = true;
          } else if (p.getAttributeName(i).equals("modified")) {
            modifiedMethod = p.getAttributeValue(i);
          } else if (scrNSminor > 1 && p.getAttributeName(i).equals("configuration-pid")) {
            configurationPid = p.getAttributeValue(i).trim().split("\\s+");
            if (configurationPid[0].length() == 0) {
              throw new IllegalXMLException("Attribute \"configuration-pid\" can not "
                                            + "contain an empty string", p);
            }
          } else {
            unrecognizedAttr(p, i);
          }
        } else {
          unrecognizedAttr(p, i);
        }
      }
    } finally {
      p.next();
    }
    if (componentName == null && scrNSminor < 1) {
      missingAttr(p, "name");
    }
    if (factory != null) {
      if (immediateSet) {
        if (immediate) {
          throw new IllegalXMLException("Immediate can not be set when we are a factory", p);
        }
      } else {
        immediate = false;
      }
    }
  }


  /*
    Parsers a <implementation class="<classname>"/>
  */
  private void parseImplementation(XmlPullParser p)
    throws IOException, XmlPullParserException
  {
    if (implementation != null) {
      throw new IllegalXMLException("Only one implementation tag allowed", p);
    }
    implementation = getSingleAttribute(p, "class");
    // If component name is not set use implementation as default
    if (componentName == null) {
      componentName = implementation;
    }
    skip(p);
  }


  /*
    Parses a <properties entry="<url>"/>
    and then reads a property specified by <url>
   */
  private void parseProperties(XmlPullParser p)
    throws IOException, XmlPullParserException
  {
    String entry = getSingleAttribute(p, "entry");
    // read a property-file and adds it contents to conf's properties.
    URL u  = bundle.getEntry(entry);
    if (u == null) {
      throw new IOException("Did not find requested entry " + entry);
    }
    properties.load(u.openStream());
    skip(p);
  }


  /* Parses a
     <property name="<name>" value="<value>" [type="<type>"]/>
     or
     <property name="<name>" [type="<type>"]>
      <val1>
      <val2>
      ...
      <valn>
     </property>

     The latter will produce an array
  */
  private void parseProperty(XmlPullParser p)
    throws IOException, XmlPullParserException
  {

    String type = null;
    String name = null;
    Object val = null;
    boolean isArray = true; // If the property values is an array or not
    ArrayList<String> values = new ArrayList<String>();

    for (int i = 0; i < p.getAttributeCount(); i++) {
      if (p.getAttributeName(i).equals("name")) {
        name = p.getAttributeValue(i);
      } else if (p.getAttributeName(i).equals("value")) {
        values.add(p.getAttributeValue(i));
        isArray = false;
      } else if (p.getAttributeName(i).equals("type")) {
        for(int j = 0; j < supportedTypes.length ; j++){
          if (supportedTypes[j].equals(p.getAttributeValue(i))) {
            type = supportedTypes[j];
            break;
          }
        }
        if (type == null) {
          invalidValue(p, supportedTypes, i);
        }
      } else {
        unrecognizedAttr(p, i);
      }
    }
    /* check if required attributes has been set */
    if (name == null) {
      missingAttr(p, "name");
    }

    if (type == null) {
      type = "String";
    }

    String text = p.nextText().trim();
    if (values.isEmpty()) {
      int firstChar = -1;
      int lastChar = -1;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (firstChar < 0) {
          if (!Character.isWhitespace(c)) {
            firstChar = i;
            lastChar = i;
          }
        } else if (c == '\n' || c == '\r') {
          values.add(text.substring(firstChar, lastChar + 1));
          firstChar = -1;
        } else if (!Character.isWhitespace(c)) {
          lastChar = i;
        }
      }
      if (firstChar >= 0) {
        values.add(text.substring(firstChar, lastChar + 1));
      }
    }

    int len = values.size();
    if ("String".equals(type)) {
      val = isArray ?  values.toArray(new String[len]) : values.get(0);
    } else if ("Boolean".equals(type)) {
      if (!isArray) {
        val = Boolean.valueOf(values.get(0));
      } else {
        boolean[] array = new boolean[len];
        for (int i=0; i<len; i++) {
          array[i] = Boolean.valueOf(values.get(i)).booleanValue();
        }
        val = array;
      }
    } else if ("Byte".equals(type)) {
      byte[] array = new byte[len];
      for (int i=0; i<len; i++) {
        array[i] = Byte.parseByte(values.get(i));
      }
      val = isArray ? (Object) array : (Object) new Byte(array[0]);
    } else if ("Character".equals(type)) {
      char[] array = new char[len];
      for (int i=0; i<len; i++) {
        array[i] = (char) Integer.parseInt(values.get(i));
      }
      val = isArray ? (Object) array : (Object) new Character(array[0]);
    } else if ("Double".equals(type)) {
      double[] array = new double[len];
      for (int i=0; i<len; i++) {
        array[i] = Double.parseDouble(values.get(i));
      }
      val = isArray ? (Object) array : (Object) new Double(array[0]);
    } else if ("Float".equals(type)) {
      float[] array = new float[len];
      for (int i=0; i<len; i++) {
        array[i] = Float.parseFloat(values.get(i));
      }
      val = isArray ? (Object) array : (Object) new Float(array[0]);
    } else if ("Integer".equals(type)) {
      int[] array = new int[len];
      for (int i=0; i<len; i++) {
        array[i] = Integer.parseInt(values.get(i));
      }
      val = isArray ? (Object) array : (Object) new Integer(array[0]);
    } else if ("Long".equals(type)) {
      long[] array = new long[len];
      for (int i=0; i<len; i++) {
        array[i] = Long.parseLong(values.get(i));
      }
      val = isArray ? (Object) array : (Object) new Long(array[0]);
    } else if ("Short".equals(type)) {
      short[] array = new short[len];
      for (int i=0; i<len; i++) {
        array[i] = Short.parseShort(values.get(i));
      }
      val = isArray ? (Object) array : (Object) new Short(array[0]);
    } else {
      throw new IllegalXMLException("Did not recognize \"" + type +
                                    "\" in property-tag", p);
    }
    p.next();
    properties.put(name, val);
  }


  /*
    Parses a
    <reference name="<name>" interface="<interface>"
        [bind="<bind-method>"] [unbind="<bind-method>"]
        [cardinality="<cardinality>"] [policy="<policy>"]/>


   */
  private void parseReference(XmlPullParser p)
    throws IOException, XmlPullParserException
  {
    String name = null;
    String interfaceName = null;
    String target = null;
    String bind = null;
    String unbind = null;
    String updated = null;
    String scope = Constants.SCOPE_BUNDLE; // default value
    String field = null;
    int fieldCollectionType = ReferenceDescription.FIELD_ELEM_SERVICE; // default value
    boolean optional = false; // default value
    boolean multiple = false; // default value
    boolean dynamic = false; // default value
    boolean greedy = false; // default value
    Boolean fieldUpdate = null; // No value

    for (int i = 0; i < p.getAttributeCount(); i++) {
      final String attrName = p.getAttributeName(i);
      if (attrName.equals("name")) {
        name = p.getAttributeValue(i);
        checkValidIdentifier(name, "name", p);
      } else if (attrName.equals("interface")) {
        interfaceName = p.getAttributeValue(i);
        checkValidIdentifier(interfaceName, "interface", p);
      } else if (attrName.equals("cardinality")) {
        String val = p.getAttributeValue(i);
        if ("1..1".equals(val)) {
          optional = false;
          multiple = false;
        } else if ("0..1".equals(val)) {
          optional = true;
          multiple = false;
        } else if ("1..n".equals(val)) {
          optional = false;
          multiple = true;
        } else if ("0..n".equals(val)) {
          multiple = true;
          optional = true;
        } else {
          invalidValue(p, new String[]{ "1..1", "0..1", "1..n", "0..n" }, i);
        }
      } else if (attrName.equals("policy")) {
        String val = p.getAttributeValue(i);
        if ("static".equals(val)) {
          dynamic = false;
        } else if ("dynamic".equals(val)) {
          dynamic = true;
        } else {
          invalidValue(p, new String[]{"static", "dynamic"}, i);
        }
      } else if (scrNSminor > 1 && attrName.equals("policy-option")) {
        String val = p.getAttributeValue(i);
        if ("greedy".equals(val)) {
          greedy = true;
        } else if ("reluctant".equals(val)) {
          greedy = false;
        } else {
          invalidValue(p, new String[]{"greedy", "reluctant"}, i);
        }
      } else if (attrName.equals("target")) {
        target = p.getAttributeValue(i);
      } else if (attrName.equals("bind")) {
        bind = p.getAttributeValue(i);
        checkValidIdentifier(bind, "bind", p);
      } else if (attrName.equals("unbind")) {
        unbind = p.getAttributeValue(i);
        checkValidIdentifier(unbind, "unbind", p);
      } else if (scrNSminor > 1 && attrName.equals("updated")) {
        updated = p.getAttributeValue(i);
        checkValidIdentifier(updated, "updated", p);
      } else if (scrNSminor > 2 && p.getAttributeName(i).equals("scope")) {
        scope = p.getAttributeValue(i);
        if (!Constants.SCOPE_BUNDLE.equals(scope) && !Constants.SCOPE_PROTOTYPE.equals(scope)
            && !"prototype_required".equals(scope)) {
          invalidValue(p, new String[]{Constants.SCOPE_BUNDLE, Constants.SCOPE_PROTOTYPE, ReferenceDescription.SCOPE_PROTOTYPE_REQUIRED}, i);
        }
      } else if (scrNSminor > 2 && p.getAttributeName(i).equals("field")) {
        field = p.getAttributeValue(i);
        checkValidIdentifier(field, "field", p);
      } else if (scrNSminor > 2 && p.getAttributeName(i).equals("field-option")) {
        String val = p.getAttributeValue(i);
        if ("replace".equals(val)) {
          fieldUpdate = Boolean.FALSE;
        } else if ("update".equals(val)) {
          fieldUpdate = Boolean.TRUE;
        } else {
          invalidValue(p, new String[]{"replace", "update"}, i);
        }
      } else if (scrNSminor > 2 && p.getAttributeName(i).equals("field-collection-type")) {
        String val = p.getAttributeValue(i);
        if ("service".equals(val)) {
          fieldCollectionType = ReferenceDescription.FIELD_ELEM_SERVICE;
        } else if ("reference".equals(val)) {
          fieldCollectionType = ReferenceDescription.FIELD_ELEM_REFERENCE;
        } else if ("serviceobjects".equals(val)) {
          fieldCollectionType = ReferenceDescription.FIELD_ELEM_SERVICE_OBJECTS;
        } else if ("properties".equals(val)) {
          fieldCollectionType = ReferenceDescription.FIELD_ELEM_PROPERTIES;
        } else if ("tuple".equals(val)) {
          fieldCollectionType = ReferenceDescription.FIELD_ELEM_TUPLE;
        } else {
          invalidValue(p, new String[]{"service", "reference", "serviceobjects", "properties", "tuple"}, i);
        }
      } else {
        unrecognizedAttr(p, i);
      }
    }
    if (interfaceName == null) {
      missingAttr(p, "interface");
    }
    if (name == null) {
      if (scrNSminor > 0) {
        name = interfaceName;
      } else {
        missingAttr(p, "name");
      }
    }
    skip(p);
    ReferenceDescription ref;
    try {
      ref = new ReferenceDescription(name, interfaceName, optional,
                                     multiple, dynamic, greedy, target,
                                     bind, unbind, updated, scope,
                                     field, fieldUpdate, fieldCollectionType);
    } catch (InvalidSyntaxException e) {
      throw new IllegalXMLException("Couldn't create filter for reference \"" +
                                    name + "\"", p, e);
    }
    if (references == null) {
      references = new ArrayList<ReferenceDescription>();
    }
    references.add(ref);
  }


  /*
    Parses a <service [servicefactory="<boolean>"]>
              <provide interface="<interface1>">
              <provide interface="<interface2>">
              ...
             </service>
   */
  private void parseService(XmlPullParser p)
      throws IOException, XmlPullParserException
  {
    if (services != null) {
      throw new IllegalXMLException(
                                    "More than one service-tag in component: \""
                                        + componentName + "\"", p);
    }
    ArrayList<String> sl = new ArrayList<String>();
    if (!immediateSet) {
      immediate = false;
    }
    /* If there is an attribute in the service tag */
    for (int i = 0; i < p.getAttributeCount(); i++) {
      if (scrNSminor < 3 && p.getAttributeName(i).equals("servicefactory")) {
        isServiceFactory = parseBoolean(p, i);
        if (isServiceFactory) {
          if (factory != null) {
            throw new IllegalXMLException("Attribute servicefactory in service-tag "
                                          + "cannot be set to \"true\" when component "
                                          + "is a factory component", p);
          }
          if (immediate) {
            throw new IllegalXMLException("Attribute servicefactory in service-tag "
                                          + "cannot be set to \"true\" when component "
                                          + "is an immediate component", p);
          }
          scope = Constants.SCOPE_BUNDLE;
        }
      } else if (scrNSminor > 2 && p.getAttributeName(i).equals("scope")) {
        scope = p.getAttributeValue(i);
        if (!Constants.SCOPE_SINGLETON.equals(scope)) {
          isServiceFactory = true;
          if (!Constants.SCOPE_BUNDLE.equals(scope) && !Constants.SCOPE_PROTOTYPE.equals(scope)) {
            throw new IllegalXMLException("Attribute scope in service-tag must be set to "
                                          + "\"bundle\", \"prototype\" or \"singleton\"", p);
          }
          if (factory != null) {
            throw new IllegalXMLException("Attribute scope in service-tag must be "
                                          + "set to \"singleton\" when component "
                                          + "is a factory component", p);
          }
          if (immediate) {
            throw new IllegalXMLException(
                                          "Attribute scope in service-tag must be "
                                          + "set to \"singleton\" when component "
                                          + "is an immediate component", p);
          }
        }
      } else {
        unrecognizedAttr(p, i);
      }
    }
    int event = p.next();
    while (event != XmlPullParser.END_TAG) {
      if (event != XmlPullParser.START_TAG) {
        event = p.next();
        continue;
      }
      if ("provide".equals(p.getName())) {
        String interfaceName = null;
        for (int i = 0; i < p.getAttributeCount(); i++) {
          if (p.getAttributeName(i).equals("interface")) {
            interfaceName = p.getAttributeValue(i);
          } else {
            throw new IllegalXMLException("Unrecognized attribute \""
                                          + p.getAttributeName(i)
                                          + "\" in provide-tag", p);
          }
        }
        if (interfaceName == null) {
          missingAttr(p, "interface");
        }
        sl.add(interfaceName);
      }
      skip(p);
      event = p.getEventType();
    }
    p.next();
    /* check if required attributes has been set */
    if (sl.isEmpty()) {
      throw new IllegalXMLException("Service-tag did not contain a proper provide-tag",
                                    p);
    }
    services = sl.toArray(new String[sl.size()]);
  }

  /**
   *
   */
  private static void skip(XmlPullParser p)
    throws IOException, XmlPullParserException
  {
    int level = 0;
    int event = p.getEventType();

    while (event != XmlPullParser.END_DOCUMENT) {
      if (event == XmlPullParser.START_TAG) {
        level++;
      } else if (event == XmlPullParser.END_TAG) {
        level--;
        if (level <= 0) {
          p.next(); // jump beyond stopping tag.
          break;
        }
      }
      event = p.next();
    }
  }


  /**
   *
   */
  private static boolean parseBoolean(XmlPullParser p, int attr)
    throws IllegalXMLException
  {
    String val = p.getAttributeValue(attr);

    if ("true".equals(val)) {
      return true;
    } else if ("false".equals(val)) {
      return false;
    } else {
      throw new IllegalXMLException("Attribute \"enabled\" of \"" +
                                    p.getName() + "\"-tag has invalid value. "+
                                    "Excepted true/false got \"" + val + "\"", p);
    }

  }


  /**
   *
   */
  private static String getSingleAttribute(XmlPullParser p, String name)
    throws IllegalXMLException
  {
    String res = null;
    for (int i = 0; i < p.getAttributeCount(); i++) {
      if (p.getAttributeName(i).equals(name)) {
        res = p.getAttributeValue(i);
      } else {
        unrecognizedAttr(p, i);
      }
    }

    if (res == null) {
      missingAttr(p, name);
    }
    return res;
  }


  /**
   *
   */
  private static void unrecognizedAttr(XmlPullParser p, int attr)
    throws IllegalXMLException
  {
    throw new IllegalXMLException("Unrecognized attribute \"" + p.getAttributeName(attr) +
                                  "\" in \"" + p.getName() + "\"-tag", p);
  }


  /**
   *
   */
  private static void missingAttr(XmlPullParser p, String attr)
    throws IllegalXMLException
  {
    throw new IllegalXMLException("Missing \"" + attr+ "\" attribute in \"" +
                                  p.getName() + "\"-tag", p);
  }


  /**
   *
   */
  private static void invalidValue(XmlPullParser p,
                                   String[] expected, int attr)
    throws IllegalXMLException {
    StringBuffer buf = new StringBuffer();
    buf.append("Attribute " + p.getAttributeName(attr) +
               " of \"" + p.getName() + "\"-tag has invalid value, expected one of ");

    for (int i = 0; i < expected.length - 1; i++) {
      buf.append("\"" + expected[i] + "\", /");
    }
    buf.append("but got \"" + p.getAttributeValue(attr) + "\".");
    throw new IllegalXMLException(buf.toString(), p);
  }


  /**
   *
   */
  private static void checkValidIdentifier(final String val, final String attr,
                                           XmlPullParser p)
    throws IllegalXMLException
  {
    boolean ok = true;
    for (int i = 0; i < val.length(); i++) {
      if (!Character.isJavaIdentifierStart(val.charAt(i))) {
        ok = false;
        break;
      }
      for (; i < val.length(); i++) {
        char c = val.charAt(i);
        if (!Character.isJavaIdentifierPart(c)) {
          ok = c == '.' && val.length() > i + 1;
          break;
        }
      }
    }
    if (!ok) {
      throw new IllegalXMLException("Attribute \"" + attr +
                                    "\" in reference-tag has an invalid value = \""
                                    + val + "\"", p);
    }
  }

}
