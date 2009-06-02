package org.knopflerfish.bundle.command;

import java.util.*;
import java.io.*;
import java.net.URL;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.threadio.ThreadIO;
import org.osgi.service.command.*;
import org.knopflerfish.bundle.command.commands.*;

public class Activator implements BundleActivator {

  static BundleContext bc;  

  CommandProcessorFactory cpFactory;

  public void start(BundleContext bc) {
    this.bc = bc;
    

    {
      Hashtable props = new Hashtable();
      props.put(Converter.CONVERTER_CLASSES, JavaLangConverter.CLASSES_STRINGS);
      bc.registerService(Converter.class.getName(), new JavaLangConverter(), props);
    }

    {
      Hashtable props = new Hashtable();
      props.put(Converter.CONVERTER_CLASSES, FrameworkConverter.CLASSES_STRINGS);
      bc.registerService(Converter.class.getName(), new FrameworkConverter(), props);
    }
    
    {
      Hashtable props = new Hashtable();
      props.put(CommandProcessor.COMMAND_SCOPE, EchoCmd.SCOPE);
      props.put(CommandProcessor.COMMAND_FUNCTION, EchoCmd.FUNCTION);
      bc.registerService(Object.class.getName(), new EchoCmd(), props);
    }


    {
      Hashtable props = new Hashtable();
      props.put(CommandProcessor.COMMAND_SCOPE, CommandCommands.SCOPE);
      props.put(CommandProcessor.COMMAND_FUNCTION, CommandCommands.FUNCTION);
      bc.registerService(Object.class.getName(), new CommandCommands(bc), props);
    }

    if(false) {
      Hashtable props = new Hashtable();
      props.put(CommandProcessor.COMMAND_SCOPE, FrameworkCommands.SCOPE);
      props.put(CommandProcessor.COMMAND_FUNCTION, FrameworkCommands.FUNCTION);
      bc.registerService(Object.class.getName(), new FrameworkCommands(bc), props);
    }
    
    {
      cpFactory = new CommandProcessorFactory();
      Hashtable props = new Hashtable();
      bc.registerService(CommandProcessor.class.getName(), cpFactory, props);
    }

  }
  
  public void stop(BundleContext bc) {
    this.bc = null;    
  }

}
