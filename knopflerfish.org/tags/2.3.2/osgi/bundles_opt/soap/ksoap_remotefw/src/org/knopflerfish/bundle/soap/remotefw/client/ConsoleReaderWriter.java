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
package org.knopflerfish.bundle.soap.remotefw.client;

import java.io.PrintWriter;
import java.io.Reader;

import org.knopflerfish.service.soap.remotefw.RemoteFW;

public class ConsoleReaderWriter {

  RemoteFW fw;
  Reader in;
  PrintWriter out;
  Thread runner = null;
  boolean bRun = false;

  public ConsoleReaderWriter(RemoteFW fw, Reader in, PrintWriter out) {
    this.fw = fw;
    this.in = in;
    this.out = out;
    start();
  }

  void start() {
    if(runner == null) {
      runner = new Thread() {
          public void run() {
            StringBuffer buf = new StringBuffer();
            while(bRun) {
              try {
                char ch = 0;
                do {
                  int i = in.read();
                  if (i != -1) {
                    ch = (char) i;
                    if (ch != '\r' && ch != '\n') {
                      buf.append(ch);
                    }
                  }
                } while (bRun && ch != '\n');
                if (buf.length() == 0) { // No command - new prompt
                  out.print("> ");
                  out.flush();
                } else {
                  String result = fw.runCommand(buf.toString());
                  out.print(result);
                  out.flush();
                }
                buf = new StringBuffer();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        };
      bRun = true;
      runner.start();
    }
  }

  void stop() {
    if(runner != null) {
      bRun = false;
      runner = null;
    }
  }

}
