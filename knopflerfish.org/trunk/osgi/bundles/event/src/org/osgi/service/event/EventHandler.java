/*
 * Copyright (c) OSGi Alliance (2005, 2013). All Rights Reserved.
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

package org.osgi.service.event;

/**
 * Listener for Events.
 * 
 * <p>
 * {@code EventHandler} objects are registered with the Framework service
 * registry and are notified with an {@code Event} object when an event is sent
 * or posted.
 * <p>
 * {@code EventHandler} objects can inspect the received {@code Event} object to
 * determine its topic and properties.
 * 
 * <p>
 * {@code EventHandler} objects must be registered with a service property
 * {@link EventConstants#EVENT_TOPIC} whose value is the list of topics in which
 * the event handler is interested.
 * <p>
 * For example:
 * 
 * <pre>
 * String[] topics = new String[] {&quot;com/isv/*&quot;};
 * Hashtable ht = new Hashtable();
 * ht.put(EventConstants.EVENT_TOPIC, topics);
 * context.registerService(EventHandler.class.getName(), this, ht);
 * </pre>
 * 
 * Event Handler services can also be registered with an
 * {@link EventConstants#EVENT_FILTER} service property to further filter the
 * events. If the syntax of this filter is invalid, then the Event Handler must
 * be ignored by the Event Admin service. The Event Admin service should log a
 * warning.
 * <p>
 * Security Considerations. Bundles wishing to monitor {@code Event} objects
 * will require {@code ServicePermission[EventHandler,REGISTER]} to register an
 * {@code EventHandler} service. The bundle must also have
 * {@code TopicPermission[topic,SUBSCRIBE]} for the topic specified in the event
 * in order to receive the event.
 * 
 * @see Event
 * 
 * @ThreadSafe
 * @author $Id: 44528634004c1b036551712f94703a8b5a55cba4 $
 */
public interface EventHandler {
	/**
	 * Called by the {@link EventAdmin} service to notify the listener of an
	 * event.
	 * 
	 * @param event The event that occurred.
	 */
	void handleEvent(Event event);
}
