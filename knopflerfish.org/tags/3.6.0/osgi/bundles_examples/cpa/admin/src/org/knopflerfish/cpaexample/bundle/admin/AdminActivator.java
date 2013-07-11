package org.knopflerfish.cpaexample.bundle.admin;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;

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

  public void start(BundleContext bc) throws BundleException
  {
    ServiceReference sRef =
      bc.getServiceReference(ConditionalPermissionAdmin.class.getName());
    if (sRef != null) {
      ConditionalPermissionAdmin cpa =
        (ConditionalPermissionAdmin) bc.getService(sRef);
     
      installPolicies(cpa, ENCODED_PINFO);
    } else {
      throw new BundleException("Bundle CPA-test can not start, There is no "
          + "ConditinalPermissionAdmin service");
    }
  }

  public void stop(BundleContext context)
  {
  }
    
  void installPolicies(ConditionalPermissionAdmin cpa, String[] pInfos) {
    ConditionalPermissionUpdate cpu = cpa.newConditionalPermissionUpdate();
    List piList = cpu.getConditionalPermissionInfos();
    
    for (int i = 0; i < pInfos.length; i++) {
      String pInfo = pInfos[i];
      ConditionalPermissionInfo cpi = cpa.newConditionalPermissionInfo(pInfo);
      piList.add(cpi);
    }
    
    cpu.commit();
  }
  
}
