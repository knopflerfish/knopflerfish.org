package org.knopflerfish.service.soap.remotefw;

import org.osgi.framework.*;
import java.util.*;

public interface RemoteFW {
  public void startBundle(long bid);
  public void stopBundle(long bid);
  public void updateBundle(long bid);
  public void uninstallBundle(long bid);
  public long installBundle(String location);

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

  // (bid, type)
  public long[]    getServiceReferences(String filter);

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
}
