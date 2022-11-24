/*
 * Copyright (c) 2006-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.endurance_test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class Activator implements BundleActivator {
  private String cacheDir;
  private PrintStream out;
  BundleContext bc = null;

  public void start(BundleContext context) throws Exception {
    String outfile =System.getProperty("org.knopflerfish.bundle.endurance_test.output");
    if (outfile == null) {
      out = System.out;      
    } else {
      out = new PrintStream(new FileOutputStream(new File(outfile)), true);
    }
       
    
    bc = context;
    cacheDir = bc.getProperty("org.osgi.framework.dir");
    out.println("Environment:");
    out.println(" OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
    out.println(" JVM: " + System.getProperty("java.vendor") + " ver: " + System.getProperty("java.version"));
    out.println(" OSGi implementation: " + bc.getProperty(Constants.FRAMEWORK_VENDOR) + " " + bc.getProperty(Constants.FRAMEWORK_VERSION));
    out.println();
    new Thread(new TestRunner()).start();
  }


  public void stop(BundleContext context) {
  }
  
  private class TestRunner implements Runnable {
    
    public void run() {
      EnduranceTest[] tests = new EnduranceTest[] {
          new Test1(bc),
          new Test2(bc),
          new Test3(bc),
          new Test4(bc),
          new Test5(bc),
          new Test6(bc),
          new Test7(bc),
          new Test8(bc),
          new Test9(bc, 500, 1, new String[] { "sv" }, "Localization test many getHeaders/install"),
          new Test9(bc, 1, 500, new String[] { "sv" }, "Localization test few getHeaders/install"),
          new Test9(bc, 500, 1, new String[] { "" }, "Raw localization test many getHeaders/install"),
          new Test9(bc, 1, 500, new String[] { "" }, "Raw localization test few getHeaders/install"),
          new Test10(bc, 500, 1, new String[] { "sv" }, "Localization test with fragments many getHeaders/install"),
          new Test10(bc, 1, 500, new String[] { "sv" }, "Localization test with fragments few getHeaders/install"),
          new Test10(bc, 500, 1, new String[] { "" }, "Raw localization test with fragments many getHeaders/install"),
          new Test10(bc, 1, 500, new String[] { "" }, "Raw localization test with fragments few getHeaders/install"),
          
          // add new tests here.
      };

      for (EnduranceTest test : tests) {

        int n = test.getNoRuns();
        int bestRun = -1;
        long bestTime = Long.MAX_VALUE;

        int worstRun = -1;
        long worstTime = Long.MIN_VALUE;

        System.out.println("Starting test \"" + test.testName() + "\"");
        test.prepare();
        long discUsage = discUsage(cacheDir);
        System.gc();
        long totalTime = System.currentTimeMillis();
        long freeBefore = Runtime.getRuntime().freeMemory();
        long totalBefore = Runtime.getRuntime().totalMemory();

        for (int o = 0; o < n; o++) {
          long tmp = System.currentTimeMillis();

          if (!test.runTest()) {
            out.println("FAILED TO RUN TEST " + test.getClass().getName());
            break;
          }

          long tmp2 = System.currentTimeMillis();

          if (bestTime > tmp2 - tmp) {
            bestTime = tmp2 - tmp;
            bestRun = o;
          }

          if (worstTime < tmp2 - tmp) {
            worstTime = tmp2 - tmp;
            worstRun = o;

          }
        }
        totalTime = System.currentTimeMillis() - totalTime;
        System.gc();
        long freeAfter = Runtime.getRuntime().freeMemory();
        long totalAfter = Runtime.getRuntime().totalMemory();

        out.println("Results from test \"" + test.testName() + "\" (executed " + test.getNoRuns() + " times)");
        out.println("Memory\t\tfree\t\ttotal\t\tused");
        out.println(" before:\t" + freeBefore / 1000 + "kB\t\t" + totalBefore / 1000 + "kB\t\t" + (totalBefore - freeBefore) / 1000 + "kB");
        out.println("  after:\t" + freeAfter / 1000 + "kB\t\t" + totalAfter / 1000 + "kB\t\t" + (totalAfter - freeAfter) / 1000 + "kB");
        out.println("Disc usage\tused");
        out.println(" before:\t" + discUsage / 1000 + "kB\t");
        out.println("  after:\t" + discUsage(cacheDir) / 1000 + "kB");
        out.println("Time ");
        out.println("  best run:\t " + bestTime + "ms\trun:" + bestRun);
        out.println(" worst run:\t " + worstTime + "ms\trun:" + worstRun);
        out.println("Total time:\t " + totalTime + "ms");
        out.println();

        test.cleanup();
      }
      
      if (System.getProperty("org.knopflerfish.bundle.endurance_test.halt_after_test", 
          "false").equals("true")) {
        try {
          System.out.println("Shutting down framework.");
          bc.getBundle(0).stop();
          
        } catch (BundleException e) {
          e.printStackTrace();
          System.exit(0);
        }    
      }
    }
  }
  
  private FilenameFilter filter = new MyFilenameFilter(); 
  
  private long discUsage(String dir) {
    
    if (dir == null) {
      return 0;        
    }
    
    File tmp = new File(dir);
    
    if (!tmp.isDirectory()) {
      throw new IllegalArgumentException(dir + " is not a directory");
    }
    
    File[] children = tmp.listFiles(filter);
    assert children != null; // tmp is a directory

    long acc = 0;
    for (File child : children) {
      if (child.isDirectory()) {
        acc += discUsage(child.getPath());
      } else {
        acc += child.length();
      }
    }
    
    return acc;
  }
  
  private static class MyFilenameFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
      return !(name.equals(".") || name.equals(".."));
    }
  }
}
