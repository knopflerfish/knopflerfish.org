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
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

// Gatespace packages

// Standard Java packages
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 ** Writes a set of configuartions formated according to the cm_data
 ** DTD to an output stream.
 **
 ** @author Gatespace
 ** @version $Id: CMDataWriter.java,v 1.1.1.1 2004/03/05 20:34:51 wistrand Exp $
 **/
class CMDataWriter
  implements CMDataNames
{

  private Configuration[] cfgs;
  private boolean deleteAllOldConfigs;
  private boolean template;
  private String factoryLdapFilter;
  private ConfigurationAdmin cfgAdm;
  private PrintWriter pw;
  
  private CMDataWriter(Configuration[] cfgs,
                       boolean deleteAllOldConfigs,
                       boolean template,
                       String factoryLdapFilter,
                       ConfigurationAdmin cfgAdmin,
                       PrintWriter pw ) 
  {
    this.cfgs     = cfgs;
    this.template = template;
    this.cfgAdm   = cfgAdmin;
    this.pw       = pw;
    this.deleteAllOldConfigs = deleteAllOldConfigs;
    this.factoryLdapFilter   = factoryLdapFilter;
  }
  

  /** Configuration keys that should not be saved (autocreated by CM).*/
  private static Hashtable ignoredKeyNames = new Hashtable();
  static {
    ignoredKeyNames.put( Constants.SERVICE_PID, Constants.SERVICE_PID );
    ignoredKeyNames.put( FACTORY_PID, FACTORY_PID );
    ignoredKeyNames.put( BUNDLE_LOCATION, BUNDLE_LOCATION );
  }
  
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
   ** @param configs  Array with configurations to export.
   ** @param deleteAllOldConfigs If true start the generated file with
   **                 an instruction to delete all existing configurations.
   ** @param template If true create an XML file suitable as a
   **                 GDSPconf template.
   ** @param factoryldapfilter If non-null factoryconfiguration
   **                 elements are written this as the ldapfilter
   **                 attribute. 
   ** @param cfgAdmin If <code>template</code> is true then a
   **                 configuration admin service is required. Used to
   **                 determine template details from configuration data.
   ** @param output   An output stream to write to.
   **
   ** @throws java.io.IOException
   **/
  static void writeCMData( Configuration[] configs,
                           boolean deleteAllOldConfigs,
                           boolean template,
                           String factoryldapfilter,
                           ConfigurationAdmin cfgAdmin,
                           OutputStream output )
    throws java.io.IOException
  {
    PrintWriter pw
      = new PrintWriter( new OutputStreamWriter(output, "ISO-8859-1"));
    CMDataWriter cmdw = new CMDataWriter
      ( configs, deleteAllOldConfigs, template,
        factoryldapfilter, cfgAdmin, pw );
    cmdw.write();
  }
  
  
  private void write()
  {
    pw.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    pw.println();
    
    pw.print("<!DOCTYPE ");
    pw.print(CM_DATA_ROOT_NAME); 
    pw.print(" PUBLIC '");
    pw.print(CM_DATA_0_1_ID);
    pw.print("' '");
    pw.print(CM_DATA_0_1_URI);
    pw.print("'>");
    pw.println();
    pw.println();

    pw.print("<");
    pw.print(CM_DATA_ROOT_NAME);
    pw.print(" ");
    pw.print(CM_DATA_VERSION_ANAME);
    pw.print("=\"0.1\">");
    pw.println();

    if (deleteAllOldConfigs) {
      pw.print("  <");
      pw.print(CM_DATA_FILTER_NAME);
      pw.print(" ldapfilter=\"\" mode=\"delete\" />");
      pw.println();
    }
    
    for (int i=0; i<cfgs.length; i++ ) {
      if (cfgs[i].getFactoryPid()!=null) {
        writeFactoryCfg(cfgs[i]);
      } else {
        writeCfg(cfgs[i]);
      }
    }

    pw.print("</");
    pw.print(CM_DATA_ROOT_NAME);
    pw.print(">");
    pw.println();

    pw.flush();
  }//of write()
 

  private void writeCfg( Configuration cfg ) 
  {
    pw.print("  <");
    pw.print(CM_DATA_CONFIGRUATION_NAME);
    pw.print(" pid=\"" );
    pw.print(cfg.getPid());
    pw.print("\" mode=\"");
    pw.print( deleteAllOldConfigs ? "new" : "update" );
    pw.print("\">");
    pw.println();

    writeProperties
      ( template ? doTemplateFilterCfg(cfg) : cfg.getProperties() );
    
    pw.print("  </");
    pw.print(CM_DATA_CONFIGRUATION_NAME);
    pw.print(">");
    pw.println();
  }

  private void writeFactoryCfg( Configuration cfg ) 
  {
    pw.print("  <");
    pw.print(CM_DATA_FACTORYCONFIGRUATION_NAME);
    pw.print(" factorypid=\"" );
    pw.print(cfg.getFactoryPid());
    pw.print("\" ");
    if (deleteAllOldConfigs) {
      pw.print("mode=\"new\"");
    } else if (factoryLdapFilter!=null) {
      pw.print("ldapfilter=\"");
      pw.print(factoryLdapFilter);
      pw.print("\" mode=\"update\"");
    } else if (cfg.getPid()!=null) {      
      pw.print("ldapfilter=\"(");
      pw.print(Constants.SERVICE_PID);
      pw.print("=");
      pw.print(cfg.getPid());
      pw.print(")\" mode=\"update\"");
    } else {      
      pw.print(")\" mode=\"new\"");
    }
    pw.println(">");

    writeProperties
      ( template ? doTemplateFilterFactoryCfg(cfg) : cfg.getProperties() );
    
    pw.print("  </");
    pw.print(CM_DATA_FACTORYCONFIGRUATION_NAME);
    pw.print(">");
    pw.println();
  }

  private void writeProperties( Dictionary properties )
  {
    for (Enumeration keyEnum = properties.keys(); keyEnum.hasMoreElements();) {
      final String key = (String) keyEnum.nextElement();
      if (!ignoredKeyNames.containsKey(key))
        writeProperty( key, properties.get(key) );
    }
  }
  

  private void writeProperty( String name, Object value )
  {
    pw.print("    <");
    pw.print(CM_DATA_PROPERTY_NAME);
    pw.print(" name=\"" );
    pw.print(name);
    pw.print("\">");
    pw.println();

    writeValue( value, "      " );
    
    pw.print("    </");
    pw.print(CM_DATA_PROPERTY_NAME);
    pw.print(">");
    pw.println();
  }
  

  private void writeValue( Object value, String indent )
  {
    if (value.getClass().isArray()) {
      if (value.getClass().getComponentType().isPrimitive()) {
        writePrimitiveArray( value, indent );
      } else {
        writeArray( (Object[]) value, indent );
      }
    } else if (value.getClass()==Vector.class) {
      writeVector( (Vector) value, indent );
    } else {
      pw.print(indent);
      pw.print("<");
      pw.print(CM_DATA_VALUE_NAME);
      pw.print(" type=\"" );
      pw.print(getCmType(value.getClass()));
      pw.print("\">");
      pw.print(value.toString());
      pw.print("</");
      pw.print(CM_DATA_VALUE_NAME);
      pw.print(">");
      pw.println();
    }
  }
  
  private void writeVector( Vector v, String indent )
  {
    pw.print(indent);
    pw.print("<");
    pw.print(CM_DATA_VECTOR_NAME);
    pw.print(" length=\"" );
    pw.print(v.size());
    pw.print("\">");
    pw.println();

    String indent2 = indent + "  ";
    for (int i=0, size=v.size(); i<size; i++) {
      writeValue( v.elementAt(i), indent2 );
    }

    pw.print(indent);
    pw.print("</");
    pw.print(CM_DATA_VECTOR_NAME);
    pw.print(">");
    pw.println();
  }
  
  private void writeArray( Object[] a, String indent )
  {
    pw.print(indent);
    pw.print("<");
    pw.print(CM_DATA_ARRAY_NAME);
    pw.print(" length=\"" );
    pw.print(a.length);
    pw.print("\" elementType=\"" );
    pw.print(getElemType(a.getClass().getComponentType()));
    pw.print("\">");
    pw.println();

    String indent2 = indent + "  ";
    for (int i=0, size=a.length; i<size; i++) {
      writeValue( a[i], indent2 );
    }

    pw.print(indent);
    pw.print("</");
    pw.print(CM_DATA_ARRAY_NAME);
    pw.print(">");
    pw.println();
  }
  
  private void writePrimitiveArray( Object a, String indent )
  {
    Class componentClass = a.getClass().getComponentType();
    int length = Array.getLength(a);

    pw.print(indent);
    pw.print("<");
    pw.print(CM_DATA_ARRAY_NAME);
    pw.print(" length=\"" );
    pw.print(length);
    pw.print("\" elementType=\"" );
    pw.print(getElemType(componentClass));
    pw.print("\">");
    pw.println();

    String indent2 = indent + "  ";
    for (int i=0; i<length; i++) {
      pw.print(indent2);
      pw.print("<");
      pw.print(CM_DATA_PRIMITIVEVALUE_NAME);
      pw.print(" type=\"" );
      pw.print(componentClass.toString());
      pw.print("\">");
      pw.print(Array.get(a,i).toString());
      pw.print("</");
      pw.print(CM_DATA_PRIMITIVEVALUE_NAME);
      pw.print(">");
      pw.println();
    }

    pw.print(indent);
    pw.print("</");
    pw.print(CM_DATA_ARRAY_NAME);
    pw.print(">");
    pw.println();
  }
  

  private String getCmType( Class vClass ) {
    final String className = vClass.getName();
    return className.substring( className.lastIndexOf(".")+1 );
  }

  private String getElemType( Class vClass ) {
    return vClass.isArray()
      ? getElemType( vClass.getComponentType() ) +"[]"
      : getCmType( vClass );
  }


  /** PID of the configuration owned by the platform configuration
   ** bundle on behalf of GDSPconf.**/
  private static final String GDSP_PID = "org.knopflerfish.bundle.gdsp.bundle";
  

  /** Do template filtering of a non-factory configuration.
   ** Only updates the GDSPconf configuration.
   **/
  private Dictionary doTemplateFilterCfg( Configuration cfg  )
  {
    Dictionary dict = cfg.getProperties();
    if (cfg.getPid().equals(GDSP_PID)) {
      // Remove JRE choice so that the default will be used
      dict.remove("JRE");
      // Remove script styl so that the default will be used.
      dict.remove("SCRIPT_STYLE");
      // Remove TOP_DIR so that the default will be used.
      dict.remove("org.knopflerfish.gdsp.topdir");
      // Remove INST_DIR so that the default will be used.
      dict.remove("org.knopflerfish.gdspconf.instdir");
      // Remove INST_DIR_USER so that the default will be used.
      dict.remove("org.knopflerfish.gdspconf.instdir.user");
      // Remove CM_DIR so that the default will be used.
      dict.remove("org.knopflerfish.bundle.cm.store");
      // Remove FW_DIR so that the default will be used.
      dict.remove("org.osgi.framework.dir");
      // Remove BIN_DIR so that the default will be used.
      dict.remove("org.knopflerfish.gdspconf.bindir");
      // Remove KERNEL so that the default will be used.
      dict.remove("KERNEL");
      // Remove CLASSPATH so that the default will be used.
      dict.remove("CLASSPATH");
      // Remove PATFORM_NAME so that the default will be used.
      dict.remove("org.knopflerfish.gosg.name");
      // Remove XARGS so that the default will be used.
      dict.remove("org.knopflerfish.gdspconf.xargs");
      // Remove XARGS_INIT so that the default will be used.
      dict.remove("org.knopflerfish.gdspconf.xargs.init");
      // Remove definition of the defaultJars path.
      Object val = dict.remove("org.knopflerfish.gosg.jars");
      String[] jarDirURLs = (val instanceof String[])
        ? (String[]) val
        : new String[]{ (String) val };
      
      // If the bundle location starts with a directory URL from
      // jarDirs remove that directory URL from the location so
      // that the current jars directory path will be used when the
      // template is loaded.
      final Object bl = dict.get("BUNDLE_LIST");
      boolean is2d = bl instanceof String[][];
      final String[]   bl1d = is2d ? null : (String[]) bl;
      final String[][] bl2d = is2d ? (String[][]) bl : null;
      final int blLength    = is2d ? bl2d.length : bl1d.length;
      for (int i=0; i<blLength; i++) {
        String location = is2d ? bl2d[i][0] : bl1d[i];
        for (int j=0; j<jarDirURLs.length; j++) {
          if (location.startsWith(jarDirURLs[j])) {
            if (is2d) {
              bl2d[i][0] = location.substring(jarDirURLs[j].length());
            } else {
              bl1d[i] = location.substring(jarDirURLs[j].length());
            }
            break;
          }
        }
      }
    }
    return dict;
  }


  /** Factory PID used by the bundle manager for the entries in the
   ** bundles list.**/
  private static final String BMF_PID
    = "org.knopflerfish.bundle.bundlemanager.factory.BundleEntry";
  
  

  /** Do template filtering of a factory configuration.
   ** Only updates BundleEntry configurations.
   **/
  private Dictionary doTemplateFilterFactoryCfg( Configuration cfg )
  {
    Dictionary dict = cfg.getProperties();
    if (cfg.getFactoryPid().equals(BMF_PID)) {
      // If the bundle location starts with a directory URL that
      // belongs to the JARS path remove it 

      // First get the GDSPconf configuration and extract the bundles
      // directory URLs path.
      String[] jarDirURLs = null;
      try {
        Configuration[] cfgs = cfgAdm.listConfigurations
          ( "(" +Constants.SERVICE_PID +"="+GDSP_PID +")" );
        Object val = cfgs[0].getProperties().get("org.knopflerfish.gosg.jars");
        if (val!=null) {
          jarDirURLs = (val instanceof String[])
            ? (String[]) val
            : new String[]{ (String) val };
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (jarDirURLs==null) {
        // Failed to get info from the GDSPCong cfg; fallback to the
        // corresponding system property.
        String jars = System.getProperty("org.knopflerfish.gosg.jars","");
        StringTokenizer jarsST = new StringTokenizer(jars,";");
        jarDirURLs = new String[jarsST.countTokens()];
        for (int i=0; i<jarDirURLs.length; i++) {
          jarDirURLs[i] = jarsST.nextToken().trim();
        }
      }
      String location = (String) dict.get("location");
      for (int i=0; i<jarDirURLs.length; i++) {
        if (location.startsWith(jarDirURLs[i])) {
          dict.put( "location", location.substring(jarDirURLs[i].length()) );
          break;
        }
      }
    }
    return dict;
  }

}// of class CMDataManager
