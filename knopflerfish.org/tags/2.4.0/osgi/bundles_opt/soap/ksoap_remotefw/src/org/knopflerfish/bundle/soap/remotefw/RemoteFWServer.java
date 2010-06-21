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
package org.knopflerfish.bundle.soap.remotefw;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.packageadmin.*;
import org.osgi.util.tracker.*;

import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;
import org.knopflerfish.util.Base64;

import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;

public class RemoteFWServer implements RemoteFW {

  ServiceRegistration reg = null;

  ServiceTracker slTracker;
  ServiceTracker pkgTracker;

  Thread reaper     = null;
  boolean bReap     = false;
  long    reapDelay = 60 * 1000;

  boolean  bDebug   = "true".equals(System.getProperty("org.knopflerfish.soap.remotefw.server.debug", "false"));

  public RemoteFWServer() {
  }

  public void startBundle(SoapPrimitive bid) {
    startBundle(Long.parseLong(bid.toString()));
  }
  public void startBundle(long bid) {
    try {
      Bundle b = Activator.bc.getBundle(bid);
      if (b != null) b.start();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to start bid=" + bid);
    }
  }

  public void stopBundle(SoapPrimitive bid) {
    stopBundle(Long.parseLong(bid.toString()));
  }
  public void stopBundle(long bid) {
    try {
      Bundle b = Activator.bc.getBundle(bid);
      if (b != null) b.stop();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to stop bid=" + bid);
    }
  }

  public void updateBundle(SoapPrimitive bid, SoapPrimitive data) {
    updateBundle(Long.parseLong(bid.toString()), data.toString());
  }
  public void updateBundle(long bid, String data) {
    try {
      if (data.startsWith(Util.B64_START)) {
        int end = data.indexOf(Util.B64_END);
        if (end == -1) {
          data = data.substring(Util.B64_START.length());
        } else {
          data = data.substring(end + Util.B64_END.length());
        }
        byte[] bytes = Base64.decode(data);
        Bundle b = Activator.bc.getBundle(bid);
      if (b != null) b.update(new ByteArrayInputStream(bytes));
      } else {
        updateBundle(bid);
      }
    } catch (BundleException e) {
      Activator.log.warn("Failed to update encoded bundle, bid=" + bid, e);
      throw new IllegalArgumentException("Failed to update encoded bundle, bid=" + bid);
    } catch (IOException e) {
      Activator.log.warn("Failed to update encoded bundle, bid=" + bid, e);
      throw new IllegalArgumentException("Failed to update encoded bundle");
    }
  }

  public void updateBundle(SoapPrimitive bid) {
    updateBundle(Long.parseLong(bid.toString()));
  }
  public void updateBundle(long bid) {
    try {
      Bundle b = Activator.bc.getBundle(bid);
      if (b != null) b.update();
    } catch (BundleException e) {
      Activator.log.warn("Failed to update bid=" + bid, e);
      throw new IllegalArgumentException("Failed to update bid=" + bid);
    }
  }

  public void uninstallBundle(SoapPrimitive bid) {
    uninstallBundle(Long.parseLong(bid.toString()));
  }
  public void uninstallBundle(long bid) {
    try {
      Bundle b = Activator.bc.getBundle(bid);
      if (b != null) b.uninstall();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to uninstall bid=" + bid);
    }
  }

  public void installBundle(SoapPrimitive bid) {
    installBundle(bid.toString());
  }
  public long installBundle(String location) {
    try {
      Bundle b;
      if (location.startsWith(Util.B64_START)) {
        int end = location.indexOf(Util.B64_END);
        String data;
        if (end == -1) {
          data = location.substring(Util.B64_START.length());
          location = Util.LOC_PROT + System.currentTimeMillis();
        } else {
          data = location.substring(end + Util.B64_END.length());
          location = Util.LOC_PROT + location.substring(Util.B64_START.length(), end);
        }
        byte[] bytes = Base64.decode(data);
        b = Activator.bc.installBundle(location, new ByteArrayInputStream(bytes));
      } else {
        b = Activator.bc.installBundle(location);
      }
      return b.getBundleId();
    } catch (BundleException e) {
      Activator.log.warn("Failed to install location=" + location, e);
      throw new IllegalArgumentException("Failed to install location=" + location);
    } catch (IOException e) {
      Activator.log.warn("Failed to install encoded bundle", e);
      throw new IllegalArgumentException("Failed to install encoded bundle");
    }
  }

