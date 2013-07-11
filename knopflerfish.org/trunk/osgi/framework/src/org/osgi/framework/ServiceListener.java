/*
 * Copyright (c) OSGi Alliance (2000, 2010). All Rights Reserved.
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

package org.osgi.framework;

import java.util.EventListener;

/**
 * A {@code ServiceEvent} listener. {@code ServiceListener} is a
 * listener interface that may be implemented by a bundle developer. When a
 * {@code ServiceEvent} is fired, it is synchronously delivered to a
 * {@code ServiceListener}. The Framework may deliver
 * {@code ServiceEvent} objects to a {@code ServiceListener} out
 * of order and may concurrently call and/or reenter a
 * {@code ServiceListener}.
 * 
 * <p>
 * A {@code ServiceListener} object is registered with the Framework
 * using the {@code BundleContext.addServiceListener} method.
 * {@code ServiceListener} objects are called with a
 * {@code ServiceEvent} object when a service is registered, modified, or
 * is in the process of unregistering.
 * 
 * <p>
 * {@code ServiceEvent} object delivery to {@code ServiceListener}
 * objects is filtered by the filter specified when the listener was registered.
 * If the Java Runtime Environment supports permissions, then additional
 * filtering is done. {@code ServiceEvent} objects are only delivered to
 * the listener if the bundle which defines the listener object's class has the
 * appropriate {@code ServicePermission} to get the service using at
 * least one of the named classes under which the service was registered.
 * 
 * <p>
 * {@code ServiceEvent} object delivery to {@code ServiceListener}
 * objects is further filtered according to package sources as defined in
 * {@link ServiceReference#isAssignableTo(Bundle, String)}.
 * 
 * @see ServiceEvent
 * @see ServicePermission
 * @ThreadSafe
 * @version $Id: d73f8e9b4babc8b53b5d1cbe7b17b732f54bb2a3 $
 */

public interface ServiceListener extends EventListener {
	/**
	 * Receives notification that a service has had a lifecycle change.
	 * 
	 * @param event The {@code ServiceEvent} object.
	 */
	public void serviceChanged(ServiceEvent event);
}
