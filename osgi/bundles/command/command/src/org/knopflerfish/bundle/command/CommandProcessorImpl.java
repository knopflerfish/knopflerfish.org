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
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;
import org.osgi.service.threadio.ThreadIO;
import org.osgi.util.tracker.ServiceTracker;

public class CommandProcessorImpl implements CommandProcessor {
  protected final Set<CommandSessionImpl> sessions = new HashSet<>();
  
  ServiceTracker<ThreadIO, ThreadIO> tioTracker;
  CommandProvidersService commandProviders;
  Bundle b;

  CommandProcessorImpl(Bundle b) {
    this.b = b;
    tioTracker = new ServiceTracker<>(Activator.bc,
                                      ThreadIO.class.getName(),
                                      null);
    tioTracker.open();

    commandProviders = new CommandProvidersService();      
    commandProviders.open();
  }
  
  public CommandSession createSession(InputStream in, PrintStream out, PrintStream err) {
    synchronized (sessions) {
      CommandSessionImpl cs = new CommandSessionImpl(this, in, out, err);
      cs.init();
      sessions.add(cs);
      return cs;
    }

  }

  public void stop() {
    synchronized (sessions) {
      tioTracker.close();
      tioTracker = null;
      for (CommandSessionImpl cs : sessions) {
        cs.close();
      }
      sessions.clear();

      commandProviders.close();
      commandProviders = null;

      b = null;
    }
  }
}
