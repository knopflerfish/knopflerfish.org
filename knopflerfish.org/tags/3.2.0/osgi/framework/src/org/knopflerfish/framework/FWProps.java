/*
 * Copyright (c) 2009-2011, KNOPFLERFISH project
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

import java.net.URLClassLoader;
import java.util.*;

import org.osgi.framework.*;

/**
 * This class contains properties used by the framework
 */
public class FWProps {

  /**
   * Constants for knopflerfish framework properties
   */
  public final static String ALL_SIGNED_PROP = "org.knopflerfish.framework.all_signed";

  public final static String AUTOMANIFEST_PROP = "org.knopflerfish.framework.automanifest";

  public final static String AUTOMANIFEST_CONFIG_PROP = "org.knopflerfish.framework.automanifest.config";

  public final static String BUNDLESTORAGE_PROP = "org.knopflerfish.framework.bundlestorage";

  public final static String BUNDLESTORAGE_CHECKSIGNED_PROP = "org.knopflerfish.framework.bundlestorage.checksigned";

  public final static String PATCH_PROP = "org.knopflerfish.framework.patch";

  public final static String PATCH_CONFIGURL_PROP = "org.knopflerfish.framework.patch.configurl";

  public final static String PATCH_DUMPCLASSES_PROP = "org.knopflerfish.framework.patch.dumpclasses";

  public final static String PATCH_DUMPCLASSES_DIR_PROP = "org.knopflerfish.framework.patch.dumpclasses.dir";

  public final static String SERVICE_CONDITIONALPERMISSIONADMIN_PROP = "org.knopflerfish.framework.service.conditionalpermissionadmin";

  public final static String SERVICE_PERMISSIONADMIN_PROP = "org.knopflerfish.framework.service.permissionadmin";

  /**
   * Property specifying how bundle threads which are aborted should be handled.
   * Possible values are {@link BundleThread#ABORT_ACTION_STOP},
   * {@link BundleThread#ABORT_ACTION_MINPRIO},
   * {@link BundleThread#ABORT_ACTION_IGNORE}. The default value is
   * {@link BundleThread#ABORT_ACTION_IGNORE}.
   */
  public final static String BUNDLETHREAD_ABORT = "org.knopflerfish.framework.bundlethread.abort";

  /**
   * Name of system property for basic system packages to be exported. The
   * normal OSGi exports will be added to this list.
   */
  public final static String SYSTEM_PACKAGES_BASE_PROP = "org.knopflerfish.framework.system.packages.base";

  /**
   * Property name pointing to file listing of system-exported packages
   */
  public final static String SYSTEM_PACKAGES_FILE_PROP = "org.knopflerfish.framework.system.packages.file";

  /**
   * Property name for selecting exporting profile of system packages.
   */
  public final static String SYSTEM_PACKAGES_VERSION_PROP = "org.knopflerfish.framework.system.packages.version";

  public final static String IS_DOUBLECHECKED_LOCKING_SAFE_PROP = "org.knopflerfish.framework.is_doublechecked_locking_safe";

  public final static String LDAP_NOCACHE_PROP = "org.knopflerfish.framework.ldap.nocache";

  public final static String LISTENER_N_THREADS_PROP = "org.knopflerfish.framework.listener.n_threads";

  /**
   * If the Main-Class manifest attribute is set and this bundles location is
   * present in the value (comma separated list) of the Framework property named
   * org.knopflerfish.framework.main.class.activation then setup up a bundle
   * activator that calls the main-method of the Main-Class when the bundle is
   * started, and if the Main-Class contains a method named stop() call that
   * method when the bundle is stopped.
   */
  public final static String MAIN_CLASS_ACTIVATION_PROP = "org.knopflerfish.framework.main.class.activation";

  public final static String STRICTBOOTCLASSLOADING_PROP = "org.knopflerfish.framework.strictbootclassloading";

  public final static String VALIDATOR_PROP = "org.knopflerfish.framework.validator";

  public final static String SETCONTEXTCLASSLOADER_PROP = "org.knopflerfish.osgi.setcontextclassloader";

  public final static String REGISTERSERVICEURLHANDLER_PROP = "org.knopflerfish.osgi.registerserviceurlhandler";

  public final static String STARTLEVEL_USE_PROP = "org.knopflerfish.startlevel.use";

