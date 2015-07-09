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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BodyOutputStream extends BufferedOutputStream 
{

  // private fields

  private int totalCount = 0;

  private int maxCount = Integer.MAX_VALUE;

  private boolean committed = false;
  
   private ResponseImpl response = null;

  // constructors

  BodyOutputStream(OutputStream out, ResponseImpl response, int size)
  {
    super(out, size);

    this.response = response;
     
  }

  // package methods

  void setContentLength(int contentLength)
  {
    if (!committed) {
      maxCount = contentLength;
    }
  }

  int getBufferByteCount()
  {
    return count;
  }

  public synchronized void setBufferSize(int size)
  {
    if (totalCount != 0) {
      throw new IllegalStateException(
                                      "Buffer size cannot be changed when data has been written");
    }

    if (size < 0) {
      throw new IllegalArgumentException("Buffer size must not be negative: "
                                         + size);
    }

    if (size > buf.length) {
      buf = new byte[size];
    }
  }

  public int getBufferSize()
  {
    return buf.length;
  }

  public synchronized void reset()
  {
    // log(Thread.currentThread().getName() + " BodyOutPutStream.reset() - called");
    totalCount = 0;
    count = 0;
    committed = false;
    maxCount = Integer.MAX_VALUE;
  }
  
  public synchronized void init(OutputStream os, ResponseImpl response)
  {
    // log("init() - called");
    this.out = os;
    this.response = response;

  }


  public synchronized boolean isCommitted()
  {
    return committed;
  }

  public boolean inProgress() {
    return isCommitted() || totalCount > 0;
  }
  
  synchronized void flush(boolean commit)
      throws IOException
  {
    if (commit) {
      flushBuffer();
    }

    out.flush();
  }

  // protected methods

  public void flushBuffer()
      throws IOException
  {
    flushBuffer(false);
  }

  public void flushBuffer(boolean lastChunk)
      throws IOException
  {
    // log("flushBuffer() - committed=" + committed + " lastChunk=" + lastChunk + " count=" + count);
    if (!committed && response != null) {
      byte b[] = response.getHeaders();
      out.write(b);
      committed = true;
    }
    writeOutputBuffer(buf, 0, count, lastChunk);
    
    count = 0;
    super.flush();
  }
  // extends BufferedOutputStream

  @Override
  public synchronized void write(int b)
      throws IOException
  {
    if (count >= buf.length) {
      flushBuffer();
    }

    if (totalCount++ < maxCount) {
      buf[count++] = (byte) b;
    } else {
      flushBuffer();
    }
  }

  @Override
  public synchronized void write(byte b[], int off, int len)
      throws IOException
  {
    // log("write(): "  + " off=" + off + " len: " + len);
   
    if (len >= buf.length) {
      flushBuffer();
      // out.write(b, off, len);
      writeOutputBuffer(b, off, len, false);
    } else {
      if (len > buf.length - count) {
        flushBuffer();
      }
      System.arraycopy(b, off, buf, count, len);
      totalCount += len;
      count += len;
    }
    // log("write() before return: "  + " count=" + count);
  }
  
  @Override
  public void flush() throws IOException  {
    // log("flush()");
    out.flush();
  }

  @Override
  public void close() throws IOException {
    // log("close()");
    flush();
    // this.out.close();
    // (new IOException()).printStackTrace();
    // super.close();
  }

  private void log(String s) {
    Activator.log.info(Thread.currentThread().getName() + " - " + s);
  }

  private void writeOutputBuffer(byte[] b, int off, int len, boolean lastChunk) throws IOException {
    // log("writeOutputBuffer(): committed=" + committed + " lastChunk=" + lastChunk + " bytes: " + len);
    if (response.isChunked()) {
      if (count > 0) {
        out.write((Integer.toString(count,16) + "\r\n").getBytes());
        out.write(b, 0, len);
        out.write("\r\n".getBytes());
      }
      if (lastChunk)
        out.write("0\r\n\r\n".getBytes());
        out.flush();
    }
    else {
      out.write(b, 0, len);
    }
  }

} // BodyOutputStream

