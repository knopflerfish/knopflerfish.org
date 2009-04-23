/*
 * Copyright (c) 2009, KNOPFLERFISH project
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

import java.io.*;
import java.net.*;
import java.security.*;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class contains properties used by the framework
 */
public class FWProps  {


  public Debug debug;

  public final static String TRUE   = "true";
  public final static String FALSE  = "false";

  // If set to true, use strict rules for loading classes from the boot class loader.
  // If false, accept class loading from the boot class path from classes themselves
  // on the boot class, but which incorrectly assumes they may access all of the boot
  // classes on any class loader (such as the bundle class loader).
  // 
  // Setting this to TRUE will, for example, result in broken serialization on the Sun JVM
  // It's debatable what is the correct OSGi R4 behavior.
  public boolean STRICTBOOTCLASSLOADING = 
    FWProps.TRUE.equals(System.getProperty("org.knopflerfish.framework.strictbootclassloading", FWProps.FALSE));

  // EXIT_ON_SHUTDOWN and USING_WRAPPER_SCRIPT  must be initialized 
  // before initProperties(). Thus, they are *not* possible
  // to set on a per-framework basis (which wouldn't make sense anyway).
  final boolean EXIT_ON_SHUTDOWN =
    TRUE.equals(System.getProperty(Main.EXITONSHUTDOWN_PROP, TRUE));

  final boolean USING_WRAPPER_SCRIPT
    = TRUE.equals(System.getProperty(Main.USINGWRAPPERSCRIPT_PROP, FALSE));


  /**
   * The "System" properties for this framework instance.  Always use
   * <tt>FWProps.setProperty(String,String)</tt> to add values to
   * this map.
   */
  protected Map/*<String, String>*/ props
    = new HashMap/*<String, String>*/();
  
  /**
   * The set of properties that must not be present in props, since a
   * bundle is allowed to update them and such updates are required to
   * be visible when calling <tt>BundleContext.getProperty(String)</tt>.
   */
  private Set volatileProperties = new HashSet();


  // If set to true, then during the UNREGISTERING event the Listener
  // can use the ServiceReference to receive an instance of the service.
  public boolean UNREGISTERSERVICE_VALID_DURING_UNREGISTERING =
    TRUE.equals(System.getProperty("org.knopflerfish.servicereference.valid.during.unregistering",
                                   TRUE));
  
  // If set to true, set the bundle startup thread's context class
  // loader to the bundle class loader. This is useful for tests
  // but shouldn't really be used in production.
  public boolean SETCONTEXTCLASSLOADER =
    TRUE.equals(System.getProperty("org.knopflerfish.osgi.setcontextclassloader", FALSE));

  public boolean REGISTERSERVICEURLHANDLER =
    TRUE.equals(System.getProperty("org.knopflerfish.osgi.registerserviceurlhandler", TRUE));


  
  boolean bIsMemoryStorage /*= false*/;

  String whichStorageImpl;


  /**
   * Whether the framework supports extension bundles or not.
   * This will be false if bIsMemoryStorage is false.
   */
  boolean SUPPORTS_EXTENSION_BUNDLES;


  public static int javaVersionMajor = -1;
  public static int javaVersionMinor = -1;
  public static int javaVersionMicro = -1;

