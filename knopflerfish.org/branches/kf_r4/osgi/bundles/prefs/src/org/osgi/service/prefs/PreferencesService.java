/*
 * $Header: /cvshome/build/org.osgi.service.prefs/src/org/osgi/service/prefs/PreferencesService.java,v 1.8 2005/08/11 03:07:45 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.osgi.service.prefs;

/**
 * The Preferences Service.
 * 
 * <p>
 * Each bundle using this service has its own set of preference trees: one for
 * system preferences, and one for each user.
 * 
 * <p>
 * A <code>PreferencesService</code> object is specific to the bundle which
 * obtained it from the service registry. If a bundle wishes to allow another
 * bundle to access its preferences, it should pass its
 * <code>PreferencesService</code> object to that bundle.
 *  
 */
public interface PreferencesService {
	/**
	 * Returns the root system node for the calling bundle.
	 * 
	 * @return The root system node for the calling bundle.
	 */
	public Preferences getSystemPreferences();

	/**
	 * Returns the root node for the specified user and the calling bundle.
	 * 
	 * @param name The user for which to return the preference root node. 
	 * @return The root node for the specified user and the calling bundle.
	 */
	public Preferences getUserPreferences(String name);

	/**
	 * Returns the names of users for which node trees exist.
	 * 
	 * @return The names of users for which node trees exist.
	 */
	public String[] getUsers();
}
