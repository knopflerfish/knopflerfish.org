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
import org.knopflerfish.framework.Util;

import org.osgi.framework.Constants;

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
   * NYI make it configurable
   */
  private boolean trustKeys = true;


  /**
   * Create a JKS based validator.
   *
   * @param fw FrameworkContext used to get configuration properties.
   */
  public JKSValidator(FrameworkContext fw) throws KeyStoreException  
  {
    keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    // NYI! Handle serveral repositories.
    String repos = fw.props.getProperty(Constants.FRAMEWORK_TRUST_REPOSITORIES);
    if (repos != null) {
      String [] l = Util.splitwords(repos, File.pathSeparator);
      for (int i = 0; i < l.length; i++) {
        String certRepo = l[i].trim();
        if (certRepo.length() > 0) {
          loadKeyStore(certRepo, null);
        }
      }
    }
    String caCertsFileName = fw.props.getProperty(PROP_BASE + "ca_certs");
    String caCertsPassword = fw.props.getProperty(PROP_BASE + "ca_certs_password");
    if (caCertsFileName != null) {
      loadKeyStore(caCertsFileName, caCertsPassword);
    }
    certProvider = fw.props.getProperty(PROP_BASE + "cert_provider");
    if (keystore == null || keystore.size() == 0) {
      throw new IllegalStateException("No keystore or keystore empty");
    }
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
      HashSet tas = new HashSet();
      for (Enumeration e = keystore.aliases(); e.hasMoreElements(); ) {
        String name = (String)e.nextElement();
        Certificate c = keystore.getCertificate(name);
        if (c != null) {
          if (trustKeys || keystore.isCertificateEntry(name)) {
            tas.add(new TrustAnchor((X509Certificate)c, null)); 
          }
        }
      }
      PKIXParameters p = new PKIXParameters(tas);
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
  void loadKeyStore(String file, String password) {
    try {
      FileInputStream is = new FileInputStream(file);
      keystore.load(is, password != null ? password.toCharArray() : null);
    } catch (Exception e) {
      System.err.println("Failed to load keystore, " + file + ": " + e);
      // NYI! Log
    }
  }

}