  static {
    String javaVersion = System.getProperty("java.version");
    // Value is on the form M.N.U_P[-xxx] where M,N,U,P are decimal integers
    if (null!=javaVersion) {
      int startPos = 0;
      int endPos   = 0;
      int max      = javaVersion.length();
      while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))) {
        endPos++;
      }
      if (startPos<endPos) {
        try {
          javaVersionMajor
            = Integer.parseInt(javaVersion.substring(startPos,endPos));
          startPos = endPos +1;
          endPos   = startPos;
          while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))) {
            endPos++;
          }
          if (startPos<endPos) {
            javaVersionMinor
              = Integer.parseInt(javaVersion.substring(startPos,endPos));
            startPos = endPos +1;
            endPos   = startPos;
            while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))){
              endPos++;
            }
            if (startPos<endPos) {
              javaVersionMicro
                = Integer.parseInt(javaVersion.substring(startPos,endPos));
            }
          }
        } catch (NumberFormatException _nfe) {
        }
      }
    }
  }

  /**
   * Is it safe to use double-checked locking or not.
   * It is safe if JSR 133 is included in the running JRE.
   * I.e., for Java SE if version is 1.5 or higher.
   */
  public final static boolean isDoubleCheckedLockingSafe
    = "true".equals(System.getProperty
                    ("org.knopflerfish.framework.is_doublechecked_locking_safe",
                     (javaVersionMajor>=1 && javaVersionMinor>=5
                      ? "true" : "false")));


  FrameworkImpl parent;
  
  public FWProps(FrameworkImpl parent) {
    this.parent = parent;    
    // See last paragraph of section 3.3.1 in the R4.0.1 and R4.1 core spec.
    volatileProperties.add(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
    initProperties();
    debug = new Debug(this);
  }



  /**
   * Retrieve the value of the named framework property.
   *
   */
  public String getProperty(String key) {
    return getProperty(key, null);
  }

  /**
   * Retrieve the value of the named framework property, with a default value.
   *
   */
  public String getProperty(String key, String def) {
    String v = (String)props.get(key);
    if(v != null) {
      return v;
    } else {
      // default to system property
      return System.getProperty(key, def);
    }
  }

  public void setProperty(String key, String val) {
    if (volatileProperties.contains(key)) {
      System.setProperty(key,val);
    } else {
      props.put(key, val);
    }
  }

  public void setProperties(Dictionary newProps) {
    for(Enumeration it = newProps.keys(); it.hasMoreElements(); ) {
      String key = (String)it.nextElement();
      setProperty(key, (String)newProps.get(key));
    }
  }

  public Dictionary getProperties(){
    Hashtable p = new Hashtable();
    p.putAll(System.getProperties());
    p.putAll(props);
    return p;
  }

  /**
   * Get a copy of the current system properties.
   */
  public java.util.Properties getSystemProperties() {
    return (java.util.Properties)System.getProperties().clone();
  }



  protected void initProperties() {
    props = new HashMap();
    
    whichStorageImpl = "org.knopflerfish.framework.bundlestorage." +
      getProperty("org.knopflerfish.framework.bundlestorage", "file") +
      ".BundleStorageImpl";
    
    bIsMemoryStorage = whichStorageImpl.equals("org.knopflerfish.framework.bundlestorage.memory.BundleStorageImpl");

    if (bIsMemoryStorage ||
        !EXIT_ON_SHUTDOWN ||
        !USING_WRAPPER_SCRIPT) {
      SUPPORTS_EXTENSION_BUNDLES = false;
      // we can not support this in this mode.
    } else {
      SUPPORTS_EXTENSION_BUNDLES = true;
    }
    // The name of the operating system of the hosting computer.
    setProperty(Constants.FRAMEWORK_OS_NAME, System.getProperty("os.name"));


    // The name of the processor of the hosting computer.
    setProperty(Constants.FRAMEWORK_PROCESSOR, System.getProperty("os.arch"));

    String ver = System.getProperty("os.version");
    String osVersion = null;
    if (ver != null) {
      int dots = 0;
      int i = 0;
      for ( ; i < ver.length(); i++) {
        char c = ver.charAt(i);
        if (Character.isDigit(c)) {
          continue;
        } else if (c == '.') {
          if (++dots < 3) {
            continue;
          }
        }
        break;
      }
      osVersion = ver.substring(0, i);
    }
    setProperty(Constants.FRAMEWORK_OS_VERSION, osVersion);
    setProperty(Constants.FRAMEWORK_VERSION,   FrameworkImpl.SPEC_VERSION);
    setProperty(Constants.FRAMEWORK_VENDOR,   "Knopflerfish");
    setProperty(Constants.FRAMEWORK_LANGUAGE,
                Locale.getDefault().getLanguage());

    // Various framework properties
    setProperty(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, TRUE);
    setProperty(Constants.SUPPORTS_FRAMEWORK_FRAGMENT, TRUE);
    setProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION,
                SUPPORTS_EXTENSION_BUNDLES ? TRUE : FALSE);
    setProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION,
                SUPPORTS_EXTENSION_BUNDLES ? TRUE : FALSE);

    Dictionary sysProps = getSystemProperties();

    setProperties(sysProps);
  }
}
