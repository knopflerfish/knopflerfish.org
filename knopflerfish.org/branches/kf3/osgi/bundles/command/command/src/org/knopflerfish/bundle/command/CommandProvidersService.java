package org.knopflerfish.bundle.command;

import java.util.*;
import java.io.*;
import java.net.URL;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.command.*;




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
    // System.out.println("convert(" + desiredType.getName() + ", " + from + ")");
    try {
      ServiceReference[] 	srl = convTracker.getServiceReferences();
      Filter filter =  Activator.bc
        .createFilter("(" + Converter.CONVERTER_CLASSES + "=" + desiredType.getName() + ")");
      for(int i = 0; srl != null && i < srl.length; i++) {
        // System.out.println(" srl[" + i + "]=" + srl[i]);
        if(filter.match(srl[i])) {
          Converter conv = (Converter)convTracker.getService(srl[i]);      
          // System.out.println(" conv=" + conv);
          Object to = conv.convert(desiredType, from);
          // System.out.println(" to=" + to);
          if(to != null) {
            return to;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  static final String OBJ = "obj";

  public Collection findCommands(String scope, String name) {
    Collection candidates = new LinkedHashSet();
    try {
      Filter filter =  Activator.bc
        .createFilter("(&" + 
                      (scope != null ? ("(" + CommandProcessor.COMMAND_SCOPE + "=" + scope + ")") : "") + 
                      "(" + CommandProcessor.COMMAND_FUNCTION + "=" + name + ")" + 
                      ")");
      ServiceReference[] srl = cpTracker.getServiceReferences();
      for(int i = 0; srl != null && i < srl.length; i++) {
        if(filter.match(srl[i])) {
          candidates.add(Activator.bc.getService(srl[i]));
        }
      }
      return candidates;
    } catch (Exception e) {
      throw new RuntimeException("Failed to find scope=" + scope + ", name=" + name + ", ", e);
    }
  }
}
