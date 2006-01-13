/*
 * Copyright (c) 2006, KNOPFLERFISH project
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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.jar.JarInputStream;

import org.osgi.framework.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import org.knopflerfish.framework.*;


/**
 * @author Mats-Ola Persson, based on an implementation by Martin Berg, Magnus Klack (refactoring by Bjorn Andersson)
 */
public class Parser {

  static String[] supportedTypes = {"Boolean", "Byte", "Char",
                                    "Double", "Float", "Integer",
                                    "Long", "Short", "String"};

  static private String SCR_NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

  public static Collection readXML(Bundle declaringBundle,
                                   URL url) throws IllegalXMLException {

    try {
      return readXML(declaringBundle, url.openStream());
    } catch (IOException e) {

      throw new IllegalXMLException("Could not open \"" + url +
                                    "\" got exception.", e);
    }

  }

  public static Collection readXML(Bundle declaringBundle,
                                   InputStream stream) throws IllegalXMLException {

    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);

      XmlPullParser parser = factory.newPullParser();
      parser.setInput(stream, null);

      return readDocument(declaringBundle, parser);

    } catch (Exception e) {
      throw new IllegalXMLException("While reading declaration in \"" + stream +
                                    "\" got exception", e);
    }
  }

  private static ArrayList readDocument(Bundle declaringBundle, XmlPullParser parser)
    throws XmlPullParserException, IOException {

    ArrayList decls = new ArrayList();
    boolean foundImplementation = false;

    while (parser.next() != XmlPullParser.END_DOCUMENT) {

      if (parser.getEventType() != XmlPullParser.START_TAG &&
          parser.getEventType() != XmlPullParser.END_TAG) {
        continue; // nothing of interest to us.
      }

      if (parser.getName().equals("component") &&
          parser.getEventType() == XmlPullParser.START_TAG &&
          (parser.getDepth() == 1 || // assume scr here. The root is component
           SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {
        // we have found a proper component-tag here.

        try {
          decls.add(readComponent(declaringBundle,
                                  parser));

        } catch (Exception e) {
          Activator.log.error("Got exception when reading component-tag", e);
        }
      }
    }

    return decls;
  }

  private static Config readComponent(Bundle bundle, XmlPullParser parser)
    throws XmlPullParserException,
           IOException,
           IllegalXMLException {

    Config curr = new Config(bundle);
    boolean serviceFound = false;

    setComponent(curr, parser);

    int event = parser.getEventType();
    int level;

    while (event != XmlPullParser.END_TAG) {

      if (event != XmlPullParser.START_TAG) {
        // nothing of interest.
        event = parser.next();
        continue;
      }

      level = parser.getDepth();

      if (parser.getName().equals("implementation") &&
          (level == 2 && "".equals(parser.getNamespace())
           || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {
        setImplementation(curr, parser);

      } else if (parser.getName().equals("property") &&
                 (level == 2 && "".equals(parser.getNamespace())
                  || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {
        setProperty(curr, parser);

      } else if (parser.getName().equals("properties") &&
                 (level == 2 && "".equals(parser.getNamespace())
                  || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {
        setProperties(curr, parser, bundle);

      } else if (parser.getName().equals("service") &&
                 (level == 2 && "".equals(parser.getNamespace())
                  || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {

        if (!serviceFound) {
          serviceFound = true;
          setService(curr, parser);
          parser.next();
        } else
          throw new
            IllegalXMLException("More than one service-tag " +
                                "in component: \"" + curr.getName()
                                + "\"");

      } else if (parser.getName().equals("reference") &&
                 (level == 2 && "".equals(parser.getNamespace())
                  || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {
        setReference(curr, parser, bundle);


      } else {
        skip(parser);
      }

      event = parser.getEventType();
    }

    if (curr.getImplementation() == null)
      throw new IllegalXMLException("Component \"" + curr.getName() +
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

        if (level == 0) {
          parser.next(); // jump beyond stopping tag.
          break;
        }
      }

      event = parser.next();
    }
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

  private static void setProperty (Config compConf,
                                   XmlPullParser parser) throws IllegalXMLException,
                                                                XmlPullParserException,
                                                                IOException {

    String type = null;
    String name = null;
    Object retval = null;
    String[] values = null;
    boolean isArray = true;

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("name")) {
        name = parser.getAttributeValue(i);

      } else if (parser.getAttributeName(i).equals("value")) {
        values = new String[] {parser.getAttributeValue(i)};
        isArray = false;

      } else if (parser.getAttributeName(i).equals("type")) {

        for(int j = 0; j < supportedTypes.length ; j++){
          if (supportedTypes[j].equals(parser.getAttributeValue(i))) {
            type = supportedTypes[j];
            break;
          }

        }

        if (type == null)
          invalidValue(parser, supportedTypes, i); // throws exception


      } else {
        unrecognizedAttr(parser, i); // throws exception

      }
    }


    /* check if required attributes has been set */
    if (name == null) {
      missingAttr(parser, name); // throws Exception
    }

    if (isArray) {
      String text = parser.nextText();
      values = text.split("\n");

    }

    if (type == null || // defaults to string
        "String".equals(type)) {
      retval = values;

    } else if ("Boolean".equals(type)) {

      boolean[] array = new boolean[values.length];
      for (int i=0; i<array.length; i++) {
        if ("true".equals(values[i]))
          array[i] = true;
        else if ("false".equals(values[i]))
          array[i] = false;
        else
          throw new IllegalXMLException("Unexpected value \"" + values[i] + "\" of boolean property.");

      }

      retval = array;
    } else if ("Byte".equals(type)) {
      byte[] array = new byte[values.length];
      for (int i=0; i<array.length; i++) {
        array[i] = Byte.parseByte(values[i]);
      }

      retval = array;
    } else if ("Char".equals(type)) {

      char[] array = new char[values.length];
      for (int i=0; i<array.length; i++) {
        array[i] = values[i].charAt(0);
      }

      retval = array;
    } else if ("Double".equals(type)) {
      double[] array = new double[values.length];
      for (int i=0; i<array.length; i++) {
        array[i] = Double.parseDouble(values[i]);
      }

      retval = array;
    } else if ("Float".equals(type)) {
      float[] array = new float[values.length];
      for (int i=0; i<array.length; i++) {
        array[i] = Float.parseFloat(values[i]);
      }

      retval = array;
    } else if ("Integer".equals(type)) {
      int[] array = new int[values.length];
      for (int i=0; i<array.length; i++) {
        array[i] = Integer.parseInt(values[i]);
      }

      retval = array;
    } else if ("Long".equals(type)) {
      long[] array = new long[values.length];
      for (int i=0; i<array.length; i++) {
        array[i] = Long.parseLong(values[i]);
      }

      retval = array;
    } else if ("Short".equals(type)) {
      short[] array = new short[values.length];
      for (int i=0; i<array.length; i++) {
        array[i] = Short.parseShort(values[i]);
      }

      retval = array;
    } else {
      throw new IllegalXMLException("This is a bug. Did not recognize \"" + type +
                                    "\" in property-tag.");

    }

    if (isArray)
      parser.next();
    else
      skip(parser);


    compConf.setProperty(name, isArray ? (Object)retval : ((Object[])retval)[0]);
  }

  /*
    Parses a <service [servicefactory="<boolean>"]>
              <provide interface="<interface1>">
              <provide interface="<interface2>">
              ...
             </service>
   */
  private static void setService(Config compConf,
                                 XmlPullParser parser) throws IllegalXMLException,
                                                              XmlPullParserException,
                                                              IOException {
    boolean interfaceFound = false;

    /* If there is an attribute in the service tag */
    for (int i = 0; i < parser.getAttributeCount(); i++) {

      if (parser.getAttributeName(i).equals("servicefactory") &&
          (parser.getDepth() == 2 && "".equals(parser.getNamespace())
           || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {
        compConf.setServiceFactory(parseBoolean(parser, i));

      } else {
        throw new IllegalXMLException("Unrecognized attribute \"" +
                                      parser.getAttributeName(i) +
                                      "\" in service-tag");
      }
    }

    int event = parser.next();
    int level;

    while (event != XmlPullParser.END_TAG) {

      if (event != XmlPullParser.START_TAG) {
        event = parser.next();
        continue;
      }

      level = parser.getDepth();

      if ("provide".equals(parser.getName()) &&
          (level == 3 && parser.getNamespace().equals("")
           || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {

        String interfaceName = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {

          if (parser.getAttributeName(i).equals("interface")) {
            interfaceName = parser.getAttributeValue(i);
            interfaceFound = true;

          } else {
            throw new IllegalXMLException("Unrecognized attribute \"" +
                                          parser .getAttributeName(i) +
                                          "\" in provide-tag");
          }
        }

        if (interfaceName == null)
          missingAttr(parser, "interface");

        compConf.addService(interfaceName);
        skip(parser);

      } else {
        skip(parser);
      }

      event = parser.getEventType();
    }

    /* check if required attributes has been set */
    if (!interfaceFound) {
      throw new IllegalXMLException("Service-tag did not contain a proper \"provides\"-tag.");

    }
  }

  /*
    Parses a <component name="<name>" [enabled="<boolean>"] [factory="boolean"] [immediate="boolean"]>
             ....
             </component>
   */
  private static void setComponent(Config compConf,
                                   XmlPullParser parser) throws IllegalXMLException,
                                                                XmlPullParserException,
                                                                IOException {

    String name = null;
    boolean enabled = true; // default value.

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("name")) {
        name = parser.getAttributeValue(i);

      } else if (parser.getAttributeName(i).equals("enabled")) {
        enabled = parseBoolean(parser, i);

      } else if (parser.getAttributeName(i).equals("factory")) {
        /*optional attribute */
        compConf.setFactory(parser.getAttributeValue(i));

      } else if (parser.getAttributeName(i).equals("immediate")) {
        /*optional attribute */
        compConf.setImmediate(parseBoolean(parser, i));

      } else {
        unrecognizedAttr(parser, i); // throws exception
      }
    }

    parser.next(); // can't use skip here since we are going to read the body.


    if (name == null) {
      missingAttr(parser, "name"); // throws exception
    }

    compConf.setAutoEnabled(enabled);
    compConf.setName(name);
  }

  /*
    Parses a <properties entry="<url>"/>
    and then reads a property specified by <url>
   */
  private static void setProperties(Config compConf,
                                    XmlPullParser parser,
                                    Bundle declaringBundle) throws IllegalXMLException,
                                                                   XmlPullParserException,
                                                                   IOException {
    String entry = null;

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("entry")) {
        entry = parser.getAttributeValue(i);
      } else {
        unrecognizedAttr(parser, i); // throws exception
      }
    }

    if (entry == null) {
      missingAttr(parser, "entry"); // throws exception
    }

    // read a property-file and adds it contents to conf's properties.
    Properties dict = new Properties();
    String bundleLocation = declaringBundle.getLocation();

    JarInputStream jis =
      new JarInputStream(new URL(bundleLocation).openStream());
    ZipEntry zipEntry;

    while ((zipEntry = jis.getNextEntry()) != null &&
           !zipEntry.getName().equals(entry))
      /* skip */ ;

    if (zipEntry == null) {
      throw new IOException("Did not find requested entry " + entry);
    }

    dict.load(jis);

    for (Enumeration e = dict.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      compConf.setProperty((String)key, dict.get(key));
    }

    // done reading file.

    skip(parser);


  }

  /*
    Parsers a <implementation class="<classname>"/>
  */
  private static void setImplementation(Config compConf,
                                        XmlPullParser parser) throws IllegalXMLException,
                                                                     XmlPullParserException,
                                                                     IOException {

    String className = null;

    if (compConf.getImplementation() != null) {
      throw new IllegalXMLException("Only one implementation tag allowed");
    }

    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("class")) {
        className = parser.getAttributeValue(i);

      } else {
        unrecognizedAttr(parser, i); // throws exception
      }
    }

    skip(parser);
    if (className == null) {
      missingAttr(parser, "class"); // throws exception
    }

    compConf.setImplementation(className);
  }

  /*
    Parses a
    <reference name="<name>" interface="<interface>"
        [bind="<bind-method>"] [unbind="<bind-method>"]
        [cardinality="<cardinality>"] [policy="<policy>"]/>


   */
  private static void setReference(Config compConf,
                                   XmlPullParser parser,
                                   Bundle declaringBundle) throws IllegalXMLException,
                                                                  XmlPullParserException,
                                                                  IOException {

    String name = null;
    String interfaceName = null;
    String target = null;
    String bind = null;
    String unbind = null;
    boolean optional = false; // default value
    boolean multiple = false; // default value
    boolean dynamic = false; // default value

    for (int i = 0; i < parser.getAttributeCount(); i++) {

      if (parser.getAttributeName(i).equals("name")) {
        if (checkNMToken(parser.getAttributeValue(i))) {
          name = parser.getAttributeValue(i);

        } else {
          throw new IllegalXMLException("Attribute \"" + parser.getAttributeName(i) +
                                        "\" in reference-tag is invalid.");
        }
      } else if (parser.getAttributeName(i).equals("interface")) {
        if (checkToken(parser.getAttributeValue(i))) {
          interfaceName = parser.getAttributeValue(i);

        } else {
          throw new IllegalXMLException("Attribute \""
                                        + parser.getAttributeName(i)
                                        + "\" in reference-tag is invalid");
        }
      } else if (parser.getAttributeName(i).equals("cardinality")) {
        String val = parser.getAttributeValue(i);

        if ("1..1".equals(val)) {
          multiple = optional = false;
        } else if ("0..1".equals(val)) {
          optional = true;
          multiple = false;
        } else if ("1..N".equals(val)) {
          optional = false;
          multiple = true;
        } else if ("0..N".equals(val)) {
          multiple = optional = true;
        } else {
          invalidValue(parser,
                       new String[]{"1..1", "0..1",
                                    "1..N", "0..N"}, i);
        }

      } else if (parser.getAttributeName(i).equals("policy")) {
        String val = parser.getAttributeValue(i);

        if ("static".equals(val)) {
          dynamic = false;
        } else if ("dynamic".equals(val)) {
          dynamic = true;
        } else {
          invalidValue(parser, new String[]{"static", "dynamic"}, i);
        }

      } else if (parser.getAttributeName(i).equals("target")) {
        target = parser.getAttributeValue(i);

      } else if (parser.getAttributeName(i).equals("bind")) {
        bind = parser.getAttributeValue(i);

      } else if (parser.getAttributeName(i).equals("unbind")) {
        unbind = parser.getAttributeValue(i);

      } else {
        unrecognizedAttr(parser, i);
      }
    }

    skip(parser);

    if (name == null)
      missingAttr(parser, "name");

    if (interfaceName == null)
      missingAttr(parser, "interface");


    BundleContext bc = Backdoor.getBundleContext(declaringBundle);

    try {
      Filter filter;
      if (target != null) {
        filter =
          bc.createFilter("(&(" + Constants.OBJECTCLASS +
                          "=" + interfaceName +")" + target + ")");
      } else {
        filter =
          bc.createFilter("(" + Constants.OBJECTCLASS + "=" + interfaceName +")");
      }
      
      Reference ref = new Reference(name, filter,
                                    optional, multiple, dynamic,
                                    bind, unbind, bc);

      compConf.addReference(ref);
    } catch (InvalidSyntaxException e) {
      throw new IllegalXMLException("Couldn't create filter for reference \"" + name + "\"", e);
    }

  }

  //TODO Check if the string follows the NMTOKEN in XML SCHEMA
  // MO: have not check this yet.
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


  private static void unrecognizedAttr(XmlPullParser parser, int attr)
    throws IllegalXMLException {
    throw new IllegalXMLException("Unrecognized attribute \"" +
                                  parser.getAttributeName(attr) +
                                  "\" in \"" + parser.getName() + "\"-tag.");
  }

  private static void missingAttr(XmlPullParser parser, String attr)
    throws IllegalXMLException {
    throw new IllegalXMLException("Missing \"" + attr+ "\" attribute in \"" +
                                  parser.getName() + "\"-tag.");
  }

  private static void invalidValue(XmlPullParser parser,
                                   String[] expected, int attr)
    throws IllegalXMLException {
    StringBuffer buf = new StringBuffer();

    buf.append("Attribute " + parser.getAttributeName(attr) +
               " of \"" + parser.getName() + "\"-tag has invalid value.");

    for (int i = 0; i < expected.length - 1; i++)
      buf.append("\"" + expected[i] + "\"/");

    buf.append(expected[expected.length - 1] +
               " but got \"" + parser.getAttributeValue(attr) + "\".");


    throw new IllegalXMLException(buf.toString());
  }



  private static boolean parseBoolean(XmlPullParser parser, int attr)
    throws IllegalXMLException {
    String val = parser.getAttributeValue(attr);

    if ("true".equals(val)) {
      return true;
    } else if ("false".equals(val)) {
      return false;
    } else {
      throw new IllegalXMLException("Attribute \"enabled\" of \"" +
                                    parser.getName() + "\"-tag has invalid value. "+
                                    "Excepted true/false got \"" + val + "\"");
    }

  }
}
