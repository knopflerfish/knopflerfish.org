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
 * Metadata for a Bean component.
 * 
 * <p>
 * This is specified by the {@code bean} element.
 * 
 * @ThreadSafe
 * @author $Id: 725928b126cb26462428e32024f18af7a0a40a4e $
 */
public interface BeanMetadata extends Target, ComponentMetadata {

	/**
	 * The bean has {@code singleton} scope.
	 * 
	 * @see #getScope()
	 */
	static final String	SCOPE_SINGLETON	= "singleton";

	/**
	 * The bean has {@code prototype} scope.
	 * 
	 * @see #getScope()
	 */
	static final String	SCOPE_PROTOTYPE	= "prototype";

	/**
	 * Return the name of the class specified for the bean.
	 * 
	 * This is specified by the {@code class} attribute of the bean definition.
	 * 
	 * @return The name of the class specified for the bean. If no class is
	 *         specified in the bean definition, because the a factory component
	 *         is used instead, then this method will return {@code null}.
	 */
	String getClassName();

	/**
	 * Return the name of the init method specified for the bean.
	 * 
	 * This is specified by the {@code init-method} attribute of the bean
	 * definition.
	 * 
	 * @return The name of the init method specified for the bean, or
	 *         {@code null} if no init method is specified.
	 */
	String getInitMethod();

	/**
	 * Return the name of the destroy method specified for the bean.
	 * 
	 * This is specified by the {@code destroy-method} attribute of the bean
	 * definition.
	 * 
	 * @return The name of the destroy method specified for the bean, or
	 *         {@code null} if no destroy method is specified.
	 */
	String getDestroyMethod();

	/**
	 * Return the arguments for the factory method or constructor of the bean.
	 * 
	 * This is specified by the child {@code argument} elements.
	 * 
	 * @return An immutable List of {@link BeanArgument} objects for the factory
	 *         method or constructor of the bean. The List is empty if no
	 *         arguments are specified for the bean.
	 */
	List<BeanArgument> getArguments();

	/**
	 * Return the properties for the bean.
	 * 
	 * This is specified by the child {@code property} elements.
	 * 
	 * @return An immutable List of {@link BeanProperty} objects, with one entry
	 *         for each property to be injected in the bean. The List is empty
	 *         if no property injection is specified for the bean.
	 * 
	 */
	List<BeanProperty> getProperties();

	/**
	 * Return the name of the factory method for the bean.
	 * 
	 * This is specified by the {@code factory-method} attribute of the bean.
	 * 
	 * @return The name of the factory method of the bean or {@code null} if no
	 *         factory method is specified for the bean.
	 */
	String getFactoryMethod();

	/**
	 * Return the Metadata for the factory component on which to invoke the
	 * factory method for the bean.
	 * 
	 * This is specified by the {@code factory-ref} attribute of the bean.
	 * 
	 * <p>
	 * When a factory method and factory component have been specified for the
	 * bean, this method returns the factory component on which to invoke the
	 * factory method for the bean. When no factory component has been specified
	 * this method will return {@code null}.
	 * 
	 * When a factory method has been specified for the bean but a factory
	 * component has not been specified, the factory method must be invoked as a
	 * static method on the bean's class.
	 * 
	 * @return The Metadata for the factory component on which to invoke the
	 *         factory method for the bean or {@code null} if no factory
	 *         component is specified.
	 */
	Target getFactoryComponent();

	/**
	 * Return the scope for the bean.
	 * 
	 * @return The scope for the bean. Returns {@code null} if the scope has not
	 *         been explicitly specified in the bean definition.
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	String getScope();
}
