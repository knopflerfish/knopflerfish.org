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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

/**
 * * Reads an input stream and extracts telnet TVM character * * This class
 * provides a very limited line editing capability
 */

public class TelnetReader extends Reader {
    // private InputStreamReader ir;
    private TelnetSession tels;

    private Vector lineBuf; // buffer while a line is being edited

    private char[] readyLine; // buffer for a that has been terminated by CR
                                // or LF

    private int rp; // pointer in read buffer line

    private InputStream is;

    private boolean busyWait;

    private boolean skipLF = false; // Skip next char if LF


    public TelnetReader(InputStream is, TelnetSession tels, boolean busyWait) {
        this.tels = tels;
        this.is = is;
        this.busyWait = busyWait;

        // ir = new InputStreamReader(is);
        lineBuf = new Vector();
        readyLine = new char[0];
        rp = 0;
    }

    public void close() throws IOException {
        is.close();
    }

    public boolean ready() throws IOException {
        // TODO, we should check skipLF
        return is.available() > 0;
    }

    /**
     * * Block here until end of line (CR) and then return * * This method
     * provides very limited line editing functionality * * CR -> return from
     * read * BS -> back one step in buffer *
     */

    public int read(char[] buf, int off, int len) throws IOException {
        int count = 0;
        int character;
        Integer tmp;

        //System.out.println("TelnetReader.read buf=" + buf + ", off=" + off +
        //", len=" + len);

        // 1. Check if there still are characters in readyLine ?

        count = chkRdyLine(buf, off, len);

        if (count > 0) {
            return count;
        }
        boolean w1 = true;
        while (w1) {
            character = readChar();

            if (character != -1) {

                //System.out.println("Char " + String.valueOf(character));

                switch (character) {

                case TCC.BS:
                    // System.out.println("BS");
                    if (lineBuf.size() > 0) {
                        lineBuf.removeElementAt(lineBuf.size() - 1);
                        // tels.echoChar(character);
                        tels.echoChar(' ');
                        tels.echoChar(character);
                    }
                    break;

                case TCC.CR:
                case TCC.LF:
                    // System.out.println("CR");
                    lineBuf.addElement(new Integer(character));
                    // tels.echoChar(character);

                    // transfer from lineBuf to readyLine

                    readyLine = new char[lineBuf.size()];
                    for (int i = 0; i < lineBuf.size(); i++) {
                        tmp = (Integer) lineBuf.elementAt(i);
                        character = tmp.intValue();
                        readyLine[i] = (char) character;
                    }
                    rp = 0;

                    lineBuf.removeAllElements();
                    count = chkRdyLine(buf, off, len);
                    return count;

                default:
                    // System.out.println("char=" + character);
                    lineBuf.addElement(new Integer(character));
                // tels.echoChar(character);
                }
            }
        }
        return count;
    }

    private int readChar() throws IOException {
        //System.out.println("TelnetReader.readChar()");
        int c;
        while (true) {
            if (busyWait) {
                while (is.available() < 1) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
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

        tels.echoChar(c);

        if (c == TCC.CR) {
          skipLF = true;
        }

        return c;
    }

    private int chkRdyLine(char[] buf, int off, int len) {
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

    public String readLine() throws IOException {
        Vector buf = new Vector();
        int character = -2;

        // System.out.println("TelnetReader.readLine()");

        try {
            while (true) {
                character = readChar();
                if (character == TCC.CR) {
                    break;
                }
                // System.out.println("TelnetReader.readLine() add char " +
                // character);
                buf.addElement(new Character((char) character));
            }

            // Convert to string

            char[] cbuf = new char[buf.size()];
            for (int i = 0; i < buf.size(); i++) {
                Character c1 = (Character) buf.elementAt(i);
                cbuf[i] = c1.charValue();
            }
            String s1 = new String(cbuf);
            return s1;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