  public long getBundle() {
    return Activator.bc.getBundle().getBundleId();
  }

  public long[] getBundles() {
    Bundle[] bl = Activator.bc.getBundles();
    long[] bids = new long[bl.length];

    for(int i = 0; i < bl.length; i++) {
      bids[i] = bl[i].getBundleId();
    }
    return bids;
  }

  public String getBundleContextProperty(SoapPrimitive key) {
    return getBundleContextProperty(key.toString());
  }
  public String getBundleContextProperty(String key) {
    String v = Activator.bc.getProperty(key);
    return v == null ? NULL_STR : v;
  }
  public String getBundleLocation(SoapPrimitive bid) {
    return getBundleLocation(Long.parseLong(bid.toString()));
  }
  public String getBundleLocation(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return (b == null ? null : b.getLocation());
  }

  public int  getBundleState(SoapPrimitive bid) {
    return getBundleState(Long.parseLong(bid.toString()));
  }
  public int  getBundleState(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return (b == null ? Bundle.UNINSTALLED : b.getState());
  }


  public long[] getServicesInUse(SoapPrimitive bid) {
    return getServicesInUse(Long.parseLong(bid.toString()));
  }
  public long[] getServicesInUse(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return Util.referencesToLong(b == null ? null : b.getServicesInUse());
  }

  public long[] getUsingBundles(SoapPrimitive sid) {
    return getUsingBundles(Long.parseLong(sid.toString()));
  }
  public long[] getUsingBundles(long sid) {
    try {
      ServiceReference[] srl
        = Activator.bc.getServiceReferences(null, "(service.id=" + sid + ")");
      if (srl.length == 0) {
        return null;
      }
      Bundle[] bundles = srl[0].getUsingBundles();
      if (bundles == null) {
        return null;
      }
      long[] bids = new long[bundles.length];
      for (int i=0; i<bundles.length; i++) {
        bids[i] = bundles[i].getBundleId();
      }
      return bids;
    } catch (InvalidSyntaxException e) {
      Activator.log.warn("Failed to get using bundles", e);
      return null;
    }
  }

  public long[] getRegisteredServices(SoapPrimitive bid) {
    return getRegisteredServices(Long.parseLong(bid.toString()));
  }
  public long[] getRegisteredServices(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    if (b == null) return new long[0];
    return Util.referencesToLong(b.getRegisteredServices());
  }

  public long[] getServiceReferences(SoapPrimitive filter) {
    return getServiceReferences(filter.toString());
  }
  public long[] getServiceReferences(String filter) {
    return getServiceReferences2(null, filter);
  }


  public long[] getServiceReferences2(SoapPrimitive clazz,
                                      SoapPrimitive filter) {
    return getServiceReferences2(clazz.toString(), filter.toString());
  }
  public long[] getServiceReferences2(String clazz, String filter) {
    try {
      if(NULL_STR.equals(clazz)) {
        clazz = null;
      }
      if(NULL_STR.equals(filter)) {
        filter = null;
      }
      ServiceReference[] srl = Activator.bc.getServiceReferences(clazz, filter);

      if(srl == null) {
        return new long[0];
      }
      long[] r = new long[srl.length * 2];
      int n = 0;
      for(int i = 0; i < srl.length; i++) {
        r[n * 2]      = ((Long)srl[i].getProperty(Constants.SERVICE_ID)).longValue();
        r[n * 2 + 1]  = srl[i].getBundle().getBundleId();

        n++;
      }
      return r;
    } catch (Exception e) {
      throw new RuntimeException("Failed to get services from filter=" + filter + ",  " + e);
    }
  }

  public Vector getBundleManifest(SoapPrimitive bundleId) {
    return getBundleManifest(Long.parseLong(bundleId.toString()));
  }
  public Vector getBundleManifest(long bundleId) {
    Bundle b = Activator.bc.getBundle(bundleId);

    //Map result = new HashMap();
    Vector result = new Vector();

    if (b != null) {
      Dictionary d = b.getHeaders();

      int i = 0;
      for(Enumeration e = d.keys(); e.hasMoreElements(); ) {
        String key = (String)e.nextElement();
        String val = (String)d.get(key);

        if (!"Application-Icon".equals(key)) {
          result.addElement(key);
          result.addElement(val);
        }

        i += 2;
      }
    }

    return result;
  }


  public long[] getServices() {
    return null;
  }

