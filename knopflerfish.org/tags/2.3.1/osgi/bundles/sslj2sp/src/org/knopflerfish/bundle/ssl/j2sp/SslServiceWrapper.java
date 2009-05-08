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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

/**
 * This Wrapper class manages basically a sinle SslServerSocketFactory
 * object which will be registered as a service. 
 * It includes keeping track of the object itself, the registration and
 * to update, delete and create this registration based on calls to
 * the update method.
 * 
 */
public class SslServiceWrapper 
{
    protected final static String DEFAULT_SERVICE_PID = 
        "org.knopflerfish.bundle.ssl.j2sp.DEFAULT";
    
    private final static String KEYSTOREPASS_KEY = "keystorepass";
    private final static String KEYSTORE_KEY = "keystore";
    private final static String DEFAULT_KEYSTORE_VALUE = "/resources/defaultkeys";
    private final static String DEFAULT_PASSPHR_VALUE = "defaultpass";

    private Dictionary m_config;
	private BundleContext m_bc;
    private LogRef m_log;
    
    private ServiceRegistration m_reg;
 
    /**
	 * @param m_bc
	 * @param m_log
	 */
	protected SslServiceWrapper(BundleContext bc, LogRef log) 
    {
        m_bc = bc;
		m_log = log;
	}

	/**
	 * @param config The latest properties to be used by this service, also used when registering framework.
     * If this is null, will tell this object to do cleanup! 
	 * @throws ConfigurationException
	 */
	protected void update (Dictionary config) 
                   throws ConfigurationException
    {
        //typically, if we have an m_config, and the passed in config is not
        //different, no reason to always recreate and reregister the same service.
        //TODO: later. For now, just always undo it

        m_config = config;
        
        //First, delete the old registration.
        if (m_reg != null)
        {
        	m_reg.unregister();
            m_reg = null;
        }
        
        //Stop here in case of cleanup
        if (m_config == null)
        {
            return; 
        }
        
        // create new SSLServerSocketFactory!!!
        boolean isDefaultConfig = DEFAULT_SERVICE_PID.equals(m_config.get("service.pid"));
        // Step 1: context
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

        
        //Step 2: obtain a key store instance, type is fixed
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

        
        InputStream is = null;
        char[] keyPassPhrase = null; 

        if (!isDefaultConfig) 
        {
            //Step 3:obtain password phrase for a keystore
            try
            {
                keyPassPhrase = ((String) m_config.get(KEYSTOREPASS_KEY)).toCharArray();
            
            } catch (Exception epass) {}
    
            //Step 4:obtain input stream for a key store
            // - if the config admin set it to type byte[], assume it is a keystore itself
            // - else if it is of type string try to interpret this string as an (absolute) path 
            //   to a file
            // - else assume that this is a incomplete configruation we got from the CM Admin, 
            //   use the default keystore
    
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
                    is = new FileInputStream((String) m_config.get(KEYSTORE_KEY));
                
                } catch (Exception ef) {}
            }
        
            if ((is == null) &&  m_log.doWarn())
            {
                m_log.warn("using default, config is invalid: " + m_config.get("service.pid"));
            }
        }
        
        // Step 3 & 4 executed now if config is bad or we just use the default config
        if (is == null)
        {       
            try
            {
                keyPassPhrase = DEFAULT_PASSPHR_VALUE.toCharArray();
                is = getClass().getResourceAsStream(DEFAULT_KEYSTORE_VALUE);
            
            } catch (Exception edef)
            {
            }
        }
        
        // Step 5: load keys into keystore
        try
        {
            myKeys.load(is, keyPassPhrase);
            
        } catch (Exception eload)
        {
            throw new ConfigurationException(
                            KEYSTORE_KEY + "," + KEYSTOREPASS_KEY,
                            "ERROR loading keys !, passphrase " + String.valueOf(keyPassPhrase));
        }
        
        //Step 6: create and initialize KeyManagerFactory
        KeyManagerFactory kmf;
        try
        {
            kmf = KeyManagerFactory.getInstance(ConstsIf.KM_TYPE_SUN);

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
        
        //Step 7: initialize context with the key manager factory
        try
        {
            context.init(kmf.getKeyManagers(), null, null);

        } catch (KeyManagementException e6)
        {
            throw new ConfigurationException(
                        "",
                        "initing SSLContext: " + e6.getMessage());
        }
    
        //Step 8: create SSL Server Socket Factory
        SSLServerSocketFactory ssl = null;
        try 
        {
			ssl = context.getServerSocketFactory();
		
        } catch (Exception e7) 
        {
            throw new ConfigurationException(
                    "",
                    "creating SSLServerSocketFactory object: " + e7.getMessage());        
        }

        m_reg = m_bc.registerService(SSLServerSocketFactory.class.getName(),
                    ssl, m_config);
        
    }
        
    protected static Dictionary getDefaultConfig()
    {
        Hashtable properties = new Hashtable();
        properties.put(Constants.SERVICE_VENDOR, "knopflerfish"); 
        properties.put(Constants.BUNDLE_NAME, "SSL Java2 Security Provider");
        properties.put(Constants.BUNDLE_DESCRIPTION, "The default SSL Server Socket factory.");

        properties.put(Constants.SERVICE_PID, SslServiceWrapper.DEFAULT_SERVICE_PID);

        properties.put(KEYSTOREPASS_KEY, DEFAULT_PASSPHR_VALUE);

        properties.put(KEYSTORE_KEY, DEFAULT_KEYSTORE_VALUE);


        return properties;
    }    

}