  /**
   * Set to true indicates startlevel compatability mode. all bundles and
   * current start level will be 1
   */
  public final static String STARTLEVEL_COMPAT_PROP = "org.knopflerfish.framework.startlevel.compat";

  /**
   * Name of special property containing a comma-separated list of all other
   * property names.
   */
  public static final String KEY_KEYS = "org.knopflerfish.framework.bundleprops.keys";

  /**
   * Common true string.
   */
  public final static String TRUE = "true";

  /**
   * Common false string.
   */
  public final static String FALSE = "false";

  // If set to true, use strict rules for loading classes from the
  // boot class loader. If false, accept class loading from the boot
  // class path from classes themselves on the boot class, but which
  // incorrectly assumes they may access all of the boot classes on
  // any class loader (such as the bundle class loader).
  //
  // Setting this to TRUE will, for example, result in broken
  // serialization on the Sun JVM It's debatable what is the correct
  // OSGi R4 behavior.
  public boolean STRICTBOOTCLASSLOADING;

  /**
   * The properties for this framework instance.
   */
  protected Map/* <String, String> */props = new Hashtable();

  /**
   * The default properties for this framework instance. TBD, maybe we should
   * make this JVM global!?
   */
  protected Map/* <String, String> */props_default = new Hashtable();

  // If set to true, then during the UNREGISTERING event the Listener
  // can use the ServiceReference to receive an instance of the service.
  public boolean UNREGISTERSERVICE_VALID_DURING_UNREGISTERING = true;

  // If set to true, set the bundle startup thread's context class
  // loader to the bundle class loader. This is useful for tests
  // but shouldn't really be used in production.
  public boolean SETCONTEXTCLASSLOADER = false;

  public boolean REGISTERSERVICEURLHANDLER = true;

  public static int javaVersionMajor = -1;
  public static int javaVersionMinor = -1;
  public static int javaVersionMicro = -1;

  static {
    String javaVersion = System.getProperty("java.version");
    // Value is on the form M.N.U_P[-xxx] where M,N,U,P are decimal integers
    if (null != javaVersion) {
      int startPos = 0;
      int endPos = 0;
      int max = javaVersion.length();
      while (endPos < max && Character.isDigit(javaVersion.charAt(endPos))) {
        endPos++;
      }
      if (startPos < endPos) {
        try {
          javaVersionMajor = Integer.parseInt(javaVersion.substring(startPos, endPos));
          startPos = endPos + 1;
          endPos = startPos;
          while (endPos < max && Character.isDigit(javaVersion.charAt(endPos))) {
            endPos++;
          }
          if (startPos < endPos) {
            javaVersionMinor = Integer.parseInt(javaVersion.substring(startPos, endPos));
            startPos = endPos + 1;
            endPos = startPos;
            while (endPos < max && Character.isDigit(javaVersion.charAt(endPos))) {
              endPos++;
            }
            if (startPos < endPos) {
              javaVersionMicro = Integer.parseInt(javaVersion.substring(startPos, endPos));
            }
          }
        } catch (NumberFormatException _nfe) {
        }
      }
    }
  }

  /**
   * Is it safe to use double-checked locking or not. It is safe if JSR 133 is
   * included in the running JRE. I.e., for Java SE if version is 1.5 or higher.
   */
  public boolean isDoubleCheckedLockingSafe;


  public FWProps(Map initProps, FrameworkContext fwCtx) {
    // Add explicitly given properties.
    props.putAll(initProps);

    // Setup Debug as early as possible.
    fwCtx.debug = new Debug(this);

    // Setup default (launch) OSGi properties, see OSGi R4 v4.2 sec 4.2.2
    initProperties(fwCtx);

    // Setup default KF framework properties
    initKFProperties();

    // Set up some instance variables that depends on the properties
    SETCONTEXTCLASSLOADER = getBooleanProperty(SETCONTEXTCLASSLOADER_PROP);
    REGISTERSERVICEURLHANDLER = getBooleanProperty(REGISTERSERVICEURLHANDLER_PROP);
    STRICTBOOTCLASSLOADING = getBooleanProperty(STRICTBOOTCLASSLOADING_PROP);
    isDoubleCheckedLockingSafe = getBooleanProperty(IS_DOUBLECHECKED_LOCKING_SAFE_PROP);
  }


