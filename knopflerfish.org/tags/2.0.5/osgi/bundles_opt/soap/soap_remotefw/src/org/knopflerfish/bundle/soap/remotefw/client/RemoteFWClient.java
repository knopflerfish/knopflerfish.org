package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;

import javax.xml.namespace.QName;

// Axis imports
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import java.net.*;
import java.lang.reflect.*;

import java.io.*;

import org.knopflerfish.util.*;


public class RemoteFWClient implements RemoteFW {

  boolean  bDebug   = "true".equals(System.getProperty("org.knopflerfish.soap.remotefw.client.debug", "false"));

  String   endpoint = null;
  Service  service  = null;
  Call     call     = null;

  BundleContextImpl remoteBC = null;

  Map caches = new HashMap();

  public RemoteFWClient() {
    CacheMap fastCache = new CacheMap(1000);
    CacheMap slowCache = new CacheMap(10000);

    caches.put("getStartLevel",               fastCache);
    caches.put("getBundleStartLevel",         fastCache);
    caches.put("getInitialBundleStartLevel",  fastCache);
    caches.put("isBundlePersistentlyStarted", fastCache);
    caches.put("getBundle",                   fastCache);
    caches.put("getBundles",                  slowCache);
    caches.put("getBundleLocation",           slowCache);
    caches.put("getBundleState",              slowCache);
    caches.put("getBundleManifest",           slowCache);
    caches.put("getRegisteredServices",       fastCache);
    caches.put("getServicesInUse",            fastCache);
    caches.put("getServiceReferences",        fastCache);
    caches.put("getServiceReferences2",       fastCache);
    caches.put("getServiceProperties",        slowCache);
    caches.put("getSystemProperties",         slowCache);
    caches.put("getServiceEvents",            fastCache);
    caches.put("getFrameworkEvents",          slowCache);
    caches.put("getBundleEvents",             fastCache);
    caches.put("getStartLevel",               slowCache);
    caches.put("getBundleStartLevel",         slowCache);

    caches.put("getExportedPackage",  slowCache);
    caches.put("getExportedPackages", slowCache);
  }


  void open(String host) throws Exception {
    endpoint = host + "/axis/services/OSGiFramework";
    service = new Service();
    call    = (Call) service.createCall();
    
    call.setTargetEndpointAddress( new URL(endpoint) );

    System.out.println("opened " + endpoint + 
		       ", bundles=" + doCall("getBundles"));

    remoteBC = new BundleContextImpl(this);
    remoteBC.start();
  }

  public void close() {
    if(remoteBC != null) {
      remoteBC.stop();
      remoteBC = null;
    }
    
    endpoint = null;
    service  = null;
    call     = null;
  }

  void flushCache() {
    synchronized(callLock) {
      for(Iterator it = caches.keySet().iterator(); it.hasNext(); ) {
	String opName = (String)it.next();
	CacheMap cache = (CacheMap)caches.get(opName);
	cache.clear();
      }
    }
  }
  public BundleContext getBundleContext() {
    return remoteBC;
  }


  public void startBundle(long bid) {
    doCall("startBundle", bid);
    flushCache();
  }

  public void stopBundle(long bid) {
    doCall("stopBundle", bid);
    flushCache();
  }

  public void updateBundle(long bid) {
    doCall("updateBundle", bid);
    flushCache();
  }

  public void uninstallBundle(long bid) {
    doCall("uninstallBundle", bid);
    flushCache();
  }

  public long installBundle(String location) {
    Long bid = (Long)doCall("installBundle", location);
    flushCache();
    return bid.longValue();
  }
  

  public long    getBundle() {
    return ((Long)doCall("getBundle")).longValue();
  }

  public long[]    getBundles() {
    return toLongArray(doCall("getBundles"));
  }

  public String      getBundleContextProperty(String key) {
    String v = (String)doCall("getBundleContextProperty", 
			      new Object[] { key });
    return NULL_STR.equals(v) ? null : v;
  }

  public String  getBundleLocation(long bid) {
    return (String)doCall("getBundleLocation", bid);
  }

  public int  getBundleState(long bid) {
    return ((Integer)doCall("getBundleState", bid)).intValue();
  }


  public long[]    getServicesInUse(long bid) {
    return toLongArray(doCall("getServicesInUse", bid));
  }

  public long[]    getRegisteredServices(long bid) {
    return toLongArray(doCall("getRegisteredServices", bid));
  }

  public long[]    getServiceReferences(String filter) {
    return getServiceReferences2(null, filter);
  }
  
  public long[]    getServiceReferences2(String clazz, String filter) {
    return toLongArray(doCall("getServiceReferences2", 
			      new Object[] {
				clazz  == null ? NULL_STR : clazz, 
				filter == null ? NULL_STR : filter
			      }));
  }

