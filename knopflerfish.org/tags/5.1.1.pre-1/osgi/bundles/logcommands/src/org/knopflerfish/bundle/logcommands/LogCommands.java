/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.logcommands;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.log.LogConfig;

/**
 * Bundle activator implementation.
 *
 * @author Jan Stein
 */
public class LogCommands
  implements BundleActivator
{
  static BundleContext bc;

  static ServiceTracker<LogConfig, LogConfig> logConfigTracker;

  /*---------------------------------------------------------------------------*
   *			  BundleActivator implementation
   *---------------------------------------------------------------------------*/

  /**
   * Called by the framework when this bundle is started.
   *
   * @param bc
   *          Bundle context.
   */
  public void start(BundleContext bc)
  {
    LogCommands.bc = bc;

    // Create service tracker for log config service
    LogCommands.logConfigTracker =
      new ServiceTracker<LogConfig, LogConfig>(bc, LogConfig.class, null);
    LogCommands.logConfigTracker.open();

    // Register log commands
    final CommandGroup logCommandGroup = new LogCommandGroup(bc);
    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put("groupName", logCommandGroup.getGroupName());
    bc.registerService(CommandGroup.class, logCommandGroup, props);

    // Register log config commands
    final CommandGroup logConfigCommandGroup = new LogConfigCommandGroup();
    props = new Hashtable<String, Object>();
    props.put("groupName", logConfigCommandGroup.getGroupName());
    bc.registerService(CommandGroup.class, logConfigCommandGroup, props);
  }

  /**
   * Called by the framework when this bundle is stopped.
   *
   * @param bc
   *          Bundle context.
   */
  public void stop(BundleContext bc)
  {
    // Close service tracker for log config service
    if (LogCommands.logConfigTracker != null) {
      LogCommands.logConfigTracker.close();
    }

  }

}
