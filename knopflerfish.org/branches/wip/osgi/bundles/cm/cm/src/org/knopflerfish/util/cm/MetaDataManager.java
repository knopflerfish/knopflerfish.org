/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.util.cm;

import com.sun.xml.parser.*;
//import org.xml.sax.*;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.DocumentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserFactory;
import org.w3c.dom.*;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;
import java.net.*;
import java.util.*;
import java.math.BigInteger;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

public class MetaDataManager
{

  // tags
  public final static String CFG = "cfg";
  public final static String MANAGED_SERVICE = "managedService";
  public final static String MANAGED_SERVICE_FACTORY = "managedServiceFactory";
  public final static String PROPERTY = "property";
  public final static String DESCRIPTION = "description";
  public final static String VALUE = "value";
  public final static String PRIMITIVE = "primitiveValue";
  public final static String HELPTEXT = "helptext";
  public final static String VECTOR = "vector";
  public final static String ARRAY = "array";

  // attributes
  public final static String VERSION = "version";
  public final static String PID = "pid";
  public final static String DESCR = "descr";
  public final static String NAME = "name";
  public final static String TYPE = "type";
  public final static String DEFAULT = "default";
  public final static String LENGTH = "length";


  // constants
  public final String BUNDLE_CONFIG = "Bundle-Config";
  
  // dtd stuff
  public static final String CFG_1_0_DTD_ID = "-//knopflerfish//DTD cfg 1.0//EN";
  public static final String CFG_1_0_DTD_URI = "config.dtd";

  // BigDecimal if available
  private static Class classBigDecimal;
  private static Constructor consBigDecimal;
  static {
    try {
      classBigDecimal = Class.forName("java.math.BigDecimal");
      consBigDecimal = classBigDecimal.getConstructor(new Class [] { String.class });
    } catch (Exception ignore) {
      classBigDecimal = null;
      consBigDecimal = null;
    }
  }

  private Hashtable metadata;
  private Parser parser;
  private Locator locator;


  public MetaDataManager() {
    metadata = new Hashtable();
    try {
      parser = ParserFactory.makeParser();
    } catch (NullPointerException e) {
      try {
        parser = new com.sun.xml.parser.Parser();
      } catch (Exception e1) {
        throw new RuntimeException( "Failed to create XML parser." +e1);
      }
    } catch (Exception e2) {
      throw new RuntimeException( "Failed to create XML parser." +e2);
    }
    parser.setEntityResolver( new CmResolver() );
  }

  /** This open method returns an input stream that works around the
   ** problem that closing the input stream returned by
   ** URL.openStream() on a <code>jar:file:xxx!/sss</code> URL does
   ** not close the underlaying jar file.
   **/
  public static InputStream getInputStream(URL url) throws IOException {
    if ("jar".equals(url.getProtocol())) {
      String spec = url.getFile();
      int separator = spec.indexOf('!');
      if (separator==-1) {
        throw new MalformedURLException
          ("no ! found in 'jar:' url spec: " + spec);
      }
      final String archiveURL = spec.substring(0,separator++);
      if (archiveURL.startsWith("file:")    // Local archive
          && (++separator != spec.length()) // Entry specified
          ) {
        final String   entryName = spec.substring( separator, spec.length() );
        final ZipFile  zf        = new ZipFile( archiveURL.substring(5) );
        final ZipEntry ze        = zf.getEntry( entryName );
        if (ze==null) throw new IOException
          ("No entry named '" +entryName +"' in " +archiveURL );
        return new ZipFileInputStream( zf, zf.getInputStream( ze ) );
      }
    }
    return url.openStream();
  }
   

  /** Returns true if the given URL points to an xml configuration
   * that can be handled by this class.*/
  public boolean isMetaData( String url)
    throws java.io.IOException, org.xml.sax.SAXException
  {
    return isMetaData( new URL(url) );
  }
  

