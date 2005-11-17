package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

public class ServiceReferenceImpl implements ServiceReference {

  long       sid;
  BundleImpl bundle;
  String[]   keys;

  Hashtable props = new Hashtable();
  
  ServiceReferenceImpl(BundleImpl bundle, long sid) {
    this.bundle = bundle;
    this.sid    = sid;
    update();
  }

  void update() {
    synchronized(props) {
      Map map = bundle.fw.getServiceProperties(sid);
      
      props.clear();
      keys = new String[map.size()];
      
      //      System.out.println("SR sid= " + sid);
      
      int i = 0;
      for(Iterator it = map.keySet().iterator(); it.hasNext();) {
	String key = (String)it.next();
	
	Object val = map.get(key);
	key = key.toLowerCase();

	props.put(key, val);
	keys[i] = key;

	//	System.out.println(key + "=" + val);
	i++;
      }
    }
  }

  public Bundle getBundle() {
    return bundle;
  }

  public Object getProperty(String key) {
    return props.get(key.toLowerCase());
  }

  public String[] getPropertyKeys() {
    return keys;
  }

  public Bundle[] getUsingBundles() {
    return new Bundle[0];
  }

  public String toString() {
    return "ServiceReferenceImpl[" + 
      "service.id=" + sid + 
      ", objectclass=" + RemoteFWClient.toDisplay(getProperty("objectclass")) + 
      "]";
    
  }

public boolean isAssignableTo(Bundle bundle, String className) {
	// TODO Auto-generated method stub
	return false;
}
}
