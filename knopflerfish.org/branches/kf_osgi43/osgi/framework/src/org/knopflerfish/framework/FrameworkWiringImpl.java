package org.knopflerfish.framework;

import java.util.Collection;
import java.util.HashSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;

public class FrameworkWiringImpl implements FrameworkWiring {

  private FrameworkContext fwCtx;

  FrameworkWiringImpl(FrameworkContext fwCtx) {
    this.fwCtx = fwCtx;
  }

  public Bundle getBundle() {
    return fwCtx.systemBundle;
  }

  public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
    Bundle[] bs = bundles != null ?
                  bundles.toArray(new Bundle[bundles.size()]) :
                  null;
    fwCtx.packageAdmin.refreshPackages(bs, listeners);
  }

  public boolean resolveBundles(Collection<Bundle> bundles) {
    Bundle[] bs = bundles != null ?
                  bundles.toArray(new Bundle[bundles.size()]) :
                  null;
    return fwCtx.packageAdmin.resolveBundles(bs);
  }

  public Collection<Bundle> getRemovalPendingBundles() {
    HashSet<Bundle> res = new HashSet<Bundle>();
    fwCtx.packages.findAllZombies(res);
    return res;
  }

  public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
    HashSet<Bundle> res = new HashSet<Bundle>(bundles);
    fwCtx.packages.packageClosure(res);
    // TODO - we only get package closure, need all dependencies
    return res;
  }

}
