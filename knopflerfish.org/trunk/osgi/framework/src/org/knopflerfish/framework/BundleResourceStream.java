/*
 * Copyright (c) 2009, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.io.InputStream;
import java.io.IOException;


/**
 * InputStream for bundle content
 *
 * @author Jan Stein
 */
public class BundleResourceStream extends InputStream {

  protected InputStream wis;

  protected long contentLen;


  /**
   * BundleResourceStream
   *
   * @param is Underlying input stream.
   * @parma length Length of content, -1 if unknown.
   */
  public BundleResourceStream(InputStream is, long length) {
    wis = is;
    contentLen = length;
  }


  /**
   * Get length of resource stream.
   *
   * @return Length of resource stream. If length is unknown
   *         -1 is returned.
   */
  public long getContentLength() {
    return contentLen;
  }


  /**
   * Read a byte from the input stream.
   *
   * @return Byte read
   */
  public int read() throws IOException {
    return wis.read();
  }

  
  /**
   * Read bytes from the input stream.
   *
   * @param dest Byte array to read into
   * @return Number of bytes actually read
   */
  public int read(byte[] dest) throws IOException {
    return wis.read(dest);
  }


  /**
   * Read a specified number of bytes from the input stream.
   *
   * @param dest  Byte array to read into
   * @param off   Starting offset into the byte array
   * @param len   Maximum number of bytes to read
   * @return Number of bytes actually read
   */
  public int read(byte[] dest, int off, int len) throws IOException {
    return wis.read(dest, off, len);
  }


  /**
   * Skip over (and discard) a specified number of bytes in this input
   * stream.
   *
   * @param len Number of bytes to be skipped
   * @return Number of bytes skipped
   */
  public long skip(long len) throws IOException {
    return wis.skip(len);
  }


  /**
   * Return the number of bytes available for immediate read
   *
   * @return the number of bytes
   */
  public int available() throws IOException {
    return wis.available();
  }


  /**
   * Close input stream
   */
  public void close() throws IOException {
    wis.close();
  }


  /**
   * Mark current position in input stream
   *
   * @param readlimit Maximum of bytes when can save.
   */
  public void mark(int readlimit) {
    wis.mark(readlimit);
  }


  /**
   * Return to marked position.
   */
  public void reset() throws IOException {
    wis.reset();
  }


  /**
   * Check it mark/reset is supported.
   *
   * @return True if supported, otherwise false.
   */
  public boolean markSupported() {
    return wis.markSupported();
  }
 
}
