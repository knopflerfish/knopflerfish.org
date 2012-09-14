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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * This class is based on the class HttpServerFactory from Knopflerfish's
 * HTTP Service bundle. 
 * The ManagedServiceFactory approach has been chosen over the managed 
 * Service Approach / using a proxy 
 * - to prevent multiple registrations of the same object with default parameters
 * - to possibly allow several different J2 based SslFactories within the same 
 *   framework.
 * Can be argued if this is really necessary. 
 */
public class SslServiceFactory implements ManagedServiceFactory 
{
    protected final static String PID = 
        "org.knopflerfish.bundle.ssl.j2sp";
    
    private final BundleContext m_bc;
    private final LogRef m_log;

    private final Object m_updateLock = new Object();
    private final Dictionary m_services = new Hashtable();
    
    public SslServiceFactory(BundleContext bc, LogRef log)
    {
        m_bc = bc;
        m_log = log;
    }
    
	public String getName() 
    {
		return "SSL Java2 Service Provider";
	}

    // public methods

    public void destroy() 
    {

        Enumeration e = m_services.keys();
        while (e.hasMoreElements())
          deleted((String) e.nextElement());
    }

    public void updated(String pid, Dictionary configuration)
          throws ConfigurationException 
    {
        synchronized(m_updateLock) 
        {
            if(m_log.doDebug()) 
          	    m_log.debug("Updated pid=" + pid);
            
            if(!PID.equals(pid)  && (null != m_services.get(PID))) 
            {
            	if(m_log.doDebug()) 
            		m_log.debug("Overriding default instance with new pid " + pid);
            	
            	deleted(PID);
            }
        
            SslServiceWrapper service = (SslServiceWrapper) m_services.get(pid);
            if (service == null) 
            {
            	if(m_log.doDebug()) 
            	   m_log.debug("create pid=" + pid);
            	
                service = new SslServiceWrapper(m_bc, m_log);
                m_services.put(pid, service);
    
            } 
            
            service.update(configuration);
                      
        }
      }

    /* (non-Javadoc)
	 * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
	 */
	public void deleted(String pid) 
    {
        SslServiceWrapper service = (SslServiceWrapper) m_services.get(pid);
        if (service != null) 
        {
            if(m_log.doDebug()) 
               m_log.debug("delete pid=" + pid);
            
            try 
            {
				service.update(null);
			
            } catch (ConfigurationException e) {}
        }
    }

}
