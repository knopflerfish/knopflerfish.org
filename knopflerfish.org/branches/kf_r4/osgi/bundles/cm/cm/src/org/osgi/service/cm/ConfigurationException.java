/*
 * $Header: /cvshome/build/org.osgi.service.cm/src/org/osgi/service/cm/ConfigurationException.java,v 1.10 2005/08/12 01:14:57 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.osgi.service.cm;

/**
 * An <code>Exception</code> class to inform the Configuration Admin service
 * of problems with configuration data.
 * 
 * @version $Revision: 1.10 $
 */
public class ConfigurationException extends Exception {
	static final long	serialVersionUID	= -1690090413441769377L;

	private String		property;
	private String		reason;

	/**
	 * Nested exception.
	 */
	private Throwable	cause;

	/**
	 * Create a <code>ConfigurationException</code> object.
	 * 
	 * @param property name of the property that caused the problem,
	 *        <code>null</code> if no specific property was the cause
	 * @param reason reason for failure
	 */
	public ConfigurationException(String property, String reason) {
		super(property + " : " + reason);
		this.property = property;
		this.reason = reason;
		this.cause = null;
	}

	/**
	 * Create a <code>ConfigurationException</code> object.
	 * 
	 * @param property name of the property that caused the problem,
	 *        <code>null</code> if no specific property was the cause
	 * @param reason reason for failure
	 * @param cause The cause of this exception.
	 * @since 1.2
	 */
	public ConfigurationException(String property, String reason,
			Throwable cause) {
		super(property + " : " + reason);
		this.property = property;
		this.reason = reason;
		this.cause = cause;
	}

	/**
	 * Return the property name that caused the failure or null.
	 * 
	 * @return name of property or null if no specific property caused the
	 *         problem
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Return the reason for this exception.
	 * 
	 * @return reason of the failure
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Returns the cause of this exception or <code>null</code> if no cause
	 * was specified when this exception was created.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause
	 *         was specified.
	 * @since 1.2
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * The cause of this exception can only be set when constructed.
	 * 
	 * @param cause Cause of the exception.
	 * @return This object.
	 * @throws java.lang.IllegalStateException This method will always throw an
	 *         <code>IllegalStateException</code> since the cause of this
	 *         exception can only be set when constructed.
	 * @since 1.2
	 */
	public Throwable initCause(Throwable cause) {
		throw new IllegalStateException();
	}
}
