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
    Map map = fw.getBundleManifest(bid);
    for(Iterator it = map.keySet().iterator(); it.hasNext();) {
      String key = (String)it.next();
      Object val = map.get(key);

      props.put(key, val);
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
      srl[i] = new ServiceReferenceImpl(this, sids[i]);
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
