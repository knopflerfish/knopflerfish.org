package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.packageadmin.*;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

public class PackageAdminImpl implements PackageAdmin {

  RemoteFWClient fw;

  PackageAdminImpl(RemoteFWClient fw) {
    this.fw  = fw;
  }

  public ExportedPackage getExportedPackage(String name) {
    Map map = fw.getExportedPackage(name);

    return new ExportedPackageImpl(map);
  }

  
  public ExportedPackage[] getExportedPackages(Bundle b) {
    Map[] maps = fw.getExportedPackages(b.getBundleId());
    
    if(maps.length == 0) {
      return null;
    }
    ExportedPackage[] pkgs = new ExportedPackage[maps.length];
    for(int i = 0; i < maps.length; i++) {
      pkgs[i] = new ExportedPackageImpl(maps[i]);
    }
    return pkgs;
  }

  
  public void refreshPackages(Bundle[] bundles) {
  }

  class ExportedPackageImpl implements ExportedPackage {
    String   name;
    Bundle   exporting;
    Bundle[] importing;
    String   version;
    boolean  pending;

    ExportedPackageImpl(Map map) {
      name      = (String)map.get("getName");
      version   = (String)map.get("getSpecificationVersion");
      pending   = ((Boolean)map.get("isRemovalPending")).booleanValue();
      exporting = fw.remoteBC.getBundle(((Long)map.get("getExportingBundle")).longValue());
      Object obj = map.get("getImportingBundles");
      //      System.out.println(obj + ": " + obj.getClass().getName());
      //      Long[] bids = new Long[0];
      long[] bids = (long[])map.get("getImportingBundles");
      if(bids.length == 0) {
	importing = null;
      } else {
	importing = new Bundle[bids.length];
	for(int i = 0; i < bids.length; i++) {
	  importing[i] = fw.remoteBC.getBundle(bids[i]);
	}
      }
    }

    public Bundle getExportingBundle() {
      return exporting;
    }
    public Bundle[] getImportingBundles() {
      return importing;
    }
    public String getName() {
      return name;
    }
    public String getSpecificationVersion() {
      return version;
    }
    public boolean isRemovalPending() {
      return pending;
    }
    
    public Version getVersion() {
    	// TODO Auto-generated method stub
    	return null;
    }
  }

public Bundle getBundle(Class clazz) {
	// TODO Auto-generated method stub
	return null;
}

public Bundle[] getBundles(String symbolicName, String versionRange) {
	// TODO Auto-generated method stub
	return null;
}

public int getBundleType(Bundle bundle) {
	// TODO Auto-generated method stub
	return 0;
}

public ExportedPackage[] getExportedPackages(String name) {
	// TODO Auto-generated method stub
	return null;
}

public Bundle[] getFragments(Bundle bundle) {
	// TODO Auto-generated method stub
	return null;
}

public Bundle[] getHosts(Bundle bundle) {
	// TODO Auto-generated method stub
	return null;
}

public RequiredBundle[] getRequiredBundles(String symbolicName) {
	// TODO Auto-generated method stub
	return null;
}

public boolean resolveBundles(Bundle[] bundles) {
	// TODO Auto-generated method stub
	return false;
}

}
