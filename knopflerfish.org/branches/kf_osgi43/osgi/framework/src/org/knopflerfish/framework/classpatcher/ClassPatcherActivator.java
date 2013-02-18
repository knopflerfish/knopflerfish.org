package org.knopflerfish.framework.classpatcher;

import org.knopflerfish.framework.FWProps;
import org.knopflerfish.framework.FrameworkContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class ClassPatcherActivator implements BundleActivator {
  static FrameworkContext fc;
  static BundleContext bc;
  ServiceRegistration<?> sr;
  static private boolean bHasCheckedASM;
  static private boolean bHasASM;

  
  public ClassPatcherActivator(FrameworkContext c) {
    if(fc == null) {
      fc = c;
    }
  }
  
  public void start(BundleContext context) throws Exception {
    if(bc == null) {
      bc = context;
    }
    if(sr == null) {
      sr = bc.registerService(WeavingHook.class.getName(), new ClassPatcherWeavingHook(), null);
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
  
  static void println(String s) {
    System.out.println(s);
    
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

      if (fc.debug.patch) {
        fc.debug.println("ASM library: " + bHasASM);
      }
    }

    return bHasASM && fc.props.getBooleanProperty(FWProps.PATCH_PROP);
  }
}
