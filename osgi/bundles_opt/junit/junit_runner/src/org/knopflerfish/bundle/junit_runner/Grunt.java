/*
 * Copyright (c) 2004-2009,2018 KNOPFLERFISH project
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

  static final String TESTRUNS_FILE = ".testruns";
  
  final BundleContext bc;

  final HashMap<String,ServiceReference<junit.framework.TestSuite>> allDefaultTests =  
      new HashMap<String, ServiceReference<junit.framework.TestSuite>>();
  
  public Grunt(BundleContext bc) {
    this.bc = bc;

    bc.registerService(TestListener.class.getName(), this, null);
  }

  private boolean bWait = false;
  private boolean bQuit = false;
  
  void doGrunt() throws BundleException {
    bWait = "true".equals(bc.getProperty("org.knopflerfish.junit_runner.wait"));
    bQuit = "true".equals(bc.getProperty("org.knopflerfish.junit_runner.quit"));
    
    if(bWait) {
      Thread t = new Thread() {
          public void run() {
            try {
              doRun();
            } catch (Exception e) {
              err("Test run threw exception: " + e);
              e.printStackTrace();
            }
            try {
              postRun();
            } catch (BundleException e) {
              err("postRun actions failed: " + e);
              e.printStackTrace();
            }
          }
        };
      t.start();
    } else {
      try {
        doRun();
      }
      catch (Exception e) {
        err("Test run threw exception: " + e);
        e.printStackTrace();
      }
      postRun();
    }
    
  }

  public void doRun() throws BundleException {
    String tests = bc.getProperty("org.knopflerfish.junit_runner.tests");
    String outdir = bc.getProperty("org.knopflerfish.junit_runner.outdir");
    // boolean bQuit = "true".equals(bc.getProperty("org.knopflerfish.junit_runner.quit"));
    // boolean bWait = "true".equals(bc.getProperty("org.knopflerfish.junit_runner.wait"));
    String runName = bc.getProperty("org.knopflerfish.junit_runner.name");
    String runDescription = bc.getProperty("org.knopflerfish.junit_runner.description");
    
    if (runName == null) {
      runName = "kf-junit";
    }
    
    if (runDescription == null) {
      runDescription = "Knopflerfish Junit Run";
    }
    
   
    
    boolean filterMode = false;
        
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

    log ("Tests are set to: " + tests);
    
    if(tests.startsWith(FILTER_PREFIX)) {
      filterMode = true;
      String       filter = tests.substring(FILTER_PREFIX.length());
      StringBuilder sb     = new StringBuilder();
      try {
        ServiceReference[] srl = bc.getServiceReferences((String)null, filter);
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
    
    Collection<ServiceReference<junit.framework.TestSuite>> testSuites;
    try {
      testSuites = bc.getServiceReferences(junit.framework.TestSuite.class, null);
    
      for (ServiceReference<junit.framework.TestSuite> sref : testSuites) {
        String id = (String)sref.getProperty("service.pid");
        if (id != null) {
          log("Detected test suite: " + id);
          allDefaultTests.put(id, sref);
        }
      }
    } catch (InvalidSyntaxException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    log("    outdir: " + outdir);
    log("      quit: " + bQuit);
    log("filterMode: " + filterMode);
    log("     tests: "  + tests);
    // log("tests=" + tests + ", outdir=" + outdir + ", quit=" + bQuit);

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

    File lockFile = new File(outDir, "." + runName);
    try {
      if (lockFile.createNewFile() == false) {
        err("A test run already exist: " + lockFile);
        throw new BundleException("Test run alreay exist: " + runName);
      }
    } catch (IOException e2) {
      e2.printStackTrace();
      throw new BundleException("Failed to create lockFile: " + lockFile);
    }

    File testRuns = new File(outDir, TESTRUNS_FILE);
    PrintWriter runPw;
    try {
      runPw = new PrintWriter(new PrintWriter(new FileOutputStream(testRuns, true)));
      runPw.println(runName);
      runPw.close();
    } catch (FileNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    File cssDir = new File(outDir, "css");
    cssDir.mkdir();
    File imagesDir = new File(outDir, "images");
    imagesDir.mkdir();
    
    copyFile(new File(outDir, "junit_style.xsl"),  "/junit_style.xsl");
    copyFile(new File(outDir, "junit_index_style.xsl"), "/junit_index_style.xsl");
    copyFile(new File(outDir, "junit.css"), "/junit.css");
    copyFile(new File(cssDir, "knopflerfish.css"), "/css/knopflerfish.css");
    copyFile(new File(imagesDir, "fadeout_15.png"), "/images/fadeout_15.png");
    copyFile(new File(imagesDir, "kf300_black.png"), "/images/kf300_black.png");
    copyFile(new File(imagesDir, "makewave_logo.png"), "/images/makewave_logo.png");
    copyFile(new File(imagesDir, "shortfadeout_20px.png"), "/images/shortfadeout_20px.png");
    
    PrintWriter runIndexPW = null;
    
//    File outRunDir = new File(outDir, runName);
//    outRunDir.mkdirs();

    String runIndexFileName = runName + "-" + INDEX_FILE;
    
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
        File indexFile = new File(outDir, runIndexFileName);
        if (indexFile.exists()) {
          runIndexPW = new PrintWriter(new PrintWriter(new FileOutputStream(indexFile, true)));
        }
        else {
          runIndexPW = new PrintWriter(new PrintWriter(new FileOutputStream(indexFile)));
          runIndexPW.println("<?xml version=\"1.0\"?>");
          runIndexPW.println("<?xml-stylesheet type=\"text/xsl\" href=\"../junit_index_style.xsl\"?>");
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new BundleException("Failed to open index.xml");
      }


      runIndexPW.println("<junit_run name=\"" + runName + "\" description=\"" + runDescription + "\">");
      
      int failCount = 0;
      for(int i = 0; i < ids.length; i++) {
        String fname = runName + "-" + ids[i] + ".xml";
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
          TestResult tr = ju.runTestSuite(pw, suite);

          if (!tr.wasSuccessful()) {
            failCount++;
          }
        } catch (Exception e) {
          log("failed test '" + ids[i] + "', out=" +outPath);
          failCount++;
          e.printStackTrace();
        } finally {
          try { pw.close(); } catch (Exception ignored) { }
        }

        if (allDefaultTests.containsKey(ids[i])) {
          allDefaultTests.remove(ids[i]);
        }
      }

      // indexPW.println("<junit_index>");
      runIndexPW.println("<summary testMode=\"" + ((filterMode) ? "Filter Mode" : "Explicit List") + "\"");
      runIndexPW.println("         testSuitesExecuted=\"" + ids.length + "\"");
      runIndexPW.println("         testSuitesFailed=\"" + failCount + "\"");
      runIndexPW.print("         testSuitesNotExecuted=\"" + allDefaultTests.size() + "\"");
      runIndexPW.println("/>");
      runIndexPW.print("<junit_not_executed>");
      for (String k : allDefaultTests.keySet()) {
        runIndexPW.println("<suite name=\"" + k + "\"/>");
      }
      runIndexPW.print("</junit_not_executed>");

      try {
        String[] xmlFiles = outDir.list();
        for(int i = 0; i < xmlFiles.length; i++) {
          String fname = xmlFiles[i];
          if(fname.endsWith(".xml") && fname.startsWith(runName) && !fname.equals(runIndexFileName)) {
            File outFile = new File(outDir, fname);
            includeXMLContents(runIndexPW, outFile);
          }
        }
      } catch (java.security.AccessControlException ace) {
        log("outDir.list() failed: " +ace.toString());
      }
      runIndexPW.println("</junit_run>");
      runIndexPW.close();
      
      // Create the overall index file
      
      BufferedReader testrunsReader = null;
      PrintWriter masterIndexPW = null;
      try {
        masterIndexPW = new PrintWriter(new FileOutputStream(new File(outDir, INDEX_FILE)));
        masterIndexPW.println("<?xml version=\"1.0\"?>");
        masterIndexPW.println("<?xml-stylesheet type=\"text/xsl\" href=\"junit_index_style.xsl\"?>");
        masterIndexPW.println("<knopflerfish_integration_tests>");
    
        testrunsReader = new BufferedReader(new FileReader(new File(outDir, TESTRUNS_FILE)));
        String line;
        while(null != (line = testrunsReader.readLine())) {
          String rname = line.trim();
          log("found testrun: " + rname);
          includeXMLContents(masterIndexPW, new File(outDir, rname + "-" + INDEX_FILE));
        }
        masterIndexPW.println("</knopflerfish_integration_tests>");
      } catch (Exception e) {
        log("Failed to create index file");
      } finally {
        try { 
          testrunsReader.close();
          masterIndexPW.close();
        } catch (Exception ignored) { }
      }
        
        
      String outDirAbs = "?outDirAbs?";
      try {
        outDirAbs = outDir.getAbsolutePath();
      } catch (AccessControlException ace) {
        log("outDir.getAbsolutePath() failed: " +ace.toString());
      }
      log("\n" +
          "All tests (" + tests + ") done.\n" +
          "Output XML in " + outDirAbs);

//      if(bQuit) {
//        log("Quit framework after tests");
//        try {
//          AccessController.doPrivileged(new PrivilegedExceptionAction() {
//            public Object run() throws IOException {
//              try {
//                bc.getBundle(0).stop();
//              } catch (BundleException be) {
//                log("Failed to quit framework after tests: " +be);
//              }
//              return null;
//            }
//          });
//        } catch (PrivilegedActionException e) {
//          log("Failed to quit framework after tests: " +e);
//        }
//      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BundleException("Failed: " + e);
    } finally {
      try {
        runIndexPW.close();
      } catch (Exception ignored) {
      }
    }
  }

  static void log(String msg) {
    System.out.println("junit_runner: " + msg);
  }

  static void err(String msg) {
    System.out.println("junit_runner [ERROR]: " + msg);
  }
  
  private void postRun() throws BundleException {
    log("Test run completed, quit=" + bQuit);
    if(bQuit) {
      log("Quitting framework after tests as requested");
      try {
        AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run() throws IOException, BundleException {
            try {
              bc.getBundle(0).stop();
            } catch (BundleException be) {
              err("Failed to quit framework after tests: " +be);
              throw be;
            }
            return null;
          }
        });
      } catch (PrivilegedActionException e) {
        err("Failed to quit framework after tests: " +e);
        throw new BundleException("Failed to quite framework", e);
      }
    }
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
    // PrintWriter stylePW = null;
    FileOutputStream fos = null;
    try {
      // stylePW = new PrintWriter(new PrintWriter(new FileOutputStream(toFile)));
      fos = new FileOutputStream(toFile);
      printResource(fos, fromResource);
    } catch (Exception e) {
      e.printStackTrace();
      log("Failed to copy style: " + e);
    } finally {
      try { fos.close();  } catch (Exception ignored) { }
      fos = null;
    }
  }


  void printResource(OutputStream out, String name)  {
    InputStream in = null;
    try {
      URL url = getClass().getResource(name);
      in = url.openStream();
      BufferedInputStream bin = new BufferedInputStream(in);
      byte[] buf = new byte[1024];
      int n = 0;
      while(-1 != (n = bin.read(buf, 0, buf.length))) {
        // out.print(new String(buf, 0, n));
        out.write(buf, 0, n);
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
