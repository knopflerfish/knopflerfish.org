/*
 * Copyright (c) OSGi Alliance (2000, 2013). All Rights Reserved.
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

package org.osgi.service.device;

import org.osgi.framework.ServiceReference;

/**
 * <p>
 * Interface for identifying device services.
 * 
 * <p>
 * A service must implement this interface or use the
 * {@link Constants#DEVICE_CATEGORY} registration property to indicate that it
 * is a device. Any services implementing this interface or registered with the
 * {@code DEVICE_CATEGORY} property will be discovered by the device manager.
 * 
 * <p>
 * Device services implementing this interface give the device manager the
 * opportunity to indicate to the device that no drivers were found that could
 * (further) refine it. In this case, the device manager calls the
 * {@link #noDriverFound()} method on the {@code Device} object.
 * 
 * <p>
 * Specialized device implementations will extend this interface by adding
 * methods appropriate to their device category to it.
 * 
 * @author $Id: a3f26e7ecae40c164624978e831b4dd27a857252 $
 * @see Driver
 * @ThreadSafe
 */
public interface Device {
	/**
	 * Return value from {@link Driver#match(ServiceReference)} indicating that
	 * the driver cannot refine the device presented to it by the device
	 * manager.
	 * 
	 * The value is zero.
	 */
	public static final int	MATCH_NONE	= 0;

	/**
	 * Indicates to this {@code Device} object that the device manager has
	 * failed to attach any drivers to it.
	 * 
	 * <p>
	 * If this {@code Device} object can be configured differently, the driver
	 * that registered this {@code Device} object may unregister it and register
	 * a different Device service instead.
	 */
	public void noDriverFound();
}
