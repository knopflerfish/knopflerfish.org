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

// OSGi packages
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

// Gatespace packages

// XML packages (provided by the jaxp bundle)
import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;

// Standard Java packages
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/** XML document handler for &lt;cm_data&gt; elements. I.e. the top
 ** level element of a cm_data XML file.
 ** @author Gatespace AB
 ** @version $Id: CMDataHandler.java,v 1.1.1.1 2004/03/05 20:34:49 wistrand Exp $
 **/
class CMDataHandler extends HandlerBase
  implements CMDataNames
{
  private Parser  parser;
  private ConfigurationAdmin cfgAdm;
  private String baseURL;

  private final ArrayList configs = new ArrayList();
  private Locator locator;
    
  /** Document handler for cm_data files.
   ** @param parser the XML parser that uses this handler.
   ** @param cfgAdm the configuartion admin to interact with.
   ** @param url    base URL used to complete relative URLs in include
   **               nodes with.
   **/
  public CMDataHandler( Parser parser,
                        ConfigurationAdmin cfgAdm,
                        String url )
  {
    this.parser = parser;
    this.cfgAdm = cfgAdm;
    this.baseURL = url;
  }
    
  // DocumentHandler callback.
  public void setDocumentLocator(Locator locator) 
  {
    this.locator = locator;
  }
  
  // DocumentHandler callback.
  public void startElement(java.lang.String name,
                           AttributeList atts)
    throws SAXException
  {
    if (CM_DATA_ROOT_NAME.equals(name)) {
      String version = atts.getValue(CM_DATA_VERSION_ANAME);
      if ("0.1".equals(version)) {
        parser.setDocumentHandler
          ( new CMData01Handler( this ) );
      } else {
        throw new SAXException
          ("Unsupported version, in <cm_data version="+version+">; "
           +" must be 0.1");
      }
    } else {
      throw new SAXException
        ("Unexpected document element, <"+name +"> found on line "
         +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
         +", column "
         +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
         +"; must be <cm_data>." );
    }
  }
    
  /** Adds a configuration to the collection of configurations
   ** mentioned in the XML document.
   **/
  void add( Configuration cfg ) 
  {
    configs.add( cfg );
  }

  /** Adds the elements of a collection of configurations to the
   ** collection of configurations mentioned in the XML document.
   **/
  void add( Collection collection ) 
  {
    configs.addAll( collection );
  }
    
  /** Returns a collection with all configurations affected by the
   ** parsed XML file.
   **/
  Collection getConfigurations() 
  {
    return configs;
  }
    
  /**
   ** Convert a string to wrapped primitive object.
   ** @param type  the primitive type to wrap
   ** @param value the value to parse and wrap.
   **/
  private Object toWrappedPrimitive( String type, String value ) {
    value = value.trim();
    Object res = null;
    if ("long".equals(type)) {
      res = new Long( value.length()==0 ? "0" : value );
    } else if ("int".equals(type)) {
      res = new Integer( value.length()==0 ? "0" : value );
    } else if ("short".equals(type)) {
      res = new Short( value.length()==0 ? "0" : value );
    } else if ("char".equals(type)) {
      res = new Character(  value.length()==0 ? 'a' : value.charAt(0) );
    } else if ("byte".equals(type)) {
      res = new Byte( value.length()==0 ? "0" : value );
    } else if ("double".equals(type)) {
      res = new Double( value.length()==0 ? "0" : value );
    } else if ("float".equals(type)) {
      res = new Float( value.length()==0 ? "0" : value );
    } else if ("boolean".equals(type)) {
      res = new Boolean( value.length()==0 ? "false" : value );
    } else {
      throw new IllegalArgumentException
        ("Unknown primitive type, '" +type +"' ignored.");
    }
    return res;
  }// of toWrappedPrimitive(String,String)


  /**
   ** Convert a string to an object.
   ** @param type  the type of the new object
   ** @param value the value to parse and convert.
   **/
  private Object toObject( String type, String value ) {
    value = value.trim();
    Object res = null;
    type = type.trim();
    if ("String".equals(type)) {
      res = value;
    } else if ("Integer".equals(type)) {
      res = new Integer( value.length()==0 ? "0" : value );
    } else if ("Long".equals(type)) {
      res = new Long( value.length()==0 ? "0" : value );
    } else if ("Float".equals(type)) {
      res = new Float( value.length()==0 ? "0" : value );
    } else if ("Double".equals(type)) {
      res = new Double( value.length()==0 ? "0" : value );
    } else if ("Byte".equals(type)) {
      res = new Byte( value.length()==0 ? "0" : value );
    } else if ("Short".equals(type)) {
      res = new Short( value.length()==0 ? "0" : value );
    } else if ("BigInteger".equals(type)) {
      res = new BigInteger( value.length()==0 ? "0" : value );
    } else if ("BigDecimal".equals(type)) {
      if(CMDataManager.classBigDecimal != null
         && CMDataManager.consBigDecimal != null) {
        try {
          res = CMDataManager.consBigDecimal.newInstance
            (new Object [] { value.length()==0 ? "0" : value });
        } catch(Exception ignored) {
          res = null;
        }
      } else {
        res = null;
      }
    } else if ("Character".equals(type)) {
      res = new Character( value.length()==0 ? 'a' : value.charAt(0) );
    } else if ("Boolean".equals(type)) {
      res = new Boolean( value.length()==0 ? "false" : value );
    } else {
      throw new IllegalArgumentException
        ("Unknown type, '" +type +"' ignored.");
    }
    return res;
  }// of toObject(String,String)


  /**
   ** Converts an array component type description to java Class object.
   ** @param type the type description to convert.
   ** @return the java Class object for the specified type.
   ** @throws IllegalArgumentException if type specification is wrong.
   **/
  private Class toJavaType( String type ) {
    // Precondition.
    if (type==null||type.length()==0)
      throw new IllegalArgumentException("null or empty type description");
    Class res = null;
    int arrayDim = 0;
    while (type.endsWith("[]")) {
      arrayDim++;
      type = type.substring(0,type.length()-2);
    }
    if ("String".equals(type)) {
      res = arrayDim==0
        ? String.class : toJavaArrayType( "Ljava.lang.String;", arrayDim );
    } else if("Integer".equals(type)) {
      res = arrayDim==0
        ? Integer.class : toJavaArrayType( "Ljava.lang.Integer;", arrayDim );
    } else if("Long".equals(type)) {
      res = arrayDim==0
        ? Long.class : toJavaArrayType( "Ljava.lang.Long;", arrayDim );
    } else if("Float".equals(type)) {
      res = arrayDim==0
        ? Float.class : toJavaArrayType( "Ljava.lang.Float;", arrayDim );
    } else if("Double".equals(type)) {
      res = arrayDim==0
        ? Double.class : toJavaArrayType( "Ljava.lang.Double;", arrayDim );
    } else if("Byte".equals(type)) {
      res = arrayDim==0
        ? Byte.class : toJavaArrayType( "Ljava.lang.Byte;", arrayDim );
    } else if("Short".equals(type)) {
      res = arrayDim==0
        ? Short.class : toJavaArrayType( "Ljava.lang.Short;", arrayDim );
    } else if("BigInteger".equals(type)) {
      res = arrayDim==0
        ? java.math.BigInteger.class
        : toJavaArrayType( "Ljava.math.BigInteger;", arrayDim );
    } else if("BigDecimal".equals(type)) {
      if(CMDataManager.classBigDecimal == null) {
        throw new IllegalArgumentException
          ("BigDecimal not supported under current profile.");
      }
      res = arrayDim==0
        ? CMDataManager.classBigDecimal
        : toJavaArrayType( "Ljava.math.BigDecimal;", arrayDim );
    } else if("Character".equals(type)) {
      res = arrayDim==0
        ? Character.class : toJavaArrayType( "Ljava.lang.Character;",arrayDim);
    } else if("Boolean".equals(type)) {
      res = arrayDim==0
        ? Boolean.class : toJavaArrayType( "Ljava.lang.Boolean;", arrayDim );
    } else if("Vector".equals(type)) {
      res = arrayDim==0
        ? Vector.class : toJavaArrayType( "Ljava.util.Vector;", arrayDim );
    } else if("long".equals(type)) {
      res = arrayDim==0
        ? Long.TYPE : toJavaArrayType( "J", arrayDim );
    } else if("int".equals(type)) {
      res = arrayDim==0
        ? Integer.TYPE : toJavaArrayType( "I", arrayDim );
    } else if("short".equals(type)) {
      res = arrayDim==0
        ? Short.TYPE : toJavaArrayType( "S", arrayDim );
    } else if("char".equals(type)) {
      res = arrayDim==0
        ? Character.TYPE : toJavaArrayType( "C", arrayDim );
    } else if("byte".equals(type)) {
      res = arrayDim==0
        ? Byte.TYPE : toJavaArrayType( "B", arrayDim );
    } else if("double".equals(type)) {
      res = arrayDim==0
        ? Double.TYPE : toJavaArrayType( "D", arrayDim );
    } else if("float".equals(type)) {
      res = arrayDim==0
        ? Float.TYPE : toJavaArrayType( "F", arrayDim );
    } else if("boolean".equals(type)) {
      res = arrayDim==0
        ? Boolean.TYPE : toJavaArrayType( "Z", arrayDim );
    } else {
      throw new IllegalArgumentException("Unknown config data type: " +type);
    }

    return res;
  }

  /**
   ** Converts a type name and an array dimension to a java class
   ** object for such arrays.
   ** @param type the component type name.
   ** @param dim  the array dimension.
   ** @return the java Class object for the specified array type.
   ** @throws IllegalArgumentException if type specification is wrong.
   **/
  private Class toJavaArrayType( String type, int dim ) {
    // Type name syntax is described in tha Javadoc for
    // java.lang.Class.getName().
    StringBuffer typeBuffer = new StringBuffer();
    for (int i=0; i<dim; i++)
      typeBuffer.append( "[" );
    typeBuffer.append(type);
    try {
      return Class.forName( typeBuffer.toString() );
    } catch (Exception e) {
      throw new IllegalArgumentException("Unknown config data type: " +type);
    }
  }

  /** XML document handler for &lt;cm_data version=0.1&gt;
   ** elements. I.e. the top level element of a cm_data XML file.
   **/
  class CMData01Handler extends HandlerBase
  {
    private CMDataHandler parentHandler;
    
    private Include01Handler includeHandler;
    private Configuration01Handler configurationHandler;
    private FactoryConfiguration01Handler factoryConfigurationHandler;
    private Filter01Handler filterHandler; 

    private Locator locator;
    
    CMData01Handler( CMDataHandler parentHandler )
    {
      this.parentHandler = parentHandler;
      
      includeHandler              = new Include01Handler( this );
      configurationHandler        = new Configuration01Handler( this );
      factoryConfigurationHandler = new FactoryConfiguration01Handler( this );
      filterHandler               = new Filter01Handler( this );
    }
    
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
    {
      if (CM_DATA_ROOT_NAME.equals(name))
        parser.setDocumentHandler(parentHandler);
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (CM_DATA_INCLUDE_NAME.equals(name)) {
        includeHandler.init(atts);
        parser.setDocumentHandler( includeHandler );
      } else if (CM_DATA_CONFIGRUATION_NAME.equals(name)) {
        configurationHandler.init(atts);
        parser.setDocumentHandler( configurationHandler );
      } else if (CM_DATA_FACTORYCONFIGRUATION_NAME.equals(name)) {
        factoryConfigurationHandler.init(atts);
        parser.setDocumentHandler( factoryConfigurationHandler );
      } else if (CM_DATA_FILTER_NAME.equals(name)) {
        filterHandler.init(atts);
        parser.setDocumentHandler( filterHandler );
      } else {
        throw new SAXException
          ("Unexpected <" +CM_DATA_ROOT_NAME +"> child element, <"
           +name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"." );
      }
    }
    
    void add( Configuration cfg ) 
    {
      parentHandler.add( cfg );
    }
    
    void add( Collection collection ) 
    {
      parentHandler.add( collection );
    }
    
  }// end of CMData01Handler


  /** XML document handler for &lt;configuration&gt; elements. **/
  class Configuration01Handler extends HandlerBase
  {
    private final CMData01Handler parentHandler;
    private final Property01Handler propertyHandler;

    private Locator locator;

    private Configuration cfg;
    private Dictionary properties;
    private String pid;
    private String mode;
    
    Configuration01Handler( CMData01Handler parentHandler )
    {
      this.parentHandler = parentHandler;
      propertyHandler    = new Property01Handler(this);
    }
    
    void init(AttributeList atts) 
      throws SAXException
    {
      pid  = atts.getValue("pid");
      mode = atts.getValue("mode");
      try {
        cfg = cfgAdm!=null ? cfgAdm.getConfiguration(pid,null)
          : new ConfigurationImpl(pid,null,null,null);
      } catch (IOException ioe) {
        throw new SAXException
          ("Failed to get/create configuration with pid = "+pid +": "
           + ioe.toString() );
      }
      properties = cfg.getProperties();
      if (properties==null || "new".equals(mode)) {
        // Must start with a new empty properties object.
        properties = new Hashtable();
      }
    }
    
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (CM_DATA_CONFIGRUATION_NAME.equals(name)) {
        if ("delete".equals(mode)) {
          try {
            cfg.delete();
          } catch (IOException ioe) {
            throw new SAXException
              ("Failed to delete configuration with pid = "+pid +": "
               + ioe.toString() );
          }
        } else {
          try {
            cfg.update( properties );
          } catch (IOException ioe) {
            throw new SAXException
              ("Failed to update configuration with pid = "+pid +": "
               + ioe.toString() );
          }
        }
        parentHandler.add( cfg );
        parser.setDocumentHandler( parentHandler );
      }
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (CM_DATA_PROPERTY_NAME.equals(name)) {
        propertyHandler.init(atts,properties);
        parser.setDocumentHandler( propertyHandler );
      } else {
        throw new SAXException
          ("Unexpected <" +CM_DATA_CONFIGRUATION_NAME
           +"> child element, <"+name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"; must be <" +CM_DATA_PROPERTY_NAME +">." );
      }
    }
    
  }// end of Configuration01Handler

  /** XML document handler for &lt;factoryconfiguration&gt; elements. **/
  class FactoryConfiguration01Handler extends HandlerBase
  {
    private final CMData01Handler parentHandler;
    private final Property01Handler propertyHandler;

    private Locator locator;

    private Configuration cfg;
    private Dictionary properties;
    private String factorypid;
    private String mode;
    private String ldapfilter;
    
    FactoryConfiguration01Handler( CMData01Handler parentHandler )
    {
      this.parentHandler = parentHandler;
      propertyHandler    = new Property01Handler(this);
    }
    
    void init(AttributeList atts) 
      throws SAXException
    {
      factorypid  = atts.getValue("factorypid");
      mode = atts.getValue("mode");
      ldapfilter = atts.getValue("ldapfilter");
      if (cfgAdm==null) {
        cfg = new ConfigurationImpl(null,null,factorypid,null);
      } else if ("new".equals(mode)) {
        try {
          cfg = cfgAdm.createFactoryConfiguration( factorypid, null );
        } catch (IOException ioe) {
          throw new SAXException
            ("Failed to get/create factory configuration with pid = "
             +factorypid +": " + ioe.toString() );
        }
      } else if ("update".equals(mode)) {
        if (ldapfilter.length()==0) {
          throw new SAXException
            ("Invalid <factoryconfiguration>, on line "
             +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
             +", column "
             +(locator!=null ? String.valueOf(locator.getColumnNumber()):"?")
             +"ldapfilter must not be empty.");
        }
        ldapfilter
          = "(&(service.factoryPid=" +factorypid +")" + ldapfilter +")";
        try {
          final Configuration[] cfgs = cfgAdm.listConfigurations(ldapfilter);
          if (cfgs==null || cfgs.length==0) {
            cfg = cfgAdm.createFactoryConfiguration( factorypid, null );
          } else {
            cfg = cfgs[0];
          }
        } catch (IOException ioe) {
          throw new SAXException
            ("Failed to find configurations for <factoryconfiguration>"
             +" on line "
             +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
             +", column "
             +(locator!=null ? String.valueOf(locator.getColumnNumber()):"?")
             +"; " +ioe.toString());
        } catch (org.osgi.framework.InvalidSyntaxException ise) {
          throw new SAXException
            ("Invalid <factoryconfiguration> element found on line "
             +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
             +", column "
             +(locator!=null ? String.valueOf(locator.getColumnNumber()):"?")
             +"; ldapfilter string '"
             +ldapfilter +" is not correct: " +ise.toString());
        }
      } else {
        throw new SAXException
          ("Invalid <factoryconfiguration> element found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()):"?")
           +"; unknow mode '"
           +mode +" must be either 'new' or 'update'." );
      }
      properties = cfg.getProperties();
      if (properties==null || "new".equals(mode)) {
        // Must start with a new empty properties object.
        properties = new Hashtable();
      }
    }
    
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (CM_DATA_FACTORYCONFIGRUATION_NAME.equals(name)) {
        try {
          cfg.update( properties );
        } catch (IOException ioe) {
          throw new SAXException
            ("Failed to update factoryconfiguration with pid = "+factorypid +": "
             + ioe.toString() );
        }
        parentHandler.add( cfg );
        parser.setDocumentHandler( parentHandler );
      }
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (CM_DATA_PROPERTY_NAME.equals(name)) {
        propertyHandler.init(atts,properties);
        parser.setDocumentHandler( propertyHandler );
      } else {
        throw new SAXException
          ("Unexpected <" +CM_DATA_FACTORYCONFIGRUATION_NAME
           +"> child element, <"+name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"; must be <" +CM_DATA_PROPERTY_NAME +">." );
      }
    }
    
  }// end of FactoryConfiguration01Handler
  

  /** XML document handler for &lt;filter&gt; elements. **/
  class Filter01Handler extends HandlerBase
  {
    private final CMData01Handler parentHandler;
    private final Property01Handler propertyHandler;

    private Locator locator;

    private Dictionary properties;
    private String mode;
    private String ldapfilter;
    
    Filter01Handler( CMData01Handler parentHandler )
    {
      this.parentHandler = parentHandler;
      propertyHandler    = new Property01Handler(this);
    }
    
    void init(AttributeList atts) 
    {
      mode = atts.getValue("mode");
      ldapfilter = atts.getValue("ldapfilter");
      properties = new Hashtable();
    }
    
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (CM_DATA_FILTER_NAME.equals(name)) {
        ArrayList cfgCollection = new ArrayList();
        try {
          final Configuration[] cfgs = cfgAdm.listConfigurations
            ( ldapfilter.length()>0 ? ldapfilter : null );

          if (cfgs!=null) {
            for (int cfgIx=0; cfgIx<cfgs.length; cfgIx++) {
              Configuration config = cfgs[cfgIx];
              cfgCollection.add( config );
              if ("delete".equals(mode)) {
                config.delete();
              } else {
                Dictionary props = config.getProperties();
                if (props==null) props = new Hashtable();
                for (Enumeration keys = properties.keys();
                     keys.hasMoreElements(); ) {
                  String key = (String) keys.nextElement();
                  props.put( key, properties.get(key) );
                }
                config.update(props);
              }
            }
          }
        } catch (IOException ioe) {
          throw new SAXException
            ("Failed to find/create/update configs that match = "
             +ldapfilter +": " + ioe.toString() );
        } catch (org.osgi.framework.InvalidSyntaxException ise) {
          throw new SAXException
            ("Invalid <filter> element found on line "
             +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
             +", column "
             +(locator!=null ? String.valueOf(locator.getColumnNumber()):"?")
             +"; ldapfilter string '"
             +ldapfilter +" is not correct: " +ise.toString());
        }
        parentHandler.add( cfgCollection );
        parser.setDocumentHandler( parentHandler );
      }
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (CM_DATA_PROPERTY_NAME.equals(name)) {
        propertyHandler.init(atts,properties);
        parser.setDocumentHandler( propertyHandler );
      } else {
        throw new SAXException
          ("Unexpected <" +CM_DATA_FILTER_NAME
           +"> child element, <"+name +"> found on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +"; must be <" +CM_DATA_PROPERTY_NAME +">." );
      }
    }
  }// end of Filter01Handler
  

  /** XML document handler for &lt;include&gt; elements. **/
  class Include01Handler extends HandlerBase
  {
    private final CMData01Handler parentHandler;

    private Locator locator;
    private String url;
    
    Include01Handler( CMData01Handler parentHandler )
    {
      this.parentHandler = parentHandler;
    }
    
    void init(AttributeList atts) 
    {
      url = atts.getValue("url");
    }
    
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (CM_DATA_INCLUDE_NAME.equals(name)) {
        InputStream is = null;
        try {
          URL incURL = new URL(new URL(baseURL),url);
          is = MetaDataManager.getInputStream(incURL);
          Collection c = CMDataManager.doParse( is, incURL.toString(),cfgAdm );
          parentHandler.add(c);
        } catch (Exception e) {
          throw new SAXException
            ("<include> on line "
             +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
             +", column "
             +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
             +" failed with the error: " +e.toString() );
        } finally {
          if (is!=null) try {
            is.close();
          } catch (IOException _ioe) {
          }          
        }
        parser.setDocumentHandler(parentHandler);
      }
    }

  }// end of Include01Handler


  /** XML document handler for &lt;property&gt; elements. **/
  class Property01Handler extends HandlerBase
  {
    private final DocumentHandler parentHandler;

    private Locator locator;

    private Dictionary properties;
    private String     propName;
    private String     propType;
    private Object     propValue;

    private StringBuffer text;
    
    Property01Handler( DocumentHandler parentHandler )
    {
      this.parentHandler = parentHandler;
    }
    
    void init(AttributeList atts, Dictionary properties) 
    {
      this.properties = properties;
      propName  = atts.getValue("name");
      propValue = null;
    }
    
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (CM_DATA_PROPERTY_NAME.equals(name)) {
        if (propValue!=null) {
          properties.put( propName, propValue );
        } 
        parser.setDocumentHandler(parentHandler);
      } else if (CM_DATA_VALUE_NAME.equals(name)) { 
        propValue = toObject( propType, text.toString() );
        text.setLength(0);
      }
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (CM_DATA_VALUE_NAME.equals(name)) {
        propType = atts.getValue("type");
        if (text==null) text = new StringBuffer();
      } else if (CM_DATA_ARRAY_NAME.equals(name)
                 || CM_DATA_VECTOR_NAME.equals(name)) {
        ArrayVector01Handler avh
          = new ArrayVector01Handler( this, CM_DATA_ARRAY_NAME.equals(name) );
        avh.init(atts);
        parser.setDocumentHandler( avh );
      } else {
        throw new SAXException
          ("Illegal <property> child <" +name +"> on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +".");
      }
    }

    public void characters(char[] ch, int start, int length)
    {
      if (text!=null) text.append(ch,start,length);
    }
   
    public void setValue( Object obj )
    {
      propValue = obj;
    }
    
  }// end of Property01Handler

  /** XML document handler for &lt;array&gt; and &lt;vector&gt; elements. **/
  class ArrayVector01Handler extends HandlerBase
  {
    private final DocumentHandler parentHandler;
    private final boolean isArray;

    private Locator locator;
    
    private int    length;
    private Class  elementType;
    private Vector values;

    private String valueType;
    private StringBuffer text;
    
    ArrayVector01Handler( DocumentHandler parentHandler,
                          boolean isArray )
    {
      this.parentHandler = parentHandler;
      this.isArray = isArray;
    }
    
    void init(AttributeList atts) 
    {
      if (isArray) {
        elementType = toJavaType( atts.getValue("elementType").trim() );
        length      = Integer.parseInt( atts.getValue("length").trim() );
      } else {
        length = 10;
      }
      values = new Vector(length,5);
    }
    
    // DocumentHandler callback.
    public void setDocumentLocator(Locator locator) 
    {
      this.locator = locator;
    }
  
    // DocumentHandler callback.
    public void endElement(java.lang.String name)
      throws SAXException
    {
      if (CM_DATA_ARRAY_NAME.equals(name)) {
        Object res = Array.newInstance( elementType, values.size() );
        try {
          // Fill in res with appropriate values.
          for (int i=0; i<values.size(); i++) {
            Array.set( res, i, values.elementAt(i) );
          }
        } catch (Exception e) {
          throw new SAXException
            ("Array element problem in array on line "
             +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
             +", column "
             +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
             +"; " +e.toString() );
        }
        if (parentHandler instanceof ArrayVector01Handler) {
          ((ArrayVector01Handler)parentHandler).addValue(res);
        } else if (parentHandler instanceof Property01Handler) {
          ((Property01Handler)parentHandler).setValue(res);
        }
        parser.setDocumentHandler(parentHandler);
      } else if (CM_DATA_VECTOR_NAME.equals(name)) { 
        if (parentHandler instanceof ArrayVector01Handler) {
          ((ArrayVector01Handler)parentHandler).addValue(values);
        } else if (parentHandler instanceof Property01Handler) {
          ((Property01Handler)parentHandler).setValue(values);
        }
        parser.setDocumentHandler(parentHandler);
      } else if (CM_DATA_VALUE_NAME.equals(name)) { 
        addValue( toObject( valueType, text.toString() ) );
        text.setLength(0);
      } else if (isArray && CM_DATA_PRIMITIVEVALUE_NAME.equals(name) ) { 
        addValue( toWrappedPrimitive( valueType, text.toString() ) );
        text.setLength(0);
      } else {
        throw new SAXException
          ((isArray?"Array":"Vector")
           +" element with strange child <" +name +"> on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +".");
      }
    }

    // DocumentHandler callback.
    public void startElement(java.lang.String name,
                             AttributeList atts)
      throws SAXException
    {
      if (CM_DATA_VALUE_NAME.equals(name)
          || CM_DATA_PRIMITIVEVALUE_NAME.equals(name) ) {
        valueType = atts.getValue("type");
        if (text==null) text = new StringBuffer();
      } else if (CM_DATA_ARRAY_NAME.equals(name)
                 || CM_DATA_VECTOR_NAME.equals(name)) {
        ArrayVector01Handler avh
          = new ArrayVector01Handler( this, CM_DATA_ARRAY_NAME.equals(name) );
        avh.init(atts);
        parser.setDocumentHandler( avh );
      } else {
        throw new SAXException
          ("Illegal <" +(isArray ? CM_DATA_ARRAY_NAME : CM_DATA_VECTOR_NAME)
           +"> child <" +name +"> on line "
           +(locator!=null ? String.valueOf(locator.getLineNumber()) :"?")
           +", column "
           +(locator!=null ? String.valueOf(locator.getColumnNumber()) : "?")
           +".");
      }
    }

    public void characters(char[] ch, int start, int length)
    {
      if (text!=null) text.append(ch,start,length);
    }
   
    public void addValue( Object obj )
    {
      values.addElement(obj);
    }
    
  }// end of ArrayVectorHandler


  /** Dummy implementation of Configuration used to implement the
   ** objects in the collection renturned by CMDataHandler if the
   ** ConfigurationAdmin service is <code>null</code>.
   **/
  static class ConfigurationImpl implements Configuration {
    private String pid = null;
    private Dictionary properties = null;
    private String factoryPid = null;
    private String bundleLocation = null;

    public ConfigurationImpl(String pid, 
                             Dictionary properties,
                             String factoryPid,
                             String bundleLocation) {
      this.pid = pid;
      this.properties = properties;
      this.factoryPid = factoryPid;
      this.bundleLocation = bundleLocation;
    }

    public String getPid() {
      return pid;
    }

    public Dictionary getProperties() {
      return properties;
    }

    public void update(Dictionary properties) throws IOException {
      this.properties = properties;
    }

    public void delete() throws IOException {
    }

    public String getFactoryPid() {
      return factoryPid;
    }

    public void update() throws IOException {
    }

    public void setBundleLocation( String bundleLocation ) {
      this.bundleLocation = bundleLocation;
    }

    public String getBundleLocation() {
      return bundleLocation;
    }
  }// of class ConfigurationImpl

  
}// end of CMDataHandler
