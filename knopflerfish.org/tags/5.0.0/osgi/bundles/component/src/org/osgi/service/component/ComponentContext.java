/*
 * Copyright (c) OSGi Alliance (2004, 2013). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.component;

import java.util.Dictionary;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A Component Context object is used by a component instance to interact with
 * its execution context including locating services by reference name. Each
 * component instance has a unique Component Context.
 * 
 * <p>
 * A component instance may have an activate method. If a component instance has
 * a suitable and accessible activate method, this method will be called when a
 * component configuration is activated. If the activate method takes a
 * {@code ComponentContext} argument, it will be passed the component instance's
 * Component Context object. If the activate method takes a
 * {@code BundleContext} argument, it will be passed the component instance's
 * Bundle Context object. If the activate method takes a {@code Map} argument,
 * it will be passed an unmodifiable Map containing the component properties.
 * 
 * <p>
 * A component instance may have a deactivate method. If a component instance
 * has a suitable and accessible deactivate method, this method will be called
 * when the component configuration is deactivated. If the deactivate method
 * takes a {@code ComponentContext} argument, it will be passed the component
 * instance's Component Context object. If the deactivate method takes a
 * {@code BundleContext} argument, it will be passed the component instance's
 * Bundle Context object. If the deactivate method takes a {@code Map} argument,
 * it will be passed an unmodifiable Map containing the component properties. If
 * the deactivate method takes an {@code int} or {@code Integer} argument, it
 * will be passed the reason code for the component instance's deactivation.
 * 
 * @ThreadSafe
 * @noimplement
 * @author $Id: 4f87f06a8d2bcbda855799d0323582eb6164f99b $
 */
public interface ComponentContext {
	/**
	 * Returns the component properties for this Component Context.
	 * 
	 * @return The properties for this Component Context. The Dictionary is read
	 *         only and cannot be modified.
	 */
	public Dictionary<String, Object> getProperties();

	/**
	 * Returns the service object for the specified reference name.
	 * 
	 * <p>
	 * If the cardinality of the reference is {@code 0..n} or {@code 1..n} and
	 * multiple services are bound to the reference, the service with the
	 * highest ranking (as specified in its {@code Constants.SERVICE_RANKING}
	 * property) is returned. If there is a tie in ranking, the service with the
	 * lowest service ID (as specified in its {@code Constants.SERVICE_ID}
	 * property); that is, the service that was registered first is returned.
	 * 
	 * @param name The name of a reference as specified in a {@code reference}
	 *        element in this component's description.
	 * @return A service object for the referenced service or {@code null} if
	 *         the reference cardinality is {@code 0..1} or {@code 0..n} and no
	 *         bound service is available.
	 * @throws ComponentException If the Service Component Runtime catches an
	 *         exception while activating the bound service.
	 */
	public Object locateService(String name);

	/**
	 * Returns the service object for the specified reference name and
	 * {@code ServiceReference}.
	 * 
	 * @param name The name of a reference as specified in a {@code reference}
	 *        element in this component's description.
	 * @param reference The {@code ServiceReference} to a bound service. This
	 *        must be a {@code ServiceReference} provided to the component via
	 *        the bind or unbind method for the specified reference name.
	 * @return A service object for the referenced service or {@code null} if
	 *         the specified {@code ServiceReference} is not a bound service for
	 *         the specified reference name.
	 * @throws ComponentException If the Service Component Runtime catches an
	 *         exception while activating the bound service.
	 */
	public Object locateService(String name, ServiceReference reference);

	/**
	 * Returns the service objects for the specified reference name.
	 * 
	 * @param name The name of a reference as specified in a {@code reference}
	 *        element in this component's description.
	 * @return An array of service objects for the referenced service or
	 *         {@code null} if the reference cardinality is {@code 0..1} or
	 *         {@code 0..n} and no bound service is available. If the reference
	 *         cardinality is {@code 0..1} or {@code 1..1} and a bound service
	 *         is available, the array will have exactly one element.
	 * @throws ComponentException If the Service Component Runtime catches an
	 *         exception while activating a bound service.
	 */
	public Object[] locateServices(String name);

	/**
	 * Returns the {@code BundleContext} of the bundle which contains this
	 * component.
	 * 
	 * @return The {@code BundleContext} of the bundle containing this
	 *         component.
	 */
	public BundleContext getBundleContext();

	/**
	 * If the component instance is registered as a service using the
	 * {@code servicefactory="true"} attribute, then this method returns the
	 * bundle using the service provided by the component instance.
	 * <p>
	 * This method will return {@code null} if:
	 * <ul>
	 * <li>The component instance is not a service, then no bundle can be using
	 * it as a service.</li>
	 * <li>The component instance is a service but did not specify the
	 * {@code servicefactory="true"} attribute, then all bundles using the
	 * service provided by the component instance will share the same component
	 * instance.</li>
	 * <li>The service provided by the component instance is not currently being
	 * used by any bundle.</li>
	 * </ul>
	 * 
	 * @return The bundle using the component instance as a service or
	 *         {@code null}.
	 */
	public Bundle getUsingBundle();

	/**
	 * Returns the Component Instance object for the component instance
	 * associated with this Component Context.
	 * 
	 * @return The Component Instance object for the component instance.
	 */
	public ComponentInstance getComponentInstance();

	/**
	 * Enables the specified component name. The specified component name must
	 * be in the same bundle as this component.
	 * 
	 * @param name The name of a component or {@code null} to indicate all
	 *        components in the bundle.
	 */
	public void enableComponent(String name);

	/**
	 * Disables the specified component name. The specified component name must
	 * be in the same bundle as this component.
	 * 
	 * @param name The name of a component.
	 */
	public void disableComponent(String name);

	/**
	 * If the component instance is registered as a service using the
	 * {@code service} element, then this method returns the service reference
	 * of the service provided by this component instance.
	 * <p>
	 * This method will return {@code null} if the component instance is not
	 * registered as a service.
	 * 
	 * @return The {@code ServiceReference} object for the component instance or
	 *         {@code null} if the component instance is not registered as a
	 *         service.
	 */
	public ServiceReference getServiceReference();
}
