/*
 * Copyright (c) 2012-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.http.console;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.knopflerfish.bundle.http.HttpConfig;
import org.knopflerfish.bundle.http.HttpServer;
import org.knopflerfish.bundle.http.HttpServerFactory;
import org.knopflerfish.bundle.http.Registration;
import org.knopflerfish.bundle.http.Registrations;
import org.knopflerfish.bundle.http.ServletRegistration;
import org.knopflerfish.bundle.http.TransactionManager;
import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;

/**
 * Console command group for the HTTP server
 *
 */
public class HttpCommandGroup
  extends CommandGroupAdapter
{
  HttpServerFactory serverFactory;

  public HttpCommandGroup(BundleContext bc, HttpServerFactory serverFactory)
  {
    super("http", "HTTP server commands");
    this.serverFactory = serverFactory;

    final Dictionary<String, String> props = new Hashtable<String, String>();
    props.put(CommandGroup.GROUP_NAME, getGroupName());
    bc.registerService(CommandGroup.class.getName(), this, props);
  }

  //
  // List command
  //
  public final static String USAGE_LIST = "[-c] [-r] [-t] [-l]";
  public final static String[] HELP_LIST =
    new String[] {
                  "List all the configured HTTP servers",
                  "-c  Show configuration info",
                  "-r  Show all registrations, servlets and resources",
                  "-t  Show info on transactions",
                  "-l  List in long format, same as supplying -c -r -t, providing extensive details" };

  public int cmdList(Dictionary<?, ?> opts,
                     Reader in,
                     PrintWriter out,
                     Session session)
  {
    int serverNum = 0;
    final boolean doLong = null != opts.get("-l");

    out.println("Configured HTTP Servers");

    for (final Enumeration<?> e = serverFactory.getServerPids(); e.hasMoreElements();) {
      final String pid = (String) e.nextElement();
      out.println("# " + serverNum++ + ": " + pid + " #");
      final HttpServer httpServer = serverFactory.getServer(pid);
      final HttpConfig config = httpServer.getHttpConfig();
      if (config.isHttpEnabled()) {
        out.println("  http: " + config.getHttpPort() + "  "
                    + (httpServer.isHttpOpen() ? "Open" : "Closed"));
      }
      if (config.isHttpsEnabled()) {
        out.println("  https: "
                    + config.getHttpsPort()
                    + "  "
                    + (httpServer.isHttpsOpen()
                      ? "Open"
                      : "Closed (pending no SSL Server Socket Factory registered)"));
      }

      if (doLong || null != opts.get("-c")) {
        final Dictionary<?, ?> d = config.getConfiguration();
        out.println("  Configuration");
        printDictionary(out, d, 2);
      }
      if (doLong || null != opts.get("-r")) {
        out.println("  Registrations");
        final Registrations regs = httpServer.getRegistrations();
        for (final Enumeration<?> e2 = regs.getAliases(); e2.hasMoreElements();) {
          String alias = (String) e2.nextElement();
          final Registration reg = regs.get(alias);
          if ("".equals(alias)) {
            alias = "/";
          }
          out.println("    '" + alias + "'");
          if (reg instanceof ServletRegistration) {
            out.print("      Servlet");
          } else {
            out.print("      Resource");
          }
          final Bundle bndl = (Bundle) reg.getOwner();
          if (bndl != null) {
            out.println(" registered by bundle: #" + bndl.getBundleId() + " - "
                        + bndl.getSymbolicName());
          } else {
            out.println(" NONE - this indicates an error");
          }
        }
      }
      if (doLong || null != opts.get("-t")) {
        out.println("  Transactions");
        final TransactionManager transManager = httpServer.getTransactionManager();
        out.println("    " + "Thread Group: " + transManager.getName());
        out.println("    " + "Active Threads: " + transManager.activeCount());
        out.println("    " + "Active Transactions: " + transManager.getActiveTransactionCount());
        out.println("    " + "Transactions Handled: " + transManager.getTransactionCount());
        out.println("    " + "Requests Handled    : " + transManager.getRequestCount());
      }
    }
    return 0;
  }

  private static void printDictionary(PrintWriter out, Dictionary<?, ?> d, int level)
  {
    final StringBuilder blanklead = new StringBuilder();
    for (int i = 0; i < level; i++) {
      blanklead.append("  ");
    }
    for (final Enumeration<?> e = d.keys(); e.hasMoreElements();) {
      final Object key = e.nextElement();
      final Object val = d.get(key);
      if (HttpConfig.MIME_PROPS_KEY.equals(key.toString())
          && val instanceof Vector) {
        out.println(blanklead.toString() + key + ":");
        printVectorArray(out, (Vector<?>) val, level + 1);
      } else {
        out.println(blanklead.toString() + key + ":" + " "
                    + ((val != null) ? val : ""));
      }
    }
  }

  private static void printVectorArray(PrintWriter out, Vector<?> v, int level)
  {
    final StringBuilder blanklead = new StringBuilder();
    for (int i = 0; i < level; i++) {
      blanklead.append("  ");
    }
    for (final Enumeration<?> e = v.elements(); e.hasMoreElements();) {
      final String[] strarr = (String[]) e.nextElement();
      out.print(blanklead.toString());
      for (final String element : strarr) {
        out.print(element + " ");
      }
      out.println();
    }
  }

}
