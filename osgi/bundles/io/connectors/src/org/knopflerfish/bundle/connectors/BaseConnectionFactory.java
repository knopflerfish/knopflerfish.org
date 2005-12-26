/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

/**
 * @author Mats-Ola Persson
 */
 
/**
	ConnectionFactories should close all currently open 
	connections when they are stopped*. This class enables
	one to easily keep track of Connections that a factory
	has created.
	
	Whenever a connection is created the Connection should 
	register itself using the registerConnection and whenever 
	a Connection is closed it should call unregisterConnection.
	When a factory is unloaded from the system the function 
	closeAll will be called, then all registered Connection
	will be closed and unregistered.
	<br><br>
	
	*: according to r4-cmpn section 109.4.1, p. 224
*/

package org.knopflerfish.bundle.connectors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.microedition.io.Connection;
import org.osgi.framework.BundleContext;

import org.osgi.service.io.ConnectionFactory;

public abstract class BaseConnectionFactory implements ConnectionFactory 
{
    private ArrayList list = new ArrayList();

    // something like new String[]{"datagram", ..}
    public abstract String[] getSupportedSchemes();
    
    public void registerFactory(BundleContext bc) {
		Hashtable properties = new Hashtable();
		properties.put(ConnectionFactory.IO_SCHEME, getSupportedSchemes());
		bc.registerService(ConnectionFactory.class.getName(), this, properties);	
    }

    public synchronized void registerConnection(Connection con) {
		list.add(con);
    }
	
	public synchronized void unregisterConnection(Connection con) {
		list.remove(con);
	}

    public synchronized void unregisterFactory(BundleContext bc) {
		int size = list.size();
	
		for (int i = 0; i < size; i++) {
			try {
				((Connection)list.get(i)).close();
			} catch (IOException e) { /* ignore */ }
		}
		
		// Just a precaution, if this factory is used again.
		list = new ArrayList(); 
    }
}
