/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.httpconsole;
	
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * A Command represents an item in teh ConsoleServlet that
 * can be activated by a certain trigger. When triggered, the
 * <tt>run</tt> method is called. For each request, the
 * <tt>HTMLable.toHTML</tt> method is called to produce the
 * output.
 */
public interface Command extends HTMLable {

  /**
   * Command wants full screen when run
   */
  public static int DISPLAY_FULLSCREEN  = 0x0001;

  /**
   * Command wants compact listing of bundles when run
   */
  public static int DISPLAY_COMPACTLIST = 0x0002;

  /**
   * Returns a bit-wise combination of the DISPLAY flags
   */
  public int getDisplayFlags();

  /**
   * Called when ConsoleServet har decided the command is triggered.
   * Typically, this happens when <tt>isTrigger</tt> has returned
   * true.
   */
  public StringBuffer run(HttpServletRequest request);

  /**
   * Id of the command. Typically used to identify a submit button.
   */
  public String       getId();

  /**
   * Should return true if command thinks the specified request
   * should trigger the <tt>run</tt> method.
   */
  public boolean      isTrigger(HttpServletRequest request);

  /**
   * Human-readable name of command.
   */
  public String       getName();

  /**
   * Human-readable description of command.
   */
  public String       getDescription();

  /**
   * URL string to command icon. Can be null.
   */
  public String       getIcon();
}
