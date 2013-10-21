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

import java.util.Dictionary;

/**
 * The base interface for {@code Role} objects managed by the User Admin
 * service.
 * 
 * <p>
 * This interface exposes the characteristics shared by all {@code Role}
 * classes: a name, a type, and a set of properties.
 * <p>
 * Properties represent public information about the {@code Role} object that
 * can be read by anyone. Specific {@link UserAdminPermission} objects are
 * required to change a {@code Role} object's properties.
 * <p>
 * {@code Role} object properties are {@code Dictionary} objects. Changes to
 * these objects are propagated to the User Admin service and made persistent.
 * <p>
 * Every User Admin service contains a set of predefined {@code Role} objects
 * that are always present and cannot be removed. All predefined {@code Role}
 * objects are of type {@code ROLE}. This version of the
 * {@code org.osgi.service.useradmin} package defines a single predefined role
 * named &quot;user.anyone&quot;, which is inherited by any other role. Other
 * predefined roles may be added in the future. Since &quot;user.anyone&quot; is
 * a {@code Role} object that has properties associated with it that can be read
 * and modified. Access to these properties and their use is application
 * specific and is controlled using {@code UserAdminPermission} in the same way
 * that properties for other {@code Role} objects are.
 * 
 * @noimplement
 * @author $Id: c366d5a634158a41a940edb0229ee95d4148ddac $
 */
public interface Role {
	/**
	 * The name of the predefined role, user.anyone, that all users and groups
	 * belong to.
	 * 
	 * @since 1.1
	 */
	public static final String	USER_ANYONE	= "user.anyone";
	/**
	 * The type of a predefined role.
	 * 
	 * <p>
	 * The value of {@code ROLE} is 0.
	 */
	public static final int		ROLE		= 0;
	/**
	 * The type of a {@link User} role.
	 * 
	 * <p>
	 * The value of {@code USER} is 1.
	 */
	public static final int		USER		= 1;
	/**
	 * The type of a {@link Group} role.
	 * 
	 * <p>
	 * The value of {@code GROUP} is 2.
	 */
	public static final int		GROUP		= 2;

	/**
	 * Returns the name of this role.
	 * 
	 * @return The role's name.
	 */
	public String getName();

	/**
	 * Returns the type of this role.
	 * 
	 * @return The role's type.
	 */
	public int getType();

	/**
	 * Returns a {@code Dictionary} of the (public) properties of this
	 * {@code Role} object. Any changes to the returned {@code Dictionary} will
	 * change the properties of this {@code Role} object. This will cause a
	 * {@code UserAdminEvent} object of type {@link UserAdminEvent#ROLE_CHANGED}
	 * to be broadcast to any {@code UserAdminListener} objects.
	 * 
	 * <p>
	 * Only objects of type {@code String} may be used as property keys, and
	 * only objects of type {@code String} or {@code byte[]} may be used as
	 * property values. Any other types will cause an exception of type
	 * {@code IllegalArgumentException} to be raised.
	 * 
	 * <p>
	 * In order to add, change, or remove a property in the returned
	 * {@code Dictionary}, a {@link UserAdminPermission} named after the
	 * property name (or a prefix of it) with action {@code changeProperty} is
	 * required.
	 * 
	 * @return {@code Dictionary} containing the properties of this {@code Role}
	 *         object.
	 */
	public Dictionary getProperties();
}
