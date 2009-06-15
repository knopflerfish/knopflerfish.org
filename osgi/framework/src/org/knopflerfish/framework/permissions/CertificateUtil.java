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

package org.knopflerfish.framework.permissions;

import java.security.cert.*;
import java.util.ArrayList;

import javax.security.auth.x500.X500Principal;



public class CertificateUtil {

  /**
   * Check if DN pattern matches any of the supplied certificate chains.
   *
   * @return Index of matching signer, otherwise -1.
   */
  static public int matchCertificates(Certificate [] certs, String pattern)
    throws IllegalArgumentException
  {
    if (certs != null) {
      if (Debug.permissions) {
        Debug.println("MatchCertificates: " + certs.length + " number of certs with " + pattern);
      }
      ArrayList pat = parseDNs(pattern);
      StringBuffer chain = new StringBuffer();
      // TBD, Refactor the chain matching code so that is only executed once.
      X500Principal prev_issuer;
      X500Principal issuer = null;
      for (int i = 0; i < certs.length; i++) {
        if (certs[i] instanceof X509Certificate) {
          X509Certificate c = (X509Certificate)certs[i];
          X500Principal subject = c.getSubjectX500Principal();
          prev_issuer = issuer;
          issuer = c.getIssuerX500Principal();
          if (Debug.permissions) {
            Debug.println("SUBJECT " + i  + ": " + subject.getName(X500Principal.CANONICAL));
            Debug.println("ISSUER " + i  + ": " + issuer.getName(X500Principal.CANONICAL));
          }
          if (prev_issuer != null && !prev_issuer.equals(subject)) {
            throw new IllegalArgumentException("Certificate chain not correctly chained");
          }
          chain.append(subject.getName(X500Principal.CANONICAL));
          if (subject.equals(issuer)) {
            // Found anchor, TBD can we use === instead?
            ArrayList dn = parseDNs(chain.toString());
            if (matchDNs(dn, dn.size() - 1, pat, pat.size() - 1)) {
              if (Debug.permissions) {
                Debug.println("MatchCertificates matched on: " + i);
              }
              return i;
            }
            if (Debug.permissions) {
              Debug.println("MatchCertificates failed on: " + i);
            }
            chain.setLength(0);
            issuer = null;
          } else {
            chain.append(" ; ");
          }
        } else {
          throw new IllegalArgumentException("Unknown Certificate type");
        }
      }
      if (chain.length() > 0) {
        throw new IllegalArgumentException("Incomplete certificate chain");
      }
    }
    return -1;
  }


  /**
   * Check if at least one of the signers is match by specified pattern.
   *
   * @return Index of matching signer, otherwise -1.
   */
  static public int matchSigners(String [] signers, String pattern) {
    if (Debug.permissions) {
      Debug.println("MatchSigners: " + signers.length + " number of certs with " + pattern);
    }
    ArrayList pat = parseDNs(pattern);
    for (int i = 0; i < signers.length; i++) {
      if (signers[i] != null) {
        ArrayList dn = parseDNs(signers[i]);
        if (Debug.permissions) {
          Debug.println("SUBJECT " + i  + ": " + signers[0]);
        }
        if (matchDNs(dn, dn.size() - 1, pat, pat.size() - 1)) {
          return i;
        }
      }
    }
    return -1;
  }


  /**
   *
   */
  static private ArrayList parseDN(String dn) {
    ArrayList res = new ArrayList();
    dn = dn.trim();
    int len = dn.length();
    if (len == 0) {
      return null;
    }
    if (dn.equals("-")) {
      res.add(dn);
      return res;
    }
    if (dn.charAt(0) == '*') {
      res.add("*");
      if (len == 1) {
        return res;
      }
      int pos = 1;
      while (dn.charAt(pos) == ' ') {
        pos++;
      }
      if (dn.charAt(pos) == ',') {
        dn = dn.substring(pos + 1);
      } else {
        throw new IllegalArgumentException("wildcard has trailing characters: " + dn);
      }
    }
    dn = new X500Principal(dn).getName(X500Principal.CANONICAL);
    len = dn.length();
    int end = 0;
    while (end < len) {
      boolean quit = false;
      boolean multi_rdn = false;
      int start = end;
      while (!quit && end < len) {
        switch (dn.charAt(end)) {
        case '+':
          multi_rdn = true;
          // Fall through
        case ',':
          quit = true;
          break;
        case '\\':
          end+=2;
          break;
        default:
          end++;
          break;
        }
      }
      res.add(new RDN(dn, start, end, multi_rdn));
      end++;
    }
    return res;
  }


