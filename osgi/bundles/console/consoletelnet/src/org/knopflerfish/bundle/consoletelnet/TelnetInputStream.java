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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads an input stream and extracts telnet commands.  When a command
 * is found a callback is made to the telnet session.
 *
 * <p>The following transformations are made in the data part of the
 *  input stream:<br>
 *  <table>
 *    <tr><th>From</th><th>To</th></tr>
 *    <tr><td>CR null</td><td>CR</td></tr>
 *    <tr><td>CR LF</td><td>CR</td></tr>
 *    <tr><td>CR n</td><td>where n is not null or LF is discarded</td></tr>
 *    <tr><td>IAC IAC</td><td>IAC</td></tr>
 *  </table>
 *
 * While parsing telnet commands no transforms
 * except IAC IAC to IAC are made.
 *
 * A state machine is used to parse the telnet commands.
 */

public class TelnetInputStream
  extends FilterInputStream
{
    private TelnetStateMachine telnetStateMachine;
    private int prevChar;
    private int thisChar;

    public TelnetInputStream(InputStream is, TelnetSession telnetSession) {
        super(is);
        telnetStateMachine = new TelnetStateMachine(telnetSession);
        prevChar = -1;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void mark(int readLimit) {
        in.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws IOException {
        while (true) {
            prevChar = thisChar;
            thisChar = in.read();

            if (thisChar == -1) {
                break;
            }
            if (prevChar == TCC.IAC && thisChar == TCC.IAC) {
                // Strip one double written IAC from input stream
                continue;
            }
            if (telnetStateMachine.getState() == 0 && thisChar == TCC.IAC) {
                // Detect command start
                telnetStateMachine.nextState(telnetStateMachine.getState(), thisChar);
                continue;
            }
            if (telnetStateMachine.getState() != 0) {
                // Continue command extraction
                telnetStateMachine.nextState(telnetStateMachine.getState(), thisChar);
                continue;
            }
            break;
        }
        return thisChar;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = 0;
        int character;
        while (count < (len - off) && in.available() > 0) {
            character = read();
            if (character == -1) {
                if (count != 0) {
                    return count;
                }
                return -1;
            }
            b[off + count] = (byte) character;
            count++;
        }
        return count;
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }
}
