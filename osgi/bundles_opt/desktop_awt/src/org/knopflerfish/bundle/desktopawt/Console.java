package org.knopflerfish.bundle.desktopawt;

import org.osgi.framework.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.io.*;

import org.knopflerfish.service.console.*;

public class Console implements ServiceListener {
  final static String CONSOLE_CLASS = 
    "org.knopflerfish.service.console.ConsoleService";

  ConsoleAWT panel;

  Console() {

    panel = new ConsoleAWT();
    panel.start();

    String filter = "(objectclass=" + CONSOLE_CLASS + ")";
    try {
      ServiceReference[] srl = Activator.bc.getServiceReferences(null, filter);
      for(int i = 0; srl != null && i < srl.length; i++) {
        serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
      }

      Activator.bc.addServiceListener(this, filter);
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void serviceChanged(ServiceEvent ev) {
    switch(ev.getType()) {
    case ServiceEvent.REGISTERED:
      if(consoleSR == null) {
        openConsole(ev.getServiceReference());
      }
      break;
    case ServiceEvent.UNREGISTERING:
      if(consoleSR == ev.getServiceReference()) {
        closeConsole();
      }
      break;
    }
  }
  
  ServiceReference consoleSR = null;
  ConsoleService console = null;
  Session               consoleSession = null;

  void openConsole(ServiceReference sr) {
    consoleSR = sr;
    console   = (ConsoleService)Activator.bc.getService(consoleSR);

    try {
      consoleSession = 
        console.runSession("ConsoleAWT",
                            panel.in,
                           new PrintWriter(panel.out));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void closeConsole() {
    if(consoleSR != null) {
      Activator.bc.ungetService(consoleSR); 
      consoleSR = null;
      console   = null;
    }
  }
}
  