  public long[] getFrameworkEvents() {
    synchronized(frameworkEvents) {
      try {

        long[] r = new long[frameworkEvents.size() * 2];
        if (Activator.log.doDebug()) Activator.log.debug("server: getFrameworkEvents size=" + r.length / 2);
        if(bDebug) {
          System.out.println("server: getFrameworkEvents size=" + r.length / 2);
        }
        int i = 0;

        for(Iterator it = frameworkEvents.iterator(); it.hasNext();) {
          FrameworkEvent ev = (FrameworkEvent)it.next();
          Bundle b = ev.getBundle();
          long bid = -1;
          if(b == null) {
            if (Activator.log.doDebug()) Activator.log.debug("fw event: " + ev + " - no bundle");
            if(bDebug) {
              System.out.println("fw event: " + ev + " - no bundle");
            }
          } else {
            bid = b.getBundleId();
          }
          r[i * 2]     = bid;
          r[i * 2 + 1] = ev.getType();
          i++;
        }

        frameworkEvents.clear();
        if (Activator.log.doDebug()) Activator.log.debug("server: getFrameworkEvents -> " + r);
        if(bDebug) {
          System.out.println("server: getFrameworkEvents -> " + r);
        }
        return r;
      } catch (Exception e) {
        if (Activator.log.doDebug()) Activator.log.debug("Exception during getFrameworkEvents", e);
        if(bDebug) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  public long[]    getBundleEvents() {
    synchronized(bundleEvents) {
      long[] r = new long[bundleEvents.size() * 2];
      int i = 0;

      for(Iterator it = bundleEvents.iterator(); it.hasNext();) {
        BundleEvent ev = (BundleEvent)it.next();
        r[i * 2]     = ev.getBundle().getBundleId();
        r[i * 2 + 1] = ev.getType();
        i++;
      }
      bundleEvents.clear();
      return r;
    }
  }

  public long[]    getServiceEvents() {
    synchronized(serviceEvents) {
      long[] r = new long[serviceEvents.size() * 2];
      int i = 0;

      for(Iterator it = serviceEvents.iterator(); it.hasNext();) {
        ServiceEvent ev = (ServiceEvent)it.next();
        r[i * 2] = ((Long)ev.getServiceReference().getProperty(Constants.SERVICE_ID)).longValue();
        r[i * 2 + 1] = ev.getType();
        i++;
      }

      serviceEvents.clear();
      return r;
    }
  }

  public Vector  getServiceProperties(SoapPrimitive sid) {
    return getServiceProperties(Long.parseLong(sid.toString()));
  }
  public Vector  getServiceProperties(long sid) {
    try {
      ServiceReference[] srl =
        Activator.bc.getServiceReferences(null, "(service.id=" + sid + ")");

      String[] keys   = srl[0].getPropertyKeys();

      //Map result = new HashMap();
      Vector result = new Vector();

      for(int i1 = 0; i1 < keys.length; i1++) {
        String key = keys[i1];
        Object val = srl[0].getProperty(keys[i1]);
        if (val instanceof Vector) {
          for (int i2=0; i2<((Vector) val).size(); i2++) {
            if (((Vector) val).elementAt(i2) instanceof Object[]) {
              ((Vector) val).setElementAt(arrayToVector((Object[]) ((Vector) val).elementAt(i2)), i2);
            }
          }
        } else if (val instanceof Object[]) {
          val = arrayToVector((Object[]) val);
        }
        //  Object strVal = Util.encodeAsString(val);
        result.addElement(key);
        result.addElement(val);
        //result.put(key, val);
      }

      return result;

    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get service properties " +
           "for service.id=" + sid + ", " + e);
    }
  }

  private Vector arrayToVector(Object[] vala) {
    Vector valv = new Vector();
    for (int i=0; i<vala.length; i++) {
      if (vala[i] instanceof Object[]) {
        valv.addElement(arrayToVector((Object[]) vala[i]));
      } else {
        valv.addElement(vala[i]);
      }
    }
    return valv;
  }

  public int  getStartLevel() {
    return ((StartLevel)slTracker.getService()).getStartLevel();
  }


  public void setStartLevel(int level) {
    ((StartLevel)slTracker.getService()).setStartLevel(level);
  }

  public void setBundleStartLevel(long bid, int level) {
    Bundle b = Activator.bc.getBundle(bid);
    ((StartLevel)slTracker.getService()).setBundleStartLevel(b, level);
  }

  public int  getBundleStartLevel(SoapPrimitive bid) {
    return getBundleStartLevel(Long.parseLong(bid.toString()));
  }
  public int  getBundleStartLevel(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return (b == null ? 0 : ((StartLevel)slTracker.getService()).getBundleStartLevel(b));
  }


  public void setInitialBundleStartLevel(int level){
    ((StartLevel)slTracker.getService()).setInitialBundleStartLevel(level);
  }

  public int  getInitialBundleStartLevel() {
    return ((StartLevel)slTracker.getService()).getInitialBundleStartLevel();
  }

  public boolean isBundlePersistentlyStarted(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return ((StartLevel)slTracker.getService()).isBundlePersistentlyStarted(b);
  }

  //TODO!
  public Vector getExportedPackage(String name) {
    //Map map = new HashMap();
    Vector result = new Vector();
    ExportedPackage pkg = ((PackageAdmin)pkgTracker.getService()).getExportedPackage(name);

    putExportPackage(result, pkg);
    return result;
  }

  public Vector  getExportedPackages(SoapPrimitive sid) {
    return getExportedPackages(Long.parseLong(sid.toString()));
  }
  // bid==-1 represents a call to getExportedPackages((Bundle)null)
  public Vector  getExportedPackages(long bid) {
    Bundle b = -1==bid ? (Bundle)null : Activator.bc.getBundle(bid);
    ExportedPackage[] pkgs = ((PackageAdmin)pkgTracker.getService()).getExportedPackages(b);

    if(pkgs == null) {
      return new Vector();
    }

    Vector maps = new Vector(pkgs.length);
    for(int i = 0; i < pkgs.length; i++) {
      Vector map = new Vector();
      maps.addElement(map);
      putExportPackage(map, pkgs[i]);
    }
    return maps;
  }

  public Vector  getExportedPackagesByPkgName(SoapPrimitive pkgName) {
    return getExportedPackagesByPkgName(pkgName.toString());
  }
  public Vector  getExportedPackagesByPkgName(String pkgName) {
    ExportedPackage[] pkgs = ((PackageAdmin)pkgTracker.getService())
      .getExportedPackages(pkgName);

    if(pkgs == null) {
      return new Vector();
    }

    Vector maps = new Vector(pkgs.length);
    for(int i = 0; i < pkgs.length; i++) {
      Vector map = new Vector();
      maps.addElement(map);
      putExportPackage(map, pkgs[i]);
    }
    return maps;
  }

  void putExportPackage(Vector result, ExportedPackage pkg) {
    if(pkg != null) {
      long[] bids;
      Bundle[] bl = pkg.getImportingBundles();
      if(bl == null) {
        bids = new long[0];
      } else {
        bids = new long[bl.length];
        for(int i = 0; i < bids.length; i++) {
          bids[i] = (bl[i].getBundleId());
        }
      }
      result.addElement("getExportingBundle");
      result.addElement(new Long(pkg.getExportingBundle().getBundleId()));
      result.addElement("getImportingBundles");
      result.addElement(bids);
      result.addElement("getName");
      result.addElement(pkg.getName());
      result.addElement("getSpecificationVersion");
      result.addElement(pkg.getSpecificationVersion());
      result.addElement("isRemovalPending");
      result.addElement(pkg.isRemovalPending() ? Boolean.TRUE : Boolean.FALSE);
    }
  }

  public void   refreshPackages(SoapObject bids) {
    refreshPackages(new long[0]);
  }
  public void   refreshPackages(long[] bids) {
    if(bids == null || bids.length == 0) {
      ((PackageAdmin)pkgTracker.getService()).refreshPackages(null);
      bids = null;
    } else {
      Bundle[] bl = new Bundle[bids.length];
      for(int i = 0; i < bids.length; i++) {
        bl[i] = Activator.bc.getBundle(bids[i]);
      }
      ((PackageAdmin)pkgTracker.getService()).refreshPackages(bl);
    }
  }

  public Vector  getRequiredBundles(SoapPrimitive sid) {
    return getRequiredBundles(sid.toString());
  }
  public Vector getRequiredBundles(String sn) {
    final String symbolicName = "00000".equals(sn) ? null: sn;
    RequiredBundle[] rbs = ((PackageAdmin)pkgTracker.getService())
      .getRequiredBundles(symbolicName);

    if(rbs == null) {
      return new Vector();
    }

    Vector maps = new Vector(rbs.length);
    for(int i = 0; i < rbs.length; i++) {
      Vector map = new Vector();
      maps.addElement(map);
      putRequiredBundle(map, rbs[i]);
    }
    return maps;
  }

  void putRequiredBundle(Vector result, RequiredBundle rb) {
    if (null==rb) return;

    long[] bids;
    Bundle[] bl = rb.getRequiringBundles();
    if(bl == null) {
      bids = new long[0];
    } else {
      bids = new long[bl.length];
      for(int i = 0; i < bids.length; i++) {
        bids[i] = (bl[i].getBundleId());
      }
    }
    result.addElement("getBundle");
    result.addElement(new Long(rb.getBundle().getBundleId()));
    result.addElement("getRequiringBundles");
    result.addElement(bids);
    result.addElement("getSymbolicName");
    result.addElement(rb.getSymbolicName());
    result.addElement("getVersion");
    result.addElement(rb.getVersion().toString());
    result.addElement("isRemovalPending");
    result.addElement(rb.isRemovalPending() ? Boolean.TRUE : Boolean.FALSE);
  }


  public long[] getFragments(SoapPrimitive bid) {
    return getFragments(Long.parseLong(bid.toString()));
  }
  public long[] getFragments(long bid) {
    Bundle bundle = -1==bid ? null : Activator.bc.getBundle(bid);
    Bundle[] bundles = ((PackageAdmin)pkgTracker.getService())
      .getFragments(bundle);

    if(bundles == null) {
      return null;
    }

    long[] bids = new long[bundles.length];
    for (int i=0; i<bundles.length; i++) {
      bids[i] = bundles[i].getBundleId();
    }
    return bids;
  }

  public long[] getHosts(SoapPrimitive bid) {
    return getHosts(Long.parseLong(bid.toString()));
  }
  public long[] getHosts(long bid) {
    Bundle bundle = -1==bid ? null : Activator.bc.getBundle(bid);
    Bundle[] bundles = ((PackageAdmin)pkgTracker.getService())
      .getHosts(bundle);

    if(bundles == null) {
      return null;
    }

    long[] bids = new long[bundles.length];
    for (int i=0; i<bundles.length; i++) {
      bids[i] = bundles[i].getBundleId();
    }
    return bids;
  }

  public long[] getBundlesPA(SoapPrimitive symbolicName,
                           SoapPrimitive versionRange)
  {
    return getBundlesPA(symbolicName.toString(), versionRange.toString());
  }
  public long[] getBundlesPA(String symbolicName, String versionRange) {
    Bundle[] bundles = ((PackageAdmin)pkgTracker.getService())
      .getBundles(symbolicName, versionRange);

    if(bundles == null) {
      return null;
    }

    long[] bids = new long[bundles.length];
    for (int i=0; i<bundles.length; i++) {
      bids[i] = bundles[i].getBundleId();
    }
    return bids;
  }

  public int getBundleType(SoapPrimitive bid) {
    return getBundleType(Long.parseLong(bid.toString()));
  }
  public int getBundleType(long bid) {
    Bundle bundle = -1==bid ? null : Activator.bc.getBundle(bid);
    int res = ((PackageAdmin)pkgTracker.getService()).getBundleType(bundle);

    return res;
  }

  public boolean resolveBundles(SoapObject so) {
    final long[] bids = new long[null==so ? 0 : so.getPropertyCount()];

    for(int i = 0; i < bids.length; i++) {
      bids[i] = new Long(so.getProperty(i).toString()).longValue();
    }
    return resolveBundles(bids);
  }
  public boolean resolveBundles(long[] bids) {
    Bundle[] bl = null;

    if(bids != null && bids.length > 0) {
      bl = new Bundle[bids.length];
      for(int i = 0; i < bids.length; i++) {
        bl[i] = Activator.bc.getBundle(bids[i]);
      }
    }
    return ((PackageAdmin)pkgTracker.getService()).resolveBundles(bl);
  }



  public Vector    getSystemProperties() {
    //Map map = new HashMap();
    Vector result = new Vector();
    Properties props = System.getProperties();

    for(Enumeration e = props.keys(); e.hasMoreElements();) {
      String key = (String)e.nextElement();
      String val = (String)props.get(key);
      //map.put(key, val);
      result.addElement(key);
      result.addElement(val);
    }
    return result;
  }


  public Vector getLog() {
    synchronized (logEvents) {
      return getLog(logEvents);
    }
  }

  public Vector getFullLog() {
    String filter = "(objectclass=" + LogReaderService.class.getName() + ")";
    ArrayList logEvents = new ArrayList();
    try {
      ServiceReference [] srl = Activator.bc.getServiceReferences(null, filter);
      for(int i = 0; srl != null && i < srl.length; i++) {
        LogReaderService lr = (LogReaderService) Activator.bc.getService(srl[i]);
        for(Enumeration e2 = lr.getLog(); e2.hasMoreElements(); ) {
          logEvents.add((LogEntry)e2.nextElement());
        }
      }
    } catch (Exception e) {}
    return getLog(logEvents);
  }

  private Vector getLog(List logEvents) {
    Vector result = new Vector();

    try {
      for(Iterator iter = logEvents.iterator(); iter.hasNext(); ) {
        LogEntry entry = (LogEntry) iter.next();
        result.addElement(new Long(entry.getBundle().getBundleId()));
        result.addElement(new Integer(entry.getLevel()));
        result.addElement(entry.getMessage());
        result.addElement(new Long(entry.getTime()));
        Throwable e = entry.getException();
        if (e == null) {
          result.addElement("-");
          result.addElement("-");
          result.addElement("-");
        } else {
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          result.addElement(sw.toString());
          result.addElement(e.toString());
          result.addElement(e.getMessage() == null ? "-" : e.getMessage());
        }
      }
      logEvents.clear();
    } catch (Throwable ignore) {
      //TODO
    }
    return result;
  }

  private Session consoleSession;
  private PrintWriter toConsole;
  private Reader fromConsole;

  public void createSession(SoapPrimitive name) {
    createSession(name.toString());
  }
  public void createSession(String name) {
    try {
      ServiceReference srl = Activator.bc.getServiceReference(ConsoleService.class.getName());
      ConsoleService console = (ConsoleService) Activator.bc.getService(srl);
      PipedWriter pipeToConsole = new PipedWriter();
      Reader consoleIn = new PipedReader(pipeToConsole);
      toConsole = new PrintWriter(pipeToConsole);

      PipedReader pipeFromConsole = new PipedReader();
      PrintWriter consoleOut = new PrintWriter(new PipedWriter(pipeFromConsole));
      fromConsole = pipeFromConsole;
      consoleSession = console.runSession(name, consoleIn, consoleOut);
    } catch (Exception e) {}
  }

  public void abortCommand() {
    consoleSession.abortCommand();
  }

  public void closeSession() {
    try {
      if (consoleSession != null) {
        consoleSession.close();
      }
    } catch (IllegalStateException ignore) {
      // close can call abortCommand, which will throw
      // IllegalStateException: Session is closed
    }
  }

  public void setEscapeChar(SoapPrimitive ch) {
    setEscapeChar(ch.toString().charAt(0));
  }
  public void setEscapeChar(char ch) {
    consoleSession.setEscapeChar(ch);
  }

  public void setInterruptString(SoapPrimitive str) {
    setInterruptString(str.toString());
  }
  public void setInterruptString(String str) {
    consoleSession.setInterruptString(str);
  }

  public String[] setAlias(String key, String[] val) {
    try {
      ServiceReference srl = Activator.bc.getServiceReference(ConsoleService.class.getName());
      ConsoleService console = (ConsoleService) Activator.bc.getService(srl);
      return console.setAlias(key, val);
    } catch (Exception e) {}
    return null;
  }

  public String runCommand(SoapObject command) {
    return "";
  }
  public String runCommand(SoapPrimitive command) {
    return runCommand(command.toString());
  }
  public String runCommand(String command) {
    // Old session is screwed up?
    // TODO: Fix this.
    closeSession();
    createSession(consoleSession == null ? "default_remote_session" : consoleSession.getName());

    toConsole.println(command);
    StringBuffer buf = new StringBuffer();
    try {
      int i;
      boolean first = true;
      boolean eatSpace = false;
      for (int unused=0; unused++<10;) {
        if (fromConsole.ready()) break;
        try {
          Thread.sleep(100);
        } catch (Exception ignore) {}
      }
      while (fromConsole.ready()) {
        i = fromConsole.read();
        if (i == -1) {
          break;
        }
        char ch = (char) i;
        if (first && ch == '>') {
          eatSpace = true;
        } else {
          if (!eatSpace || ch != ' ') {
            buf.append(ch);
          }
          eatSpace = false;
        }
        first = false;
      }
    } catch (Exception e) {
      Activator.log.warn("Exception during runCommand", e);
      e.printStackTrace();
      buf.append(e.toString());
    }
    return buf.toString();
  }


  public void start() {
    if(reg == null) {

      slTracker = new ServiceTracker(Activator.bc,
                                     StartLevel.class.getName(),
                                     null);
      slTracker.open();

      pkgTracker = new ServiceTracker(Activator.bc,
                                      PackageAdmin.class.getName(),
                                      null);
      pkgTracker.open();

      Hashtable props = new Hashtable();

      props.put("SOAP.service.name", "OSGiFramework");

      reg = Activator.bc.registerService(RemoteFW.class.getName(),
                                         this,
                                         props);

      String filter = "(objectclass=" + LogReaderService.class.getName() + ")";
      try {
        ServiceReference [] srl = Activator.bc.getServiceReferences(null, filter);
        for(int i = 0; srl != null && i < srl.length; i++) {
          LogReaderService lr = (LogReaderService) Activator.bc.getService(srl[i]);
          lr.addLogListener(new LogListener() {
              public void logged(LogEntry event) {
                synchronized(logEvents) {
                  logEvents.add(event);
                }
              }
            });
          for(Enumeration e2 = lr.getLog(); e2.hasMoreElements(); ) {
            logEvents.add((LogEntry)e2.nextElement());
          }
        }
      } catch (Exception e) {}
      Activator.bc.addBundleListener(new BundleListener() {
          public void bundleChanged(BundleEvent event) {
            synchronized(bundleEvents) {
              bundleEvents.add(event);
            }
          }
        });
      Activator.bc.addServiceListener(new ServiceListener() {
          public void serviceChanged(ServiceEvent event) {
            synchronized(serviceEvents) {
              serviceEvents.add(event);
            }
          }
        });
      Activator.bc.addFrameworkListener(new FrameworkListener() {
          public void frameworkEvent(FrameworkEvent ev ) {
            synchronized(frameworkEvents) {
              int type = ev.getType();
              Bundle b = ev.getBundle();
              if(b == null) {
                Object obj = ev.getSource();
                if (Activator.log.doDebug()) Activator.log.debug("obj=" + obj + " source class=" + (obj == null ? "null" : obj.getClass().getName()));
                if(bDebug) {
                  System.out.println("obj=" + obj);
                  if(obj != null) {
                    System.out.println("source class=" + obj.getClass().getName());
                  }
                }
                if(obj != null && (obj instanceof Bundle)) {
                  b = (Bundle)obj;
                }
              }
              if (Activator.log.doDebug()) Activator.log.debug("server: add fw event: " + ev + ", type=" + type + ", bundle=" + b);
              if(bDebug) {
                System.out.println("server: add fw event: " + ev + ", type=" + type + ", bundle=" + b);
              }
              if(b != null) {
                frameworkEvents.add(new FrameworkEvent(type,
                                                       b,
                                                       null));
              }
            }
          }
        });

      reaper = new Thread() {
          public void run() {
            while(bReap) {
              try {
                reapEvents();
                Thread.sleep(reapDelay);
              } catch (Exception e) {

              }
            }
          }
        };

      bReap = true;
      reaper.start();
    }
  }

  void reapEvents() {
    trimList(serviceEvents,   MAX_SERVICE_EVENTS);
    trimList(bundleEvents,    MAX_BUNDLE_EVENTS);
    trimList(frameworkEvents, MAX_FRAMEWORK_EVENTS);
  }


  void trimList(List list, int max) {
    synchronized(list) {
      while(list.size() > max) {
        list.remove(0);
      }
    }
  }

  public void stop() {
    if(reaper != null) {
      bReap = false;
      try {
        reaper.wait(1000);
      } catch (Exception ignored) {
      }
      reaper = null;
    }
    if(reg != null) {
      reg.unregister();
      reg = null;

      slTracker.close();
    }
  }

  static int MAX_SERVICE_EVENTS   = 1000;
  static int MAX_BUNDLE_EVENTS    = 1000;
  static int MAX_FRAMEWORK_EVENTS = 1000;

  List bundleEvents    = new ArrayList();
  List serviceEvents   = new ArrayList();
  List frameworkEvents = new ArrayList();
  List logEvents       = new ArrayList();
}
