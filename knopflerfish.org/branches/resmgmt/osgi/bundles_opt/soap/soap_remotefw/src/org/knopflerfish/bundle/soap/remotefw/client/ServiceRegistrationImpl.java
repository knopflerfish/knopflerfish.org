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

public class ServiceRegistrationImpl implements ServiceRegistration {
  Hashtable props;
  String[]  clazzes;
  Object    service;
  public ServiceRegistrationImpl(String[] clazzes, 
				 Object service,
				 Dictionary props) {
    this.clazzes = new String[clazzes.length];
    for(int i = 0; i < clazzes.length; i++) {
      this.clazzes[i] = clazzes[i];
    }

    this.props = new Hashtable();
    for(Enumeration e = props.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      this.props.put(key, props.get(key));
    }

    this.service = service;
  }

  public ServiceReference getReference() {
    throw new RuntimeException("Not implemented");
  }

  public void setProperties(Dictionary properties) {
    throw new RuntimeException("Not implemented");
  }

  public void unregister() {
    throw new RuntimeException("Not implemented");
  }
 }
