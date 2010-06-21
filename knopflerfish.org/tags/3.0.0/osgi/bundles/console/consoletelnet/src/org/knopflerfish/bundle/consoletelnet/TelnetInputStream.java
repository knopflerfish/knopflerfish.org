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

package org.knopflerfish.bundle.consoletelnet;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * * Reads an input stream and extracts telnet commands. * When a command is
 * found a callback is made to the * telnet session. * * The following
 * transformations are made: * * CR null to CR * CR LF to CR * CR n, where n is
 * not null or LF is discarded * IAC IAC to IAC * * in the data part of the
 * input stream. * * While parsing telnet commands no transforms * except IAC
 * IAC to IAC are made. * * A state machine is used to parse the telnet
 * commands.
 */

public class TelnetInputStream extends FilterInputStream {
    private TelnetStateMachine tsm;

    private int prevChar;

    private int thisChar;

    /*
     * public TelnetInputStream (InputStream is) { super(is); }
     */

    public TelnetInputStream(InputStream is, TelnetSession tels) {
        super(is);
        tsm = new TelnetStateMachine(tels);
        prevChar = -1;
    }

    public int available() throws IOException {
        return in.available();
    }

    public void close() throws IOException {
        in.close();
    }

    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    public boolean markSupported() {
        return in.markSupported();
    }

    public int read() throws IOException {
        scanStream: while (true) {
            prevChar = thisChar;
            thisChar = in.read();

            // System.out.println("TelnetIS char=" + thisChar);

            if (thisChar == -1) {
                break scanStream;
            }
            if (prevChar == TCC.IAC && thisChar == TCC.IAC) {
                // Strip one double written IAC from input stream
                continue scanStream;
            }
            if (tsm.getState() == 0 && thisChar == TCC.IAC) {
                // Detect command start
                tsm.nextState(tsm.getState(), thisChar);
                continue scanStream;
            }
            if (tsm.getState() != 0) {
                // Continue commad extraction
                tsm.nextState(tsm.getState(), thisChar);
                continue scanStream;
            }
            break scanStream;
        }
        return thisChar;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int count = 0;
        int character = 0;
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

    public void reset() throws IOException {
        in.reset();
    }

    public long skip(long n) throws IOException {
        return in.skip(n);
    }
}
