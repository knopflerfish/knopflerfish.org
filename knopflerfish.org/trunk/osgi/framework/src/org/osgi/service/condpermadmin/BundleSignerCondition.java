/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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

package org.osgi.service.condpermadmin;

import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

import org.knopflerfish.framework.BundleImpl;
import org.knopflerfish.framework.permissions.CertificateUtil;


public class BundleSignerCondition {

  private static final String	CONDITION_TYPE	= "org.osgi.service.condpermadmin.BundleSignerCondition";

  /**
   * Constructs a Condition that tries to match the passed Bundle's certificates
   * to the certficate pattern.
   * 
   * @param bundle The Bundle being evaluated.
   * @param info The ConditionInfo to construct the condition for. The args of
   *        the ConditionInfo specify a single String specifying the chain of
   *        distinguished names pattern to match against the signer of the
   *        Bundle.
   * @return A Condition which checks the signers of the specified bundle.        
   */
  static public Condition getCondition(Bundle bundle, ConditionInfo info) {
    if (!CONDITION_TYPE.equals(info.getType()))
      throw new IllegalArgumentException("ConditionInfo must be of type \""
					 + CONDITION_TYPE + "\"");
    String[] args = info.getArgs();
    if (args.length != 1) {
      throw new IllegalArgumentException("Illegal number of args: " + args.length);
    }
    Certificate [] certs = ((BundleImpl)bundle).getCertificates();
    if (CertificateUtil.matchCertificates(certs, args[0]) >= 0) {
      return Condition.TRUE;
    } else {
      return Condition.FALSE;
    }
  }

  private BundleSignerCondition() {
    // private constructor to prevent objects of this type
  }

}
