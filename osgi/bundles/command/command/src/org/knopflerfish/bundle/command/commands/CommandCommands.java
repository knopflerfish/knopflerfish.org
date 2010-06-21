package org.knopflerfish.bundle.command.commands;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.lang.reflect.*;
import org.osgi.framework.*;
import org.osgi.service.command.*;

public class CommandCommands {

  public static String SCOPE = "command";
  public static String[] FUNCTION = new String[] { 
    "help", 
    "alias",
    "unalias",
  };

  BundleContext bc;
  Map aliasMap = new HashMap();

  public CommandCommands(BundleContext bc) {
    this.bc = bc;
  }
  
  public void alias(String from, String to) {
    aliasMap.put(from, to);
  }

  public void unalias(String from) {
    aliasMap.remove(from);
  }

  public Object help(String scope) throws Exception {
    String filter = "(" + CommandProcessor.COMMAND_FUNCTION + "=*)";
    if(scope != null) {
      filter = 
        "(&" + 
        filter + 
        "(" + CommandProcessor.COMMAND_SCOPE + "=" + scope + ")" + 
        ")";
        
    }
    ServiceReference[] srl = bc.getServiceReferences(null, filter);
    for(int i = 0; srl != null && i < srl.length; i++) {
      Object obj = null;
      try {
        obj = bc.getService(srl[i]);
        System.out.println(obj.getClass().getName());
        System.out.println(" function: " + srl[i].getProperty(CommandProcessor.COMMAND_FUNCTION));
        System.out.println(" scope:    " + srl[i].getProperty(CommandProcessor.COMMAND_SCOPE));

        String[] function = (String[])srl[i].getProperty(CommandProcessor.COMMAND_FUNCTION);
        for(int j = 0; j < function.length; j++) {
          System.out.println("  " + function[j]);
        }
        /*
        Method[] ml = obj.getClass().getMethods();
        for(int j = 0; ml != null && j < ml.length; j++) {
          if(ml[i].getDeclaringClass() != Object.class) {
            System.out.println("  " + ml[j]);
          }
        }
        */
      } finally {
        bc.ungetService(srl[i]);
      }
    } 
    
    return "foo";
  }
}
