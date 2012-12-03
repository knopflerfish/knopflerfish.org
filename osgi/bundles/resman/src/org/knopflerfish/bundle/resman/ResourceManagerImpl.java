package org.knopflerfish.bundle.resman;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.lang.ClassLoader;

import org.osgi.framework.Bundle;

import org.knopflerfish.framework.BundleClassLoader;
import org.knopflerfish.framework.ExtensionContext;

import mika.max.ResourceMonitor;

import org.knopflerfish.service.resman.ResourceManager;
import org.knopflerfish.service.resman.BundleMonitor;

public class ResourceManagerImpl implements ResourceManager {
  ExtensionContext extCtx;
  HashMap monitors;

  public ResourceManagerImpl(ExtensionContext extCtx) {
    this.extCtx = extCtx;
    monitors = new HashMap();
 }


  public BundleMonitorImpl monitor(BundleClassLoader bcl) {
    ResourceMonitor resmon = new ResourceMonitor(bcl);
    resmon.enableMemoryMonitoring(10000000);
    resmon.enableThreadCountMonitoring(20);
    resmon.enableThreadCpuMonitoring(100);
    BundleMonitorImpl bmi = new BundleMonitorImpl(bcl, resmon);
    monitors.put(bcl.getBundle(), bmi);
    // System.out.println("here I am");
    return bmi;
  }

  void unmonitor(ClassLoader bcl) {
    monitors.remove(bcl);
  }

  void printMonitors() {
    Collection c = monitors.values();
    for (Iterator i = c.iterator(); i.hasNext(); ) {
      BundleMonitor bmon = (BundleMonitor)i.next();
      System.out.println("Memory: " +bmon.getMemory() + " Threads: " + bmon.getThreadCount());
    }
  }

  public BundleMonitor monitor(Bundle b) {
    final ClassLoader cl = extCtx.getClassLoader(b);

    return (cl instanceof BundleClassLoader)
      ? monitor( (BundleClassLoader) cl)
      : null;
  }

  public void unmonitor(Bundle b) {
    // monitor(((BundleImpl)b).gen.getClassLoader());
    // monitors.remove(b);
  }

  public Collection getMonitors() {
    return monitors.values();
  }

  public BundleMonitor getMonitor(Bundle b) {
    return (BundleMonitor)monitors.get(b);
  }

}



class BundleMonitorImpl implements BundleMonitor {
  BundleClassLoader bcl;
  ResourceMonitor resmon;

  BundleMonitorImpl(BundleClassLoader bcl, ResourceMonitor resmon) {
    this.bcl = bcl;
    this.resmon = resmon;
  }

  public Bundle getBundle() {
    return bcl.getBundle();
  }

  public long getMemory() {
    return resmon.getCurrentMemory();
  }
  public int getThreadCount() {
    return resmon.getCurrentThreadCount();
  }

  public int getCPU() {
    return resmon.getCurrentThreadCpu();
  }

  public long getMemoryLimit() {
    return resmon.getMaxMemory();
  }
  public int getThreadCountLimit() {
    return resmon.getMaxThreadCount();
  }
  public int getCPULimit() {
    return resmon.getMaxThreadCpu();
  }
}
