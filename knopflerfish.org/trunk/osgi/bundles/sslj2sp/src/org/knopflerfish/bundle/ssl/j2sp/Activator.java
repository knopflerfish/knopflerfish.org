/*
 * Copyright (c) 2003, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knopflerfish.bundle.ssl.j2sp;

//import org.knopflerfish.bundle.util.LogClient;
import java.util.Dictionary;
import java.util.Hashtable;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

/**
 * Standard bundle activator, which registers a managed service factory
 * @see SslServiceFactory.
 * Based on the HTTP Service design, this class is also in charge of 
 * triggering the creation of a default SslServerSocketFactory if nothing
 * else is present. 
 */
public class Activator implements org.osgi.framework.BundleActivator
{
    //public final LogClient m_log = new LogClient();
    protected LogRef m_log;
    
    private BundleContext m_bc;
    
    ServiceRegistration m_reg;
    SslServiceFactory m_factory;


	/**
	 * 
	 */
	public Activator()
	{
		super();
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception
	{
        
        m_bc = bc;
        m_log = new LogRef(m_bc);

        m_factory = new SslServiceFactory(m_bc, m_log);

        Dictionary parameters = new Hashtable();
        parameters.put("service.pid", SslServiceFactory.PID);
        m_reg = bc.registerService(ManagedServiceFactory.class.getName(),
                                           m_factory,
                                           parameters);

        ServiceReference adminRef = null;
        try 
        {
            ConfigurationAdmin admin = null;
            Configuration[] configs = null;
            try 
            {
                adminRef = bc.getServiceReference(ConfigurationAdmin.class.getName());

                // Potential start order problem!
                if(adminRef != null) 
                {
                    admin = (ConfigurationAdmin) bc.getService(adminRef);
                    String filter =
                    "(&(service.m_factoryPid=" + SslServiceFactory.PID + ")" +
                    "(|(service.bundleLocation=" + bc.getBundle().getLocation() + ")" +
                    "(service.bundleLocation=NULL)" +
                    "(!(service.bundleLocation=*))))";
                    configs = admin.listConfigurations(filter);
                 }
            } catch (Exception e) 
            {
                if (m_log.doDebug()) m_log.debug("Exception when trying to get CM", e);
            }
            
            if (admin == null) 
            {
                if (m_log.doInfo()) 
                    m_log.info("No CM present, using default configuration");
                
                m_factory.updated(SslServiceWrapper.DEFAULT_SERVICE_PID,
                                      SslServiceWrapper.getDefaultConfig());
            } else 
            {
                if (configs == null || configs.length == 0) 
                {
                    if (m_log.doInfo()) 
                        m_log.info("No configuration present, creating default configuration");

                    m_factory.updated(SslServiceFactory.PID,
                        SslServiceWrapper.getDefaultConfig());
                }
            }
        
        } catch (ConfigurationException ce) 
        {
            m_log.error("Configuration error", ce);
            
        } finally 
        {
            if (adminRef != null)
            {
                m_bc.ungetService(adminRef);
            }
        }
    }


	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext arg0) throws Exception
	{
        if (m_reg != null)
        {
        	m_reg.unregister();
        }
        if (m_factory != null)
        {
        	m_factory.destroy();
        }
        m_log.close();
        m_log = null;
	}
   


}
