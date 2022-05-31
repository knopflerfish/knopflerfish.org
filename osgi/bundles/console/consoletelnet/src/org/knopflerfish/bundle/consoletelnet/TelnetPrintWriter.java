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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * This extension of a PrintWriter makes the following
 * translations on data in the output stream
 *
 * CR LF -> no change
 * CR <n>, when n != LF -> CR NULL <n>
 * <n> LF -> <n> CR LF
 *
 * All characters in the output buffer are
 * anded with a mask from the session.
 * This mask is either 0x7F (7 bit) or 0xFF (8 bit)
 *
 * It also has a clear method to empty the output buffer
 * and a writeCommand method to send telnet commands.
 */
public class TelnetPrintWriter extends PrintWriter {
    private TelnetSession telnetSession;
    private CharArrayWriter charArrayWriter; // Intermediate array writer
    private PrintWriter printWriter; // Private PrintWriter
    private boolean prevWasCR = false;

    public TelnetPrintWriter(OutputStream os, TelnetStateMachine tsm,
            TelnetSession telnetSession) {
        super(os);
        this.telnetSession = telnetSession;
        Writer w1 = super.out; // get the underlying writer
        charArrayWriter = new CharArrayWriter(); // intermediate array writer
        super.out = charArrayWriter; // replace underlying Writer with a CharArrayWriter
        printWriter = new PrintWriter(w1); // a private PrintWriter
    }

    /**
     * * Override flusg and close methods,
     */
    @Override
    public void flush() {
        charArrayWriter.flush();
        pribuf();
        printWriter.flush();
    }

    @Override
    public void close() {
        flush();
        charArrayWriter.close();
        printWriter.close();
    }

    /**
     * * Override all println methods, add CRLF as end of line
     */
    @Override
    public synchronized void println() {
        // super.println();
        write(TCC.CR);
        write(TCC.LF);
        flush();
    }

    @Override
    public synchronized void println(boolean b) {
        super.print(b);
        this.println();
    }

    @Override
    public synchronized void println(char c) {
        synchronized (this) {
            super.print(c);
            this.println();
        }
    }

    @Override
    public synchronized void println(int i) {
        super.print(i);
        this.println();
    }

    @Override
    public synchronized void println(long l) {
        super.print(l);
        this.println();
    }

    @Override
    public synchronized void println(float f) {
        super.print(f);
        this.println();
    }

    @Override
    public synchronized void println(double d) {
        super.print(d);
        this.println();
    }

    @Override
    public synchronized void println(char[] c) {
        super.print(c);
        this.println();
    }

    @Override
    public synchronized void println(String s) {
        synchronized (this) {
            super.print(s);
            this.println();
        }
    }

    @Override
    public synchronized void println(Object o) {
        super.print(o);
        this.println();
    }

    /**
     * * Override all write methods, write to internal buffer first
     */
    @Override
    public synchronized void write(char[] buf) {
        charArrayWriter.write(buf, 0, buf.length);
    }

    @Override
    public synchronized void write(char[] buf, int off, int len) {
        charArrayWriter.write(buf, off, len);
    }

    @Override
    public synchronized void write(String s) {
        synchronized (this) {
            try {
                charArrayWriter.write(s);
            } catch (IOException iox) {
                System.out.println("Exception in TelnetPrintWriter write (String) "
                                + iox.toString());
            }
        }
    }

    @Override
    public synchronized void write(int c) {
        charArrayWriter.write(c);
    }

    @Override
    public synchronized void write(String s, int off, int len) {
        charArrayWriter.write(s, off, len);
    }

    /**
     * * Scan the output buffer for CR<n> where n != LF, * which is translated
     * it to CR NULL n
     */

    private synchronized void pribuf() {
        char[] buf = charArrayWriter.toCharArray();
        charArrayWriter.reset();
        char mask = telnetSession.getMask();

        for (char c : buf) {
            if (prevWasCR) {
                if (c != TCC.LF_char) { // add a NULL
                    printWriter.write(TCC.NULL_char);
                }
                printWriter.write(c & mask);
            } else {
                if (c == TCC.LF_char) { // a single LF -> CRLF
                    printWriter.write(TCC.CR_char);
                    printWriter.write(TCC.LF_char);
                } else {
                    printWriter.write(c & mask);
                }
            }
            prevWasCR = c == TCC.CR_char;
        }
    }

    public synchronized void reset() {
        charArrayWriter.reset();
    }

    /**
     * * Write a telnet command in the output stream. * *
     * 
     * @param tc, command string to write
     */
    public synchronized void writeCommand(String tc) {
        if (tc != null) {
            printWriter.print(tc);
            printWriter.flush();
        }
    }
}