  /** Returns true if the given URL points to an xml configuration
   * that can be handled by this class.*/
  public boolean isMetaData( URL url)
    throws java.io.IOException, org.xml.sax.SAXException
  {
    // Brute force approach to save some CPU cyles (XML-parsing is slow).
    InputStream    is = getInputStream(url);
    BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
    try {
      // Read max 10 lines looking for "<cfg ".
      String line = br.readLine();
      for ( int i=0; i<10&&line!=null; i++) {
        if (line.indexOf("<cfg ")>-1) return true;
        // Check for old style GDKconf xml format
        if (line.indexOf("<configuration>")>-1) return false;
        line = br.readLine();
      }
    } finally {
      try { is.close(); } catch (IOException _ioe) {}
    }

    // Do it the safe but slow way.
    IsMetaDataHandler imdh = new IsMetaDataHandler();
    parser.setDocumentHandler( imdh );
    is = getInputStream(url);
    try {
      InputSource in = new InputSource(is);
      // The parser requires a system id on the input source in the case
      // of a relative system id in the doctype of the parsed XML file.
      // A dummy value is ok since we allways map the public ID to an
      // internal system UIR.
      in.setSystemId( "dummy" );
      in.setEncoding("UTF-8");// Default encoding to UTF-8 and not UTF8
      parser.parse( in );
    } finally {
      try { is.close(); } catch (IOException _ioe) {}
    }
    return imdh.isMetaData();
  }

  public void addMetaData(InputStream is) throws Exception {
    InputSource in = new InputSource(is);
    // The parser requires a system id on the input source in the case
    // of a relative system id in the doctype of the parsed XML file.
    // A dummy value is ok since we allways map the public ID to an
    // internal system UIR.
    in.setSystemId( "dummy" );
    in.setEncoding("UTF-8");// Default encoding to UTF-8 and not UTF8

    parser.setDocumentHandler(new MetaDataHandler());
    parser.parse(in);
  }

  /**
   * As <code>addMetaData</code>, but throws an exception when the root
   * element is not "cfg".
   * <p>
   * Note: I think this should be added to addMetaData, but I don't
   * know if this will cause unexpected behavior in other places. /EW
   * </p>
   * @deprecated as of GDSP 3.2, use <code>addMetaData(InputStream)</code>.
   */
  public void addMetaDataWithCheck(InputStream is) throws Exception {
    addMetaData(is);
  }

  /** Adds metadata from a bundle.
   ** @param f the bundle to add meta data for.
   **/
  public void addMetaData(File f) throws Exception {
    if(!f.exists() || !f.isFile()) {
      throw new Exception("Could not read meta data: File Doesn't exist.");
    }
    JarInputStream jis = new JarInputStream(new FileInputStream(f));
    Manifest mf = jis.getManifest();
    Attributes a = mf.getMainAttributes();
    String cfgxml = a.getValue(BUNDLE_CONFIG);

    if(jis != null) {
      jis.close();
    }

    if(cfgxml != null) {
      ZipFile zf = new ZipFile(f);
      InputStream is = null;
      try {
        if(cfgxml.startsWith("!/")) {
          ZipEntry entry = zf.getEntry(cfgxml.substring(2));
          is = zf.getInputStream(entry);
        } else {
          is = (new URL(cfgxml)).openStream();
        }
        if(is == null) {
          throw new Exception
            ("Could not read meta data. Couldn't open xml file.");
        }
        addMetaData(is);
      } finally {
        try { is.close(); } catch (IOException _ioe) {}
        try { zf.close(); } catch (IOException _ioe) {}
      }
    } else {
      throw new Exception("Could not read meta data. No manifest tag.");
    }
  }

  public void loadSerializedMetadata( File f ) {
    InputStream is = null;
    try {
      try {
        is = new FileInputStream(f);
        ObjectInputStream ois = new ObjectInputStream(is);
	metadata = (Hashtable)ois.readObject();
      } catch(Exception e) {
	e.printStackTrace();
      }
    } finally {
      try { is.close(); } catch (IOException _ioe) {}
    }
  }

  public void saveSerializedMetadata( File f ) {
    OutputStream os = null;
    try {
      try {
        os = new FileOutputStream(f);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(metadata);
      } catch(Exception e) {
	e.printStackTrace();
      }
    } finally {
      try { os.close(); } catch (IOException _ioe) {}
    }
  }
  
