/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.util.cm;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;

/** 
 * Input stream for a zip file entry that closes the zip file when
 * closed. 
 */
public class ZipFileInputStream extends InputStream {

  InputStream in;
  ZipFile zf;

  /** 
   * This open method returns an input stream that works around the
   * problem that closing the input stream returned by
   * URL.openStream() on a <code>jar:file:xxx!/sss</code> URL does
   * not close the underlaying jar file.
   **/
  public static InputStream getInputStream(URL url) throws IOException {
    if ("jar".equals(url.getProtocol())) {
      String spec = url.getFile();
      int separator = spec.indexOf('!');
      if (separator==-1) {
        throw new MalformedURLException
          ("no ! found in 'jar:' url spec: " + spec);
      }
      final String archiveURL = spec.substring(0,separator++);
      if (archiveURL.startsWith("file:")    // Local archive
          && (++separator != spec.length()) // Entry specified
          ) {
        final String   entryName = spec.substring( separator, spec.length() );
        final ZipFile  zf        = new ZipFile( archiveURL.substring(5) );
        final ZipEntry ze        = zf.getEntry( entryName );
        if (ze==null) throw new IOException
          ("No entry named '" +entryName +"' in " +archiveURL );
        return new ZipFileInputStream( zf, zf.getInputStream( ze ) );
      }
    }
    return url.openStream();
  }

  public ZipFileInputStream( ZipFile zf, InputStream in ) {
    this.zf = zf;
    this.in = in;
  }

  public int read() throws IOException {
    return in.read();
  }

  public int read(byte[] b) throws IOException {
    return in.read(b);
  }

  public int read(byte[] b,
                  int off,
                  int len)
    throws IOException
  {
    return in.read(b,off,len);
  }

  public long skip(long n) throws IOException {
    return in.skip(n);
  }

  public int available() throws IOException {
    return in.available();
  }

  public void close() throws IOException {
    in.close();
    zf.close();
  }

  public void mark(int readlimit) {
    in.mark(readlimit);
  }

  public void reset() throws IOException {
    in.reset();
  }

  public boolean markSupported() {
    return in.markSupported();
  }
}// ZipFileInputStream
