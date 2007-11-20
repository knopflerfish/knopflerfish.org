package org.knopflerfish.bundle.test.memtest;

import org.osgi.framework.*;

import java.util.*;
import java.io.*;

public class Activator implements BundleActivator {
  String logFile;
  long   interval;
  long   totaltime;
  long   sleep = 2 * 1000;
  Thread runner;
  
  BundleContext bc;
  boolean bRun = false;
  PrintWriter out;
  Runtime rt;

  public void start(BundleContext bc) throws BundleException {

    this.bc = bc;
    rt = Runtime.getRuntime();

    logFile = 
      System.getProperty("org.knopflerfish.bundle.test.memtest.logfile",
                         "memtest.log");
    interval = 
      Long.getLong("org.knopflerfish.bundle.test.memtest.interval",
                   10).longValue() * 1000;

    totaltime =
      Long.getLong("org.knopflerfish.bundle.test.memtest.totaltime",
                   1).longValue() * 60 * 1000;

    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
    } catch (Exception e) {
      throw new BundleException("Failed to open file: " + e);
    }
    System.out.println("## started memtest " + new Date() + 
                       ", log=" + logFile + 
                       ", interval=" + interval + 
                       ", total=" + totaltime);
    
    runner = new Thread(new Runnable() {
        public void run() {
          long initTime = System.currentTimeMillis();
          long lastTime = -1;
          
          while(bRun) {
            long now = System.currentTimeMillis();
            if(now - lastTime >= interval) {
              dumpState();
              lastTime = System.currentTimeMillis();
            }
            try {
              Thread.sleep(sleep);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      });
    bRun = true;
    runner.start();
  }

  long oldTotalMem = 0;
  long oldFreeMem  = 0;
  long oldUsedMem  = 0;
  
  void dumpState() {
    rt.gc();
    long totalMem = rt.totalMemory();
    long freeMem  = rt.freeMemory();
    long usedMem  = totalMem - freeMem;
    
    long now = System.currentTimeMillis();
    
    String msg = now +
    ", " + totalMem + "(" + (totalMem - oldTotalMem) + ")" +
    ", " + freeMem + "(" + (freeMem - oldFreeMem) + ")" +
    ", " + usedMem + "(" + (usedMem - oldUsedMem) + ")";
    
    Bundle[] bl = bc.getBundles();
    
    oldTotalMem = totalMem;
    oldFreeMem  = freeMem;
    oldUsedMem  = usedMem;
    out.println(msg);
    out.flush();
    System.out.println(msg);
  }

  void stopAll() {
    System.out.println("## stopped memtest on " + new Date());
    bRun = false;
    if(runner != null) {
      try {
        runner.join(sleep * 2);
      } catch (Exception e) {
      }
      runner = null;
      try {
        out.flush();
        out.close();
      } catch (Exception e) {
      }
    }
  }
  
  public void stop(BundleContext bc) {
    stopAll();
  }
}
