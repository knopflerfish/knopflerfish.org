/*
 * Copyright (c) OSGi Alliance (2011, 2013). All Rights Reserved.
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

package org.osgi.service.component.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identify the annotated method as the {@code deactivate} method of a Service
 * Component.
 * 
 * <p>
 * The annotated method is the deactivate method of the Component.
 * 
 * <p>
 * This annotation is not processed at runtime by a Service Component Runtime
 * implementation. It must be processed by tools and used to add a Component
 * Description to the bundle.
 * 
 * @see "The deactivate attribute of the component element of a Component Description."
 * @author $Id: 1809c70ebf50ecdb649b1804bcd48d21a626fa37 $
 * @since 1.1
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Deactivate {
	// marker annotation
}
