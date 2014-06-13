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
 * Metadata for the verified id of another component managed by the Blueprint
 * Container. The id itself will be injected, not the component to which the id
 * refers. No implicit dependency is created.
 * 
 * @ThreadSafe
 * @author $Id: 9903067b9248e6f6ba2431c5cb166bc9833fc3e1 $
 */
public interface IdRefMetadata extends NonNullMetadata {
	/**
	 * Return the id of the referenced component.
	 * 
	 * This is specified by the {@code component-id} attribute of a component.
	 * 
	 * @return The id of the referenced component.
	 */
	String getComponentId();
}
