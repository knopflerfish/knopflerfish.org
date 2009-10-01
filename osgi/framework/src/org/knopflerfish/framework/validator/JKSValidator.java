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

import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.Validator;
import java.io.*;
import java.security.cert.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.*;


/**
 * JKS certificate validator
 *
 * @author Jan Stein
 */
public class JKSValidator implements Validator {

  /**
   * PKIX algorithm provider string.
   */
  final private static String CERT_ALGORITHM_PKIX = "PKIX";

  /**
   * X509 certificate type string.
   */
  final private static String CERT_TYPE_X509 = "X.509";

  /**
   * Property base string.
   */
  final private static String PROP_BASE = "org.knopflerfish.framework.validator.jks.";

  /**
   * CA certificates repository.
   */
  private String caCertsFileName;

  /**
   * CA certificates repository password.
   */
  private String caCertsPassword;

  /**
   * Certificate provider;
   */
  private String certProvider;

  /**
   * Certificate algorithm, only supports PKIX now.
   */
  private String certAlgorithm = CERT_ALGORITHM_PKIX;

  /**
   * 
   */
  private CertificateFactory certFactory = null;

  /**
   * 
   */
  private String certFactoryType = null;

  /**
   * 
   */
  private CertPathValidator certValidator = null;

  /**
   * 
   */
  private String certValidatorAlgorithm = null;

  /**
   *
   */
  private KeyStore keystore;


  /**
   * Create a JKS based validator.
   *
   * @param fw FrameworkContext used to get configuration properties.
   */
  public JKSValidator(FrameworkContext fw)
  {
    caCertsFileName = fw.props.getProperty(PROP_BASE + "ca_certs");
    caCertsPassword = fw.props.getProperty(PROP_BASE + "ca_certs_password");
    certProvider = fw.props.getProperty(PROP_BASE + "cert_provider");
    keystore = getKeyStore();
  }


  /**
   * Check if a certificate chain is to be trusted.
   *
   * @return true, if validator trusts certificate chain, otherwise false.
   */
  public boolean validateCertificateChain(List /* X509Certificate */ chain) {
    try {
      CertPath c = getCertificateFactory(CERT_TYPE_X509).generateCertPath(chain);
      CertPathValidator cpv = getCertPathValidator(certAlgorithm);
      CertPathParameters params = getCertPathParameters(keystore, certAlgorithm);
      cpv.validate(c, params);
    } catch (GeneralSecurityException gse) {
      // NYI! Log this?
      return false;
    }
    return true;
  }


  /**
   * 
   */
  CertificateFactory getCertificateFactory(String certType)
    throws GeneralSecurityException
  {
    if (certFactory == null || certFactoryType != certType) {
      certFactoryType = certType;
      if (certProvider != null) {
        certFactory = CertificateFactory.getInstance(certType, certProvider);
      } else {
        certFactory = CertificateFactory.getInstance(certType);
      }
    }
    return certFactory;
  }


  /**
   * 
   */
  CertPathParameters getCertPathParameters(KeyStore keystore, String certAlgo)
    throws GeneralSecurityException
  {
    if (CERT_ALGORITHM_PKIX.equals(certAlgo)) {
      PKIXParameters p = new PKIXParameters(keystore);
      // NYI! Handle CRLs
      p.setRevocationEnabled(false);
      return p;
    } else {
      throw new GeneralSecurityException(certAlgo + " not supported");
    }
  }


  /**
   * 
   */
  CertPathValidator getCertPathValidator(String certAlgo)
    throws GeneralSecurityException
  {
    if (certValidator == null || certValidatorAlgorithm != certAlgo) {
      certValidatorAlgorithm = certAlgo;
      if (certProvider != null) {
        certValidator = CertPathValidator.getInstance(certAlgo, certProvider);
      } else {
        certValidator = CertPathValidator.getInstance(certAlgo);
      }
    }
    return certValidator;
  }


  /**
   * 
   */
  KeyStore getKeyStore() {
    if (caCertsFileName == null) {
      caCertsFileName = System.getProperty("java.home")
        + "/lib/security/cacerts".replace('/', File.separatorChar);
    }
    if (caCertsPassword == null) {
      caCertsPassword = "changeit";
    }
    try {
      FileInputStream is = new FileInputStream(caCertsFileName);
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(is, caCertsPassword.toCharArray());
      return keystore;
    } catch (Exception e) {
      System.err.println("Failed to load keystore, " + caCertsFileName + ": " + e);
      // NYI! Log
    }
    return null;
  }

}
