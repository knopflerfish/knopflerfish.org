/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.consoletelnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

/**
 * Reads an input stream and extracts telnet TVM character.
 *
 * This class provides a very limited line editing capability.
 */
public class TelnetReader
  extends Reader {

  private final TelnetSession telnetSession;

  private final Vector<Integer> lineBuf = new Vector<>(); // buffer while a line is being edited

  private char[] readyLine; // buffer for a that has been terminated by CR
                            // or LF

  private int rp; // pointer in read buffer line

  private final InputStream is;

  private final boolean busyWait;

  private boolean skipLF = false; // Skip next char if LF

  public TelnetReader(InputStream is, TelnetSession telnetSession, boolean busyWait)
  {
    this.telnetSession = telnetSession;
    this.is = is;
    this.busyWait = busyWait;

    readyLine = new char[0];
    rp = 0;
  }

  @Override
  public void close()
      throws IOException
  {
    is.close();
  }

  @Override
  public boolean ready()
      throws IOException
  {
    // TODO, we should check skipLF
    return is.available() > 0;
  }

  /**
   * Block here until end of line (CR) and then return
   *
   * This method provides very limited line editing functionality
   *
   * CR -&lt; return from read BS -&lt; back one step in buffer
   *
   */
  @Override
  public int read(char[] buf, int off, int len)
      throws IOException
  {
    int count;
    int character;
    Integer tmp;

    count = chkRdyLine(buf, off, len);

    if (count > 0) {
      return count;
    }

    while (true) {
      character = readChar();

      if (character != -1) {

        switch (character) {

        case TCC.BS:
          if (lineBuf.size() > 0) {
            lineBuf.removeElementAt(lineBuf.size() - 1);
            telnetSession.echoChar(' ');
            telnetSession.echoChar(character);
          }
          break;

        case TCC.CR:
        case TCC.LF:
          lineBuf.addElement(character);

          readyLine = new char[lineBuf.size()];
          for (int i = 0; i < lineBuf.size(); i++) {
            tmp = lineBuf.elementAt(i);
            character = tmp;
            readyLine[i] = (char) character;
          }
          rp = 0;

          lineBuf.removeAllElements();
          count = chkRdyLine(buf, off, len);
          return count;

        default:
          lineBuf.addElement(character);
        }
      } else {
        // The input stream is closed; ignore the buffered data
        // and return -1 to to tell next layer that the reader
        // is closed.
        return -1;
      }
    }
  }

  private int readChar()
      throws IOException
  {
    int c;
    while (true) {
      if (busyWait) {
        while (is.available() < 1) {
          try {
            Thread.sleep(20);
          } catch (final Exception ignored) {
          }
        }
      }
      c = is.read();
      if (c != 0) {
        if (skipLF) {
          skipLF = false;
          if (c == TCC.LF) {
            continue;
          }
        }
        break;
      }
    }

    telnetSession.echoChar(c);

    if (c == TCC.CR) {
      skipLF = true;
    }
    return c;
  }

  private int chkRdyLine(char[] buf, int off, int len)
  {
    int count = 0;
    if (rp < readyLine.length) {
      while (count < (len - off)) {
        buf[off + count] = readyLine[rp++];
        count++;
      }
    }
    return count;
  }

  /**
   * * Read input data until CR or LF found;
   */

  public String readLine()
      throws IOException
  {
    final Vector<Character> buf = new Vector<>();
    int character;

    try {
      while (true) {
        character = readChar();
        if (character == TCC.CR) {
          break;
        }

        if (-1 == character) {
          // The input stream is closed; ignore the buffered
          // data and return null to to tell next layer that
          // the reader is closed.
          return null;
        }
        buf.addElement((char) character);
      }

      // Convert to string

      final char[] cbuf = new char[buf.size()];
      for (int i = 0; i < buf.size(); i++) {
        final Character c1 = buf.elementAt(i);
        cbuf[i] = c1;
      }
      return new String(cbuf);
    } catch (final IOException e) {
      e.printStackTrace();
      throw e;
    }
  }
}
