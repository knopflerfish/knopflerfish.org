/**
 * Copyright (c) 1999-2001 Gatespace AB. All Rights Reserved.
 * 
 */

package org.knopflerfish.bundle.cm;

import org.knopflerfish.service.log.*;

import org.osgi.service.cm.*;
import org.osgi.framework.*;
import java.io.*;

/**
 * CM bundle activator implementation
 *
 * @author Per Gustafson
 * @version $Revision: 1.1.1.1 $
 */

public class Activator implements BundleActivator {
  private static final String STORE_DIR_PROP = "com.gatespace.bundle.cm.store";
  private static final String DEFAULT_STORE_DIR = "cm_store";
  
  static BundleContext bc;
  static LogRef log;
  
  private ServiceRegistration serviceRegistration;

  public void start(BundleContext bc) throws BundleException {
    this.bc = bc;
    throwIfBundleContextIsNull();
    createLogRef();
    createAndRegisterConfigurationAdminFactory();
  }

  public void stop(BundleContext bc) throws BundleException {
    unregisterConfigurationAdminFactory();
    closeLogRef();
  }

  private void createLogRef() {
    throwIfBundleContextIsNull();
    log = new LogRef(bc);
  }

  private void closeLogRef() {
    if(log != null) {
      log.close();
      log = null;
    }
  }

  private void createAndRegisterConfigurationAdminFactory() {
    throwIfBundleContextIsNull();
    File storeDir = getStoreDir();
    serviceRegistration =
      bc.registerService( ConfigurationAdmin.class.getName(),
                          new ConfigurationAdminFactory(storeDir),
                          null);
  }

  private void unregisterConfigurationAdminFactory() {
    if(serviceRegistration !=  null) {
      serviceRegistration.unregister();
      serviceRegistration = null;
    }
  }


  private File getStoreDir() {
    throwIfBundleContextIsNull();
    String storeDirName = System.getProperty(STORE_DIR_PROP);
    File storeDir = null;
    if(storeDirName == null || "".equals(storeDirName)) {
      storeDir = bc.getDataFile(DEFAULT_STORE_DIR);
    } else {
      storeDir = new File(storeDirName);
    }
    return storeDir;
  }

  private void throwIfBundleContextIsNull() {
    if(bc == null) {
      throw new NullPointerException("Null BundleContext in Activator");
    }
  }
}
