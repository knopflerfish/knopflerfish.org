/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.junit_runner;


import java.util.*;
import org.osgi.framework.*;


import org.knopflerfish.service.junit.*;
import junit.framework.*;
import java.io.*;

public class Activator implements BundleActivator
{
  BundleContext               bc = null;
  
  public void start(BundleContext bc) throws BundleException {
    this.bc = bc;

    doTestRun();
  }

  public void stop(BundleContext bc) {
    this.bc = null;
  }

  void doTestRun() throws BundleException {
    String tests = System.getProperty("org.knopflerfish.junit_runner.tests");
    String outdir = System.getProperty("org.knopflerfish.junit_runner.outdir");
    boolean bQuit = "true".equals(System.getProperty("org.knopflerfish.junit_runner.quit"));



    if(tests == null) {
      tests = "";
    }

    if(outdir == null) {
      outdir = "junit_out";
    }

    log("tests=" + tests + ", outdir=" + outdir + ", quit=" + bQuit);

    File outDir = null;
    try {
      outDir = new File(outdir);
      outDir.mkdirs();
    } catch (Exception e) {
      throw new BundleException("Failed to create outdir=" + outdir + ": " + e);
    }

    try {
      StringTokenizer st = new StringTokenizer(tests);
      String[]       ids = new String[st.countTokens()];
      int n = 0;
      while (st.hasMoreTokens()) {
	ids[n++] = st.nextToken().trim();
      }
      
      ServiceReference sr = bc.getServiceReference(JUnitService.class.getName());
      if(sr == null) {
	throw new BundleException("JUnitService is not available");
      }
      
      JUnitService ju = (JUnitService)bc.getService(sr);
      if(ju == null) {
	throw new BundleException("JUnitService instance is not available");
      }
      
      for(int i = 0; i < ids.length; i++) {
	File outFile = new File(outDir, ids[i] + ".xml");
	PrintWriter pw = null;
	try {
	  log("run test '" + ids[i] + "', out=" + outFile.getAbsolutePath());	pw = new PrintWriter(new FileOutputStream(outFile));
	  TestSuite suite = ju.getTestSuite(ids[i], null);
	  ju.runTest(pw, suite);
	} catch (Exception e) {
	  log("failed test '" + ids[i] + "', out=" + outFile.getAbsolutePath());
	  e.printStackTrace();
	} finally {
	  try { pw.close(); } catch (Exception ignored) { }
	}
      }
      
      if(bQuit) {
	log("Quit framework after tests");
	bc.getBundle(0).stop();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BundleException("Failed: " + e);
    } finally {
    }
  }
    
  void log(String msg) {
    System.out.println("junit_runner: " + msg);
  }
}
  
  
