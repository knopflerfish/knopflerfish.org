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
import java.util.*;

import org.osgi.service.command.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;

public class ConsoleWrapper {
  ServiceReference    sr;
  CommandGroupAdapter        cg;
  BundleContext       bc;
  ServiceRegistration reg;
  Dispatcher          dispatcher;

  public ConsoleWrapper(BundleContext bc, 
                        ServiceReference sr, 
                        CommandGroupAdapter cg) {
    this.bc = bc;    
    this.sr = sr;
    this.cg = cg;
  }

  public void open() {
    if(reg != null) {
      return;
    }

    String[] names = getCommands(cg); 
    if(names != null && names.length > 0) {
      Hashtable props = new Hashtable();
      props.put(CommandProcessor.COMMAND_SCOPE, cg.getGroupName());
      props.put(CommandProcessor.COMMAND_FUNCTION, names);

      dispatcher = new Dispatcher(cg);
      
      /*
      System.out.println("register " + dispatcher + ", scope=" + cg.getGroupName() + " with " + names.length + " commands");
      for(int i = 0; i < names.length; i++) {
        System.out.println(" " + names[i]);
        
      }
      */
      BundleContext regBC = sr.getBundle().getBundleContext();
      reg = regBC.registerService(Object.class.getName(), dispatcher, props);
    } else {
      // System.out.println("no names in " + cg);
    }
  }

  
  String[] getCommands(CommandGroup cg) {
    if(cg instanceof CommandGroupAdapter) {
      CommandGroupAdapter cga = (CommandGroupAdapter)cg;
      Map namesMap = cga.getCommandNames();
      String[] names = new String[namesMap.size()];
      int i = 0;
      for(Iterator it = namesMap.keySet().iterator(); it.hasNext(); ) {
        String name = (String)it.next();
        String help = (String)namesMap.get(name);
        names[i] = name;
        i++;
      }
      return names;
    }
    return null;
  }

  public void close() {
    if(reg == null) {
      return;
    }

    reg.unregister();
    reg = null;
  }

}

