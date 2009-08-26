/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.startlevel.*;
import org.osgi.util.tracker.*;

import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.service.soap.remotefw.*;
import org.knopflerfish.bundle.soap.remotefw.*;
import org.knopflerfish.util.*;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;


public class RemoteFWClient implements RemoteFW {

  boolean  bDebug   = "true".equals(System.getProperty("org.knopflerfish.soap.remotefw.client.debug", "false"));

  String   endpoint = null;
  HttpTransportSE httpTransport = null;
  SoapSerializationEnvelope soapEnvelope = null;

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
    caches.put("getUsingBundles",             slowCache);
    caches.put("getServiceProperties",        slowCache);
    caches.put("getSystemProperties",         slowCache);
    caches.put("getServiceEvents",            fastCache);
    caches.put("getFrameworkEvents",          slowCache);
    caches.put("getBundleEvents",             fastCache);
    caches.put("getStartLevel",               slowCache);
    caches.put("getBundleStartLevel",         slowCache);

    // Package Admin methods
    caches.put("getExportedPackage",           slowCache);
    caches.put("getExportedPackages",          slowCache);
    caches.put("getExportedPackagesByPkgName", slowCache);
    caches.put("getRequiredBundles",           slowCache);
    caches.put("getFragments",                 slowCache);
    caches.put("getHosts",                     slowCache);
    caches.put("getBundlesPA",                 fastCache);
    caches.put("getBundleType",                slowCache);
  }


  void open(String host) throws Exception {
    endpoint = host + "/soap/services/OSGiFramework";

    httpTransport = new HttpTransportSE(endpoint);
    soapEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
    httpTransport.debug = true;

    SoapObject rpc = new SoapObject("http://www.w3.org/2001/12/soap-envelope", "getBundles");
    soapEnvelope.env = "http://www.w3.org/2001/12/soap-envelope";
    soapEnvelope.bodyOut = rpc;
    httpTransport.call("getBundles", soapEnvelope);

    remoteBC = new BundleContextImpl(this);
    remoteBC.start();
  }

  public void close() {
    if(remoteBC != null) {
      remoteBC.stop();
      remoteBC = null;
    }
    endpoint = null;
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
    String location = getBundleLocation(bid);
    if (location.startsWith(Util.LOC_PROT)) {
      try {
        String data = encodeFile(location.substring(Util.LOC_PROT.length()));
        doCall("updateBundle", new Object[] { new Long(bid), data });
      } catch (IOException e) {
        Activator.log.warn("Failed to encode bundle for update", e);
        throw new NestedRuntimeException("Failed to encode bundle for update", e);
      }
    } else {
      doCall("updateBundle", bid);
    }
    flushCache();
  }

  public void uninstallBundle(long bid) {
    doCall("uninstallBundle", bid);
    flushCache();
  }

  public long installBundle(String location) {
    try {
      if (location.startsWith(Util.FILE_PROT) && !"true".equals(System.getProperty("org.knopflerfish.soap.remotefw.client.sendlocalpaths", "false"))) {
        location = encodeFile(location);
      }
    } catch (IOException e) {
      Activator.log.warn("Failed to call installBundle(file:...): ", e);
      throw new NestedRuntimeException("Failed to call installBundle(file:...): ", e);
    }
    Object obj = doCall("installBundle", location);
    Long bid = new Long(obj == null ? "0" : obj.toString());
    flushCache();
    return bid.longValue();
  }

  private String encodeFile(String location) throws IOException {
    FileInputStream in = new FileInputStream(location.substring("file:".length()));
    int i;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    while ((i = in.read()) != -1) {
      out.write(i);
    }
    return "B64(" + location + "):" + Base64.encode(out.toByteArray());
  }


  public long    getBundle() {
    Object obj = doCall("getBundle");
    return (obj == null ? 0 : new Long(obj.toString()).longValue());
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
    return toString(doCall("getBundleLocation", bid));
  }

  public int  getBundleState(long bid) {
    Object obj = doCall("getBundleState", bid);
    return (obj == null ? 0 : new Integer(obj.toString()).intValue());
  }


  public long[]    getUsingBundles(long sid) {
    return toLongArray(doCall("getUsingBundles", sid));
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

  public Vector  getBundleManifest(long bid) {
    Object obj = doCall("getBundleManifest", bid);
    if (obj instanceof Vector) {
      return (Vector) obj;
    } else if (obj instanceof SoapObject) {
      return soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException("getBundleManifest returned something strange: " + obj + " (" + obj.getClass().getName() + ")");
    }
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

  public Vector  getServiceProperties(long sid) {
    Object obj = doCall("getServiceProperties", sid);
    if (obj instanceof Vector) {
      return (Vector) obj;
    } else if (obj instanceof SoapObject) {
      return soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException("getServiceProperties returned something strange: " + obj + " (" + obj.getClass().getName() + ")");
    }
  }

  private Vector soapObjectToVector(SoapObject sobj) {
    Vector vector = new Vector();
    for (int i = 0; i < sobj.getPropertyCount(); i++) {
      Object obj = sobj.getProperty(i);
      if (obj instanceof SoapObject) {
        obj = soapObjectToVector((SoapObject) obj);
      }
      vector.addElement(obj);
    }
    return vector;
  }

  public int  getStartLevel() {
    Object obj = doCall("getStartLevel");
    return (obj == null ? 0 : new Integer(obj.toString()).intValue());
  }


  public void setStartLevel(int level) {
    doCall("setStartLevel", level);
  }

  public void setBundleStartLevel(long bid, int level) {
    doCall("setBundleStartLevel", new Object[] { new Long(bid),
                                                 new Integer(level)});
  }

  public int  getBundleStartLevel(long bid) {
    Object obj = doCall("getBundleStartLevel", bid);
    return (obj == null ? 0 : new Integer(obj.toString()).intValue());
  }


  public void setInitialBundleStartLevel(int level){
    doCall("setInitialBundleStartLevel", level);
  }

  public int  getInitialBundleStartLevel() {
    Object obj = doCall("getInitialBundleStartLevel");
    return (obj == null ? 0 : new Integer(obj.toString()).intValue());
  }

  public boolean isBundlePersistentlyStarted(long bid) {
    return new Boolean(doCall("isBundlePersistentlyStarted", bid).toString()).booleanValue();
  }


  public Vector    getExportedPackage(String name) {
    Object obj = doCall("getExportedPackage", name);
    if (obj instanceof Vector) {
      return (Vector) obj;
    } else if (obj instanceof SoapObject) {
      return soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException("getExportedPackage returned something strange: " + obj + " (" + obj.getClass().getName() + ")");
    }
  }

  // bid==-1 represents getExportedPackages((Bundle)null)
  public Vector  getExportedPackages(long bid) {
    Object obj = doCall("getExportedPackages", bid);
    if (obj instanceof Vector) {
      return (Vector) obj;
    } else if (obj instanceof SoapObject) {
      return soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException("getExportedPackages returned something strange: " + obj + " (" + obj.getClass().getName() + ")");
    }
  }

  public Vector getExportedPackagesByPkgName(String pkgName) {
    Object obj = doCall("getExportedPackagesByPkgName", pkgName);
    if (obj instanceof Vector) {
      return (Vector) obj;
    } else if (obj instanceof SoapObject) {
      return soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException
        ("getExportedPackages returned something strange: " +obj
         +" (" + obj.getClass().getName() + ")");
    }
  }


  public void   refreshPackages(long[] bids) {
    if(bids == null) {
      doCall("refreshPackages", new long[0]);
    } else {
      doCall("refreshPackages", bids);
    }
  }

  public Vector getRequiredBundles(String symbolicName) {
    Object obj = doCall("getRequiredBundles",
                        null==symbolicName ? "00000" : symbolicName);
    if (obj instanceof Vector) {
      return (Vector) obj;
    } else if (obj instanceof SoapObject) {
      return soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException
        ("getRequiredBundles returned something strange: "
         + obj + " (" + obj.getClass().getName() + ")");
    }
  }

  public long[] getFragments(long bid) {
    return toLongArray(doCall("getFragments", bid));
  }


  public long[] getHosts(long bid) {
    return toLongArray(doCall("getHosts", bid));
  }

  public long[] getBundlesPA(String symbolicName, String versionRange) {
    return toLongArray(doCall("getBundlesPA",
                              new Object[]{ symbolicName, versionRange}));
  }

  public int getBundleType(long bid) {
    Object obj = doCall("getBundleType", bid);
    return (obj == null ? 0 : new Integer(obj.toString()).intValue());
  }

  public boolean resolveBundles(long[] bids) {
    Object obj = doCall("resolveBundles", null==bids ? new long[0] : bids);
    return null==obj ? false : new Boolean(obj.toString()).booleanValue();
  }


  public Vector getSystemProperties() {
    Object obj = doCall("getSystemProperties");
    if (obj instanceof Vector) {
      return (Vector) obj;
    } else if (obj instanceof SoapObject) {
      return soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException("getSystemProperties returned something strange: " + obj + " (" + obj.getClass().getName() + ")");
    }
  }

  public Vector getFullLog() {
    Object obj = doCall("getFullLog");
    return getLog(obj);
  }

  public Vector getLog() {
    Object obj = doCall("getLog");
    return getLog(obj);
  }

  private Vector getLog(Object obj) {
    Vector vector;
    if (obj instanceof Vector) {
      vector = (Vector) obj;
    } else if (obj instanceof SoapObject) {
      vector = soapObjectToVector((SoapObject) obj);
    } else {
      throw new RuntimeException("getSystemProperties returned something strange: " + obj + " (" + obj.getClass().getName() + ")");
    }
    Vector result = new Vector();
    for (int i=0; i<vector.size();) {
      Bundle bundle = new BundleImpl(this, new Long(vector.elementAt(i++).toString()).longValue());
      int level = new Integer(vector.elementAt(i++).toString()).intValue();
      //Throwable exception;
      String message = vector.elementAt(i++).toString();
      long time = new Long(vector.elementAt(i++).toString()).longValue();
      Throwable e = null;
      if (vector.elementAt(i).toString().length() > 1) {
        e = new LogThrowableImpl(vector.elementAt(i++).toString(), vector.elementAt(i++).toString(), vector.elementAt(i++).toString());
      } else {
        i += 3;
      }
      result.addElement(new LogEntryImpl(this, bundle, level, message, time, e));
    }
    return result;
  }

  public void createSession(String name) {
    doCall("createSession", name);
  }

  public void abortCommand() {
    doCall("abortCommand");
  }

  public void closeSession() {
    doCall("closeSession");
  }

  public void setEscapeChar(char ch) {
    doCall("setEscapeChar", new Character(ch));
  }

  public void setInterruptString(String str) {
    doCall("setInterruptString", str);
  }

  public String[] setAlias(String key, String[] val) {
    Vector vector = new Vector();
    vector.addElement(key);
    for (int i=0; i<val.length; i++) {
      vector.addElement(val[i]);
    }
    return (String[]) doCall("setAlias", vector);
  }

  public String runCommand(String command) {
    Object obj = doCall("runCommand", command);
    if (obj instanceof SoapObject) {
      return "";
    }
    return obj.toString();
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
    if(bDebug) System.out.println("doCall " + opName);
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
        SoapObject rpc = new SoapObject("http://www.w3.org/2001/12/soap-envelope", opName);
        for (int i=0; i<params.length; i++) {
          if(bDebug) System.out.println("doCall   param " + i + " = " + params[i]);
          rpc.addProperty("item"+i, params[i]);
        }
        soapEnvelope.bodyOut = rpc;

        if(bDebug) {
          System.out.println("doCall " + opName +
                             "(" + toDisplay(params) + ")");
        }
        httpTransport.call(opName, soapEnvelope);
        Object r = soapEnvelope.getResult();

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

  static Map vectorToMap(Vector vector) {
    Map result = new HashMap();
    for (Enumeration enumIsReserved = vector.elements(); enumIsReserved.hasMoreElements();) {
      Object key = enumIsReserved.nextElement();
      if (!enumIsReserved.hasMoreElements()) break;
      Object val = enumIsReserved.nextElement();
      if (key instanceof SoapPrimitive) key = key.toString();
      if (val instanceof SoapPrimitive) {
        String name = ((SoapPrimitive)val).getName();
        if ("long".equals(name)) {
          val = new Long(val.toString());
        } else if ("int".equals (name)) {
          val = new Integer(val.toString());
        } else if ("boolean".equals (name)) {
          val = new Boolean(val.toString());
        } else {
          val = val.toString();
        }
      }
      result.put(key, val);
    }
    return result;
  }

  static long[] toLongArray(Object obj) {
    if(obj == null) {
      return null;
    }
    long[] la;
    if (obj instanceof SoapObject) {
      SoapObject so = (SoapObject) obj;
      la = new long[so.getPropertyCount()];
      for(int i = 0; i < la.length; i++) {
        la[i] = new Long(so.getProperty(i).toString()).longValue();
      }
    } else {
      la = new long[Array.getLength(obj)];
      for(int i = 0; i < la.length; i++) {
        la[i] = ((Long)Array.get(obj, i)).longValue();
      }
    }

    return la;
  }

  static String toString(Object obj) {
    return (obj == null ? null : obj.toString());
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
