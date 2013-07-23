/*
 * Copyright (c) OSGi Alliance (2010, 2011). All Rights Reserved.
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
 * Device Management Tree Package Version 2.0.
 * 
 * <p>This package contains the public API for the Device Management Tree 
 * manipulations. Permission classes are provided by the 
 * {@code org.osgi.service.dmt.security} package, and DMT plugin interfaces can be found in
 * the {@code org.osgi.service.dmt.spi} package.  Asynchronous notifications to remote 
 * management servers can be sent using the interfaces in the 
 * {@code org.osgi.service.dmt.notification} package.
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
 * {@code  Import-Package: org.osgi.service.dmt; version="[2.0,3.0)"}
 * <p>
 * Example import for providers implementing the API in this package:
 * <p>
 * {@code  Import-Package: org.osgi.service.dmt; version="[2.0,2.1)"}
 * 
 * @version $Id: 2cef1c3f595a788d21d5ac0a727bf153ea86e929 $
 */

package org.osgi.service.dmt;
