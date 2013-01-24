package org.knopflerfish.bundle.command;

import java.util.*;
import org.osgi.service.command.*;
import java.lang.reflect.Method;
import org.knopflerfish.bundle.command.commands.*;


public class CommandProvidersTest implements CommandProviders {
  
  JavaLangConverter conv = new JavaLangConverter();
  
  Map commands = new HashMap() {{
    put("echocmd", new EchoCmd());
  }};
  
  public Object convert(Class desiredType, Object from) {
    return conv.convert(desiredType, from);
  }
  

  public Collection findCommands(String scope, String name) {
    Collection candidates = new LinkedHashSet();
    if(scope != null) {
      Object r = commands.get(scope);
      if(r != null) {
        candidates.add(r);
      }
    } else {
      candidates.addAll(commands.values());
    }
    
    for(Iterator it = candidates.iterator(); it.hasNext(); ) {
      Object obj = it.next();
      
      Method[] ml = obj.getClass().getMethods();
      for(int i = 0; i < ml.length; i++) {
        if(ml[i].getName().equalsIgnoreCase(name)) {
          candidates.add(obj);
        }
      }
    }
    return candidates;
  }
}
