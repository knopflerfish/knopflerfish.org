/*
 * Copyright (c) OSGi Alliance (2001, 2013). All Rights Reserved.
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

package org.osgi.service.useradmin;

/**
 * Listener for UserAdminEvents.
 * 
 * <p>
 * {@code UserAdminListener} objects are registered with the Framework service
 * registry and notified with a {@code UserAdminEvent} object when a
 * {@code Role} object has been created, removed, or modified.
 * <p>
 * {@code UserAdminListener} objects can further inspect the received
 * {@code UserAdminEvent} object to determine its type, the {@code Role} object
 * it occurred on, and the User Admin service that generated it.
 * 
 * @see UserAdmin
 * @see UserAdminEvent
 * 
 * @author $Id: 3d45a44b22bc49e707be0bf62fe4efcda4420eb3 $
 */
public interface UserAdminListener {
	/**
	 * Receives notification that a {@code Role} object has been created,
	 * removed, or modified.
	 * 
	 * @param event The {@code UserAdminEvent} object.
	 */
	public void roleChanged(UserAdminEvent event);
}
