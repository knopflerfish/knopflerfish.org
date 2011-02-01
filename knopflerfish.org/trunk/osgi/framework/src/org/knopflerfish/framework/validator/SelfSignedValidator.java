/*
 * Copyright (c) 2009-2011, KNOPFLERFISH project
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

import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.Validator;
import java.security.cert.*;
import java.util.*;

/**
 * Self signed certificate validator
 * 
 * @author Jan Stein
 */
public class SelfSignedValidator implements Validator {

  /**
   * Create a SelfSignedCertificate validator.
   * 
   * @param fw
   *          FrameworkContext used to get configuration properties.
   */
  public SelfSignedValidator(FrameworkContext fw) {
  }

  /**
   * Check if a certificate chain is to be trusted. We expect the input to be a
   * correc chain.
   * 
   * @return true, if validator trusts certificate chain, otherwise false.
   */
  public boolean validateCertificateChain(List /* X509Certificate */chain) {
    try {
      for (Iterator i = chain.iterator(); i.hasNext();) {
        ((X509Certificate) i.next()).checkValidity();
      }
    } catch (CertificateException _) {
      return false;
    }
    return true;
  }

}
