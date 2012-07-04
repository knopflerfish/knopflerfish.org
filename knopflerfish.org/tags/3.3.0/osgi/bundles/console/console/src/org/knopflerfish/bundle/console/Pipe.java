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

package org.knopflerfish.bundle.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Pipe between Command Input and Output.
 * 
 * @author Jan Stein
 * @version $Revision: 1.1.1.1 $
 */
class Pipe extends PrintWriter {

    PipeBuffer pipe;

    public Pipe() {
        this(new PipeBuffer());
    }

    Pipe(PipeBuffer p) {
        super(p);
        pipe = p;
    }

    public Reader getReader() {
        return pipe.getReader();
    }

}

class PipeBuffer extends Writer {

    private static final int SIZE = 512;

    private char[] buf = new char[SIZE];

    private int size = 0;

    private int rpos = 0;

    private PipeReader pr;

    boolean open = true;

    PipeBuffer() {
        super();
        pr = new PipeReader(this);
    }

    public void write(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {
            while (len > 0) {
                while (SIZE == size) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new IOException("Write interrupted");
                    }
                }
                int clen;
                if (size + len > SIZE) {
                    clen = SIZE - size;
                } else {
                    clen = len;
                }
                int wpos = (rpos + size) % SIZE;
                if (wpos + clen > SIZE) {
                    clen = SIZE - wpos;
                }
                System.arraycopy(cbuf, off, buf, wpos, clen);
                len -= clen;
                off += clen;
                size += clen;
                lock.notifyAll();
            }
        }
    }

    public void flush() {
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            while (open && size < len) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new IOException("Read interrupted");
                }
            }
            if (size < len) {
                len = size;
            }
            int clen;
            if (rpos + len > SIZE) {
                clen = SIZE - rpos;
                System.arraycopy(buf, rpos, cbuf, off, clen);
                rpos = 0;
                off += clen;
                clen = len - clen;
            } else {
                clen = len;
            }
            if (len > 0) {
                System.arraycopy(buf, rpos, cbuf, off, clen);
                rpos += clen;
                size -= len;
                lock.notifyAll();
                return len;
            }
            return -1;
        }
    }

    public void close() {
        synchronized (lock) {
            open = false;
            lock.notify();
        }
    }

    public Reader getReader() {
        return pr;
    }
}

class PipeReader extends Reader {

    private PipeBuffer pb;

    public PipeReader(PipeBuffer pb) {
        super();
        this.pb = pb;
    }

    /**
     * Read characters into a portion of an array.
     * 
     * @param buf
     *            Output buffer
     * @param off
     *            Offset at which to start storing characters
     * @param len
     *            Maximum number of characters to read
     * @return The number of characters read, or -1 if end of stream
     * @exception IOException
     *                If an I/O error occurs
     */
    public int read(char buf[], int off, int len) throws IOException {
        return pb.read(buf, off, len);
    }

    /**
     * Close the stream.
     */
    public void close() {
        pb.close();
    }

}
