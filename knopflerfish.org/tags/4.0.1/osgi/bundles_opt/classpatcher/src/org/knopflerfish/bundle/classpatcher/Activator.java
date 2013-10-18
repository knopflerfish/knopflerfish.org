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
  ServiceRegistration<?> sr;
  static Debug debug;
  static private boolean bHasCheckedASM;
  static private boolean bHasASM;

  public void start(BundleContext context) throws Exception {
    if(bc == null) {
      bc = context;
    }
    if(sr == null) {
      sr = bc.registerService(WeavingHook.class.getName(), new ClassPatcherWeavingHook(), null);
    }
    if(debug == null) {
      debug = new Debug();
    }
  }


  public void stop(BundleContext context) throws Exception {
    if(sr != null) {
      sr.unregister();
    }
    if(bc != null) {
      bc = null;
    }
  }
  
  static boolean isBundlePatch() {
    if (!bHasCheckedASM) {
      try {
        Class.forName("org.objectweb.asm.ClassReader");
        bHasASM = true;
      } catch (Exception no_asm_class) {
        bHasASM = false;
      }
      bHasCheckedASM = true;
    }

    return bHasASM && patchingEnabled();
  }
  
  static boolean patchingEnabled() {
    return "true".equalsIgnoreCase(bc.getProperty(PATCH_PROP));
  }
  
  static boolean dumpClasses() {
    return "true".equalsIgnoreCase(PATCH_DUMPCLASSES_PROP);
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
      return (insideDebug.get()).booleanValue();
    }

    /**
     * The actual println implementation.
     * 
     * @param str
     *          the message to print.
     */
    private void println0(final String str) {
      System.err.println("## DEBUG: " + str);
    }

    /**
     * Common {@code println()} method for debug messages.
     * 
     * @param str
     *          the message to print.
     */
    public void println(final String str) {
      if (useDoPrivileged()) {
        // The call to this method can be made from a the framework on
        // behalf of a bundle that have no permissions at all assigned
        // to it.
        //
        // Use doPrivileged() here to protect the Framework from
        // PrintStream implementations that does not wrap calls needing
        // permissions in their own doPrivileged().
        if (!isInside()) {
          AccessController.doPrivileged(new PrivilegedAction<Object>() {
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
     * @param str
     *          the message to print.
     * @param t
     *          the throwable to print a stack trace for.
     */
    private void printStackTrace0(final String str, final Throwable t) {
      System.err.println("## DEBUG: " + str);
      t.printStackTrace();
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
          AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
              inside(true);
              printStackTrace0(str, t);
              inside(false);
              return null;
            }
          });
        }
      } else {
        printStackTrace0(str, t);
      }
    }
  }

  public static void println(String s) {
    System.out.println(s); 
  }
}
