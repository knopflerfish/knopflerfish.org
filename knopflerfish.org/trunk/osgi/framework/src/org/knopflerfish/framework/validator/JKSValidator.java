/*
 * Copyright (c) 2009-2014, KNOPFLERFISH project
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import org.knopflerfish.framework.Debug;
import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.Util;
import org.knopflerfish.framework.Validator;
import org.osgi.framework.Constants;


/**
 * JKS certificate validator
 *
 * @author Jan Stein
 */
public class JKSValidator implements Validator {

  /**
   * Property strings.
   */
  final private static String CA_CERTS_PROP =
    "org.knopflerfish.framework.validator.jks.ca_certs";

  final private static String CA_CERTS_PASSWORD_PROP =
    "org.knopflerfish.framework.validator.jks.ca_certs_password";

  final private static String CERT_PROVIDER_PROP =
    "org.knopflerfish.framework.validator.jks.cert_provider";

  final private static String CERT_DATE_PROP =
      "org.knopflerfish.framework.validator.date";


  /**
   * Certificate provider;
   */
  private String certProvider;

  /**
   * 
   */
  private CertificateFactory certFactory = null;

  /**
   * 
   */
  private CertPathValidator certValidator = null;

  /**
   *
   */
  final private KeyStore keystore;

  /**
   *
   */
  private Date validationDate;

  /**
   * NYI make it configurable
   */
  private boolean trustKeys = true;

  /**
   * Debug handle
   */
  final private Debug debug;


  /**
   * Create a JKS based validator.
   *
   * @param fw FrameworkContext used to get configuration properties.
   * @throws KeyStoreException 
   * @throws ParseException 
   */
  public JKSValidator(FrameworkContext fw) throws KeyStoreException, ParseException
  {
    debug = fw.debug;
    keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    // NYI! Handle serveral repositories.
    fw.props.setPropertyDefault(CERT_PROVIDER_PROP, "");
    certProvider = fw.props.getProperty(CERT_PROVIDER_PROP);
    String repos = fw.props.getProperty(Constants.FRAMEWORK_TRUST_REPOSITORIES);
    if (repos.length() > 0) {
      String [] l = Util.splitwords(repos, File.pathSeparator);
      for (int i = 0; i < l.length; i++) {
        String certRepo = l[i].trim();
        if (certRepo.length() > 0) {
          loadKeyStore(certRepo, null);
        }
      }
    } else {
      fw.props.setPropertyDefault(CA_CERTS_PROP,
                                  System.getProperty("java.home")
                                  + "/lib/security/cacerts".replace('/', File.separatorChar));
      fw.props.setPropertyDefault(CA_CERTS_PASSWORD_PROP, "changeit");
      final String caCertsFileName = fw.props.getProperty(CA_CERTS_PROP);
      if (caCertsFileName != null) {
        loadKeyStore(caCertsFileName, fw.props.getProperty(CA_CERTS_PASSWORD_PROP));
      }
    }
    String d = fw.props.getProperty(CERT_DATE_PROP);
    if (d != null) {
      validationDate = DateFormat.getDateInstance(DateFormat.SHORT).parse(d);
      if (debug.certificates) {
        debug.println("Set validation date to " + validationDate);
      }
    } else {
      validationDate = null;
    }
  }


  /**
   * Check if a certificate chain is to be trusted.
   *
   * @return true, if validator trusts certificate chain, otherwise false.
   */
  public boolean validateCertificateChain(List<X509Certificate> chain) {
    if (keystore == null) {
      return false;
    }
    try {
      CertPath c = getCertificateFactory().generateCertPath(chain);
      CertPathValidator cpv = getCertPathValidator();
      CertPathParameters params = getCertPathParameters(keystore);
      cpv.validate(c, params);
    } catch (GeneralSecurityException gse) {
      if (debug.certificates) {
        debug.printStackTrace("Failed to validate cert", gse);
      }
      // NYI! Log this?
      return false;
    }
    return true;
  }


  /**
   * 
   */
  private CertificateFactory getCertificateFactory()
    throws GeneralSecurityException
  {
    if (certFactory == null) {
      if (certProvider.length() > 0) {
        certFactory = CertificateFactory.getInstance("X.509", certProvider);
      } else {
        certFactory = CertificateFactory.getInstance("X.509");
      }
    }
    return certFactory;
  }


  /**
   * 
   */
  private CertPathParameters getCertPathParameters(KeyStore keystore)
    throws GeneralSecurityException
  {
    HashSet<TrustAnchor> tas = new HashSet<TrustAnchor>();
    for (Enumeration<String> e = keystore.aliases(); e.hasMoreElements(); ) {
      String name = e.nextElement();
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
    if (validationDate != null) {
      p.setDate(validationDate);
    }
    return p;
  }


  /**
   * 
   */
  private CertPathValidator getCertPathValidator()
    throws GeneralSecurityException
  {
    if (certValidator == null) {
      if (certProvider.length() > 0) {
        certValidator = CertPathValidator.getInstance("PKIX", certProvider);
      } else {
        certValidator = CertPathValidator.getInstance("PKIX");
      }
    }
    return certValidator;
  }


  /**
   * 
   */
  private void loadKeyStore(String file, String password) {
    FileInputStream is = null;
    try {
      is = new FileInputStream(file);
      keystore.load(is, password != null ? password.toCharArray() : null);
      if (debug.certificates) {
        debug.println("Loaded keystore, " + file);
      }
    } catch (Exception e) {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) { }
      }
      debug.printStackTrace("Failed to load keystore, " + file, e);
    }
  }

}
