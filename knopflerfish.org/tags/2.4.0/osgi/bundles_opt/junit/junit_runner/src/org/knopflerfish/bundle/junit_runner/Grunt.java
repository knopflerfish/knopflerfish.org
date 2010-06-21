/*
 * Copyright (c) 2004-2009, KNOPFLERFISH project
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



import java.io.*;
import java.net.URL;
import java.security.*;
import java.util.*;

import junit.framework.*;
import org.knopflerfish.service.junit.*;

import org.osgi.framework.*;

class Grunt  implements TestListener {
  static final String FILTER_PREFIX  = "filter:";
  static final String DEFAULT_OUTDIR = "junit_grunt";
  static final String DEFAULT_TESTS  = "filter:(objectclass=junit.framework.TestSuite)";
  static final String INDEX_FILE     = "index.xml";

  BundleContext bc;

  public Grunt(BundleContext bc) {
    this.bc = bc;

    bc.registerService(TestListener.class.getName(), this, null);
  }

  boolean bWait = false;

  void doGrunt() throws BundleException {
    bWait = "true".equals(System.getProperty("org.knopflerfish.junit_runner.wait"));
    if(bWait) {
      Thread t = new Thread() {
          public void run() {
            try {
              doRun();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };
      t.start();
    } else {
      doRun();
    }
  }

  public void doRun() throws BundleException {
    String tests = System.getProperty("org.knopflerfish.junit_runner.tests");
    String outdir = System.getProperty("org.knopflerfish.junit_runner.outdir");
    boolean bQuit = "true".equals(System.getProperty("org.knopflerfish.junit_runner.quit"));
    boolean bWait = "true".equals(System.getProperty("org.knopflerfish.junit_runner.wait"));

    if(bWait) {
      Bundle system = bc.getBundle(0);
      log("Wait for framework start");
      int n = 100;
      while(system.getState() != Bundle.ACTIVE) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new BundleException("wait interrupted: " + e);
        }
        n--;
        if(n <= 0) {
          throw new BundleException("Framework failed to start in a reasonable time");
        }
      }
      log("Framework start complete");
    }
    if(tests == null) {
      tests = DEFAULT_TESTS;
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
        indexPW = new PrintWriter(new PrintWriter(new FileOutputStream(new File(outDir, INDEX_FILE))));
      } catch (Exception e) {
        e.printStackTrace();
        throw new BundleException("Failed to create index.xml");
      }

      indexPW.println("<?xml version=\"1.0\"?>");
      indexPW.println("<?xml-stylesheet type=\"text/xsl\" href=\"junit_index_style.xsl\"?>");


      for(int i = 0; i < ids.length; i++) {
        String fname = ids[i] + ".xml";
        File outFile = new File(outDir, fname);
        PrintWriter pw = null;
        String outPath = "?";
        try {
          outPath = outFile.getAbsolutePath();
        } catch (Exception e) {
          log("Failed to get absolute path for fname: " +e);
        }
        try {
          log("run test '" + ids[i] + "', out=" + outPath);
          pw = new PrintWriter(new FileOutputStream(outFile));
          TestSuite suite = ju.getTestSuite(ids[i], null);
          ju.runTest(pw, suite);

        } catch (Exception e) {
          log("failed test '" + ids[i] + "', out=" +outPath);
          e.printStackTrace();
        } finally {
          try { pw.close(); } catch (Exception ignored) { }
        }
      }

      indexPW.println("<junit_index>");
      try {
        String[] xmlFiles = outDir.list();
        for(int i = 0; i < xmlFiles.length; i++) {
          String fname = xmlFiles[i];
          if(fname.endsWith(".xml") && !fname.equals(INDEX_FILE)) {
            File outFile = new File(outDir, fname);
            includeXMLContents(indexPW, outFile);
          }
        }
      } catch (java.security.AccessControlException ace) {
        log("outDir.list() failed: " +ace.toString());
      }
      indexPW.println("</junit_index>");


      String outDirAbs = "?outDirAbs?";
      try {
        outDirAbs = outDir.getAbsolutePath();
      } catch (AccessControlException ace) {
        log("outDir.getAbsolutePath() failed: " +ace.toString());
      }
      log("\n" +
          "All tests (" + tests + ") done.\n" +
          "Output XML in " + outDirAbs);

      if(bQuit) {
        log("Quit framework after tests");
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws IOException {
                  try {
                    bc.getBundle(0).stop();
                  } catch (BundleException be) {
                    log("Failed to quit framework after tests: " +be);
                  }
                  return null;
                }
            });
        } catch (PrivilegedActionException e) {
          log("Failed to quit framework after tests: " +e);
        }
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

  void includeXMLContents(PrintWriter out, File srcFile) throws IOException {
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

  // TestListener method
  public void startTest(Test test)
  {
    log("Starting test " +test );
  }
  // TestListener method
  public void endTest(Test test)
  {
    log("End test " +test);
  }
  // TestListener method
  public void addError(Test test, Throwable t)
  {
    log("Test error " +test +" throwable: " +t);
  }
  // TestListener method
  public void addFailure(Test test, AssertionFailedError t)
  {
    log("Test failure " +test +" Assertion: " +t);
  }
}
