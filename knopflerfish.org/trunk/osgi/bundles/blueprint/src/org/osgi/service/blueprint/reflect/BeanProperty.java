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

/**
 * Metadata for a property to be injected into a bean. The properties of a bean
 * are obtained from {@link BeanMetadata#getProperties()}.
 * 
 * This is specified by the {@code property} elements of a bean. Properties are
 * defined according to the Java Beans conventions.
 * 
 * @ThreadSafe
 * @author $Id: 698d624c9385b8c80d7f8d977cf4f710c0e1864b $
 */
public interface BeanProperty {

	/**
	 * Return the name of the property to be injected. The name follows Java
	 * Beans conventions.
	 * 
	 * This is specified by the {@code name} attribute.
	 * 
	 * @return The name of the property to be injected.
	 */
	String getName();

	/**
	 * Return the Metadata for the value to be injected into a bean.
	 * 
	 * This is specified by the {@code value} attribute or in inlined text.
	 * 
	 * @return The Metadata for the value to be injected into a bean.
	 */
	Metadata getValue();
}
