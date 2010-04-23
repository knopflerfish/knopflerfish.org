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

  FWProps props;

  /**
   * Thread local storage to prevent recursive debug message
   * in permission checks
   */
  private ThreadLocal insideDebug;


  /**
   * Report whenever the bundle classloader does something.
   */
  boolean classLoader;


  /**
   * Report event handling events.
   */
  boolean errors;


  /**
   * Report package handling events.
   */
  boolean packages;

  /**
   * Report startlevel.
   */
  boolean startlevel;

  /**
   * Report url
   */
  boolean url;

  /**
   * Report LDAP handling
   */
  boolean ldap;

  /**
   * When security is enabled, print information about service
   * reference lookups that are rejected due to missing permissions
   * for calling bundle.
   */
  boolean service_reference;

  /**
   * When security is enabled, print information about resource
   * lookups that are rejected due to missing permissions for the
   * calling bundle.
   */
  boolean bundle_resource;

  /**
   * When security is enabled, print information about context
   * lookups that are rejected due to missing permissions for the
   * calling bundle.
   */
  boolean bundle_context;

  /**
   * Report Class patching handling
   */
  boolean patch;

  /**
   * Report triggering of lazy activation
   */
  boolean lazyActivation;

  /**
   * Report framework create, init, start, stop
   */
  boolean framework;

  /**
   * Report Automanifest handling
   */
  boolean automanifest;

  /**
   * When security is enabled, use a doPrivileged() around
   * the actual call to System.out.println() to allow for PrintStream
   * implementations that does not handle the case with limited
   * priviledges themselfs.
   */
  boolean use_do_privilege;

  /**
   * Report hooks handling
   */
  boolean hooks;

  /**
   * Report permission handling
   */
  public boolean permissions;

  /**
   * Report certificate matching
   */
  public boolean certificates;


  public Debug(FWProps props) {
    this.props = props;
    classLoader =
      props.getProperty("org.knopflerfish.framework.debug.classloader", false);
    errors =
      props.getProperty("org.knopflerfish.framework.debug.errors", false);
    packages =
      props.getProperty("org.knopflerfish.framework.debug.packages", false);
    startlevel =
      props.getProperty("org.knopflerfish.framework.debug.startlevel", false);
    url =
      props.getProperty("org.knopflerfish.framework.debug.url", false);
    service_reference =
      props.getProperty("org.knopflerfish.framework.debug.service_reference", false);
    bundle_resource =
      props.getProperty("org.knopflerfish.framework.debug.bundle_resource", false);
    bundle_context =
      props.getProperty("org.knopflerfish.framework.debug.bundle_context", false);
    patch =
      props.getProperty("org.knopflerfish.framework.debug.patch", false);
    automanifest =
      props.getProperty("org.knopflerfish.framework.debug.automanifest", false);
    lazyActivation =
      props.getProperty("org.knopflerfish.framework.debug.lazy_activation", false);
    framework =
      props.getProperty("org.knopflerfish.framework.debug.framework", false);
    hooks =
      props.getProperty("org.knopflerfish.framework.debug.hooks", false);
    permissions =
      props.getProperty("org.knopflerfish.framework.debug.permissions", false);;
    certificates =
      props.getProperty("org.knopflerfish.framework.debug.certificates", false);
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
    System.out.println("## DEBUG: " + str);
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
    System.out.println("## DEBUG: " + str);
    t.printStackTrace();
    if (t instanceof BundleException) {
      Throwable n = ((BundleException)t).getNestedException();
      if (n != null) {
        System.out.println("Nested bundle exception:");
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