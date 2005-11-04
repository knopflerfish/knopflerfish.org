/*
 * $Header: /cvshome/build/org.osgi.service.upnp/src/org/osgi/service/upnp/UPnPLocalStateVariable.java,v 1.10 2005/08/11 03:07:43 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2005). All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
/**
 * To keep the current values getting from subscribed UPnPDevices. 
 * 
 * The actual values of the UPnPStateVaraible are passed as Java object type. 
 * 
 * @since 1.1
 **/
package org.osgi.service.upnp;

/**
 * A local UPnP state variable which allows the value of the state variable to
 * be queried.
 * 
 * @since 1.1
 */
public interface UPnPLocalStateVariable extends UPnPStateVariable {
	/**
	 * This method will keep the current values of UPnPStateVariables of a
	 * UPnPDevice whenever UPnPStateVariable's value is changed , this method
	 * must be called.
	 * 
	 * @return <code>Object</code> current value of UPnPStateVariable. if the
	 *         current value is initialized with the default value defined UPnP
	 *         service description.
	 */
	public Object getCurrentValue();
}
