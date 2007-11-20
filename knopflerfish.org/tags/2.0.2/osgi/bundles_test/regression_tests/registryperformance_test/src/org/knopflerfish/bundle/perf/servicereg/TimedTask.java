package org.knopflerfish.bundle.perf.servicereg;

import org.osgi.framework.*;
import java.io.*;
import java.util.*;

public abstract class TimedTask {
  String module = "";
  String msg    = "";
  long   time = -1;
  long   mem  = -1;

  static String logPrefix = "";

  public static PrintStream logWriter = System.out;

  public TimedTask(String module, String msg) {
    this.module = module;
    this.msg    = msg;
  }
  
  abstract public Object run();
  
  public String toString() {
    return "TimedTask[" + 
      "module=" + module + 
      ", msg=" + msg + 
      ", time=" + time +
      ", mem=" + mem +
      "]";
  }

  public void log() {
    log(this);
  }

  public static Object log(TimedTask task) {
    long now = System.currentTimeMillis();
    long total = Runtime.getRuntime().totalMemory();
    long free  = Runtime.getRuntime().freeMemory();
    
    Object r = null;
    try {
      r = task.run();
    } catch (Exception e) {
      log(task.module, "Failed: " + task.msg);
      e.printStackTrace();
    }

    task.time = System.currentTimeMillis() - now;
    task.mem  = free - Runtime.getRuntime().freeMemory();
    
    log(task.module, task.msg, task.time, task.mem, null);
    
    return r;
  } 

  static void log(String module, String msg) {
    log(module, msg, 0, 0, null);
  }

  static int logId = 0;

  static void log(String module, String msg, long time, long mem, Exception e) {
    logId++;

    long now = System.currentTimeMillis();

    StringBuffer sb = new StringBuffer();

    sb.append("" + now);   // date
    sb.append(logId);

    sb.append(", ");       // id
    sb.append("" + logId);

    sb.append(", ");       // prefix
    sb.append(logPrefix);

    sb.append(", ");       // module
    sb.append("\"" + module + "\"");


    sb.append(", ");       // message
    sb.append("\"" + msg + "\"");

    sb.append(", ");       // time
    sb.append(time);

    sb.append(", ");       // mem
    sb.append(mem);

    sb.append(", ");       // exception
    sb.append("\"");
    sb.append(e != null ? e.toString() : "none");
    sb.append("\"");

    logWriter.println(sb.toString());

    if(logWriter != System.out) {
      System.out.println("-- fwtest log: " + sb);
    }
    logWriter.flush();
  }

}
