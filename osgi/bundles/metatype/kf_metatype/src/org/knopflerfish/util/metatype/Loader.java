/*
 * Copyright (c) 2003-2014, KNOPFLERFISH project
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

/**
 * @author Erik Wistrand
 * @author Philippe Laporte
 */

//TODO use only one parser...

package org.knopflerfish.util.metatype;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.IXMLReader;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLElement;
import net.n3.nanoxml.XMLParserFactory;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.util.Text;

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
public class Loader
{
  static final String METATYPE = "metatype";
  static final String SERVICES = "services";
  static final String FACTORIES = "factories";
  static final String VALUES = "values";
  static final String SCHEMA = "schema";

  static final String SERVICE_PID = "service.pid";
  static final String FACTORY_PID = "factory.pid";

  static final String ITEM = "item";

  static final String ATTR_PID = "pid";
  static final String ATTR_DECS = "description";

  static final String ATTR_TYPE = "type";
  static final String ATTR_NAME = "name";
  static final String ATTR_BASE = "base";
  static final String ATTR_VALUE = "value";
  static final String ATTR_ICONURL = "iconURL";
  static final String ATTR_MINOCCURS = "minOccurs";
  static final String ATTR_MAXOCCURS = "maxOccurs";

  static final String ATTR_ARRAY = "array";

  static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";
  static final String METATYPE_NS = "https://www.knopflerfish.org/XMLMetatype";

  static final String TAG_ANNOTATION = "annotation";
  static final String TAG_SIMPLETYPE = "simpleType";
  static final String TAG_COMPLEXTYPE = "complexType";
  static final String TAG_ELEMENT = "element";
  static final String TAG_RESTRICTION = "restriction";
  static final String TAG_ENUMERATION = "enumeration";

  static final String TAG_DOCUMENTATION = "documentation";
  static final String TAG_APPINFO = "appInfo";
  static final String TAG_SEQUENCE = "sequence";

  static final String BUNDLE_PROTO = "bundle://";

  /**
   * Load a MetaTypeProvider from an XML file.
   */
  public static MTP loadMTPFromURL(Bundle bundle, URL url)
      throws IOException
  {
    InputStream in = null;

    try {
      in = url.openStream();
      final IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
      final IXMLReader reader = new StdXMLReader(in);
      parser.setReader(reader);
      final XMLElement el = (XMLElement) parser.parse();
      return loadMTP(bundle, url, el);
    } catch (final Throwable t) {
      throw (IOException) new IOException("Failed to load " + url + " " + t)
          .initCause(t);
    } finally {
      try {
        in.close();
      } catch (final Exception ignored) {
      }
    }

  }

  /**
   * Load defaults from an XML file into an MTP.
   */
  public static List<Dictionary<String, Object>> loadDefaultsFromURL(MTP mtp,
                                                                     URL url)
      throws IOException
  {
    InputStream in = null;

    try {
      in = url.openStream();
      final IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
      final IXMLReader reader = new StdXMLReader(in);
      parser.setReader(reader);
      final XMLElement el = (XMLElement) parser.parse();

      if (isName(el, METATYPE_NS, VALUES)) {
        final List<Dictionary<String, Object>> propList = loadValues(mtp, el);
        setDefaultValues(mtp, propList);
        return propList;
      } else {
        for (final Enumeration<?> e = el.enumerateChildren(); e
            .hasMoreElements();) {
          final XMLElement childEl = (XMLElement) e.nextElement();
          if (isName(childEl, METATYPE_NS, VALUES)) {

            final List<Dictionary<String, Object>> propList =
              loadValues(mtp, childEl);

            setDefaultValues(mtp, propList);

            return propList;
          }
        }
      }
      throw new XMLException("No values tag in " + url, el);
    } catch (final Throwable t) {
      throw (IOException) new IOException("Failed to load " + url + " " + t)
          .initCause(t);
    } finally {
      try {
        in.close();
      } catch (final Exception ignored) {
      }
    }
  }

  /**
   * Load a MetaTypeProvider from an XML "config" element.
   *
   * <ol>
   * <li>Load all service and factory definitions into a MetaTypeProvider
   * instance.
   * <li>Load any default data.
   * <li>Insert default data into definitions in MetaTypeProvider using the
   * <tt>setDefaultValues</tt> method.
   * </ol>
   *
   */
  public static MTP loadMTP(Bundle bundle, URL sourceURL, XMLElement el)
  {

    assertTagName(el, METATYPE_NS, METATYPE);

    CMConfig[] services = null;
    CMConfig[] factories = null;

    boolean bHasDefValues = false;

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();
      if (isName(childEl, METATYPE_NS, SERVICES)) {
        services = parseServices(childEl, false);
      } else if (isName(childEl, METATYPE_NS, FACTORIES)) {
        factories = parseServices(childEl, true);
      } else if (isName(childEl, METATYPE_NS, VALUES)) {
        bHasDefValues = true;
      } else if (isName(childEl, XSD_NS, SCHEMA)) {
        final CMConfig[] any = parseSchema(childEl);
        final List<CMConfig> sa = new ArrayList<CMConfig>();
        final List<CMConfig> fa = new ArrayList<CMConfig>();
        for (final CMConfig element : any) {
          if (element.maxInstances > 1) {
            fa.add(element);
          } else {
            sa.add(element);
          }
        }
        services = new CMConfig[sa.size()];
        sa.toArray(services);
        factories = new CMConfig[fa.size()];
        fa.toArray(factories);
      } else {
        throw new XMLException("Unexpected element", el);
      }
    }

