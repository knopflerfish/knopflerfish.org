/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.io.*;
import java.net.*;


/**
 * Reference URL handling, used for accepting file: references.
 *
 * Accepts URLs on the form
 * <pre>
 *  reference:file URL]
 * </pre>
 * Where <tt>[file URL]</tt> is any valid file: URL. 
 *
 * <p>
 * <tt>openConnection</tt> simply returns the URLConnection
 * created by removing the <tt>reference:</tt> prefix.
 * </p>
 *
 */
public class ReferenceURLStreamHandler extends URLStreamHandler {

  final public static String PROTOCOL = "reference";

  ReferenceURLStreamHandler() {
    super();
  }

  /**
   *
   * @throws IOException if the specified URL is not a reference to a
   *                     file: URL
   */
  public URLConnection openConnection(URL url) throws IOException {
    URL actual = new URL(getActual(url));
    
    if(!"file".equals(actual.getProtocol())) {
      throw 
	new IOException("Only file: URLs are allowed as references, got " + url);
    }

    return actual.openConnection();
  }


  /**
   * Get the actual URL string represented by the specified 
   * <tt>reference:</tt> URL.
   *
   * @throws IllegalArgumentException if the specified URL does not
   *                                  have a reference: protocol.
   */
  protected static String getActual(URL u) {
    String s = u.toString();
    if(!s.startsWith(PROTOCOL + ":")) {
      throw new IllegalArgumentException("URL " + u + " does not start with " + 
					 PROTOCOL + ":");
    }
    return s.substring(PROTOCOL.length() + 1);
  }    
}