  /**
   * Retrieve boolean value of the named framework property, with a default
   * value.
   * 
   */
  public boolean getBooleanProperty(String key) {
    String v = getProperty(key);
    if (v != null) {
      return TRUE.equalsIgnoreCase(v);
    }
    return false;
  }


  /**
   * Retrieve the value of the named framework property, with a default value.
   * 
   */
  public String getProperty(String key) {
    if (KEY_KEYS.equals(key)) {
      return makeKeys();
    }
    String v = (String)props.get(key);
    if (v == null) {
      v = System.getProperty(key);
      if (v == null) {
        // We know that we don't need to trim the result
        return (String)props_default.get(key);
      }
    }
    return v.trim();
  }


  public void setPropertyDefault(String key, String val) {
    // No need to save default if already have a value
    if (!props.containsKey(key)) {
      props_default.put(key, val);
    }
  }


  /**
   * Set property if not set to system property if it exists otherwise set to
   * supplied value.
   */
  public void setPropertyIfNotSet(String key, String val) {
    if (!props.containsKey(key)) {
      props.put(key, System.getProperty(key, val));
    }
  }


  public Dictionary getProperties() {
    Hashtable p = new Hashtable(props_default);
    p.putAll(System.getProperties());
    p.putAll(props);
    p.put(KEY_KEYS, makeKeys());
    return p;
  }


