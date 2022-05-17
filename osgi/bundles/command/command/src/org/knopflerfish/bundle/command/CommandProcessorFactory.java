/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
