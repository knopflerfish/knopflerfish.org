package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

public class StartLevelImpl implements StartLevel {

  RemoteFW fw;

  StartLevelImpl(RemoteFW fw) {
    this.fw  = fw;
  }

  public int getBundleStartLevel(Bundle bundle) {
    return fw.getBundleStartLevel(bundle.getBundleId());
  }

  public int getInitialBundleStartLevel() {
    return fw.getInitialBundleStartLevel();
  }

  public int getStartLevel() {
    return fw.getStartLevel();
  }

  public boolean isBundlePersistentlyStarted(Bundle bundle) {
    return fw.isBundlePersistentlyStarted(bundle.getBundleId());
  }

  public void setBundleStartLevel(Bundle bundle, int startlevel) {
    fw.setBundleStartLevel(bundle.getBundleId(), startlevel);
  }

  public void setInitialBundleStartLevel(int startlevel) {
    fw.setInitialBundleStartLevel(startlevel);
  }

  public void setStartLevel(int startlevel) {
    fw.setStartLevel(startlevel);
  }
}
