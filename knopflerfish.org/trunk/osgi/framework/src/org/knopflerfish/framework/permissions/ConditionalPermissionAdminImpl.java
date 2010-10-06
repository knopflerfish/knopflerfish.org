/*
 * Copyright (c) 2008-2010, KNOPFLERFISH project
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


import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.service.condpermadmin.*;

import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.Util;

/**
 * Framework service to administer Conditional Permissions. Conditional
 * Permissions can be added to, retrieved from, and removed from the framework.
 * 
 */
public class ConditionalPermissionAdminImpl implements ConditionalPermissionAdmin {

  public final static String SPEC_VERSION = "1.1.0";

  private ConditionalPermissionInfoStorage cpis;

  private PermissionInfoStorage pis;

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
  public Enumeration getConditionalPermissionInfos() {
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
  public ConditionalPermissionInfo
      newConditionalPermissionInfo(String name,
                                   ConditionInfo conditions[],
                                   PermissionInfo permissions[],
                                   String access) {
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

    private HashMap signerMap = new HashMap();

    DummyBundle(String [] signers) {
      for (int i = 0; i < signers.length; i++) {
        String [] chain = Util.splitwords(signers[i], ";");
        ArrayList tmp = new ArrayList(chain.length);
        for (int j = 0; j < chain.length; j++) {
          tmp.add(new X509Dummy(chain[j]));
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

    public Dictionary getHeaders() {
      return getHeaders(null);
    }

    public long getBundleId() {
      return -1;
    }

    public String getLocation() {
      return "";
    }

    public ServiceReference[] getRegisteredServices() {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public ServiceReference[] getServicesInUse() {
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

    public Map getSignerCertificates(int signersType) {
      return (Map)signerMap.clone();
    }

    public Version getVersion() {
      return Version.emptyVersion;
    }

    public Enumeration findEntries(String path,
                                   String filePattern,
                                   boolean recurse)
    {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public URL getEntry(String name) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Enumeration getEntryPaths(String path) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Dictionary getHeaders(String locale) {
      return new Hashtable();
    }

    public Enumeration getResources(String name) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    public Class loadClass(final String name) {
      throw new IllegalStateException("Bundle is uninstalled");
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

    public void checkValidity() { 
    }   

    public void checkValidity(Date date) { 
    }

    public int getVersion() {
      return 1;
    }

    public BigInteger getSerialNumber() {
      return null;
    }

    public Principal getIssuerDN() {
      return null;
    }

    public Principal getSubjectDN() {
      return subject;
    }

    public Date getNotBefore() {
      return null;
    }

    public Date getNotAfter() {
      return null;
    }

    public byte[] getTBSCertificate() {
      return null;
    }

    public byte[] getSignature() {
      return null;
    }

    public String getSigAlgName() {
      return null;
    }

    public String getSigAlgOID() {
      return null;
    }

    public byte[] getSigAlgParams() {
      return null;
    }

    public boolean[] getIssuerUniqueID() {
      return null;
    }

    public boolean[] getSubjectUniqueID() {
      return null;
    }

    public boolean[] getKeyUsage() {
      return null;
    }

    public int getBasicConstraints() {
      return 0;
    }

    public byte [] getEncoded() {
      return null;
    }

    public PublicKey getPublicKey() {
      return null;
    }

    public String toString() {
      return "X509Dummy, " + subject.getName();
    }

    public void verify(PublicKey k) {
    }

    public void verify(PublicKey k, String p) {
    }

    public byte [] getExtensionValue(String oid) {
      return null;
    }

    public Set getCriticalExtensionOIDs() {
      return null;
    }

    public Set getNonCriticalExtensionOIDs() {
      return null;
    }

    public boolean hasUnsupportedCriticalExtension() {
      return false;
    }

    public boolean equals(Object o) {
      return subject.equals(o);
    }

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
