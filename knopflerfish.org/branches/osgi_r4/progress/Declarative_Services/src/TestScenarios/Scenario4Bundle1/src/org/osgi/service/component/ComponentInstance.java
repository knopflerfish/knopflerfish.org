/*
 * $Header: /cvshome/build/org.osgi.service.component/src/org/osgi/service/component/ComponentInstance.java,v 1.9 2005/07/09 04:00:25 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2004, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.osgi.service.component;

/**
 * A ComponentInstance encapsulates an activated component configuration.
 * ComponentInstances are created whenever an configuration of a component is
 * activated.
 * 
 * <p>
 * ComponentInstances are never reused. A new ComponentInstance object will be
 * created when the component configuration is activated again.
 * 
 * @version $Revision: 1.9 $
 */
public interface ComponentInstance {
	/**
	 * Dispose of this component configuration. The component configuration will
	 * be deactivated. If the component configuration has already been
	 * deactivated, this method does nothing.
	 */
	public void dispose();

	/**
	 * Returns the component object for this activated component configuration.
	 * 
	 * @return The component object or <code>null</code> if the component
	 *         configuration has been deactivated.
	 */
	public Object getInstance();
}