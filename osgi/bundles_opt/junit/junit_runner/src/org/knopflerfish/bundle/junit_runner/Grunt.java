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
import java.net.URL;

import org.knopflerfish.service.junit.*;
import junit.framework.*;
import java.io.*;

class Grunt {
  static final String DEFAULT_OUTDIR = "junit_grunt";

  BundleContext bc;

  public Grunt(BundleContext bc) {
    this.bc = bc;
  }

  static final String FILTER_PREFIX = "filter:";

  void doGrunt() throws BundleException {
    String tests = System.getProperty("org.knopflerfish.junit_runner.tests");
    String outdir = System.getProperty("org.knopflerfish.junit_runner.outdir");
    boolean bQuit = "true".equals(System.getProperty("org.knopflerfish.junit_runner.quit"));

    if(tests == null) {
      tests = "";
    }

    if(outdir == null) {
      outdir = DEFAULT_OUTDIR;
    }

    if(tests.startsWith(FILTER_PREFIX)) {
      String       filter = tests.substring(FILTER_PREFIX.length());
      StringBuffer sb     = new StringBuffer();
      try {
	ServiceReference[] srl = bc.getServiceReferences(null, filter);
	for(int i = 0; srl != null && i < srl.length; i++) {
	  String id = (String)srl[i].getProperty("service.pid");
	  if(id != null) {
	    if(sb.length() > 0) {
	      sb.append(" ");
	    }
	    sb.append(id);
	  }
	}
	tests = sb.toString();
      } catch (Exception e) {
	throw new BundleException("Filter failed, filter='" + filter + "', err=" + e);
      }
    }
    
    log("tests=" + tests + ", outdir=" + outdir + ", quit=" + bQuit);
    
    File outDir = null;
    try {
      outDir = new File(outdir);
      outDir.mkdirs();

    } catch (Exception e) {
      throw new BundleException("Failed to create outdir=" + outdir + ": " + e);
    }

    if(!(outDir != null && outDir.exists() && outDir.isDirectory())) {
      throw new BundleException("Failed to create outdir " + outdir);
    }

    copyFile(new File(outDir, "junit_style.xsl"),  "/junit_style.xsl");
    copyFile(new File(outDir, "junit_index_style.xsl"), "/junit_index_style.xsl");
    copyFile(new File(outDir, "junit.css"), "/junit.css");

    PrintWriter indexPW = null;

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

      try {
	indexPW = new PrintWriter(new PrintWriter(new FileOutputStream(new File(outDir, "index.xml"))));
      } catch (Exception e) {
	e.printStackTrace();
	throw new BundleException("Failed to create index.xml");
      }
      
      indexPW.println("<?xml version=\"1.0\"?>");
      indexPW.println("<?xml-stylesheet type=\"text/xsl\" href=\"junit_index_style.xsl\"?>");
      //      indexPW.println("<!DOCTYPE junit_index [\n");

      for(int i = 0; i < ids.length; i++) {
	String fname = ids[i] + ".xml";
	File outFile = new File(outDir, fname);
	PrintWriter pw = null;
	try {
	  log("run test '" + ids[i] + "', out=" + outFile.getAbsolutePath());	pw = new PrintWriter(new FileOutputStream(outFile));
	  TestSuite suite = ju.getTestSuite(ids[i], null);
	  ju.runTest(pw, suite);

	  /*
	  indexPW.println(" <!ENTITY testid_" + i + 
			  " SYSTEM " + 
			  " \"" + fname + "\">");
	  */
	} catch (Exception e) {
	  log("failed test '" + ids[i] + "', out=" + outFile.getAbsolutePath());
	  e.printStackTrace();
	} finally {
	  try { pw.close(); } catch (Exception ignored) { }
	}
      }

      //      indexPW.println("]>"); // end of DOCTYPE

      indexPW.println("<junit_index>");
      for(int i = 0; i < ids.length; i++) {
	String fname = ids[i] + ".xml";
	File outFile = new File(outDir, fname);
	printFileContents(indexPW, outFile); 
	// indexPW.println("&testid_" + i + ";");
      }
      indexPW.println("</junit_index>");

      
      log("\n" + 
	  "All tests (" + tests + ") done.\n" + 
	  "Output XML in " + outDir.getAbsolutePath());

      if(bQuit) {
	log("Quit framework after tests");
	bc.getBundle(0).stop();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BundleException("Failed: " + e);
    } finally {
      try {
	indexPW.close();
      } catch (Exception ignored) {
      }
    }
  }
    
  void log(String msg) {
    System.out.println("junit_runner: " + msg);
  }

  void printFileContents(PrintWriter out, File srcFile) throws IOException {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(srcFile));
      String line;
      while(null != (line = in.readLine())) {
	String cmp = line.trim();
	if(!(cmp.startsWith("<?xml ") || 
	     cmp.startsWith("<?xml-stylesheet "))) { 
	  out.println(line);
	}
      }
    } finally {
      try { in.close(); } catch (Exception ignored) { }
    }
  }

  void copyFile(File toFile, String fromResource) {
    PrintWriter stylePW = null;
    try {
      stylePW = new PrintWriter(new PrintWriter(new FileOutputStream(toFile)));
      
      printResource(stylePW, fromResource);
    } catch (Exception e) {
      e.printStackTrace();
      log("Failed to copy style: " + e);
    } finally {
      try { stylePW.close();  } catch (Exception ignored) { }
      stylePW = null;
    }
  }


  void printResource(PrintWriter out, String name)  {
    InputStream in = null;
    try {
      URL url = getClass().getResource(name);
      in = url.openStream();
      BufferedInputStream bin = new BufferedInputStream(in);
      byte[] buf = new byte[1024];
      int n = 0;
      while(-1 != (n = bin.read(buf, 0, buf.length))) {
	out.print(new String(buf, 0, n));
      }
    } catch (Exception e) {
      e.printStackTrace();
      log("printResource(" + name + ") failed: " +  e);
    } finally {
      try { in.close(); } catch (Exception ignored) { }
    }
  }

}
  
  
