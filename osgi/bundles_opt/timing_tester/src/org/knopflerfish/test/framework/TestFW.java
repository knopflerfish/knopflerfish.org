package org.knopflerfish.test.framework;

import org.knopflerfish.framework.Main;
import org.osgi.framework.*;
import java.io.*;
import java.util.*;

public class TestFW  {

  public Main fwMain;
  public Object notifier = new Object();

  public static TestFW tester = new TestFW();

  TestFW() {
    log("fwtest", "starting new test");
    tester = this;
  }

  public static void main(String[] argv) {
    tester.start(argv);

    System.exit(0);
  }

  void start(final String[] argv) {

    boolean bDelete = "true".equals(System.getProperty("fwdir.delete",
                                                       "true"));
    if(bDelete) {
      log(new TimedTask("fwtest", "delete fwdir") {
          public Object run() {
            deleteTree(new File("fwdir"));
            return null;
          }
        });
    }

    runStartupTest(argv);

  }

  void runStartupTest(final String[] argv) {

    log(new TimedTask("fwtest", "run fw main") {
        public Object run() {
          Thread t = new Thread() {
              public void run() {
                //              log("runStartup", "creating Main");
                fwMain = new Main();
                //              log("runStartup", "launching Main");
                fwMain.main(argv);
                //              log("runStartup", "launched Main");
              }
            };
          t.start();

          synchronized(notifier) {
            try {
              log("runStartup", "wait for notify");
              notifier.wait();
              log("runStartup", "got notify");
            } catch (Exception e) {
              log("runStartup", "wait failed", e);
            }
          }

          return null;
        }

      });
  }

  static void deleteTree(File f) {
    if(f.exists()) {
      if(f.isDirectory()) {
        String[] children = f.list();
        for(int i = 0; i < children.length; i++) {
          deleteTree(new File(f, children[i]));
        }
      }
      f.delete();
    }
  }

  static Object log(TimedTask task) {
    long now = System.currentTimeMillis();
    long total = Runtime.getRuntime().totalMemory();
    long free  = Runtime.getRuntime().freeMemory();

    Object r = null;
    try {
      r = task.run();
    } catch (Exception e) {
      log(task.module, "Failed: " + task.msg, e);
    }

    task.time = System.currentTimeMillis() - now;
    task.mem  = free - Runtime.getRuntime().freeMemory();

    log(task.module, task.msg, task.time, task.mem, null);

    return r;
  }

  static void log(String module, String msg, Exception e) {
    log(module, msg, 0, 0, e);
  }

  static void log(String module, String msg) {
    log(module, msg, 0, 0, null);
  }

  static int logId = 0;

  static void log(String module, String msg, long time, long mem, Exception e) {
    logId++;

    long now = System.currentTimeMillis();

    StringBuilder sb = new StringBuilder();

    sb.append(now);   // date
    sb.append(logId);

    sb.append(", ");       // id
    sb.append(logId);

    sb.append(", ");       // prefix
    sb.append(System.getProperty("test.log.prefix", "test"));

    sb.append(", ");       // module
    sb.append("\"").append(module).append("\"");


    sb.append(", ");       // message
    sb.append("\"").append(msg).append("\"");

    sb.append(", ");       // time
    sb.append(time);

    sb.append(", ");       // mem
    sb.append(mem);

    sb.append(", ");       // exception
    sb.append("\"");
    sb.append(e != null ? e.toString() : "none");
    sb.append("\"");

    System.out.println("-- fwtest log: " + sb);
    try {
      File logFile = new File(System.getProperty("test.log.file", "log.csv"));
      System.out.println("-- logFile: " + logFile);
      PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true));
      logWriter.println(sb.toString());
      logWriter.flush();
      logWriter.close();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
  }

}
