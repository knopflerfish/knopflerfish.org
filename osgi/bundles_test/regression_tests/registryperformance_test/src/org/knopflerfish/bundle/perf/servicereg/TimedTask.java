/*
 * Copyright (c) 2004-2022 KNOPFLERFISH project
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

package org.knopflerfish.bundle.perf.servicereg;

import java.io.PrintStream;

public abstract class TimedTask {
  String module;
  String msg;
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

  public static Object log(TimedTask task) {
    long now = System.currentTimeMillis();
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

    StringBuilder sb = new StringBuilder();

    sb.append(now);   // date
    sb.append(logId);

    sb.append(", ");       // id
    sb.append(logId);

    sb.append(", ");       // prefix
    sb.append(logPrefix);

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

    logWriter.println(sb.toString());

    if(logWriter != System.out) {
      System.out.println("-- fwtest log: " + sb);
    }
    logWriter.flush();
  }

}
