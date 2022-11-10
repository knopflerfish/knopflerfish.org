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

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Converter;
import org.osgi.service.threadio.ThreadIO;

public class CommandSessionImpl implements CommandSession {
  private CommandProcessorImpl cp;
  private InputStream in;
  private PrintStream out;
  private PrintStream err;

  private final Map<String, Object> sessionVars = new HashMap<>();
  
  CommandSessionImpl(CommandProcessorImpl cp,
                     InputStream in, 
                     PrintStream out, 
                     PrintStream err) {
    this.cp  = cp;
    this.in  = in;
    this.out = out;
    this.err = err;
  }

  void init() {
  }

  public Object convert(@SuppressWarnings("rawtypes") Class type, Object in) {
    ServiceReference<Converter> sr = null;
    Converter converter = null;
    try {
      String filter = "(" + Converter.CONVERTER_CLASSES + "=" + type.getName() + ")";
      Collection<ServiceReference<Converter>> srl
          = Activator.bc.getServiceReferences(Converter.class, filter);
      if (srl == null || srl.isEmpty()) {
        throw new RuntimeException("No converter for type=" + type.getName());
      }
      sr = srl.iterator().next();
      converter = Activator.bc.getService(sr);

      return converter.convert(type, in);

    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("Bad filter:" + e);
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert " + in + " to " + type.getName() + ", " + e); 
    } finally {
      if (converter != null) {
        Activator.bc.ungetService(sr);
      }
    }
  }
  
  public Object execute(CharSequence commandline) {
    return execute(commandline, in, out, err);
  }
  
  public Object execute(CharSequence commandline,                           
                        InputStream in,   
                        PrintStream out, 
                        PrintStream err) {
    ThreadIO tio = cp.tioTracker.getService();
    if(tio == null) {
      throw new RuntimeException("No ThreadIO service available");
    }
    try {
      tio.setStreams(in, out, err);

      Program p = new Program(null, cp.commandProviders);
      p.getVarMap().putAll(sessionVars);

      Object r = p.exec(commandline);
      sessionVars.putAll(p.getVarMap());
      return r;
        
    } finally {
      tio.close();
    }
  }

  public void close() {
    this.in  = null;
    this.out = null;
    this.err = null;
  }


  public InputStream getKeyboard() {
    throw new RuntimeException("NYI");
  }

  public PrintStream getConsole() {
    throw new RuntimeException("NYI");
  }

  public Object get(String name) {
    synchronized(sessionVars) {
      return sessionVars.get(name);
    }
  }

  public void put(String name, Object value) {
    synchronized(sessionVars) {
      sessionVars.put(name, value);
    }
  }

  public CharSequence format(Object target, int level) {
    throw new RuntimeException("NYI");
  }

}
