package org.knopflerfish.bundle.soap.remotefw.client;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.SessionListener;

import org.knopflerfish.service.soap.remotefw.RemoteFW;

public class ConsoleSessionImpl implements Session {

  RemoteFW fw;
  String name;
  Reader in;
  PrintWriter out;
  char escapeChar;
  String interruptString;
  ConsoleReaderWriter rw;

  public ConsoleSessionImpl(RemoteFW fw, String name, Reader in, PrintWriter out) {
    this.fw  = fw;
    this.name = name;
    this.in = in;
    this.out = out;
    fw.createSession(name);
    rw = new ConsoleReaderWriter(fw, in, out);
  }

  public void abortCommand() {
    fw.abortCommand();
  }

  public void addSessionListener(SessionListener l) {
    //TODO?
  }

  public void close() {
    rw.stop();
    fw.closeSession();
  }

  public char getEscapeChar() {
    return escapeChar;
  }

  public String getInterruptString() {
    return interruptString;
  }

  public String getName() {
    return name;
  }

  public Dictionary getProperties() {
    return null;
    //TODO?
  }

  public void removeSessionListener(SessionListener l) {
    //TODO?
  }

  public void setEscapeChar(char escapeChar) {
    fw.setEscapeChar(escapeChar);
    this.escapeChar = escapeChar;
  }

  public void setInterruptString(String interruptString) {
    fw.setInterruptString(interruptString);
    this.interruptString = interruptString;
  }

}
