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
  RemoteFW fw;

  long[] sids;

  BundleImpl(RemoteFW fw, long bid) {
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
  }
  
  public void stop() {
    fw.stopBundle(bid);
  }

  public void uninstall() {
    fw.uninstallBundle(bid);
  }

  public void update() {
    fw.updateBundle(bid);
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
}
