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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * * This extension of FilterOutputStream makes the following * translations on
 * data in the output stream * * CR LF to no change * CR n, when n != LF to CR
 * NULL n * n LF to n CR LF * * All characters in the output buffer are * anded
 * with a mask from the session. * This mask is either 0x7F (7 bit) or 0xFF (8
 * bit) * * It also has a clear method to empty the output buffer * and a
 * writeCommand method to send telnet commands. *
 */

public class TelnetOutputStream extends FilterOutputStream {
    private TelnetSession ts;

    private ByteArrayOutputStream baos; // Intermediate byte array stream

    private DataOutputStream dos2; // Internal DataOutputStream

    private boolean prevWasCR = false; // True if previous char was CR

    public TelnetOutputStream(OutputStream os, TelnetSession ts) {
        super(os);
        this.ts = ts;
        OutputStream os1 = super.out; // get the underlaying stream
        baos = new ByteArrayOutputStream(); // intermediate array stream
        super.out = baos; // replace underlying Stream with a
                            // ByteArrayOutputStream
        dos2 = new DataOutputStream(os1); // a new OutputStream that uses the
        // original output stream
    }

    /**
     * * Override flush and close methods,
     */

    public void flush() throws IOException {
        scanBuf();
        dos2.flush();
    }

    public void close() throws IOException {
        flush();
        baos.close();
        dos2.close();
    }

    /**
     * * Override all write methods, add CRLF when appropriate
     */

    public void write(byte[] b) throws IOException {
        baos.write(b);
    }

    public void write(byte[] b, int off, int len) {
        baos.write(b, off, len);
    }

    public void write(int b) {
        baos.write(b);
    }

    /**
     * * Scan the output buffer for: * * CR LF to no change * CR n, when n != LF
     * to CR NULL n * n LF to n CR LF *
     */

    private synchronized void scanBuf() throws IOException {
        byte[] buf = baos.toByteArray();
        baos.reset();
        byte mask = (byte) ts.getMask();

        for (int i = 0; i < buf.length; i++) {
            if (prevWasCR == true) {
                if (buf[i] != (byte) TCC.LF) { // add a LF
                    dos2.write((byte) TCC.LF);
                    dos2.write(buf[i] & mask);
                } else {
                    dos2.write(buf[i] & mask);
                }
            } else {
                if (buf[i] == (byte) TCC.LF) { // a single LF -> CRLF
                    dos2.write((byte) TCC.CR);
                    dos2.write((byte) TCC.LF);
                } else {
                    dos2.write(buf[i] & mask);
                }
            }
            prevWasCR = (buf[i] == (byte) TCC.CR) ? true : false;
        }
    }

    public synchronized void reset() {
        baos.reset();
    }

    /**
     * * Write a telnet command in the output stream. * *
     * 
     * @parameter tc, command string to write
     */

    public synchronized void writeCommand(String tc) throws IOException {
        if (tc != null) {
            dos2.writeBytes(tc);
            dos2.flush();
        }
    }
}
