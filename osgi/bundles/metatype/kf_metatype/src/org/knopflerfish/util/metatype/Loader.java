/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;
import net.n3.nanoxml.*;

import java.net.URL;
import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

import org.knopflerfish.util.Text;
/**
 * Helper class which loads (and saves) KF  Metatype XML.
 *
 * <p>
 * This implementaion uses the nanoxml package.
 * </p>
 * <p>
 * NanoXML is distributed under the zlib/libpng license.<br>
 * See <a href="http://nanoxml.sourceforge.net/orig/copyright.html">http://nanoxml.sourceforge.net/orig/copyright.html</a>
 * for details.<br>
 * The full license text is also include in the kf_metatype bundle jar 
 * </p>
 * Nanoxml is Copyrighted 2000-2002 Marc De Scheemaecker, All Rights
 * Reserved.
 * </p>
 */
public class Loader {
  static final String METATYPE      = "metatype";
  static final String SERVICES      = "services";
  static final String FACTORIES     = "factories";
  static final String VALUES        = "values";
  static final String SCHEMA        = "schema";

  static final String SERVICE_PID   = "service.pid";
  static final String FACTORY_PID   = "factory.pid";

  static final String ITEM          = "item";

  static final String ATTR_PID  = "pid";
  static final String ATTR_DECS = "description";

  static final String ATTR_TYPE      = "type";
  static final String ATTR_NAME      = "name";
  static final String ATTR_BASE      = "base";
  static final String ATTR_VALUE     = "value";
  static final String ATTR_ICONURL   = "iconURL";
  static final String ATTR_MINOCCURS = "minOccurs";
  static final String ATTR_MAXOCCURS = "maxOccurs";

  static final String ATTR_ARRAY     = "array";

  static final String XSD_NS       = "http://www.w3.org/2001/XMLSchema";
  static final String METATYPE_NS  = "http://www.knopflerfish.org/XMLMetatype";

  static final String TAG_ANNOTATION    = "annotation";
  static final String TAG_SIMPLETYPE    = "simpleType";
  static final String TAG_COMPLEXTYPE   = "complexType";
  static final String TAG_ELEMENT       = "element";
  static final String TAG_RESTRICTION   = "restriction";
  static final String TAG_ENUMERATION   = "enumeration";

  static final String TAG_DOCUMENTATION    = "documentation";
  static final String TAG_APPINFO          = "appinfo";
  static final String TAG_SEQUENCE         = "sequence";

  static final String BUNDLE_PROTO = "bundle://";

  /**
   * Load a MetaTypeProvider from an XML file.
   */
  public static MTP loadMTPFromURL(Bundle bundle, URL url) throws IOException {
    InputStream in = null;
    
    try {
      in                = url.openStream();
      IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
      IXMLReader reader = new StdXMLReader(in);
      parser.setReader(reader);
      XMLElement el  = (XMLElement) parser.parse();
      return loadMTP(bundle, url.toString(), el);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException("Failed to load " + url + " " + e);
    } finally {
      try { in.close(); } catch (Exception ignored) { }
    }

  }

