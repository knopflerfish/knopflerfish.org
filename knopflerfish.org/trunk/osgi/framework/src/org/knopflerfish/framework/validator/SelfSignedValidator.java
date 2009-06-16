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

package org.knopflerfish.framework.validator;

import org.knopflerfish.framework.Framework;
import org.knopflerfish.framework.Validator;
import java.io.*;
import java.security.cert.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.*;
import javax.security.auth.x500.X500Principal;


/**
 * Self signed certificate validator
 *
 * @author Jan Stein
 */
public class SelfSignedValidator implements Validator {

  /**
   * X509 certificate type string.
   */
  final private static String CERT_TYPE_X509 = "X.509";


  /**
   * Check that certificates are valid:
   * 
   */
  public Certificate [] checkCertificates(Certificate [] certs) {
    ArrayList failed = new ArrayList();
    ArrayList chain = new ArrayList();
    String certPathType = null;
    boolean chainEnd = false;
    X500Principal prevIssuer = null;
    int i = 0;
    while (i < certs.length) {
      if (certPathType == null) {
        certPathType = certs[i].getType();
      } else if (certPathType != certs[i].getType()) {
        // Broken chain, we fail everything after this
        break;
      }
      if (certPathType == CERT_TYPE_X509) {
        X509Certificate cert = (X509Certificate) certs[i];
        X500Principal issuer = cert.getIssuerX500Principal();
        X500Principal subject = cert.getSubjectX500Principal();
        // TBD, can we use == and do we need to check uniqID?
        if (prevIssuer != null && !prevIssuer.equals(subject)) {
          // Broken chain, we fail everything after this
          break;
        }
        // TBD, should we test date and other attributes?
        if (subject.equals(issuer)) {
          chainEnd = true;
          prevIssuer = null;
        } else {
          prevIssuer = issuer;
        }
      } else {
        // Unsupported type
        failed.add(certs[i++]);
        continue;
      }
      if (chainEnd) {
        chain.clear();
        chainEnd = false;
        i++;
      } else {
        chain.add(certs[i++]);
      }
    }
    // Add remaining certs as failed
    failed.addAll(chain);
    while (i < certs.length) {
      failed.add(certs[i++]);
    }

    return (Certificate [])failed.toArray(new Certificate[failed.size()]);
  }

}
