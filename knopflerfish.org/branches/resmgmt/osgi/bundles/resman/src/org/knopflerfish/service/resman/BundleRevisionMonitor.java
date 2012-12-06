package org.knopflerfish.service.resman;

import org.osgi.framework.Bundle;

/**
 * Bundle revision resource usage monitoring.
 */
public interface BundleRevisionMonitor {

  /**
   * Get the bundle that this bundle monitor object monitors.
   */
  Bundle getBundle();

  /**
   * Get the generation number of the bundle revision that this bundle
   * revision monitor object monitors.
   */
  int getBundleGeneration();


  ////////////////////////////////////////////////////////////
  //
  // Memory
  //
  ////////////////////////////////////////////////////////////

  /**
   * Get the memory consumed by the current bundle revision of the
   * monitored bundle.
   *
   * I.e., memory allocated by classes loaded by the class loader of
   * the current bundle revision.
   */
  long getMemory();

  /**
   * Get the limit for memory consumed by the current bundle revision
   * of the monitored bundle.
   *
   * I.e., the limit for memory allocated by classes loaded by the
   * class loader of the current bundle revision.
   */
  long getMemoryLimit();

  /**
   * Set the limit for memory consumed by the current bundle revision
   * of the monitored bundle. When this limit is exceeded the JRE will
   * throw an OutOfMemoryError.
   *
   * I.e., the limit for memory allocated by classes loaded by the
   * class loader of the current bundle revision.
   */
  void setMemoryLimit(int limit);



  ////////////////////////////////////////////////////////////
  //
  // Threads
  //
  ////////////////////////////////////////////////////////////

  /**
   * Get the number of threads started by the current bundle revision
   * of the monitored bundle.
   *
   * I.e., number of calls to {@link Thread#start()} made by classes
   * loaded by the class loader of the current bundle revision.
   */
  int getThreadCount();

  /**
   * Get the maximum number of threads that the current bundle revision
   * is allowed to start.
   *
   * I.e., the limit on the number of calls to {@link Thread#start()}
   * made by classes loaded by the class loader of the current bundle
   * revision.
   */
  int getThreadCountLimit();

  /**
   * Set the maximum number of threads that the current bundle revision
   * is allowed to start. When this limit is exceeded the JRE will
   * not start the thread and throw an OutOfMemoryError.
   *
   * I.e., the limit on the number of calls to {@link Thread#start()}
   * made by classes loaded by the class loader of the current bundle
   * revision.
   */
  void setThreadCountLimit(int limit);


  ////////////////////////////////////////////////////////////
  //
  // CPU
  //
  ////////////////////////////////////////////////////////////

  /**
   * Get the percentage of CPU time being consumed by the current
   * bundle revision of the monitored bundle.
   *
   * I.e., the percentage of CPU time being consumed by classes
   * loaded by the class loader of the current bundle revision.
   */
  int getCPU();

  /**
   * Get the limit for the percentage of CPU time being consumed by
   * the current bundle revision of the monitored bundle.
   *
   * I.e., the limit for the percentage of CPU time being consumed by
   * classes loaded by the class loader of the current bundle
   * revision.
   */
  int getCPULimit();

  /**
   * Set the limit for the percentage of CPU time being consumed by
   * the current bundle revision of the monitored bundle.  When this
   * limit is exceeded the JRE will throw an OutOfMemoryError.
   *
   * I.e., the limit for the percentage of CPU time being consumed by
   * classes loaded by the class loader of the current bundle
   * revision.
   */
  void setCPULimit(int limit);

}
