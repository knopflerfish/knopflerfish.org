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

import org.knopflerfish.bundle.util.LogClient;
import org.osgi.framework.*;
import org.osgi.service.cm.*;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

import javax.net.ssl.*;

public class BundleActivator extends SSLServerSocketFactory implements org.osgi.framework.BundleActivator, ManagedService
{
    public final LogClient m_log = new LogClient();
    
    private final static String PID = BundleActivator.class.getName();
    
    private final static String KEYSTOREPASS_KEY = "keystorepass";
    private final static String KEYSTORE_KEY = "keystore";

    private SSLServerSocketFactory m_ssl;
    private BundleContext m_bc;
    private Dictionary m_config;
	private final static String DEFAULT_KEYSTORE_VALUE = "/resources/defaultkeys";
    private final static String DEFAULT_PASSPHR_VALUE = "defaultpass";
	private ServiceRegistration m_managedReg;


	/**
	 * 
	 */
	public BundleActivator()
	{
		super();
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception
	{
            m_bc = bc;
            m_log.init(m_bc);
            
            m_config = getDefaultConfig();
              
            //no need to create a factory now without the configuration properties, 
            // update will be called at least once,
            
            m_managedReg = m_bc.registerService(
                new String[] {ManagedService.class.getName(), SSLServerSocketFactory.class.getName()}, 
                this, 
                m_config);      
        
    }


	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext arg0) throws Exception
	{
        if (m_managedReg != null)
        {
            m_managedReg.unregister(); //TODO NO FRAMEWORK METHODS WIHT LOCK !!!
            m_managedReg = null;
        }
        
        m_ssl = null;
        m_log.cleanup();
	}
   

    private SSLServerSocketFactory getServerSocketFactory() throws ConfigurationException
    {
        SSLContext context = null;
		try
		{
			context = SSLContext.getInstance(ConstsIf.PROT_TLS_V1);
		
        } catch (NoSuchAlgorithmException e)
		{
            throw new ConfigurationException(
                        "",
                        "creating SSLContext: ERROR no such algorithm");
		}

		KeyStore myKeys;
		try
		{
			myKeys = KeyStore.getInstance(ConstsIf.KS_TYPE_JKS);

		} catch (KeyStoreException e1)
		{
            throw new ConfigurationException(
                        "",
                        "creating SSLContext: ERROR no such algorithm");
		}

        /**
         * this is the order of attempts that will be tried to obtain a keystore; 
         * - if the config admin set it to type byte[], assume it is a keystore itself
         * - else if it is of type string try to interpret this string as an (absolute) path 
         *   to a file
         * - else use the default keystore from the bundle itself with the default password 
         */
        char[] keyPassPhrase = null; 
        try
        {
            keyPassPhrase = ((String) m_config.get(KEYSTOREPASS_KEY)).toCharArray();
        
        } catch (Exception epass) {}
        
        InputStream is = null;

        // from CM as byte[] ?
        if ((keyPassPhrase != null) && (is == null))
        {        
            try 
            {
                is = new ByteArrayInputStream((byte[]) m_config.get(KEYSTORE_KEY));
    
            } catch (Exception eb) {}
        }        

        //from CM as a file pointer ?
        if ((keyPassPhrase != null) && (is == null))
        {        
            try 
            {
                ((String) m_config.get(KEYSTOREPASS_KEY)).toCharArray();                
                is = new FileInputStream((String) m_config.get(KEYSTORE_KEY));
            
            } catch (Exception ef) {}
        }
        
        if (is == null) // enough nonsense, use the default keystore
        {        
            m_log.warn("[" + PID + "]  trying default keystore now");
            try
            {
                keyPassPhrase = DEFAULT_PASSPHR_VALUE.toCharArray();
    			is = getClass().getResourceAsStream(DEFAULT_KEYSTORE_VALUE);
    		
            } catch (Exception edef)
    		{
            }
        }
        
        try
        {
            myKeys.load(is, keyPassPhrase);
            
        } catch (Exception eload)
        {
            throw new ConfigurationException(
                            KEYSTORE_KEY + "," + KEYSTOREPASS_KEY,
                            "ERROR loading keys !, passphrase " + String.valueOf(keyPassPhrase));
        }
        
        KeyManagerFactory kmf;
		try
		{
			kmf = KeyManagerFactory.getInstance("SunX509");

		} catch (NoSuchAlgorithmException e4)
		{
            throw new ConfigurationException(
                        "",
                        "creating KeyManagerFactory: ERROR no such algorithm");
		}
		try
		{
			kmf.init(myKeys, keyPassPhrase);

		} catch (Exception e5)
		{
            throw new ConfigurationException(
                        "",
                        "initing kmf: " + e5.getMessage());
		}
            
        try
		{
			context.init(kmf.getKeyManagers(), null, null);

		} catch (KeyManagementException e6)
		{
            throw new ConfigurationException(
                        "",
                        "initing SSLContext: " + e6.getMessage());
		}
    
        return context.getServerSocketFactory();
    }


    private Hashtable getDefaultConfig()
    {
        Hashtable properties = new Hashtable();
        properties.put(Constants.SERVICE_VENDOR, "knopflerfish"); 
        properties.put(Constants.BUNDLE_NAME, "SSL Java2 Security Provider");
        properties.put(Constants.BUNDLE_DESCRIPTION, "This service will receive "
        + "the desired configuration used to configure and register a SSLServerSocketFactory.");

        properties.put(Constants.SERVICE_PID, PID);

        properties.put(KEYSTOREPASS_KEY, DEFAULT_PASSPHR_VALUE);

        properties.put(KEYSTORE_KEY, DEFAULT_KEYSTORE_VALUE);


        return properties;
    }    


	/* (non-Javadoc)
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	public void updated(Dictionary config) throws ConfigurationException
	{
        m_log.info("[" + PID + "]  update invoked");
          
        
        /*  //TODO two things:
         *  - use a service factory to make sure to prevent this "short preregistration" blip
         *    before it comes in a stable state;
         *  - prevent unnecessary executions of this thing if nothing has changed.
         */ 
        if (config != null)
        {
            m_config = config;
        }        
		
        try
        {
            m_ssl = null;
            m_ssl = getServerSocketFactory();
            
        } finally
        {
            String state = ((m_ssl == null) ? "ERROR" : "OPERATIONAL");
            m_config.put("INTERNAL_STATE", state);
            
            m_log.info("[" + PID + "]  updated, INTERNAL_STATE= " + state);
            
            m_managedReg.setProperties(m_config);
        }
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.SSLServerSocketFactory#getDefaultCipherSuites()
	 */
	public String[] getDefaultCipherSuites()
	{
		return ((m_ssl == null) ? new String[0] : m_ssl.getDefaultCipherSuites());
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.SSLServerSocketFactory#getSupportedCipherSuites()
	 */
	public String[] getSupportedCipherSuites()
	{
		return ((m_ssl == null) ? new String[0] : m_ssl.getSupportedCipherSuites());
	}

	/* (non-Javadoc)
	 * @see javax.net.ServerSocketFactory#createServerSocket(int)
	 */
	public ServerSocket createServerSocket(int arg0) throws IOException
    {
        return m_ssl.createServerSocket(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.net.ServerSocketFactory#createServerSocket(int, int)
	 */
	public ServerSocket createServerSocket(int arg0, int arg1) throws IOException
	{
        return m_ssl.createServerSocket(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.net.ServerSocketFactory#createServerSocket(int, int, java.net.InetAddress)
	 */
	public ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException
	{
        return m_ssl.createServerSocket(arg0, arg1, arg2);
	}
}