    final MTP mtp = new MTP(sourceURL.toString());
    mtp.setBundle(bundle);

    // Insert services and factory definition into MTP
    // default values will be default values defined by AD
    for (int i = 0; services != null && i < services.length; i++) {
      final OCD ocd =
        new OCD(services[i].pid, services[i].pid, services[i].desc, sourceURL);
      ocd.maxInstances = 1;
      String iconURL = services[i].iconURL;
      if (iconURL != null) {
        try {
          if (bundle != null) {
            if (iconURL.startsWith("/")) {
              iconURL = BUNDLE_PROTO + "$(BID)" + iconURL;
            }
            iconURL =
              Text.replace(iconURL, "$(BID)",
                           Long.toString(bundle.getBundleId()));
          }
          ocd.setIconURL(iconURL);
        } catch (final Exception e) {
          System.err.println("Failed to set icon url: " + e);
        }
      }
      for (final AD ad : services[i].ads) {
        ocd.add(ad, ad.isOptional()
          ? ObjectClassDefinition.OPTIONAL
          : ObjectClassDefinition.REQUIRED);
      }
      mtp.addService(services[i].pid, ocd);
    }

    for (int i = 0; factories != null && i < factories.length; i++) {
      final OCD ocd =
        new OCD(factories[i].pid, factories[i].pid, factories[i].desc,
                sourceURL);
      ocd.maxInstances = factories[i].maxInstances;
      if (factories[i].iconURL != null) {
        try {
          ocd.setIconURL(factories[i].iconURL);
        } catch (final Exception e) {
          System.err.println("Failed to set icon url: " + e);
        }
      }
      for (final AD ad : factories[i].ads) {
        ocd.add(ad, ObjectClassDefinition.REQUIRED);
      }
      mtp.addFactory(factories[i].pid, ocd);
    }