  /**
   * load defaults from an XML file into an MTP
   */
  public static List loadDefaultsFromURL(MTP mtp, URL url) throws IOException {
    InputStream in = null;
    
    try {
      in                = url.openStream();
      IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
      IXMLReader reader = new StdXMLReader(in);
      parser.setReader(reader);
      XMLElement el  = (XMLElement) parser.parse();

      if(isName(el, METATYPE_NS, VALUES)) {
	List propList = loadValues(mtp, el);
	setDefaultValues(mtp, propList);
	return propList;
      } else {
	for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
	  XMLElement childEl = (XMLElement)e.nextElement();
	  if(isName(childEl, METATYPE_NS, VALUES)) {
	    
	    List propList = loadValues(mtp, childEl);
	    
	    setDefaultValues(mtp, propList);
	    
	    return propList;
	  }
	}
      }
      throw new XMLException("No values tag in " + url, el);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException("Failed to load " + url + " " + e);
    } finally {
      try { in.close(); } catch (Exception ignored) { }
    }
  }

  /**
   * Load a MetaTypeProvider from an XML "config" element.
   *
   * <ol>
   *  <li>Load all service and factory definitions into
   *      a MetaTypeProvider instance.
   *  <li>Load any default data
   *  <li>Insert default data into definitions in MetaTypeProvider
   *      using the <tt>setDefaultValues</tt> method
   * </ol>
   *
   */
  public static MTP loadMTP(Bundle bundle, String sourceName, XMLElement el) {
    
    assertTagName(el, METATYPE_NS, METATYPE);

    CMConfig[] services  = null;
    CMConfig[] factories = null;

    boolean bHasDefValues = false;

    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl = (XMLElement)e.nextElement();
      if(isName(childEl, METATYPE_NS, SERVICES)) {
	services = parseServices(childEl, false);
      } else if(isName(childEl, METATYPE_NS, FACTORIES)) {
	factories = parseServices(childEl, true);
      } else if(isName(childEl, METATYPE_NS, VALUES)) {
	bHasDefValues = true;
      } else if(isName(childEl, XSD_NS, SCHEMA)) {
	CMConfig[] any = parseSchema(childEl);
	List sa = new ArrayList();
	List fa = new ArrayList();
	for(int i = 0; i < any.length; i++) {
	  if(any[i].maxInstances > 1) {
	    fa.add(any[i]);
	  } else {
	    sa.add(any[i]);
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

    MTP mtp = new MTP(sourceName);

    // Insert servcies and factory definition into MTP
    // default values will be default values defined by AD
    for(int i = 0; services != null && i < services.length; i++) {
      OCD ocd = new OCD(services[i].pid, 
			services[i].pid, 
			services[i].desc);
      ocd.maxInstances = 1;
      String iconURL = services[i].iconURL;
      if(iconURL != null) {
	try {
	  if(bundle != null) {
	    if(iconURL.startsWith("/")) {
	      iconURL = BUNDLE_PROTO + "$(BID)" + iconURL;
	    }
	    iconURL = Text.replace(iconURL, "$(BID)", 
				   Long.toString(bundle.getBundleId()));
	  }
	  ocd.setIconURL(iconURL);
	} catch (Exception e) {
	  System.err.println("Failed to set icon url: " +  e);
	}
      }
      for(int j = 0; j < services[i].ads.length; j++) {
	ocd.add(services[i].ads[j], 
		services[i].ads[j].isOptional() 
		? ObjectClassDefinition.OPTIONAL
		: ObjectClassDefinition.REQUIRED);
      }
      mtp.addService(services[i].pid, ocd);
    }
    for(int i = 0; factories != null && i < factories.length; i++) {
      OCD ocd = new OCD(factories[i].pid, 
			factories[i].pid, 
			factories[i].desc);
      ocd.maxInstances = factories[i].maxInstances;
      if(factories[i].iconURL != null) {
	try {
	  ocd.setIconURL(factories[i].iconURL);
	} catch (Exception e) {
	  System.err.println("Failed to set icon url: "+ e);
	}
      }
      for(int j = 0; j < factories[i].ads.length; j++) {
	ocd.add(factories[i].ads[j], ObjectClassDefinition.REQUIRED);
      }
      mtp.addFactory(factories[i].pid, ocd);
    }

      
    // Overwrite MTP default values with values found in
    // DEFAULTVALUES section in source XML
    if(bHasDefValues) {

      for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
	XMLElement childEl = (XMLElement)e.nextElement();
	if(isName(childEl, METATYPE_NS, VALUES)) {
	  List propList = loadValues(mtp, childEl);
	
	  setDefaultValues(mtp, propList);
	}
      }
    }
    
    return mtp;
  }


  /**
   * Overwrite default values in MTP using a set of dictionaries.
   *
   * @param mtp MetaTypeProvider containing instances of <tt>AD</tt>
   * @param propList List of Dictionary
   */
  public static void setDefaultValues(MetaTypeProvider mtp, 
				      List propList) {

    for(Iterator it = propList.iterator(); it.hasNext();) {
      Dictionary            props   = (Dictionary)it.next();
      String                pid     = (String)props.get(SERVICE_PID);
      if(pid == null) {
	pid     = (String)props.get("factory.pid");
      }

      ObjectClassDefinition ocd     = null;
      try {
	ocd = mtp.getObjectClassDefinition(pid, null);
      } catch (Exception ignored) {
      }

      if(ocd == null) {
	throw new IllegalArgumentException("No definition for pid '" + pid + "'");
      } else {
	AttributeDefinition[] ads = 
	  ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);

	for(int i = 0; ads != null && i < ads.length; i++) {
	  Object val =  props.get(ads[i].getID());

	  if(!(ads[i] instanceof AD)) {
	    throw new IllegalArgumentException("AttributeDefinitions must be instances of AD, otherwise default values cannot be set");
	  }

	  AD ad = (AD)ads[i];

	  if(val instanceof Vector) {
	    ad.setDefaultValue(toStringArray((Vector)val));
	  } else if(val.getClass().isArray()) {
	    ad.setDefaultValue(toStringArray((Object[])val));
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
  public static List loadValues(MetaTypeProvider mtp, XMLElement el) {

    //    assertTagName(el, DEFAULTVALUES);

    List propList = new ArrayList();

    // String (pid) -> Integer (count)
    Map  countMap = new HashMap();

    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement            childEl = (XMLElement)e.nextElement();
      String                pid     = childEl.getName();
      ObjectClassDefinition ocd     = null;
      

      
      try {
	ocd = mtp.getObjectClassDefinition(pid, null);
      } catch (Exception ignored) {
      }
      if(ocd == null) {
	throw new XMLException("Undefined pid '" + pid + "'", childEl);
      }
      Dictionary props = 
	loadValues(ocd.getAttributeDefinitions(ObjectClassDefinition.ALL), 
		   childEl);
      int maxInstances = 1;
      if(ocd instanceof OCD) {
	maxInstances = ((OCD)ocd).maxInstances;
      }

      Integer count = (Integer)countMap.get(pid);
      if(count == null) {
	count = new Integer(0);
      }
      count = new Integer(count.intValue() + 1);
      if(count.intValue() > maxInstances) {
	throw new XMLException("PID " + pid + " can only have " +
			       maxInstances + " instance(s), found " + 
			       count, el);
      }

      countMap.put(pid, count);
      
      props.put(maxInstances > 1 ? "factory.pid" : SERVICE_PID, pid);
      propList.add(props);
    }

    return propList;
  }

  public static Dictionary loadValues(AttributeDefinition[] attrs, 
				      XMLElement el) {

    if(attrs == null) {
      throw new NullPointerException("attrs array cannot be null");
    }

    Hashtable props = new Hashtable();
    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement            childEl = (XMLElement)e.nextElement();
      String                id      = childEl.getFullName();

      AttributeDefinition attr = null;

      //      System.out.println("load id=" + id);

      for(int i = 0; attr == null && i < attrs.length; i++) {
	//	System.out.println(i + ": " + attrs[i]);
	if(id.equals(attrs[i].getID())) {
	  attr = attrs[i];
	}
      }
      if(attr == null) {
	throw new XMLException("Undefined id '" + id + "'", childEl);
      }
      Object val = loadValue(attr, childEl);
      props.put(id, val);
    }

    // Verify that all attributes are found
    for(int i = 0; i < attrs.length; i++) {
      if(!props.containsKey(attrs[i].getID())) {
	throw new XMLException("Missing attribute id '" + attrs[i].getID() + "'", 
			       el);
      }
    }
    return props;
  }


  /**
   * Load a java object from an XML element using type info in the 
   * specified definition.
   *
   */
  public static Object loadValue(AttributeDefinition attr, XMLElement el) {
    assertTagName(el, attr.getID());

    if(attr.getCardinality() < 0) {
      return loadSequence(attr, el, -attr.getCardinality(), ITEM);
    }

    if(attr.getCardinality() > 0) {
      Vector v = loadSequence(attr, el, -attr.getCardinality(), ITEM);
      Object[] array = 
	(Object[])Array.newInstance(AD.getClass(attr.getType()), v.size());
      v.copyInto(array);

      return array;
    }

    return loadContent(attr, el);
  }

  public static Vector loadSequence(AttributeDefinition attr, 
				    XMLElement el,
				    int max,
				    String tagName) {
    Vector v = new Vector();

    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement            childEl = (XMLElement)e.nextElement();

      assertTagName(childEl, tagName);
      
      v.addElement(loadContent(attr, childEl));
    }

    return v;
  }

  /**
   * Load the contents of a tag into a java object.
   *
   * @param el   element which content should be converted to a java object.
   * @param attr definition defining type.
   */
  public static Object loadContent(AttributeDefinition attr, XMLElement el) {
    String content = el.getContent();

    if(content == null) {
      content = "";
    }
    
    content = content.trim();

    String msg = attr.validate(content);
    if(msg != null && !"".equals(msg)) {
      throw new XMLException("Validation error: '" + msg + "'", el);
    }


    return AD.parseSingle(content, attr.getType());
  }

  /**
   * Parse a services or factories tag info an array of wrapper
   * objects.
   *
   *<p>
   * Children to this tag must all be "xsd:schema"
   *</p>
   */
  static CMConfig[] parseServices(XMLElement el, boolean bFactory) {

    assertTagName(el, METATYPE_NS, bFactory ? FACTORIES : SERVICES);

    List list = new ArrayList();

    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl = (XMLElement)e.nextElement();

      CMConfig[] conf = parseSchema(childEl);
      if(conf.length == 0) {
	throw new XMLException("No lements in schema", childEl);
      }
      conf[0].maxInstances = bFactory ? Integer.MAX_VALUE : 1;
      
      list.add(conf[0]);
    }

    CMConfig[] ads = new CMConfig[list.size()];

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
  private static CMConfig[] parseSchema(XMLElement el) {

    assertTagName(el, XSD_NS, "schema"); 

    /*
    if(el.getChildrenCount() != 1) {
      throw new XMLException("service/factory schema must contain exacly one xsd.complexType", el);
    }
    */

    List v = new ArrayList();

    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl = (XMLElement)e.nextElement();
      
      AD[]      ads = parseComplexType(childEl);
      Annotation an = loadAnnotationFromAny(childEl);
      
      String iconURL = childEl.getAttribute(ATTR_ICONURL);
      if("".equals(iconURL)) {
	iconURL = null;
      }
      int maxOccurs = getInteger(childEl, ATTR_MAXOCCURS, 1);
      
      String name = childEl.getAttribute(ATTR_NAME).toString();
      
      //      System.out.println("load " +  name + ", maxOccurs=" + maxOccurs);

      v.add(new CMConfig(name,
			 ads,
			 an != null ? an.doc : "",
			 iconURL,
			 maxOccurs));
    }

    CMConfig[] r = new CMConfig[v.size()];
    v.toArray(r);
    return r;
  }

  static final String UNBOUNDED = "unbounded";

  static int getInteger(XMLElement el, String attr, int def) {
    String s = el.getAttribute(attr, Integer.toString(def));
    if(UNBOUNDED.equals(s)) {
      return Integer.MAX_VALUE;
    }
    return Integer.parseInt(s);
  }


  /**
   * Parse an XSD complexType info an array of <tt>AttributeDefinition</tt>,
   * definining the metadata for a PID.
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
   * The names of the elements definines the ID's of the definitions. Each
   * ID must only be present once.
   * </p>
   *
   * @throws XMLException if the <tt>el</tt> tag is not an "xsd:complexType"
   */
  static AD[] parseComplexType(XMLElement el) {
    assertTagName(el, XSD_NS, TAG_COMPLEXTYPE);

    Set list = new HashSet();

    Annotation annotation = null;

    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl = (XMLElement)e.nextElement();
      if(isName(childEl, XSD_NS, TAG_ANNOTATION)) {
	annotation = loadAnnotation(childEl);
      } else {
	try {
	  AD ad = parseAttributeDefinition(childEl);
	  if(list.contains(ad)) {
	    throw new XMLException("Multiple definitions of id '" + ad.getID() + 
				   "'", childEl);
	  }
	  if(ad == null) {
	    throw new XMLException("Null ad", childEl);
	  }
	  list.add(ad);
	} catch (XMLException ex) {
	  System.err.println("Failed in " + el.getFullName() + 
			     ", name=" + el.getAttribute(ATTR_NAME) + 
			     ", line=" + el.getLineNr() + ", " + ex);
	  throw ex;
	}
      }
    }
    
    AD[] ads = new AD[list.size()];
    
    list.toArray(ads);
    return ads;
  }
  
  /**
   * Parse an XSD sequence into an <tt>AttributeDefinition</tt> of either
   * vector or array type.
   *
   * <p>
   * Only one child name "element" is allowed, and this child specifies 
   * the element type of the vector/array.
   * </p>
   *
   * @throws XMLException of element is not an "xsd:sequence"
   */
  static AD parseComplexTypeAttr(XMLElement el) {
    assertTagName(el, XSD_NS, TAG_COMPLEXTYPE);

    AD attr = null;
    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl   = (XMLElement)e.nextElement();

      if(isName(childEl, XSD_NS, TAG_SEQUENCE)) {
	if(attr != null) {
	  throw new XMLException("Only one sequence is allowed in complexType", 
				 childEl);
	}
	attr = parseSequence(childEl, el.getAttribute(ATTR_NAME));
      } else if(isName(childEl, XSD_NS, TAG_RESTRICTION)) {
	//System.out.println("skip restriction");
      } else if(isName(childEl, XSD_NS, TAG_ANNOTATION)) {
	// parse later
      }
    }
    if(attr == null) {
      throw new XMLException("No sequence found in complexType", el);
    }

    return addAnnotation(attr, el);
    
  }

  static AD parseSimpleTypeAttr(XMLElement el) {
    assertTagName(el, XSD_NS, TAG_SIMPLETYPE);

    AD attr = null;

    String id      = el.getAttribute(ATTR_NAME).toString();

    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl   = (XMLElement)e.nextElement();

      if(isName(childEl, XSD_NS, TAG_RESTRICTION)) {

	int type       = getType(childEl);
		
	int      card     = 0;
	String   name     = id;
	String[] defValue = null;
	
	attr = new AD(id, type, card, name, defValue);
	addEnumeration(childEl, attr);
      } else if(isName(childEl, XSD_NS, TAG_ANNOTATION)) {
	// accept and parse later;
      }
    }

    return addAnnotation(attr, el);
  }

  static void addEnumeration(XMLElement el, AD ad) {
    assertTagName(el, XSD_NS, TAG_RESTRICTION);

    Vector v = new Vector();
    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl   = (XMLElement)e.nextElement();
      //      System.out.println(" addEnum " + childEl.getName());
      if(isName(childEl, XSD_NS, TAG_ENUMERATION)) {
	String val = childEl.getAttribute(ATTR_VALUE);
	if(val == null) {
	  throw new XMLException("No value specified in enum", childEl);
	}
	String label = val;
	Annotation annotation = loadAnnotationFromAny(childEl);
	if(annotation != null && annotation.doc != null) {
	  label = annotation.doc;
	}
	v.addElement(new String[] { val, label });
      }
    }
    
    //    System.out.println("optvalues=" + v);

    if(v.size() > 0) {
      String[] optValues = new String[v.size()];
      String[] optLabels = new String[v.size()];
      for(int i = 0; i < v.size(); i++) {
	String[] row = (String[])v.elementAt(i);
	optValues[i] = row[0];
	optLabels[i] = row[1];
      }
      ad.setOptions(optValues, optLabels);
    }
  }
  static AD addAnnotation(AD attr, 
			  XMLElement el) {
    Annotation a = loadAnnotationFromAny(el);
    if(a != null) {
      if(a.doc != null) {
	attr.setDescription(a.doc);
      }
    }

    int minOccurs = 1;
    try {
      minOccurs = Integer.parseInt(el.getAttribute(ATTR_MINOCCURS, "1"));
    } catch (Exception e) {
      throw new XMLException("minOccurs must be a valid integer: " + e, el);
    }
    if(minOccurs > 1) {
      throw new XMLException("minOccurs cannot be > 1, is " + minOccurs, el);
    }
    attr.bOptional = minOccurs == 0;

    return attr;
  }

  static Annotation loadAnnotationFromAny(XMLElement el) {
    
    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl   = (XMLElement)e.nextElement();
      if(isName(childEl, XSD_NS, TAG_ANNOTATION)) {
	return loadAnnotation(childEl);
      }
    }
    return null;
  }

  static Annotation loadAnnotation(XMLElement el) {

    assertTagName(el, XSD_NS, TAG_ANNOTATION);

    Annotation a = null;
    for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
      XMLElement childEl   = (XMLElement)e.nextElement();
      if(isName(childEl, XSD_NS, TAG_DOCUMENTATION)) {
	if(a == null) { a = new Annotation(); }
	a.doc = "" + childEl.getContent();
      } else if(isName(childEl, XSD_NS, TAG_APPINFO)) {
	if(a == null) { a = new Annotation(); }
	a.appinfo = "" + childEl.getContent();
      }
    }
    return a;
  }

  static AD parseSequence(XMLElement el, String name) {
    //    System.out.println("parseSequence " + el.getAttribute(ATTR_NAME));

    assertTagName(el, XSD_NS, TAG_SEQUENCE);


    boolean    bArray    = 
      "true".equals(el.getAttribute(ATTR_ARRAY, "false").toLowerCase());
    
    
    int maxOccurs = getInteger(el, ATTR_MAXOCCURS, Integer.MAX_VALUE);

    if(el.getChildrenCount() != 1) {
      throw new XMLException("sequence children count must be " +
					 "exactly one", el);
    } else {
      String id      = name;
      int type = -1;
      
      for(Enumeration e = el.enumerateChildren(); e.hasMoreElements(); ) {
	XMLElement childEl   = (XMLElement)e.nextElement();
	String     childName = childEl.getAttribute(ATTR_NAME).toString();
	int        card      = -1;
	
	
	
	if(!ITEM.equals(childName)) {
	  throw new XMLException("Only '" + ITEM + "'" + 
				 " names are allowed in sequences, found " + 
				 childName, childEl);
	}
	
	if(bArray) {
	  card = maxOccurs;
	} else {
	  if(maxOccurs == Integer.MAX_VALUE) {
	    card = Integer.MIN_VALUE;
	  } else {
	    card = -maxOccurs;
	  }
	}
	
	AD ad = parseAttributeDefinition(childEl);

	type = ad.getType();

	String[] defValue = null;

	return new AD(id, type, card, id, defValue);	
      }
      
      throw new XMLException("parseSequence failed", el);
    }
  }
  
  
  /**
   * Parse an XSD element into an <tt>AttributeDefinition</tt>
   *
   * <p>
   * The type of the definition is derived using the <tt>getType</tt>
   * method.
   * </p>
   * <p>
   * If the XSD element is a sequence, parse using the <tt>parseSequence</tt>
   * method.
   * </p>
   */
  static AD parseAttributeDefinition(XMLElement el) {
    // System.out.println("parseAttributeDefinition " +   el.getFullName() + ", name=" + el.getAttribute(ATTR_NAME));
    
    AD ad = null;

    if(isName(el, XSD_NS, TAG_SIMPLETYPE)) {
      ad = parseSimpleTypeAttr(el);
    } else if(isName(el, XSD_NS, TAG_COMPLEXTYPE)) {
      ad = parseComplexTypeAttr(el);
    } else if(isName(el, XSD_NS, TAG_ELEMENT)) {
      
      String id      = el.getAttribute(ATTR_NAME);
      if(id == null) {
	throw new XMLException("No id specified in element", el);
      }

      String strType = el.getAttribute(ATTR_TYPE);
      if(strType == null) {
	throw new XMLException("No type specified in " + id, el);
      }
      int type       = getType(el);
      
      
      int      card     = 0;
      String   name     = id;
      String[] defValue = null;
      
      ad = new AD(id, type, card, name, defValue);
    } else if(isName(el, XSD_NS, TAG_ANNOTATION)) {
      //
    } else {
      throw new XMLException("Unsupported tag " + el.getName() +  
			     ", ns=" + el.getNamespace() + 
			     ", name=" + el.getAttribute(ATTR_NAME), el);
    }


    return addAnnotation(ad, el);
  }

  /**
   * Get <tt>AttributeDefinition</tt> type from XSD element type.
   *
   * @return One of the <tt>AttributeDefinition.STRING...BOOLEAN</tt> types.
   * @throws XMLException if the XSD type is unsupported.
   */
  static int getType(XMLElement el) {
    int type = -1;

    String strType = null;
    if(isName(el, XSD_NS, TAG_ELEMENT)) {
      strType = el.getAttribute(ATTR_TYPE);
    } else if(isName(el, XSD_NS, TAG_RESTRICTION)) {
      strType = el.getAttribute(ATTR_BASE);
    } else {
      throw new XMLException("Type is only supported in element and restriction tags", el);
    }
    
    if(strType == null) {
      throw new XMLException("No type in tag", el);
    }


    if("xsd:int".equals(strType)) {
      type = AttributeDefinition.INTEGER;
    } else if("xsd:string".equals(strType)) {
      type = AttributeDefinition.STRING;
    } else if("xsd:boolean".equals(strType)) {
      type = AttributeDefinition.BOOLEAN;
    } else if("xsd:float".equals(strType)) {
      type = AttributeDefinition.FLOAT;
    } else if("xsd:long".equals(strType)) {
      type = AttributeDefinition.LONG;
    } else if("xsd:short".equals(strType)) {
      type = AttributeDefinition.SHORT;
    } else if("xsd:char".equals(strType)) {
      type = AttributeDefinition.CHARACTER;
    } else if("xsd:double".equals(strType)) {
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
				      List propList,
				      PrintWriter out) {
    if(bXMLHeader) {
      out.println("<?xml version=\"1.0\"?>");
    }

    if(bMetatypeTag) {
      out.println("<metatype:metatype\n" + 
		  "  xmlns:metatype=\"http://www.knopflerfish.org/XMLMetatype\"\n" + 
		  "  xmlns:xsd     = \"http://www.w3.org/2001/XMLSchema\">");    
    }
    out.println("");

    out.println("   <xsd:schema>\n");
    printOCDXML(mtp, servicePIDs, 1, out);
    out.println("");

    printOCDXML(mtp, factoryPIDs, Integer.MAX_VALUE, out);
    out.println("");

    out.println("   </xsd:schema>\n");

    if(propList != null) {
      printValuesXML(propList, false, out);
    }

    if(bMetatypeTag) {
      out.println("</metatype:metatype>");
    }  
  }

  /**
   * Print a set of ObjectClassDefinitions as XML.
   *
   * @param mtp Metatype provider
   * @param pids Set of String (PIDs)
   * @param out writer to print to.
   */
  public static void printOCDXML(MetaTypeProvider mtp, 
				 String[]         pids, 
				 int              maxOccurs,
				 PrintWriter      out) {
    for(int i = 0; i < pids.length; i++) {
      String pid = pids[i];
      ObjectClassDefinition ocd = mtp.getObjectClassDefinition(pid, null);
      if(ocd instanceof OCD) {
	maxOccurs = ((OCD)ocd).maxInstances;
      }
      AttributeDefinition[] ads = 
	ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
      out.println("");
      out.println("    <!-- " + 
		  (maxOccurs > 1 ? "Factory " : "Service ") + 
		  pid + " -->");
      //      out.println("   <xsd:schema>");
      out.print ("    <xsd:complexType " + ATTR_NAME + "=\"" + pid + "\"");
      out.print (" " + ATTR_MAXOCCURS + "=\"" + 
		 (maxOccurs == Integer.MAX_VALUE 
		  ? UNBOUNDED 
		  : Integer.toString(maxOccurs)) 
		 + "\"");
      
      if(ocd instanceof OCD) {
	OCD o2 = (OCD)ocd;
	String urlStr = o2.getIconURL();
	if(urlStr != null) {
	  if(urlStr.startsWith(BUNDLE_PROTO)) {
	    int ix = urlStr.indexOf("/", BUNDLE_PROTO.length());
	    if(ix != -1) {
	      urlStr = urlStr.substring(ix);
	    }
	  }
	  out.print(" " + ATTR_ICONURL + "=\"" + urlStr + "\"");
	}
      }
      out.println(">");
      printAnnotation(ocd.getDescription(), "     ", out);
      for(int j = 0; j < ads.length; j++) {
	printXML(out, ads[j]);
      }
      out.println("    </xsd:complexType>");
      //      out.println("   </xsd:schema>\n");
    }
  }


  static void printXMLSequence(PrintWriter out, 
			       AttributeDefinition ad,
			       boolean bArray) {
    out.println("     <xsd:complexType " + ATTR_NAME + " = \"" + ad.getID() + "\">");
    out.println("      <xsd:sequence " + ATTR_ARRAY + "=\"" + bArray + "\">");
    out.println("       <xsd:element " + ATTR_NAME + " = \"" + ITEM + 
		"\" " + ATTR_TYPE + "= \"" + 
		getXSDType(ad.getType()) + "\"/>");
    out.println("      </xsd:sequence>");
    out.println("     </xsd:complexType>");

  }
  
  /**
   * Print an attribute definition as XML.
   */
  public static void printXML(PrintWriter out,
			      AttributeDefinition ad) {
    if(ad.getCardinality() > 0) {
      printXMLSequence(out, ad, false);
    } else if(ad.getCardinality() < 0) {
      printXMLSequence(out, ad, true);
    } else {
      printXMLSingle(out, ad);
    }
  }

  static void printXMLSingle(PrintWriter out,
			     AttributeDefinition ad) {
    
    String tag = getXSDType(ad.getType());
    
    String[] optValues = ad.getOptionValues();
    String[] optLabels = ad.getOptionLabels();
    String   desc = ad.getDescription();

    if(optValues != null) {
      out.println("      <xsd:simpleType name = \"" + ad.getID() + "\">");
      out.println("       <xsd:restriction base=\"" + tag + "\">");
      for(int i = 0; i < optValues.length; i++) {
	out.println("       <xsd:enumeration value=\"" + optValues[i] + "\">");
	if(optLabels != null) {
	  printAnnotation(optLabels[i], "        ", out);
	}
	out.println("       </xsd:enumeration>");
      }
      out.println("       </xsd:restriction>");
      out.println("      </xsd:simpleType>");
    } else {
      if("".equals(desc)) {
	out.println("     <xsd:element name=\"" + ad.getID() + "\"" +
		    " type=\"" + tag + "\"/>");
      } else {
	out.println("     <xsd:element name=\"" + ad.getID() + "\"" + 
		    " type=\"" + tag + "\">");
	printAnnotation(desc, "      ", out);
	out.println("     </xsd:element>");
      }
    }
  }

  public static void printValuesXML(List propList,
				    boolean bXMLHeader,
				    PrintWriter out) {

    if(bXMLHeader) {
      out.println("<?xml version=\"1.0\"?>");
    }

    out.println(" <metatype:values\n" + 	"  xmlns:metatype=\"http://www.knopflerfish.org/XMLMetatype\">" );
    out.println("");
    
    for(Iterator it = propList.iterator(); it.hasNext();) {
      Dictionary            props   = (Dictionary)it.next();
      String                pid     = (String)props.get(SERVICE_PID);
      if(pid == null) {
	pid     = (String)props.get("factory.pid");
      }

      out.println("");
      out.println("  <!-- pid " + pid + " -->");
      out.println("  <" + pid + ">");
      printPropertiesXML(props, out);
      out.println("  </" + pid + ">");
    }

    out.println(" </metatype:values>");
  }

  static void printPropertiesXML(Dictionary props, PrintWriter out) {
    for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      Object val = props.get(key);
      
      if(val instanceof Vector) {
	out.println("   <" + key + ">");
	Vector v = (Vector)val;
	for(int i = 0; i < v.size(); i++) {
	  out.println("    <item>" + v.elementAt(i) + "</item>");
	}
	out.println("   </" + key + ">");
      } else if(val.getClass().isArray()) {
	out.println("   <" + key + ">");
	for(int i = 0; i < Array.getLength(val); i++) {
	  out.println("    <item>" + Array.get(val, i) + "</item>");
	}
	out.println("   </" + key + ">");
      } else {
	out.println("   <" + key + ">" + val.toString() + "</" + key + ">");
      }
    }
  }


  static String getXSDType(int type) {
    String tag = "";
    switch(type) {
    case AttributeDefinition.STRING:     return "xsd:string";
    case AttributeDefinition.INTEGER:    return "xsd:int";   
    case AttributeDefinition.LONG:       return "xsd:long";  
    case AttributeDefinition.SHORT:      return "xsd:short"; 
    case AttributeDefinition.DOUBLE:     return "xsd:double";
    case AttributeDefinition.CHARACTER:  return "xsd:char";
    case AttributeDefinition.FLOAT:      return "xsd:float"; 
    case AttributeDefinition.BOOLEAN:    return "xsd:boolean"; 
    case AttributeDefinition.BIGINTEGER: return "xsd:integer"; 
    case AttributeDefinition.BIGDECIMAL: return "xsd:decimal"; 
    default: throw new IllegalArgumentException("Cannot print " + type);
    }
  }

  static void printAnnotation(String s, String prefix, PrintWriter out) {
    out.println(prefix + "<xsd:annotation>");
    out.println(prefix + " <xsd:documentation>" + s + "</xsd:documentation>");
    out.println(prefix + "</xsd:annotation>");
  }

  static void assertTagName(XMLElement el, 
			    String name) {
    assertTagName(el, null, name);
  }

  static void assertTagName(XMLElement el, 
			    String namespace, 
			    String name) {
    if(!isName(el, namespace, name)) {
      throw new XMLException("Excepted tag '" + namespace + ":" + name + 
			     "', found '" + el.getFullName() + "'", el);
    }
  }


  static boolean isName(XMLElement el, 
			String namespace, 
			String name) {

    boolean b =  el.getName().equals(name) && 
      (namespace == null || 
       el.getNamespace() == null ||
       namespace.equals(el.getNamespace()));
    
    return b;
  }

  private static String[] toStringArray(Object[] val) {
    String[] r = new String[val.length];
    for(int i = 0; i < val.length; i++) {
      r[i] = AD.escape(val[i].toString());
    }

    return r;
  }

  private static String[] toStringArray(Vector val) {
    String[] r = new String[val.size()];
    for(int i = 0; i < val.size(); i++) {
      r[i] = AD.escape(val.elementAt(i).toString());
    }

    return r;
  }

}

