package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;
import org.osgi.service.packageadmin.*;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

import org.knopflerfish.bundle.soap.remotefw.*;

public class BundleContextImpl implements BundleContext {
  RemoteFWClient fw;

  boolean  bDebug   = "true".equals(System.getProperty("org.knopflerfish.soap.remotefw.client.debug", "false"));

  Thread  runner = null;
  boolean bRun   = false;
  long    delay  = 3 * 1000;

  StartLevelImpl startLevel;
  PackageAdminImpl pkgAdmin;

  BundleContextImpl(RemoteFWClient fw) {
    this.fw  = fw;
    startLevel = new StartLevelImpl(fw);
    pkgAdmin   = new PackageAdminImpl(fw);

    try {
      delay = Long.parseLong(System.getProperty("org.knopflerfish.soap.remotefw.client.eventinterval", Long.toString(delay)));
    } catch (Exception e) {
    }
  }

  void start() {
    if(runner == null) {
      runner = new Thread() {
	  public void run() {
	    while(bRun) {
	      try {
		if(!bInEvent) {
		  doEvents();
		}
		if(bDebug) {
		  System.out.println("sleep " + delay);
		}

		Thread.sleep(delay);
	      } catch (Exception e) {
		//
	      }
	    }
	  }
	};
      bRun = true;
      runner.start();
    }
  }

  void stop() {
    if(runner != null) {
      bRun = false;
      try {
	runner.wait(delay * 2);
      } catch (Exception ignored) {
      }
      runner = null;
    }
  }

  Object eventLock = new Object();

  int eventCount = 0;

  boolean bInEvent = false;

  void doEvents() {
    synchronized(eventLock) {
      try {
	if(bInEvent) {
	  Exception e = new RuntimeException("already in doEvents");
	  e.printStackTrace();
	  return;
	}
	bInEvent = true;
	eventCount++;
	if(bDebug) {
	  System.out.println("doAllEvents " + eventCount);
	}
	doBundleEvents();
	doServiceEvents();
	doFrameworkEvents();
	
	if(bDebug) {
	  System.out.println("done doAllEvents " + eventCount);
	}
      } finally {
	bInEvent = false;
      }
    }
  }

  void doBundleEvents() {
    synchronized(eventLock) {
      long[] evs = fw.getBundleEvents();
      if(bDebug) {
	System.out.println("doBundleEvents " + evs.length);
      }
      for(int i = 0; i < evs.length / 2; i++) {
	long bid = evs[i * 2];
	Bundle      b  = getBundle(bid);
	if(b == null) {
	  System.out.println("*** No bundle bid=" + bid);
	} else {
	  BundleEvent ev = new BundleEvent((int)evs[i * 2 +1], b);
	  
	  sendBundleEvent(ev);
	}
      }
    }
  }

  void doFrameworkEvents() {
    synchronized(eventLock) {
      
      long[] evs = fw.getFrameworkEvents();
      if(bDebug) {
	System.out.println("doFrameworkEvents " + evs.length / 2);
      }
      for(int i = 0; i < evs.length / 2; i++) {
	FrameworkEvent ev = new FrameworkEvent((int)evs[i * 2 + 1],
					       getBundle(evs[i*2]),
					       null);
	
	sendFrameworkEvent(ev);
      }
      
      if(bDebug) {
	System.out.println("*** done doFrameworkEvents " + evs.length);
      }
    }
  }

  void doServiceEvents() {
    synchronized(eventLock) {
      long[] evs = fw.getServiceEvents();
      if(bDebug) {
	System.out.println("doServiceEvents " + evs.length);
      }
      for(int i = 0; i < evs.length / 2; i++) {
	long             sid  = evs[i * 2];
	int              type = (int)evs[i * 2 + 1];
	ServiceReference sr   = getServiceReference(sid);
	
	if(sr == null) {
	  System.err.println("*** no sid=" + sid);
	} else {
	  ServiceEvent     ev   = new ServiceEvent(type, sr);
	  
	  sendServiceEvent(ev);
	}
      }
    }
  }

  void sendBundleEvent(BundleEvent ev) {
    synchronized(bundleListeners) {
      for(Iterator it = bundleListeners.iterator(); it.hasNext();) {
	BundleListener l = (BundleListener)it.next();
	try {
	  l.bundleChanged(ev);
	} catch (Exception e) {
	  e.printStackTrace();
	}
      }
    }
  }

  void sendFrameworkEvent(FrameworkEvent ev) {
    synchronized(frameworkListeners) {
      for(Iterator it = frameworkListeners.iterator(); it.hasNext();) {
	FrameworkListener l = (FrameworkListener)it.next();
	try {
	  l.frameworkEvent(ev);
	} catch (Exception e) {
	  e.printStackTrace();
	}
      }
    }
  }

  void sendServiceEvent(ServiceEvent ev) {
    synchronized(serviceListeners) {
      //      Hashtable props = new Hashtable();
      ServiceReferenceImpl sr = (ServiceReferenceImpl)ev.getServiceReference();
      Hashtable props = sr.props;

      for(Iterator it = serviceListeners.keySet().iterator(); it.hasNext();) {
	ServiceListener l = (ServiceListener)it.next();
	Filter filter     = (Filter)serviceListeners.get(l);

	if(filter.match(props)) {
	  try {
	    l.serviceChanged(ev);
	  } catch (Exception e) {
	    e.printStackTrace();
	  }
	}
      }
    }
  }

