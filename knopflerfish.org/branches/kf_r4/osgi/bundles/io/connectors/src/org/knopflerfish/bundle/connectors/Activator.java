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
* @author Mats-Ola Persson
*/
public class Activator implements BundleActivator {
    
    private BaseConnectionFactory[] factories;
	
    public void start(BundleContext context) {
		
		factories = new BaseConnectionFactory[] { new DatagramConnectionFactory(),
			new HttpConnectionFactory(),
			new SocketConnectionFactory() };
		
		for (int i = 0; i < factories.length; i++)
			factories[i].registerFactory(context);
    }
	
    // handle unregistration according to specs
    public void stop(BundleContext context) { 
		for (int i = 0; i < factories.length; i++)
			factories[i].unregisterFactory(context);
    }
	
} // Activator
