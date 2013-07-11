/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.io.IOException;
import java.net.*;
import java.util.*;


/**
 * Bundle URL handling.
 *
 * @author Jan Stein, Gunnar Ekolin
 */
public class BundleURLStreamHandler extends URLStreamHandler {

  final public static String PROTOCOL = "bundle";

  final public static String PERM_OK = "P";

  // Currently we only support a single framework instance in the same
  // class-loader context!
  private ArrayList /* FrameworkContext */ framework = new ArrayList(2);


  // TODO, we need a more efficient and cleaner solution here.


  public URLConnection openConnection(URL u) throws IOException {
    String h = u.getHost();
    FrameworkContext fw = getFramework(h);
    if (fw == null) {
      throw new IOException("Framework associated with URL is not active");
    }
    if (u.getAuthority() != PERM_OK) {
      fw.perm.checkResourceAdminPerm(fw.bundles.getBundle(getId(h)));
      // NYI, set authority
    }
    return new BundleURLConnection(u, fw);
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
          } else if (sc[pos] == '!' || sc[pos] == '.') {
            if (id == -1) {
              id = Long.parseLong(new String(sc, 2, pos - 2));
            }
          } else if (!Character.isDigit(sc[pos])) {
            throw new IllegalArgumentException
              ("Illegal chars in bundle id specification");
          }
        }
        host = new String(sc, 2, pos - 2);
        if (pos < len && sc[pos] == ':') {
          ++pos;
          cpElem = 0;
          while (pos < len) {
            if (sc[pos] == '/') {
              break;
            } else if (!Character.isDigit(sc[pos])) {
              throw new IllegalArgumentException
                ("Illegal chars in bundle port specification");
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
              if (ipos>pstart) { // There is a path-level to remove.
                dots = 0;
                while (ipos > pstart && sc[--ipos] != '/')
                  ;
                continue;
              }
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
          if (ipos > pstart) { // There is a level to remove
            while (ipos > pstart && sc[--ipos] != '/')
              ;
          } else { // On top level, keep the ".."
            while (dots-- > 0) {
              sc[++ipos] = '.';
            }
            // Add trailing '/' to ensure that a relative URL created
            // with path ".." results in the same URL as one created
            // using "../".
            sc[++ipos] = '/';
          }
        }
        path = new String(sc, pstart, ipos - pstart + 1);
      }
    }
    if (id == -1) {
      id = getId(host);
    }
    FrameworkContext fw = getFramework(host);
    if (fw == null) {
      throw new IllegalArgumentException("Framework associated with URL is not active");
    }
    fw.perm.checkResourceAdminPerm(fw.bundles.getBundle(id));
    setURL(u, PROTOCOL, host, cpElem, PERM_OK, null, path, null, null);
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
   * @return    <tt>true</tt> if and only if they
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
  // Package
  //

  /**
   * Add framework that uses this URLStreamHandlerFactory.
   *
   * @param fw Framework context for framework to add.
   */
  void addFramework(FrameworkContext fw) {
    framework.add(fw);
  }


  /**
   * Remove framework that uses this URLStreamHandlerFactory.
   *
   * @param fw Framework context for framework to remove.
   */
  void removeFramework(FrameworkContext fw) {
    framework.remove(fw);
  }

  //
  // Private
  //

  private FrameworkContext getFramework(String host) {
    Iterator i = framework.iterator();
    int e = host.indexOf("!");
    int fwId;
    if (e == -1) {
      fwId = 0;
    } else {
      try {
        fwId = Integer.parseInt(host.substring(e + 1));
      } catch (NumberFormatException _) {
        return null;
      }
    }
    while (i.hasNext()) {
      FrameworkContext fw = (FrameworkContext)i.next();
      if (fw.id == fwId) {
        return fw;
      }
    }
    return null;
  }


  public static long getId(String host) {
    int e = host.indexOf(".");
    if (e == -1) {
      e = host.indexOf("!");
    }
    if (e >= 0) {
      host = host.substring(0, e);
    }
    return Long.parseLong(host);
  }

}
