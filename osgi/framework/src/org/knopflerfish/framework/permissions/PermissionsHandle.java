/*
 * Copyright (c) 2006-2013, KNOPFLERFISH project
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
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.Hashtable;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.hooks.weaving.WovenClassListener;

import org.knopflerfish.framework.FrameworkContext;
import org.osgi.framework.wiring.BundleWiring;


/**
 *
 */
public class PermissionsHandle implements WovenClassListener {

  FrameworkContext framework;

  private final PermissionInfoStorage pinfos;
  private final ConditionalPermissionInfoStorage cpinfos;
  private final Hashtable<Long, PermissionsWrapper> pcCache
    = new Hashtable<Long, PermissionsWrapper>();
  private final PermissionAdminImpl pa;
  private final ConditionalPermissionAdminImpl cpa;


  /**
   *
   */
  public PermissionsHandle(FrameworkContext fw) {
    framework = fw;
    pinfos = new PermissionInfoStorage(fw);
    pa = new PermissionAdminImpl(pinfos);
    cpinfos = new ConditionalPermissionInfoStorage(this);
    cpa = new ConditionalPermissionAdminImpl(cpinfos, pinfos, fw);
    Policy.setPolicy(new FrameworkPolicy(Policy.getPolicy(), this));
  }


  /**
   * Get PermissionAdmin service.
   *
   * @return PermissionAdmin service object.
   */
  public PermissionAdminImpl getPermissionAdminService() {
    return pa;
  }


  /**
   * Get ConditionalPermissionAdmin service.
   *
   * @return ConditionalPermissionAdmin service object.
   */
  public ConditionalPermissionAdminImpl getConditionalPermissionAdminService() {
    return cpa;
  }


  /**
   * Gets the permissionCollection assigned to the bundle with the specified id.
   * The collection contains the configured permissions for the bundle location
   * plus implicit granted permissions (i.e FilePermission for the data area).
   *
   * @param bid The bundle id whose permissions are to be returned.
   *
   * @return The permissions assigned to the bundle with the specified
   * location, or the default permissions if that bundle has not been assigned
   * any permissions or does not yet exist.
   */
  public PermissionCollection getPermissionCollection(Long bid) {
    return pcCache.get(bid);
  }


  /**
   * Create the permissionCollection assigned to the bundle.
   * We return a permission wrapper so that we can change it dynamically.
   *
   * @param loc Location of bundle whose permissions are to be created.
   * @param b Bundle whose permissions are to be created.
   * @param localPerms New local permissions for the bundle.
   *
   * @return The permissions assigned to the bundle with the specified
   * location
   */
  public  PermissionCollection createPermissionCollection(String loc,
                                                          Bundle b,
                                                          InputStream localPerms) {
    final Long bid = new Long(b.getBundleId());
    // Need to lock cond.perm. changes, when adding a new PermissionsWrapper
    PermissionsWrapper pc;
    synchronized (cpinfos) {
      pc = new PermissionsWrapper(framework, pinfos, cpinfos, loc, b, localPerms);
      pcCache.put(bid, pc);
    }
    return pc;
  }


  /**
   * Remove cached information about specified bundle.
   *
   * @param bid Bundle ID for bundle to be purged.
   *
   * @return True if we purged something, which means that we purged the last
   *         permissionCollection not a zombie.
   */
  public boolean purgePermissionCollection(Long bid, PermissionCollection pc) {
    pinfos.purgeCallback(pc);
    if (pcCache.get(bid) == pc) {
      pcCache.remove(bid);
      return true;
    }
    return false;
  }

  /**
   * Get iterator over all active PermissionWrappers.
   *
   * @return Iterator of PermissionWrappers
   */
  Iterator<PermissionsWrapper> getPermissionWrappers() {
    return pcCache.values().iterator();
  }


  /**
   * Listener for woven classes.
   */
  @Override
  public void modified(WovenClass wc)
  {
    if ((wc.getState() & (WovenClass.DEFINED|WovenClass.DEFINE_FAILED)) != 0) {
      BundleWiring bundleWiring = wc.getBundleWiring();
      if (bundleWiring != null) {
        PermissionsWrapper pw = pcCache.get(new Long(bundleWiring.getBundle().getBundleId()));
        if (pw != null) { // TODO, is this really necessary.
          pw.addWovenDynamicImport(wc.getDynamicImports());
        }
      }
    }
  }

}
