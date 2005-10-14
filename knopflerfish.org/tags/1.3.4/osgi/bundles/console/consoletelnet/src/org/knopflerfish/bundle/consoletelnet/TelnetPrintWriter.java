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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * * This extension of a PrintWriter makes the following * translations on data
 * in the output stream * * CR LF -> no change * CR <n>, when n != LF -> CR NULL
 * <n> * <n> LF -> <n> CR LF * * All characters in the output buffer are * anded
 * with a mask from the session. * This mask is either 0x7F (7 bit) or 0xFF (8
 * bit) * * It also has a clear method to empty the output buffer * and a
 * writeCommand method to send telnet commands. *
 */

public class TelnetPrintWriter extends PrintWriter {
    private TelnetSession ts;

    private CharArrayWriter caw; // Intermediate array writer

    private PrintWriter pw2; // Private PrintWriter

    private boolean prevWasCR = false;

    public TelnetPrintWriter(OutputStream os, TelnetStateMachine tsm,
            TelnetSession ts) {
        super(os);
        this.ts = ts;
        Writer w1 = super.out; // get the underlaying writer
        caw = new CharArrayWriter(); // intermediate array writer
        super.out = caw; // replace underlying Writer with a CharArrayWriter
        pw2 = new PrintWriter(w1); // a private PrintWriter
    }

    /**
     * * Override flusg and close methods,
     */

    public void flush() {
        caw.flush();
        pribuf();
        pw2.flush();
    }

    public void close() {
        flush();
        caw.close();
        pw2.close();
    }

    /**
     * * Override all println methods, add CRLF as end of line
     */

    public synchronized void println() {
        // super.println();
        write(TCC.CR);
        write(TCC.LF);
        flush();
    }

    public synchronized void println(boolean b) {
        super.print(b);
        this.println();
    }

    public synchronized void println(char c) {
        synchronized (this) {
            super.print(c);
            this.println();
        }
    }

    public synchronized void println(int i) {
        super.print(i);
        this.println();
    }

    public synchronized void println(long l) {
        super.print(l);
        this.println();
    }

    public synchronized void println(float f) {
        super.print(f);
        this.println();
    }

    public synchronized void println(double d) {
        super.print(d);
        this.println();
    }

    public synchronized void println(char[] c) {
        super.print(c);
        this.println();
    }

    public synchronized void println(String s) {
        synchronized (this) {
            super.print(s);
            this.println();
        }
    }

    public synchronized void println(Object o) {
        super.print(o);
        this.println();
    }

    /**
     * * Override all write methods, write to internal buffer first
     */

    public synchronized void write(char[] buf) {
        caw.write(buf, 0, buf.length);
    }

    public synchronized void write(char[] buf, int off, int len) {
        caw.write(buf, off, len);
    }

    public synchronized void write(String s) {
        synchronized (this) {
            try {
                caw.write(s);
            } catch (IOException iox) {
                System.out
                        .println("Exception in TelnetPrintWriter write (String) "
                                + iox.toString());
            }
        }
    }

    public synchronized void write(int c) {
        caw.write(c);
    }

    public synchronized void write(String s, int off, int len) {
        caw.write(s, off, len);
    }

    /**
     * * Scan the output buffer for CR<n> where n != LF, * which is translated
     * it to CR NULL n
     */

    private synchronized void pribuf() {
        char[] buf = caw.toCharArray();
        caw.reset();
        char mask = ts.getMask();

        for (int i = 0; i < buf.length; i++) {
            if (prevWasCR == true) {
                if (buf[i] != TCC.LF_char) { // add a NULL
                    pw2.write(TCC.NULL_char);
                    pw2.write(buf[i] & mask);
                } else {
                    pw2.write(buf[i] & mask);
                }
            } else {
                if (buf[i] == TCC.LF_char) { // a single LF -> CRLF
                    pw2.write(TCC.CR_char);
                    pw2.write(TCC.LF_char);
                } else {
                    pw2.write(buf[i] & mask);
                }
            }
            prevWasCR = (buf[i] == TCC.CR_char) ? true : false;
        }
        // pw2.flush();
    }

    public synchronized void reset() {
        caw.reset();
    }

    /**
     * * Write a telnet command in the output stream. * *
     * 
     * @parameter tc, command string to write
     */

    public synchronized void writeCommand(String tc) {
        if (tc != null) {
            pw2.print(tc);
            pw2.flush();
        }
    }
}
