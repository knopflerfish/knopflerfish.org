/*
 * Copyright (c) 2015, KNOPFLERFISH project
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
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

// An output body for writing compressed contents. Extends the BodyOutputStream
// If compression is not enabled simply writes directly uncompressed content,
// bypassing the deflater

public class BodyOutputStreamZip  extends BodyOutputStream
{
  // private fields
  private boolean useGzip = false;
  
  /** The standard 10 byte GZIP header */
  private static final byte[] GZIP_HEADER = new byte[] { 0x1f, (byte) 0x8b,
          Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, 0 };

  /** CRC-32 of uncompressed data. */
  private final CRC32 crc = new CRC32();

  /** Deflater to deflate data */
  private final Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
  
  byte[] zipbuf = new byte[1024];
  
  // constructors

  BodyOutputStreamZip(OutputStream out, ResponseImpl response, int size) throws IOException
  {
    super(out, response, size);
  }

  // package methods

//  
  public synchronized void reset(boolean useGzip)
  {
    deflater.reset();
    super.reset();
    this.useGzip = useGzip;
    if (useGzip)
      useGzip();
  }
  
  @Override
  public synchronized void init(OutputStream os, ResponseImpl response)
  {
    super.init(os, response);
    useGzip = false;
  }

  
  @Override
  public synchronized void write(int b)
      throws IOException
  {
    if (!useGzip) {
      byte[] buf = new byte[1];
      buf[0] = (byte)(b & 0xff);
      write(buf, 0, 1);
    }
    else {
      super.write(b);
    }
  }

  @Override
  public synchronized void write(byte b[], int off, int len)
      throws IOException
  {
    if (!useGzip) {
      super.write(b, off, len);
      return;
    }
    
    // log("write(): "  + " off=" + off + " len: " + len);
    // new Throwable("").printStackTrace(System.out);
    
    crc.update(b, off, len);
    deflater.setInput(b, off, len);

    while( !deflater.needsInput() ) {
        int r = deflater.deflate(zipbuf, 0, zipbuf.length);
        if (r == 0)
          continue;
        super.write(zipbuf, 0, r);
    }
  }
  
  private void finish() throws IOException {
    deflater.finish();
    
    int r;
    do {
        r = deflater.deflate(zipbuf, 0, zipbuf.length);
        super.write(zipbuf, 0, r);
    } while( r == zipbuf.length );

    // write GZIP trailer
    writeInt((int) crc.getValue());
    writeInt((int) deflater.getBytesRead());
  }
  
  private void writeInt(int v) throws IOException {
    super.write(v & 0xff);
    super.write((v >> 8) & 0xff);
    super.write((v >> 16) & 0xff);
    super.write((v >> 24) & 0xff);
}
  
  @Override
  public void flushBuffer(boolean lastChunk)
      throws IOException
  {
    // log ("Flushing zip buffer, lastChunk=" + lastChunk);
    if (useGzip) {
      if (lastChunk)
        finish();
    }
    super.flushBuffer(lastChunk);
  }


  @Override
  public void close() throws IOException {
    super.close();
  }
  
  public void useGzip() {
    if (useGzip)
      return;
    
    if (inProgress())
      throw new IllegalStateException("Can not change to compress mode when response is in progress");

    writeGzipHeader();
    this.useGzip = true;
  }

  private void writeGzipHeader() {
    try {
      super.write(GZIP_HEADER, 0, GZIP_HEADER.length);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new IllegalStateException("Failed to set compress mode", e);
    }
  }
  
  private void log(String s) {
    Activator.log.info(Thread.currentThread().getName() + " [BodyOutputStreamZip] - " + s);
  }

  } // BodyOutputStream

