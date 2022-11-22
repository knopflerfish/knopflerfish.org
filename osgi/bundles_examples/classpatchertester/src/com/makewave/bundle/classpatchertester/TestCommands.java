/*
 * Copyright (c) 2022, KNOPFLERFISH project
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

package com.makewave.bundle.classpatchertester;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleContext;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;

public class TestCommands
    extends CommandGroupAdapter
    implements Runnable
{

  @SuppressWarnings("unused")
  public TestCommands(BundleContext bc) {
    super("patchtest", "Class patcher commands");
  }

  @SuppressWarnings("unused")
  public final static String USAGE_EXIT = "[-bare]";

  @SuppressWarnings("unused")
  public final static String[] HELP_EXIT = new String[] {
      "Run System.exit(0) to check that the class patcher has worked",
      "and that the framework will survive. If it works, this bundle will be",
      "stopped.",
      "-bare       Don't create a thread.",
      "            Things might hang since stopping the bundle includes",
      "            unregistering the CommandGroup and unregistering waits",
      "            for the command to finish and the command waits for the",
      "            bundle to be stopped.",
  };

  @SuppressWarnings("unused")
  public int cmdExit(
      Dictionary<?, ?> opts,
      Reader in,
      PrintWriter out,
      Session session) {
    final boolean useThread = opts.get("-bare") == null;
    if (useThread) {
      new Thread(this).start();
    } else {
      run();
    }
    return 0;
  }

  /**
   * System.exit is expected to be replaced by a call to ClassPatcherWrappers,
   * which stops the calling bundle instead of killing the JVM.
   * Since things hang if the bundle tries to stop itself in a command,
   * do this in a separate Thread.
   */
  @Override
  public void run() {
    System.exit(0);
  }

  @SuppressWarnings("unused")
  public final static String USAGE_FORNAME = "[<className>]";

  @SuppressWarnings("unused")
  public final static String[] HELP_FORNAME = new String[] {
      "Run Class.forName(className) to check that the class patcher has worked.",
      "If it works, the class will be loaded with the bundle class",
      "loader as fallback.",
      "<className>  The name of the class to be loaded.",
      "             Default: java.util.Arrays"
  };

  /**
   * Class.forName is expected to be replaced by a call to ClassPatcherWrappers
   * which uses fallback to bundle class loader.
   */
  @SuppressWarnings("unused")
  public int cmdForname(
      Dictionary<?, ?> opts,
      Reader in,
      PrintWriter out,
      Session session) {
    try {
      String className = (String) opts.get("className");
      if (className == null) {
        className = "java.util.Arrays";
      }

      Class.forName(className);

      return 0;
    } catch (ClassNotFoundException e) {
      out.print("Class loading failed: ");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
      return 1;
    }
  }
}