  protected String makeKeys() {
    StringBuffer sb = new StringBuffer();
    for (Iterator it = props.keySet().iterator(); it.hasNext();) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(it.next().toString());
    }
    for (Iterator it = props_default.keySet().iterator(); it.hasNext();) {
      sb.append(',');
      sb.append(it.next().toString());
    }
    return sb.toString();
  }


  /**
   * Create the default set of framework (launch) properties.
   */
  protected void initProperties(FrameworkContext fwCtx) {
    setPropertyIfNotSet(Constants.FRAMEWORK_BOOTDELEGATION, "");
    setPropertyIfNotSet(Constants.FRAMEWORK_BUNDLE_PARENT,
        Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
    setPropertyIfNotSet(Constants.FRAMEWORK_EXECPERMISSION, "");

    if (!props.containsKey(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)) {
      StringBuffer ee = new StringBuffer();
      // Always allow ee minimum
      ee.append("OSGi/Minimum-1.0");
      ee.append(",OSGi/Minimum-1.1");
      ee.append(",OSGi/Minimum-1.2");
      // Set up the default ExecutionEnvironment
      if (1 == javaVersionMajor) {
        for (int i = javaVersionMinor; i > 1; i--) {
          ee.append((i > 5) ? ",JavaSE-1." : ",J2SE-1.");
          ee.append(i);
        }
      }
      props.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee.toString());
    }

    setPropertyIfNotSet(Constants.FRAMEWORK_LANGUAGE, Locale.getDefault().getLanguage());
    setPropertyIfNotSet(Constants.FRAMEWORK_LIBRARY_EXTENSIONS, "");

    setPropertyIfNotSet(Constants.FRAMEWORK_OS_NAME,
        Alias.unifyOsName(System.getProperty("os.name")));

    if (!props.containsKey(Constants.FRAMEWORK_OS_VERSION)) {
      String ver = System.getProperty("os.version");
      int maj = 0;
      int min = 0;
      int mic = 0;
      String qual = null;
      if (ver != null) {
        // Convert os.version to a reasonable default
        try {
          StringTokenizer st = new StringTokenizer(ver.trim(), ".");
          maj = Integer.parseInt(st.nextToken());
          if (st.hasMoreTokens()) {
            qual = st.nextToken();
            min = Integer.parseInt(qual);
            qual = null;
            if (st.hasMoreTokens()) {
              qual = st.nextToken();
              mic = Integer.parseInt(qual);
              qual = null;
              if (st.hasMoreTokens()) {
                qual = st.nextToken();
              }
            }
          }
        } catch (Exception ignore) {
        }
      }
      Version osVersion;
      try {
        osVersion = new Version(maj, min, mic, qual);
      } catch (IllegalArgumentException skip) {
        osVersion = new Version(maj, min, mic, null);
      }
      props.put(Constants.FRAMEWORK_OS_VERSION, osVersion.toString());
    }

    setPropertyIfNotSet(Constants.FRAMEWORK_PROCESSOR,
        Alias.unifyProcessor(System.getProperty("os.arch")));

    setPropertyIfNotSet(Constants.FRAMEWORK_SECURITY, "");

    setPropertyIfNotSet(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "1");

    setPropertyIfNotSet(Constants.FRAMEWORK_STORAGE, "fwdir");

    setPropertyIfNotSet(Constants.FRAMEWORK_STORAGE_CLEAN, "");

    // NYI, fill this with values
    setPropertyIfNotSet(Constants.FRAMEWORK_SYSTEMPACKAGES, "");

    setPropertyIfNotSet(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "");

    setPropertyIfNotSet(Constants.FRAMEWORK_TRUST_REPOSITORIES, "");

    // NYI
    setPropertyIfNotSet(Constants.FRAMEWORK_WINDOWSYSTEM, "");

    // Impl. constants
    props.put(Constants.FRAMEWORK_VERSION, FrameworkContext.SPEC_VERSION);
    props.put(Constants.FRAMEWORK_VENDOR, "Knopflerfish");
    props.put(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, TRUE);
    props.put(Constants.SUPPORTS_FRAMEWORK_FRAGMENT, TRUE);
    // Only first framework support framework extension
    // NYI! Improve this in the future
    props.put(Constants.SUPPORTS_FRAMEWORK_EXTENSION,
        getClass().getClassLoader() instanceof URLClassLoader && fwCtx.id == 0 ? TRUE : FALSE);
    // Only first framework can support bootclasspath extension
    // NYI! Improve this in the future
    setPropertyIfNotSet(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION, FALSE);
    if (getBooleanProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION)
        && !(getClass().getClassLoader() instanceof URLClassLoader && fwCtx.id == 1)) {
      props.put(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION, FALSE);
    }
  }


  /**
   * Create the default set of KF specific framework properties.
   */
  protected void initKFProperties() {
    setPropertyDefault(ALL_SIGNED_PROP, FALSE);
    setPropertyDefault(AUTOMANIFEST_PROP, FALSE);
    setPropertyDefault(AUTOMANIFEST_CONFIG_PROP, "!!/automanifest.props");
    setPropertyDefault(BUNDLESTORAGE_PROP, "file");
    setPropertyDefault(BUNDLESTORAGE_CHECKSIGNED_PROP, TRUE);
    setPropertyDefault(PATCH_PROP, FALSE);
    setPropertyDefault(PATCH_CONFIGURL_PROP, "!!/patches.props");
    setPropertyDefault(PATCH_DUMPCLASSES_PROP, FALSE);
    setPropertyDefault(PATCH_DUMPCLASSES_DIR_PROP, "patchedclasses");
    setPropertyDefault(SERVICE_CONDITIONALPERMISSIONADMIN_PROP, TRUE);
    setPropertyDefault(SERVICE_PERMISSIONADMIN_PROP, TRUE);
    setPropertyDefault(SYSTEM_PACKAGES_BASE_PROP, "");
    setPropertyDefault(SYSTEM_PACKAGES_FILE_PROP, "");
    setPropertyDefault(SYSTEM_PACKAGES_VERSION_PROP, Integer.toString(javaVersionMajor) + "."
        + javaVersionMinor);
    setPropertyDefault(IS_DOUBLECHECKED_LOCKING_SAFE_PROP, javaVersionMajor >= 1
        && javaVersionMinor >= 5 ? TRUE : FALSE);
    setPropertyDefault(LDAP_NOCACHE_PROP, FALSE);
    setPropertyDefault(MAIN_CLASS_ACTIVATION_PROP, "");
    setPropertyDefault(STRICTBOOTCLASSLOADING_PROP, FALSE);
    setPropertyDefault(VALIDATOR_PROP, getProperty(Constants.FRAMEWORK_TRUST_REPOSITORIES)
        .length() > 0 ? "JKSValidator" : "none");
    setPropertyDefault(SETCONTEXTCLASSLOADER_PROP, FALSE);
    setPropertyDefault(REGISTERSERVICEURLHANDLER_PROP, TRUE);
    setPropertyDefault(STARTLEVEL_COMPAT_PROP, FALSE);
    setPropertyDefault(STARTLEVEL_USE_PROP, TRUE);
  }

}