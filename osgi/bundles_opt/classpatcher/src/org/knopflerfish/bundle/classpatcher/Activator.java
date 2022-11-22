/*
 * Copyright (c) 2013-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.classpatcher;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class Activator implements BundleActivator {
  // We retain the old System property names from when this functionality was embedded
  // in the framework due to backward compatibility
  public final static String DEBUG_PATCH_PROP = "org.knopflerfish.framework.debug.patch";
  public final static String PATCH_PROP = "org.knopflerfish.framework.patch";
  public final static String PATCH_CONFIGURL_PROP = "org.knopflerfish.framework.patch.configurl";
  public final static String PATCH_DUMPCLASSES_PROP = "org.knopflerfish.framework.patch.dumpclasses";
  public final static String PATCH_DUMPCLASSES_DIR_PROP = "org.knopflerfish.framework.patch.dumpclasses.dir";
  
  static BundleContext bc;
  ServiceRegistration<WeavingHook> sr;
  static Debug debug;
  static private boolean bHasCheckedASM;
  static private boolean bHasASM;

  public void start(BundleContext context) {
    if (bc == null) {
      bc = context;
    }
    if (sr == null) {
      sr = bc.registerService(WeavingHook.class, new ClassPatcherWeavingHook(), null);
    }
    if (debug == null) {
      debug = new Debug();
    }
  }


  public void stop(BundleContext context) {
    if (sr != null) {
      sr.unregister();
    }
    if (bc != null) {
      bc = null;
    }
  }
  
  static boolean isBundlePatch() {
    if (!patchingEnabled()) {
      return false;
    }

    if (!bHasCheckedASM) {
      bHasCheckedASM = true;
      try {
        Class.forName("org.objectweb.asm.ClassReader");
        Activator.println("asm class found");
        bHasASM = true;
      } catch (Exception no_asm_class) {
        Activator.println("no asm class");
        bHasASM = false;
      }
    }

    return bHasASM && patchingEnabled();
  }
  
  static boolean patchingEnabled() {
    return "true".equalsIgnoreCase(bc.getProperty(PATCH_PROP));
  }
  
  static boolean dumpClasses() {
    return "true".equalsIgnoreCase(bc.getProperty(PATCH_DUMPCLASSES_PROP));
  }
  
  static String getDumpClassesDir() {
    return bc.getProperty(Activator.PATCH_DUMPCLASSES_DIR_PROP);
  }
  
  static String getConfigUrl() {
    return bc.getProperty(PATCH_CONFIGURL_PROP);
  }
  
  static boolean debugEnabled() {
    return "true".equalsIgnoreCase(bc.getProperty(DEBUG_PATCH_PROP));
    
  }

  static class Debug {
    private ThreadLocal<Boolean> insideDebug;

    /**
     * Check if we should use doPriviledged
     */
    private boolean useDoPrivileged() {
      if (System.getSecurityManager() != null) {
        if (insideDebug == null) {
          insideDebug = new ThreadLocal<Boolean>() {
            @Override
            protected synchronized Boolean initialValue() {
              return Boolean.FALSE;
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
      insideDebug.set(b);
    }

    /**
     * Are we already inside a debug print?
     */
    private boolean isInside() {
      return insideDebug.get();
    }

    /**
     * Common {@code println()} method for debug messages.
     *
     * @param str
     *          the message to print.
     */
    public void println(final String str) {
      printStackTrace(str,  null);
    }

    /**
     * The actual printStackTrace() implementation.
     *
     * @param str
     *          the message to print.
     * @param t
     *          the throwable to print a stack trace for.
     */
    private void printStackTrace0(final String str, final Throwable t) {
      System.err.println("## DEBUG: " + str);
      if (t != null) {
        t.printStackTrace();
      }
      if (t instanceof BundleException) {
        final Throwable n = ((BundleException) t).getNestedException();
        if (n != null) {
          System.err.println("Nested bundle exception:");
          n.printStackTrace();
        }
      }
    }

    /**
     * Common printStackTrace method for debug messages.
     *
     * @param str
     *          the message to print.
     * @param t
     *          the throwable to print a stack trace for.
     */
    public void printStackTrace(final String str, final Throwable t) {
      if (useDoPrivileged()) {
        // The call to this method can be made from a the framework on
        // behalf of a bundle that have no permissions at all assigned
        // to it.
        //
        // Use doPrivileged() here to protect the Framework from
        // PrintStream implementations that does not wrap calls needing
        // permissions in their own doPrivileged().
        if (!isInside()) {
          AccessController.doPrivileged((PrivilegedAction<Void>) () ->
              enterAndPrint(str, t));
        }
      } else {
        printStackTrace0(str, t);
      }
    }

    private Void enterAndPrint(String str, Throwable t) {
      inside(true);
      printStackTrace0(str, t);
      inside(false);
      return null;
    }
  }

  public static void println(String s) {
    System.out.println(s);
  }
}
