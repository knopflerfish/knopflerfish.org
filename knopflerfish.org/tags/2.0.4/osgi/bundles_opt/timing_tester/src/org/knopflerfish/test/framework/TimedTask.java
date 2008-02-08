package org.knopflerfish.test.framework;

import org.knopflerfish.framework.Main;
import org.osgi.framework.*;
import java.io.*;
import java.util.*;

public abstract class TimedTask {
  String module;
  String msg;
  long   time = -1;
  long   mem  = -1;

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
}
