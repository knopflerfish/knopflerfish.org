package org.knopflerfish.bundle.command;

import java.util.*;
import java.io.*;
import org.osgi.framework.*;
import org.osgi.service.threadio.ThreadIO;
import org.osgi.service.command.*;
import org.osgi.util.tracker.ServiceTracker;

public class CommandProcessorImpl implements CommandProcessor {
  protected Set    sessions = new HashSet();
  
  ServiceTracker          tioTracker;
  CommandProvidersService commandProviders;
  Bundle b;

  CommandProcessorImpl(Bundle b) {
    this.b = b;
    tioTracker = new ServiceTracker(Activator.bc, 
                                    ThreadIO.class.getName(),
                                    null);
    tioTracker.open();

    commandProviders = new CommandProvidersService();      
    commandProviders.open();
  }
  
  public CommandSession createSession(InputStream in, PrintStream out, PrintStream err) {
    synchronized(sessions) {
      CommandSessionImpl cs = new CommandSessionImpl(this, in, out, err);
      cs.init();
      sessions.add(cs);
      return cs;
    }

  }

  public void stop() {
    synchronized(sessions) {
      tioTracker.close();
      tioTracker = null;
      for(Iterator it = sessions.iterator(); it.hasNext(); ) {
        CommandSessionImpl cs = (CommandSessionImpl)it.next();
        cs.close();
      }
      sessions.clear();
      sessions = null;

      commandProviders.close();
      commandProviders = null;

      b = null;
    }
  }
}
