/*
 * Copyright (c) 2006, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knopflerfish.bundle.extension_test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.*;

public class Activator implements BundleActivator {
  
  private static final String RESTART_FLAG_FILE = "RESTARTED";
  static boolean RESTARTED=false;
  private static final String SUCCESSFUL_TEST = "##### SUCCESS!";
  private static final String FAILED_TEST = "##### FAILED!";
  private BundleContext bc;
  
  public void start(BundleContext bc) throws Exception {
    this.bc = bc;
    checkRestart();

    if (RESTARTED) {
      afterRestart();
    } else {
      beforeRestart();
    }
  }
  
  void afterRestart() {
    System.out.println("In afterRestart!");
    
    try {
      Class klass1 = Class.forName("org.knopflerfish.service.bundleExt1_test.BundleExt1Test");
      System.out.println("Checking if the boot class extension is loaded by the boot class loaders.");
      
      if (klass1.getClassLoader() == null) {
        System.out.println(SUCCESSFUL_TEST); // some implementations use null to represent the boot class loader.
        
      } else {
        ClassLoader loader = ClassLoader.getSystemClassLoader();      
        while (loader != null) {
          System.out.println(loader);
          if (loader.getParent() != null) {
            loader = loader.getParent();
          } else {
            
            if (loader == klass1.getClassLoader()) {
              success();
            } else {
              failed();
            }
            break;
          }
        }
      }
      
      Class klass2 = Class.forName("org.knopflerfish.service.bundleExt2_test.BundleExt2Test");
      System.out.println("Checking if the framework extension is loaded by the framework classloader.");
      if (bc.getBundle(0).getClass().getClassLoader() != klass2.getClassLoader()) {
        failed();
      } else {
        success();
      }
      
    } catch (ClassNotFoundException e) {
      failed(); 
      e.printStackTrace();
    }
    
    try {
      System.out.println("Halting..");
      bc.getBundle(0).stop();
    } catch (BundleException e) {
      System.out.println(FAILED_TEST);
      e.printStackTrace();
    }
  }
  
  void beforeRestart() {
    System.out.println("In beforeRestart!");
    ServiceTracker tracker = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
    tracker.open();
    PackageAdmin pa = (PackageAdmin)tracker.getService();
    Bundle extensionBC = null;
    Bundle extensionFW = null;
    
    try {
      extensionBC = Util.installBundle(bc, "bundleExt1_test-1.0.0.jar");
      extensionFW = Util.installBundle(bc, "bundleExt2_test-1.0.0.jar");
    } catch (BundleException e1) {
      e1.printStackTrace();
      failed();
    }
    
    File flag = bc.getDataFile(RESTART_FLAG_FILE);
    FileOutputStream fout;
    
    try {
      fout = new FileOutputStream(flag);
      fout.write("slirkeslork".getBytes());
      fout.close();
    } catch (FileNotFoundException e) {
      failed();
      e.printStackTrace();
    } catch (IOException e) {
      failed();
      e.printStackTrace();
    } 
    
    pa.refreshPackages(new Bundle[] { extensionBC, extensionFW });
  }
  

  public void stop(BundleContext bc) throws Exception {
  }
  
  private void checkRestart() {
    File baseDir = bc.getDataFile("");
    
    File[] files = baseDir.listFiles();
    
    for (int i = 0; i < files.length; i++) {
      if (RESTART_FLAG_FILE.equals(files[i].getName())) {
        RESTARTED=true;
      }
    }
  }

  private void failed() {
    System.out.println(FAILED_TEST);
    try {
      bc.getBundle(0).stop();
    } catch (BundleException e) {
      e.printStackTrace();
    }
    
    while (true) {}
  }
  
  
  private void success() {
    System.out.println(SUCCESSFUL_TEST);
  }
  
}
