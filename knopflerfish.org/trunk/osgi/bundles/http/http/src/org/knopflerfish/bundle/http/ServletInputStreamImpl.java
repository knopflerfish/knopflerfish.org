/*
 * Copyright (c) 2003,2011,2015 KNOPFLERFISH project
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;

public class ServletInputStreamImpl
  extends ServletInputStream
{

  private boolean isLimited = false;

  private volatile int available;

  private final BufferedServletInputStream is;

  private byte[] lineBuffer = null;

  private static int limitRequestLine = 8190;

  public ServletInputStreamImpl(final InputStream is)
  {
    this(is, -1);
  }

  public ServletInputStreamImpl(final InputStream is, final int available)
  {

    this.is = new BufferedServletInputStream(is);
    is.mark(Integer.MAX_VALUE);
    setLimit(available);
  }

  public void setLimit(int available)
  {
    this.available = available;
    this.isLimited = available != -1;
  }

  public static void setLimitRequestLine(int limit)
  {
    limitRequestLine = limit;
  }

  @Override
  public int read()
      throws IOException
  {

    if (isLimited && --available < 0) {
      return -1;
    }

    return is.read();
  }

  @Override
  public int read(byte[] b, int off, int len)  throws IOException
  {
    return is.read(b, off, len);
  }
  
  public String readLine()
      throws IOException, HttpException
  {

    if (lineBuffer == null) {
      lineBuffer = new byte[127];
    }

    int count = readLine(lineBuffer, 0, lineBuffer.length);
    int offset = count;

    while (count > 0 && offset == lineBuffer.length
           && lineBuffer[offset - 1] != '\n') {
      // double the size of the buffer
      final int newsize = offset * 2 + 1;
      if (newsize > limitRequestLine) {
        throw new HttpException(
                                HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                                "Request line length exceeds upper limit");
      }
      byte[] tmp = new byte[newsize];
      System.arraycopy(lineBuffer, 0, tmp, 0, offset);
      lineBuffer = tmp;
      tmp = null;

      count = readLine(lineBuffer, offset, lineBuffer.length - offset);
      offset += count;
    }

    if (count == -1) {
      throw new IOException("End of stream reached before CRLF");
    }

    return HttpUtil.newString(lineBuffer, 0, 0, offset - 2); // remove
                                                             // CRLF
  }
  
  public void init(InputStream is) throws IOException {
    this.is.init(is);
  }
  
  public void init() {
    this.is.init();
  }
  
  @Override
  public boolean markSupported() {
    return is.markSupported();
  }
  
  @Override
  public void mark(int readlimit) {
    // is.printInfo("mark() - readlimit=" + readlimit);
    is.mark(readlimit);
  }
  
  @Override
  public void reset() throws IOException {
    // is.printInfo("reset() - before");
    is.reset();
    // is.printInfo("reset() - after");
  }
  
  @Override
  public void close() throws IOException {
    is.close();
  }
  
  // Helper class that wraps a BufferedInputStream
  class BufferedServletInputStream extends BufferedInputStream {
    
    public BufferedServletInputStream(InputStream is) {
      super(is);
    }
    
    public void init(InputStream is) {
      in = is;
      init();
    }
    
    public void init() {
      // printInfo("enter init()");
      is.mark(Integer.MAX_VALUE);
      setLimit(-1);
      markpos = -1;
      count = 0;
      pos = 0;
      // printInfo("leave init()");
    }
    
    public void printInfo(String s) {
      Activator.log.info(Thread.currentThread().getName() + " - " + s + " count=" + count + " pos=" + pos + " markpos=" + markpos + " marklimit=" + marklimit);
    }
    
  }

} // ServletInputStreamImpl
