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
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

/**
 * Body output strema which compresses using GZIP.
 * All data written to the stream is buffered and written
 * at the first flushBuffer() call.
 */
public class CompressedBodyOutputStream extends BodyOutputStream {

  protected ByteArrayOutputStream buf;
  protected GZIPOutputStream gzip;

  public CompressedBodyOutputStream(OutputStream out, 
                                    ResponseImpl response, 
                                    int size) {
    super(out, response, size);
    try {
      this.buf       = new ByteArrayOutputStream(size);
      this.gzip      = new GZIPOutputStream(buf);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create compressed body output stream");
    }
  }


  void setContentLength(int contentLength) {
    // noop
  }

  int getBufferByteCount() {
    return buf.size();
  }

  public void setBufferSize(int size) {
    // noop
  }

  public int getBufferSize() {
    return buf.size();
  }

  public synchronized void reset() {
    // noop
  }

  public synchronized boolean isCommitted() {
    return committed;
  }

  synchronized void flush(boolean commit) throws IOException {
    if (commit) {
      flushBuffer();
    }
  }


  protected void flushBuffer() throws IOException {
    if (!committed && response != null) {
      gzip.finish();
      gzip.close();
      //      byte[] bytes = buf.toByteArray();
      
      response.setHeader(HeaderBase.CONTENT_ENCODING_HEADER_KEY, 
                         "gzip");
      response.setHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY, 
                         Integer.toString(buf.size()));

      out.write(response.getHeaders());

      committed = true;
      buf.writeTo(out);
    }
  }


  // extends BufferedOutputStream

  public synchronized void write(int b) throws IOException {
    gzip.write(b);
  }

  public synchronized void write(byte b[], int off, int len) throws IOException {
    gzip.write(b, off, len);
  }

  public void flush() throws IOException { 
  }

  public void close() throws IOException { 
  }
} 
