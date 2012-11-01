package org.knopflerfish.bundle.command;

import java.util.*;
import java.io.*;
import java.net.URL;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.command.*;
import java.lang.reflect.*;



public class CommandProvidersService implements CommandProviders {
  ServiceTracker cpTracker;
  ServiceTracker convTracker;

  public CommandProvidersService() {
    
  }
  
  public void open() {
    try {
      Filter filter =  Activator.bc
        .createFilter("(&" + 
                      "(" + CommandProcessor.COMMAND_SCOPE + "=*)" + 
                      "(" + CommandProcessor.COMMAND_FUNCTION + "=*)" + 
                      ")");
      
      cpTracker = new ServiceTracker(Activator.bc, 
                                     filter,
                                     null);
      cpTracker.open();
    } catch (Exception e) {
      throw new RuntimeException("Failed to init command provider tracker " + e); 
    }
    convTracker = new ServiceTracker(Activator.bc, 
                                     Converter.class.getName(),
                                     null);
    convTracker.open();
  }

  public void close() {
    cpTracker.close();
    cpTracker = null;

    convTracker.close();
    convTracker = null;
  }

  public Object convert(Class desiredType, Object from) {
    if(from == null) {
      return null;
    }

    // Give service converters a chance
    try {
      ServiceReference[] srl = convTracker.getServiceReferences();
      Filter filter =  Activator.bc
        .createFilter("(" + Converter.CONVERTER_CLASSES + "=" + desiredType.getName() + ")");
      for(int i = 0; srl != null && i < srl.length; i++) {
        if(filter.match(srl[i])) {
          Converter conv = (Converter)convTracker.getService(srl[i]);      
          Object to = conv.convert(desiredType, from);
          if(to != null) {
            return to;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // final attempt, try java casting
    if(desiredType.isAssignableFrom(from.getClass())) {
      return from;
    }

    return null;
  }
  
  static final String OBJ = "obj";

  public Collection findCommands(String scope, String name) {
    // System.out.println("findCommands " + scope + ", " + name);
    Collection candidates = new LinkedHashSet();
    try {
      Filter filter =  Activator.bc
        .createFilter("(&" + 
                      (scope != null ? ("(" + CommandProcessor.COMMAND_SCOPE + "=" + scope + ")") : "") + 
                      "(" + CommandProcessor.COMMAND_FUNCTION + "=*)" + 
                      ")");
      ServiceReference[] srl = cpTracker.getServiceReferences();
      for(int i = 0; srl != null && i < srl.length; i++) {
        if(filter.match(srl[i])) {
          Object fobj = srl[i].getProperty(CommandProcessor.COMMAND_FUNCTION);
          Object service = Activator.bc.getService(srl[i]);
          if(matchName(service, fobj, name)) {
            candidates.add(Activator.bc.getService(srl[i]));
          }
          Activator.bc.ungetService(srl[i]);
        }
      }
      return candidates;
    } catch (Exception e) {
      throw new RuntimeException("Failed to find scope=" + scope + ", name=" + name + ", ", e);
    }
  }

  String getNamePart(String s) {
    int ix = s.indexOf(" ");
    if(ix != -1) {
      return s.substring(0, ix);
    }
    return s;
  }

  String getHelpPart(String s) {
    int ix = s.indexOf(" ");
    if(ix != -1) {
      return s.substring(ix+1);
    }
    return "";
  }

  void addNames(Collection names, Object service, String name) {
    if(name.endsWith("*")) {
      String prefix = name.substring(0, name.length()-1);
      Method[] ml = service.getClass().getMethods();
      for(int i = 0; i < ml.length; i++) {
        String s = ml[i].getName();
        if(s.startsWith(prefix)) {
          names.add(getNamePart(s));
        }
      }
    } else {
      names.add(getNamePart(name));
    }
  }

  boolean matchName(Object service, Object fobj, String name) {
    Set names = new HashSet();
    if(fobj instanceof List) {
      for(Iterator it = ((List)fobj).iterator(); it.hasNext(); ) {
        String s = it.next().toString();
        addNames(names, service, s);

      }
    } else if(fobj.getClass().isArray()) {
      for(int i = 0; i < Array.getLength(fobj); i++) {
        addNames(names, service, Array.get(fobj, i).toString());
      }
    } else {
      addNames(names, service, fobj.toString());
    }

    return names.contains(name);

  }
}
