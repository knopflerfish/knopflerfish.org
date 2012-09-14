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

package org.knopflerfish.framework.bundlestorage;


import java.security.cert.*;
import java.util.*;


/**
 * Interface for managing bundle contents.
 *
 * @author Jan Stein
 */
public class Util {

  /**
   * Take an array of X509 certificates and arrange them as a list
   * of chains. Certificates of unknown types and broken chains are
   * add returned in failed list.
   *
   */
  public static ArrayList getCertificateChains(Certificate [] c, List failed) {
    if (c == null) {
      return null;
    }
    ArrayList res = new ArrayList(3);
    ArrayList chain = new ArrayList(3);
    int i = 0;
    while (i < c.length) {
      if (c[i] instanceof X509Certificate) {
        X509Certificate cert = (X509Certificate) c[i++];
        // TBD, can we use == and do we need to check uniqID?
        chain.add(cert);
        if (cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())) {
          res.add(chain);
          chain = new ArrayList(3);
        }
      } else {
        // Unsupported type
        if (!chain.isEmpty()) {
          failed.addAll(chain);
          chain.clear();
        }
        failed.add(c[i++]);
      }
    }
    // Add remaining certs as failed
    failed.addAll(chain);

    return res;
  }

}
