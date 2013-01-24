/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import org.osgi.framework.BundleException;

/**
 * Variables that controls debugging of the framework code.
 *
 * @author Jan Stein
 */
public class Debug {

  /**
   * Thread local storage to prevent recursive debug message
   * in permission checks
   */
  private ThreadLocal insideDebug;


  /**
   * Report Automanifest handling
   */
  public static String AUTOMANIFEST_PROP = "org.knopflerfish.framework.debug.automanifest";
  boolean automanifest;

  /**
   * When security is enabled, print information about resource
   * lookups that are rejected due to missing permissions for the
   * calling bundle.
   */
  public static String BUNDLE_RESOURCE_PROP = "org.knopflerfish.framework.debug.bundle_resource";
  boolean bundle_resource;

  /**
   * Report certificate matching
   */
  public static String CERTIFICATES_PROP = "org.knopflerfish.framework.debug.certificates";
  public boolean certificates;

  /**
   * Report whenever the bundle classloader does something.
   */
  public static String CLASSLOADER_PROP = "org.knopflerfish.framework.debug.classloader";
  boolean classLoader;

  /**
   * Report error handling events.
   */
  public static String ERRORS_PROP = "org.knopflerfish.framework.debug.errors";
  boolean errors;

  /**
   * Report framework create, init, start, stop
   */
  public static String FRAMEWORK_PROP = "org.knopflerfish.framework.debug.framework";
  boolean framework;

  /**
   * Report hooks handling
   */
  public static String HOOKS_PROP = "org.knopflerfish.framework.debug.hooks";
  boolean hooks;

  /**
   * Report triggering of lazy activation
   */
  public static String LAZY_ACTIVATION_PROP = "org.knopflerfish.framework.debug.lazy_activation";
  boolean lazy_activation;

  /**
   * Report LDAP handling
   */
  public static String LDAP_PROP = "org.knopflerfish.framework.debug.ldap";
  boolean ldap;

  /**
   * Report package handling events.
   */
  public static String PACKAGES_PROP = "org.knopflerfish.framework.debug.packages";
  boolean packages;

  /**
   * Report Class patching handling
   */
  public static String PATCH_PROP = "org.knopflerfish.framework.debug.patch";
  boolean patch;

  /**
   * Report permission handling
   */
  public static String PERMISSIONS_PROP = "org.knopflerfish.framework.debug.permissions";
  public boolean permissions;

  /**
   * When security is enabled, print information about service
   * reference lookups that are rejected due to missing permissions
   * for calling bundle.
   */
  public static String SERVICE_REFERENCE_PROP = "org.knopflerfish.framework.debug.service_reference";
  boolean service_reference;

  /**
   * Report startlevel.
   */
  public static String STARTLEVEL_PROP = "org.knopflerfish.framework.debug.startlevel";
  boolean startlevel;

  /**
   * Report url
   */
  public static String URL_PROP = "org.knopflerfish.framework.debug.url";
  boolean url;

  /**
   * Report warning handling events.
   */
  public static String WARNINGS_PROP = "org.knopflerfish.framework.debug.warnings";
  boolean warnings;



