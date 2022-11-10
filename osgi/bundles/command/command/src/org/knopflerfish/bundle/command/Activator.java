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

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.Converter;

import org.knopflerfish.bundle.command.commands.CommandCommands;
import org.knopflerfish.bundle.command.commands.EchoCmd;
import org.knopflerfish.bundle.command.commands.FrameworkCommands;

public class Activator implements BundleActivator {

  static BundleContext bc;  

  CommandProcessorFactory cpFactory;

  @SuppressWarnings("ConstantConditions")
  public void start(BundleContext bc) {
    Activator.bc = bc;
    
    {
      Hashtable<String, Object> props = new Hashtable<>();
      props.put(Converter.CONVERTER_CLASSES, JavaLangConverter.CLASSES_STRINGS);
      bc.registerService(Converter.class.getName(), new JavaLangConverter(), props);
    }

    {
      Hashtable<String, Object> props = new Hashtable<>();
      props.put(Converter.CONVERTER_CLASSES, FrameworkConverter.CLASSES_STRINGS);
      bc.registerService(Converter.class.getName(), new FrameworkConverter(), props);
    }
    
    {
      Hashtable<String, Object> props = new Hashtable<>();
      props.put(CommandProcessor.COMMAND_SCOPE, EchoCmd.SCOPE);
      props.put(CommandProcessor.COMMAND_FUNCTION, EchoCmd.FUNCTION);
      bc.registerService(Object.class.getName(), new EchoCmd(), props);
    }

    {
      Hashtable<String, Object> props = new Hashtable<>();
      props.put(CommandProcessor.COMMAND_SCOPE, CommandCommands.SCOPE);
      props.put(CommandProcessor.COMMAND_FUNCTION, CommandCommands.FUNCTION);
      bc.registerService(Object.class.getName(), new CommandCommands(bc), props);
    }

    if (false) {
      Hashtable<String, Object> props = new Hashtable<>();
      props.put(CommandProcessor.COMMAND_SCOPE, FrameworkCommands.SCOPE);
      props.put(CommandProcessor.COMMAND_FUNCTION, FrameworkCommands.FUNCTION);
      bc.registerService(Object.class.getName(), new FrameworkCommands(bc), props);
    }
    
    {
      cpFactory = new CommandProcessorFactory();
      Hashtable<String, Object> props = new Hashtable<>();
      bc.registerService(CommandProcessor.class.getName(), cpFactory, props);
    }

  }
  
  public void stop(BundleContext bc) {
    Activator.bc = null;
  }

}
