package org.knopflerfish.bundle.axis;

import org.apache.axis.ConfigurationException;
import org.apache.axis.EngineConfiguration;

import org.apache.axis.description.ServiceDesc;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.InputStream;

/**
 * Replacement of the method getDeployedServices in SimpleProvider, since it does
 * not account for the services in the default configuration.
 * In addition dynamic loading of WSDD data added.
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class ConfigurationProvider extends org.apache.axis.configuration.SimpleProvider
{
    /** An optional "default" EngineConfiguration */
    EngineConfiguration defaultConfiguration = null;

    /**
     * Constructor which takes an EngineConfiguration which will be used
     * as the default.
     */
    public ConfigurationProvider(EngineConfiguration defaultConfiguration) {
	super(defaultConfiguration);
        this.defaultConfiguration = defaultConfiguration;
    }



    /**
     * Get an enumeration of the services deployed to this engine
     */
    public Iterator getDeployedServices() throws ConfigurationException {
        ArrayList serviceDescs = new ArrayList();
        Iterator i = super.getDeployedServices();
        while (i.hasNext()) {
            serviceDescs.add((ServiceDesc) i.next());		  
        }
	if (defaultConfiguration != null) {
	  Iterator defi = defaultConfiguration.getDeployedServices();
	  while(defi.hasNext()) {
            ServiceDesc sd = (ServiceDesc)defi.next();
            serviceDescs.add(sd);		  
          }
        }
        return serviceDescs.iterator();
    }
    
    public void addWSDD(InputStream stream) {
	System.err.println("ConfigurationProvider.addWSDD()  not yet implemented");    
    }
}