  public Enumeration getPids() {
    return metadata.keys();
  }

  public MetaData getMetaData(String pid) {
    return (MetaData)metadata.get(pid);
  }


  Object createValue(String type, String def) {
    def = "".equals(def) ? null : def;
    if(type.equals("String")) {
      return def == null ? new String() : new String(def);
    } else if(type.equals("Integer")||type.equals("int")) {
      return def == null ? new Integer(0) : new Integer(def);
    } else if(type.equals("Long")||type.equals("long")) {
      return def == null ? new Long(0) : new Long(def);
    } else if(type.equals("Float")||type.equals("float")) {
      return def == null ? new Float(0) : new Float(def);
    } else if(type.equals("Double")||type.equals("double")) {
      return def == null ? new Double(0) : new Double(def);
    } else if(type.equals("Byte")||type.equals("byte")) {
      return def == null ? new Byte("0") : new Byte(def);
    } else if(type.equals("Short")||type.equals("short")) {
      return def == null ? new Short("0") : new Short(def);
    } else if(type.equals("BigInteger")) {
      return def == null ? new BigInteger("0") : new BigInteger(def);
    } else if(type.equals("BigDecimal")) {
      Object o = null;
      if(classBigDecimal != null && consBigDecimal != null) {
        def = def == null ? "0" : def;
        try {
          o = consBigDecimal.newInstance(new Object [] { def });
        } catch(Exception ignored) {
          o = null;
        }
        return o;
      } else {
        return null;
      }
    } else if(type.equals("Character")||type.equals("char")) {
      return def == null ? new Character('a') : new Character(def.charAt(0));
    } else if(type.equals("Boolean")||type.equals("boolean")) {
      return def == null ? new Boolean(false) : new Boolean(def);
    } else {
      // Unsupported type
      return null;
    }
  }

  Class getType(String type) {
    // Primitive types
    if(type.equals("int")) {
      return Integer.TYPE;
    } else if(type.equals("long")) {
      return Long.TYPE;
    } else if(type.equals("float")) {
      return Float.TYPE;
    } else if(type.equals("double")) {
      return Double.TYPE;
    } else if(type.equals("byte")) {
      return Byte.TYPE;
    } else if(type.equals("short")) {
      return Short.TYPE;
    } else if(type.equals("char")) {
      return Character.TYPE;
    } else if(type.equals("boolean")) {
      return Boolean.TYPE;
    // Object types
    } else if(type.equals("String")) {
      return String.class;
    } else if(type.equals("Integer")) {
      return Integer.class;
    } else if(type.equals("Long")) {
      return Long.class;
    } else if(type.equals("Float")) {
      return Float.class;
    } else if(type.equals("Double")) {
      return Double.class;
    } else if(type.equals("Byte")) {
      return Byte.class;
    } else if(type.equals("Short")) {
      return Short.class;
    } else if(type.equals("BigInteger")) {
      return BigInteger.class;
    } else if(type.equals("BigDecimal")) {
      return classBigDecimal;
    } else if(type.equals("Character")) {
      return Character.class;
    } else if(type.equals("Boolean")) {
      return Boolean.class;
    } else {
      // Unsupported type
      return null;
    }
  }

  /** Helper class that checks that the first element in the XML
   ** document is <cfg> and nothing else.
   */
  class IsMetaDataHandler
    extends HandlerBase
  {
    private boolean firstElementSeen = false;
    private boolean isMetaData = false;
    
    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
    {
      if (!firstElementSeen) {
        isMetaData = CFG.equals(name);
        firstElementSeen = true;
      }
    }
    public boolean isMetaData() 
    {
      return isMetaData;
    }
    
  }// class IsMetaDataHandler


