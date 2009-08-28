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
 * Manage multiple service instances.
 * 
 * Bundles registering this interface are giving the Configuration Admin service
 * the ability to create and configure a number of instances of a service that
 * the implementing bundle can provide. For example, a bundle implementing a
 * DHCP server could be instantiated multiple times for different interfaces
 * using a factory.
 * 
 * <p>
 * Each of these <i>service instances</i> is represented, in the persistent
 * storage of the Configuration Admin service, by a factory
 * <tt>Configuration</tt> object that has a PID. When such a
 * <tt>Configuration</tt> is updated, the Configuration Admin service calls
 * the <tt>ManagedServiceFactory</tt> updated method with the new properties.
 * When <tt>updated</tt> is called with a new PID, the Managed Service Factory
 * should create a new factory instance based on these configuration properties.
 * When called with a PID that it has seen before, it should update that
 * existing service instance with the new configuration information.
 * 
 * <p>
 * In general it is expected that the implementation of this interface will
 * maintain a data structure that maps PIDs to the factory instances that it has
 * created. The semantics of a factory instance are defined by the Managed
 * Service Factory. However, if the factory instance is registered as a service
 * object with the service registry, its PID should match the PID of the
 * corresponding <tt>Configuration</tt> object (but it should <b>not</b> be
 * registered as a Managed Service!).
 * 
 * <p>
 * An example that demonstrates the use of a factory. It will create serial
 * ports under command of the Configuration Admin service.
 * 
 * <pre>
 *    class SerialPortFactory
 *      implements ManagedServiceFactory {
 *      ServiceRegistration registration;
 *      Hashtable ports;
 *      void start(BundleContext context) {
 *        Hashtable properties = new Hashtable();
 *        properties.put( Constants.SERVICE_PID,
 *          &quot;com.acme.serialportfactory&quot; );
 *        registration = context.registerService(
 *          ManagedServiceFactory.class.getName(),
 *          this,
 *          properties
 *        );
 *      }
 *      public void updated( String pid,
 *        Dictionary properties  ) {
 *        String portName = (String) properties.get(&quot;port&quot;);
 *        SerialPortService port =
 *          (SerialPort) ports.get( pid );
 *        if ( port == null ) {
 *          port = new SerialPortService();
 *          ports.put( pid, port );
 *          port.open();
 *        }
 *        if ( port.getPortName().equals(portName) )
 *          return;
 *        port.setPortName( portName );
 *      }
 *      public void deleted( String pid ) {
 *        SerialPortService port =
 *          (SerialPort) ports.get( pid );
 *        port.close();
 *        ports.remove( pid );
 *      }
 *      ...
 *    }
 * </pre>
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */
public interface ManagedServiceFactory {

    /**
     * Return a descriptive name of this factory.
     * 
     * @return the name for the factory, which might be localized
     */
    String getName();

    /**
     * Create a new instance, or update the configuration of an existing
     * instance.
     * 
     * If the PID of the <tt>Configuration</tt> object is new for the Managed
     * Service Factory, then create a new factory instance, using the
     * configuration <tt>properties</tt> provided. Else, update the service
     * instance with the provided <tt>properties</tt>.
     * 
     * <p>
     * If the factory instance is registered with the Framework, then the
     * configuration <tt>properties</tt> should be copied to its registry
     * properties. This is not mandatory and security sensitive properties
     * should obviously not be copied.
     * 
     * <p>
     * If this method throws any <tt>Exception</tt>, the Configuration Admin
     * service must catch it and should log it.
     * 
     * <p>
     * When the implementation of updated detects any kind of error in the
     * configuration properties, it should create a new
     * {@link ConfigurationException} which describes the problem.
     * 
     * <p>
     * The Configuration Admin service must call this method asynchronously.
     * This implies that implementors of the <tt>ManagedServiceFactory</tt>
     * class can be assured that the callback will not take place during
     * registration when they execute the registration in a synchronized method.
     * 
     * @param pid
     *            The PID for this configuration.
     * @param properties
     *            A copy of the configuration properties. This argument must not
     *            contain the service.bundleLocation" property. The value of
     *            this property may be obtained from the
     *            <tt>Configuration.getBundleLocation</tt> method.
     * @throws ConfigurationException
     *             when the configuration properties are invalid.
     */
    void updated(String pid, Dictionary properties)
            throws ConfigurationException;

    /**
     * Remove a factory instance.
     * 
     * Remove the factory instance associated with the PID. If the instance was
     * registered with the service registry, it should be unregistered.
     * <p>
     * If this method throws any <tt>Exception</tt>, the Configuration Admin
     * service must catch it and should log it.
     * <p>
     * The Configuration Admin service must call this method asynchronously.
     * 
     * @param pid
     *            the PID of the service to be removed
     */
    void deleted(String pid);
}
