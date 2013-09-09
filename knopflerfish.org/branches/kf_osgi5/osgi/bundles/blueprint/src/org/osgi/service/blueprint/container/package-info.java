/*
 * Copyright (c) OSGi Alliance (2010, 2012). All Rights Reserved.
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

/**
 * Blueprint Container Package Version 1.0.
 * 
 * <p>
 * This package defines the primary interface to a Blueprint Container,
 * {@code BlueprintContainer}. An instance of this type is available
 * inside a Blueprint Container as an implicitly defined component with the name
 * &quot;blueprintContainer&quot;.
 *
 * <p>
 * This package also declares the supporting exception types, listener, and constants 
 * for working with a Blueprint Container.
 *
 * <p>
 * Bundles wishing to use this package must list the package in the
 * Import-Package header of the bundle's manifest. This package has two types of
 * users: the consumers that use the API in this package and the providers that
 * implement the API in this package.
 * 
 * <p>
 * Example import for consumers using the API in this package:
 * <p>
 * {@code  Import-Package: org.osgi.service.blueprint.container; version="[1.0,2.0)"}
 * <p>
 * Example import for providers implementing the API in this package:
 * <p>
 * {@code  Import-Package: org.osgi.service.blueprint.container; version="[1.0,1.1)"}
 * 
 * @version 1.0
 * @author $Id: 5634fcb81d94a2134051808fc7d726baff5bb4f2 $
 */

package org.osgi.service.blueprint.container;

