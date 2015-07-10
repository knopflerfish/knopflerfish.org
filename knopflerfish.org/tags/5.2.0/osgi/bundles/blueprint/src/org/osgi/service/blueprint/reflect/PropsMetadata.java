/*
 * Copyright (c) OSGi Alliance (2008, 2013). All Rights Reserved.
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

package org.osgi.service.blueprint.reflect;

import java.util.List;

/**
 * Metadata for a {@code java.util.Properties} based value.
 * 
 * <p>
 * The {@link MapEntry} objects of properties are defined with keys and values
 * of type {@code String}.
 * 
 * <p>
 * This is specified by the {@code props} element.
 * 
 * @ThreadSafe
 * @author $Id: ae35d411f96d63b1f3cd3e48c33ed6d99ea722b1 $
 */
public interface PropsMetadata extends NonNullMetadata {

	/**
	 * Return the entries for the properties.
	 * 
	 * @return An immutable List of {@link MapEntry} objects for each entry in
	 *         the properties. The List is empty if no entries are specified for
	 *         the properties.
	 */
	List<MapEntry> getEntries();
}
