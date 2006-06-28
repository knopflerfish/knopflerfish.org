/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

import java.io.*;
import java.net.*;


/**
 * Bundle URL handling.
 *
 * @author Jan Stein
 */
public class BundleURLStreamHandler extends URLStreamHandler {

  final public static String PROTOCOL = "bundle";

  final public static String PERM_OK = "P";

  private Bundles bundles;

  private PermissionOps secure;


  // TODO, we need a more efficient and cleaner solution here.

  BundleURLStreamHandler(Bundles b, PermissionOps s) {
    bundles = b;
    secure = s;
  }


  public URLConnection openConnection(URL u) {
    if (u.getAuthority() != PERM_OK) {
      secure.checkResourceAdminPerm(bundles.getBundle(getId(u.getHost())));
    }
    return new BundleURLConnection(u, bundles);
  }


  protected void parseURL(URL u, String s, int start, int limit)  {
    String path = u.getPath();
    String host = u.getHost();
    long id = -1;
    int cpElem = u.getPort();
    if (limit > start) {
      int len = limit - start;
      char [] sc = new char[len];
      s.getChars(start, limit, sc, 0);
      int pos = 0;
      if (len >= 2 && sc[0] == '/' && sc[1] == '/') {
        for (pos = 2; pos < len; pos++) {
          if (sc[pos] == ':' || sc[pos] == '/') {
            break;
          } else if (sc[pos] == '.' || sc[pos] == '_') {
            if (id == -1) {
              id = Long.parseLong(new String(sc, 2, pos - 2));
            }
          } else if (!Character.isDigit(sc[pos])) {
            throw new IllegalArgumentException("Illegal chars in bundle id specification");
          }
        }
        host = new String(sc, 2, pos - 2);
        if (pos < len && sc[pos] == ':') {
          int portpos = ++pos;
          cpElem = 0;
          while (pos < len) {
            if (sc[pos] == '/') {
              break;
            } else if (!Character.isDigit(sc[pos])) {
              throw new IllegalArgumentException("Illegal chars in bundle port specification");
            }
            cpElem = 10 * cpElem + (sc[pos++] - '0');
          }
        } else {
          cpElem = -1;
        }
      }
      if (pos < len) {
        int pstart;
        if (sc[pos] != '/') {
          if (path != null) { 
            int dirend = path.lastIndexOf('/') + 1;
            if (dirend > 0) {
              int plen = len - pos;
              pstart = path.startsWith("/") ? 0 : 1;
              len = dirend + plen + pstart;
              if (len > sc.length) {
                char [] newsc = new char [len];
                System.arraycopy(sc, pos, newsc, dirend + pstart, plen);
                sc = newsc;
              } else if (pos != dirend) {
                System.arraycopy(sc, pos, sc, dirend + pstart, plen);
              }
              path.getChars(1 - pstart, dirend, sc, 1);
            } else {
              len = 1;
            }
          } else {
            len = 1;
          }
          sc[0] = '/';
          pstart = 0;
          pos = 0;
        } else {
          pstart = pos;
        }
        int dots = 0;
        int ipos = pstart - 1;
        boolean slash = false;
        for (; pos < len; pos++) {
          if (sc[pos] == '/') {
            if (slash) {
              continue;
            }
            slash = true;
            if (dots == 1) {
              dots = 0;
              continue;
            } else if (dots == 2) {
              while (ipos > pstart && sc[--ipos] != '/')
                ;
            }
          } else if (sc[pos] == '.') {
            if (slash) {
              dots = 1;
              slash = false;
              continue;
            } else if (dots == 1) {
              dots = 2;
              continue;
            }
          } else {
            slash = false;
          }
          while (dots-- > 0) {
            sc[++ipos] = '.';
          }
          if (++ipos != pos) {
            sc[ipos] = sc[pos];
          }
        }
        if (dots == 2) {
          while (ipos > pstart && sc[--ipos] != '/')
            ;
        }
        path = new String(sc, pstart, ipos - pstart + 1);
      }
    }
    if (id == -1) {
      id = getId(host);
    }
    secure.checkResourceAdminPerm(bundles.getBundle(id));
    setURL(u, u.getProtocol(), host, cpElem, PERM_OK, null, path, null, null);
  }


  /**
   * Equals calculation for bundle URLs.
   * @return <tt>true</tt> if the two urls are 
   * considered equal, ie. they refer to the same 
   * fragment in the same file.
   *
   */
  protected boolean equals(URL u1, URL u2) {
    return sameFile(u1, u2);
  }


  /**
   * Provides the hash calculation
   * @return an <tt>int</tt> suitable for hash table indexing
   */
  protected int hashCode(URL u) {
    int h = 0;

    if (PROTOCOL.equals(u.getProtocol())) {
      String host = u.getHost();
      if (host != null)
	h = host.hashCode();

      String file = u.getFile();
      if (file != null)
	h += file.hashCode();

      h += u.getPort();
    } else {
      h = u.hashCode();
    }
    return h;
  }

  /**
   * Compare two urls to see whether they refer to the same file,
   * i.e., having the same protocol, host, port, and path.
   * @return true if u1 and u2 refer to the same file
   */
  protected boolean sameFile(URL u1, URL u2) {
    String p1 = u1.getProtocol();
    if (PROTOCOL.equals(p1)) {
      if (!p1.equals(u2.getProtocol()))
	return false;

      if (!hostsEqual(u1, u2))
	return false;

      if (!(u1.getFile() == u2.getFile() ||
	    (u1.getFile() != null && u1.getFile().equals(u2.getFile()))))
	return false;

      if (u1.getPort() != u2.getPort())
	return false;

      return true;
    } else {
      return u1.equals(u2);
    }
  }


  /**
   * Compares the host components of two URLs.
   * @param u1 the URL of the first host to compare 
   * @param u2 the URL of the second host to compare 
   * @return	<tt>true</tt> if and only if they 
   * are equal, <tt>false</tt> otherwise.
   */
  protected boolean hostsEqual(URL u1, URL u2) {
    String s1 = u1.getHost();
    String s2 = u2.getHost();
    return (s1 == s2) || (s1 != null && s1.equals(s2));
  }

  
  /**
   * Converts a bundle URL to a String.
   *
   * @param   url   the URL.
   * @return  a string representation of the URL.
   */
  protected String toExternalForm(URL url) {
    StringBuffer res = new StringBuffer(url.getProtocol());
    res.append("://");
    res.append(url.getHost());
    int port = url.getPort();
    if (port >= 0) {
      res.append(":").append(port);
    }
    res.append(url.getPath());
    return res.toString();
  }


  protected synchronized InetAddress getHostAddress(URL url) {
    return null;
  }

  //
  // Private
  //

  private long getId(String host) {
    int i = host.indexOf(".");
    if (i == -1) {
      i = host.indexOf("_");
    }
    if (i >= 0) {
      host = host.substring(0, i);
    }
    return Long.parseLong(host);
  }

}
