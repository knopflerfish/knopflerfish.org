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
import org.knopflerfish.service.soap.remotefw.*;


public class RemoteConnectionImpl implements RemoteConnection {

  Map clients = new HashMap();

  public RemoteConnectionImpl() {
  }

  public BundleContext connect(String host) {
    try {
      RemoteFWClient remoteClient = new RemoteFWClient();
      remoteClient.open(host);
      
      BundleContext bc = remoteClient.getBundleContext();
      clients.put(bc, remoteClient);
      return bc;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Failed to connect to host=" + host);
    }
  }

  public void disconnect(BundleContext bc) {
    RemoteFWClient remoteClient = (RemoteFWClient)clients.get(bc);
    if(remoteClient != null) {
      remoteClient.close();
      clients.remove(bc);
    } else {
      System.err.println("No remote bc=" + bc);
    }
  }
}
