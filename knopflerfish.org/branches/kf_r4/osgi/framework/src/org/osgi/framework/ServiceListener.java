/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/ServiceListener.java,v 1.12 2006/06/16 16:31:18 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2006). All Rights Reserved.
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
 * A <code>ServiceEvent</code> listener. When a <code>ServiceEvent</code> is
 * fired, it is synchronously delivered to a <code>BundleListener</code>.
 * 
 * <p>
 * <code>ServiceListener</code> is a listener interface that may be
 * implemented by a bundle developer.
 * <p>
 * A <code>ServiceListener</code> object is registered with the Framework
 * using the <code>BundleContext.addServiceListener</code> method.
 * <code>ServiceListener</code> objects are called with a
 * <code>ServiceEvent</code> object when a service is registered, modified, or
 * is in the process of unregistering.
 * 
 * <p>
 * <code>ServiceEvent</code> object delivery to <code>ServiceListener</code>
 * objects is filtered by the filter specified when the listener was registered.
 * If the Java Runtime Environment supports permissions, then additional
 * filtering is done. <code>ServiceEvent</code> objects are only delivered to
 * the listener if the bundle which defines the listener object's class has the
 * appropriate <code>ServicePermission</code> to get the service using at
 * least one of the named classes the service was registered under.
 * 
 * <p>
 * <code>ServiceEvent</code> object delivery to <code>ServiceListener</code>
 * objects is further filtered according to package sources as defined in
 * {@link ServiceReference#isAssignableTo(Bundle, String)}.
 * 
 * @version $Revision: 1.12 $
 * @see ServiceEvent
 * @see ServicePermission
 */

public interface ServiceListener extends EventListener {
	/**
	 * Receives notification that a service has had a lifecycle change.
	 * 
	 * @param event The <code>ServiceEvent</code> object.
	 */
	public void serviceChanged(ServiceEvent event);
}