  public Debug(FWProps props) {
    props.setPropertyDefault(AUTOMANIFEST_PROP, FWProps.FALSE);
    props.setPropertyDefault(BUNDLE_RESOURCE_PROP, FWProps.FALSE);
    props.setPropertyDefault(CERTIFICATES_PROP, FWProps.FALSE);
    props.setPropertyDefault(CLASSLOADER_PROP, FWProps.FALSE);
    props.setPropertyDefault(ERRORS_PROP, FWProps.FALSE);
    props.setPropertyDefault(FRAMEWORK_PROP, FWProps.FALSE);
    props.setPropertyDefault(HOOKS_PROP, FWProps.FALSE);
    props.setPropertyDefault(LAZY_ACTIVATION_PROP, FWProps.FALSE);
    props.setPropertyDefault(PACKAGES_PROP, FWProps.FALSE);
    props.setPropertyDefault(PATCH_PROP, FWProps.FALSE);
    props.setPropertyDefault(PERMISSIONS_PROP, FWProps.FALSE);
    props.setPropertyDefault(SERVICE_REFERENCE_PROP, FWProps.FALSE);
    props.setPropertyDefault(STARTLEVEL_PROP, FWProps.FALSE);
    props.setPropertyDefault(URL_PROP, FWProps.FALSE);
    automanifest = props.getBooleanProperty(AUTOMANIFEST_PROP);
    bundle_resource = props.getBooleanProperty(BUNDLE_RESOURCE_PROP);
    certificates = props.getBooleanProperty(CERTIFICATES_PROP);
    classLoader = props.getBooleanProperty(CLASSLOADER_PROP);
    errors = props.getBooleanProperty(ERRORS_PROP);
    framework = props.getBooleanProperty(FRAMEWORK_PROP);
    hooks = props.getBooleanProperty(HOOKS_PROP);
    lazy_activation = props.getBooleanProperty(LAZY_ACTIVATION_PROP);
    packages = props.getBooleanProperty(PACKAGES_PROP);
    patch = props.getBooleanProperty(PATCH_PROP);
    permissions = props.getBooleanProperty(PERMISSIONS_PROP);
    service_reference = props.getBooleanProperty(SERVICE_REFERENCE_PROP);
    startlevel = props.getBooleanProperty(STARTLEVEL_PROP);
    url = props.getBooleanProperty(URL_PROP);
    warnings = props.getBooleanProperty(WARNINGS_PROP);
  }


  /**
   * Check if we should use doPriviledged
   */
  private boolean useDoPrivileged() {
    if (System.getSecurityManager() != null) {
      if (insideDebug == null) {
        insideDebug = new ThreadLocal() {
            protected synchronized Object initialValue() {
              return new Boolean(false);
            }
          };
      }
      return true;
    }
    return false;
  }


  /**
   * Are we already inside a debug print?
   */
  private void inside(boolean b) {
    insideDebug.set(new Boolean(b));
  }

  
  /**
   * Are we already inside a debug print?
   */
  private boolean isInside() {
    return ((Boolean) (insideDebug.get())).booleanValue();
  }

  
  /**
   * The actual println implementation.
   *
   * @param str the message to print.
   */
  private void println0(final String str) {
    System.err.println("## DEBUG: " + str);
  }

  /**
   * Common println method for debug messages.
   *
   * @param str the message to print.
   */
  public void println(final String str) {
    if(useDoPrivileged()) {
      // The call to this method can be made from a the framework on
      // behalf of a bundle that have no permissions at all assinged
      // to it.
      //
      // Use doPrivileged() here to protect the Framework from
      // PrintStream implementations that does not wrapp calls needing
      // premissions in their own doPrivileged().
      if (!isInside()) {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              inside(true);
              println0(str);
              inside(false);
              return null;
            }
          });
      }
    } else {
      println0(str);
    }
  }

  /**
   * The actual printStackTrace() implementation.
   *
   * @param str the message to print.
   * @param t   the throwable to print a stack trace for.
   */
  private void printStackTrace0(final String str, final Throwable t) {
    System.err.println("## DEBUG: " + str);
    t.printStackTrace();
    if (t instanceof BundleException) {
      Throwable n = ((BundleException)t).getNestedException();
      if (n != null) {
        System.err.println("Nested bundle exception:");
        n.printStackTrace();
      }
    }
  }

  /**
   * Common printStackTrace method for debug messages.
   *
   * @param str the message to print.
   * @param t   the throwable to print a stack trace for.
   */
  public void printStackTrace(final String str, final Throwable t) {
    if(useDoPrivileged()) {
      // The call to this method can be made from a the framework on
      // behalf of a bundle that have no permissions at all assinged
      // to it.
      //
      // Use doPrivileged() here to protect the Framework from
      // PrintStream implementations that does not wrapp calls needing
      // premissions in their own doPrivileged().
      if (!isInside()) {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              inside(true);
              printStackTrace0(str,t);
              inside(false);
              return null;
            }
          });
      }
    } else {
      printStackTrace0(str,t);
    }
  }

}
