package org.knopflerfish.service.resman;

import org.osgi.framework.Bundle;

public interface BundleMonitor {
  Bundle getBundle();
  long getMemory();
  int getThreadCount();
  int getCPU();

  long getMemoryLimit();
  int getThreadCountLimit();
  int getCPULimit();

  void setMemoryLimit(int limit);
  void setThreadCountLimit(int limit);
  void setCPULimit(int limit);

}
