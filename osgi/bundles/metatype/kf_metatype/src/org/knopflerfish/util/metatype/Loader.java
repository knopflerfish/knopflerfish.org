/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

//TODO use only one parser...

package org.knopflerfish.util.metatype;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import net.n3.nanoxml.XMLElement;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Helper class which loads (and saves) KF Metatype XML, as well as the R4
 * Metatype XML.
 *
 * <p>
 * This implementation uses the nanoxml package for KF Metatype XML, and kXML
 * for R4 Metatype XML.
 * </p>
 *
 * <p>
 * NanoXML is distributed under the zlib/libpng license.<br>
 * See <a
 * href="http://nanoxml.sourceforge.net/orig/copyright.html">http://nanoxml
 * .sourceforge.net/orig/copyright.html</a> for details.<br>
 * The full license text is also include in the kf_metatype bundle jar.
 * </p>
 *
 * <p>
 * Nanoxml is Copyrighted 2000-2002 Marc De Scheemaecker, All Rights Reserved.
 * </p>
 */
public class Loader extends KFLegacyMetaTypeParser {
  // ----------------- R4 -----------------------------------------------------

  // TODO finish the impl

  static final String METADATA = "MetaData";
  static final String OCD = "OCD";
  static final String AD_E = "AD";
  static final String OBJECT = "Object";
  static final String ATTRIBUTE = "Attribute";
  static final String DESIGNATE = "Designate";
  static final String OPTION = "Option";
  static final String ICON = "Icon";
  static final String VALUE = "Value";
  static final String CONTENT = "Content";

  static final String ATTR_LOCALIZATION = "localization";

  static final String ATTR_PID = "pid";
  static final String ATTR_DECS = "description";

  static final String ATTR_ID = "id";
  static final String ATTR_DESCRIPTION = "description";

  static final String ATTR_CARDINALITY = "cardinality";
  static final String ATTR_MIN = "min";
  static final String ATTR_MAX = "max";
  static final String ATTR_DEFAULT = "default";
  static final String ATTR_REQUIRED = "required";

  static final String ATTR_OCDREF = "ocdref";

  static final String ATTR_ADREF = "adref";
  static final String ATTR_CONTENT = "content";

  static final String ATTR_FACTORYPID = "factoryPid";
  static final String ATTR_BUNDLE = "bundle";
  static final String ATTR_OPTIONAL = "optional";
  static final String ATTR_MERGE = "merge";

  static final String ATTR_LABEL = "label";

  static final String ATTR_RESOURCE = "resource";
  static final String ATTR_SIZE = "size";

  private static final String CHARACTER_ENCODING = "UTF8";

  private static XmlPullParser xml_parser/* = null */;

  private static String content/* = null */;

  private static BundleMetaTypeResource currentBMTR;
  private static MetaData currentMetaData;
  private static OCD currentOCD;
  private static AD currentAD;
  private static List<String> currentOptionLabels = new ArrayList<>();
  private static List<String> currentOptionValues = new ArrayList<>();

  private static String currentDesignatePid;
  private static String currentDesignateFactoryPid;
  private static String currentObjectOCDref;

  private static ServiceTracker<?, ?> confAdminTracker;
  private static Configuration currentConf;
  private static List<AE> currentAttributes;
  private static AE currentAE;

  private static Bundle currentBundle;

  public static BundleMetaTypeResource loadBMTIfromUrl(BundleContext bc,
                                                       Bundle b,
                                                       URL url)
      throws IOException
  {
    if (xml_parser == null) {
      xml_parser = new KXmlParser();

      confAdminTracker =
          new ServiceTracker<>(bc,
              ConfigurationAdmin.class.getName(),
              null);
      confAdminTracker.open();
    }

    currentBMTR = new BundleMetaTypeResource(b);

    currentBundle = b;

    try {
      processDocument(xml_parser, url);
      return currentBMTR;
    } catch (final Exception e) {
      throw new IOException("Failed to load " + url + " " + e);
    }

  } // method

