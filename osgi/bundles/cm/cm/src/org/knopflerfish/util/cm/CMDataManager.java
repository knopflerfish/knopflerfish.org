/*
 * Copyright (c) 2003, KNOPFLERFISH project
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
import com.sun.xml.parser.*; // Must have this to get import-package right
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserFactory;

// Standard Java packages
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;


/**
 ** Loads/saves Configuration Management data from/to XML files
 ** that uses the cm_data DTD.
 **
 ** <p>
 ** Public methods in this class intentionally avoids throwing any 
 ** XML related exceptions. However, at any XML parse error, 
 ** IllegalArgumentException is thrown instead, with a nice descriptive
 ** string of the error.
 ** </p>
 **
 ** @author Gatespace
 ** @version $Id: CMDataManager.java,v 1.1.1.1 2004/03/05 20:34:49 wistrand Exp $
 **/
public class CMDataManager
  implements CMDataNames
{

  /**
   ** Add constructor that makes it impossible for external users to
   ** create instances of this class (it is a library class and should
   ** not be instantiated).
   **/
  private CMDataManager() {
    throw new UnsupportedOperationException
      ("CmDataManager is not instanciable.");
  }


  /** BigDecimal class if available */
  static Class classBigDecimal;
  /** BigDecimal constructor if available */
  static Constructor consBigDecimal;
  static {
    try {
      classBigDecimal = Class.forName("java.math.BigDecimal");
      consBigDecimal
        = classBigDecimal.getConstructor(new Class [] { String.class });
    } catch (Exception ignore) {
      classBigDecimal = null;
      consBigDecimal = null;
    }
  }


  //////////////////// Load cm_data file and do the operations in it.////////
  /** 
   ** Parses an cm_data DTD XML-file and updates CM using the
   ** specified cfgAdmin service.
   **
   ** @param url      String with URL syntax pointing to the XML-file
   **                 to parse.
   ** @param cfgAdmin Configuration admin service to use when updating
   **                 CM with data from the specified XML-file.
   ** @throws java.io.IOException
   ** @throws java.lang.IllegalArgumentException If the cfgAdmin is
   **   null or the root element of the XML-file is not named
   **   'cm_data', or if the XML parse failed.
   **/
  public static void handleCMData(  String url,
                                    ConfigurationAdmin cfgAdmin )
    throws java.io.IOException
  {
    InputStream is = MetaDataManager.getInputStream(new URL(url));
    try {
      handleCMData( is, url, cfgAdmin );
    } finally {
      is.close();
    }
  }//of handleCMData(String,ConfigurationAdmin)

  /** 
   ** Parses an cm_data DTD XML-stream and updates CM using the
   ** specified cfgAdmin service.
   **
   ** @param is       XML stream to parse.
   ** @param cfgAdmin Configuration admin service to use when updating
   **                 CM with data from the specified XML-file.
   ** @throws java.io.IOException
   ** @throws java.lang.IllegalArgumentException If the cfgAdmin is
   **   null or the root element of the XML-file is not named
   **   'cm_data', or if the XML parse failed.
   **/
  public static void handleCMData( InputStream is,
                                   ConfigurationAdmin cfgAdmin )
    throws java.io.IOException
  {
    handleCMData( is, null, cfgAdmin );
  }
  
  /** 
   ** Parses an cm_data DTD XML-stream and updates CM using the
   ** specified cfgAdmin service.
   **
   ** @param is       XML stream to parse.
   ** @param url      URL of the stream to parse, must be a correct
   **                 value if the document contains an
   **                 &lt;include&gt; element with a realtive URL.
   ** @param cfgAdmin Configuration admin service to use when updating
   **                 CM with data from the specified XML-file.
   ** @throws java.io.IOException
   ** @throws java.lang.IllegalArgumentException If the cfgAdmin is
   **   null or the root element of the XML-file is not named
   **   'cm_data', or if the XML parse failed.
   **/
  public static void handleCMData( InputStream is,
                                   String url,
                                   ConfigurationAdmin cfgAdmin )
    throws java.io.IOException
  {
    if (cfgAdmin==null)
      throw new IllegalArgumentException
        ("handleCMData called with null reference to the cfgAdmin service." );
    try {
      doParse( is, url, cfgAdmin );
    } catch (SAXException e) {
      throw new IllegalArgumentException
        ("XML parse failed (" + e + ")\n" + 
         XMLErrors.getErrorString(e, "<inputstream>"));
    }
  }//of handleCMData(InputStream,ConfigurationAdmin)

  /** 
   ** Parses a cm_data DTD XML-stream.
   **
   ** @param is XML stream to parse.
   ** @return  collection of org.osgi.service.cm.Configuration
   ** @throws java.io.IOException
   ** @throws java.lang.IllegalArgumentException If the root element 
   **   of the XML-file is not named 'cm_data', or if the XML parse failed.
   **/
  public static Collection parseCMData( InputStream is )
    throws java.io.IOException
  {
    try {
      return doParse( is, null, null );
    } catch (SAXException e) {
      throw new IllegalArgumentException
        ("XML parse failed (" + e + ")\n"
         +XMLErrors.getErrorString(e, "<inputstream>"));
    }
  }//of parseCMData(InputStream)


  /** Parses the cm_data file and updates CM if a cfgAdmin is given.
   ** @param  is Stream with XML to parse.
   ** @param url URL of the stream to parse, must be a correct
   **            value if the document contains an
   **            &lt;include&gt; element with a realtive URL.
   ** @param cfgAdmin Configuration admin service to use when updating
   **                 CM with data from the specified XML-file.
   ** @return a collection with all the configurations that was
   ** mentioned in the input stream.
   **/
  static Collection doParse( InputStream is,
                             String url,
                             ConfigurationAdmin cfgAdmin )
    throws org.xml.sax.SAXException, IOException
  {
    Parser parser = null;
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


    InputSource in = new InputSource(is);
    // The parser requires a system id on the input source in the case
    // of a relative system id in the doctype of the parsed XML file.
    // A dummy value is ok since we allways map the public ID to an
    // internal system UIR.
    in.setSystemId( "dummy" );
    in.setEncoding("UTF-8");// Default encoding to UTF-8 and not UTF8

    CMDataHandler handler = new CMDataHandler(parser, cfgAdmin, url);
    parser.setDocumentHandler(handler);
    parser.parse(in);
    return handler.getConfigurations();
  }// of doParse(InputStream,String,ConfigurationAdmin)



  //////////////////// Write cm_data file for given configurations.////////
  /** 
   ** Exports the specified configurations in a cm_data XML file.
   **
   ** @param configs Array with configurations to export.
   ** @param deleteAllOldConfigs If true start the generated file with
   **                an instruction to delete all existing configurations.
   ** @param output  An output stream to write to.
   **
   ** @throws java.io.IOException
   ** @throws IllegalArgumentException
   **/
  public static void exportCMData( Configuration[] configs,
                                   boolean deleteAllOldConfigs,
                                   OutputStream output )
    throws java.io.IOException
  {
    CMDataWriter.writeCMData( configs, deleteAllOldConfigs, false,
                              null, null, output );
  }

  //////////////////// Write cm_data file for given configurations.////////
  /** 
   ** Exports the specified configurations in a cm_data XML file.
   ** <p>
   ** The template saves a configuration where some data is left out
   ** so that it can be filled in by GDSPconf based on information
   ** from the system platform that the template is used on.
   ** </p>
   ** The follwoing data is left out
   ** <ul>
   **   <li>JRE to use.
   **   <li>SCRIPT_STYLE to use.
   **   <li>Default bundles direcotry (org.knopflerfish.gosg.jars).
   **   <li>The location for bundles located in the default bundles
   **       directory are rewitten (the part of the location that
   **       matches the default bundles directory is removed.
   ** </ul>
   **
   ** @param configs Array with configurations to export.
   ** @param deleteAllOldConfigs If true start the generated file with
   **                an instruction to delete all existing
   **                configurations. Also sets the mode of all
   **                (factory) configurations to 'new' and not 'update'. 
   ** @param template If true create an XML file suitable as a
   **                 GDSPconf template.
   ** @param cfgAdmin If <code>template</code> is true then a
   **                 configuration admin service is required. Used to
   **                 determine template details from configuration data.
   ** @param output   An output stream to write to.
   **
   ** @throws java.io.IOException
   **/
  public static void exportCMData( Configuration[] configs,
                                   boolean deleteAllOldConfigs,
                                   boolean template,
                                   ConfigurationAdmin cfgAdmin,
                                   OutputStream output )
    throws java.io.IOException
  {
    CMDataWriter.writeCMData( configs, deleteAllOldConfigs, template,
                              null, cfgAdmin, output );
    
  }//of exportCMData(Configuration[],boolean,boolean,ConfigurationAdmin,OutputStream)

  /** 
   ** Exports a configuration into a cm_data XML file.
   ** <p>
   ** If <code>pid != null</code>, factorypPid and ldapFilter are
   ** ignored and the properties contained in properties are exported
   ** into a non factory configuration.
   ** </p>
   **
   ** <p>
   ** If <code>pid == null</code>, a factory configuration will be
   ** exported. <code>factorypPid</code> and <code>ldapFilter</code>
   ** must in this case not be <code>null</code>.
   ** </p>
   **
   ** @param pid Configiration pid
   ** @param factoryPid Configuration factory pid
   ** @param ldapfilter Filter for the factory configuration
   ** @param properties Properties for the configuration
   ** @param output  An output stream to write to.
   **
   ** @throws java.io.IOException
   **/
  public static void exportCMData( String pid,
                                   String factoryPid,
                                   String ldapfilter,
                                   Dictionary properties,
                                   OutputStream output )
    throws java.io.IOException
  {
    Configuration[] cfgs = new Configuration[]{
      new CMDataHandler.ConfigurationImpl(pid,properties,factoryPid,null),
    };
    CMDataWriter.writeCMData( cfgs, false, false, ldapfilter, null, output );
  }//of exportCMData(String,String,String,Dictionary,OutputStream)


}// of class CMDataManager
