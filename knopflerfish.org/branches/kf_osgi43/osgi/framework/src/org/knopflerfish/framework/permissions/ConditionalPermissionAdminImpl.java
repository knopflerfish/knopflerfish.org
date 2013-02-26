/*
 * Copyright (c) 2008-2013, KNOPFLERFISH project
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


import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;

import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.Util;

/**
 * Framework service to administer Conditional Permissions. Conditional
 * Permissions can be added to, retrieved from, and removed from the framework.
 *
 */
public class ConditionalPermissionAdminImpl implements ConditionalPermissionAdmin {

  public final static String SPEC_VERSION = "1.1.0";

  private final ConditionalPermissionInfoStorage cpis;

  private final PermissionInfoStorage pis;

  final private FrameworkContext framework;


  /**
   *
   */
  public ConditionalPermissionAdminImpl(ConditionalPermissionInfoStorage cpis,
                                        PermissionInfoStorage pis,
                                        FrameworkContext framework) {
    this.cpis = cpis;
    this.pis = pis;
    this.framework = framework;
  }


  //
  // Interface ConditionalPermissionAdmin
  //

  /**
   * Create a new Conditional Permission Info.
   *
   * The Conditional Permission Info will be given a unique, never reused
   * name.
   *
   * @param conds The Conditions that need to be satisfied to enable the
   *        corresponding Permissions.
   * @param perms The Permissions that are enable when the corresponding
   *        Conditions are satisfied.
   * @return The ConditionalPermissionInfo for the specified Conditions and
   *         Permissions.
   * @throws SecurityException If the caller does not have
   *         <code>AllPermission</code>.
   */
  public ConditionalPermissionInfo
  addConditionalPermissionInfo(ConditionInfo conds[], PermissionInfo perms[]) {
    return cpis.put(null, conds, perms);
  }


  /**
   * Set or create a Conditional Permission Info with a specified name.
   *
   * If the specified name is <code>null</code>, a new Conditional
   * Permission Info must be created and will be given a unique, never reused
   * name. If there is currently no Conditional Permission Info with the
   * specified name, a new Conditional Permission Info must be created with
   * the specified name. Otherwise, the Conditional Permission Info with the
   * specified name must be updated with the specified Conditions and
   * Permissions.
   *
   * @param name The name of the Conditional Permission Info, or
   *        <code>null</code>.
   * @param conds The Conditions that need to be satisfied to enable the
   *        corresponding Permissions.
   * @param perms The Permissions that are enable when the corresponding
   *        Conditions are satisfied.
   * @return The ConditionalPermissionInfo that for the specified name,
   *         Conditions and Permissions.
   * @throws SecurityException If the caller does not have
   *         <code>AllPermission</code>.
   */
  public ConditionalPermissionInfo
  setConditionalPermissionInfo(String name, ConditionInfo conds[], PermissionInfo perms[]) {
    return cpis.put(name, conds, perms);
  }


  /**
   * Returns the Conditional Permission Infos that are currently managed by
   * Conditional Permission Admin. Calling
   * {@link ConditionalPermissionInfo#delete()} will remove the Conditional
   * Permission Info from Conditional Permission Admin.
   *
   * @return An enumeration of the Conditional Permission Infos that are
   *         currently managed by Conditional Permission Admin.
   */
  public Enumeration<ConditionalPermissionInfo> getConditionalPermissionInfos() {
    return cpis.getAllEnumeration();
  }


  /**
   * Return the Conditional Permission Info with the specified name.
   *
   * @param name The name of the Conditional Permission Info to be returned.
   * @return The Conditional Permission Info with the specified name.
   */
  public ConditionalPermissionInfo getConditionalPermissionInfo(String name) {
    return cpis.get(name);
  }


  /**
   * Returns the Access Control Context that corresponds to the specified
   * signers.
   *
   * @param signers The signers for which to return an Access Control Context.
   * @return An <code>AccessControlContext</code> that has the Permissions
   *         associated with the signer.
   */
  public AccessControlContext getAccessControlContext(String[] signers) {
    PermissionCollection perms;
    synchronized (cpis) {
      perms = new PermissionsWrapper(framework, pis, cpis, null, new DummyBundle(signers), null);
    }
    return new AccessControlContext(new ProtectionDomain[] {new ProtectionDomain(null, perms)});
  }



  /**
   *
   * @see org.osgi.service.condpermadmin.ConditionalPermissionAdmin#newConditionalPermissionUpdate()
   */
  public ConditionalPermissionUpdate newConditionalPermissionUpdate() {
    return cpis.getUpdate();
  }


  /**
   *
   * @see org.osgi.service.condpermadmin.ConditionalPermissionAdmin#newConditionalPermissionInfo()
   */
  public ConditionalPermissionInfo newConditionalPermissionInfo(String name,
                                                                ConditionInfo conditions[],
                                                                PermissionInfo permissions[],
                                                                String access)
  {
    if (ConditionalPermissionInfo.ALLOW.equalsIgnoreCase(access)) {
      access = ConditionalPermissionInfo.ALLOW;
    } else if (ConditionalPermissionInfo.DENY.equalsIgnoreCase(access)) {
      access = ConditionalPermissionInfo.DENY;
    } else {
      throw new IllegalArgumentException("access must be " +
                                         ConditionalPermissionInfo.ALLOW +
                                         " or " +
                                         ConditionalPermissionInfo.DENY);
    }
    if (permissions == null || permissions.length == 0) {
      throw new IllegalArgumentException("permissions must contain atleast one element");
    }
    return new ConditionalPermissionInfoImpl(cpis, name, conditions,
                                             permissions, access, framework);
  }


