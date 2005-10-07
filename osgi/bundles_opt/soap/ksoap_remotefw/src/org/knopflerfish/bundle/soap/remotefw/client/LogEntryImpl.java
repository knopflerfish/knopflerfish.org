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

public class LogEntryImpl implements LogEntry {

  RemoteFW fw;
  Bundle bundle;
  int level;
  Throwable exception;
  String message;
  long time;

  LogEntryImpl(RemoteFW fw,
               Bundle bundle,
               int level,
               String message,
               long time,
               Throwable exception) {
    this.fw  = fw;
    this.bundle = bundle;
    this.level = level;
    this.message = message;
    this.time = time;
    this.exception = exception;
  }

  LogEntryImpl(RemoteFW fw, Bundle bundle) {
    this.fw  = fw;
    this.bundle = bundle;
  }

  public Bundle getBundle() {
    return bundle;
  }

  public Throwable getException() {
    return exception;
  }

  public int getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public ServiceReference getServiceReference() {
    return null;
    //TODO?
  }

  public long getTime() {
    return time;
  }

}