  /** Top level document handler for parsing Meta-data files. */
  class MetaDataHandler
    extends HandlerBase
  {
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      MetaDataManager.this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (CFG.equals(name)) {
        String version = atts.getValue(VERSION);
        if ("0.1".equals(version)) {
          parser.setDocumentHandler( new CFG01Handler(this) );
        } else {
          throw new SAXException
            ("Unsupported version, in <cfg version="+version+">; "
             +" must be 0.1");
        }
      } else {
        throw new SAXException
          ("Unexpected document element, <"+name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"; must be <cfg>." );
      }
    }
  }// class MetaDataHandler


  /** Top document handler for parsing &lt;cfg version=0.1&gt; files. */
  class CFG01Handler
    extends HandlerBase
  {
    private DocumentHandler parentHandler;
    
    public CFG01Handler( DocumentHandler parentHandler) 
    {
      this.parentHandler = parentHandler;
    }

    // DocumentHandler callback.
    public void endElement(java.lang.String name)
    {
      if (CFG.equals(name))
        parser.setDocumentHandler(parentHandler);
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (MANAGED_SERVICE_FACTORY.equals(name)) {
        parser.setDocumentHandler( new ManagedServiceFactoryHandler(this,atts) );
      } else if (MANAGED_SERVICE.equals(name)) {
        parser.setDocumentHandler( new ManagedServiceHandler(this,atts) );
      } else {
        throw new SAXException
          ("Unexpected <cfg> child element, <"+name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
    }
  }// class CFG01Handler

  class ManagedServiceHandler
    extends HandlerBase
  {
    private DocumentHandler parentHandler;
    private MetaDataImpl md = new MetaDataImpl();
    private PropertyHandler propHandler = new PropertyHandler(this,md);
    
    public ManagedServiceHandler( DocumentHandler parentHandler,
                                  AttributeList atts ) 
    {
      this.parentHandler = parentHandler;
      md.pid = atts.getValue(PID);
      md.description = atts.getValue(DESCR);
    }

    // DocumentHandler callback.
    public void endElement(java.lang.String name)
    {
      if (MANAGED_SERVICE.equals(name)) {
        parser.setDocumentHandler(parentHandler);
        metadata.put(md.getPid(), md);
      }
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (PROPERTY.equals(name)) {
        propHandler.setAttributes(atts);
        parser.setDocumentHandler(propHandler);
      } else {
        throw new SAXException
          ("Unexpected <"+MANAGED_SERVICE+"> child element, <"
           +name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
    }
  }// class ManagedServiceHandler


  class ManagedServiceFactoryHandler
    extends HandlerBase
  {
    private DocumentHandler parentHandler;
    private MetaDataImpl md = new MetaDataImpl();
    private PropertyHandler propHandler = new PropertyHandler(this,md);
    
    public ManagedServiceFactoryHandler( DocumentHandler parentHandler,
                                         AttributeList atts ) 
    {
      this.parentHandler = parentHandler;
      md.pid = atts.getValue(PID);
      md.description = atts.getValue(DESCR);
      md.isFactory = true;
    }

    // DocumentHandler callback.
    public void endElement(java.lang.String name)
    {
      if (MANAGED_SERVICE_FACTORY.equals(name)) {
        parser.setDocumentHandler(parentHandler);
        metadata.put(md.getPid(), md);
      }
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (PROPERTY.equals(name)) {
        propHandler.setAttributes(atts);
        parser.setDocumentHandler(propHandler);
      } else {
        throw new SAXException
          ("Unexpected <"+MANAGED_SERVICE_FACTORY+"> child element, <"
           +name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
    }
  }// class ManagedServiceFactoryHandler


  class PropertyHandler
    extends HandlerBase
  {
    private DocumentHandler parentHandler;
    private DocumentHandler textHandler  = new TextHandler(this);
    MetaDataImpl md;
    String name;
    
    public PropertyHandler( DocumentHandler parentHandler,
                            MetaDataImpl    md ) 
    {
      this.parentHandler = parentHandler;
      this.md = md;
    }

    public void setAttributes( AttributeList   atts) 
    {
      name = atts.getValue(NAME);
      md.properties.addElement( name );
    }
    
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
    {
      if (PROPERTY.equals(name)) {
        parser.setDocumentHandler(parentHandler);
      }
    }
    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (DESCRIPTION.equals(name)||HELPTEXT.equals(name)) {
        parser.setDocumentHandler(textHandler);
      } else if (VALUE.equals(name)) {
        parser.setDocumentHandler( new ValueHandler(this,atts) );
      } else if (VECTOR.equals(name)) {
        parser.setDocumentHandler( new VectorHandler(this) );
      } else if (ARRAY.equals(name)) {
        parser.setDocumentHandler( new ArrayHandler(this,atts) );
      } else {
        throw new SAXException
          ("Unexpected <"+PROPERTY+"> child element, <"
           +name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
    }
    // callback from the ValueHandler, ArrayHandler and VectorHandler
    void setValue( Class componentType,
                   Object def,
                   Object templ,
                   String[] headers) 
    {
      md.template.put(name,templ);
      if (def!=null) { //default value given
        md.defaults.put(name,def);
      }
      if (componentType!=null) {
        md.ctypes.put(name,componentType);
      }
      if (headers!=null) {
        md.headers.put(name, headers);
      }
    }
    
  }// class PropertyHandler

  class TextHandler
    extends HandlerBase
  {
    private PropertyHandler pHandler;
    private StringBuffer text = new StringBuffer();
    
    public TextHandler( PropertyHandler pHandler ) 
    {
      this.pHandler = pHandler;
    }

    // DocumentHandler callback.
    public void endElement(java.lang.String name)
    {
      if (DESCRIPTION.equals(name)) {
        parser.setDocumentHandler(pHandler);
        pHandler.md.descriptions.put(pHandler.name,text.toString());
      } else if (HELPTEXT.equals(name)) {
        parser.setDocumentHandler(pHandler);
        pHandler.md.helptexts.put(pHandler.name,text.toString());
      }
      text.setLength(0);
    }
    public void characters(char[] ch, int start, int length)
    {
      text.append(ch,start,length);
    }
    
   }// class TextHandler

  // Handles both <value> and <primitiveValue>.
  class ValueHandler
    extends HandlerBase
  {
    private DocumentHandler parentHandler;
    private String vName;
    private Class type;
    private Object def;
    private Object templ;
    
    public ValueHandler( DocumentHandler parentHandler,
                         AttributeList atts ) 
      throws SAXException
    {
      this.parentHandler = parentHandler;
      vName = atts.getValue(NAME);
      String vtype = atts.getValue(TYPE);
      if(vtype == null) {
        throw new SAXException
          ("Error: type attribute missing in <value> tag on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
      type = getType(vtype);
      String vdefault = atts.getValue(DEFAULT);
      templ  = createValue(vtype, vdefault);
      if (vdefault!=null) { //default value given
        def = templ;
      }
    }
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (VALUE.equals(name)||PRIMITIVE.equals(name)) {
        if (parentHandler instanceof PropertyHandler) {
          ((PropertyHandler)parentHandler).setValue(null,def,templ,null);
        } else if (parentHandler instanceof VectorHandler) {
          ((VectorHandler)parentHandler).addValue(type,def,templ,vName);
        } else if (parentHandler instanceof ArrayHandler) {
          ((ArrayHandler)parentHandler).addValue(type,def,templ,vName);
        }
        parser.setDocumentHandler(parentHandler);
      }
    }
  }// class ValueHandler


  class ArrayHandler
    extends HandlerBase
  {
    private DocumentHandler parentHandler;
    private int length;
    private Class componentType;
    private ArrayList values = new ArrayList();
    private String[] headers;
    private ArrayList headersAL;
    
    public ArrayHandler( DocumentHandler parentHandler,
                         AttributeList atts ) 
    {
      this.parentHandler = parentHandler;

      String l = atts.getValue(LENGTH);
      if (l!=null) {
        try {
          length = Integer.parseInt(l);
          values.ensureCapacity(length);
        } catch (Exception _e) {
        }
      }
    }

    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (ARRAY.equals(name)) {
        if (componentType!=null) {
          Object def   = null;
          if (values.size()>0) {
            length = values.size();
            def = Array.newInstance(componentType,length);
            for (int i=0;i<length;i++) {
              Array.set(def,i,values.get(i));
            }            
          }
          Object templ = def==null ? Array.newInstance(componentType,0) : def;
          Class type = templ.getClass();
          if (headers==null && headersAL.size()>0)
            headers
              = (String[]) headersAL.toArray(new String[headersAL.size()]);
          if (parentHandler instanceof PropertyHandler) {
            PropertyHandler ph = (PropertyHandler)parentHandler;
            if (headersAL!=null && headersAL.size()>0) {
              // Special case; one dimensionall vector.
              headers = new String[]{ (String)headersAL.get(0)};
            }
            ph.setValue(null,def,templ,headers);
          }  else if (parentHandler instanceof VectorHandler) {
            VectorHandler vh = (VectorHandler) parentHandler;
            vh.addValue(type,def,templ,headers);
          }  else if (parentHandler instanceof ArrayHandler) {
            ArrayHandler ah = (ArrayHandler) parentHandler;
            ah.addValue(type,def,templ,headers);
          }
        }
        parser.setDocumentHandler(parentHandler);
      }
    }
    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (VALUE.equals(name)||PRIMITIVE.equals(name)) {
        parser.setDocumentHandler( new ValueHandler(this,atts) );
      } else if (VECTOR.equals(name)) {
        parser.setDocumentHandler( new VectorHandler(this) );
      } else if (ARRAY.equals(name)) {
        parser.setDocumentHandler( new ArrayHandler(this,atts) );
      } else {
        throw new SAXException
          ("Unexpected <"+ARRAY+"> child element, <"
           +name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
    }

    void addValue( Class type, Object def, Object templ, String name ) 
      throws SAXException
    {
      addValue(type,def,templ);
      if (headersAL==null) headersAL = new ArrayList();
      headersAL.add(name);
    }
    
    void addValue( Class type, Object def, Object templ, String[] headers ) 
      throws SAXException
    {
      addValue(type,def,templ);
      if (headers!=null && this.headers==null) this.headers = headers;
    }
    
    private void addValue( Class type, Object def, Object templ ) 
      throws SAXException
    {
      if (componentType==null) {
        componentType = type;
      } else if (!componentType.equals(type)) {
        throw new SAXException("<array> with mixed component types");
      }
      if (def!=null) {
        values.add(def);
      }
    }

  }// class ArrayHandler

  class VectorHandler
    extends HandlerBase
  {
    private DocumentHandler parentHandler;
    private int length;
    private Class componentType;
    private Vector defaultV;
    private String[] headers;    
    private ArrayList headersAL;    
    
    public VectorHandler( DocumentHandler parentHandler ) 
    {
      this.parentHandler = parentHandler;
      defaultV  = new Vector(20);
    }

    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (VECTOR.equals(name)) {
        if (componentType!=null) {
          if (defaultV.size()==0)  defaultV  = null;
          Vector templV = defaultV == null ? new Vector() : defaultV;
          if (headers==null && headersAL.size()>0)
            headers
              = (String[]) headersAL.toArray(new String[headersAL.size()]);
          if (parentHandler instanceof PropertyHandler) {
            PropertyHandler ph = (PropertyHandler) parentHandler;
            if (headersAL!=null && headersAL.size()>0) {
              // Special case; one dimensionall vector.
              headers = new String[]{ (String)headersAL.get(0)};
            }
            ph.setValue(componentType, defaultV, templV, headers );
          }  else if (parentHandler instanceof VectorHandler) {
            VectorHandler vh = (VectorHandler) parentHandler;
            vh.addValue(Vector.class,defaultV,templV,headers);
          }  else if (parentHandler instanceof ArrayHandler) {
            ArrayHandler ah = (ArrayHandler) parentHandler;
            ah.addValue(Vector.class,defaultV,templV,headers);
          }
        }
        parser.setDocumentHandler(parentHandler);
      }
    }
    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (VALUE.equals(name)||PRIMITIVE.equals(name)) {
        parser.setDocumentHandler( new ValueHandler(this,atts) );
      } else if (VECTOR.equals(name)) {
        parser.setDocumentHandler( new VectorHandler(this) );
      } else if (ARRAY.equals(name)) {
        parser.setDocumentHandler( new ArrayHandler(this,atts) );
      } else {
        throw new SAXException
          ("Unexpected <"+VECTOR+"> child element, <"
           +name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
    }

    void addValue( Class type, Object def, Object templ, String name ) 
      throws SAXException
    {
      addValue(type,def,templ);
      if (headersAL==null) headersAL = new ArrayList();
      headersAL.add(name);
    }
    
    void addValue( Class type, Object def, Object templ, String[] headers ) 
      throws SAXException
    {
      addValue(type,def,templ);
      if (headers!=null && this.headers==null) this.headers = headers;
    }
    
    void addValue( Class type, Object def, Object templ ) 
      throws SAXException
    {
      if (componentType==null) {
        componentType = type;
      } else if (!componentType.equals(type)) {
        throw new SAXException("<vector> with mixed component types");
      }
      if (def!=null) {
        defaultV.addElement(def);
      }
    }
  }// class VectorHandler

}

class MetaDataImpl implements MetaData {
  String pid;
  String description;
  boolean isFactory = false;
  Vector properties = new Vector();
  Hashtable descriptions = new Hashtable();
  Hashtable helptexts = new Hashtable();
  Hashtable defaults = new Hashtable();
  Hashtable template = new Hashtable();
  Hashtable ctypes = new Hashtable();
  Hashtable headers = new Hashtable();
  
  public String getPid() {
    return pid;
  }

  public String getDescription() {
    return description == null ? "" : description;
  }

  public boolean isFactory() {
    return isFactory;
  }

  public String[] getProperties() {
    String[] ps = new String[properties.size()];
    properties.copyInto(ps);
    return ps;
  }

  public String getDescription(String property) {
    return (String)descriptions.get(property);
  }

  public String getHelptext(String property) {
    return (String)helptexts.get(property);
  }

  public Class getType(String property) {
    return template.get(property).getClass();
  }

  public Class getComponentType(String property) {
    return (Class)ctypes.get(property);
  }

  public String[] getTableHeaders(String property) {
    return (String[])headers.get(property);
  }

  public Object getDefault(String property) {
    return defaults.get(property);
  }

  public Dictionary getConfigTemplate(boolean defaultOnly) {
    if(defaultOnly) {
      return copyOf(defaults);
    } else {
      return copyOf(template);
    }
  }
  
  private Hashtable copyOf(Hashtable in) {
    if(in == null) {
      return null;
    }
    Hashtable out = null;
    // Use serialization to deep copy hashtable
    try {
      ByteArrayOutputStream buf = 
        new ByteArrayOutputStream();
      ObjectOutputStream os =
        new ObjectOutputStream(buf);
      os.writeObject(in);
      ObjectInputStream is =
        new ObjectInputStream(
          new ByteArrayInputStream(
            buf.toByteArray()));
      out = (Hashtable)is.readObject();
    } catch(Exception e) {
      out = null;
    }
    return out;
  }
}

/** Input stream for a zip file entry that closes the zip file when
 ** closed. 
 **/
class ZipFileInputStream extends InputStream {

  InputStream in;
  ZipFile zf;

  ZipFileInputStream( ZipFile zf, InputStream in ) {
    this.zf = zf;
    this.in = in;
  }

  public int read() throws IOException {
    return in.read();
  }

  public int read(byte[] b) throws IOException {
    return in.read(b);
  }

  public int read(byte[] b,
                  int off,
                  int len)
    throws IOException
  {
    return in.read(b,off,len);
  }

  public long skip(long n) throws IOException {
    return in.skip(n);
  }

  public int available() throws IOException {
    return in.available();
  }

  public void close() throws IOException {
    in.close();
    zf.close();
  }

  public void mark(int readlimit) {
    in.mark(readlimit);
  }

  public void reset() throws IOException {
    in.reset();
  }

  public boolean markSupported() {
    return in.markSupported();
  }
}// ZipFileInputStream
