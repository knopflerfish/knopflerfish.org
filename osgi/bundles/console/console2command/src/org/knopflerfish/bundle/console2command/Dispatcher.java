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

import java.io.*;
import java.util.*;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;

public class Dispatcher {
  private CommandGroupAdapter commandGroupAdapter;

  public Dispatcher( CommandGroupAdapter commandGroupAdapter) {
    this.commandGroupAdapter = commandGroupAdapter;
  }
  
  public String toString() {
    return "Dispatcher[" + "cg=" + commandGroupAdapter + "]";
  }
  
  public void main(Object[] args0) {
    try {
      String name = args0[0].toString();

      CommandGroupAdapter.DynamicCmd command =
        new CommandGroupAdapter.DynamicCmd(commandGroupAdapter, name);
      
      StringWriter sout = new StringWriter();
      PrintWriter out = new PrintWriter(sout);
      
      String[] args = new String[args0.length];
      for(int i = 0; i < args0.length; i++) {
        args[i] = args0[i] != null ? args0[i].toString() : "";
      }
      Dictionary<String, Object> opts = commandGroupAdapter.getOpt(args, command.usage);

      Reader in = new InputStreamReader(System.in);
      Session session = null;
      command.cmd.invoke(commandGroupAdapter, opts, in, out, session);
      
      System.out.println(sout.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