  /**
   *
   * @see org.osgi.service.condpermadmin.ConditionalPermissionAdmin#newConditionalPermissionInfo()
   */
  public ConditionalPermissionInfo newConditionalPermissionInfo(String encoded)  {
    return new ConditionalPermissionInfoImpl(cpis, encoded, framework);
  }


  /**
   * Dummy Bundle class only used for getAccessControlContext().
   */
  static class DummyBundle implements Bundle {

    private final HashMap<X509Certificate,List<X509Certificate>> signerMap
      = new HashMap<X509Certificate, List<X509Certificate>>();

    DummyBundle(String [] signers) {
      for (final String signer : signers) {
        final String [] chain = Util.splitwords(signer, ";");
        final ArrayList<X509Certificate> tmp
          = new ArrayList<X509Certificate>(chain.length);
        for (final String element : chain) {
          tmp.add(new X509Dummy(element));
        }
        signerMap.put(tmp.get(0), tmp);
      }
    }


    public int getState() {
      return UNINSTALLED;
    }

    public void start() {
      start(0);
    }

    public void start(int options) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public void stop() {
      stop(0);
    }

    public void stop(int options) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public void update() {
      update(null);
    }

    public void update(final InputStream in) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public void uninstall() {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Dictionary<String, String> getHeaders() {
      return getHeaders(null);
    }

    public long getBundleId() {
      return -1;
    }

    public String getLocation() {
      return "";
    }

    public ServiceReference<?>[] getRegisteredServices() {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public ServiceReference<?>[] getServicesInUse() {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public boolean hasPermission(Object permission) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public BundleContext getBundleContext() {
      return null;
    }

    public URL getResource(String name) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public String getSymbolicName() {
      return null;
    }

    public long getLastModified() {
      return 0;
    }

    public Map<X509Certificate, List<X509Certificate>>
      getSignerCertificates(int signersType)
    {
      @SuppressWarnings("unchecked")
      final Map<X509Certificate, List<X509Certificate>> res
        = (Map<X509Certificate, List<X509Certificate>>) signerMap.clone();
      return res;
    }

    public Version getVersion() {
      return Version.emptyVersion;
    }

    public <A> A adapt(Class<A> type) {
      return null;
    }

    public File getDataFile(String filename) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Enumeration<URL> findEntries(String path,
                                        String filePattern,
                                        boolean recurse)
    {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public URL getEntry(String name) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Enumeration<String> getEntryPaths(String path) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Dictionary<String, String> getHeaders(String locale) {
      return new Hashtable<String, String>();
    }

    public Enumeration<URL> getResources(String name) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Class<?> loadClass(final String name) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public int compareTo(Bundle bundle) {
      return 0;
    }
  }

  /**
   * Dummy X509 class only used for getAccessControlContext().
   */
  static class X509Dummy extends X509Certificate {

    Principal subject;

    X509Dummy(String dn) {
      super();
      subject = new PrincipalDummy(dn);
    }

    @Override
    public void checkValidity() {
    }

    @Override
    public void checkValidity(Date date) {
    }

    @Override
    public int getVersion() {
      return 1;
    }

    @Override
    public BigInteger getSerialNumber() {
      return null;
    }

    @Override
    public Principal getIssuerDN() {
      return null;
    }

    @Override
    public Principal getSubjectDN() {
      return subject;
    }

    @Override
    public Date getNotBefore() {
      return null;
    }

    @Override
    public Date getNotAfter() {
      return null;
    }

    @Override
    public byte[] getTBSCertificate() {
      return null;
    }

    @Override
    public byte[] getSignature() {
      return null;
    }

    @Override
    public String getSigAlgName() {
      return null;
    }

    @Override
    public String getSigAlgOID() {
      return null;
    }

    @Override
    public byte[] getSigAlgParams() {
      return null;
    }

    @Override
    public boolean[] getIssuerUniqueID() {
      return null;
    }

    @Override
    public boolean[] getSubjectUniqueID() {
      return null;
    }

    @Override
    public boolean[] getKeyUsage() {
      return null;
    }

    @Override
    public int getBasicConstraints() {
      return 0;
    }

    @Override
    public byte [] getEncoded() {
      return null;
    }

    @Override
    public PublicKey getPublicKey() {
      return null;
    }

    @Override
    public String toString() {
      return "X509Dummy, " + subject.getName();
    }

    @Override
    public void verify(PublicKey k) {
    }

    @Override
    public void verify(PublicKey k, String p) {
    }

    public byte [] getExtensionValue(String oid) {
      return null;
    }

    public Set<String> getCriticalExtensionOIDs() {
      return null;
    }

    public Set<String> getNonCriticalExtensionOIDs() {
      return null;
    }

    public boolean hasUnsupportedCriticalExtension() {
      return false;
    }

    @Override
    public boolean equals(Object o) {
      return subject.equals(o);
    }

    @Override
    public int hashCode() {
      return subject.hashCode();
    }
  }

  /**
   * Dummy Principal class only used for getAccessControlContext().
   */
  static class PrincipalDummy implements Principal {

    final private String name;

    PrincipalDummy(String dn) {
      name = dn;
    }

    public String getName() {
      return name;
    }
  }
}
