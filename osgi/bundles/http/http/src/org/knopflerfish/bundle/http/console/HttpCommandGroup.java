/*
 * Copyright (c) 2012, KNOPFLERFISH project
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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;
import java.io.Reader;
import java.io.PrintWriter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;

import org.knopflerfish.bundle.http.HttpServerFactory;
import org.knopflerfish.bundle.http.HttpServer;
import org.knopflerfish.bundle.http.HttpConfig;
import org.knopflerfish.bundle.http.Registrations;
import org.knopflerfish.bundle.http.Registration;
import org.knopflerfish.bundle.http.ServletRegistration;
import org.knopflerfish.bundle.http.ResourceRegistration;
import org.knopflerfish.bundle.http.TransactionManager;

/**
 * Console command group for the HTTP server
 *
 */
public class HttpCommandGroup extends CommandGroupAdapter {

  private String path = ".";
  HttpServerFactory serverFactory;

  public HttpCommandGroup(BundleContext bc, HttpServerFactory serverFactory) {
    super("http", "HTTP server commands");
    this.serverFactory = serverFactory;

    Dictionary props = new Hashtable();
    props.put(CommandGroup.GROUP_NAME, getGroupName());
    bc.registerService(CommandGroup.class.getName(), this, props);
  }
  
  //
  // List command
  //
  public final static String USAGE_LIST = "[-c] [-r] [-t] [-l]";
  public final static String [] HELP_LIST = new String [] {
    "List all the configured HTTP servers",
    "-c  Show configuration info",
    "-r  Show all registrations, servlets and resources",
    "-t  Show info on transactions",
    "-l	 List in long format, same as supplying -c -r -t, providing extensive details"
  };


  public int cmdList(Dictionary opts,
		     Reader in,
		     PrintWriter out,
		     Session session)
  {
    int serverNum = 0;
    boolean doLong = null != opts.get("-l");

    out.println("Configured HTTP Servers");

    for (Enumeration e = serverFactory.getServerPids(); e.hasMoreElements(); ) {
      String pid = (String)e.nextElement();
      out.println("# " + serverNum++ + ": " + pid + " #");
      HttpServer httpServer = (HttpServer)serverFactory.getServer(pid);
      HttpConfig config = httpServer.getHttpConfig();
      if (config.isHttpEnabled()) 
	out.println("  http: " + config.getHttpPort() 
		    + "  " + (httpServer.isHttpOpen() ? "Open" : "Closed"));
      if (config.isHttpsEnabled()) 
	out.println("  https: " + config.getHttpsPort() 
		    + "  " + (httpServer.isHttpsOpen() ? "Open" : "Closed (pending no SSL Server Socket Factory registered)"));
      
      if (doLong || null != opts.get("-c")) {
	Dictionary d = config.getConfiguration();
	out.println("  Configuration");
	printDictionary(out, d, 2);
      }
      if (doLong || null != opts.get("-r")) {
	out.println("  Registrations");
	Registrations regs = httpServer.getRegistrations();
	for (Enumeration e2 = regs.getAliases(); e2.hasMoreElements(); ) {
	  String alias = (String)e2.nextElement();
	  Registration reg = regs.get(alias);
	  if ("".equals(alias))
	    alias = "/";
	  out.println("    '" + alias + "'");
	  if (reg instanceof ServletRegistration)
	    out.print("      Servlet");
	  else
	    out.print("      Resource");
	  Bundle bndl = (Bundle)reg.getOwner();
	  if (bndl != null) 
	    out.println(" registered by bundle: #" + bndl.getBundleId() + " - " + bndl.getSymbolicName());
	  else
	    out.println(" NONE - this indicates an error");
	}
      }
      if (doLong || null != opts.get("-t")) {
	out.println("  Transactions");
	TransactionManager transManager = httpServer.getTransactionManager();
	out.println("    " + "Thread Group: " + transManager.getName());
	out.println("    " + "Active Threads: " + transManager.activeCount());
	out.println("    " + "Transactions Handled: " + transManager.getTransactionCount());
      }
    }
    return 0;
  }

  private static void printDictionary(PrintWriter out, Dictionary d, int level) {
    StringBuffer blanklead = new StringBuffer();
    for (int i = 0; i < level; i++) {
      blanklead.append("  ");
    }
    for (Enumeration e = d.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      Object val = d.get(key);
      if (HttpConfig.MIME_PROPS_KEY.equals(key.toString()) && val instanceof Vector) {
	out.println(blanklead.toString() + key + ":");
	printVectorArray(out, (Vector)val, level+1);
      }
      else
	out.println(blanklead.toString() + key + ":" + " " + ((val != null) ? val : ""));
    }
  }
  
  private static void printVectorArray(PrintWriter out, Vector v, int level) {
    StringBuffer blanklead = new StringBuffer();
    for (int i = 0; i < level; i++) {
      blanklead.append("  ");
    }
    for (Enumeration e = v.elements(); e.hasMoreElements(); ) {
      String[] strarr = (String[])e.nextElement();
      out.print(blanklead.toString());
      for (int j = 0; j < strarr.length; j++) {
	out.print(strarr[j] + " ");
      }
      out.println();
    }
  }

}

