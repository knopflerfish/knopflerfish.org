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
import java.util.Dictionary;

/**
 * The configuration information for a <tt>ManagedService</tt> or
 * <tt>ManagedServiceFactory</tt> object.
 * 
 * The Configuration Admin service uses this interface to represent the
 * configuration information for a <tt>ManagedService</tt> or for a service
 * instance of a <tt>ManagedServiceFactory</tt>.
 * 
 * <p>
 * A <tt>Configuration</tt> object contains a configuration dictionary and
 * allows the properties to be updated via this object. Bundles wishing to
 * receive configuration dictionaries do not need to use this class - they
 * register a <tt>ManagedService</tt> or <tt>ManagedServiceFactory</tt>.
 * Only administrative bundles, and bundles wishing to update their own
 * configurations need to use this class.
 * 
 * <p>
 * The properties handled in this configuration have case insensitive
 * <tt>String</tt> objects as keys. However, case is preserved from the last
 * set key/value. The value of the property may be of the following types:
 * 
 * <pre>
 *    type        ::=
 *       String     | Integer    | Long
 *     | Float      | Double     | Byte
 *     | Short      | Character  | Boolean
 *     | vector     | arrays
 *    primitive   ::= long | int | short | char | byte | double | float
 *    arrays      ::= primitive '[]' | type '[]' | null
 *    vector      ::= Vector of type or null
 * </pre>
 * 
 * <p>
 * This explicitly allows vectors and arrays of mixed types and containing
 * <tt>null</tt>.
 * <p>
 * A configuration can be <i>bound</i> to a bundle location (<tt>Bundle.getLocation()</tt>).
 * The purpose of binding a <tt>Configuration</tt> object to a location is to
 * make it impossible for another bundle to forge a PID that would match this
 * configuration. When a configuration is bound to a specific location, and a
 * bundle with a different location registers a corresponding
 * <tt>ManagedService</tt> object or <tt>ManagedServiceFactory</tt> object,
 * then the configuration is not passed to the updated method of that object.
 * 
 * <p>
 * If a configuration's location is <tt>null</tt>, it is not yet bound to a
 * location. It will become bound to the location of the first bundle that
 * registers a <tt>ManagedService</tt> or <tt>ManagedServiceFactory</tt>
 * object with the corresponding PID.
 * <p>
 * The same <tt>Configuration</tt> object is used for configuring both a
 * Managed Service Factory and a Managed Service. When it is important to
 * differentiate between these two the term "factory configuration" is used.
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */

public interface Configuration {

    /**
     * Get the PID for this <tt>Configuration</tt> object.
     * 
     * @return the PID for this <tt>Configuration</tt> object.
     * @throws IllegalStateException
     *             if this configuration has been deleted
     */

    String getPid();

    /**
     * Return the properties of this <tt>Configuration</tt> object.
     * 
     * The <tt>Dictionary</tt> object returned is a private copy for the
     * caller and may be changed without influencing the stored configuration.
     * The keys in the returned dictionary are case insensitive and are always
     * of type <tt>String</tt>.
     * 
     * <p>
     * If called just after the configuration is created and before update has
     * been called, this method returns <tt>null</tt>.
     * 
     * @return A private copy of the properties for the caller or <tt>null</tt>.
     *         These properties must not contain the "service.bundleLocation"
     *         property. The value of this property may be obtained from the
     *         <tt>getBundleLocation</tt> method.
     * @throws IllegalStateException
     *             if this configuration has been deleted
     */
    Dictionary getProperties();

    /**
     * Update the properties of this <tt>Configuration</tt> object.
     * 
     * Stores the properties in persistent storage after adding or overwriting
     * the following properties:
     * <ul>
     * <li>"service.pid" : is set to be the PID of this configuration.</li>
     * <li>"service.factoryPid" : if this is a factory configuration it is set
     * to the factory PID else it is not set.</li>
     * </ul>
     * These system properties are all of type <tt>String</tt>.
     * 
     * <p>
     * If the corresponding Managed Service/Managed Service Factory is
     * registered, its updated method must be called asynchronously. Else, this
     * callback is delayed until aforementioned registration occurs.
     * 
     * @param properties
     *            the new set of properties for this configuration
     * @throws IOException
     *             if update cannot be made persistent
     * @throws IllegalArgumentException
     *             if the <tt>Dictionary</tt> object contains invalid
     *             configuration types
     * @throws IllegalStateException
     *             if this configuration has been deleted
     */
    void update(Dictionary properties) throws IOException;