class XMLException extends IllegalArgumentException {
  XMLElement el;

  private XMLException() {
  }

  public XMLException(XMLElement el) {
    this("", el);
  }

  public XMLException(String msg, XMLElement el) {
    super(msg + ", line=" + el.getLineNr());
    this.el = el;
  }

  public XMLElement getXMLElement() {
    return el;
  }
}


class CMConfig {
  public String  pid;
  public int     maxInstances = 1;
  public AD[]    ads;
  public String  desc;
  public String  iconURL;

  public CMConfig(String pid,
		  AD[] ads, 
		  String desc,
		  String iconURL,
		  int maxInstances) {
    this.pid          = pid;
    this.ads          = ads;
    this.desc         = desc != null ? desc : "";
    this.iconURL      = iconURL;
    this.maxInstances = maxInstances;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("CMConfig[");
    sb.append("pid=" + pid);
    sb.append(", desc=" + desc);
    sb.append(", iconURL=" + iconURL);
    sb.append(", maxInstances=" + maxInstances);
    sb.append(", attribs=");
    for(int i = 0; i < ads.length; i++) {
      sb.append(ads[i]);
      if(i < ads.length - 1) {
	sb.append(", ");
      }
    }
    
    return sb.toString();
  }
}

class Annotation {
  String appinfo;
  String doc;

  public Annotation() {
  }
}
