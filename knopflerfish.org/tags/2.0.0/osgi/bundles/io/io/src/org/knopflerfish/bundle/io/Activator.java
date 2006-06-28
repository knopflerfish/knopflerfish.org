package org.knopflerfish.bundle.io;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.io.*;

public class Activator implements BundleActivator {
    
    private ConnectorServiceImpl connService = null;
  
    public void start(BundleContext bc) {
	connService = new ConnectorServiceImpl(bc);
	Hashtable props = new Hashtable();
	bc.registerService(ConnectorService.class.getName(), 
			   connService,
			   props);
    }

    public void stop(BundleContext bc) {
	connService = null;
    }
}
