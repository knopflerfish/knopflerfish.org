/*
 * Copyright (c) 2011-2022, KNOPFLERFISH project
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
package org.knopflerfish.cpaexample.bundle.admin;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;

@SuppressWarnings("unused")
public class AdminActivator implements BundleActivator {
  /*
  // initial version, for tested bundles without local permission
  private static final String[] ENCODED_PINFO = {
    "allow { [org.osgi.service.condpermadmin.BundleLocationCondition \"file:jars/*\"] (java.security.AllPermission) } \"allToTrusted\"",
    "allow { [org.osgi.service.condpermadmin.BundleLocationCondition \"file:/opt/kf/totest/*\"] (org.osgi.framework.PackagePermission \"*\" \"import\") } \"importToTested\""
  };
  */

  // final version, for tested bundles that use local permissions
  private static final String[] ENCODED_PINFO = {
    "allow { [org.osgi.service.condpermadmin.BundleLocationCondition \"file:jars/*\"] (java.security.AllPermission) } \"allToTrusted\"",
    "allow { [org.osgi.service.condpermadmin.BundleLocationCondition \"file:/opt/kf/totest/cpaexample_user*\"] (java.security.AllPermission) } \"allToUser\"",
    "allow { [org.osgi.service.condpermadmin.BundleLocationCondition \"file:/opt/kf/totest/cpaexample_caller*\"] (java.security.AllPermission) } \"allToCaller\""
  };

  public void start(BundleContext bc) throws BundleException {
    ServiceReference<ConditionalPermissionAdmin> sRef =
        bc.getServiceReference(ConditionalPermissionAdmin.class);
    if (sRef != null) {
      ConditionalPermissionAdmin cpa = bc.getService(sRef);
     
      installPolicies(cpa, ENCODED_PINFO);
    } else {
      throw new BundleException("Bundle CPA-test can not start, There is no "
          + "ConditinalPermissionAdmin service");
    }
  }

  public void stop(BundleContext context) {
  }
    
  @SuppressWarnings("SameParameterValue")
  void installPolicies(ConditionalPermissionAdmin cpa, String[] pInfos) {
    ConditionalPermissionUpdate cpu = cpa.newConditionalPermissionUpdate();
    List<ConditionalPermissionInfo> piList = cpu.getConditionalPermissionInfos();

    for (String pInfo : pInfos) {
      ConditionalPermissionInfo cpi = cpa.newConditionalPermissionInfo(pInfo);
      piList.add(cpi);
    }
    
    cpu.commit();
  }
  
}