  private static void processDocument(XmlPullParser xpp, URL url)
      throws XmlPullParserException, IOException
  {
    try (InputStream in = url.openStream()) {

      xpp.setInput(in, CHARACTER_ENCODING);

      int eventType = xpp.getEventType();

      do {
        if (eventType == XmlPullParser.START_DOCUMENT) {
          // System.out.println("Start document");
        } else if (eventType == XmlPullParser.END_DOCUMENT) {
          // System.out.println("End document");
        } else if (eventType == XmlPullParser.START_TAG) {
          try {
            startElement(xpp.getName(), url);
          } catch (final Exception e) {
            // System.out.println("Got exception:" + e);
            // e.printStackTrace(System.err);
          }
          // System.out.println("Start element: " + name);
        } else if (eventType == XmlPullParser.END_TAG) {
          try {
            endElement(xpp.getName(), content);
          } catch (final Exception e) {
            // System.out.println("Got exception");
          }
          // System.out.println("End element: " + name);
        } else if (eventType == XmlPullParser.TEXT) {

          content = xpp.getText().trim();
          // System.out.println("Text: " + content);
        //} else {
          // System.out.println("Got something else");
        }
        try {
          eventType = xpp.next();
        } catch (final IOException ex) {
          // System.out.println(ex); //stream closed for example
          return; // TODO proper handling
        } catch (final XmlPullParserException e) { // catch also initial call
                                                   // upstairs
          // System.out.println(e);
          return; // TODO proper handling
        }
      } while (eventType != XmlPullParser.END_DOCUMENT);
      // System.out.println("End document");
      // System.out.flush();
    }
  } // method

