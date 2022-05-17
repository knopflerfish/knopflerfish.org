/*
 * Copyright (c) 2009-2022, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.service.command.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.CommandGroupAdapter;

public class ConsoleWrapper {
  private ServiceReference<?> serviceReference;
  private CommandGroupAdapter commandGroupAdapter;
  private ServiceRegistration<?> serviceRegistration;

  public ConsoleWrapper(ServiceReference<?> serviceReference,
                        CommandGroupAdapter commandGroupAdapter) {
    this.serviceReference = serviceReference;
    this.commandGroupAdapter = commandGroupAdapter;
  }

  public void open() {
    if (serviceRegistration != null) {
      return;
    }

    String[] names = getCommands(commandGroupAdapter);
    if (names != null && names.length > 0) {
      Dispatcher dispatcher = new Dispatcher(commandGroupAdapter);

      Hashtable<String, Object> props = new Hashtable<>();
      props.put(CommandProcessor.COMMAND_SCOPE, commandGroupAdapter.getGroupName());
      props.put(CommandProcessor.COMMAND_FUNCTION, names);

      BundleContext bundleContext = serviceReference.getBundle().getBundleContext();
      serviceRegistration = bundleContext.registerService(Object.class.getName(), dispatcher, props);
    }
  }

  private String[] getCommands(CommandGroup commandGroup) {
    if (commandGroup instanceof CommandGroupAdapter) {
      CommandGroupAdapter commandGroupAdapter = (CommandGroupAdapter) commandGroup;
      return commandGroupAdapter.getCommandNames().keySet().toArray(new String[0]);
    }
    return null;
  }

  public void close() {
    if (serviceRegistration == null) {
      return;
    }

    serviceRegistration.unregister();
    serviceRegistration = null;
  }

}

