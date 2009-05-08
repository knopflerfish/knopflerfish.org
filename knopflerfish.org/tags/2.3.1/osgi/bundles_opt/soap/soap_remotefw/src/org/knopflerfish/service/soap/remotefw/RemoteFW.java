package org.knopflerfish.service.soap.remotefw;

import org.osgi.framework.*;
import java.util.*;

public interface RemoteFW {
  public static final String NULL_STR = "@@NULL@@";

  public void startBundle(long bid);
  public void stopBundle(long bid);
  public void updateBundle(long bid);
  public void uninstallBundle(long bid);
  public long installBundle(String location);

  public long        getBundle(); 

  public String      getBundleContextProperty(String key); 

  // (bid)
  public long[]     getBundles();

  public String     getBundleLocation(long bid);
  public int        getBundleState(long bid);

  // (key, value)
  public Map       getBundleManifest(long bid);

  // (sid)
  public long[]    getRegisteredServices(long bid);

  // (sid)
  public long[]    getServicesInUse(long bid);

  // (sid, bid)
  public long[]    getServiceReferences(String filter);
  public long[]    getServiceReferences2(String clazz, String filter);

  // (bid, type)
  public long[]    getBundleEvents();

  // (sid, type)
  public long[]    getServiceEvents();

  // (bid, type)
  public long[]     getFrameworkEvents();

  // (key, value)
  public Map getServiceProperties(long sid);
  
  public int     getStartLevel();
  public void    setStartLevel(int level);
  public void    setBundleStartLevel(long bid, int level);
  public int     getBundleStartLevel(long bid);
  public void    setInitialBundleStartLevel(int level);
  public int     getInitialBundleStartLevel();
  public boolean isBundlePersistentlyStarted(long bid);

  public Map    getExportedPackage(String name);
  public Map[]  getExportedPackages(long bid);
  public void   refreshPackages(long[] bids) ;
 
  public Map    getSystemProperties();

}

