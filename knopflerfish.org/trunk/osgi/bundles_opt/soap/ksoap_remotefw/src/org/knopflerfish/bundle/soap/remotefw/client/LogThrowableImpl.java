package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.ArrayList;
import java.util.Enumeration;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

public class LogThrowableImpl extends Throwable {

  RemoteFW fw;

  String stackTrace;
  String toString;

  public LogThrowableImpl(String stackTrace, String toString, String message) {
    super(message);
    this.stackTrace = stackTrace;
    this.toString = toString;
  }

  public void printStackTrace() {
    System.out.print(stackTrace);
  }

  public void printStackTrace(PrintStream s) {
    s.print(stackTrace);
  }

  public void printStackTrace(PrintWriter s) {
    s.print(stackTrace);
  }

  public String toString() {
    return toString;
  }

}
