package org.knopflerfish.bundle.classpatcher;

import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

public class ClassPatcherWeavingHook implements WeavingHook {

  public void weave(WovenClass wc) {
    if(Activator.isBundlePatch()) {
        byte[] patchedBytes = ClassPatcher.getInstance(wc.getBundleWiring().getBundle()).patch(wc.getClassName(), wc.getBytes());
        wc.setBytes(patchedBytes);
    }
  }
}
