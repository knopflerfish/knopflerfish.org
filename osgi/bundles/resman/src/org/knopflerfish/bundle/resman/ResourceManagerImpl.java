package org.knopflerfish.bundle.resman;

import java.lang.ClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import mika.max.ResourceMonitor;

import org.osgi.framework.Bundle;

import org.knopflerfish.framework.BundleClassLoader;
import org.knopflerfish.framework.ExtensionContext;

import org.knopflerfish.service.resman.ResourceManager;
import org.knopflerfish.service.resman.BundleMonitor;
import org.knopflerfish.service.resman.BundleRevisionMonitor;

/**
 * Resource manager implementation for Mika Max.
 *
 * @author Makewave AB.
 */
public class ResourceManagerImpl implements ResourceManager {

  // Note: All access to monitors map must be synchronized to avoid
  // raise between adding and removing monitoring of bundle class
  // loaders.
  final HashMap /*<Bundle,BundleMonitorImpl>*/ monitors = new HashMap();

  public ResourceManagerImpl() {
  }

  BundleMonitorImpl monitor(final BundleClassLoader bcl) {
    final Bundle b  = bcl.getBundle();
    BundleMonitorImpl bmi = null;

    synchronized(monitors) {
      bmi = (BundleMonitorImpl) monitors.get(b);
      if (bmi == null) {
        bmi = new BundleMonitorImpl(b, bcl);
        monitors.put(bmi.getBundle(), bmi);
      } else  {
        bmi.addClassLoader(bcl);
      }
    }

    return bmi;
  }

  void unmonitor(final BundleClassLoader bcl) {
    final Bundle b  = bcl.getBundle();
    synchronized(monitors) {
      final BundleMonitorImpl bmi = (BundleMonitorImpl) monitors.get(b);
      if (bmi != null) {
        if (bmi.removeClassLoader(bcl)) {
          monitors.remove(bcl);
        }
      }
    }
  }

  public BundleMonitor monitor(final Bundle b) {
    final ClassLoader cl = ExtActivator.extCtx.getClassLoader(b);

    return (cl instanceof BundleClassLoader)
      ? monitor( (BundleClassLoader) cl)
      : null;
  }

  public void unmonitor(final Bundle b) {
    synchronized(monitors) {
      final BundleMonitorImpl bmi = (BundleMonitorImpl) monitors.remove(b);
      if (bmi!=null) {
        bmi.removeAlllClassLoaders();
      }
    }
  }

  // Unmonitor all bundles.
  public void unmonitor() {
    synchronized(monitors) {
      for (Iterator i = monitors.keySet().iterator(); i.hasNext(); ) {
        final Bundle b = (Bundle) i.next();
        unmonitor(b);
      }
    }
  }

  public Collection getMonitors() {
    return Collections.unmodifiableCollection(monitors.values());
  }

  public BundleMonitor getMonitor(final Bundle b) {
    synchronized(monitors) {
      return (BundleMonitor) monitors.get(b);
    }
  }
}


////////////////////////////////////////////////////////////
// BundleMonitorImpl
////////////////////////////////////////////////////////////
class BundleMonitorImpl implements BundleMonitor {
  int memoryLimit = 10000000;
  int threadCountLimit = 20;
  int cpuLimit = 100;

  final Bundle bundle;

  /** The current bundle revision resource monitor. */
  BundleRevisionMonitorImpl curBRM = null;

  /**
   * List of bundle class loaders and their associated bundle revision
   * monitors.
   */
  final Map /*<BundleClassLoader,BundleRevisionMonitorImpl>*/ monitors
    = new LinkedHashMap();

  BundleMonitorImpl(final Bundle bundle, final BundleClassLoader bcl) {
    this.bundle = bundle;
    addClassLoader(bcl);
  }


  ////////////////////////////////////////////////////////////
  //
  // BundleMonitor methods.
  //
  ////////////////////////////////////////////////////////////

  public Iterator /*<BundleRevisionMonitor>*/ getBundleRevisionMonitors() {
    return monitors.values().iterator();
  }


  ////////////////////////////////////////////////////////////
  //
  // BundleRevisionMonitor methods.
  //
  ////////////////////////////////////////////////////////////

  public Bundle getBundle() {
    return bundle;
  }

  public int getBundleGeneration() {
    return curBRM.getBundleGeneration();
  }


  ////////////////////////////////////////////////////////////
  //
  // Memory
  //
  ////////////////////////////////////////////////////////////

  public long getMemory() {
    return curBRM.getMemory();
  }

  public long getMemoryLimit() {
    return curBRM.getMemoryLimit();
  }

  public void setMemoryLimit(int limit) {
    memoryLimit = limit;
    curBRM.setMemoryLimit(limit);
  }


  ////////////////////////////////////////////////////////////
  //
  // Threads
  //
  ////////////////////////////////////////////////////////////

  public int getThreadCount() {
    return curBRM.getThreadCount();
  }

  public int getThreadCountLimit() {
    return curBRM.getThreadCountLimit();
  }

