package org.knopflerfish.bundle.soap.remotefw;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;

public class RemoteFWImpl implements RemoteFW {
  
  ServiceRegistration reg = null;

  ServiceTracker slTracker;

  Thread reaper     = null;
  boolean bReap     = false;
  long    reapDelay = 60 * 1000;

  public RemoteFWImpl() {
  }

  public void startBundle(long bid) {
    try {
      Activator.bc.getBundle(bid).start();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to start bid=" + bid);
    }
  }

  public void stopBundle(long bid) {
    try {
      Activator.bc.getBundle(bid).stop();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to stop bid=" + bid);
    }
  }

  public void updateBundle(long bid) {
    try {
      Activator.bc.getBundle(bid).update();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to update bid=" + bid);
    }
  }

  public void uninstallBundle(long bid) {
    try {
      Activator.bc.getBundle(bid).uninstall();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to uninstall bid=" + bid);
    }
  }

  public long installBundle(String location) {
    try {
      Bundle b = Activator.bc.installBundle(location);
      return b.getBundleId();
    } catch (BundleException e) {
      throw new IllegalArgumentException("Failed to install location=" + location);
    }
  }


  public long[]    getBundles() {
    Bundle[] bl = Activator.bc.getBundles();
    long[] bids = new long[bl.length];

    for(int i = 0; i < bl.length; i++) {
      bids[i] = bl[i].getBundleId();
    }
    return bids;
  }

  public String  getBundleLocation(long bid) {
    Bundle b = Activator.bc.getBundle(bid);

    return b.getLocation();
  }

  public int  getBundleState(long bid) {
    Bundle b = Activator.bc.getBundle(bid);

    return b.getState();
  }


  public long[]    getServicesInUse(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return Util.referencesToLong(b.getServicesInUse());
  }

  public long[]    getRegisteredServices(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return Util.referencesToLong(b.getRegisteredServices());
  }

  public long[]    getServiceReferences(String filter) {
    try {
      ServiceReference[] srl = Activator.bc.getServiceReferences(null, filter);
      if(srl == null) {
	return new long[0];
      }
      long[] r = new long[srl.length * 2];
      int n = 0;
      for(int i = 0; i < srl.length; i++) {
	r[n * 2]     = srl[i].getBundle().getBundleId();
	r[n * 2 + 1] = ((Long)srl[i].getProperty(Constants.SERVICE_ID)).longValue();
	n++;
      }
      return r;
    } catch (Exception e) {
      throw new RuntimeException("Failed to get services from filter=" + filter + ",  " + e);
    }
  }

  public Map  getBundleManifest(long bundleId) {
    Bundle b = Activator.bc.getBundle(bundleId);

    Dictionary d = b.getHeaders();

    Map result = new HashMap();

    int i = 0;
    for(Enumeration e = d.keys(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      String val = (String)d.get(key);

      result.put(key, val);

      i += 2;
    }

    return result;
  }


  public long[]    getServices() {
    return null;
  }

  public long[]    getFrameworkEvents() {
    synchronized(frameworkEvents) {
      long[] r = new long[frameworkEvents.size() * 2];
      int i = 0;

      for(Iterator it = frameworkEvents.iterator(); it.hasNext();) {
	FrameworkEvent ev = (FrameworkEvent)it.next();
	r[i * 2]     = ev.getBundle().getBundleId();
	r[i * 2 + 1] = ev.getType();
	i++;
      }

      frameworkEvents.clear();
      return r;
    }
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

  public Map  getServiceProperties(long sid) {
    try {
      ServiceReference[] srl = 
	Activator.bc.getServiceReferences(null, "(service.id=" + sid + ")");
      
      String[] keys   = srl[0].getPropertyKeys();
        
      Map result = new HashMap();

      for(int i = 0; i < keys.length; i++) {
	String key = keys[i];
	Object val = srl[0].getProperty(keys[i]);
	//	Object strVal = Util.encodeAsString(val);

	result.put(key, val);
      }
      
      return result;

    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get service properties " +
					 "for service.id=" + sid + ", " + e);
    }
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

  public int  getBundleStartLevel(long bid) {
    Bundle b = Activator.bc.getBundle(bid);
    return ((StartLevel)slTracker.getService()).getBundleStartLevel(b);
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

  public void start() {
    if(reg == null) {

      slTracker = new ServiceTracker(Activator.bc,
				     StartLevel.class.getName(),
				     null);
      slTracker.open();

      Hashtable props = new Hashtable();
      
      props.put("SOAP.service.name", "OSGiFramework");
      
      reg = Activator.bc.registerService(RemoteFW.class.getName(),
					 this,
					 props);

      
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
	  public void frameworkEvent(FrameworkEvent event ) {
	    synchronized(frameworkEvents) {
	      frameworkEvents.add(event);
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
}
