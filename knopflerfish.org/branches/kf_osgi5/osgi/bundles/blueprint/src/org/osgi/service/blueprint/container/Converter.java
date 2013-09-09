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

package org.osgi.service.blueprint.container;

/**
 * Type converter to convert an object to a target type.
 * 
 * @ThreadSafe
 * @author $Id: baf2391dfd6e84d36ca0a50d4743da4dd62e5f41 $
 */
public interface Converter {

	/**
	 * Return if this converter is able to convert the specified object to the
	 * specified type.
	 * 
	 * @param sourceObject The source object {@code s} to convert.
	 * @param targetType The target type {@code T}.
	 * 
	 * @return {@code true} if the conversion is possible, {@code false}
	 *         otherwise.
	 */
	boolean canConvert(Object sourceObject, ReifiedType targetType);

	/**
	 * Convert the specified object to an instance of the specified type.
	 * 
	 * @param sourceObject The source object {@code s} to convert.
	 * @param targetType The target type {@code T}.
	 * @return An instance with a type that is assignable from targetType's raw
	 *         class
	 * @throws Exception If the conversion cannot succeed. This exception should
	 *         not be thrown when the {@link #canConvert(Object, ReifiedType)
	 *         canConvert} method has returned {@code true}.
	 */
	Object convert(Object sourceObject, ReifiedType targetType) throws Exception;
}
