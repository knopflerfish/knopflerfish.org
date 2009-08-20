/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing.console;

import java.io.OutputStream;

import javax.swing.JTextArea;


public class TextAreaOutputStream
  extends OutputStream
{
  final SwingIO swingIO;
  final JTextArea text;

  final static int BUFFER_SIZE = 1024;
  final byte[] buffer = new byte[BUFFER_SIZE];
  int pointer = 0;

  TextAreaOutputStream(SwingIO swingIO, JTextArea text)
  {
    this.swingIO = swingIO;
    this.text  = text;
  }

  public void flush()
  {
    final String line = new String(buffer, 0, pointer);
    text.append(line);
    pointer = 0;
  }

  public void write(int i)
  {
    byte b = (byte) i;

    if (b == '\r') {
      // Ignore; swing components does not use \r
    } else if(b == '\n') {
      final String line = new String(buffer, 0, pointer);
      text.append(line +"\n");
      pointer = 0;
      swingIO.showLastLine();
    } else {
      buffer[pointer++] = b;
      if(pointer>=BUFFER_SIZE){
        final String line =new String(buffer,0,pointer);
        text.append(line);
        pointer = 0;
      }
    }
  }
}
