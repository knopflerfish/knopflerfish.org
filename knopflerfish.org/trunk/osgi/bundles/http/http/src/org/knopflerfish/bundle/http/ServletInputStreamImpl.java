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

package org.knopflerfish.bundle.http;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

public class ServletInputStreamImpl extends ServletInputStream {

    private boolean isLimited = false;

    private volatile int available;

    private final InputStream is;

    private byte[] lineBuffer = null;

    public ServletInputStreamImpl(final InputStream is) {
        this(is, -1);
    }

    public ServletInputStreamImpl(final InputStream is, final int available) {

        this.is = is;

        setLimit(available);
    }

    public void setLimit(int available) {

        this.available = available;
        this.isLimited = available != -1;
    }

    public int read() throws IOException {

        if (isLimited && --available < 0)
            return -1;

        return is.read();
    }

    public String readLine() throws IOException {

        if (lineBuffer == null)
            lineBuffer = new byte[127];

        int count = readLine(lineBuffer, 0, lineBuffer.length);
        int offset = count;

        while (count > 0 && offset == lineBuffer.length
                && lineBuffer[offset - 1] != '\n') {
            // double the size of the buffer
            byte[] tmp = new byte[offset * 2 + 1];
            System.arraycopy(lineBuffer, 0, tmp, 0, offset);
            lineBuffer = tmp;
            tmp = null;

            count = readLine(lineBuffer, offset, lineBuffer.length - offset);
            offset += count;
        }

        if (count == -1)
            throw new IOException("End of stream reached before CRLF");

        return HttpUtil.newString(lineBuffer, 0, 0, offset - 2); // remove
                                                                    // CRLF
    }

} // ServletInputStreamImpl
