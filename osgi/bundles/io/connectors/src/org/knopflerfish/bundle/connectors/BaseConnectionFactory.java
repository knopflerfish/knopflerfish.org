/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

/*
 * @author Mats-Ola Persson
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

    protected synchronized void addConnection(Connection con) {
	list.add(con);
    }

    public synchronized void unregisterFactory(BundleContext bc) {
	int size = list.size();
	
	try {
	    for (int i = 0; i < size; i++)
		((Connection)list.get(i)).close();
	    
	} catch (IOException e) { /* ignore */ }
    }
}
