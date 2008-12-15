/*
 * Copyright (c) 2004-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.oscarshell;

import org.knopflerfish.service.console.*;
import org.ungoverned.osgi.service.shell.*;
import java.io.*;
import java.util.*;
import org.osgi.framework.*;

/**
 * <p>
 * Wrapper class that exposes all Oscar shell commands
 * as a singel Knopflerfish console command group.
 * </p>
 *
 * <p>
 * This class will only get commands on demand (help and execute), and
 * will not keep them for longer than necessary.
 * </p>
 */
public class OscarShellCommandGroup implements CommandGroup {

  /**
   * The group name is always "oscar"
   */
  public String getGroupName() {
    return "oscar";
  }

  public String getShortHelp() {
    return "Commands available via the Oscar shell API";
  }

  /**
   * Build the long help text by listing all registered
   * Oscar commands.
   */
  public String getLongHelp() {
    StringBuffer sb = new StringBuffer();

    sb.append(getShortHelp());
    sb.append("\n");

    ServiceReference[] srl = getCommandReferences(null);
    if(srl.length == 0) {
      sb.append(" No Oscar commands installed\n");
    }

    for(int i = 0; i < srl.length; i++) {
      StringBuffer line = new StringBuffer();
      line.append(" ");
      try {
        Command cmd = (Command)Activator.bc.getService(srl[i]);

        line.append(cmd.getName());

        while(line.length() < 12) {
          line.append(" ");
        }
        line.append(" - ");
        line.append(cmd.getShortDescription());


      } catch (Exception e) {
        sb.append("Failed to get command service #" + srl[i].getProperty("service.id"));
      } finally {
        Activator.bc.ungetService(srl[i]);
      }
      sb.append(line.toString());

      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * <p>
   * Execute a single Oscar shell command by using the first
   * argument string as command name, try to get this command
   * service, (re)build  the command line, and finally call
   * <tt>Command.execute</tt>
   * </p>
   * <p>
   * Arguments containing spaces are surrounded with <tt>"</tt>
   * before calling <tt>Command.execute</tt>
   * </p>
   * <p>
   * If the command does not exist, or fails, print this on the
   * output stream and return 1.
   * </p>
   */
  public int execute(String[] args,
                     Reader in,
                     PrintWriter out, Session session) {
    if (args.length<1) {
      out.println("ERROR: No oscar command specified.");
      return 1;
    }

    String name = args[0].trim();
    ServiceReference[] srl = getCommandReferences(name);

    if(srl == null || srl.length == 0) {
      out.println("ERROR: No oscar command: " + name);
      return 1;
    }
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < args.length; i++) {
      boolean bCit = -1 != args[i].indexOf(" ");
      if(bCit) {
        sb.append("\"");
      }
      sb.append(args[i]);
      if(bCit) {
        sb.append("\"");
      }
      if(i < args.length - 1) {
        sb.append(" ");
      }
    }

    try {
      Command cmd = (Command)Activator.bc.getService(srl[0]);

      // Create a wrapper stream for both output and error
      // streams
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      PrintStream outStream = new PrintStream(bout);

      // ...and call the command
      cmd.execute(sb.toString(),outStream, outStream);

      // grab the resulting string and feed it to the console
      String result = bout.toString();
      out.println(result);

    } catch (Exception e) {
      out.println("Failed to call " + args[0] + ": " + e);
      return 1;
    } finally {
      // alway unget the service
      Activator.bc.getService(srl[0]);
    }
    return 0;
  }


  /**
   * <p>
   * Get the currently registered Oscar Command services matching a
   * given name.
   * </p>
   *
   * @param name name of command to get. If <tt>null</tt>, return
   *             all commands.
   * @return array of ServiceReference to Command services. Zero
   *         services is representes by a zero.sized array.
   *
   */
  protected ServiceReference[] getCommandReferences(String name) {
    String filter =
      "(objectclass=" + Command.class.getName() + ")";

    ServiceReference[] srl = null;

    Vector v = new Vector();
    try {
      // Get all services, then filter if necessary.
      srl = Activator.bc.getServiceReferences(null, filter);
      if(name != null) {
        for(int i = 0; srl != null && i < srl.length; i++) {
          Command cmd = (Command)Activator.bc.getService(srl[i]);
          if(name.equals(cmd.getName())) {
            v.addElement(srl[i]);
          }
        }
        srl = new ServiceReference[v.size()];
        v.copyInto(srl);
      }
    } catch(InvalidSyntaxException e) {
      throw new RuntimeException("Bad filter: " + filter + ", " + e);
    }

    if(srl == null) {
      srl = new ServiceReference[0];
    }
    return srl;
  }
}