  // any missing attribute gets the element ignored
  protected static void startElement(String element, URL sourceURL)
      throws Exception
  {
    final int n_attrs = xml_parser.getAttributeCount();
    final HashMap<String, String> attrs = new HashMap<>();
    for (int i = 0; i < n_attrs; i++) {
      attrs
          .put(xml_parser.getAttributeName(i), xml_parser.getAttributeValue(i));
    }

    if (METADATA.equals(element) || element.endsWith(METADATA)) {
      final String localization = attrs.get(ATTR_LOCALIZATION);
      if (localization != null) {
        currentMetaData = new MetaData(localization, currentBundle);
      } else {
        currentMetaData = new MetaData(currentBundle);
      }
    } else if (OCD.equals(element)) {
      final String id = attrs.get(ATTR_ID);
      if (id == null) {
        return;// TODO not valid: required attribute is missing
      }

      final String name = attrs.get(ATTR_NAME);
      if (name == null) {
        // TODO not valid: required attribute is missing
        return;
      }

      final String desc = attrs.get(ATTR_DESCRIPTION);

      currentOCD = new OCD(id, name, desc, sourceURL);
    } else if (AD_E.equals(element)) {
      final String id = attrs.get(ATTR_ID);
      if (id == null) {
        throw new Exception("Missing attribute: " + ATTR_ID);
      }

      final String name = attrs.get(ATTR_NAME);
      final String desc = attrs.get(ATTR_DESCRIPTION);

      final String typeS = attrs.get(ATTR_TYPE);
      int type;
      if (typeS != null) {
        type = getType(typeS);
      } else {
        throw new Exception("Invalid type: " + typeS);
      }

      final String card = attrs.get(ATTR_CARDINALITY);
      int cardinality;
      if (card != null) {
        cardinality = Integer.parseInt(card);
      } else {
        cardinality = 0;
      }

      final String min = attrs.get(ATTR_MIN);
      final String max = attrs.get(ATTR_MAX);

      final String default_attr = attrs.get(ATTR_DEFAULT);
      String[] defaults = null;
      if (default_attr != null) {
        final StringTokenizer st = new StringTokenizer(default_attr, ",");
        final int number = st.countTokens();
        if (number > 0) {
          defaults = new String[number];
          for (int i = 0; i < defaults.length; i++) {
            defaults[i] = st.nextToken();
          }
        }
      }

      final String requiredS = attrs.get(ATTR_REQUIRED);
      boolean required;
      if (requiredS != null) {
        required = Boolean.parseBoolean(requiredS);
      } else {
        required = true;
      }

      currentAD =
        new AD(id, type, cardinality, name, desc, defaults, min, max, required);
    } else if (OBJECT.equals(element)) {
      final String ocdref = attrs.get(ATTR_OCDREF);
      if (ocdref != null) {
        currentObjectOCDref = ocdref;
      } else {
        // TODO not valid: required attribute is missing
        return;
      }
    } else if (ATTRIBUTE.equals(element)) {
      final String adref = attrs.get(ATTR_ADREF);
      if (adref != null) {
        currentAE = new AE(adref);
      } else {
        // TODO not valid: required attribute is missing
        return;
      }

      final String content = attrs.get(ATTR_CONTENT);
      if (content != null) {
        final StringTokenizer st = new StringTokenizer(content, ",");
        while (st.hasMoreTokens()) {
          currentAE.addValue(st.nextToken());
        }
      }

      currentAttributes.add(currentAE);
    } else if (DESIGNATE.equals(element)) {
      // TODO: If optional and error detected ignore this DESIGNATE definition.
      @SuppressWarnings("unused")
      boolean optionalB;
      final String optional = attrs.get(ATTR_OPTIONAL);
      if (optional != null) {
        //noinspection UnusedAssignment
        optionalB = Boolean.parseBoolean(optional);
      } else {
        //noinspection UnusedAssignment
        optionalB = false;
      }

      final String pid = attrs.get(ATTR_PID);
      if (pid != null) {
        currentDesignatePid = pid;
      }
      /*
       * else{ //TODO not valid: required attribute is missing if(!optionalB){
       * return; } }
       */

      final String factoryPid = attrs.get(ATTR_FACTORYPID);
      if (factoryPid != null && !factoryPid.equals("")) {
        currentDesignateFactoryPid = factoryPid;
        currentDesignatePid = null;
      }

      final String bundle_location = attrs.get(ATTR_BUNDLE);

      if (currentDesignatePid != null) {
        final ConfigurationAdmin ca =
          (ConfigurationAdmin) confAdminTracker.getService();
        if (ca != null) {
          currentConf =
            ca.getConfiguration(currentDesignatePid, bundle_location);

          final String merge = attrs.get(ATTR_MERGE);

          if (!Boolean.parseBoolean(merge)) {
            currentConf.delete();
            currentConf =
              ca.getConfiguration(currentDesignatePid, bundle_location);
          }

          final String location = currentConf.getBundleLocation();
          if (location != null && !location.equals(bundle_location)) {
            // currentConf = null; //will prevent processing
            currentConf.setBundleLocation(bundle_location);
          }
        }
      } else if (currentDesignateFactoryPid != null) {
        final ConfigurationAdmin ca =
          (ConfigurationAdmin) confAdminTracker.getService();
        if (ca != null) {
          currentConf =
            ca.createFactoryConfiguration(currentDesignateFactoryPid,
                                          bundle_location);
          // merge is meaningless
        }
      }

      currentAttributes = new ArrayList<>();
    } else if (OPTION.equals(element)) {

      final String label = attrs.get(ATTR_LABEL);
      if (label != null) {
        currentOptionLabels.add(label);
      } else {
        // TODO not valid: required attribute is missing
        return;
      }

      final String value = attrs.get(ATTR_VALUE);
      if (value != null) {
        currentOptionValues.add(value);
      } else {
        // TODO not valid: required attribute is missing
        return;
      }

    } else if (ICON.equals(element)) {

      final String resource = attrs.get(ATTR_RESOURCE);
      if (resource == null) {
        // TODO not valid: required attribute is missing
        return;
      }

      final String sizeS = attrs.get(ATTR_SIZE);
      int size;
      if (sizeS != null) {
        size = Integer.parseInt(sizeS);
      } else {
        // TODO not valid: required attribute is missing
        return;
      }

      currentOCD.addIcon(size, resource);
    }

  }

