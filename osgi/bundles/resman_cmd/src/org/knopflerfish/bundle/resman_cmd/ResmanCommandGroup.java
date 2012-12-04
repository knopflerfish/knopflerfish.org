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

package org.knopflerfish.bundle.resman_cmd;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;

import org.osgi.framework.BundleContext;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;


/**
 * Console commands for interaction with the resource management
 * service.
 *
 * @author Gunnar Ekolin, Makewave AB
 */

public class ResmanCommandGroup
  extends CommandGroupAdapter
{
  final BundleContext bc;

  private final ResourceManagerHelper resmanHelper;

  ResmanCommandGroup(BundleContext bc) {
    super("resman", "Resource management commands");
    this.bc = bc;

    // The resman service is a framework singleton internal services
    // thus, we take a shortcut and skip the service tracking

    resmanHelper = initResmanHelper();
  }

  private ResourceManagerHelper initResmanHelper() {
    // Try to see if we can create the ResmanHelper object.
    try {
      return new ResourceManagerHelperImpl(bc);
    } catch (Exception ex) {
      //log.error("Failed to create permissionAdminHelper: " + ex, ex);
    } catch (LinkageError ce) {
      //log.info("There is no PermissionAdmin service available.", ce);
    }
    return null;
  }


  //
  // usage
  //

  public final static String USAGE_USAGE = "[<bundle>] ...";

  public final static String[] HELP_USAGE = new String[] {
    "Display bundle resource consumption.",
    "<bundle>  Name or id of bundles to show.",
    "          If bundle list is empty, show all bundles."
  };

  public int cmdUsage(Dictionary opts,
                      Reader in,
                      PrintWriter out,
                      Session session) {
    if (resmanHelper == null) {
      out.println("Resource management service is not available");
      return 1;
    } else {
      return resmanHelper.cmdUsage(opts, in, out, session);
    }
  }

}
