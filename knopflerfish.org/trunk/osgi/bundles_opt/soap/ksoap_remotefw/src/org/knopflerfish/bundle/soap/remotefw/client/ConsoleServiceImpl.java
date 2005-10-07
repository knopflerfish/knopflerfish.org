package org.knopflerfish.bundle.soap.remotefw.client;

import java.io.PrintWriter;
import java.io.Reader;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;

import org.knopflerfish.service.soap.remotefw.RemoteFW;

public class ConsoleServiceImpl implements ConsoleService {

  RemoteFW fw;

  ConsoleServiceImpl(RemoteFW fw) {
    this.fw  = fw;
  }

  public String runCommand(String command) {
    return fw.runCommand(command);
  }

  public Session runSession(String name, Reader in, PrintWriter out) {
    return new ConsoleSessionImpl(fw, name, in, out);
  }

  public String[] setAlias(String key, String[] val) {
    return fw.setAlias(key, val);
  }


}
