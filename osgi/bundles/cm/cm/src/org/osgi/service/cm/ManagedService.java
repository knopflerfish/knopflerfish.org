/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.osgi.service.cm;

import java.util.Dictionary;

/**
 * A service that can receive configuration data from a Configuration Admin
 * service.
 * 
 * <p>
 * A Managed Service is a service that needs configuration data. Such an object
 * should be registered with the Framework registry with the
 * <tt>service.pid</tt> property set to some unique identitifier called a PID.
 * 
 * <p>
 * If the Configuration Admin service has a <tt>Configuration</tt> object
 * corresponding to this PID, it will callback the <tt>updated()</tt> method
 * of the <tt>ManagedService</tt> object, passing the properties of that
 * <tt>Configuration</tt> object.
 * 
 * <p>
 * If it has no such <tt>Configuration</tt> object, then it calls back with a
 * <tt>null</tt> properties argument. Registering a Managed Service will
 * always result in a callback to the <tt>updated()</tt> method provided the
 * Configuration Admin service is, or becomes active. This callback must always
 * be done asynchronously.
 * 
 * <p>
 * Else, every time that either of the <tt>updated()</tt> methods is called on
 * that <tt>Configuration</tt> object, the <tt>ManagedService.updated()</tt>
 * method with the new properties is called. If the <tt>delete()</tt> method
 * is called on that <tt>Configuration</tt> object,
 * <tt>ManagedService.updated()</tt> is called with a <tt>null</tt> for the
 * properties parameter. All these callbacks must be done asynchronously.
 * 
 * <p>
 * The following example shows the code of a serial port that will create a port
 * depending on configuration information.
 * 
 * <pre>
 *    class SerialPort implements ManagedService {
 *   
 *      ServiceRegistration registration;
 *      Hashtable configuration;
 *      CommPortIdentifier id;
 *   
 *      synchronized void open(CommPortIdentifier id,
 *      BundleContext context) {
 *        this.id = id;
 *        registration = context.registerService(
 *          ManagedService.class.getName(),
 *          this,
 *          null // Properties will come from CM in updated
 *        );
 *      }
 *   
 *      Hashtable getDefaults() {
 *        Hashtable defaults = new Hashtable();
 *        defaults.put( &quot;port&quot;, id.getName() );
 *        defaults.put( &quot;product&quot;, &quot;unknown&quot; );
 *        defaults.put( &quot;baud&quot;, &quot;9600&quot; );
 *        defaults.put( Constants.SERVICE_PID,
 *          &quot;com.acme.serialport.&quot; + id.getName() );
 *        return defaults;
 *      }
 *   
 *      public synchronized void updated(
 *        Dictionary configuration  ) {
 *        if ( configuration ==
 * <tt>
 * null
 * </tt>
 *    )
 *          registration.setProperties( getDefaults() );
 *        else {
 *          setSpeed( configuration.get(&quot;baud&quot;) );
 *          registration.setProperties( configuration );
 *        }
 *      }
 *      ...
 *    }
 * </pre>
 * 
 * <p>
 * As a convention, it is recommended that when a Managed Service is updated, it
 * should copy all the properties it does not recognize into the service
 * registration properties. This will allow the Configuration Admin service to
 * set properties on services which can then be used by other applications.
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */

public interface ManagedService {
    /**
     * Update the configuration for a Managed Service.
     * 
     * <p>
     * When the implementation of <tt>updated(Dictionary)</tt> detects any
     * kind of error in the configuration properties, it should create a new
     * <tt>ConfigurationException</tt> which describes the problem. This can
     * allow a management system to provide useful information to a human
     * administrator.
     * 
     * <p>
     * If this method throws any other <tt>Exception</tt>, the Configuration
     * Admin service must catch it and should log it.
     * <p>
     * The Configuration Admin service must call this method asynchronously
     * which initiated the callback. This implies that implementors of Managed
     * Service can be assured that the callback will not take place during
     * registration when they execute the registration in a synchronized method.
     * 
     * @param properties
     *            A copy of the Configuration properties, or <tt>null</tt>.
     *            This argument must not contain the "service.bundleLocation"
     *            property. The value of this property may be obtained from the
     *            <tt>Configuration.getBundleLocation</tt> method.
     * @throws ConfigurationException
     *             when the update fails
     */
    void updated(Dictionary properties) throws ConfigurationException;
}
