/*
 * $Header: /cvshome/build/org.osgi.service.event/src/org/osgi/service/event/Event.java,v 1.4 2005/05/13 20:33:29 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2005). All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.event;

import java.util.*;

import org.osgi.framework.Filter;

/**
 * An event.
 *
 * <p>
 * <code>Event</code> objects are delivered to <code>EventHandler</code>
 * services which subsrcibe to the topic of the event.
 *
 * <p>
 * <i>NOTE: Although it is permitted to subclass <code>Event</code>, the
 * operations defined by this class MUST NOT be overridden. </i>
 *
 * @version $Revision: 1.4 $
 */
public class Event {
	/**
	 * The topic of this event.
	 */
	String	topic;
	/**
	 * The properties carried by this event. Keys are strings and values are
	 * objects
	 */
	Hashtable	properties;

	/**
	 * Constructs an event.
	 *
	 * @param topic The topic of the event.
	 * @param properties The event's properties (may be <code>null</code>).
	 *
	 * @throws IllegalArgumentException If topic is not a valid topic name.
	 */
	public Event(String topic, Dictionary properties) {
		this.topic = topic;
		validateTopicName();
		this.properties = new Hashtable();
		if (properties != null) {
			for (Enumeration e = properties.keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				Object value = properties.get(key);
				this.properties.put(key, value);
			}
		}
		this.properties.put(EventConstants.EVENT_TOPIC, topic);
	}

	/**
	 * Retrieves a property.
	 *
	 * @param name the name of the property to retrieve
	 *
	 * @return The value of the property, or <code>null</code> if not found.
	 */
	public final Object getProperty(String name) {
		return properties.get(name);
	}

	/**
	 * Returns a list of this event's property names.
	 *
	 * @return A non-empty array with one element per property.
	 */
	public final String[] getPropertyNames() {
		String[] names = new String[properties.size()];
		Enumeration keys = properties.keys();
		for (int i = 0; keys.hasMoreElements(); i++) {
			names[i] = (String) keys.nextElement();
		}
		return names;
	}

	/**
	 * Returns the topic of this event.
	 *
	 * @return The topic of this event.
	 */
	public final String getTopic() {
		return topic;
	}

	/**
	 * Tests this event's properties against the given filter.
	 *
	 * @param filter The filter to test.
	 *
	 * @return true If this event's properties match the filter, false
	 *         otherwise.
	 */
	public final boolean matches(Filter filter) {
		return filter.matchCase(properties);
		
	}

	/**
	 * Compares this <code>Event</code> object to another object.
	 *
	 * <p>
	 * An event is considered to be <b>equal to </b> another
	 * event if the topic is equal and the properties are equal.
	 *
	 * @param object The <code>Event</code> object to be compared.
	 * @return <code>true</code> if <code>object</code> is a
	 *         <code>Event</code> and is equal to this object;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object object) {
		if (object == this) { // quicktest
			return true;
		}

		if (!(object instanceof Event)) {
			return false;
		}

		Event event = (Event) object;
		return topic.equals(event.topic) && properties.equals(event.properties);
	}

	/**
	 * Returns a hash code value for the object.
	 *
	 * @return An integer which is a hash code value for this object.
	 */
	public int hashCode() {
		return topic.hashCode() ^ properties.hashCode();
	}

	/**
	 * Returns the string representation of this event.
	 *
	 * @return The string representation of this event.
	 */
	public String toString() {
		return getClass().getName() + " [topic=" + topic + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static final String	SEPARATOR	= "/"; //$NON-NLS-1$

	/**
	 * Called by the constructor to validate the topic name.
	 *
	 * @throws IllegalArgumentException If the topic name is invalid.
	 */
	private void validateTopicName() {
		try {
			StringTokenizer st = new StringTokenizer(topic, SEPARATOR, true);
			validateToken(st.nextToken());

			while (st.hasMoreTokens()) {
				st.nextToken(); // consume delimiter
				validateToken(st.nextToken());
			}
		}
		catch (NoSuchElementException e) {
			throw new IllegalArgumentException("invalid topic"); //$NON-NLS-1$
		}
	}

	private static final String	alphaGrammar	= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"; //$NON-NLS-1$
	private static final String	tokenGrammar	= alphaGrammar + "0123456789_"; //$NON-NLS-1$

	/**
	 * Validate a token.
	 *
	 * @throws IllegalArgumentException If the token is invalid.
	 */
	private void validateToken(String token) {
		int length = token.length();
		if (length < 1) {
			throw new IllegalArgumentException("invalid topic"); //$NON-NLS-1$
		}
		if (alphaGrammar.indexOf(token.charAt(0)) == -1) { //$NON-NLS-1$
			throw new IllegalArgumentException("invalid topic"); //$NON-NLS-1$
		}
		for (int i = 1; i < length; i++) {
			if (tokenGrammar.indexOf(token.charAt(i)) == -1) { //$NON-NLS-1$
				throw new IllegalArgumentException("invalid topic"); //$NON-NLS-1$
			}
		}
	}
}
