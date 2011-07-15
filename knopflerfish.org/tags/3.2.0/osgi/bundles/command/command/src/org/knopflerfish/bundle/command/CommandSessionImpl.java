package org.knopflerfish.bundle.command;

import java.util.*;
import java.io.*;
import org.osgi.framework.*;
import org.osgi.service.threadio.ThreadIO;
import org.osgi.service.command.*;

public class CommandSessionImpl implements CommandSession {
  CommandProcessorImpl cp;
  InputStream in;
  PrintStream out;
  PrintStream err;

  Map sessionVars = new HashMap();
  
  CommandSessionImpl(CommandProcessorImpl cp,
                     InputStream in, 
                     PrintStream out, 
                     PrintStream err) {
    this.cp  = cp;
    this.in  = in;
    this.out = out;
    this.err = err;
  }

  void init() {
  }

  public Object convert(Class type,   Object in) {
    ServiceReference[] srl  = null;
    Converter        conv = null;
    try {
      String filter = "(" + Converter.CONVERTER_CLASSES + "=" + type.getClass().getName() + ")";
      srl = Activator.bc.getServiceReferences(Converter.class.getName(), filter);
      if(srl == null || srl.length == 0) {
        throw new RuntimeException("No converter for type=" + type.getName());
      }      
      conv = (Converter)Activator.bc.getService(srl[0]);

      Object r = conv.convert(type, in);
      return r;

    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("Bad filter:" + e);
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert " + in + " to " + type.getName() + ", " + e); 
    } finally {
      if(conv != null) {
        Activator.bc.ungetService(srl[0]);
      }
    }
  }
  
  public Object execute(CharSequence commandline) {
    return execute(commandline, in, out, err);
  }
  
  public Object execute(CharSequence commandline,                           
                        InputStream in,   
                        PrintStream out, 
                        PrintStream err) {
    ThreadIO tio = (ThreadIO)cp.tioTracker.getService();
    if(tio == null) {
      throw new RuntimeException("No ThreadIO service available");
    }
    try {
      tio.setStreams(in, out, err);

      Program p = new Program(null, cp.commandProviders);
      p.getVarMap().putAll(sessionVars);

      Object r = p.exec(commandline);
      sessionVars.putAll(p.getVarMap());
      return r;
        
    } finally {
      tio.close();
    }
  }

  public void close() {
    this.in  = null;
    this.out = null;
    this.err = null;
  }


  public InputStream getKeyboard() {
    throw new RuntimeException("NYI");
  }

  public PrintStream getConsole() {
    throw new RuntimeException("NYI");
  }

  public Object get(String name) {
    synchronized(sessionVars) {
      return sessionVars.get(name);
    }
  }

  public void put(String name, Object value) {
    synchronized(sessionVars) {
      sessionVars.put(name, value);
    }
  }

  public CharSequence format(Object target, int level) {
    throw new RuntimeException("NYI");
  }

}
