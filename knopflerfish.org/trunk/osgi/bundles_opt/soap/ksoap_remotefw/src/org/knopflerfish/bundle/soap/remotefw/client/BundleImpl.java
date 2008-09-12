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
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

public class BundleImpl implements Bundle {

  long bid;
  RemoteFWClient fw;

  long[] sids;

  BundleImpl(RemoteFWClient fw, long bid) {
    this.fw  = fw;
    this.bid = bid;

    load();
  }

  public long getBundleId() {
    return bid;
  }

  void load() {
    sids = fw.getRegisteredServices(bid);
  }

  public BundleContext getBundleContext() {
    return fw.remoteBC;
  }

  public Dictionary getHeaders() {
    Hashtable props = new Hashtable();
    Vector vector = fw.getBundleManifest(bid);
    for (Enumeration enumIsReserved = vector.elements(); enumIsReserved.hasMoreElements();) {
      Object key = enumIsReserved.nextElement();
      if (!enumIsReserved.hasMoreElements()) break;
      Object val = enumIsReserved.nextElement();
      props.put(key.toString(), val.toString());
    }
    return props;
  }


  public String getLocation() {
    return fw.getBundleLocation(bid);
  }

  public ServiceReference[] getRegisteredServices() {

    load();

    if(sids.length == 0) {
      return null;
    }
    ServiceReference[] srl = new ServiceReference[sids.length];

    for(int i = 0; i < sids.length; i++) {
      srl[i] = new ServiceReferenceImpl(fw, this, sids[i]);
    }
    return srl;
  }

  public URL getResource(String name) {
    return null;
  }

  public ServiceReference[] getServicesInUse() {
    return new ServiceReference[0];
  }

  public int getState() {
    return fw.getBundleState(bid);
  }

  public boolean hasPermission(Object permission) {
    return true;
  }

  public void start() {
    fw.startBundle(bid);
    fw.remoteBC.doEvents();
  }

  public void stop() {
    fw.stopBundle(bid);
    fw.remoteBC.doEvents();
  }

  public void uninstall() {
    fw.uninstallBundle(bid);
    fw.remoteBC.doEvents();
  }

  public void update() {
    fw.updateBundle(bid);
    fw.remoteBC.doEvents();
  }

  public void update(InputStream in) {
    throw new RuntimeException("Not implemented");
  }

  public int hashCode() {
    return (int)bid;
  }

  public boolean equals(Object other) {
    if(other == null || other.getClass() != BundleImpl.class) {
      return false;
    }
    return bid == ((BundleImpl)other).bid;
  }

public Enumeration findEntries(String path, String filePattern, boolean recurse) {
	// TODO Auto-generated method stub
	return null;
}

public URL getEntry(String name) {
	// TODO Auto-generated method stub
	return null;
}

public Enumeration getEntryPaths(String path) {
	// TODO Auto-generated method stub
	return null;
}

public Dictionary getHeaders(String locale) {
	// TODO Auto-generated method stub
	return null;
}

public long getLastModified() {
	// TODO Auto-generated method stub
	return 0;
}

public Enumeration getResources(String name) throws IOException {
	// TODO Auto-generated method stub
	return null;
}

public String getSymbolicName() {
	// TODO Auto-generated method stub
	return null;
}

public Class loadClass(String name) throws ClassNotFoundException {
	// TODO Auto-generated method stub
	return null;
}
}