  public Map  getBundleManifest(long bid) {
    return (Map)doCall("getBundleManifest", bid);
  }


  public long[]    getServices() {
    return toLongArray(doCall("getServices"));
  }

  public long[]    getFrameworkEvents() {
    return toLongArray(doCall("getFrameworkEvents"));
  }

  public long[]    getBundleEvents() {
    return toLongArray(doCall("getBundleEvents"));
  }

  public long[]    getServiceEvents() {
    return toLongArray(doCall("getServiceEvents"));
  }

  public Map  getServiceProperties(long sid) {
    return (Map)doCall("getServiceProperties", sid);
  }

  
  public int  getStartLevel() {
    return ((Integer)doCall("getStartLevel")).intValue();
  }


  public void setStartLevel(int level) {
    doCall("setStartLevel", level);
  }
  
  public void setBundleStartLevel(long bid, int level) {
    doCall("setBundleStartLevel", new Object[] { new Long(bid),
						 new Integer(level)});
  }

  public int  getBundleStartLevel(long bid) {
    return ((Integer)doCall("getBundleStartLevel", bid)).intValue();
  }


  public void setInitialBundleStartLevel(int level){
    doCall("setInitialBundleStartLevel", level);
  }
  
  public int  getInitialBundleStartLevel() {
    return ((Integer)doCall("getInitialBundleStartLevel")).intValue();
  }

  public boolean isBundlePersistentlyStarted(long bid) {
    return ((Boolean)doCall("isBundlePersistentlyStarted", bid)).booleanValue();
  }


  public Map    getExportedPackage(String name) {
    return (Map)doCall("getExportedPackage", name);
  }

  public Map[]  getExportedPackages(long bid) {
    return (Map[])doCall("getExportedPackages", bid);
  }

  public void   refreshPackages(long[] bids) {
    if(bids == null) {
      doCall("refreshPackages", new long[0]);
    } else {
      doCall("refreshPackages", bids);
    }
  }

  public Map    getSystemProperties() {
    return (Map)doCall("getSystemProperties");
  }

  Object doCall(String opName) {
    return doCall(opName, new Object[0]);
  }

  Object doCall(String opName, long val) {
    return doCall(opName, new Object[] { new Long(val) });
  }

  Object doCall(String opName, int val) {
    return doCall(opName, new Object[] { new Integer(val) });
  }

  Object doCall(String opName, Object val) {
    return doCall(opName, new Object[] { val });
  }
  
  Object callLock = new Object();

  Object doCall(String opName, Object[] params) {
    synchronized(callLock) {
      String cacheKey = null;
      Map cache = (Map)caches.get(opName);

      if(cache != null) {
	cacheKey = opName + ":" + toDisplay(params);
	Object cacheResult = cache.get(cacheKey);
	
	if(cacheResult != null) {
	  if(bDebug) {
	    System.out.println("cached " + opName + 
			       "(" + toDisplay(params) + ")");
	  }
	  return cacheResult;
	}
      }
      try {
	call.setOperationName(new QName(opName));
	
	if(bDebug) {
	  System.out.println("doCall " + opName + 
			     "(" + toDisplay(params) + ")");
	}
	Object r = call.invoke(params);
	
	if(cache != null) {
	  cache.put(cacheKey, r);
	}
	return r;
      } catch (Exception e) {
	e.printStackTrace();
	throw new NestedRuntimeException("Failed to call " + opName + ": ", e);
      }
    }
  }

  static long[] toLongArray(Object obj) {
    if(obj == null) {
      return null;
    }
    long[] la = new long[Array.getLength(obj)];

    for(int i = 0; i < la.length; i++) {
      la[i] = ((Long)Array.get(obj, i)).longValue();
    }
    return la;
  }

  public static Object toDisplay(Object val) {
    if(val != null) {
      if(NULL_STR.equals(val)) {
	return "null";
      }
      if(val instanceof String) {
	return "\"" + val + "\"";
      }
      if(val.getClass().isArray()) {
	StringBuffer sb = new StringBuffer();
	sb.append("[");
	for(int i = 0; i < Array.getLength(val); i++) {
	  sb.append(toDisplay(Array.get(val, i)));
	  if(i < Array.getLength(val) - 1) {
	    sb.append(",");
	  }
	}
	sb.append("]");
	return sb.toString();
      }
    }

    return val;
  }
}


class NestedRuntimeException extends RuntimeException {
  Throwable nested;

  public NestedRuntimeException(String msg, Throwable t) {
    super(msg);
    this.nested = nested;
  }

  public String getMessage() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.getMessage());

    if(nested != null) {
      StringWriter sw = new StringWriter();
      nested.printStackTrace(new PrintWriter(sw));
      sb.append(", Nested exception:\n" + sw.toString());
    }

    return sb.toString();
  }

  public String toString() {
    return getClass().toString() + ": " + getMessage();
  }
}