  public void setThreadCountLimit(int limit) {
    threadCountLimit = limit;
    curBRM.setThreadCountLimit(limit);
  }


  ////////////////////////////////////////////////////////////
  //
  // CPU
  //
  ////////////////////////////////////////////////////////////

  public int getCPU() {
    return curBRM.getCPU();
  }

  public int getCPULimit() {
    return curBRM.getCPULimit();
  }

  public void setCPULimit(int limit) {
    cpuLimit = limit;
    curBRM.setCPULimit(limit);
  }


  ////////////////////////////////////////////////////////////
  //
  // Internal methods
  //
  ////////////////////////////////////////////////////////////

  /**
   * Adds a bundle class loader to the set of monitored bundle
   * class loaders for this bundle. The last added class loader is the
   * current one.
   *
   * @param bcl the bundle class loader to start monitoring.
   *
   */
  void addClassLoader(final BundleClassLoader bcl) {
    synchronized(monitors) {
      if (null==monitors.get(bcl)) {
        final BundleRevisionMonitorImpl nBRM
          = new BundleRevisionMonitorImpl(bcl);
        monitors.put(bcl, nBRM);
        this.curBRM = nBRM;
      }
    }
  }

  /**
   * Remove a bundle class loader from the set of monitored bundle
   * class loaders for this bundle.
   *
   * @param bcl the bundle class loader to stop monitoring.
   *
   * @return <code>true</code> if the set of monitored bundle class
   *         loaders for this bundle is empty after the removal.
   */
  boolean removeClassLoader(final BundleClassLoader bcl) {
    synchronized(monitors) {
      final BundleRevisionMonitorImpl oBRM
        = (BundleRevisionMonitorImpl) monitors.remove(bcl);

      if (this.curBRM==oBRM) {
        this.curBRM = getCurrentBRM();
      }
      oBRM.detach();

      return monitors.isEmpty();
    }
  }

  /**
   * Removes all bundle revision monitors from this bundle monitor.
   */
  void removeAlllClassLoaders()  {
    synchronized(monitors) {
      for (Iterator it = monitors.values().iterator(); it.hasNext(); ) {
        final BundleRevisionMonitorImpl brmi
          = (BundleRevisionMonitorImpl) it.next();

        brmi.detach();
        it.remove();
      }
    }
  }


  /**
   * The current bundle revision resource monitor is the last one
   * created for this bundle.
   *
   * @return the current bundle revision resource monitor.
   */
  private BundleRevisionMonitorImpl getCurrentBRM() {
    // No synchronized(monitors) here since it is only called from such code!
    BundleRevisionMonitorImpl res = null;

    for (Iterator it = monitors.values().iterator(); it.hasNext(); ) {
      res = (BundleRevisionMonitorImpl) it.next();
    }

    return res;
  }


  ////////////////////////////////////////////////////////////
  // BundleRevisionMonitorImpl
  ////////////////////////////////////////////////////////////
  class BundleRevisionMonitorImpl implements BundleRevisionMonitor {
    final BundleClassLoader bcl;
    final ResourceMonitor resmon;

    BundleRevisionMonitorImpl(final BundleClassLoader bcl) {
      this.bcl = bcl;

      resmon = new ResourceMonitor(bcl);
      resmon.enableMemoryMonitoring(memoryLimit);
      resmon.enableThreadCountMonitoring(threadCountLimit);
      resmon.enableThreadCpuMonitoring(cpuLimit);
    }

    public Bundle getBundle() {
      return bcl.getBundle();
    }

    public int getBundleGeneration() {
      return ExtActivator.extCtx.getGeneration(bcl);
    }

    ////////////////////////////////////////////////////////////
    //
    // Memory
    //
    ////////////////////////////////////////////////////////////

    public long getMemory() {
      return resmon.getCurrentMemory();
    }

    public long getMemoryLimit() {
      return resmon.getMaxMemory();
    }

    public void setMemoryLimit(int limit) {
      resmon.enableMemoryMonitoring(limit);
    }


    ////////////////////////////////////////////////////////////
    //
    // Threads
    //
    ////////////////////////////////////////////////////////////

    public int getThreadCount() {
      return resmon.getCurrentThreadCount();
    }

    public int getThreadCountLimit() {
      return resmon.getMaxThreadCount();
    }

    public void setThreadCountLimit(int limit) {
      resmon.enableThreadCountMonitoring(limit);
    }


    ////////////////////////////////////////////////////////////
    //
    // CPU
    //
    ////////////////////////////////////////////////////////////

    public int getCPU() {
      return resmon.getCurrentThreadCpu();
    }

    public int getCPULimit() {
      return resmon.getMaxThreadCpu();
    }

    public void setCPULimit(int limit) {
      resmon.enableThreadCpuMonitoring(limit);
    }


    ////////////////////////////////////////////////////////////
    //
    // Internal methods.
    //
    ////////////////////////////////////////////////////////////

    void detach()  {
      resmon.detach();
    }

  }


}
