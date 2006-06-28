/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
