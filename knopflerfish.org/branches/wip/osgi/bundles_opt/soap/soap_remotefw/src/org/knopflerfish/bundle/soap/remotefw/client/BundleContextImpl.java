package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

import org.knopflerfish.bundle.soap.remotefw.*;

public class BundleContextImpl implements BundleContext {
  RemoteFW fw;

  Thread  runner = null;
  boolean bRun   = false;
  long    delay  = 10 * 1000;

  BundleContextImpl(RemoteFW fw) {
    this.fw  = fw;
  }

  void start() {
    if(runner == null) {
      runner = new Thread() {
	  public void run() {
	    try {
	      doEvents();
	      Thread.sleep(delay);
	    } catch (Exception e) {
	      //
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

  void doEvents() {
    doBundleEvents();
    doServiceEvents();
    doFrameworkEvents();
  }

  void doBundleEvents() {
    long[] evs = fw.getBundleEvents();
    for(int i = 0; i < evs.length / 2; i++) {
      Bundle      b  = getBundle(evs[i * 2]);
      BundleEvent ev = new BundleEvent((int)evs[i * 2 +1], b);

      sendBundleEvent(ev);
    }
  }

  void doFrameworkEvents() {
    long[] evs = fw.getFrameworkEvents();
    for(int i = 0; i < evs.length; i++) {
      FrameworkEvent ev = new FrameworkEvent((int)evs[i * 2],
					     getBundle(evs[i*2 + 1]),
					     null);

      sendFrameworkEvent(ev);
    }
  }

  void doServiceEvents() {
    long[] evs = fw.getServiceEvents();
    for(int i = 0; i < evs.length; i++) {
      long             sid  = evs[i * 2];
      int              type = (int)evs[i * 2 + 1];
      ServiceReference sr   = getServiceReference(sid);

      ServiceEvent     ev   = new ServiceEvent(type, sr);

      sendServiceEvent(ev);
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
      for(Iterator it = serviceListeners.keySet().iterator(); it.hasNext();) {
	ServiceListener l = (ServiceListener)it.next();
	Filter filter     = (Filter)serviceListeners.get(l);

	if(filter.match(ev.getServiceReference())) {
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
      addServiceListener(listener, null);
    } catch (Exception e) {
    }
  }

  public void addServiceListener(ServiceListener listener, String filter) 
  throws InvalidSyntaxException { 
    synchronized(serviceListeners) {
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
    throw new RuntimeException("Not implemented");
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
      Bundle[] bl = new Bundle[bundleMap.size()];
      int i = 0;
      for(Iterator it = bundleMap.keySet().iterator(); it.hasNext();) {
	Long   bid = (Long)it.next();
	Bundle b   = (Bundle)bundleMap.get(bid);
	bl[i++] = b;
      }

      return bl;
    }
  }

  public File getDataFile(String filename) { 
    throw new RuntimeException("Not implemented");
  }

  public String getProperty(String key) { 
    throw new RuntimeException("NYI");
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
      sr = new ServiceReferenceImpl((BundleImpl)getBundle(srl[0]), srl[1]);
      serviceMap.put(SID, sr);
      return sr;
    }
    return null;
  }

  public Object getService(ServiceReference reference) { 
    throw new RuntimeException("NYI");
  }

  public ServiceReference getServiceReference(String clazz) { 
    throw new RuntimeException("NYI");
  }

  public ServiceReference[] getServiceReferences(String clazz, String filter)
  { 
    throw new RuntimeException("NYI");
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
    throw new RuntimeException("Not implemented");
  }

  
  public ServiceRegistration registerService(String clazz, 
					     Object service, 
					     Dictionary properties) { 
    throw new RuntimeException("Not implemented");
  }


  public void removeBundleListener(BundleListener listener) { 
    throw new RuntimeException("NYI");
  }

  public void removeFrameworkListener(FrameworkListener listener) { 
    throw new RuntimeException("NYI");
  }

  public void removeServiceListener(ServiceListener listener) { 
    throw new RuntimeException("NYI");
  }

  public boolean ungetService(ServiceReference reference) { 
    throw new RuntimeException("NYI");
  }
}
