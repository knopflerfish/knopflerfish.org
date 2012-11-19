package org.knopflerfish.service.resman;

import org.osgi.framework.Bundle;

public interface BundleMonitor {
  public Bundle getBundle();
  public long getMemory();
  public int getThreadCount();
  public int getCPU();

  public long getMemoryLimit();
  public int getThreadCountLimit();
  public int getCPULimit();

}
