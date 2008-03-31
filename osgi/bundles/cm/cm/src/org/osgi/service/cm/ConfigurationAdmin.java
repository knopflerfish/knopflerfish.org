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

import java.io.IOException;

import org.osgi.framework.InvalidSyntaxException;

/**
 * Service for administering configuration data.
 * 
 * <p>
 * The main purpose of this interface is to store bundle configuration data
 * persistently. This information is represented in <tt>Configuration</tt>
 * objects. The actual configuration data is a <tt>Dictionary</tt> of
 * properties inside a <tt>Configuration</tt> object.
 * 
 * <p>
 * There are two principally different ways to manage configurations. First
 * there is the concept of a Managed Service, where configuration data is
 * uniquely associated with an object registered with the service registry.
 * 
 * <p>
 * Next, there is the concept of a factory where the Configuration Admin service
 * will maintain 0 or more <tt>Configuration</tt> objects for a Managed
 * Service Factory that is registered with the Framework.
 * 
 * <p>
 * The first concept is intended for configuration data about "things/services"
 * whose existence is defined externally, e.g. a specific printer. Factories are
 * intended for "things/services" that can be created any number of times, e.g.
 * a configuration for a DHCP server for different networks.
 * 
 * <p>
 * Bundles that require configuration should register a Managed Service or a
 * Managed Service Factory in the service registry. A registration property
 * named <tt>service.pid</tt> (persistent identifier or PID) must be used to
 * identify this Managed Service or Managed Service Factory to the Configuration
 * Admin service.
 * 
 * <p>
 * When the ConfigurationAdmin detects the registration of a Managed Service, it
 * checks its persistent storage for a configuration object whose PID matches
 * the PID registration property (<tt>service.pid</tt>) of the Managed
 * Service. If found, it calls {@link ManagedService#updated} method with the
 * new properties. The implementation of a Configuration Admin service must run
 * these call-backs asynchronously to allow proper synchronization.
 * 
 * <p>
 * When the Configuration Admin service detects a Managed Service Factory
 * registration, it checks its storage for configuration objects whose
 * <tt>factoryPid</tt> matches the PID of the Managed Service Factory. For
 * each such <tt>Configuration</tt> objects, it calls the
 * <tt>ManagedServiceFactory.updated</tt> method asynchronously with the new
 * properties. The calls to the <tt>updated</tt> method of a
 * <tt>ManagedServiceFactory</tt> must be executed sequentially and not
 * overlap in time.
 * 
 * <p>
 * In general, bundles having permission to use the Configuration Admin service
 * can only access and modify their own configuration information. Accessing or
 * modifying the configuration of another bundle requires
 * <tt>AdminPermission</tt>.
 * 
 * <p>
 * <tt>Configuration</tt> objects can be <i>bound</i> to a specified bundle
 * location. In this case, if a matching Managed Service or Managed Service
 * Factory is registered by a bundle with a different location, then the
 * Configuration Admin service must not do the normal callback, and it should
 * log an error. In the case where a <tt>Configuration</tt> object is not
 * bound, its location field is <tt>null</tt>, the Configuration Admin
 * service will bind it to the location of the bundle that registers the first
 * Managed Service or Managed Service Factory that has a corresponding PID
 * property. When a <tt>Configuration</tt> object is bound to a bundle
 * location in this manner, the Confguration Admin service must detect if the
 * bundle corresponding to the location is uninstalled. If this occurs, the
 * <tt>Configuration</tt> object is unbound, that is its location field is set
 * back to <tt>null</tt>.
 * 
 * <p>
 * The method descriptions of this class refer to a concept of "the calling
 * bundle". This is a loose way of referring to the bundle which obtained the
 * Configuration Admin service from the service registry. Implementations of
 * <tt>ConfigurationAdmin</tt> must use a
 * {@link org.osgi.framework.ServiceFactory} to support this concept.
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */

public interface ConfigurationAdmin {
    /**
     * Service property naming the Factory PID in the configuration dictionary.
     * The property's value is of type <tt>String</tt>.
     * 
     * @since 1.1
     */
    public final static String SERVICE_FACTORYPID = "service.factoryPid";

    /**
     * Service property naming the location of the bundle that is associated
     * with a a <tt>Configuration</tt> object. This property can be searched
     * for but must not appear in the configuration dictionary for security
     * reason. The property's value is of type <tt>String</tt>.
     * 
     * @since 1.1
     */
    public final static String SERVICE_BUNDLELOCATION = "service.bundleLocation";

    /**
     * Create a new factory <tt>Configuration</tt> object with a new PID.
     * 
     * The properties of the new <tt>Configuration</tt> object are
     * <tt>null</tt> until the first time that its
     * {@link Configuration#update} method is called.
     * 
     * <p>
     * It is not required that the <tt>factoryPid</tt> maps to a registered
     * Managed Service Factory.
     * <p>
     * The <tt>Configuration</tt> object is bound to the location of the
     * calling bundle.
     * 
     * @param factoryPid
     *            PID of factory (not <tt>null</tt>).
     * @return a new <tt>Configuration</tt> object.
     * @throws IOException
     *             if access to persistent storage fails.
     * @throws SecurityException
     *             if caller does not have <tt>AdminPermission</tt> and
     *             <tt>factoryPid</tt> is bound to another bundle.
     */
    Configuration createFactoryConfiguration(String factoryPid)
            throws IOException;

