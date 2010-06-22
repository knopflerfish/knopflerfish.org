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
  private static String caCertsFileName = Framework.getProperty(PROP_BASE + "ca_certs");

  /**
   * CA certificates repository password.
   */
  private static String caCertsPassword = Framework.getProperty(PROP_BASE + "ca_certs_password");

  /**
   * Certificate provider;
   */
  final private static String certProvider = Framework.getProperty(PROP_BASE + "cert_provider");

  /**
   * Certificate algorithm, only supports PKIX now.
   */
  private static String certAlgorithm = CERT_ALGORITHM_PKIX;

  /**
   * 
   */
  private static CertificateFactory certFactory = null;

  /**
   * 
   */
  private static String certFactoryType = null;

  /**
   * 
   */
  private static CertPathValidator certValidator = null;

  /**
   * 
   */
  private static String certValidatorAlgorithm = null;

  /**
   *
   */
  private KeyStore keystore;


  /**
   * Create an Archive based on contents of an InputStream,
   * the archive is saved as local copy in the specified
   * directory.
   *
   * @param storage BundleStorageImpl for this archive.
   * @param dir Directory to save data in.
   * @param rev Revision of bundle content (used for updates).
   * @param is Jar file data in an InputStream.
   * @param url URL to use to CodeSource.
   * @param location Location for archive
   */
  public JKSValidator()
  {
    keystore = getKeyStore();
  }


  /**
   * Check that certificates are valid:
   * 
   */
  public Certificate [] checkCertificates(Certificate [] certs) {
    List failed = new ArrayList();
    for (Iterator i = getCertificateChains(certs, failed).iterator(); i.hasNext();) {
      CertPath c = (CertPath)i.next();
      try {
        CertPathValidator cpv = getCertPathValidator(certAlgorithm);
        CertPathParameters params = getCertPathParameters(keystore, certAlgorithm);
        cpv.validate(c, params);
      } catch (GeneralSecurityException gse) {
        // NYI! Log this?
        failed.addAll(c.getCertificates());
      }
    }
    return (Certificate [])failed.toArray(new Certificate[failed.size()]);
  }


  /**
   * Take an array of certificates and arrange them as a list
   * of CertPath objects.
   *
   */
  private List getCertificateChains(Certificate [] c, List failed) {
    ArrayList res = new ArrayList();
    ArrayList chain = new ArrayList();
    String certPathType = null;
    boolean foundAnchor = false;
    int i = 0;
    while (i < c.length) {
      if (certPathType == null) {
        certPathType = c[i].getType();
      } else if (certPathType != c[i].getType()) {
        // Broken chain, we fail everything after this
        break;
      }
      if (certPathType == CERT_TYPE_X509) {
        X509Certificate cert = (X509Certificate) c[i];
        // TBD, can we use == and do we need to check uniqID?
        if (cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())) {
          foundAnchor = true;
        }
      } else {
        // Unsupported type
        failed.add(c[i++]);
        continue;
      }
      chain.add(c[i++]);
      if (foundAnchor) {
        try {
          res.add(getCertificateFactory(certPathType).generateCertPath(chain));
        } catch (GeneralSecurityException gse) {
          failed.addAll(chain);
        }
        chain.clear();
        foundAnchor = false;
      }
    }
    // Add remaining certs as failed
    failed.addAll(chain);
    while (i < c.length) {
      failed.add(c[i++]);
    }

    return res;
  }


  /**
   * 
   */
  static CertificateFactory getCertificateFactory(String certType)
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
  static CertPathParameters getCertPathParameters(KeyStore keystore, String certAlgo)
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
