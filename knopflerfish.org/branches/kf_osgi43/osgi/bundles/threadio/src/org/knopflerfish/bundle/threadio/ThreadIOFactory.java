package org.knopflerfish.bundle.threadio;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.ServiceFactory;

public class ThreadIOFactory implements ServiceFactory {

  // <Bundle, ThreadIOImpl>
  Map services = new HashMap();

  public Object getService(Bundle bundle, 
                           ServiceRegistration reg) {
    synchronized(services) {
      ThreadIOImpl tio = (ThreadIOImpl)services.get(bundle);
      if(tio == null) {
        tio = new ThreadIOImpl(bundle);
        services.put(bundle, tio);
      }
      return tio;
    }
  }
  
  public void 	ungetService(Bundle bundle, 
                             ServiceRegistration reg, 
                             Object service) {
    synchronized(services) {
      ThreadIOImpl tio = (ThreadIOImpl)services.get(bundle);
      if(tio == null) {
        services.remove(bundle);
      }
      tio.close();
    }
  }

  void stop() {
    synchronized(services) {
      for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
        Bundle b = (Bundle)it.next();
        ThreadIOImpl tio = (ThreadIOImpl)services.get(b);
        tio.stop();
      }
    }
  }

}
