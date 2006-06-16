/*
 * Created on Wed Jun 14 11:13:31 CEST 2006
 */
package org.knopflerfish.bundle.httpclient_connector;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.io.ConnectionFactory;

public class Activator implements BundleActivator {
  private ServiceRegistration reg;

  public void start(BundleContext bc) throws Exception {
	  Hashtable properties = new Hashtable();
	  properties.put(ConnectionFactory.IO_SCHEME, new String[] {"http"});
	  reg = bc.registerService(ConnectionFactory.class.getName(), new HttpClientFactory(), properties);
	  
  }

  public void stop(BundleContext context) throws Exception {
	  reg.unregister();
	  reg = null;
  }
  
}