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
    Map map = fw.vectorToMap(fw.getExportedPackage(name));

    return new ExportedPackageImpl(map);
  }


  public ExportedPackage[] getExportedPackages(Bundle b) {
    Vector vecs = fw.getExportedPackages(b.getBundleId());
    Map[] maps = new Map[vecs.size()];
    for (int i=0; i<vecs.size(); i++) maps[i] = fw.vectorToMap((Vector) vecs.elementAt(i));

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
    long[] bids = (bundles == null ? null : new long[bundles.length]);
    if (bids != null) {
      for (int i=0; i<bundles.length; i++) {
        bids[i] = bundles[i].getBundleId();
      }
    }
    fw.refreshPackages(bids);
  }

  class ExportedPackageImpl implements ExportedPackage {
    String   name;
    Bundle   exporting;
    Bundle[] importing;
    String   version;
    boolean  pending;

    ExportedPackageImpl(Map map) {
      name      = map.get("getName").toString();
      version   = map.get("getSpecificationVersion").toString();
      pending   = ((Boolean)map.get("isRemovalPending")).booleanValue();
      exporting = fw.remoteBC.getBundle(((Long)map.get("getExportingBundle")).longValue());
      Object obj = map.get("getImportingBundles");
      //      System.out.println(obj + ": " + obj.getClass().getName());
      //      Long[] bids = new Long[0];
      long[] bids;
      if (obj instanceof long[]) {
        bids = (long[]) obj;
      } else if (obj instanceof Vector) {
        bids = new long[((Vector) obj).size()];
        for (int i=0; i<bids.length; i++) {
          bids[i] = new Long(((Vector) obj).elementAt(i).toString()).longValue();
        }
      } else {
        throw new RuntimeException("getImportingBundles is of type " + obj.getClass().getName());
      }
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
  }

}
