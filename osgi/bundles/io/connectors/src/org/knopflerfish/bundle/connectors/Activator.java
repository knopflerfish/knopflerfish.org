/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

package org.knopflerfish.bundle.connectors;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.io.ConnectionFactory;

import org.knopflerfish.bundle.connectors.http.HttpConnectionFactory;
import org.knopflerfish.bundle.connectors.socket.SocketConnectionFactory;
import org.knopflerfish.bundle.connectors.datagram.DatagramConnectionFactory;

/**
 * @author Kaspar Weilenmann 
 * @author Philippe Laporte
 */
public class Activator implements BundleActivator {

  // private methods

  private static void registerSocketConnectionFactory(BundleContext context) {
	Dictionary properties = new Hashtable();
    properties.put(ConnectionFactory.IO_SCHEME, new String[] { "socket" });
    context.registerService(ConnectionFactory.class.getName(), new SocketConnectionFactory(), properties);
  }

  private static void registerHttpConnectionFactory(BundleContext context) {
    Dictionary properties = new Hashtable();
    properties.put(ConnectionFactory.IO_SCHEME, new String[] { "http" });
    context.registerService(ConnectionFactory.class.getName(), new HttpConnectionFactory(), properties);
  }

  private static void registerDatagramConnectionFactory(BundleContext context) {
	Dictionary properties = new Hashtable();
	properties.put(ConnectionFactory.IO_SCHEME, new String[] { "datagram" });
	context.registerService(ConnectionFactory.class.getName(), new DatagramConnectionFactory(), properties);
  }

  // implements BundleActivator

  public void start(BundleContext context) {
    registerSocketConnectionFactory(context);
    registerHttpConnectionFactory(context);
    registerDatagramConnectionFactory(context);
  }

  //TODO handle unregistration according to specs
  public void stop(BundleContext context) { }

} // Activator
