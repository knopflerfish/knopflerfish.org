/*
 * Copyright (c) 2009, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
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

package org.knopflerfish.bundle.console2command;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.command.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.CommandGroupAdapter;

public class Activator implements  BundleActivator {
  static BundleContext bc;
  ServiceTracker cmdTracker;
  public void start(BundleContext bc) throws Exception {
    this.bc = bc;    
    
    cmdTracker = new ServiceTracker(bc, 
                                    CommandGroup.class.getName(), 
                                    cmdTrackerCustomizer);    

    cmdTracker.open();
  }
  

  public void stop(BundleContext bc) {
    cmdTracker.close();
    cmdTracker = null;
    this.bc = null;
  }  
  
 
  Map/*<ServiceReference, ConsoleWrapper>*/ wrappers = new HashMap();

  ServiceTrackerCustomizer cmdTrackerCustomizer = 
    new ServiceTrackerCustomizer() {
      
      public Object addingService(ServiceReference sr) {
        Object obj = bc.getService(sr);
        modifiedService(sr, obj);
        return obj;
      }
      
      public void modifiedService(ServiceReference sr, Object service) {
        synchronized(wrappers) {
          ConsoleWrapper wrapper = (ConsoleWrapper)wrappers.get(sr);
          if(wrapper != null) {
            wrapper.close();
            wrappers.remove(sr);
          }
          if(service instanceof CommandGroupAdapter) {
            wrapper = new ConsoleWrapper(bc, sr, (CommandGroupAdapter)service);
            wrappers.put(sr, wrapper);
            wrapper.open();
          }
        }
      }
      
      public void removedService(ServiceReference sr, Object service) {
        synchronized(wrappers) {
          ConsoleWrapper wrapper = (ConsoleWrapper)wrappers.get(sr);
          if(wrapper != null) {
            wrapper.close();
            wrappers.remove(sr);
          }
        }
      }
    };
}
