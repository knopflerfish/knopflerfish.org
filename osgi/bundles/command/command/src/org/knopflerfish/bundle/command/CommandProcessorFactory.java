package org.knopflerfish.bundle.command;

import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.threadio.ThreadIO;
import org.osgi.service.command.*;

public class CommandProcessorFactory implements ServiceFactory {

  // <Bundle, CommandProcessorImpl>
  Map services = new HashMap();
  
  public Object getService(Bundle bundle, 
                           ServiceRegistration reg) {
    synchronized(services) {
      CommandProcessorImpl cp = (CommandProcessorImpl)services.get(bundle);
      if(cp == null) {
        cp = new CommandProcessorImpl(bundle);
        services.put(bundle, cp);
      }
      return cp;
    }
  }
  
  public void 	ungetService(Bundle bundle, 
                             ServiceRegistration reg, 
                             Object service) {
    synchronized(services) {
      CommandProcessorImpl cp = (CommandProcessorImpl)services.get(bundle);
      if(cp == null) {
        services.remove(bundle);
      }
      cp.stop();
    }
  }

  void stop() {
    synchronized(services) {
      for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
        Bundle b = (Bundle)it.next();
        CommandProcessorImpl cp = (CommandProcessorImpl)services.get(b);
        cp.stop();
      }
    }
  }
}
