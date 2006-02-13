/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/BundleListener.java,v 1.9 2005/08/05 03:22:00 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.util.EventListener;

/**
 * A <code>BundleEvent</code> listener. When a <code>BundleEvent</code> is
 * fired, it is asynchronously delivered to a <code>BundleListener</code>.
 * 
 * <p>
 * <code>BundleListener</code> is a listener interface that may be implemented
 * by a bundle developer.
 * <p>
 * A <code>BundleListener</code> object is registered with the Framework using
 * the {@link BundleContext#addBundleListener} method.
 * <code>BundleListener</code>s are called with a <code>BundleEvent</code>
 * object when a bundle has been installed, resolved, started, stopped, updated,
 * unresolved, or uninstalled.
 * 
 * @version $Revision: 1.9 $
 * @see BundleEvent
 */

public interface BundleListener extends EventListener {
	/**
	 * Receives notification that a bundle has had a lifecycle change.
	 * 
	 * @param event The <code>BundleEvent</code>.
	 */
	public void bundleChanged(BundleEvent event);
}