    /**
     * Create a new factory <tt>Configuration</tt> object with a new PID.
     * 
     * The properties of the new <tt>Configuration</tt> object are
     * <tt>null</tt> until the first time that its
     * {@link Configuration#update} method is called.
     * 
     * <p>
     * It is not required that the <tt>factoryPid</tt> maps to a registered
     * Managed Service Factory.
     * 
     * <p>
     * The <tt>Configuration</tt> is bound to the location specified. If this
     * location is <tt>null</tt> it will be bound to the location of the first
     * bundle that registers a Managed Service Factory with a corresponding PID.
     * 
     * <p>
     * This method requires <tt>AdminPermission</tt>.
     * 
     * @param factoryPid
     *            PID of factory (not <tt>null</tt>).
     * @param location
     *            a bundle location string, or <tt>null</tt>.
     * @return a new <tt>Configuration</tt> object.
     * @throws IOException
     *             if access to persistent storage fails.
     * @throws SecurityException
     *             if caller does not have <tt>AdminPermission</tt>.
     */
    Configuration createFactoryConfiguration(String factoryPid, String location)
            throws IOException;

    /**
     * Get an existing <tt>Configuration</tt> object from the persistent
     * store, or create a new <tt>Configuration</tt> object.
     * 
     * <p>
     * If a <tt>Configuration</tt> with this PID already exists in
     * Configuration Admin service return it. The location parameter is ignored
     * in this case.
     * 
     * <p>
     * Else, return a new <tt>Configuration</tt> object. This new object is
     * bound to the location and the properties are set to <tt>null</tt>. If
     * the location parameter is <tt>null</tt>, it will be set when a Managed
     * Service with the corresponding PID is registered for the first time.
     * 
     * <p>
     * This method requires <tt>AdminPermission</tt>.
     * 
     * @param pid
     *            persistent identifier.
     * @param location
     *            the bundle location string, or <tt>null</tt>.
     * @return an existing or new <tt>Configuration</tt> object.
     * @throws IOException
     *             if access to persistent storage fails.
     * @throws SecurityException
     *             if the caller does not have <tt>AdminPermission</tt>.
     */
    Configuration getConfiguration(String pid, String location)
            throws IOException;

    /**
     * Get an existing or new <tt>Configuration</tt> object from the
     * persistent store.
     * 
     * If the <tt>Configuration</tt> object for this PID does not exist,
     * create a new <tt>Configuration</tt> object for that PID, where
     * properties are <tt>null</tt>. Bind its location to the calling
     * bundle's location.
     * 
     * <p>
     * Else, if the location of the existing <tt>Configuration</tt> object is
     * <tt>null</tt>, set it to the calling bundle's location.
     * <p>
     * If the location of the <tt>Configuration</tt> object does not match the
     * calling bundle, throw a <tt>SecurityException</tt>.
     * 
     * @param pid
     *            persistent identifier.
     * @return an existing or new <tt>Configuration</tt> matching the PID.
     * @throws IOException
     *             if access to persistent storage fails.
     * @throws SecurityException
     *             if the <tt>Configuration</tt> object is bound to a location
     *             different from that of the calling bundle and it has no
     *             <tt>AdminPermission</tt>.
     */
    Configuration getConfiguration(String pid) throws IOException;

    /**
     * List the current <tt>Configuration</tt> objects which match the filter.
     * 
     * <p>
     * Only <tt>Configuration</tt> objects with non-<tt>null</tt>
     * properties are considered current. That is,
     * <tt>Configuration.getProperties()</tt> is guaranteed not to return
     * <tt>null</tt> for each of the returned <tt>Configuration</tt>
     * objects.
     * 
     * <p>
     * Normally only <tt>Configuration</tt> objects that are bound to the
     * location of the calling bundle are returned. If the caller has
     * <tt>AdminPermission</tt>, then all matching <tt>Configuration</tt>
     * objects are returned.
     * 
     * <p>
     * The syntax of the filter string is as defined in the <tt>Filter</tt>
     * class. The filter can test any configuration parameters including the
     * following system properties:
     * <ul>
     * <li><tt>service.pid</tt> - <tt>String</tt> - the PID under which
     * this is registered</li>
     * <li><tt>service.factoryPid</tt> - <tt>String</tt> - the factory if
     * applicable</li>
     * <li><tt>service.bundleLocation</tt> - <tt>String</tt> - the bundle
     * location</li>
     * </ul>
     * The filter can also be <tt>null</tt>, meaning that all
     * <tt>Configuration</tt> objects should be returned.
     * 
     * @param filter
     *            a <tt>Filter</tt> object, or <tt>null</tt> to retrieve all
     *            <tt>Configuration</tt> objects.
     * @return all matching <tt>Configuration</tt> objects, or <tt>null</tt>
     *         if there aren't any
     * @throws IOException
     *             if access to persistent storage fails
     * @throws InvalidSyntaxException
     *             if the filter string is invalid
     */
    Configuration[] listConfigurations(String filter) throws IOException,
            InvalidSyntaxException;
}
