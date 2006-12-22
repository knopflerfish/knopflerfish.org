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

package org.knopflerfish.bundle.desktop.swing.console;

import java.io.IOException;
import java.io.Reader;


public class TextReader extends Reader {

  StringBuffer sb = new StringBuffer();

  Object  lock     = new Object();
  Object  waitLock = new Object();

  public TextReader() {
  }

  public int available() {
    if(sb == null) {
      throw new RuntimeException("Stream closed");
    }
    System.out.println("read available=" + sb.length());
    return sb.length();
  }

  public void close() {
    flush();
    sb = null;
  }
  
  public void mark(int readlimit) {
  }

  
  public boolean markSupported() {
    return false;
  }

  public void reset() {
  }

  public long skip(long n) {
    if(sb == null) {
      throw new RuntimeException("Stream closed");
    }
    sb.delete(0, (int)Math.max(n, sb.length()));
    return sb.length();
  }
 
  public void print(String s) {
    if(sb == null) {
      throw new RuntimeException("Stream closed");
    }
    sb.append(s);
    if(s.length() > 0) {
      flush();
    }
  }

  void flush() {
    synchronized(waitLock) {
      waitLock.notifyAll();
    }
  }

  char[] buf = new char[1];

  public int read() throws IOException {
    if(-1 != read(buf, 0, 1)) {
      return buf[0];
    }
    
    return -1;
  }

  public int read(char[] b) throws IOException {
    return read(b, 0, b.length);
  }

  public int read(char[] cbuf, int off, int len) throws IOException {
    synchronized(waitLock) {
      if(sb == null) {
	throw new RuntimeException("Stream closed");
      }
      if(len == 0) {
	return 0;
      }
      try {
	while(sb == null || len > sb.length()) {
	  waitLock.wait();
	}
      } catch(InterruptedException e) {
	throw new IOException(e.getMessage());
      }
      
      int i   = 0;
      while(i < len) {
	cbuf[off + i] = sb.charAt(i);
	i++;
      }
      sb.delete(0, i);
      return len;
    }
  }
}
