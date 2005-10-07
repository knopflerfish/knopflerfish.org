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
  public Vector       getBundleManifest(long bid);

  // (sid)
  public long[]    getRegisteredServices(long bid);

  // (sid)
  public long[]    getServicesInUse(long bid);

  // (sid, bid)
  public long[]    getServiceReferences(String filter);
  public long[]    getServiceReferences2(String clazz, String filter);

  // (bid)
  public long[]    getUsingBundles(long sid);

  // (bid, type)
  public long[]    getBundleEvents();

  // (sid, type)
  public long[]    getServiceEvents();

  // (bid, type)
  public long[]     getFrameworkEvents();

  // (key, value)
  public Vector getServiceProperties(long sid);

  public int     getStartLevel();
  public void    setStartLevel(int level);
  public void    setBundleStartLevel(long bid, int level);
  public int     getBundleStartLevel(long bid);
  public void    setInitialBundleStartLevel(int level);
  public int     getInitialBundleStartLevel();
  public boolean isBundlePersistentlyStarted(long bid);

  public Vector    getExportedPackage(String name);
  public Vector  getExportedPackages(long bid);
  public void   refreshPackages(long[] bids) ;

  public Vector    getSystemProperties();

  public Vector getLog();
  public Vector getFullLog();

  public void createSession(String name);
  public void abortCommand();
  public void closeSession();
  public void setEscapeChar(char ch);
  public void setInterruptString(String str);
  public String[] setAlias(String key, String[] val);
  public String runCommand(String command);

}