    /**
     * Delete this <tt>Configuration</tt> object.
     * 
     * Removes this configuration object from the persistent store. Notify
     * asynchronously the corresponding Managed Service or Managed Service
     * Factory. A <tt>ManagedService</tt> object is notified by a call to its
     * <tt>updated</tt> method with a <tt>null</tt> properties argument. A
     * <tt>ManagedServiceFactory</tt> object is notified by a call to its
     * <tt>deleted</tt> method.
     * 
     * @throws IOException
     *             If delete fails
     * @throws IllegalStateException
     *             if this configuration has been deleted
     */
    void delete() throws IOException;

    /**
     * For a factory configuration return the PID of the corresponding Managed
     * Service Factory, else return <tt>null</tt>.
     * 
     * @return factory PID or <tt>null</tt>
     * @throws IllegalStateException
     *             if this configuration has been deleted
     */
    String getFactoryPid();

    /**
     * Update the <tt>Configuration</tt> object with the current properties.
     * 
     * Initiate the <tt>updated</tt> callback to the Managed Service or
     * Managed Service Factory with the current properties asynchronously.
     * 
     * <p>
     * This is the only way for a bundle that uses a Configuration Plugin
     * service to initate a callback. For example, when that bundle detects a
     * change that requires an update of the Managed Service or Managed Service
     * Factory via its <tt>ConfigurationPlugin</tt> object.
     * 
     * @see ConfigurationPlugin
     * @throws IOException
     *             if update cannot access the properties in persistent storage
     * @throws IllegalStateException
     *             if this configuration has been deleted
     */
    void update() throws IOException;

    /**
     * Bind this <tt>Configuration</tt> object to the specified bundle
     * location.
     * 
     * If the bundleLocation parameter is <tt>null</tt> then the
     * <tt>Configuration</tt> object will not be bound to a location. It will
     * be set to the bundle's location before the first time a Managed
     * Service/Managed Service Factory receives this <tt>Configuration</tt>
     * object via the updated method and before any plugins are called. The
     * bundle location will be set persistently.
     * 
     * <p>
     * This method requires <tt>AdminPermission</tt>.
     * 
     * @param bundleLocation
     *            a bundle location or <tt>null</tt>
     * @throws SecurityException
     *             if the caller does not have <tt>AdminPermission</tt>
     * @throws IllegalStateException
     *             if this configuration has been deleted
     */
    void setBundleLocation(String bundleLocation);

    /**
     * Get the bundle location.
     * 
     * Returns the bundle location to which this configuration is bound, or
     * <tt>null</tt> if it is not yet bound to a bundle location.
     * <p>
     * This call requires <tt>AdminPermission</tt>.
     * 
     * @return location to which this configuration is bound, or <tt>null</tt>.
     * @throws SecurityException
     *             if the caller does not have <tt>AdminPermission</tt>.
     * @throws IllegalStateException
     *             if this <tt>Configuration</tt> object has been deleted.
     */
    String getBundleLocation();

    /**
     * Equality is defined to have equal PIDs
     * 
     * Two Configuration objects are equal when their PIDs are equal.
     * 
     * @param other
     *            <tt>Configuration</tt> object to compare against
     * @return <tt>true</tt> if equal, <tt>false</tt> if not a
     *         <tt>Configuration</tt> object or one with a different PID.
     */
    boolean equals(Object other);

    /**
     * Hash code is based on PID.
     * 
     * The hashcode for two Configuration objects must be the same when the
     * Configuration PID's are the same.
     * 
     * @return hash code for this Configuration object
     */
    int hashCode();
}