  /**
   *
   */
  static private ArrayList parseDNs(String dns) {
    ArrayList res = new ArrayList();
    int start = 0;
    int end = 0;
    int len = dns.length();
    boolean quote = false;
    while (end < len) {
      switch (dns.charAt(end)) {
      case '"' :
        quote = !quote;
        break;
      case '\\' :
        if (++end == len) {
          throw new IllegalArgumentException("Escape character at end of string");
        }
        break;
      case ';' :
        if (!quote) {
          res.add(parseDN(dns.substring(start, end)));
          start = end + 1;
        }
        break;
      }
      end++;
    }
    if (quote) {
      throw new IllegalArgumentException("Unterminated quote at end of string");
    }
    if (start < end) {
      res.add(parseDN(dns.substring(start, end)));
    }
    return res;
  }


  /**
   * Number of entries a possible wildcard represents (0,1 or many).
   */
  static private int wildcardDN(ArrayList dn) {
    if (dn.size() == 1) {
      Object o = dn.get(0);
      if (o instanceof String) {
        if ("-".equals(o)) {
          return Integer.MAX_VALUE;
        } else if ("*".equals(o)) {
          return 1;
        }
      }
    }
    return 0;
  }


  /**
   * Match a DN chain.
   *
   */
  static private boolean matchDN(ArrayList dn, ArrayList pattern) {
    int d_end = dn.size() - 1;
    int p_end = pattern.size() - 1;
    int p_base;

    if (pattern.get(0) instanceof String) {
      // All strings are assumed to contain a "*"
      if (d_end < p_end - 1) {
        return false;
      }
      p_base = 1;
    } else if (d_end == p_end) {
      p_base = 0;
    } else {
      // If not wildcard, then length must be the same
      return false;
    }
    while (p_end >= p_base) {
      Object p = pattern.get(p_end--);
      Object d = dn.get(d_end--);
      if (!((RDN)d).match((RDN)p)) {
        return false;
      }
    }
    // Check that the wildcard doesn't match part of a multiRDN.
    if (d_end >= 0 && ((RDN)dn.get(d_end)).multi) {
      return false;
    }
    return true;
  }


  /**
   * Match several DN chains.
   *
   */
  static private boolean matchDNs(ArrayList dns, int dns_ix,
                                  ArrayList patterns, int patterns_ix) {
    if (dns_ix < 0) {
      return patterns_ix < 0;
    }
    if (patterns_ix < 0) {
      return false;
    }
    ArrayList pat = (ArrayList)patterns.get(patterns_ix--);
    int x = wildcardDN(pat);
    if (x > 0) {
      for (int i = 0; i <= x; i++) {
        if (matchDNs(dns, dns_ix - i, patterns, patterns_ix)) {
          return true;
        }
      }
      return false;
    }
    if (!matchDN((ArrayList)dns.get(dns_ix), pat)) {
      return false;
    }
    return matchDNs(dns, dns_ix - 1, patterns, patterns_ix);
  }
}


/**
 * Class for holding RDN information. If multi is set, this means
 * that we are part of a multi-RDN. The multi-RDN is ended with
 * a RDN with an unset multi (this is RDN is included).
 */
class RDN {
  final String name;
  final String value;
  final boolean multi;

  RDN(String dn, int start, int end, boolean multi_rdn) {
    String s = dn.substring(start, end);
    int eq = dn.indexOf('=', start);
   
    if (eq < 0 || eq > end) {
      throw new IllegalArgumentException("No assignment in RDN: " + dn.substring(start, end));
    }
    name = dn.substring(start, eq);
    String v = dn.substring(eq + 1, end);
    value = v.equals("*") ? null : v;
    multi = multi_rdn;
  }

  boolean match(RDN patRDN) {
    return name.equals(patRDN.name)
      && (patRDN.value == null || patRDN.value.equals(value))
      && multi == patRDN.multi;
  }
}