  Set bundleListeners    = new HashSet();
  Set frameworkListeners = new HashSet();
  Map serviceListeners   = new HashMap();

  public void addBundleListener(BundleListener listener) { 
    synchronized(bundleListeners) {
      bundleListeners.add(listener);
    }
  }

  public void addFrameworkListener(FrameworkListener listener) { 
    synchronized(frameworkListeners) {
      frameworkListeners.add(listener);
    }
  }

  public void addServiceListener(ServiceListener listener) { 
    try {
      addServiceListener(listener, "(objectclass=*)");
    } catch (Exception e) {
    }
  }

  public void addServiceListener(ServiceListener listener, String filter) 
  throws InvalidSyntaxException { 
    synchronized(serviceListeners) {
      if(filter == null || "".equals(filter)) {
	filter = "(objectclass=*)";
      }
      Filter f = createFilter(filter);
      serviceListeners.put(listener, f);
    }
  }

  public Filter createFilter(String filter) throws InvalidSyntaxException { 
    return Activator.bc.createFilter(filter);
  }

  // Long (bid) -> BundleImpl
  Map bundleMap = new HashMap();

  public Bundle getBundle() { 
    return getBundle(fw.getBundle());
  }

  public Bundle getBundle(long id) { 
    synchronized(bundleMap) {
      Long bid = new Long(id);
      BundleImpl b = (BundleImpl)bundleMap.get(bid);
      if(b == null) {
	b = new BundleImpl(fw, id);
	bundleMap.put(bid, b);
      }
      return b;
    }
  }


  public Bundle[] getBundles() { 
    synchronized(bundleMap) {
      long[] bids = fw.getBundles();

      Bundle[] bl = new Bundle[bids.length];
      for(int i = 0; i < bids.length; i++) {
	bl[i] = getBundle(bids[i]);
      }

      return bl;
    }
  }

  public File getDataFile(String filename) { 
    throw new RuntimeException("Not implemented");
  }

  public String getProperty(String key) { 
    return fw.getBundleContextProperty(key);
  }

  // Long -> ServerReferenceImpl
  Map serviceMap = new HashMap();

  ServiceReference getServiceReference(long sid) {
    Long SID = new Long(sid);
    ServiceReferenceImpl sr = (ServiceReferenceImpl)serviceMap.get(SID);
    if(sr != null) {
      return sr;
    }

    long[] srl = fw.getServiceReferences("(service.id=" + sid + ")");
    if(srl.length == 2) {
      sr = new ServiceReferenceImpl((BundleImpl)getBundle(srl[1]), srl[0]);
      serviceMap.put(SID, sr);
      return sr;
    }
    throw new IllegalArgumentException("No service id=" + sid + ", length=" + srl.length);
  }

  public Object getService(ServiceReference sr) { 
    String[] clazzes = (String[])sr.getProperty("objectclass");

    if(sr instanceof ServiceReferenceImpl) {
      if(inArray(clazzes, StartLevel.class.getName())) {
	return startLevel;
      }
      if(inArray(clazzes, PackageAdmin.class.getName())) {
	return pkgAdmin;
      }
      return null;
    } else {
      throw new IllegalArgumentException("Bad ServiceReference passed: " + sr);
    }
  }

  public ServiceReference getServiceReference(String clazz) { 
    ServiceReference[] srl = getServiceReferences(clazz, null);
    if(srl != null && srl.length > 0) {
      return srl[0];
    }
    return null;
  }

  public ServiceReference[] getServiceReferences(String clazz, String filter)
  { 
    long[] sids = fw.getServiceReferences2(clazz, filter);

    if(sids == null || sids.length == 0) {
      return null;
    }

    ServiceReference[] srl = new ServiceReference[sids.length / 2];


    for(int i = 0; i < srl.length; i++) {
      srl[i] = getServiceReference(sids[i * 2]);
    }
    return srl;
  }

  public Bundle installBundle(String location) { 
    return new BundleImpl(fw, fw.installBundle(location));
  }


  public Bundle installBundle(String location, InputStream in) { 
    throw new RuntimeException("Not implemented");
  }

  public ServiceRegistration registerService(String[] clazzes, 
					     Object service, 
					     Dictionary properties) { 
    throw new RuntimeException("registerService not implemented: service=" + service + ", classes=" + RemoteFWClient.toDisplay(clazzes));
   }

  
  public ServiceRegistration registerService(String clazz, 
					     Object service, 
					     Dictionary properties) { 
    return registerService(new String[] { clazz }, service, properties);
  }


  public void removeBundleListener(BundleListener listener) { 
    synchronized(bundleListeners) {
      bundleListeners.remove(listener);
    }
  }

  public void removeFrameworkListener(FrameworkListener listener) { 
    synchronized(frameworkListeners) {
      frameworkListeners.remove(listener);
    }
  }
     

  public void removeServiceListener(ServiceListener listener) { 
    synchronized(serviceListeners) {
      serviceListeners.remove(listener);
    }
  }

  public boolean ungetService(ServiceReference sr) { 
    if(bDebug) {
      System.out.println("ungetService: noop: " + sr);
    }
    return true;
  }

  static boolean inArray(Object[] sa, Object s) {
    for(int i = 0; sa != null && i < sa.length; i++) {
      if(sa[i] != null && sa[i].equals(s)) {
	return true;
      }
    }
    return false;
  }

public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
	// TODO Auto-generated method stub
	return null;
}
}