    // Overwrite MTP default values with values found in
    // DEFAULTVALUES section in source XML
    if (bHasDefValues) {
      for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
        final XMLElement childEl = (XMLElement) e.nextElement();
        if (isName(childEl, METATYPE_NS, VALUES)) {
          final List<Dictionary<String, Object>> propList =
            loadValues(mtp, childEl);

          setDefaultValues(mtp, propList);
        }
      }
    }

    return mtp;
  }

  /**
   * Overwrite default values in MTP using a set of dictionaries.
   *
   * @param mtp
   *          MetaTypeProvider containing instances of <tt>AD</tt>
   * @param propList
   *          List of Dictionary
   */
  public static void setDefaultValues(MetaTypeProvider mtp,
                                      List<Dictionary<String, Object>> propList)
  {

    for (final Dictionary<String, Object> dictionary : propList) {
   final Dictionary<?, ?> props = dictionary;
   String pid = (String) props.get(SERVICE_PID);
   if (pid == null) {
    pid = (String) props.get("factory.pid");
   }

   ObjectClassDefinition ocd = null;
   try {
    ocd = mtp.getObjectClassDefinition(pid, null);
   } catch (final Exception ignored) {
   }

   if (ocd == null) {
    throw new IllegalArgumentException("No definition for pid '" + pid
                                       + "'");
   } else {
    final AttributeDefinition[] ads =
      ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);

    for (int i = 0; ads != null && i < ads.length; i++) {
      final Object val = props.get(ads[i].getID());

      if (!(ads[i] instanceof AD)) {
        throw new IllegalArgumentException(
                                           "AttributeDefinitions must be instances of AD, otherwise default values cannot be set");
      }

      final AD ad = (AD) ads[i];

      if (val instanceof Vector) {
        ad.setDefaultValue(toStringArray((Vector<?>) val));
      } else if (val.getClass().isArray()) {
        ad.setDefaultValue(toStringArray((Object[]) val));
      } else {
        ad.setDefaultValue(new String[] { val.toString() });
      }
    }
   }
  }
  }

  /**
   * @return String (pid) -> Dictionary
   */
  public static List<Dictionary<String, Object>> loadValues(MetaTypeProvider mtp,
                                                            XMLElement el)
  {

    // assertTagName(el, DEFAULTVALUES);

    final List<Dictionary<String, Object>> propList =
      new ArrayList<Dictionary<String, Object>>();

    // String (pid) -> Integer (count)
    final Map<String, Integer> countMap = new HashMap<String, Integer>();

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();
      final String pid = childEl.getName();
      ObjectClassDefinition ocd = null;

      try {
        ocd = mtp.getObjectClassDefinition(pid, null);
      } catch (final Exception ignored) {
      }
      if (ocd == null) {
        throw new XMLException("Undefined pid '" + pid + "'", childEl);
      }
      final Dictionary<String, Object> props =
        loadValues(ocd.getAttributeDefinitions(ObjectClassDefinition.ALL),
                   childEl);
      int maxInstances = 1;
      if (ocd instanceof OCD) {
        maxInstances = ((OCD) ocd).maxInstances;
      }

      Integer count = countMap.get(pid);
      if (count == null) {
        count = new Integer(0);
      }
      count = new Integer(count.intValue() + 1);
      if (count.intValue() > maxInstances) {
        throw new XMLException("PID " + pid + " can only have " + maxInstances
                               + " instance(s), found " + count, el);
      }

      countMap.put(pid, count);

      props.put(maxInstances > 1 ? "factory.pid" : SERVICE_PID, pid);
      propList.add(props);
    }

    return propList;
  }

  public static Dictionary<String, Object> loadValues(AttributeDefinition[] attrs,
                                                      XMLElement el)
  {

    if (attrs == null) {
      throw new NullPointerException("attrs array cannot be null");
    }

    final Hashtable<String, Object> props = new Hashtable<String, Object>();
    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();
      final String id = childEl.getFullName();

      AttributeDefinition attr = null;

      // System.out.println("load id=" + id);

      for (int i = 0; attr == null && i < attrs.length; i++) {
        // System.out.println(i + ": " + attrs[i]);
        if (id.equals(attrs[i].getID())) {
          attr = attrs[i];
        }
      }
      if (attr == null) {
        throw new XMLException("Undefined id '" + id + "'", childEl);
      }
      final Object val = loadValue(attr, childEl);
      props.put(id, val);
    }

    // Verify that all attributes are found
    for (int i = 0; i < attrs.length; i++) {
      if (!props.containsKey(attrs[i].getID())) {
        throw new XMLException("Missing attribute id '" + attrs[i].getID()
                               + "'", el);
      }
    }
    return props;
  }

  /**
   * Load a java object from an XML element using type info in the specified
   * definition.
   *
   */
  public static Object loadValue(AttributeDefinition attr, XMLElement el)
  {
    assertTagName(el, attr.getID());

    if (attr.getCardinality() < 0) {
      return loadSequence(attr, el, -attr.getCardinality(), ITEM);
    }

    if (attr.getCardinality() > 0) {
      final Vector<Object> v =
        loadSequence(attr, el, -attr.getCardinality(), ITEM);
      final Object[] array =
        (Object[]) Array.newInstance(AD.getClass(attr.getType()), v.size());
      v.copyInto(array);

      return array;
    }

    return loadContent(attr, el);
  }

  public static Vector<Object> loadSequence(AttributeDefinition attr,
                                            XMLElement el,
                                            int max,
                                            String tagName)
  {
    final Vector<Object> v = new Vector<Object>();

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();

      assertTagName(childEl, tagName);

      v.addElement(loadContent(attr, childEl));
    }

    return v;
  }

  /**
   * Load the contents of a tag into a java object.
   *
   * @param el
   *          element which content should be converted to a java object.
   * @param attr
   *          definition defining type.
   */
  public static Object loadContent(AttributeDefinition attr, XMLElement el)
  {
    String content = el.getContent();

    if (content == null) {
      content = "";
    }

    content = content.trim();

    final String msg = attr.validate(content);
    if (msg != null && !"".equals(msg)) {
      throw new XMLException("Validation error: '" + msg + "'", el);
    }

    return AD.parseSingle(content, attr.getType());
  }

  /**
   * Parse a services or factories tag info an array of wrapper objects.
   *
   * <p>
   * Children to this tag must all be "xsd:schema"
   * </p>
   */
  static CMConfig[] parseServices(XMLElement el, boolean bFactory)
  {

    assertTagName(el, METATYPE_NS, bFactory ? FACTORIES : SERVICES);

    final List<CMConfig> list = new ArrayList<CMConfig>();

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();

      final CMConfig[] conf = parseSchema(childEl);
      if (conf.length == 0) {
        throw new XMLException("No elements in schema", childEl);
      }
      conf[0].maxInstances = bFactory ? Integer.MAX_VALUE : 1;

      list.add(conf[0]);
    }

    final CMConfig[] ads = new CMConfig[list.size()];

    list.toArray(ads);
    return ads;
  }

  /**
   * Parse an XSD schema into a wrapper object for services and factories.
   *
   * <p>
   * Each schema element must contain exacly one child "xsd:complexType"
   * </p>
   */
  private static CMConfig[] parseSchema(XMLElement el)
  {

    assertTagName(el, XSD_NS, "schema");

    /*
     * if(el.getChildrenCount() != 1) { throw new
     * XMLException("service/factory schema must contain exacly one xsd.complexType"
     * , el); }
     */

    final List<CMConfig> v = new ArrayList<CMConfig>();

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();

      final AD[] ads = parseComplexType(childEl);
      final Annotation an = loadAnnotationFromAny(childEl);

      String iconURL = childEl.getAttribute(ATTR_ICONURL);
      if ("".equals(iconURL)) {
        iconURL = null;
      }
      final int maxOccurs = getInteger(childEl, ATTR_MAXOCCURS, 1);

      final String name = childEl.getAttribute(ATTR_NAME).toString();

      // System.out.println("load " + name + ", maxOccurs=" + maxOccurs);

      v.add(new CMConfig(name, ads, an != null ? an.doc : "", iconURL,
                         maxOccurs));
    }

    final CMConfig[] r = new CMConfig[v.size()];
    v.toArray(r);
    return r;
  }

  static final String UNBOUNDED = "unbounded";

  static int getInteger(XMLElement el, String attr, int def)
  {
    final String s = el.getAttribute(attr, Integer.toString(def));
    if (UNBOUNDED.equals(s)) {
      return Integer.MAX_VALUE;
    }
    return Integer.parseInt(s);
  }

  /**
   * Parse an XSD complexType info an array of <tt>AttributeDefinition</tt>,
   * defining the metadata for a PID.
   *
   * <p>
   * The name of the complexTyp specifies the metadata PID.
   * </p>
   *
   * <p>
   * The following child elements are supported:
   * <ul>
   * <li><b>xsd:element</b> - defines a singleton definition
   * <li><b>xsd:sequence</b> - defines a vector or array definition
   * </ul>
   *
   * The names of the elements defines the ID's of the definitions. Each ID
   * must only be present once.
   * </p>
   *
   * @throws XMLException
   *           if the <tt>el</tt> tag is not an "xsd:complexType"
   */
  static AD[] parseComplexType(XMLElement el)
  {
    assertTagName(el, XSD_NS, TAG_COMPLEXTYPE);

    final Set<AD> list = new HashSet<AD>();

    @SuppressWarnings("unused")
    Annotation annotation = null;

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();
      if (isName(childEl, XSD_NS, TAG_ANNOTATION)) {
        annotation = loadAnnotation(childEl);
      } else {
        try {
          final AD ad = parseAttributeDefinition(childEl);
          if (list.contains(ad)) {
            throw new XMLException("Multiple definitions of id '" + ad.getID()
                                   + "'", childEl);
          }
          if (ad == null) {
            throw new XMLException("Null ad", childEl);
          }
          list.add(ad);
        } catch (final XMLException ex) {
          System.err.println("Failed in " + el.getFullName() + ", name="
                             + el.getAttribute(ATTR_NAME) + ", line="
                             + el.getLineNr() + ", " + ex);
          throw ex;
        }
      }
    }

    final AD[] ads = new AD[list.size()];

    list.toArray(ads);
    return ads;
  }

  /**
   * Parse an XSD sequence into an <tt>AttributeDefinition</tt> of either vector
   * or array type.
   *
   * <p>
   * Only one child name "element" is allowed, and this child specifies the
   * element type of the vector/array.
   * </p>
   *
   * @throws XMLException
   *           of element is not an "xsd:sequence"
   */
  static AD parseComplexTypeAttr(XMLElement el)
  {
    assertTagName(el, XSD_NS, TAG_COMPLEXTYPE);

    AD attr = null;
    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();

      if (isName(childEl, XSD_NS, TAG_SEQUENCE)) {
        if (attr != null) {
          throw new XMLException("Only one sequence is allowed in complexType",
                                 childEl);
        }
        attr = parseSequence(childEl, el.getAttribute(ATTR_NAME));
      } else if (isName(childEl, XSD_NS, TAG_RESTRICTION)) {
        // System.out.println("skip restriction");
      } else if (isName(childEl, XSD_NS, TAG_ANNOTATION)) {
        // parse later
      }
    }
    if (attr == null) {
      throw new XMLException("No sequence found in complexType", el);
    }

    return addAnnotation(attr, el);

  }

  static AD parseSimpleTypeAttr(XMLElement el)
  {
    assertTagName(el, XSD_NS, TAG_SIMPLETYPE);

    AD attr = null;

    final String id = el.getAttribute(ATTR_NAME).toString();

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();

      if (isName(childEl, XSD_NS, TAG_RESTRICTION)) {

        final int type = getType(childEl);

        final int card = 0;
        final String name = id;
        final String[] defValue = null;

        attr = new AD(id, type, card, name, defValue);
        addEnumeration(childEl, attr);
      } else if (isName(childEl, XSD_NS, TAG_ANNOTATION)) {
        // accept and parse later;
      }
    }

    return addAnnotation(attr, el);
  }

  static void addEnumeration(XMLElement el, AD ad)
  {
    assertTagName(el, XSD_NS, TAG_RESTRICTION);

    final Vector<String[]> v = new Vector<String[]>();
    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();
      // System.out.println(" addEnum " + childEl.getName());
      if (isName(childEl, XSD_NS, TAG_ENUMERATION)) {
        final String val = childEl.getAttribute(ATTR_VALUE);
        if (val == null) {
          throw new XMLException("No value specified in enum", childEl);
        }
        String label = val;
        final Annotation annotation = loadAnnotationFromAny(childEl);
        if (annotation != null && annotation.doc != null) {
          label = annotation.doc;
        }
        v.addElement(new String[] { val, label });
      }
    }

    // System.out.println("optvalues=" + v);

    if (v.size() > 0) {
      final String[] optValues = new String[v.size()];
      final String[] optLabels = new String[v.size()];
      for (int i = 0; i < v.size(); i++) {
        final String[] row = v.elementAt(i);
        optValues[i] = row[0];
        optLabels[i] = row[1];
      }
      ad.setOptions(optValues, optLabels);
    }
  }

  static AD addAnnotation(AD attr, XMLElement el)
  {
    final Annotation a = loadAnnotationFromAny(el);
    if (a != null) {
      if (a.doc != null) {
        attr.setDescription(a.doc);
      }
    }

    int minOccurs = 1;
    try {
      minOccurs = Integer.parseInt(el.getAttribute(ATTR_MINOCCURS, "1"));
    } catch (final Exception e) {
      throw new XMLException("minOccurs must be a valid integer: " + e, el);
    }
    if (minOccurs > 1) {
      throw new XMLException("minOccurs cannot be > 1, is " + minOccurs, el);
    }
    attr.bOptional = minOccurs == 0;

    return attr;
  }

  static Annotation loadAnnotationFromAny(XMLElement el)
  {

    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();
      if (isName(childEl, XSD_NS, TAG_ANNOTATION)) {
        return loadAnnotation(childEl);
      }
    }
    return null;
  }

  static Annotation loadAnnotation(XMLElement el)
  {

    assertTagName(el, XSD_NS, TAG_ANNOTATION);

    Annotation a = null;
    for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement childEl = (XMLElement) e.nextElement();
      if (isName(childEl, XSD_NS, TAG_DOCUMENTATION)) {
        if (a == null) {
          a = new Annotation();
        }
        a.doc = "" + childEl.getContent();
      } else if (isName(childEl, XSD_NS, TAG_APPINFO)) {
        if (a == null) {
          a = new Annotation();
        }
        a.appinfo = "" + childEl.getContent();
      }
    }
    return a;
  }

  static AD parseSequence(XMLElement el, String name)
  {
    // System.out.println("parseSequence " + el.getAttribute(ATTR_NAME));

    assertTagName(el, XSD_NS, TAG_SEQUENCE);

    final boolean bArray =
      "true".equals(el.getAttribute(ATTR_ARRAY, "false").toLowerCase());

    final int maxOccurs = getInteger(el, ATTR_MAXOCCURS, Integer.MAX_VALUE);

    if (el.getChildrenCount() != 1) {
      throw new XMLException(
                             "sequence children count must be " + "exactly one",
                             el);
    } else {
      final String id = name;
      int type = -1;

      for (final Enumeration<?> e = el.enumerateChildren(); e.hasMoreElements();) {
        final XMLElement childEl = (XMLElement) e.nextElement();
        final String childName = childEl.getAttribute(ATTR_NAME).toString();
        int card = -1;

        if (!ITEM.equals(childName)) {
          throw new XMLException("Only '" + ITEM + "'"
                                 + " names are allowed in sequences, found "
                                 + childName, childEl);
        }

        if (bArray) {
          card = maxOccurs;
        } else {
          if (maxOccurs == Integer.MAX_VALUE) {
            card = Integer.MIN_VALUE;
          } else {
            card = -maxOccurs;
          }
        }

        final AD ad = parseAttributeDefinition(childEl);

        type = ad.getType();

        final String[] defValue = null;

        return new AD(id, type, card, id, defValue);
      }

      throw new XMLException("parseSequence failed", el);
    }
  }

  /**
   * Parse an XSD element into an <tt>AttributeDefinition</tt>.
   *
   * <p>
   * The type of the definition is derived using the <tt>getType</tt> method.
   * </p>
   *
   * <p>
   * If the XSD element is a sequence, parse using the <tt>parseSequence</tt>
   * method.
   * </p>
   */
  static AD parseAttributeDefinition(XMLElement el)
  {
    // System.out.println("parseAttributeDefinition " + el.getFullName() +
    // ", name=" + el.getAttribute(ATTR_NAME));

    AD ad = null;

    if (isName(el, XSD_NS, TAG_SIMPLETYPE)) {
      ad = parseSimpleTypeAttr(el);
    } else if (isName(el, XSD_NS, TAG_COMPLEXTYPE)) {
      ad = parseComplexTypeAttr(el);
    } else if (isName(el, XSD_NS, TAG_ELEMENT)) {

      final String id = el.getAttribute(ATTR_NAME);
      if (id == null) {
        throw new XMLException("No id specified in element", el);
      }

      final String strType = el.getAttribute(ATTR_TYPE);
      if (strType == null) {
        throw new XMLException("No type specified in " + id, el);
      }
      final int type = getType(el);

      final int card = 0;
      final String name = id;
      final String[] defValue = null;

      ad = new AD(id, type, card, name, defValue);
    } else if (isName(el, XSD_NS, TAG_ANNOTATION)) {
      //
    } else {
      throw new XMLException("Unsupported tag " + el.getName() + ", ns="
                             + el.getNamespace() + ", name="
                             + el.getAttribute(ATTR_NAME), el);
    }

    return addAnnotation(ad, el);
  }

  /**
   * Get <tt>AttributeDefinition</tt> type from XSD element type.
   *
   * @return One of the <tt>AttributeDefinition.STRING...BOOLEAN</tt> types.
   * @throws XMLException
   *           if the XSD type is unsupported.
   */
  static int getType(XMLElement el)
  {
    int type = -1;

    String strType = null;
    if (isName(el, XSD_NS, TAG_ELEMENT)) {
      strType = el.getAttribute(ATTR_TYPE);
    } else if (isName(el, XSD_NS, TAG_RESTRICTION)) {
      strType = el.getAttribute(ATTR_BASE);
    } else {
      throw new XMLException(
                             "Type is only supported in element and restriction tags",
                             el);
    }

    if (strType == null) {
      throw new XMLException("No type in tag", el);
    }

    if ("xsd:int".equals(strType)) {
      type = AttributeDefinition.INTEGER;
    } else if ("xsd:string".equals(strType)) {
      type = AttributeDefinition.STRING;
    } else if ("xsd:boolean".equals(strType)) {
      type = AttributeDefinition.BOOLEAN;
    } else if ("xsd:float".equals(strType)) {
      type = AttributeDefinition.FLOAT;
    } else if ("xsd:long".equals(strType)) {
      type = AttributeDefinition.LONG;
    } else if ("xsd:short".equals(strType)) {
      type = AttributeDefinition.SHORT;
    } else if ("xsd:char".equals(strType)) {
      type = AttributeDefinition.CHARACTER;
    } else if ("xsd:double".equals(strType)) {
      type = AttributeDefinition.DOUBLE;
    } else {
      throw new XMLException("Unsupported type '" + strType + "'", el);
    }

    return type;
  }

  /**
   * Print sets of definitions to an XML file.
   */
  public static void printMetatypeXML(MetaTypeProvider mtp,
                                      String[] servicePIDs,
                                      String[] factoryPIDs,
                                      boolean bXMLHeader,
                                      boolean bMetatypeTag,
                                      List<?> propList,
                                      PrintWriter out)
  {
    if (bXMLHeader) {
      out.println("<?xml version=\"1.0\"?>");
    }

    if (bMetatypeTag) {
      out.println("<metatype:metatype\n"
                  + "  xmlns:metatype=\"https://www.knopflerfish.org/XMLMetatype\"\n"
                  + "  xmlns:xsd     = \"http://www.w3.org/2001/XMLSchema\">");
    }
    out.println("");

    out.println("   <xsd:schema>\n");
    printOCDXML(mtp, servicePIDs, 1, out);
    out.println("");

    printOCDXML(mtp, factoryPIDs, Integer.MAX_VALUE, out);
    out.println("");

    out.println("   </xsd:schema>\n");

    if (propList != null) {
      printValuesXML(propList, false, out);
    }

    if (bMetatypeTag) {
      out.println("</metatype:metatype>");
    }
  }

  /**
   * Print a set of ObjectClassDefinitions as XML.
   *
   * @param mtp
   *          Metatype provider
   * @param pids
   *          Set of String (PIDs)
   * @param out
   *          writer to print to.
   */
  public static void printOCDXML(MetaTypeProvider mtp,
                                 String[] pids,
                                 int maxOccurs,
                                 PrintWriter out)
  {
    for (final String pid : pids) {
      final ObjectClassDefinition ocd = mtp.getObjectClassDefinition(pid, null);
      if (ocd instanceof OCD) {
        maxOccurs = ((OCD) ocd).maxInstances;
      }
      final AttributeDefinition[] ads =
        ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
      out.println("");
      out.println("    <!-- " + (maxOccurs > 1 ? "Factory " : "Service ") + pid
                  + " -->");
      // out.println("   <xsd:schema>");
      out.print("    <xsd:complexType " + ATTR_NAME + "=\"" + pid + "\"");
      out.print(" "
                + ATTR_MAXOCCURS
                + "=\""
                + (maxOccurs == Integer.MAX_VALUE ? UNBOUNDED : Integer
                    .toString(maxOccurs)) + "\"");

      if (ocd instanceof OCD) {
        final OCD o2 = (OCD) ocd;
        final String urlStr = o2.getIconURL(0);
        if (urlStr != null) {
          out.print(" " + ATTR_ICONURL + "=\"" + urlStr + "\"");
        }
      }
      out.println(">");
      printAnnotation(ocd.getDescription(), "     ", out);
      for (final AttributeDefinition ad : ads) {
        printXML(out, ad);
      }
      out.println("    </xsd:complexType>");
      // out.println("   </xsd:schema>\n");
    }
  }

  static void printXMLSequence(PrintWriter out,
                               AttributeDefinition ad,
                               boolean bArray)
  {
    out.println("     <xsd:complexType " + ATTR_NAME + " = \"" + ad.getID()
                + "\">");
    out.println("      <xsd:sequence " + ATTR_ARRAY + "=\"" + bArray + "\">");
    out.println("       <xsd:element " + ATTR_NAME + " = \"" + ITEM + "\" "
                + ATTR_TYPE + "= \"" + getXSDType(ad.getType()) + "\"/>");
    out.println("      </xsd:sequence>");
    out.println("     </xsd:complexType>");

  }

  /**
   * Print an attribute definition as XML.
   */
  public static void printXML(PrintWriter out, AttributeDefinition ad)
  {
    if (ad.getCardinality() > 0) {
      printXMLSequence(out, ad, false);
    } else if (ad.getCardinality() < 0) {
      printXMLSequence(out, ad, true);
    } else {
      printXMLSingle(out, ad);
    }
  }

  static void printXMLSingle(PrintWriter out, AttributeDefinition ad)
  {

    final String tag = getXSDType(ad.getType());

    final String[] optValues = ad.getOptionValues();
    final String[] optLabels = ad.getOptionLabels();
    final String desc = ad.getDescription();

    if (optValues != null) {
      out.println("      <xsd:simpleType name = \"" + ad.getID() + "\">");
      out.println("       <xsd:restriction base=\"" + tag + "\">");
      for (int i = 0; i < optValues.length; i++) {
        out.println("       <xsd:enumeration value=\"" + optValues[i] + "\">");
        if (optLabels != null) {
          printAnnotation(optLabels[i], "        ", out);
        }
        out.println("       </xsd:enumeration>");
      }
      out.println("       </xsd:restriction>");
      out.println("      </xsd:simpleType>");
    } else {
      if ("".equals(desc)) {
        out.println("     <xsd:element name=\"" + ad.getID() + "\""
                    + " type=\"" + tag + "\"/>");
      } else {
        out.println("     <xsd:element name=\"" + ad.getID() + "\""
                    + " type=\"" + tag + "\">");
        printAnnotation(desc, "      ", out);
        out.println("     </xsd:element>");
      }
    }
  }

  public static void printValuesXML(List<?> propList,
                                    boolean bXMLHeader,
                                    PrintWriter out)
  {

    if (bXMLHeader) {
      out.println("<?xml version=\"1.0\"?>");
    }

    out.println(" <metatype:values\n"
                + "  xmlns:metatype=\"https://www.knopflerfish.org/XMLMetatype\">");
    out.println("");

    for (final Object name : propList) {
      final Dictionary<?, ?> props = (Dictionary<?, ?>) name;
      String pid = (String) props.get(SERVICE_PID);
      if (pid == null) {
        pid = (String) props.get("factory.pid");
      }

      out.println("");
      out.println("  <!-- pid " + pid + " -->");
      out.println("  <" + pid + ">");
      printPropertiesXML(props, out);
      out.println("  </" + pid + ">");
    }

    out.println(" </metatype:values>");
  }

  static void printPropertiesXML(Dictionary<?, ?> props, PrintWriter out)
  {
    for (final Enumeration<?> e = props.keys(); e.hasMoreElements();) {
      final String key = (String) e.nextElement();
      final Object val = props.get(key);

      if (val instanceof Vector) {
        out.println("   <" + key + ">");
        final Vector<?> v = (Vector<?>) val;
        for (int i = 0; i < v.size(); i++) {
          out.println("    <item>" + v.elementAt(i) + "</item>");
        }
        out.println("   </" + key + ">");
      } else if (val.getClass().isArray()) {
        out.println("   <" + key + ">");
        for (int i = 0; i < Array.getLength(val); i++) {
          out.println("    <item>" + Array.get(val, i) + "</item>");
        }
        out.println("   </" + key + ">");
      } else {
        out.println("   <" + key + ">" + val.toString() + "</" + key + ">");
      }
    }
  }

  static String getXSDType(int type)
  {
    switch (type) {
    case AttributeDefinition.STRING:
      return "xsd:string";
    case AttributeDefinition.INTEGER:
      return "xsd:int";
    case AttributeDefinition.LONG:
      return "xsd:long";
    case AttributeDefinition.SHORT:
      return "xsd:short";
    case AttributeDefinition.DOUBLE:
      return "xsd:double";
    case AttributeDefinition.CHARACTER:
      return "xsd:char";
    case AttributeDefinition.FLOAT:
      return "xsd:float";
    case AttributeDefinition.BOOLEAN:
      return "xsd:boolean";
    case AttributeDefinition.BIGINTEGER:
      return "xsd:integer";
    case AttributeDefinition.BIGDECIMAL:
      return "xsd:decimal";
    default:
      throw new IllegalArgumentException("Cannot print " + type);
    }
  }

  static void printAnnotation(String s, String prefix, PrintWriter out)
  {
    out.println(prefix + "<xsd:annotation>");
    out.println(prefix + " <xsd:documentation>" + s + "</xsd:documentation>");
    out.println(prefix + "</xsd:annotation>");
  }

  static void assertTagName(XMLElement el, String name)
  {
    assertTagName(el, null, name);
  }

  static void assertTagName(XMLElement el, String namespace, String name)
  {
    if (!isName(el, namespace, name)) {
      throw new XMLException("Excepted tag '" + namespace + ":" + name
                             + "', found '" + el.getFullName() + "'", el);
    }
  }

  static boolean isName(XMLElement el, String namespace, String name)
  {

    final boolean b =
      el.getName().equals(name)
          && (namespace == null || el.getNamespace() == null || namespace
              .equals(el.getNamespace()));

    return b;
  }

  private static String[] toStringArray(Object[] val)
  {
    final String[] r = new String[val.length];
    for (int i = 0; i < val.length; i++) {
      r[i] = AD.escape(val[i].toString());
    }

    return r;
  }

  private static String[] toStringArray(Vector<?> val)
  {
    final String[] r = new String[val.size()];
    for (int i = 0; i < val.size(); i++) {
      r[i] = AD.escape(val.elementAt(i).toString());
    }

    return r;
  }

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
  private static List<String> currentOptionLabels = new ArrayList<String>();
  private static List<String> currentOptionValues = new ArrayList<String>();

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
        new ServiceTracker<Object, Object>(bc,
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
    InputStream in = null;
    try {
      in = url.openStream();

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
        } else {
          // System.out.println("Got something else");
        }
        try {
          eventType = xpp.next();
        } catch (final java.io.IOException ex) {

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
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (final IOException _ignore) {
        }
      }
    }
  } // method

  // any missing attribute gets the element ignored
  protected static void startElement(String element, URL sourceURL)
      throws Exception
  {
    final int n_attrs = xml_parser.getAttributeCount();
    final HashMap<String, String> attrs = new HashMap<String, String>();
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
        required = Boolean.valueOf(requiredS).booleanValue();
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
        optionalB = Boolean.valueOf(optional).booleanValue();
      } else {
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

          if (merge == null || !Boolean.valueOf(merge).booleanValue()) {
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

      currentAttributes = new ArrayList<AE>();
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

  protected static void endElement(String element, String content)
      throws Exception
  {

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
          List<String> defValues = new ArrayList<String>(Arrays.asList(currentAD.defValue));
          defValues.retainAll(currentOptionValues);
          if (defValues.size()==0) {
            currentAD.defValue = null;
          } else {
            currentAD.defValue = defValues.toArray(new String[defValues.size()]);
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
    int type = -1;

    if ("Integer".equals(strType)) {
      type = AttributeDefinition.INTEGER;
    } else if ("String".equals(strType)) {
      type = AttributeDefinition.STRING;
    } else if ("Boolean".equals(strType)) {
      type = AttributeDefinition.BOOLEAN;
    } else if ("Float".equals(strType)) {
      type = AttributeDefinition.FLOAT;
    } else if ("Long".equals(strType)) {
      type = AttributeDefinition.LONG;
    } else if ("Short".equals(strType)) {
      type = AttributeDefinition.SHORT;
    } else if ("Char".equals(strType)) {
      type = AttributeDefinition.CHARACTER;
    } else if ("Byte".equals(strType)) {
      type = AttributeDefinition.BYTE;
    } else if ("Double".equals(strType)) {
      type = AttributeDefinition.DOUBLE;
    } else if ("Password".equals(strType)) {
      type = AttributeDefinition.PASSWORD;
    } else {
      throw new IllegalArgumentException("Unsupported type '" + strType);
    }

    return type;
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
  public int maxInstances = 1;
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
    final StringBuffer sb = new StringBuffer();

    sb.append("CMConfig[");
    sb.append("pid=" + pid);
    sb.append(", desc=" + desc);
    sb.append(", iconURL=" + iconURL);
    sb.append(", maxInstances=" + maxInstances);
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