  protected static void endElement(String element, String content) {

    if (METADATA.equals(element) || element.endsWith(METADATA)) {
      currentBMTR.addMetaData(currentMetaData);
      currentMetaData.prepare();
      currentMetaData = null;
    } else if (OCD.equals(element)) {
      currentMetaData.addOCD(currentOCD);
      currentOCD = null;
    } else if (AD_E.equals(element)) {
      currentOCD.add(currentAD, currentAD.getRequired());
      String[] optionValues = null;
      String[] optionLabels = null;
      int number;
      if ((number = currentOptionValues.size()) > 0) {
        optionValues = currentOptionValues.toArray(new String[number]);
        // same number
        optionLabels = currentOptionLabels.toArray(new String[number]);

        // Must check that all default values are valid option values.
        if (currentAD.defValue != null) {
          // Any default value that is not a valid option value must be removed.
          List<String> defValues = new ArrayList<>(Arrays.asList(currentAD.defValue));
          defValues.retainAll(currentOptionValues);
          if (defValues.size()==0) {
            currentAD.defValue = null;
          } else {
            currentAD.defValue = defValues.toArray(new String[0]);
          }
        }
      }
      currentAD.setOptions(optionValues, optionLabels);
      currentOptionValues.clear();
      currentOptionLabels.clear();
      currentAD = null;
    } else if (DESIGNATE.equals(element)) {
      // MetaInfo
      currentMetaData.designate(currentDesignateFactoryPid,
                                currentDesignatePid, currentObjectOCDref,
                                currentConf, currentAttributes);

      currentDesignatePid = null;
      currentDesignateFactoryPid = null;
      currentAttributes = null;
      currentObjectOCDref = null;
      currentConf = null;
    } // seems like not sure yet see:
      // http://membercvs.osgi.org/bugs/show_bug.cgi?id=129
    else if (VALUE.equals(element) || CONTENT.equals(element)) {
      currentAE.addValue(content);
    }
  }

  static int getType(String strType)
  {
    switch (strType) {
      case "Integer":
        return AttributeDefinition.INTEGER;
      case "String":
        return AttributeDefinition.STRING;
      case "Boolean":
        return AttributeDefinition.BOOLEAN;
      case "Float":
        return AttributeDefinition.FLOAT;
      case "Long":
        return AttributeDefinition.LONG;
      case "Short":
        return AttributeDefinition.SHORT;
      case "Char":
        return AttributeDefinition.CHARACTER;
      case "Byte":
        return AttributeDefinition.BYTE;
      case "Double":
        return AttributeDefinition.DOUBLE;
      case "Password":
        return AttributeDefinition.PASSWORD;
    }
    throw new IllegalArgumentException("Unsupported type '" + strType);
  }

} // class

class XMLException
  extends IllegalArgumentException
{
  private static final long serialVersionUID = 1L;
  XMLElement el;

  public XMLException(XMLElement el)
  {
    this("", el);
  }

  public XMLException(String msg, XMLElement el)
  {
    super(msg + ", line=" + el.getLineNr());
    this.el = el;
  }

  public XMLElement getXMLElement()
  {
    return el;
  }
}

class CMConfig
{
  public String pid;
  public int maxInstances;
  public AD[] ads;
  public String desc;
  public String iconURL;

  public CMConfig(String pid, AD[] ads, String desc, String iconURL,
                  int maxInstances)
  {
    this.pid = pid;
    this.ads = ads;
    this.desc = desc != null ? desc : "";
    this.iconURL = iconURL;
    this.maxInstances = maxInstances;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();

    sb.append("CMConfig[");
    sb.append("pid=").append(pid);
    sb.append(", desc=").append(desc);
    sb.append(", iconURL=").append(iconURL);
    sb.append(", maxInstances=").append(maxInstances);
    sb.append(", attribs=");
    for (int i = 0; i < ads.length; i++) {
      sb.append(ads[i]);
      if (i < ads.length - 1) {
        sb.append(", ");
      }
    }

    return sb.toString();
  }
}

class Annotation
{
  String appinfo;
  String doc;

  public Annotation()
  {
  }
}
